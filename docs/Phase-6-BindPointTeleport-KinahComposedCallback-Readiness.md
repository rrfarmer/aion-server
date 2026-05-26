# Phase 6 Bind-Point Teleport Kinah Composed Callback Readiness

Date: May 26, 2026
Unit of Work: UOW-1239
Scope: Read-only readiness audit for the composed scheduled Kinah callback chain.
Source of truth: Java project.

## Readiness Result

The scheduled bind-point Kinah callback chain is now composed end-to-end as non-live metadata. It is not ready for live dispatch.

C# can now model these Java-adjacent stages without executing them:

1. scheduled Kinah success/failure branch;
2. Kinah item decrement metadata;
3. owner-checked persistence operation metadata;
4. supplied persistence result decision;
5. `SM_INVENTORY_UPDATE_ITEM` packet intent;
6. disabled send adapter result;
7. supplied send-result gate;
8. owner rollback/commit policy;
9. final callback outcome verdict for stop/rollback/continue.

This is a planning and verification scaffold only. Do not wire `GameServerConnection`, live inventory mutation, live SQL, live `SendPacketAsync`, cooldown/action `3` fanout execution, or final movement from this chain yet.

## Java Facts

Java source files reviewed:

- `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_BIND_POINT_TELEPORT.java`

Java scheduled success order:

1. `tryDecreaseKinah(price, DEC_KINAH_FLY)` mutates storage.
2. `ItemPacketService.sendItemUpdatePacket` sends `SM_INVENTORY_UPDATE_ITEM`.
3. The item is marked dirty for later `InventoryDAO.store(player)`.
4. `addCooldown(player, locId)` stores cooldown.
5. `PacketSendUtility.broadcastPacket(player, SM_BIND_POINT_TELEPORT(action=3), true)` includes the source player.
6. Final movement is scheduled and calls `TeleportService.teleportTo` only if the player remains alive and not about to die.

Current C# intentionally stages persistence before send and keeps rollback metadata around the future live owner. That ordering remains an intentional difference until a Java-like dirty storage lifecycle exists or the staged policy is explicitly accepted for live execution.

## Current C# Chain

| Stage | C# Artifact | Current Evidence | Live Ready? | Notes |
|---|---|---|---|---|
| Kinah requirement branch | `BindPointTeleportScheduledKinahPlanService` | Unit tested | Partial | Non-live scalar plan only. |
| Kinah mutation metadata | `BindPointTeleportScheduledKinahMutationPlanService` | Unit tested | No | Snapshot only; no owner/lock mutation. |
| Callback mutation carry-through | `BindPointTeleportScheduledCallbackPlanService`; `BindPointTeleportRuntimeCallbackExecutionBridgeService` | Unit tested | Partial | Carries metadata and can execute supplied cooldown/fanout metadata, but not inventory effects. |
| Persistence operation | `BindPointTeleportKinahPersistenceOperationPlanService` | Unit tested | No | Produces owner-checked SQL shape; no SQL execution. |
| Persistence decision | `BindPointTeleportKinahPersistenceDecisionBridgeService` | Unit tested | No | Consumes supplied results only. |
| Inventory update packet intent | `BindPointTeleportKinahInventoryUpdatePacketPlanService` | Unit tested | No | Builds packet intent only. |
| Disabled send seam | `BindPointTeleportKinahInventorySendAdapterPlanService` | Unit tested | No | Proves no `SendPacketAsync` call. |
| Send-result gate | `BindPointTeleportKinahInventorySendResultPlanService` | Unit tested | No | Consumes supplied sent/missing/failed result. |
| Owner rollback policy | `BindPointTeleportKinahOwnerRollbackPlanService` | Unit tested | No | Does not mutate live inventory. |
| Callback outcome | `BindPointTeleportKinahCallbackOutcomePlanService` | Unit tested | No | Final metadata verdict only. |

## Live Gates Still Required

| Gate | Required Before Live Execution | Current Blocker | Suggested Next Evidence |
|---|---|---|---|
| Inventory owner/lock | Apply updated Kinah and restore original snapshot on every failure path without racing other inventory updates. | No live owner/lock; no mutation executor. | Owner/lock design or pure owner contract with concurrency notes. |
| SQL adapter | Execute owner-checked count update and map affected rows/exceptions. | Contract only; no repository adapter. | SQL adapter readiness checklist or disabled executor seam. |
| Send adapter | Call `SendPacketAsync` only after chosen persistence policy allows it. | Disabled no-op seam only. | Opt-in live send seam after owner/SQL gates. |
| Cooldown/action `3` fanout | Insert runtime cooldown and broadcast source-included action `3` only after committed Kinah outcome. | Runtime callback bridge consumes supplied success metadata; not wired to outcome. | Outcome-to-runtime fanout bridge or readiness audit after owner/SQL design. |
| Final movement | Preserve Java final alive/about-to-die gate and `TeleportService.teleportTo` side-effect order. | Movement remains planner-only. | Separate movement readiness after inventory/fanout live gates. |
| Known-list parity | Match Java `PacketSendUtility.broadcastPacket(..., true)` sighted-player semantics. | C# registry visibility is not Java-known-list verified. | Known-list/fanout parity audit and tests. |

## Explorer Findings Integrated

Read-only owner/lock analysis found that Java `Storage.tryDecreaseKinah` is a simple non-atomic check-then-decrement path. There is no `synchronized` block or explicit lock around Kinah decrement, `kinahItem` and item counts are plain fields, non-Kinah storage uses `ConcurrentHashMap`, and deleted items use `ConcurrentLinkedQueue`. A future C# owner/lock should therefore be documented as a conservative safety boundary that preserves visible behavior rather than a literal Java lock port. It must keep zero-count Kinah items, preserve non-positive price success/no-mutation behavior, and avoid losing concurrent Kinah updates.

Read-only SQL analysis found that Java `InventoryDAO.store(player)` persists dirty items later with broad full-row update batches, ignores affected row counts, commits delete/insert/update categories separately, and sets supplied items to `UPDATED` after the store attempt. The current C# owner-checked `affectedRows == 1` contract is safer but remains an intentional difference from Java's dirty persistence timing and result semantics.

Read-only fanout analysis found that Java bind-point `PacketSendUtility.broadcastPacket(player, packet, true)` sends to self first, then iterates `player.getKnownList().forEachPlayer(...)`. This is known-list membership, not the `broadcastToSightedPlayers` helper. Current C# `BroadcastToVisiblePlayersAsync` is distance/registry based, uses `WorldVisibility.IsVisibleTo`, and does not guarantee Java self-first ordering. C# source-included fanout metadata is correct, but known-list parity remains unverified.

## Parallel Work Discovery - UOW-1239

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Composed callback readiness audit | `BindPointTeleportService`, `Storage`, `ItemPacketService`, `InventoryDAO` | docs only | Documentation Update | Yes, orchestrator-owned | Low | Shared docs need one owner; no code edits. |
| B | Live owner/lock design analysis | `Storage`, `PlayerStorage`, item mutation classes | none/read-only | Java Analysis | Yes | Medium | Independent read-only analysis. |
| C | SQL adapter readiness analysis | `InventoryDAO`; C# repository patterns | none/read-only | Java/C# Analysis | Yes | Low/Medium | Independent read-only analysis. |
| D | Known-list fanout parity analysis | `PacketSendUtility`, known-list classes | none/read-only | Java Analysis | Yes | Low/Medium | Independent read-only analysis. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Expected Result |
|---|---|---|---|---|---|
| Orchestrator | Composed callback readiness audit | Documentation Update | This readiness doc, live readiness doc, progress, handoff | Production C#, tests | Commit UOW-1239 docs. |
| Explorer A | Live owner/lock Java analysis | Java Analysis | Read-only | all writes | Owner/lock report. |
| Explorer B | SQL adapter readiness analysis | Java/C# Analysis | Read-only | all writes | SQL adapter report. |
| Explorer C | Known-list fanout analysis | Java Analysis | Read-only | all writes | Fanout parity report. |

## Migration Parity Table - UOW-1239

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | composed bind-point Kinah metadata chain including `Aion.GameServer.Services.BindPointTeleportKinahCallbackOutcomePlanService` | Service / Callback Boundary | Partial | Regression Tested | Needs Verification | Non-live outcome chain is composed. Live dispatch, scheduler callback ownership, fanout execution, and movement remain blocked. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | `BindPointTeleportScheduledKinahMutationPlanService`; `BindPointTeleportKinahOwnerRollbackPlanService` | Storage / Mutation | Partial | Unit Tested | Needs Verification | Snapshot/rollback metadata exists. Missing live owner/lock, Java dirty-state lifecycle, and concurrency verification. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | mutation/persistence/send/rollback/outcome planner chain | Storage / Count Mutation | Partial | Unit Tested | Intentional Difference | C# stages persist-before-send and rollback metadata. Java sends during mutation and persists dirty state later. |
| `com.aionemu.gameserver.dao.InventoryDAO` | `BindPointTeleportKinahPersistenceOperationPlanService`; future repository adapter | Repository / Persistence | Partial | Unit Tested | Needs Verification | Owner-checked SQL shape is modeled but not executed. Transaction/autocommit and affected-row behavior remain unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `BindPointTeleportKinahInventorySendAdapterPlanService`; `BindPointTeleportKinahInventorySendResultPlanService` | Packet Utility / Send Boundary | Partial | Unit Tested | Needs Verification | Packet intent and disabled send metadata exist. No live `SendPacketAsync` or Java runtime packet comparison. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `BindPointTeleportRuntimeFanoutService`; future callback outcome-to-fanout bridge | Network Utility / Fanout | Partial | Regression Tested | Needs Verification | Runtime fanout adapter exists for control flows, but callback outcome is not wired to action `3` fanout and Java known-list filtering remains unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService.teleportTo` | `BindPointTeleportFinalMovementPlanService`; `BindPointTeleportTeleportToSideEffectPlanService` | Movement Service | Partial | Unit Tested | Needs Verification | Final movement metadata exists only as planners; live movement side effects and packet order remain blocked. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None in UOW-1239 | Documentation/readiness audit | Java/C# source and existing tests | Summarizes the composed non-live callback chain and live gates. | Manual review plus prior regression tests only. | No new executable coverage or Java runtime comparison in this unit. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 readiness audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 live inventory owner/lock, 1 live SQL adapter, 1 live send adapter, 1 callback fanout bridge, 1 known-list parity gate, and 1 live movement path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This unit is documentation-only.
- The composed Kinah callback chain remains non-live and supplied-result based.
- C# persist-before-send/rollback policy is still an intentional difference from Java dirty storage timing and Java affected-row handling.
- Java has no explicit Kinah mutation lock; a future C# owner lock should be documented as a conservative safety boundary.
- Java bind-point fanout is self-first plus known-list membership; current C# fanout is registry/distance based and ordering is not proven self-first.
- Live SQL, live send, live owner lock, runtime callback fanout, final movement, and `GameServerConnection` dispatch remain blocked.
- Java runtime packet/storage comparison was not executed.
- Reflection behavior did not change. Date/time behavior did not change. Threading, serialization, packet-order, persistence, rollback, fanout, known-list, and movement parity remain `Needs Verification`.

## Next Recommended Unit of Work

Prefer a pure in-memory `PlayerKinahOwner` contract for applying and rolling back scheduled Kinah mutations. Keep it non-live first: define owner responsibilities, lock scope, rollback snapshots, zero-count Kinah behavior, non-positive price behavior, and callback outcome integration without executing SQL, sending packets, dispatching `GameServerConnection`, broadcasting fanout, or moving the player.

Safe follow-up candidates:

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Pure in-memory Kinah owner contract | new service/test pair | Medium | Best next executable gate before live SQL/send. |
| B | Bind-point SQL repository adapter seam | new repository/test pair | Medium | Use only after owner contract or keep disabled; Java affected-row behavior differs. |
| C | Registry/visibility characterization tests | existing/new fanout tests only | Low/Medium | Documents current C# distance approximation before true known-list parity. |
| D | Known-list-backed fanout design | docs only | Low | Do before replacing registry/distance fanout. |
