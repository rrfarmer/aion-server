# Phase 6 Session 1825 Completion - Add Disabled Craft Inventory Persistence Adapter

Date: 2026-05-31
Unit of Work: UOW-1825
Status: Complete

## Scope

Add a disabled persistence adapter around craft-start inventory SQL descriptors. This unit records Java `InventoryDAO.store` execution boundaries without opening a database connection or executing SQL.

## Completed Work

- Added `CraftStartInventoryPersistenceAdapterPlanService.CreateDisabledPlan(...)`.
- Added `CraftStartInventoryPersistenceAdapterPlan`.
- Added `CraftStartInventoryPersistenceAdapterOperation`.
- Added `CraftStartInventoryPersistenceAdapterStatus`.
- Recorded Java persistence boundaries:
  - open connection
  - begin transaction / autocommit disabled
  - execute SQL descriptors
  - commit batches
  - release object ids after successful delete
- Preserved existing SQL descriptor grouping.
- Added disabled execution flags and counts.
- Added missing, not-ready, and no-SQL guard behavior.
- Kept all execution non-live.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 321 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4573 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.dao.InventoryDAO.store`
- `com.aionemu.gameserver.dao.InventoryDAO.deleteItems`
- `com.aionemu.gameserver.dao.InventoryDAO.updateItems`
- `com.aionemu.gameserver.utils.idfactory.IDFactory.releaseObjectIds`

## Migration Parity Table - UOW-1825

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `InventoryDAO.store` connection and transaction boundary | `CraftStartInventoryPersistenceAdapterPlanService.CreateDisabledPlan` | Persistence Adapter | Partial | Unit Tested | Partial Parity | C# records would-open-connection and would-begin-transaction flags; no connection is opened and no transaction is started. |
| `InventoryDAO.deleteItems` / `updateItems` SQL execution boundary | `CraftStartInventoryPersistenceAdapterOperation` | Persistence Adapter | Partial | Unit Tested | Partial Parity | C# consumes delete/update SQL descriptors and records would-execute SQL; no SQL is executed. |
| `InventoryDAO.store` post-delete object-id release | `WouldReleaseObjectIdsAfterSuccessfulDelete` / `DidReleaseObjectIds` | Persistence Adapter | Partial | Unit Tested | Partial Parity | C# records Java's release boundary after successful delete; `IDFactory.releaseObjectIds` remains uncalled. |

## Risks / Gaps

- The persistence adapter is disabled and does not execute database writes.
- No connection, transaction, batch commit, rollback, or Java autocommit behavior is executed.
- Object ids are not released; the C# plan only records release intent.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No DP spend, live `CraftingTask` creation, scheduler startup, or craft completion is wired.
- Java storage delete quest callbacks/logging are not executed.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Integrate the disabled packet-send and persistence adapters into `CraftStartLiveExecutorFacadePlanService` so the facade consumes concrete adapter plans rather than only high-level boundary flags.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - begin live-enabled persistence adapter design only after explicit transaction/rollback scope
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1825-Completion.md`
- `docs/Phase-6-Session-1825-Handoff.md`
