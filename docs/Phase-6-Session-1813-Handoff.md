# Phase 6 Session 1813 Handoff - CM_CRAFT Composition Observer Seam

Date: 2026-05-31
Unit of Work: UOW-1813
Status: Completed

## What Changed

- Added optional `cmCraftStartCompositionPlanObserver` support to `GameServerConnection`.
- Added a non-live `ObserveCraftStartCompositionPlan(...)` call from `HandleCraftAsync`.
- Real CM_CRAFT packet processing can now emit both:
  - `CmCraftRuntimePlan`
  - `CmCraftStartCompositionPlan`
- Composition uses loaded static data when available and records conservative missing-template facts when static data is absent.
- Added integration coverage for real packet processing start-intent composition and runtime-blocked composition.
- Preserved no-live-side-effect behavior: no packets are sent, no DP is spent, no inventory is mutated, and no task is started.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 304 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4556 tests.

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No DP spend, inventory mutation, persistence, packet send, `CraftingTask` creation, or scheduler start.
- Integration coverage currently proves conservative missing-recipe composition from the packet seam; it does not yet prove a static-data-backed ready path.
- C# still lacks a direct Java `StaticObject` model, so composition relies on supplied target facts.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add static-data-backed CM_CRAFT handler composition coverage for a real recipe/product so the observer seam can prove successful ready-path composition from packet processing.

Safe alternative candidates:

- Begin non-live inventory mutation plan for material/bonus consumption.
- Add a live-safe craft finish cooldown application mutation plan.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CM_CRAFT.runImpl`, Java `CraftService.startCrafting`, C# `GameServerConnection.HandleCraftAsync`, and `CmCraftStartCompositionPlanService`.
- If choosing the static-data-backed coverage unit, search loaded `RecipeTemplateTable` fixtures for a recipe with simple component data and product item template coverage; keep the handler seam non-live.
