# Phase 6 Session 2412 Completion - Leave-World Duel Loss

## Scope
- Audited Java leave-world non-dead duel handling.
- Wired the C# leave-world path to lose an active duel only when the logging-out player is not dead.
- Added connection workflow coverage for the duel-loss branch and the Java `if dead ... else if dueling` ordering.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/services/DuelService.java`
  - `loseDuel(Player loser)`
  - `isDueling(Player player)`
  - `removeDuel(Player player)`
  - `onDuelEnd(DuelResult duelResult, Player player, int opponentId)`

## Implemented
- `GameServerConnection.LeavePlayerWorldAsync(...)` now branches like Java after kisk logout:
  - dead players take the dead-player revive branch;
  - otherwise dueling players call the modeled duel-loss cleanup.
- Added `ApplyLogoutDuelLossAsync(...)` to dispatch `PlayerDuelRequestService.LoseDuel(...)` packet intents through the existing duel packet send path.
- Added workflow tests proving a non-dead duelist loses and that a dead duelist takes revive instead of duel loss.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Dead-vs-duel logout branching is now pinned after kisk logout. Full logout ordering remains partial. |
| `com.aionemu.gameserver.services.DuelService` | `Aion.GameServer.Services.PlayerDuelRequestService` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Logout invokes modeled `LoseDuel`, sends loser/winner result packets, and removes both duel-map directions. Java debuff cleanup, summoned-object attack cancel, draw-task cancellation, and `PlayerService.getPlayerName` DB fallback remain partial. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_NonDeadDuelingPlayerLosesDuelLikeJavaLogoutBranch` | Regression | Java `PlayerLeaveWorldService.leaveWorld` non-dead `DuelService.loseDuel` branch. | Logging out a non-dead duelist sends lose/win results and removes the duel. | Focused connection workflow test plus existing duel service behavior. | Does not cover Java effect cleanup or draw task cancellation. |
| `LeavePlayerWorldAsync_DeadDuelingPlayerTakesReviveBranchBeforeDuelLossLikeJava` | Regression | Java `if (player.isDead()) ... else if (DuelService.isDueling(player))` ordering. | Dead logout revives and does not lose the duel in the same leave-world pass. | Focused connection workflow test. | Leaves broader Java duel/death interactions for later slices. |

## Validation Decision
- Changed surface: production connection dispatch plus focused duel workflow tests.
- Specific behavior/contract: non-dead dueling logout calls Java-equivalent `loseDuel`, while dead logout takes the revive branch before duel loss.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionDuelRequestTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: production connection/runtime-state behavior changed.
- Broad .NET decision: skipped after focused duel/logout and adjacent dead-logout workflow tests passed; no focused evidence exposed wider risk.
- Why this scope is sufficient: the edited connection tests exercise both Java branch outcomes, and `GameServerConnectionDuelRequestTests` already contains the service-level lose/draw duel packet assertions.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionDuelRequestTests|FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests" --no-restore`
  - Passed: 25 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside the kisk, dead-player revive, and duel-loss slices.
- Java `DuelService.onDuelEnd` cancels current skills, ends debuffs by opponent, and cancels summoned-object attacks; the C# duel service currently only models result packets and duel-map cleanup.
- Java draw-task scheduling/cancellation is represented only as service comments and remove-map behavior.
- Later inventory/warehouse owner cleanup remains unpinned.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 2.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
