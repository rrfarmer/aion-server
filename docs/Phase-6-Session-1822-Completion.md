# Phase 6 Session 1822 Completion - Add Disabled Craft Start Executor Facade

Date: 2026-05-31
Unit of Work: UOW-1822
Status: Complete

## Scope

Add a disabled live craft-start executor facade that consumes the existing CM_CRAFT composition, side-effect boundary, mutation, persistence, packet, DP, and task plans. This unit records the Java live side-effect boundaries and proves that no C# live side effects dispatch by default.

## Completed Work

- Added `CraftStartLiveExecutorFacadePlan`.
- Added `CraftStartLiveExecutorOperation`.
- Added `CraftStartLiveExecutorFacadeStatus`.
- Added `CraftStartLiveExecutorOperationKind`.
- Added `CraftStartLiveExecutorOperationStatus`.
- Added `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan(...)`.
- Recorded disabled Java side-effect operation order:
  - apply inventory mutation
  - mark inventory persistence state
  - send inventory packets
  - optionally spend recipe DP
  - create `CraftingTask`
  - start `CraftingTask`
- Added would/did flags for each live boundary so the disabled default can prove no live mutation, DB write, packet send, DP spend, task creation, or task start occurred.
- Added focused tests for:
  - ready composition recording all disabled success boundaries
  - no-DP recipes omitting the DP spend boundary
  - not-ready composition stopping before success side effects

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 318 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4570 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.dao.InventoryDAO.store`
- `com.aionemu.gameserver.skillengine.task.CraftingTask`

## Migration Parity Table - UOW-1822

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` live success side-effect sequence | `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan` | Execution Facade | Partial | Unit Tested | Partial Parity | C# records the Java success boundaries but marks every operation `NotAttemptedDisabled`; no live side effect dispatch occurs. |
| `Storage.decreaseItemCount` inventory mutation and packet side effects | `CraftStartLiveExecutorOperationKind.ApplyInventoryMutation` / `SendInventoryPackets` | Execution Facade | Partial | Unit Tested | Partial Parity | C# records would-mutate and would-send boundaries from existing plans; live mutation and packet sending remain disabled. |
| `InventoryDAO.store` delayed persistence boundary | `CraftStartLiveExecutorOperationKind.MarkInventoryPersistenceState` | Execution Facade | Partial | Unit Tested | Partial Parity | C# records persistence-state/write boundary intent; no DAO call, transaction, or object-id release occurs. |
| `CraftService.startCrafting` DP and task boundaries | `SpendRecipeDp`, `CreateCraftingTask`, `StartCraftingTask` facade operations | Execution Facade | Partial | Unit Tested | Partial Parity | C# records optional DP spend and task lifecycle boundaries; no DP mutation, task allocation, interval mutation, or scheduler start occurs. |

## Risks / Gaps

- The executor facade is disabled and does not execute live side effects.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No item persistence is written to the database.
- No DP spend is executed by the facade.
- No live `CraftingTask` is created or started.
- Java transaction behavior and object-id release after successful delete remain pending.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add exact Java SQL descriptor planning for craft inventory update/delete rows, including `InventoryDAO.DELETE_QUERY`, `InventoryDAO.UPDATE_QUERY`, delete-before-update grouping, and the known gap around object-id release after successful delete.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - add a disabled inventory packet send adapter for craft-start packet intent
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1822-Completion.md`
- `docs/Phase-6-Session-1822-Handoff.md`
