# Phase 6 Session 2428 Completion

Status: Phase 6 continues; UOW-2428 enabled live C# alliance disconnected logout dispatch and no-online alliance disband cleanup. League broadcast side effects remain metadata-only.

## Scope

- UOW: UOW-2428 alliance disconnected live logout dispatch.
- Reviewed Java alliance logout, disconnected, leader-change, disband, and leave events.
- Wired `PlayerEnterWorldService.LeaveWorldAsync` to dispatch alliance disconnected logout after group disconnected logout, preserving Java team logout order.
- Added a disconnected no-online alliance disband runtime operation.
- Added focused logout and runtime tests for alliance disconnected fanout, leader-change ordering, and no-online disband cleanup.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `onPlayerLogout` updates `PlayerAllianceMember.lastOnlineTime`, then fires `PlayerDisconnectedEvent`.
  - `disband(alliance, false)` removes find-group recruitment, runs `AllianceDisbandEvent`, removes the alliance map entry, then notifies league membership when present.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - Leader disconnect runs `ChangeAllianceLeaderEvent` before disconnected fanout.
  - Sends `STR_FORCE_HE_BECOME_OFFLINE`, `SM_ALLIANCE_MEMBER_INFO(DISCONNECTED)`, and `SM_ALLIANCE_INFO` to every other member.
  - Disbands after fanout when no online members remain; otherwise broadcasts to league when present.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - Event-player-null leader change prefers an online vice captain, then next online member.
  - Removes the new leader from vice captains, sends `SM_ALLIANCE_INFO`, and sends `STR_FORCE_YOU_BECOME_NEW_LEADER` to the new leader.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
  - Replays `PlayerAllianceLeavedEvent(DISBAND)` for every member.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
  - Disband leave removes member state and sends disband messages through Java `PacketSendUtility`.

## Implemented

- Added `PlayerAllianceRuntime.DisbandAfterDisconnectedNoOnlineMembers(int allianceId)`.
  - Guards against disbanding while any alliance member remains online.
  - Removes find-group alliance recruitment.
  - Clears every member's alliance metadata and alliance group reference.
  - Removes alliance descriptor, member list, league id, vice captains, ready state, and brand state.
  - Returns `PlayerAllianceDisconnectedDisbandPlan` metadata, including base leave plans and whether Java would notify league after disband.
- Added `PlayerAllianceDisconnectedDisbandPlan`.
- Updated `PlayerEnterWorldService.LeaveWorldAsync` to call `DispatchAllianceDisconnectedLogoutAsync` after group disconnected dispatch.
- Added live alliance logout dispatch:
  - Leader-disconnect fallback mutation through `PlayerAllianceRuntime.ChangeLeader`.
  - Per-member leader-change packet ordering matching Java's `forEach`.
  - Offline system/member/alliance-info fanout to online remaining members.
  - No-online disband after fanout, matching Java event order.
  - Self-recipient and offline-recipient sends are skipped to model Java `PacketSendUtility.sendPacket(Player, ...)`.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DisbandAfterDisconnectedNoOnlineMembers_ClearsRuntimeAndRecruitmentLikeJavaLogoutDisband` | Unit | Java `PlayerDisconnectedEvent`, `PlayerAllianceService.disband`, `AllianceDisbandEvent`, and `PlayerAllianceLeavedEvent` source review | Runtime disconnected disband removes find-group recruitment, clears alliance state for all offline members, records base leave metadata, and removes alliance runtime maps. | Focused C# runtime assertions over recruitment removal, alliance removal, member metadata clearing, and offline base leave plans. | League-left notification is metadata only. |
| `LeaveWorld_DispatchesAllianceDisconnectedFanoutToRemainingMembersLikeJavaLogout` | Regression | Java `PlayerAllianceService.onPlayerLogout` and `PlayerDisconnectedEvent` source review | Non-leader alliance logout sends offline system message, disconnected member-info, and alliance-info to remaining online members, and skips the disconnected player. | Focused C# logout/registry assertions over packet order, packet types, recipients, and message id `1301019`. | Real client connection not exercised. |
| `LeaveWorld_DispatchesAllianceLeaderChangeBeforeDisconnectedFanoutLikeJavaLogout` | Regression | Java `PlayerDisconnectedEvent` and `ChangeAllianceLeaderEvent` source review | Leader logout selects vice captain fallback, mutates leader, removes fallback from vice captains, sends leader-change packets before disconnected fanout, and skips self sends. | Focused C# logout/registry assertions over recipient order, packet types, leader message id `1300999`, offline message id `1301019`, runtime leader mutation, and vice-captain removal. | League leader-change broadcasts remain metadata-only. |
| `LeaveWorld_DisbandAllianceWhenNoMembersRemainOnlineLikeJavaLogout` | Regression | Java alliance disconnected no-online disband source review | Live logout disbands the alliance when all members are offline, removes find-group team recruitment, clears runtime membership, and sends no packets. | Focused C# logout/registry assertions over no packet sends, runtime cleanup, find-group cleanup, and logout persistence call. | League-left side effect is not live. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `Aion.GameServer.Services.PlayerEnterWorldService.DispatchAllianceDisconnectedLogoutAsync` plus `PlayerAllianceRuntime.UpdateMemberLastOnlineTime` | Logout Hook / Service | Partial | Regression Tested | Partial Parity | Alliance last-online update now feeds live disconnected dispatch. Java league side effects remain metadata-only. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchAllianceDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Live C# dispatch covers non-leader fanout, leader-disconnect fallback ordering, and no-online disband cleanup. League broadcast branch remains not live. |
| `com.aionemu.gameserver.model.team.alliance.events.ChangeAllianceLeaderEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime.ChangeLeader` plus logout dispatch | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Leader logout now mutates fallback leader before disconnected fanout and preserves Java per-member packet ordering for non-league alliances. League leader broadcast remains metadata-only. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.DisbandAfterDisconnectedNoOnlineMembers` | Service / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# removes find-group recruitment and runtime alliance state for disconnected no-online logout. Manual disband and offline timeout disband are not implied. |
| `com.aionemu.gameserver.model.team.alliance.events.AllianceDisbandEvent` | `Aion.GameServer.Services.PlayerAllianceDisconnectedDisbandPlan` plus runtime cleanup | Event / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# clears the effective offline disband result and records base leave metadata. Live `EventService.onLeftTeam` and league-left dispatch remain missing. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceRuntime` / `PlayerAllianceLeaveWorkflowPlan` / `PlayerAllianceDisconnectedDisbandPlan` | Event / Leave Handling | Partial | Unit Tested | Partial Parity | Existing normal leave workflow remains; disconnected no-online disband now clears alliance state. Broader leave reasons and Vortex side effects are not covered here. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service / State Store | Partial | Unit Tested | Partial Parity | Disconnected alliance disband now calls existing team recruitment removal. Live world broadcast intent remains metadata-only in this logout branch. |

## Validation Decision

- Changed surface: live logout dispatch and runtime state mutation for alliance disconnected events.
- Specific behavior/contract: Java alliance logout updates member last-online, performs leader-change before disconnected fanout, sends offline/member/alliance-info packets to online remaining members, then disbands when no online members remain.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 82
  - Failed: 0
  - Skipped: 0
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from source review.
- Broad-validation trigger:
  - Live alliance runtime mutation, connection dispatch, and find-group cleanup.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered command compiled affected dependencies and asserted the exact logout/runtime branches; no packet primitive, shared registry implementation, persistence repository, scheduler, or serialization helper changed.
- Why this scope is sufficient:
  - The changed live effects are isolated to `LeaveWorldAsync` alliance dispatch and `PlayerAllianceRuntime` disconnected disband cleanup. Focused tests cover live packet ordering, runtime leader mutation, vice-captain mutation, no packet sends for all-offline disband, and find-group recruitment removal.

## Known Remaining Gaps

- Java league broadcasts from alliance disconnected and leader-change events remain metadata-only.
- Java league-left notification after `PlayerAllianceService.disband(alliance, false)` is recorded only as metadata.
- Java `EventService.onLeftTeam` remains metadata-only for disconnected alliance disband.
- Manual alliance disband command parity, offline timeout disband parity, Vortex defence/offence cleanup, and broader alliance leave reasons still need review.
- C# logout ordering still differs from Java around world removal and persistence timing.

## Summary Metrics

- Java artifacts reviewed: 5.
- C# artifacts reviewed: 5.
- Production files changed: 3.
- Test files changed: 2.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 7.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
