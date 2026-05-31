# Phase 6 Session 1815 Completion - Add Craft Start Inventory Mutation Planning

Date: 2026-05-31
Unit of Work: UOW-1815
Status: Complete

## Scope

Add non-live inventory mutation planning for successful start-craft material and bonus consumption. This unit intentionally does not mutate player inventory, persist item state, send inventory packets, spend DP, create `CraftingTask`, or start scheduler work.

## Completed Work

- Added `CraftService.CreateStartInventoryMutationPlan(...)`.
- Added `CraftStartInventoryMutationPlan` and `CraftStartInventoryMutationStatus`.
- Planned Java `Storage.decreaseByItemId` stack behavior for craft consumption rows:
  - walk matching cube, unequipped item stacks in inventory order
  - decrease each stack until the requested count is satisfied
  - record updated item snapshots when a stack remains
  - record deleted object ids when a non-kinah stack reaches zero
- Kept the planner non-live and non-mutating; tests verify original `Player.InventoryItems` counts are unchanged.
- Added focused tests for bonus-before-component mutation intent ordering, multi-stack component consumption, insufficient inventory reporting, and no-op behavior when consumption was not planned.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 308 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4560 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.model.gameobjects.Item.decreaseItemCount`

## Migration Parity Table - UOW-1815

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` bonus/component decrease order | `CraftService.CreateStartInventoryMutationPlan` | Inventory Planner | Partial | Unit Tested | Partial Parity | C# consumes the prior consumption plan order; no live inventory mutation occurs. |
| `Storage.decreaseByItemId` stack walking | `CraftStartInventoryMutationPlan.UpdatedItems` / `DeletedObjectIds` | Inventory Planner | Partial | Unit Tested | Partial Parity | C# plans ordered stack decreases, updates, and deletes for cube unequipped items; persistence and packets remain pending. |
| `Item.decreaseItemCount` zero-stack delete behavior | `CraftStartInventoryMutationPlan.DeletedObjectIds` | Inventory Planner | Partial | Unit Tested | Partial Parity | C# records deleted object ids when planned counts reach zero; no item state is mutated live. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No item persistence state changes are written.
- No `SM_INVENTORY_UPDATE_ITEM` or `SM_DELETE_ITEM` packets are sent.
- No DP spend, live `CraftingTask`, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a non-live craft start inventory packet plan for the mutation intent so updated stacks map to `SM_INVENTORY_UPDATE_ITEM` and deleted stacks map to `SM_DELETE_ITEM` without sending them.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - compose the new inventory mutation planner into `CmCraftStartCompositionPlan`
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1815-Completion.md`
- `docs/Phase-6-Session-1815-Handoff.md`
