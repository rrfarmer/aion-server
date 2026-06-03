# Phase 6 Session 2422 Completion

Status: Phase 6 continues; UOW-2422 added a disabled, non-live legion warehouse persistence contract plan. The plan distinguishes Java logout warehouse save parameters from periodic warehouse save parameters and keeps repository wiring disabled.

## Scope

- UOW: UOW-2422 disabled legion warehouse persistence contract plan.
- Added a small planner that captures Java `InventoryDAO.store(allItems, playerId, accountId, legionId)` modes for legion warehouse persistence.
- Added focused tests for logout mode, periodic-save mode, missing-contract blockers, and all-prerequisite readiness while still non-live.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `LegionWhUpdate(Player)` combines `getItemsWithKinah()` and deleted items.
  - Calls `InventoryDAO.store(allItems, player.getObjectId(), player.getAccount().getId(), legion.getLegionId())`.
  - Calls `ItemStoneListDAO.save(allItems)` after inventory persistence.
  - Catches and logs `Exception`.
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`
  - `LegionWarehouseSaveTask.run` combines the same current/deleted item set.
  - Calls `InventoryDAO.store(allItems, null, null, legion.getLegionId())`.
  - Calls `ItemStoneListDAO.save(allItems)` after inventory persistence.
  - Catches and logs `Exception`.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `store(List<Item>, Integer playerId, Integer accountId, Integer legionId)` partitions changed/new/deleted items, opens one connection, disables autocommit, deletes/inserts/updates, marks all items updated, and returns combined operation success.
  - `getItemOwnerId` uses `legionId` for legion warehouse items when present, otherwise falls back to `playerId`.
- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`
  - `save(List<Item>)` extracts item stones by category and stores them after item persistence.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/LegionWarehousePersistenceContractPlanService.cs`
  - New disabled contract planner added in this unit.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerLegionLogoutCleanupReadinessPlanService.cs`
  - Adjacent readiness planner from UOW-2421; tests were included as focused adjacent coverage.

## Implemented

- Added `LegionWarehousePersistenceContractPlanService`.
- Added `LegionWarehousePersistenceMode.Logout` and `PeriodicSave`.
- Added `LegionWarehouseJavaStoreCallDescriptor` to preserve the Java caller and argument sources.
- Added disabled repository method descriptor text for a future `SaveLegionWarehouseItemsAsync(...)` contract.
- Added `LegionWarehousePersistenceContractPlanServiceTests`.
- No repository interface, SQL implementation, logout hook, periodic task, item mutation, or packet fanout was added.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreateDisabledPlan_LogoutMode_RecordsPlayerAccountAndLegionParameters` | Unit | Java `LegionService.LegionWhUpdate` source review | Logout mode records player object id, account id, and legion id argument sources. | Focused C# assertion based on Java source review. | No live persistence. |
| `CreateDisabledPlan_PeriodicMode_RecordsNullPlayerAndAccountParameters` | Unit | Java `PeriodicSaveService.LegionWarehouseSaveTask.run` source review | Periodic mode records null player/account ids and legion id source. | Focused C# assertion based on Java source review. | No periodic task. |
| `CreateDisabledPlan_WithMissingContracts_KeepsRepositoryWiringDisabled` | Unit | Java inventory plus item-stone persistence source review | Missing item-stone/repository prerequisites keep repository wiring disabled. | Focused C# assertion. | No SQL contract added. |
| `CreateDisabledPlan_AllPrerequisitesReady_MarksReadyButDoesNotAddRepositoryMethod` | Unit | Java persistence source review | All prerequisites can mark future repository readiness while `DidAddRepositoryMethod` and `IsLive` remain false. | Focused C# assertion. | Still non-live. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService` | Service / Contract Planner | Partial | Unit Tested | Needs Verification | C# records Java logout parameter mode and item-stone follow-up but does not persist items. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService` | Scheduled Service / Contract Planner | Partial | Unit Tested | Needs Verification | C# records Java periodic parameter mode with null player/account ids but has no live periodic task. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(List<Item>, Integer, Integer, Integer)` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService.FutureRepositoryMethod` | Repository / Contract Planner | Partial | Unit Tested | Needs Verification | Future method signature is descriptor-only. No repository method, SQL, transaction behavior, persistent-state mutation, or object-id release behavior was implemented. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.save(List<Item>)` | `Aion.GameServer.Services.LegionWarehousePersistenceContractCriterion.ItemStonePersistenceContractAvailable` | Repository / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the required follow-up persistence; no C# item-stone contract exists. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` legion warehouse save slice | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService` plus `LegionWarehousePersistenceContractPlanService` | Logout Lifecycle / Planner | Partial | Unit Tested | Partial Parity | Adjacent readiness tests still pass. `LeaveWorldAsync` remains unwired. |

## Validation Decision

- Changed surface: one non-live C# contract planner plus focused tests.
- Specific behavior/contract: disabled contract plan distinguishes Java logout warehouse save parameters from periodic warehouse save parameters and keeps live repository wiring disabled until item and item-stone contracts exist.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehousePersistenceContractPlanServiceTests|FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Result:
  - Passed: 8
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled affected project/dependencies and covered the new planner plus adjacent readiness gate.
- Why this scope is sufficient:
  - The unit changed only standalone non-live planner metadata and tests, with no repository interface method, SQL execution, live logout hook, packet primitive, scheduler, or shared runtime mutation.

## Known Remaining Gaps

- No live legion warehouse repository method.
- No C# inventory SQL for legion warehouse item persistence.
- No C# item-stone repository contract for warehouse items.
- No persistent-state mutation/object-id release behavior like Java `InventoryDAO.store`.
- No live logout or periodic save hook.
- No Java/C# runtime comparison.

## Summary Metrics

- Java artifacts reviewed: 4.
- C# artifacts reviewed: 2.
- Production files changed: 1.
- Test files changed: 1.
- New focused tests: 4.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification: 5.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
