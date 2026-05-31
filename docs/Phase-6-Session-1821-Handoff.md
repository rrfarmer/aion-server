# Phase 6 Session 1821 Handoff - Craft Consumption Persistence Planning

Date: 2026-05-31
Unit of Work: UOW-1821
Status: Completed

## What Changed

- Added non-live craft inventory persistence-state planning.
- Added `CraftStartInventoryPersistencePlan`.
- Added `CraftStartInventoryPersistenceOperation`.
- Added `CraftStartInventoryPersistenceStatus`.
- Added `CraftStartInventoryPersistenceOperationKind`.
- Added `CraftService.CreateStartInventoryPersistencePlan(...)`.
- Extended deleted craft mutation operations with a deleted item snapshot so persistence planning can apply Java `Item.setPersistentState(...)` rules.
- Extended `CmCraftStartCompositionPlan` with `InventoryPersistencePlan`.
- Added CM_CRAFT composition step `CreateInventoryPersistencePlan`.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 315 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4567 tests.

## Known Gaps

- Persistence planning is non-live and does not write the database.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No live packet sending, DP spend, task creation, scheduler startup, or craft completion is wired.
- Java transaction behavior and object-id release after successful delete remain pending.
- Java storage delete quest callbacks/logging are still not executed.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: begin a disabled live craft-start executor facade that consumes the existing boundary, mutation, persistence, packet, DP, and task plans but proves by default that no live side effects dispatch until explicitly enabled.

Safe alternative candidates:

- Add a live-safe craft finish cooldown application mutation plan.
- Add exact Java SQL descriptor planning for craft inventory update/delete rows.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.startCrafting`, Java `Storage.decreaseItemCount`, Java `InventoryDAO.store`, the C# side-effect boundary, C# persistence plan, C# DP spend method, and current task planner.
- Keep the next executor facade disabled by default unless it explicitly scopes and verifies each live side-effect dependency.
