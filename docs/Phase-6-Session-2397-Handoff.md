# Phase 6 Session 2397 Handoff

## Current Phase
- Phase 6: Port Game Core.

## Last Completed UOW
- `UOW-2397`: Modeled Java autogroup logout current-instance `destroyIfPossible` check.

## Commits Made
- `[Phase 6][UOW-2397] Model autogroup logout destroy check`

## Files Changed
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/AutoGroupInstanceLeaveRuntimeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AutoGroupInstanceLeaveRuntimeServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionAutoGroupTests.cs`
- `docs/Phase-6-Session-2397-Completion.md`
- `docs/Phase-6-Session-2397-Handoff.md`

## Java Artifacts Touched
- `com.aionemu.gameserver.services.player.PlayerLeaveWorldService`
- `com.aionemu.gameserver.services.AutoGroupService`
- `com.aionemu.gameserver.model.autogroup.AutoInstance`
- `com.aionemu.gameserver.model.autogroup.AutoPvpInstance`
- `com.aionemu.gameserver.model.autogroup.AutoPvPFFAInstance`
- `com.aionemu.gameserver.model.autogroup.AutoHarmonyInstance`

## C# Artifacts Touched
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService`
- `Aion.GameServer.Tests.AutoGroupInstanceLeaveRuntimeServiceTests`
- `Aion.GameServer.Tests.GameServerConnectionAutoGroupTests`

## What Changed
- Added a logout-specific current auto-instance destroy check in `AutoGroupInstanceLeaveRuntimeService`.
- Added result/status records for the logout destroy-check branch.
- `LeavePlayerWorldAsync(...)` now invokes this check after autogroup search cleanup, start-enter cancel-enter intents, and queue rechecks.
- Tests document the Java nuance: this branch does not unregister the player and normally does not destroy while `registeredAGPlayers` still contains the logging-out player.

## Tests Run
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests|FullyQualifiedName~AutoGroupInstanceLeavePlanServiceTests" --no-restore`
- Result: 38 passed, 0 failed, 0 skipped.
- Java/Maven: skipped because no targeted Java fixture exists and no Java source changed.
- Broad .NET: skipped after focused validation; broad trigger was live connection dispatch and instance destroy workflow, but the changed path was isolated and directly covered by focused autogroup connection/runtime/plan tests.

## Migration Parity Table
| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` | `Aion.GameServer.Network.Aion.GameServerConnection` | Connection / Logout Handler | Partial | Integration Tested | Partial Parity | Autogroup current-instance destroy check is now invoked in logout ordering. Overall leave-world ordering remains partial. |
| `com.aionemu.gameserver.services.AutoGroupService` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Service | Partial | Unit Tested | Partial Parity | Current-instance logout destroy check is modeled and tested as a guarded no-op while the player remains registered. |
| `com.aionemu.gameserver.model.autogroup.AutoInstance` | `Aion.GameServer.Services.AutoGroupInstanceLeaveRuntimeService` | Runtime Model | Partial | Unit Tested | Partial Parity | Registered-player membership and destroy predicate are represented; subclass-specific scoring and group bookkeeping remain broader partial parity. |

## Known Gaps
- Concurrent Java mutation between the `registeredAGPlayers.containsKey` guard and `destroyIfPossible` is not deterministically tested.
- Logout-specific quick-entry refill after start-enter cancel-enter has no separate integration test.
- Logout-specific additional-registration cleanup during ready-match dispatch has no separate integration test.
- Overall Java `PlayerLeaveWorldService.leaveWorld(...)` ordering remains partial.

## Next Recommended UOW
- `UOW-2398`: Add a logout-specific quick-entry refill test for start-enter cancel-enter to prove the shared cancel-enter helper covers Java `destroyOrAddPlayersFromQuickEntries(autoInstance)` during logout.

## Suggested Discovery For UOW-2398
- Java:
  - `AutoGroupService.cancelEnter(Player player, int instanceMaskId)`
  - `AutoGroupService.destroyOrAddPlayersFromQuickEntries(AutoInstance autoInstance)`
  - `AutoGroupService.checkQueueForQuickEntries(AutoInstance autoInstance)`
  - `AutoPvpInstance.addLookingForParty(...)`
- C#:
  - `GameServerConnection.ApplyAutoGroupCancelEnterAsync(...)`
  - `AutoGroupLookingPartyRegistrationService.TryRefillQueuedQuickEntry(...)`
  - `AutoGroupInstanceLeaveRuntimeService.TryAddOpenQuickEntry(...)`
  - `GameServerConnectionAutoGroupTests`

## Focused Validation Recipe For Next UOW
- Specific behavior to validate: logout-driven start-enter cancel-enter refills a queued quick-entry party through the shared cancel-enter helper, sends ready window 4 to the quick-entry player, schedules penalty refreshes for cleanup intents, and preserves Java no-op behavior when no refill is available.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionAutoGroupTests|FullyQualifiedName~AutoGroupLookingPartyRegistrationServiceTests|FullyQualifiedName~AutoGroupInstanceLeaveRuntimeServiceTests" --no-restore`
- Focused Java/Maven command: not expected unless a targeted Java fixture is added; use Java source review by default.
- Broad-validation trigger: live connection dispatch and scheduler intents if a new connection-level test or helper change is added.

## Safe Candidate UOWs
- Add a logout-specific additional-registration cleanup test for ready-match dispatch.
- Add tests for stop-registration close behavior confirming it sends cancel windows without penalty refresh intents.
- Continue broader `PlayerLeaveWorldService.leaveWorld(...)` ordering slices outside autogroup.
