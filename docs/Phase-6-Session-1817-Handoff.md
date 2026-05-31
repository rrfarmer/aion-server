# Phase 6 Session 1817 Handoff - Compose Craft Start Inventory Plans

Date: 2026-05-31
Unit of Work: UOW-1817
Status: Completed

## What Changed

- Extended `CmCraftStartCompositionPlan` with `InventoryMutationPlan` and `InventoryPacketPlan`.
- Added composition steps for inventory mutation and packet planning.
- Composed `CraftService.CreateStartInventoryMutationPlan(...)` and `CraftService.CreateStartInventoryPacketPlan(...)` into the successful ready CM_CRAFT start path.
- Kept runtime-blocked and validation-failed paths without mutation/packet intent.
- Updated composition and `GameServerConnection` CM_CRAFT tests to assert non-live mutation/packet intent from both direct planner calls and real packet processing.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 311 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4563 tests.

## Known Gaps

- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- Java `ItemPacketService.sendItemDeletePacket` also sends `SM_CUBE_UPDATE` after cube deletes; this is still not planned.
- No item persistence state changes are written.
- No DP spend, live `CraftingTask`, scheduler startup, or craft completion is wired.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add non-live `SM_CUBE_UPDATE` planning for deleted craft-consumption stacks so the packet plan better reflects Java `ItemPacketService.sendItemDeletePacket`.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Begin a live-safe CM_CRAFT start side-effect boundary that still does not mutate/send by default.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `ItemPacketService.sendItemDeletePacket`, C# `SmCubeUpdate`, `CraftStartInventoryPacketPlan`, and current cube-size snapshot helpers.
- Keep the next unit non-live unless it explicitly scopes and verifies packet sending or inventory mutation side effects.
