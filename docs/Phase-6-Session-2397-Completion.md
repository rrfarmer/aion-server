# Phase 6 Session 2397 Completion - Model Autogroup Logout Destroy Check

## Scope
- Modeled the final Java `AutoGroupService.onLogout(...)` current-auto-instance `destroyIfPossible(autoInstance)` branch.
- Preserved Java's guarded behavior: the branch only runs when the current auto instance still contains the logging-out player in `registeredAGPlayers`, which normally prevents `destroyIfPossible` from destroying because the registered-player map is not empty.

## Java Source Reviewed
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player player)` ordering and semi-offline setup before autogroup logout.
- `game-server/src/com/aionemu/gameserver/services/AutoGroupService.java`
  - `onLogout(Player player)`
  - `destroyIfPossible(AutoInstance autoInstance)`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoInstance.java`
  - `getRegisteredAGPlayers()`
  - `unregister(Player player)`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvpInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoPvPFFAInstance.java`
- `game-server/src/com/aionemu/gameserver/model/autogroup/AutoHarmonyInstance.java`

## Implemented
- Added `AutoGroupInstanceLeaveRuntimeService.DestroyCurrentAutoInstanceIfPossibleOnLogout(...)`.
- Added `AutoGroupLogoutCurrentInstanceDestroyResult` and `AutoGroupLogoutCurrentInstanceDestroyStatus`.
- `GameServerConnection.LeavePlayerWorldAsync(...)` now invokes the modeled current-instance logout destroy check after queued search cleanup, start-enter cancel-enter intents, and queue rechecks.
- The connection computes the online-player count as current instance player count minus the logging-out player, matching Java's `PlayerLeaveWorldService` semi-offline ordering before `AutoGroupService.onLogout(...)`.
- The runtime result records missing/current-unregistered/no-destroy outcomes so future slices can reason about this branch without conflating it with normal `onLeaveInstance(...)`.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | C# logout now invokes the autogroup current-instance destroy check after search cleanup. Overall Java leave-world ordering remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Service | Partial | Unit Tested | Partial Parity | Current auto-instance logout destroy check is modeled conservatively. Java guard means registered players are not unregistered by this branch, so destroy normally does not happen. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Model | Partial | Unit Tested | Partial Parity | Registered-player membership is checked before destroyIfPossible. Subclass scoring/group details remain broader partial parity. |

## Tests Updated
| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `DestroyCurrentAutoInstanceIfPossibleOnLogout_ChecksButDoesNotDestroyWhilePlayerStillRegisteredLikeJava` | Unit | Java `AutoGroupService.onLogout` guard plus `destroyIfPossible` condition | A registered current auto-instance player triggers the check but is not unregistered or destroyed because registered players remain. | Source-reviewed Java branch with C# runtime assertions. | Does not model concurrent mutation between the guard and destroy check. |
| `DestroyCurrentAutoInstanceIfPossibleOnLogout_MissingOrUnregisteredPlayerIsNoOpLikeJavaGuard` | Unit | Java `autoInstance != null && registeredAGPlayers.containsKey(objectId)` guard | Missing current instance and unregistered current player produce no destroy side effects. | Source-reviewed Java guard with C# runtime assertions. | None for guard behavior. |
| `LeavePlayerWorldAsync_AutoGroupLogoutDestroyCheckDoesNotUnregisterRegisteredPlayerLikeJava` | Integration | Java `PlayerLeaveWorldService.leaveWorld` -> `AutoGroupService.onLogout` current-instance branch | Live logout calls the modeled branch without accidentally using normal `onLeaveInstance` unregister/destroy behavior. | C# connection/runtime/world-state assertions tied to reviewed Java ordering. | Does not prove actual destroy because Java guard prevents it in this scenario. |

## Validation Decision
- Changed surface: live connection dispatch and autogroup runtime state.
- Specific behavior/contract: after autogroup logout search cleanup, Java checks the player's current world-map auto instance and calls `destroyIfPossible(autoInstance)` only if that auto instance still has the player registered; that branch must not unregister the player and usually does not destroy because registered players remain.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore`
- Focused Java/Maven command: skipped; no targeted Java fixture exists and no Java source changed.
- Broad-validation trigger: live connection dispatch and instance destroy workflow.
- Broad .NET decision: skipped after focused validation; the changed path is isolated to autogroup logout current-instance checking and the tests directly cover runtime, connection, and adjacent leave-plan behavior.
- Why this scope is sufficient: focused tests prove the Java guard/no-destroy semantics and protect against accidentally reusing normal `onLeaveInstance(...)`, which would unregister and destroy in the tested setup.

## Validation Result
- Passed: 38 tests, 0 failed, 0 skipped.
- Pre-existing nullable/analyzer warnings remain in unrelated files.

## Known Remaining Gaps
- Java `destroyIfPossible` destruction from concurrent registry mutation is represented by result shape but not practically triggered by deterministic C# tests.
- Logout-specific quick-entry refill after start-enter cancel-enter remains covered only through the shared cancel-enter helper.
- Additional-registration cleanup during logout-triggered ready matches is covered through shared ready-match tests, not a separate logout-specific cleanup scenario.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering is still partial.

## Summary Metrics
- Total Java artifacts discovered in this UOW: 6.
- Total artifacts ported or extended in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification / partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall Phase 6 completion: unchanged materially; this is a narrow autogroup logout destroy-check slice.
