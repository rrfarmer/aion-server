# Phase 6 Session 1819 Completion - Add Ordered Craft Consumption Operations

Date: 2026-05-31
Unit of Work: UOW-1819
Status: Complete

## Scope

Add non-live ordered craft-consumption mutation operations so packet intent can preserve Java's per-stack `Storage.decreaseByItemId` / `decreaseItemCount` emission order. This unit intentionally does not send packets, mutate player inventory, persist item state, spend DP, create `CraftingTask`, start scheduler work, or execute live `CraftService.startCrafting`.

## Completed Work

- Added `CraftStartInventoryMutationOperation` and `CraftStartInventoryMutationOperationKind`.
- Extended `CraftStartInventoryMutationPlan` with `OrderedOperations`.
- Updated `CraftService.CreateStartInventoryMutationPlan(...)` to record each stack operation in Java decrease order:
  - deleted bonus/component stacks
  - updated component stacks
- Updated `CraftService.CreateStartInventoryPacketPlan(...)` to emit packets from `OrderedOperations` instead of grouped summary lists.
- Preserved existing `UpdatedItems` and `DeletedObjectIds` summary fields for current callers.
- Added focused tests proving:
  - ordered mutation operations preserve bonus-before-component and stack-walk order
  - insufficient-inventory plans retain partial ordered operations
  - packet intent now follows delete/cube/update order from the operation stream
  - direct CM_CRAFT composition reflects the ordered packet sequence

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 312 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4564 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket`

## Migration Parity Table - UOW-1819

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` bonus/component decrease order | `CraftStartInventoryMutationPlan.OrderedOperations` | Inventory Planner | Partial | Unit Tested | Partial Parity | C# records ordered non-live operations for bonus first, then selected component decreases; no live inventory mutation occurs. |
| `Storage.decreaseByItemId` stack-walk order | `CraftStartInventoryMutationOperation` | Inventory Planner | Partial | Unit Tested | Partial Parity | C# records per-stack update/delete operations in working inventory order; persistence and quest callbacks remain pending. |
| `Storage.decreaseItemCount` packet side-effect order | `CraftService.CreateStartInventoryPacketPlan` | Packet Planner | Partial | Unit Tested | Partial Parity | C# packet intent now follows ordered operations for update/delete/cube packets; no live send or Java golden packet comparison occurs. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No item persistence state changes are written.
- Java quest callbacks/logging from storage delete are not modeled.
- No DP spend, live `CraftingTask`, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a live-safe craft-start side-effect boundary plan that can sequence DP spend, ordered inventory mutation/packet intent, and task-start intent without executing live side effects by default.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - add persistence-state planning for craft-consumed item updates/deletes
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1819-Completion.md`
- `docs/Phase-6-Session-1819-Handoff.md`
