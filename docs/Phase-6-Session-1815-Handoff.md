# Phase 6 Session 1815 Handoff - Craft Start Inventory Mutation Planning

Date: 2026-05-31
Unit of Work: UOW-1815
Status: Completed

## What Changed

- Added non-live `CraftService.CreateStartInventoryMutationPlan(...)`.
- Added `CraftStartInventoryMutationPlan` and `CraftStartInventoryMutationStatus`.
- Planned Java `Storage.decreaseByItemId` stack-walking behavior for craft consumption:
  - updated item snapshots for partial stack decreases
  - deleted object ids for stacks reduced to zero
  - conservative insufficient-inventory status
- Added focused tests for multi-stack decreases, bonus/component order flowing from the consumption plan, insufficient inventory, and no-op behavior when consumption was not planned.
- Preserved no-live-side-effect behavior: no inventory is mutated, no packets are sent, no persistence occurs, and no task is started.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 308 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4560 tests.

## Known Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No item persistence state changes are written.
- No `SM_INVENTORY_UPDATE_ITEM` or `SM_DELETE_ITEM` packets are sent.
- The mutation planner is not yet composed into `CmCraftStartCompositionPlan`.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add a non-live craft start inventory packet plan for the mutation intent so updated stacks map to `SM_INVENTORY_UPDATE_ITEM` and deleted stacks map to `SM_DELETE_ITEM` without sending them.

Safe alternative candidates:

- Compose the new inventory mutation planner into `CmCraftStartCompositionPlan`.
- Add a live-safe craft finish cooldown application mutation plan.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `Storage.decreaseItemCount`, `ItemPacketService.sendItemPacket`, `ItemPacketService.sendItemDeletePacket`, and C# inventory packet constructors.
- Keep the next unit non-live unless it explicitly scopes and verifies packet sending or inventory mutation side effects.
