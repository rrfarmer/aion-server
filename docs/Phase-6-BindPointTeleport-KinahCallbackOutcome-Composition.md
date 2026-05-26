# Phase 6 Bind-Point Teleport Kinah Callback Outcome Composition

Date: May 26, 2026
Unit of Work: UOW-1238
Scope: Pure scheduled Kinah callback outcome composer for bind-point teleport.
Source of truth: Java project.

## Composition Result

C# now has `BindPointTeleportKinahCallbackOutcomePlanService`, a pure metadata composer that joins scheduled Kinah mutation metadata, owner-checked persistence operation/result metadata, disabled inventory-send metadata, and owner rollback metadata into one callback outcome.

The composer does not mutate inventory, execute SQL, send packets, broadcast fanout, dispatch from `GameServerConnection`, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

Java scheduled callback success order:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)`.
2. Send `SM_INVENTORY_UPDATE_ITEM` during storage mutation.
3. Store dirty inventory later through `InventoryDAO`.
4. Add cooldown.
5. Broadcast `SM_BIND_POINT_TELEPORT(action=3)`.
6. Schedule final movement.

C# intentionally stages persistence and send metadata before allowing cooldown/action `3` fanout.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomeStatus`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomeStep`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomePlan`
- `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomePlanService`

Outcome statuses:

- `StoppedNotEnoughKinah`
- `ContinueWithoutMutation`
- `AwaitingPersistenceResult`
- `RollbackAfterPersistenceFailure`
- `AwaitingSendResult`
- `RollbackAfterSendFailure`
- `ReadyForCooldownFanout`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_NotEnoughKinahStopsBeforePersistence` | Failed Kinah branch stops before persistence/send/fanout. | Source-derived from Java failed `tryDecreaseKinah`. |
| `CreatePlan_NonPositivePriceContinuesWithoutMutation` | No-mutation branch can continue without SQL or packet send. | Source-derived from Java `amount > 0` guard. |
| `CreatePlan_MissingPersistenceResultAwaitsBeforeSend` | Missing persistence result blocks packet send and records rollback requirement. | C# staging guard. |
| `CreatePlan_PersistenceFailureRequiresRollback` | Missing-row/multi-row persistence results require rollback. | Intentional C# safety gate. |
| `CreatePlan_DisabledSendRequiresRollbackAfterPersistence` | Disabled no-send result blocks cooldown/action `3` fanout and requires rollback. | Intentional C# safety gate. |
| `CreatePlan_SavedAndSentCommitsAndContinues` | Supplied saved persistence and sent packet allow commit and cooldown/fanout metadata. | Source-derived order plus C# staged policy. |

## Migration Parity Table - UOW-1238

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomePlanService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | C# now has a pure stop/rollback/continue verdict for scheduled Kinah metadata. Live callback dispatch remains disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; callback outcome composer | Storage / Mutation | Partial | Unit Tested | Needs Verification | Mutation success/failure metadata feeds the outcome. No live storage owner/lock exists. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | persistence/send/rollback/outcome composer chain | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# still stages persist-before-send and rollback; Java sends during mutation and persists later. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceOperationPlanService`; callback outcome composer | Repository / Persistence | Partial | Unit Tested | Needs Verification | Outcome consumes supplied persistence results; no SQL executes. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventorySendAdapterPlanService`; callback outcome composer | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Disabled send result forces rollback; supplied sent result can continue. No live `SendPacketAsync`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` action `3` | existing cooldown fanout metadata; callback outcome composer | Packet / Fanout Boundary | Partial | Regression Tested | Needs Verification | Outcome can allow cooldown fanout metadata only after supplied success; live fanout remains disabled for this path. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 pure callback outcome composer plus 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live inventory owner/lock, 1 live `SendPacketAsync` adapter, 1 live dispatch path, 1 live fanout path, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Composer is non-live and consumes supplied metadata only.
- C# persist-before-send/rollback policy remains an intentional difference from Java dirty storage timing.
- No live SQL, live send, live owner lock, runtime fanout execution, final movement, or `GameServerConnection` dispatch exists for this path.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, serialization, packet-order, persistence, rollback, fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a read-only readiness audit for the now-composed scheduled Kinah callback chain and pick the next executable gate: either a real live owner/lock design for applying and rolling back the mutation, or a narrow SQL adapter readiness checklist. Keep live dispatch, live SQL execution, packet sends, fanout, and movement disabled.
