# Phase 6ABF Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1222
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, teleport side-effect metadata, live-adapter readiness docs, concrete failure system-message helpers, non-live handler composition, runtime owner/control/fanout bridges, metadata-only action `1` scheduling, callback-side cooldown/action `3` fanout for supplied Kinah-success metadata, and a scheduled Kinah live-boundary audit. Full `GameServerConnection` dispatch, live Kinah mutation, inventory persistence, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1222 added `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveBoundary-Audit.md`, a source-backed audit of the Java scheduled callback payment boundary. It confirms Java `tryDecreaseKinah(price, DEC_KINAH_FLY)` mutates storage, sends `SM_INVENTORY_UPDATE_ITEM` with mask `0x4B`, and marks storage update-required before cooldown/action `3` fanout. C# should add packet-level `DEC_KINAH_FLY` readiness before any live bind-point inventory mutation.

Files changed:

- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveBoundary-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ABF-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 91 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1222

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled Kinah callback branch | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService`; `BindPointTeleportRuntimeCallbackExecutionBridgeService` | Service / Callback Boundary | Partial | Unit Tested | Needs Verification | C# records and consumes scheduled Kinah success/failure metadata, but actual live `tryDecreaseKinah`, inventory packet send, persistence, and fee-failure send remain disabled. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | future C# player inventory owner or adapter | Storage / Inventory Owner | Not Started | No Tests | Unknown | Java delegates player inventory Kinah decrements to `Storage.tryDecreaseKinah`. C# lacks a shared audited equivalent for bind-point scheduled callbacks. |
| `com.aionemu.gameserver.model.items.storage.Storage` | future C# live Kinah mutation boundary | Storage / Mutation | Not Started | No Tests | Unknown | Java checks current Kinah, decrements item count, never deletes the Kinah item at zero, sends item update when actor exists, and marks storage update-required. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType.DEC_KINAH_FLY` | `Aion.GameServer.Services.BindPointTeleportScheduledKinahPlanService.DecKinahFlyUpdateTypeMask`; future `SmInventoryUpdateItem.DecreaseKinahFly` | Enum / Packet Mask | Partial | Unit Tested | Needs Verification | Planner records mask `0x4B`; `SmInventoryUpdateItem` does not yet expose a named fly/teleport Kinah constant. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | future C# send boundary using `SmInventoryUpdateItem` | Packet Utility / Send Boundary | Partial | Manual Only | Needs Verification | C# packet type exists and accepts an update mask, but no bind-point live send path emits it in Java order. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested in other flows | Needs Verification | Normal update path writes item id, client name, full item blob, and update mask. No Java runtime byte capture for `DEC_KINAH_FLY`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.CannotMoveToAirportNotEnoughFee` | Packet / Failure Message | Partial | Unit Tested | Needs Verification | Named helper exists, but scheduled callback live failure send is not wired. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1222 | Documentation-only audit of the scheduled Kinah live mutation boundary. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 live-boundary audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 2 grouped rows plus live persistence/threading policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live bind-point scheduled Kinah mutation is still blocked.
- Failure-message send for scheduled Kinah failure remains unwired.
- Inventory update packet order and persistence behavior are not live.
- Java storage persistence-state behavior does not yet have a shared C# equivalent for this path.
- Threading/concurrency behavior for scheduled inventory mutation remains unverified.
- Runtime fanout still uses C# visible-player registry approximation instead of Java persistent known-list membership.
- Final movement remains metadata-only.
- Reflection behavior did not change. Serialization is source-derived, not Java-runtime verified. Threading, date/time, persistence, fanout ordering, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add packet-level `DEC_KINAH_FLY` readiness.
- Scope:
  - Add `SmInventoryUpdateItem.DecreaseKinahFly = 0x4B`.
  - Add a focused packet/unit test that serializes a Kinah inventory update and asserts the trailing mask is `0x4B`.
  - Update the scheduled Kinah audit/readiness/progress/handoff docs and parity table.
- Scope guard:
  - Do not mutate live inventory.
  - Do not add repository writes.
  - Do not wire `GameServerConnection`.
  - Do not call movement services.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Packet-level `DEC_KINAH_FLY` constant/test | `SmInventoryUpdateItem.cs`, packet tests | Low | Best next step; contained and executable. |
| B | Shared Kinah mutation owner design audit | new doc only | Medium | Useful after packet readiness, before live mutation. |
| C | Known-list parity audit for C# visible registry approximation | new doc only | Low/Medium | Helps unblock future live fanout claims. |
| D | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |

### Do Not Parallelize

- Live Kinah mutation and `GameServerConnection` dispatch.
- Kinah mutation and final movement.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_INVENTORY_UPDATE_ITEM.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeCallbackExecutionBridgeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- Latest completed commits:
  - `7c79dcb87 [Phase 6][UOW-1221] Add bind point teleport callback execution bridge`
  - next commit should be `[Phase 6][UOW-1222] Add bind point teleport scheduled Kinah boundary audit`

Keep live bind-point behavior disabled until packet-level Kinah update readiness, live inventory mutation/persistence, final movement packet ordering, and persistent known-list behavior each have focused parity slices.
