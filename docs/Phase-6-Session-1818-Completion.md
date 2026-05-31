# Phase 6 Session 1818 Completion - Add Craft Start Cube Update Packet Planning

Date: 2026-05-31
Unit of Work: UOW-1818
Status: Complete

## Scope

Add non-live `SM_CUBE_UPDATE` planning after deleted craft-consumption stacks so the craft start inventory packet plan better reflects Java `ItemPacketService.sendItemDeletePacket`. This unit intentionally does not send packets, mutate player inventory, persist item state, spend DP, create `CraftingTask`, start scheduler work, or fully reorder packet intent into Java's exact per-stack emission stream.

## Completed Work

- Updated `CraftService.CreateStartInventoryPacketPlan(...)` to accept an optional `Player` snapshot.
- Added `CraftStartInventoryPacketStatus.MissingCubeSizeSnapshot` for deleted-stack plans that cannot compute Java cube-size refreshes.
- Appended `SmCubeUpdate.CubeSizeSnapshot(...)` after each planned `SmDeleteItem` when a player snapshot is available.
- Updated `CmCraftStartCompositionPlanService` to pass the player snapshot into packet planning.
- Added focused tests for:
  - update/delete/cube packet sequence
  - projected cube item counts after each delete
  - conservative missing-player-snapshot status
  - static-data-backed CM_CRAFT observer packet intent including `SmCubeUpdate`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 312 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4564 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`

## Migration Parity Table - UOW-1818

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ItemPacketService.sendItemDeletePacket` cube delete follow-up | `CraftService.CreateStartInventoryPacketPlan` / `SmCubeUpdate.CubeSizeSnapshot` | Packet Planner | Partial | Unit/Integration Tested | Partial Parity | C# now plans `SmCubeUpdate` after craft-consumption deletes when a player snapshot is available; no live send occurs. |
| `SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, player)` | `SmCubeUpdate.CubeSizeSnapshot` from craft packet plan | Packet Planner | Partial | Unit Tested | Partial Parity | Tests decode C# cube payload fields and projected counts; no Java golden packet comparison was captured in this unit. |
| `Storage.decreaseItemCount` delete packet side effects during `checkCraft` consumption | `CraftStartInventoryPacketPlan.Packets` | Packet Planner | Partial | Unit/Integration Tested | Partial Parity | C# plans delete plus cube update for deleted stacks, but packet ordering still follows current mutation-plan grouping rather than Java's exact per-stack emission order. |

## Risks / Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- Packet intent ordering still follows current mutation-plan grouping rather than Java's exact per-stack decrease side-effect order.
- No item persistence state changes are written.
- No DP spend, live `CraftingTask`, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add an ordered craft-consumption mutation/packet operation plan so update/delete/cube packet intent can preserve Java's per-stack emission order across bonus and component decreases.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - begin a live-safe CM_CRAFT start side-effect boundary that still does not mutate/send by default
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1818-Completion.md`
- `docs/Phase-6-Session-1818-Handoff.md`
