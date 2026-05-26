# Phase 6ABC Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1219
Status: Phase 6 continues; bind-point teleport now has parser, action selection, operation/control packet intents, fanout intents, request-level composition, runtime task/cooldown state semantics, scheduled Kinah decrement/failure intent, final movement gate intent, scheduled callback composition metadata, a `TeleportService.teleportTo` side-effect audit, a non-live teleport side-effect planner, callback-side side-effect metadata composition, a live-adapter readiness checklist, concrete failure system-message helpers, a non-live handler composition bridge, a runtime-owner design audit, an isolated runtime owner implementation, a non-sending runtime control bridge, and isolated source-included fanout for action `2`/login action `3` control intents. Full `GameServerConnection` dispatch, action `1` live callback execution, live inventory mutation, persistent known-list parity, and movement remain disabled.

## Session Summary

UOW-1219 added `BindPointTeleportRuntimeFanoutService`, a thin adapter that broadcasts `BindPointTeleportRuntimeControlBridgePlan` packet intents through `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportRuntimeFanoutServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-LiveFanout-Audit.md`
- `docs/Phase-6-PricesService-Consumer-Map.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ABC-Completion.md`

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleportRuntimeFanoutServiceTests" --nologo` passed 3 tests.
- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 82 tests.
- No Java runtime comparison was run, so parity remains `Needs Verification`.

## Migration Parity Table - UOW-1219

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportRuntimeFanoutService.BroadcastControlPlanAsync` | Fanout Utility / Adapter | Partial | Unit Tested | Needs Verification | Action `2` cancel bridge output is sent through `BroadcastToVisiblePlayersAsync(..., includeSourcePlayer: true)`. C# registry uses visible-distance approximation, not Java persistent `KnownList`; no Java runtime fanout comparison. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacketAndReceive(VisibleObject,AionServerPacket)` | `BindPointTeleportRuntimeFanoutService.BroadcastControlPlanAsync` | Fanout Utility / Adapter | Partial | Unit Tested | Needs Verification | Login action `3` bridge output uses source-included broadcast. Java sends to source then known players; C# delegates to registry include-source semantics. Known-list ordering and duplication behavior remain unverified. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.cancelTeleport` | `BindPointTeleportRuntimeControlBridgeService` plus `BindPointTeleportRuntimeFanoutService` | Service / Control Flow | Partial | Unit Tested | Needs Verification | Existing owner cancel and action `2` packet intent can now be broadcast by an isolated adapter. Still not invoked from `GameServerConnection`. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.onLogin` | `BindPointTeleportRuntimeControlBridgeService` plus `BindPointTeleportRuntimeFanoutService` | Service / Login Flow | Partial | Unit Tested | Needs Verification | Active cooldown action `3` packet intent can now be broadcast by an isolated adapter. No live enter-world/login hook. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT` | `Aion.GameServer.Network.Aion.ServerPackets.SmBindPointTeleport` through runtime fanout adapter | Packet / Serialization Dependency | Partial | Unit Tested | Needs Verification | Tests assert action `2` and login action `3` payloads at fanout boundary. No Java runtime packet capture. |
| `com.aionemu.gameserver.world.knownlist.KnownList` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync` | Known-List Dependency | Partial | Unit Tested | Needs Verification | Adapter records the known C# approximation explicitly. Persistent Java known-list membership, ordering, duplicate handling, and visibility edge cases remain unverified. |

Tests added:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `BroadcastControlPlanAsync_NoopsWhenControlBridgeHasNoPacket` | No packet intent produces no registry call. | Source-derived only. |
| `BroadcastControlPlanAsync_CancelPlanUsesSourceIncludedVisibleFanout` | Action `2` cancel packet is broadcast with `includeSourcePlayer: true`. | Source-derived only. |
| `BroadcastControlPlanAsync_LoginCooldownUsesBroadcastPacketAndReceiveSemantics` | Login action `3` cooldown packet is broadcast with source inclusion and Java utility metadata. | Source-derived only. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 isolated runtime fanout adapter plus 3 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 4 grouped categories: live dispatch, action `1` scheduled Kinah/inventory mutation, final movement, and persistent known-list parity
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Live `GameServerConnection` dispatch remains disabled.
- Action `1` scheduler callback fanout is not wired through this adapter yet.
- Runtime fanout uses C# visible-player registry approximation, while Java uses `KnownList.forEachPlayer`; persistent known-list parity remains unverified.
- Inventory mutation/persistence, scheduled Kinah callback execution, cooldown insertion from action `1`, and final movement remain unwired.
- Reflection behavior did not change. Serialization is source-derived, not Java-runtime verified. Threading, date/time, persistence, fanout ordering, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add an action `1` scheduled-callback owner bridge that schedules the existing non-live callback metadata through `BindPointTeleportRuntimeStateOwner` without performing Kinah mutation, cooldown insertion, fanout, or movement.
- Why: The owner and control fanout paths are staged, but action `1` still lacks a safe bridge from operation/callback metadata into the runtime task slot.
- Scope guard:
  - Do not mutate Kinah.
  - Do not insert cooldowns from the callback.
  - Do not broadcast action `3` from the callback.
  - Do not call movement services.
  - Do not wire full `GameServerConnection` dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Action `1` scheduled-callback owner bridge without inventory mutation | new service/test pair | Medium | Best next step; keep callback side effects metadata-only. |
| B | Static hotspot fact assembly audit | new doc or isolated adapter/test | Medium | Avoid shared static-data loaders unless owned exclusively. |
| C | Known-list parity audit for C# visible registry approximation | new doc only | Low/Medium | Useful before broad live fanout claims. |
| D | Continue `SM_SELL_ITEM` live-readiness fact assembly | new/non-overlapping sell-item adapter files and tests | Medium | Avoid shared dialog routing unless owned exclusively. |

### Do Not Parallelize

- `GameServerConnection` live bind-point dispatch: wait until action `1` scheduler bridge, inventory, movement, and known-list prerequisites are satisfied.
- Shared progress/handoff/parity docs: Orchestrator should own final edits.
- Action `1` callback bridge and live movement adapter in the same unit: too much side-effect risk.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
  - `game-server/src/com/aionemu/gameserver/controllers/CreatureController.java`
  - `game-server/src/com/aionemu/gameserver/model/TaskId.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeStateOwner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeControlBridgeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportRuntimeFanoutService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportScheduledCallbackPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportHandlerCompositionPlanService.cs`
- Latest completed commits:
  - `54aed5b58 [Phase 6][UOW-1218] Add bind point teleport runtime control bridge`
  - next commit should be `[Phase 6][UOW-1219] Add bind point teleport runtime fanout`
- Keep live bind-point behavior disabled until action `1` scheduled callback handling, live inventory mutation/packets, live movement packet ordering, and persistent known-list behavior each have focused parity slices.
