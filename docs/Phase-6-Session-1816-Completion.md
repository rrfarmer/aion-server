# Phase 6 Session 1816 Completion - Add Craft Start Inventory Packet Planning

Date: 2026-05-31
Unit of Work: UOW-1816
Status: Complete

## Scope

Add non-live packet planning for craft-start inventory consumption mutations. This unit intentionally does not send packets, mutate player inventory, persist item state, spend DP, create `CraftingTask`, start scheduler work, or compose the new packet planner into the CM_CRAFT handler plan.

## Completed Work

- Added `CraftService.CreateStartInventoryPacketPlan(...)`.
- Added `CraftStartInventoryPacketPlan` and `CraftStartInventoryPacketStatus`.
- Planned Java packet intent for successful craft consumption mutation rows:
  - updated stacks become `SM_INVENTORY_UPDATE_ITEM` with `ItemUpdateType.DEC_ITEM_USE` / `SmInventoryUpdateItem.DecreaseItemUse`
  - deleted zero-count stacks become `SM_DELETE_ITEM` with `ItemDeleteType.USE` / `SmDeleteItem.UseDeleteType`
- Added conservative guards for missing mutation evidence, missing item template table, and missing updated-stack item templates.
- Added focused tests for packet ordering, update/delete payload values, missing template behavior, and no-live-side-effect status.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 311 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4563 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType`
- `com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType`

## Migration Parity Table - UOW-1816

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `Storage.decreaseItemCount` surviving-stack packet path | `CraftService.CreateStartInventoryPacketPlan` / `CraftStartInventoryPacketPlan.Packets` | Packet Planner | Partial | Unit Tested | Partial Parity | C# plans `SmInventoryUpdateItem` with `DecreaseItemUse` for updated stacks; no live send occurs. |
| `ItemPacketService.sendItemDeletePacket` cube delete packet path | `CraftService.CreateStartInventoryPacketPlan` / `SmDeleteItem` entries | Packet Planner | Partial | Unit Tested | Partial Parity | C# plans `SmDeleteItem` with `UseDeleteType` for deleted craft-consumption stacks; Java also sends `SM_CUBE_UPDATE` after deletes, which remains outside this slice. |
| `ItemPacketService.ItemUpdateType.DEC_ITEM_USE` / `ItemDeleteType.USE` masks | `SmInventoryUpdateItem.DecreaseItemUse` / `SmDeleteItem.UseDeleteType` | Packet Constants | Partial | Unit Tested | Partial Parity | Tests decode serialized C# packet payloads for update/delete masks; no Java golden packet comparison was captured in this unit. |

## Risks / Gaps

- No live packets are sent by the new planner.
- Java sends `SM_CUBE_UPDATE` after cube deletes; this unit records only update/delete packet intent.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No item persistence state changes are written.
- The mutation and packet planners are not yet composed into `CmCraftStartCompositionPlan`.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Compose the craft start inventory mutation and packet planners into `CmCraftStartCompositionPlan` so ready CM_CRAFT observer output can report consumption mutation and packet intent without live side effects.
- Safe alternatives:
  - add non-live `SM_CUBE_UPDATE` planning for deleted craft-consumption stacks
  - add a live-safe craft finish cooldown application mutation plan
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1816-Completion.md`
- `docs/Phase-6-Session-1816-Handoff.md`
