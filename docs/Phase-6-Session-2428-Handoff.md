# Phase 6 Session 2428 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2428 alliance disconnected live logout dispatch.

## Last Completed UOW

- UOW-2428 wired live alliance disconnected logout dispatch in C#.
- Alliance logout now sends Java-shaped disconnected fanout, applies leader fallback before fanout, and clears alliance/find-group state when no members remain online.
- League broadcast side effects remain metadata-only.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2428] Wire alliance disconnected logout dispatch`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `docs/Phase-6-Session-2428-Completion.md`
- `docs/Phase-6-Session-2428-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/AllianceDisbandEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Services.PlayerAllianceRuntime`
- `Aion.GameServer.Services.PlayerAllianceDisconnectedDisbandPlan`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`
- `Aion.GameServer.Tests.PlayerAllianceRuntimeTests`

## What Changed

- `LeaveWorldAsync` now calls `DispatchAllianceDisconnectedLogoutAsync` after group disconnected dispatch.
- Alliance leader logout applies `PlayerAllianceRuntime.ChangeLeader` before disconnected fanout.
- Leader-change packet order is dispatched per Java member iteration:
  - `SM_ALLIANCE_INFO` for each online remaining member.
  - `STR_FORCE_YOU_BECOME_NEW_LEADER` for the fallback leader.
- Disconnected fanout sends `STR_FORCE_HE_BECOME_OFFLINE`, `SM_ALLIANCE_MEMBER_INFO(DISCONNECTED)`, and `SM_ALLIANCE_INFO`.
- Offline recipients and the disconnected player are skipped to match Java `PacketSendUtility.sendPacket(Player, ...)`.
- Added `PlayerAllianceRuntime.DisbandAfterDisconnectedNoOnlineMembers`.
- Added `PlayerAllianceDisconnectedDisbandPlan`.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~PlayerAllianceDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 82
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

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

## Known Gaps

- Java league broadcasts from alliance disconnected and leader-change events remain metadata-only.
- Java league-left notification after alliance disband is not live.
- Java `EventService.onLeftTeam` remains metadata-only for disconnected alliance disband.
- Manual alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence.

## Remaining Risks

- Live alliance dispatch now mutates shared runtime state and sends through the connection registry; focused tests covered the changed branches, but no broad suite was run.
- League runtime composition may need careful ordering because Java `PlayerAllianceService.disband(alliance, false)` notifies league after alliance map removal.
- Java `ConcurrentHashMap` iteration order remains not deterministic; C# runtime list order is deterministic.
- Offline recipient skip was added for alliance logout to match Java `PacketSendUtility`; existing group disconnected dispatch may still need an offline-recipient audit.

## Next Recommended UOW

UOW-2429: Close one remaining team-logout parity edge, preferably an audit/fix for offline-recipient behavior in group disconnected dispatch or a readiness/live slice for alliance league broadcasts.

Safe candidate A:
- Audit group disconnected logout offline-recipient behavior.
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupDisconnectedPlannerTests.cs`
- Risk:
  - Group dispatch currently skips self but may send to offline non-self fake recipients even though Java `PacketSendUtility` would no-op.

Safe candidate B:
- Add readiness/live plan for alliance disconnected league broadcast behavior.
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLeagueRuntimeTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- Risk:
  - League broadcasts may require multi-alliance packet ordering and league captain mutation.

## Focused Validation Recipe

- For candidate A:
  - Specific behavior/contract:
    - Java `PacketSendUtility.sendPacket(Player, ...)` skips offline group recipients during disconnected logout.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough.
  - Broad-validation trigger:
    - live connection dispatch behavior; document before considering unfiltered project/solution validation.
- For candidate B:
  - Specific behavior/contract:
    - Java alliance disconnected and leader-change events call league broadcast hooks in league alliances, and alliance disband notifies league after disband when `onBefore` is false.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLeagueRuntimeTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore`
  - Java/Maven:
    - Not expected unless Java source or fixtures change; Java source review should be enough unless a targeted Java league test is found.
  - Broad-validation trigger:
    - live league/alliance runtime mutation and connection dispatch; document before considering unfiltered project/solution validation.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- UOW-2426 made group disconnected non-disband logout live.
- UOW-2427 made group disconnected no-online disband live.
- UOW-2428 made alliance disconnected logout live for non-league fanout, leader fallback, and no-online disband cleanup.
