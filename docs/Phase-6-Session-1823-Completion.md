# Phase 6 Session 1823 Completion - Add Craft Inventory SQL Descriptor Planning

Date: 2026-05-31
Unit of Work: UOW-1823
Status: Complete

## Scope

Add exact Java SQL descriptor planning for craft-start inventory consumption persistence. This unit records Java `InventoryDAO` update/delete SQL and DAO grouping only; it does not execute live database writes, release object ids, mutate inventory, send packets, spend DP, or start crafting tasks.

## Completed Work

- Added exact Java `InventoryDAO.DELETE_QUERY` and `InventoryDAO.UPDATE_QUERY` constants to `CraftStartInventoryPersistencePlan`.
- Added `CraftStartInventoryPersistenceSqlDescriptor`.
- Added `CraftStartInventoryPersistenceSqlOperationKind`.
- Added `SqlDescriptors` to `CraftStartInventoryPersistencePlan`.
- Planned SQL descriptors in Java `InventoryDAO.store` grouping order:
  - delete rows first through `InventoryDAO.deleteItems`
  - update rows second through `InventoryDAO.updateItems`
- Preserved existing ordered mutation operations separately from DAO batch grouping.
- Added object-id release intent fields:
  - `ObjectIdsPendingRelease`
  - `WouldReleaseObjectIdsAfterSuccessfulDelete`
  - `DidReleaseObjectIds`
- Kept descriptor execution disabled: `WouldExecuteSql` is true for persisted update/delete rows, while `DidExecuteSql` remains false.
- Confirmed Java `NEW -> NOACTION` deleted stacks produce no SQL descriptor and no object-id release intent.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 318 tests.
- First broad run timed out before returning a result.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4570 tests on rerun with a longer timeout.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.dao.InventoryDAO.DELETE_QUERY`
- `com.aionemu.gameserver.dao.InventoryDAO.UPDATE_QUERY`
- `com.aionemu.gameserver.dao.InventoryDAO.store`
- `com.aionemu.gameserver.dao.InventoryDAO.deleteItems`
- `com.aionemu.gameserver.dao.InventoryDAO.updateItems`

## Migration Parity Table - UOW-1823

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `InventoryDAO.DELETE_QUERY` | `CraftStartInventoryPersistencePlan.JavaInventoryDeleteSql` | SQL Descriptor | Partial | Unit Tested | Partial Parity | C# records the exact Java delete SQL for persisted deleted craft-consumption rows; no DB execution occurs. |
| `InventoryDAO.UPDATE_QUERY` | `CraftStartInventoryPersistencePlan.JavaInventoryUpdateSql` | SQL Descriptor | Partial | Unit Tested | Partial Parity | C# records the exact Java update SQL for updated craft-consumption rows; full parameter binding remains non-live. |
| `InventoryDAO.store` delete-before-update grouping | `CraftStartInventoryPersistencePlan.SqlDescriptors` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# groups delete descriptors before update descriptors like Java `store`; transaction, commit/rollback, and successful write results remain pending. |
| `InventoryDAO.store` object-id release after successful delete | `ObjectIdsPendingRelease` / `WouldReleaseObjectIdsAfterSuccessfulDelete` / `DidReleaseObjectIds` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# records the release boundary but does not call `IDFactory.releaseObjectIds`; live release depends on future successful delete execution. |

## Risks / Gaps

- SQL descriptors are non-live and do not execute database writes.
- No transaction, commit/rollback, or Java autocommit behavior is executed.
- Java `insertItems` remains outside this craft-consumption scope.
- Object ids are not released; the C# plan only records pending release intent.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No DP spend, live `CraftingTask` creation, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a disabled inventory packet send adapter for craft-start packet intent so the existing packet plan can be consumed without dispatching packets by default.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - begin a live-disabled craft inventory persistence adapter around the new SQL descriptors
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1823-Completion.md`
- `docs/Phase-6-Session-1823-Handoff.md`
