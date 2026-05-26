# Phase 6 Bind-Point Teleport Kinah Owner Outcome Integration

Date: May 26, 2026
Unit of Work: UOW-1242
Scope: Non-live integration of the scheduled Kinah owner result through callback outcome metadata.
Source of truth: Java project.

## Integration Result

C# now has `BindPointTeleportKinahOwnerCallbackOutcomeIntegrationService`, a non-live integration seam that composes:

1. in-memory owner Kinah mutation,
2. owner callback bridge,
3. owner-checked persistence operation/result/decision metadata,
4. inventory update packet intent,
5. callback composition metadata,
6. disabled or supplied send result metadata,
7. owner rollback/commit plan,
8. final callback outcome metadata.

The only state mutation is the in-memory owner Kinah mutation and possible rollback through `BindPointTeleportKinahInventoryOwnerService`. The integration does not execute SQL, send packets, dispatch from `GameServerConnection`, broadcast fanout, or move the player.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

Java scheduled callback order:

1. scheduled task calls `tryDecreaseKinah(price, DEC_KINAH_FLY)`;
2. failed Kinah decrease sends not-enough-fee and returns before cooldown, action `3`, and movement;
3. successful positive decrease mutates the Kinah item and sends `SM_INVENTORY_UPDATE_ITEM` immediately;
4. bind-point service then stores cooldown and broadcasts action `3`;
5. final movement is scheduled one second later and still checks dead/about-to-die gates.

Java `calculateTeleportationPrice` clamps the normal service-computed price to at least `1`; the C# non-positive path is retained as defensive storage parity for the underlying `Storage.decreaseKinah amount > 0` guard.

## C# Implementation

New C# artifacts:

- `Aion.GameServer.Services.BindPointTeleportKinahOwnerCallbackOutcomeIntegrationPlan`
- `Aion.GameServer.Services.BindPointTeleportKinahOwnerCallbackOutcomeIntegrationService`

## Tests Added

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `CreatePlan_NotEnoughOwnerResultStopsBeforePersistence` | Late owner failure stops before SQL, packet send, cooldown, fanout, and movement; inventory remains unchanged. | Source-derived from Java failed `tryDecreaseKinah` branch. |
| `CreatePlan_NonPositivePriceContinuesWithoutMutation` | Defensive non-positive price path continues without mutation, SQL, packet send, or rollback. | Source-derived from Java `amount > 0` guard; normal bind-point price is clamped to at least `1`. |
| `CreatePlan_PersistenceFailureRollsBackOwnerMutation` | Owner mutation is applied first, then missing/multi-row persistence failures require rollback and block later side effects. | Intentional C# persist-before-send safety gate. |
| `CreatePlan_DisabledSendRollsBackOwnerMutationAfterPersistence` | Saved persistence plus disabled send requires owner rollback before cooldown/fanout/movement. | Intentional C# disabled-send safety gate. |
| `CreatePlan_SavedAndSentCommitsOwnerMutationAndContinues` | Supplied saved persistence and sent packet result commit the owner mutation and allow cooldown/fanout/final movement metadata. | Source-derived Java order plus C# staged policy. |
| `CreatePlan_ExactPriceCommitsZeroCountKinahItem` | Exact-price success keeps a zero-count Kinah item and persists count `0`. | Source-derived from Java no-delete Kinah behavior. |

## Migration Parity Table - UOW-1242

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `Aion.GameServer.Services.BindPointTeleportKinahOwnerCallbackOutcomeIntegrationService` | Service / Callback Integration | Partial | Unit Tested | Needs Verification | Owner result now flows through persistence, packet intent, send decision, rollback, and final outcome metadata. Live dispatch and runtime side effects remain disabled. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportKinahInventoryOwnerService`; owner outcome integration | Storage / Mutation Owner | Partial | Unit Tested | Partial Parity | Missing/insufficient, defensive non-positive, positive decrement, rollback, and commit paths are covered. C# per-player lock and rollback are intentional safety boundaries. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | owner integration plus persistence/send/rollback planners | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | Java mutates and sends packet before dirty persistence; C# stages owner-checked persistence and rollback before allowing send/fanout metadata. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceOperationPlanService` through owner integration | Repository / Persistence | Partial | Unit Tested | Needs Verification | Integration consumes supplied affected-row results only. No SQL is executed; Java dirty persistence ignores affected rows. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; send decision via owner integration | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet intent and supplied/disabled send outcomes are composed. No live `SendPacketAsync` or golden-byte Java comparison. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket` | runtime callback metadata consumed by owner integration | Network Utility / Fanout | Partial | Unit Tested | Needs Verification | Integration can require cooldown/action `3` metadata before outcome readiness. Java known-list/self-first fanout is still not live or verified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | final movement metadata consumed by owner integration | Movement Service | Partial | Unit Tested | Needs Verification | Outcome carries final movement readiness only as metadata. Dead/about-to-die and live movement side effects remain outside this unit. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 1 non-live owner outcome integration service plus 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live SQL adapter, 1 live send adapter, 1 live callback fanout bridge, 1 known-list parity gate, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Integration is non-live; it consumes supplied persistence, send, and runtime callback metadata.
- The current C# composition still supplies runtime callback metadata before send decision because existing metadata contracts require composition before send decision. This is documented as a staging wrinkle; live execution must preserve Java's actual send-before-cooldown/fanout order.
- C# persist-before-send/rollback remains an intentional difference from Java dirty storage timing.
- SQL execution, packet send, `GameServerConnection` dispatch, known-list fanout, and movement remain disabled.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, packet-order, dirty-state persistence, threading, known-list fanout, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Add a send-before-runtime callback ordering adapter or design note that removes the current metadata-ordering wrinkle before any live send/fanout path is enabled. Keep it non-live and prove that inventory update send success gates cooldown/action `3` fanout and final movement metadata in Java order.

Update after UOW-1243: `BindPointTeleportKinahSendBeforeRuntimeOrderingService` now records the Java-required metadata order: persistence success, packet intent, packet send success, cooldown storage, action `3` fanout, and final movement metadata. The next safe seam is a live-adapter readiness audit before any `GameServerConnection` execution is enabled.
