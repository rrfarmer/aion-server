# Phase 6ACD Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1246
Status: Phase 6 continues; bind-point scheduled Kinah now has disabled/opt-in SQL persistence and inventory packet send adapter seams. `GameServerConnection` dispatch, known-list fanout parity, final movement, and Java runtime packet validation remain disabled or unverified.

## Session Summary

UOW-1246 added a disabled/opt-in inventory packet send adapter for scheduled bind-point Kinah. It consumes the existing packet intent, can call `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled, and maps the registry outcome into the existing send-result shape used by ordering and rollback gates.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahInventorySendAdapterPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKinahInventorySendAdapterPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KinahInventoryLiveSendAdapter-Plan.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-KinahSendBeforeRuntimeOrdering.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACD-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKinahInventorySendAdapterPlanServiceTests" --nologo` passed 10 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 186 tests.
- No Java runtime comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1246

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUpdatePacket` | `Aion.GameServer.Services.BindPointTeleportKinahInventorySendAdapterService`; `BindPointTeleportKinahInventorySendAdapterPlanService` | Packet Utility / Send Adapter | Partial | Unit Tested | Needs Verification | Opt-in adapter can call registry send for prepared `SmInventoryUpdateItem`; default remains disabled. No Java packet capture or live client comparison. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `BindPointTeleportKinahInventoryUpdatePacketPlanService`; `BindPointTeleportKinahInventorySendAdapterService` | Storage / Count Mutation Send Boundary | Partial | Unit Tested | Intentional Difference | Java sends during mutation and then marks storage dirty. C# uses staged packet intent plus explicit opt-in send result after persistence policy. Exact zero-count Kinah update remains packet intent, not delete. |
| `com.aionemu.gameserver.model.items.storage.Storage.tryDecreaseKinah` | owner mutation/persistence/send adapter chain | Storage / Kinah Mutation | Partial | Unit Tested | Needs Verification | C# chain can now produce opt-in send results, but live owner mutation plus dispatch are still not wired. Threading/locking remains in owner service, not this adapter. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled callback | `BindPointTeleportKinahInventorySendAdapterService`; `BindPointTeleportKinahInventorySendResultPlanService`; `BindPointTeleportKinahSendBeforeRuntimeOrderingService` | Service / Callback Send Gate | Partial | Regression Tested | Needs Verification | Send result now can come from an opt-in adapter and still gates cooldown/action `3` fanout. Scheduled callback live dispatch and final movement remain disabled. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryUpdateItem` via packet intent | Packet / Serialization | Partial | Regression Tested | Needs Verification | Existing packet intent uses `DecreaseKinahFly`/mask `0x4B`; this unit did not add golden-byte Java comparison. Serialization remains source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 opt-in send adapter service plus 7 focused tests added to the existing adapter test fixture
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 known-list fanout gate, 1 live movement adapter, 1 `GameServerConnection` dispatch path, 1 Java packet capture path, and 1 live scheduled task path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Adapter is opt-in but not registered or called from `GameServerConnection`.
- Java runtime packet bytes were not captured for `SM_INVENTORY_UPDATE_ITEM`.
- Live known-list fanout, cooldown storage execution, final teleport scheduling, and movement remain disabled.
- C# staged persistence-before-send policy remains intentionally different from Java dirty storage timing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, known-list fanout, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a known-list fanout parity design or characterization slice for bind-point action `3`.
- Scope:
  - Compare Java `PacketSendUtility.broadcastPacket(player, packet, true)` behavior against current C# registry/distance fanout.
  - Preserve self-first or self-included ordering expectations explicitly.
  - Keep live scheduled callback dispatch disabled.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Known-list fanout parity characterization | fanout tests/docs | Medium | Best next blocker after SQL/send seams. |
| B | Java packet capture design for `SM_INVENTORY_UPDATE_ITEM` | docs only | Low/Medium | Needed before claiming serialization parity. |
| C | Movement side-effect readiness audit | docs only | Low/Medium | Helps stage final movement adapter. |
| D | DB integration test design for Kinah SQL | docs or gated test skeleton | Medium | Keep off default CI unless environment variable is set. |

### Do Not Parallelize

- Known-list fanout and final movement execution in the same unit.
- `GameServerConnection` dispatch with any remaining disabled adapter.
- Packet send adapter and SQL adapter rewiring.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahInventorySendAdapterPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKinahSendBeforeRuntimeOrderingService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `docs/Phase-6-BindPointTeleport-KinahInventoryLiveSendAdapter-Plan.md`
- Latest completed commits:
  - `2ed5b29eb [Phase 6][UOW-1245] Add bind point teleport Kinah SQL persistence adapter`
  - next commit should be `[Phase 6][UOW-1246] Add bind point teleport Kinah inventory send adapter`

Keep live bind-point behavior disabled until known-list fanout, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
