# Phase 6 Session 2410 Completion - Leave-World Kisk Offline Binding

## Scope
- Audited Java leave-world kisk logout behavior.
- Added connection-level C# coverage proving leave-world reaches the kisk offline-binding side effect.
- Confirmed the stored binding can be restored once on a returning player, matching the Java logout/login slice.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)`
- `game-server/src/com/aionemu/gameserver/services/KiskService.java`
  - `onLogout(Player player)`
  - `onLogin(Player player)`

## Implemented
- Added `LeavePlayerWorldAsync_BoundKiskRegistersOfflineBindingLikeJavaKiskServiceOnLogout`.
- The regression registers kisk `9001`, logs out player `1002` while bound to that kisk, then restores the offline binding on a fresh player object.
- The test asserts the restore status, restored kisk object id, player `BoundKiskObjectId`, kisk membership, and one-shot removal of the offline binding.
- No production code changed in this UOW.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection dispatch | Partial | Regression Tested | Partial Parity | Leave-world now has connection-level coverage for the Java `KiskService.onLogout(player)` side effect. Broader Java leave-world ordering remains partial. |
| `com.aionemu.gameserver.services.KiskService` | `Aion.GameServer.Services.PlayerKiskRegistry` | Service | Partial | Unit Tested / Regression Tested | Partial Parity | Registry restore behavior was already covered; this UOW pins the leave-world path to offline binding registration. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LeavePlayerWorldAsync_BoundKiskRegistersOfflineBindingLikeJavaKiskServiceOnLogout` | Regression | Java `PlayerLeaveWorldService.leaveWorld` calls `KiskService.onLogout`; `KiskService.onLogout` stores the current kisk for `onLogin` restoration. | Logging out a bound player stores an offline kisk binding that restores once on return. | Focused connection workflow test plus adjacent `PlayerKiskRegistryTests`. | Does not prove full Java leave-world ordering around KiskService relative to every other logout service. |

## Validation Decision
- Changed surface: test-only connection workflow coverage.
- Specific behavior/contract: leave-world invokes the kisk offline-binding side effect equivalent to Java `KiskService.onLogout(player)`.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerKiskRegistryTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: none; this UOW changed one focused test.
- Broad .NET decision: skipped.
- Why this scope is sufficient: the edited connection workflow test proves the leave-world side effect is reached, while adjacent registry tests cover the offline restore contract.

## Validation Result
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionKiskReviveWorkflowTests|FullyQualifiedName~PlayerKiskRegistryTests" --no-restore`
  - Passed: 18 tests, 0 failed, 0 skipped.
  - Pre-existing nullable/analyzer warnings were emitted during the compile-producing run.

## Known Remaining Gaps
- Full Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial outside the kisk slice.
- Kisk logout ordering relative to instance, GM, and dead-player revive handling is not pinned.
- Replacement readiness still needs broader real-client gameplay coverage beyond this workflow slice.

## Summary Metrics
- Total Java artifacts reviewed in this UOW: 2.
- Total C# artifacts changed in this UOW: 1 test class.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: Phase 6 remains in progress; no estimate change.
