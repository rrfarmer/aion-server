# Phase 6 Session 1812 Handoff - CM_CRAFT Start Composition Adapter Planning

Date: 2026-05-31
Unit of Work: UOW-1812
Status: Completed

## What Changed

- Added non-live `CmCraftStartCompositionPlanService.CreatePlan(...)`.
- Added `CmCraftStartCompositionPlan`, `CmCraftStartCompositionPlanStatus`, and `CmCraftStartCompositionPlanStep`.
- Composed `CmCraftRuntimePlan.StartIntent` into existing craft planners using Java CM_CRAFT inputs:
  - `recipeId`
  - `targetObjId`
  - `craftType`
  - `materialsData`
- Covered runtime blocked, validation failed, and successful ready-for-DP/task-start composition paths.
- Kept all behavior non-live; no packets are sent, no DP is spent, no inventory is mutated, and no task is started.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 302 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4554 tests.

## Known Gaps

- `GameServerConnection.HandleCraftAsync` still only records `CmCraftRuntimePlan`; it does not compose `CmCraftStartCompositionPlan`.
- No live `CraftService.startCrafting` execution.
- No DP spend, inventory mutation, persistence, packet send, `CraftingTask` creation, or scheduler start.
- C# still lacks a direct Java `StaticObject` model, so composition relies on supplied target facts.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: wire a non-live `GameServerConnection` observer/composition seam for `CmCraftStartCompositionPlan` so real CM_CRAFT packet processing can be tested through the adapter without dispatching live side effects.

Safe alternative candidates:

- Begin non-live inventory mutation plan for material/bonus consumption.
- Add a live-safe craft finish cooldown application mutation plan.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CM_CRAFT.runImpl`, Java `CraftService.startCrafting`, C# `GameServerConnection.HandleCraftAsync`, and the new composition adapter.
- If wiring the observer/composition seam, keep it non-live and test-only observable; do not start live task execution unless the unit explicitly scopes and verifies it.
