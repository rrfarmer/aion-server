# Phase 6 Session 2427 Completion

Status: Phase 6 continues; UOW-2427 enabled the Java group disconnected no-online disband branch in the C# logout path. Alliance disconnected dispatch remains unwired.

## Scope

- UOW: UOW-2427 group disconnected no-online disband.
- Reviewed Java `PlayerDisconnectedEvent`, `PlayerGroupService.disband`, `GroupDisbandEvent`, `PlayerGroupLeavedEvent`, and base `PlayerLeavedEvent`.
- Added a dedicated C# runtime disband operation for the disconnected logout branch where no group members remain online.
- Wired `PlayerEnterWorldService.LeaveWorldAsync` to invoke the disband operation when `PlayerGroupDisconnectedPlanner` reports `NoOnlineMembersDisband`.
- Added focused runtime and logout regression tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - If `group.getOnlineMembers().isEmpty()`, calls `PlayerGroupService.disband(group)`.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `disband(group)` removes find-group recruitment, removes the group from the static map, then fires `GroupDisbandEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/GroupDisbandEvent.java`
  - Replays `PlayerGroupLeavedEvent(DISBAND)` for each member.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupLeavedEvent.java`
  - Removes each member, attempts disband system messages, then runs base leave behavior.
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
  - Offline members receive no leave packets, but the event-service left-team notification would still be invoked.

## Implemented

- Added `PlayerGroupRuntime.DisbandAfterDisconnectedNoOnlineMembers(int teamId)`.
  - Guards against misuse by returning null if any runtime member is online.
  - Removes find-group team recruitment through `FindGroupRecruitmentPlanService`.
  - Clears every member's group metadata.
  - Removes group descriptor, member list, and brand state.
  - Returns `PlayerGroupDisconnectedDisbandPlan` metadata with base leave side-effect plans.
- Updated `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` to handle `NoOnlineMembersDisband` before the normal disconnected fanout.
- Tightened the existing grouped last-online test by marking its teammate online, keeping it on the non-disband branch.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `DisbandAfterDisconnectedNoOnlineMembers_ClearsRuntimeAndRecruitmentLikeJavaLogoutDisband` | Unit | Java `PlayerDisconnectedEvent`, `PlayerGroupService.disband`, `GroupDisbandEvent`, `PlayerGroupLeavedEvent`, and `PlayerLeavedEvent` source review | Runtime no-online disband removes find-group recruitment, clears group state for all offline members, and records base leave metadata without packet intents. | Focused C# runtime assertions over recruitment removal, group removal, member metadata clearing, and offline base leave plans. | C# still records only metadata for Java `EventService.onLeftTeam`; no live event-service dispatch exists. |
| `LeaveWorld_DisbandGroupWhenNoMembersRemainOnlineLikeJavaLogout` | Regression | Java logout source review through `PlayerGroupService.onPlayerLogout` and `PlayerDisconnectedEvent` | Live logout disbands the group when the logging-out player makes all members offline, removes team recruitment, clears runtime membership, and sends no packets. | Focused C# logout/registry assertions over no packet sends, runtime cleanup, and find-group cleanup. | Real client connection not exercised; registry fake verifies the live dispatch boundary. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | No-online disband branch is now live for group logout. Non-disband fanout and leader-disconnect dispatch were covered in UOW-2426. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.DisbandAfterDisconnectedNoOnlineMembers` | Service / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# removes find-group recruitment and runtime group state for the no-online disconnected branch. Broader manual disband commands and offline timeout paths are not implied. |
| `com.aionemu.gameserver.model.team.group.events.GroupDisbandEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedDisbandPlan` plus runtime cleanup | Event / Runtime Mutation | Partial | Unit Tested | Partial Parity | C# replays the effective offline cleanup result and records base leave metadata, but does not live-dispatch Java `EventService.onLeftTeam`. |
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupLeavedEvent` | `Aion.GameServer.Services.PlayerGroupRuntime` / `PlayerGroupLeavePlan` / `PlayerGroupDisconnectedDisbandPlan` | Event / Leave Handling | Partial | Unit Tested | Partial Parity | Normal leave disband and disconnected no-online disband now remove recruitment. Packet attempts to offline members are intentionally no live sends. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service / State Store | Partial | Unit Tested | Partial Parity | Disconnected group disband now calls the existing team recruitment removal path. Live world broadcast intent remains metadata-only in this logout branch. |
| `com.aionemu.gameserver.model.team.common.events.PlayerLeavedEvent` | `Aion.GameServer.Services.PlayerBaseLeavePlanner` | Base Event / Side Effects | Partial | Unit Tested | Partial Parity | Offline no-online disband records that base leave would notify EventService and has no packet intents. Live EventService integration is still missing. |

## Validation Decision

- Changed surface: live logout runtime state mutation plus find-group cleanup for the group disconnected no-online branch.
- Specific behavior/contract: Java group disconnected logout calls `PlayerGroupService.disband(group)` when no group members are online; disband removes find-group recruitment and group runtime state, while offline packet sends are no-ops.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 101
  - Failed: 0
  - Skipped: 0
- Hygiene:
  - `git diff --check`
  - Passed; only Git CRLF conversion warnings.
- Focused Java/Maven command:
  - Not run; no Java source or fixtures changed, and Java evidence came from source review.
- Broad-validation trigger:
  - Live runtime disband plus find-group cleanup is a broad-validation trigger.
- Broad .NET decision:
  - Full project/solution validation skipped after focused evidence. The filtered command compiled affected dependencies and asserted the exact logout/runtime branch; no packet primitive, shared registry implementation, persistence repository, scheduler, or serialization helper changed.
- Why this scope is sufficient:
  - The changed live effect is isolated to group runtime cleanup and an existing find-group removal API. Focused tests cover direct runtime cleanup plus the logout composition branch.

## Known Remaining Gaps

- Alliance disconnected-event live dispatch remains unwired.
- Java `EventService.onLeftTeam` from disband is represented only as base leave metadata.
- Manual group disband command parity and offline timeout disband parity are not completed by this UOW.
- Real client connection dispatch is not exercised for this branch because Java sends no packets when every member is offline.
- C# logout ordering still differs from Java around world removal and persistence timing.

## Summary Metrics

- Java artifacts reviewed: 5.
- C# artifacts reviewed: 5.
- Production files changed: 3.
- Test files changed: 2.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 6.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
