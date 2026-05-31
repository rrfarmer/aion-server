# Phase 6 Session 1817 Completion - Compose Craft Start Inventory Plans

Date: 2026-05-31
Unit of Work: UOW-1817
Status: Complete

## Scope

Compose the existing non-live craft start inventory mutation and packet planners into `CmCraftStartCompositionPlan`. This unit intentionally does not send packets, mutate player inventory, persist item state, spend DP, create `CraftingTask`, start scheduler work, or add Java's `SM_CUBE_UPDATE` delete follow-up.

## Completed Work

- Extended `CmCraftStartCompositionPlan` with:
  - `InventoryMutationPlan`
  - `InventoryPacketPlan`
- Added composition steps:
  - `CreateInventoryMutationPlan`
  - `CreateInventoryPacketPlan`
- Updated `CmCraftStartCompositionPlanService.CreatePlan(...)` so ready start-craft composition now includes:
  - validation
  - consumption
  - inventory mutation intent
  - inventory packet intent
  - task plan
- Kept failure and runtime-blocked composition paths free of mutation/packet intent.
- Added focused composition tests for planned updated/deleted stack intents and packet types.
- Updated static-data-backed `GameServerConnection` CM_CRAFT observer coverage to prove real packet processing now exposes the mutation and packet plans without sending packets.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 311 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4563 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT.runImpl`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket`

## Migration Parity Table - UOW-1817

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` forwarding to `CraftService.startCrafting(...)` | `CmCraftStartCompositionPlanService.CreatePlan` | Handler Adapter | Partial | Unit/Integration Tested | Partial Parity | Real CM_CRAFT observer output now includes downstream mutation and packet intent, but no live service dispatch occurs. |
| `CraftService.startCrafting` successful `checkCraft` path before DP spend/task start | `CmCraftStartCompositionPlan.InventoryMutationPlan` / `InventoryPacketPlan` | Adapter Planner | Partial | Unit/Integration Tested | Partial Parity | C# composes consumption mutation and packet intent for the ready path; DP spend, live task start, persistence, and packet sends remain pending. |
| `Storage.decreaseItemCount` packet side effects during `checkCraft` consumption | `CraftStartInventoryPacketPlan` through `CmCraftStartCompositionPlan` | Packet Planner | Partial | Unit/Integration Tested | Partial Parity | C# exposes `SM_INVENTORY_UPDATE_ITEM` / `SM_DELETE_ITEM` intent through handler composition; Java's `SM_CUBE_UPDATE` after deletes remains outside this slice. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live packets are sent.
- Java sends `SM_CUBE_UPDATE` after cube deletes; this unit does not plan that packet.
- No item persistence state changes are written.
- No DP spend, live `CraftingTask`, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add non-live `SM_CUBE_UPDATE` planning for deleted craft-consumption stacks so the packet plan better reflects Java `ItemPacketService.sendItemDeletePacket`.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - begin a live-safe CM_CRAFT start side-effect boundary that still does not mutate/send by default
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1817-Completion.md`
- `docs/Phase-6-Session-1817-Handoff.md`
