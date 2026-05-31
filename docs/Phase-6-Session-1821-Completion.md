# Phase 6 Session 1821 Completion - Add Craft Consumption Persistence Planning

Date: 2026-05-31
Unit of Work: UOW-1821
Status: Complete

## Scope

Add non-live persistence-state planning for craft-consumed inventory updates/deletes. This unit maps ordered craft inventory mutation operations to Java-style dirty item states and future `InventoryDAO.store` update/delete intent without mutating live storage, writing the database, releasing object ids, sending packets, spending DP, or starting craft tasks.

## Completed Work

- Added `CraftStartInventoryPersistencePlan`.
- Added `CraftStartInventoryPersistenceOperation`.
- Added `CraftStartInventoryPersistenceStatus`.
- Added `CraftStartInventoryPersistenceOperationKind`.
- Added `CraftService.CreateStartInventoryPersistencePlan(...)`.
- Extended `CraftStartInventoryMutationOperation` so deleted operations can carry the deleted item snapshot, preserving Java `Item.setPersistentState(...)` transition evidence.
- Planned Java persistence outcomes:
  - updated stacks transition to `UpdateRequired`
  - persisted deleted stacks transition to `Deleted` and map to `InventoryDAO.deleteItems`
  - newly created stacks deleted before persistence transition to `NoAction` and produce no delete row
- Extended `CmCraftStartCompositionPlan` with `InventoryPersistencePlan`.
- Added CM_CRAFT composition ordering for inventory persistence planning after mutation planning and before packet intent planning.
- Added focused tests for:
  - ordered update/delete persistence operation descriptors
  - `NEW -> NOACTION` deleted-stack transition
  - mutation-not-planned persistence guard
  - CM_CRAFT composition carrying persistence intent

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 315 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4567 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.model.items.storage.Storage.delete`
- `com.aionemu.gameserver.model.gameobjects.Item.decreaseItemCount`
- `com.aionemu.gameserver.model.gameobjects.Item.setPersistentState`
- `com.aionemu.gameserver.dao.InventoryDAO.store`
- `com.aionemu.gameserver.dao.InventoryDAO.deleteItems`
- `com.aionemu.gameserver.dao.InventoryDAO.updateItems`

## Migration Parity Table - UOW-1821

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `Storage.decreaseItemCount` dirty item state transitions | `CraftService.CreateStartInventoryPersistencePlan` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# maps non-live ordered mutation operations to update/delete/no-action descriptors; no storage mutation or DB write occurs. |
| `Item.setPersistentState` `NEW -> NOACTION` delete behavior | `CraftStartInventoryPersistenceOperationKind.NoAction` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# preserves Java's newly created then deleted item no-op persistence outcome for planned deleted stacks. |
| `InventoryDAO.store` update/delete grouping | `CraftStartInventoryPersistencePlan.UpdatedItems` / `DeletedObjectIds` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# exposes DAO method intent for `deleteItems` and `updateItems`; exact SQL execution, transaction behavior, and ID release remain pending. |
| `CM_CRAFT.runImpl -> CraftService.startCrafting` composed start intent | `CmCraftStartCompositionPlan.InventoryPersistencePlan` | Orchestration Planner | Partial | Unit Tested | Partial Parity | C# CM_CRAFT composition now carries persistence-state intent after mutation planning; live dispatch remains disabled. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No item persistence state is written to the database.
- No transaction, commit/rollback, or Java autocommit behavior is executed.
- Java `InventoryDAO.store` releases deleted object ids after successful delete; C# only records delete intent.
- Java storage delete quest callbacks/logging are not executed.
- No live DP spend, `CraftingTask` creation, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Begin a disabled live craft-start executor facade that consumes the existing boundary, mutation, persistence, packet, DP, and task plans but proves by default that no live side effects dispatch until explicitly enabled.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - add exact Java SQL descriptor planning for craft inventory update/delete rows
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1821-Completion.md`
- `docs/Phase-6-Session-1821-Handoff.md`
