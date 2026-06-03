# Phase 6 Session 2429 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2429 group disconnected offline-recipient dispatch parity.

## Last Completed UOW

- UOW-2429 aligned group disconnected logout live dispatch with Java `PacketSendUtility.sendPacket(Player, packet)`.
- Offline remaining group members are now skipped in both disconnected fanout and leader-change fanout.
- Planner metadata remains unchanged and still records Java's attempted sends.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2429] Skip offline group logout recipients`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2429-Completion.md`
- `docs/Phase-6-Session-2429-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerDisconnectedEvent.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerEnterWorldService`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## What Changed

- Added `ShouldSkipTeamLogoutRecipient(...)` in `PlayerEnterWorldService`.
- Group disconnected logout dispatch now skips recipients who are:
  - the disconnected player,
  - offline remaining members,
  - no longer present in the runtime group member snapshot.
- Added tests for:
  - offline recipient skip during non-leader disconnected fanout,
  - offline recipient skip during leader-change and disconnected fanout.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupDisconnectedPlannerTests" --no-restore`
- Result:
  - Passed: 58
  - Failed: 0
  - Skipped: 0
- `git diff --check`
- Result:
  - Passed; only Git CRLF conversion warnings.
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.group.events.PlayerDisconnectedEvent` | `Aion.GameServer.Services.PlayerGroupDisconnectedPlanner` plus `PlayerEnterWorldService.DispatchGroupDisconnectedLogoutAsync` | Event / Live Dispatch | Partial | Regression Tested | Partial Parity | Live dispatch now skips offline non-self recipients in addition to the disconnected player, matching Java `PacketSendUtility`. Planner still records attempted sends for Java-source metadata. |
| `com.aionemu.gameserver.model.team.group.events.ChangeGroupLeaderEvent` | `Aion.GameServer.Services.PlayerGroupRuntime.ChangeLeader` plus logout dispatch | Event / Runtime Mutation | Partial | Regression Tested | Partial Parity | Leader-disconnect fallback mutation remains live; leader-change packet sends now skip offline recipients. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.PlayerEnterWorldService.ShouldSkipTeamLogoutRecipient` plus `IGameClientConnectionRegistry.SendPacketToPlayerAsync` | Packet Send Utility / Boundary | Partial | Regression Tested | Partial Parity | C# now models the Java `player.isOnline()` send guard for group disconnected logout. Broader PacketSendUtility/broadcast parity is not implied. |

## Known Gaps

- Alliance league broadcasts from disconnected and leader-change events remain metadata-only.
- Java league-left notification after alliance disband is not live.
- Java `EventService.onLeftTeam` remains metadata-only for group and alliance disconnected disband.
- Manual group/alliance disband command parity and offline timeout disband parity still need review.
- Vortex defence/offence cleanup for alliance leave/kick/logout-adjacent flows still needs review.
- C# logout ordering is still not fully Java-identical around world removal and persistence.

## Remaining Risks

- Live team logout dispatch now uses runtime online state at send time; focused tests covered group and alliance disconnected branches, but no broad suite was run.
- League runtime composition may need careful ordering because Java alliance disband can notify league after alliance map removal.
- Java `ConcurrentHashMap` iteration order remains not deterministic; C# runtime list order is deterministic.

## Next Recommended UOW

UOW-2430: Add readiness or live wiring for alliance disconnected league broadcast behavior.

Suggested scope:
- Java:
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerDisconnectedEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/events/ChangeAllianceLeaderEvent.java`
  - `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/League.java`
  - `game-server/src/com/aionemu/gameserver/model/team/league/events/LeagueLeftEvent.java`
- C#:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerLeagueRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLeagueRuntimeTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`

Safe fallback candidate:
- Audit manual/offline-timeout group or alliance disband behavior against Java now that logout disband is live.
- Java:
  - `PlayerGroupService.OfflinePlayerChecker`
  - `PlayerAllianceService.OfflinePlayerAllianceChecker`
  - `PlayerGroupLeavedEvent`
  - `PlayerAllianceLeavedEvent`
- C#:
  - `PlayerGroupRuntime`
  - `PlayerAllianceRuntime`
  - adjacent runtime tests.

## Focused Validation Recipe

- For UOW-2430:
  - Specific behavior/contract:
    - Java alliance disconnected and leader-change events call league broadcast hooks in league alliances, and alliance disband notifies league after disband when `onBefore` is false.
  - Focused C# command:
    - If adding live league/alliance wiring:
      - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLeagueRuntimeTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore`
    - If only adding readiness/planning metadata:
      - use the edited readiness test class filter, or `git diff --check` for docs-only.
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
- UOW-2429 aligned group disconnected live dispatch with Java's offline-recipient send guard.
