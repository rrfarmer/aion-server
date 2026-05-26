# Phase 6 Bind-Point Teleport Kinah Owner Rollback Plan

Date: May 26, 2026
Unit of Work: UOW-1235
Scope: Non-live owner/rollback contract planner for scheduled bind-point Kinah mutation.
Source of truth: Java project.

## Plan Result

C# now has `BindPointTeleportKinahOwnerRollbackPlanService`, a pure planner that records how a future inventory owner should treat the scheduled Kinah mutation snapshot. It captures original Kinah metadata, updated Kinah metadata, the mutation snapshot, and rollback snapshot. It does not mutate `Player.InventoryItems`, persist SQL, send packets, broadcast fanout, dispatch from `GameServerConnection`, or move the player.

The future owner contract is:

- not-enough Kinah: do not apply mutation and do not rollback;
- non-positive price: continue without mutation or rollback;
- mutation prepared but persistence/send result missing: apply only under an owner and be ready to rollback;
- persistence/send failure: rollback to original Kinah and stop before cooldown/action `3` fanout/movement;
- saved persistence plus sent packet: commit updated Kinah and continue.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

Java behavior:

1. `Storage.tryDecreaseKinah` mutates live storage directly on success.
2. Java sends the inventory update during `decreaseItemCount` and marks storage dirty.
3. Java does not have a callback-local persist-before-send rollback result; C# is staging this as a safety policy because the Java dirty-state lifecycle is not yet ported for this path.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahOwnerRollbackPlanStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahOwnerRollbackPlanStep`
- `Aion.GameServer.Services.BindPointTeleportKinahOwnerRollbackPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahOwnerRollbackPlanService`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_NotEnoughKinahDoesNotApplyOrRollbackMutation` | Failed Kinah branch does not apply or rollback mutation. | Source-derived from Java failed `tryDecreaseKinah`. |
| `CreatePlan_NonPositivePriceContinuesWithoutMutationOrRollback` | Non-positive price continues without mutation/rollback. | Source-derived from Java `amount > 0` guard. |
| `CreatePlan_MutationAwaitingResultsRecordsRollbackRequirement` | Prepared mutation records original and updated Kinah plus rollback requirement while waiting for results. | C# owner policy; Java dirty lifecycle differs. |
| `CreatePlan_PersistenceFailureRequiresRollback` | Missing-row/failed persistence metadata requires rollback and blocks fanout. | Intentional C# safety gate. |
| `CreatePlan_SendFailureRequiresRollback` | Failed send metadata requires rollback and blocks fanout. | Intentional C# safety gate. |
| `CreatePlan_SavedAndSentCommitsUpdatedKinahAndContinues` | Saved persistence and sent packet commit updated Kinah and allow fanout continuation. | Source-derived ordering plus C# staged policy. |

## Migration Parity Table - UOW-1235

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `Aion.GameServer.Services.BindPointTeleportKinahOwnerRollbackPlanService` | Storage / Owner Contract | Partial | Unit Tested | Needs Verification | C# records original/updated Kinah and rollback policy for a future owner. No live mutation or lock exists. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahOwnerRollbackPlanService` plus packet/persistence planners | Storage / Count Mutation | Partial | Unit Tested | Needs Verification | Java mutates/sends/marks dirty in one path. C# models rollback around staged persist/send outcomes as an intentional safety policy. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventorySendResultPlanService`; owner rollback plan | Packet Utility / Send Boundary | Partial | Unit Tested with supplied results | Needs Verification | Send failure metadata can now force rollback, but no live send occurs. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceResult`; owner rollback plan | Repository / Persistence | Partial | Unit Tested with supplied results | Needs Verification | Persistence failure metadata can now force rollback, but no SQL adapter exists. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future live owner using rollback plan plus existing metadata chain | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | Future callback owner can use this contract before cooldown/action `3`; live dispatch remains disabled. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live owner/rollback planner plus 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live inventory owner/lock, 1 live repository adapter, 1 live send adapter, and 1 live dispatch/movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Planner is non-live and does not mutate the player inventory.
- C# rollback policy is an intentional staged safety policy, not Java's dirty storage lifecycle.
- No live owner/lock, SQL adapter, packet send adapter, runtime fanout wiring, or final movement exists for this path.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, persistence, serialization, packet-order, and rollback parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a no-op live send adapter seam design or pure service that consumes packet intent and returns the supplied send-result shape without calling `SendPacketAsync`. Keep it disabled from `GameServerConnection`; do not wire live dispatch, SQL, fanout, or movement.

Update after UOW-1236: `BindPointTeleportKinahInventorySendAdapterPlanService` now provides the disabled no-op send seam and proves a supplied registry is not called. Next, prefer a repository SQL adapter design or pure owner-checked persistence contract before any live SQL or packet send implementation.
