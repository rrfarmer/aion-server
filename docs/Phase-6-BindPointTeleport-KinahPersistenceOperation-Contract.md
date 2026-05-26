# Phase 6 Bind-Point Teleport Kinah Persistence Operation Contract

Date: May 26, 2026
Unit of Work: UOW-1237
Scope: Pure owner-checked persistence operation contract for scheduled bind-point Kinah count updates.
Source of truth: Java project.

## Contract Result

C# now has `BindPointTeleportKinahPersistenceOperationPlanService`, a pure persistence operation contract for the scheduled bind-point Kinah update. It converts `BindPointTeleportScheduledKinahMutationPlan` into an owner-checked count update shape and maps supplied execution facts into `BindPointTeleportKinahPersistenceResult`.

The service does not open a database connection, execute SQL, mutate inventory, send packets, broadcast cooldown fanout, dispatch from `GameServerConnection`, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`

Java behavior:

1. `Storage.tryDecreaseKinah` mutates the Kinah item and sends `SM_INVENTORY_UPDATE_ITEM` during the storage mutation.
2. Java marks the item dirty and persists it later through `InventoryDAO.store(player)`.
3. Java does not delete Kinah when the count reaches zero.
4. `InventoryDAO.updateItems` persists broader dirty item rows than the narrow C# count-update contract.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceOperationStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceOperationParameter`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceOperationPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahPersistenceOperationPlanService`

The planned SQL shape is:

```sql
UPDATE inventory SET item_count = ? WHERE item_unique_id = ? AND item_owner = ?
```

Result mapping:

- affected rows `1`: `Saved`, no rollback;
- affected rows other than `1`: `MissingRow`, rollback required;
- exception supplied: `Failed`, rollback required;
- not-enough Kinah or no-mutation paths: no SQL operation is created.

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_NotEnoughKinahDoesNotCreateSql` | Failed scheduled Kinah branch creates no SQL. | Source-derived from Java failed `tryDecreaseKinah`. |
| `CreatePlan_NonPositivePriceDoesNotCreateSql` | Non-positive price creates no SQL. | Source-derived from Java `amount > 0` guard. |
| `CreatePlan_DecrementReadyCreatesOwnerCheckedCountUpdate` | Positive decrements create owner-checked SQL parameters and never delete zero-count Kinah. | C# narrowed contract from Java dirty item update. |
| `CreateResult_OneAffectedRowSavesWithoutRollback` | One affected row maps to `Saved`. | C# supplied execution mapping. |
| `CreateResult_NonSingleAffectedRowsRequireMissingRowRollback` | Zero or multiple affected rows map to rollback-required `MissingRow`. | Intentional C# safety gate. |
| `CreateResult_ExceptionRequiresFailedRollback` | Exceptions map to rollback-required `Failed`. | Intentional C# safety gate. |

## Migration Parity Table - UOW-1237

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.InventoryDAO` | `Aion.GameServer.Services.BindPointTeleportKinahPersistenceOperationPlanService` | Repository / Persistence Contract | Partial | Unit Tested | Needs Verification | C# now has a pure owner-checked count-update contract and supplied result mapper. No SQL is executed. Java full-row dirty persistence is broader. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; persistence operation contract | Storage / Mutation | Partial | Unit Tested | Needs Verification | Mutation metadata feeds the persistence operation contract. No live storage owner/lock exists. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahPersistenceOperationPlanService`; packet/send planners | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# deliberately stages persist-before-send around rollback policy; Java sends during storage mutation and persists later. Live parity remains unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventorySendAdapterPlanService`; persistence operation contract | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Persistence result still gates the disabled send seam. No live packet send occurs. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | bind-point Kinah metadata/persistence/send planner chain | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Callback can now model mutation, owner-checked persistence, disabled send, and rollback decisions as metadata. Live dispatch remains disabled. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 pure persistence operation contract plus 8 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live inventory owner/lock, 1 live `SendPacketAsync` adapter, 1 live dispatch path, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- No SQL is executed; row counts and exceptions are supplied.
- C# persist-before-send remains an intentional staged safety policy and does not match Java dirty storage timing.
- No live inventory lock/owner applies or rolls back the in-memory mutation.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading behavior did not change. Serialization, packet order, persistence, rollback, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a non-live composition bridge that consumes mutation, persistence operation/result, disabled send, and owner rollback metadata into one scheduled Kinah callback outcome. Keep it pure and disabled; do not execute SQL, send packets, wire `GameServerConnection`, broadcast fanout, or move the player.
