# Phase 6ABH Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1224
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, teleport side-effect metadata, live-adapter readiness docs, concrete failure system-message helpers, non-live handler composition, runtime owner/control/fanout bridges, metadata-only action `1` scheduling, callback-side cooldown/action `3` fanout for supplied Kinah-success metadata, a scheduled Kinah live-boundary audit, packet-level `DEC_KINAH_FLY` readiness, and a Kinah mutation owner design audit. Full `GameServerConnection` dispatch, live Kinah mutation, inventory persistence, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1224 added `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md`, which defines the future live owner/boundary needed before scheduled bind-point callbacks can mutate Kinah. It confirms C# should not copy handler-local repository shortcuts into the callback path; the next code unit should be a non-live mutation planner.

Files changed:

- `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveBoundary-Audit.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ABH-Completion.md`

## Validation

- Documentation-only unit; no executable code changed.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 92 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1224

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | future C# Kinah mutation owner/boundary | Storage / Mutation | Not Started | No Tests | Unknown | Design audit pins required behavior: missing/insufficient fail, exact Kinah succeeds to zero, zero Kinah item is not deleted, packet mask is `DEC_KINAH_FLY`, and storage persistence must be addressed. |
| `com.aionemu.gameserver.model.items.storage.PlayerStorage` | `Aion.GameServer.Model.GameObjects.Player.InventoryItems` plus future owner | Storage / Inventory Owner | Partial | Manual Only | Needs Verification | C# currently stores inventory as a replaceable read-only list without a Java-like storage owner or lock. Future callback mutation needs explicit synchronization. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket` / `sendItemUpdatePacket` | future live boundary using `SmInventoryUpdateItem.DecreaseKinahFly` | Packet Utility / Send Boundary | Partial | Unit Tested for packet mask | Needs Verification | Packet mask prerequisite exists, but live send timing remains unimplemented. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` | Packet / Serialization | Partial | Unit Tested | Needs Verification | C# can emit the trailing `0x4B` mask; full Java runtime packet comparison is still missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | future C# live scheduled Kinah mutation + existing runtime callback bridge | Service / Callback Boundary | Partial | Unit Tested for surrounding metadata | Needs Verification | Design audit keeps live mutation disabled until owner, persistence, packet send, and failure-message policy are implemented. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| None in UOW-1224 | Documentation-only design audit for the future live Kinah mutation owner. | Manual Java/C# source inspection only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 0 live artifacts; 1 design audit completed
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 storage mutation owner plus persistence/threading policy
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live scheduled Kinah mutation remains blocked.
- No shared C# inventory owner/lock exists yet.
- Immediate persistence vs Java dirty-state persistence remains an unresolved policy choice.
- Failure-message send and rollback behavior are not implemented.
- Java runtime packet comparison for the full Kinah update remains missing.
- Runtime fanout still uses C# visible-player registry approximation instead of Java persistent known-list membership.
- Final movement remains metadata-only.

## Next Work Options

### Recommended Sequential Task

- Task: Implement a non-live `BindPointTeleportScheduledKinahMutationPlanService`.
- Scope:
  - Consume `Player.InventoryItems` and required price.
  - Return missing-Kinah, insufficient-Kinah, and decrement-ready statuses.
  - Preserve unrelated inventory and keep zero-count Kinah item.
  - Carry updated Kinah item metadata and `SmInventoryUpdateItem.DecreaseKinahFly` packet intent.
  - Add unit tests for missing Kinah, insufficient Kinah, exact Kinah to zero, positive decrement, and unrelated item preservation.
- Scope guard:
  - No repository writes.
  - No live packet sends.
  - No `GameServerConnection` dispatch.
  - No movement.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live scheduled Kinah mutation planner | new service/test pair | Medium | Best next step; isolated from dispatch and persistence. |
| B | Known-list parity audit for C# visible registry approximation | new doc only | Low/Medium | Helps unblock future live fanout claims. |
| C | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |
| D | `SmInventoryUpdateItem` Java runtime capture design note | new doc only | Low | Useful if Java packet observer tooling becomes available. |

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
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
  - `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledKinahPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmInventoryUpdateItem.cs`
  - `docs/Phase-6-BindPointTeleport-KinahMutationOwner-Design.md`
- Latest completed commits:
  - `e465f1da1 [Phase 6][UOW-1223] Add bind point teleport fly Kinah packet mask`
  - next commit should be `[Phase 6][UOW-1224] Add bind point teleport Kinah mutation owner design`

Keep live bind-point behavior disabled until live inventory mutation/persistence, final movement packet ordering, and persistent known-list behavior each have focused parity slices.
