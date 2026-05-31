# Phase 6 Session 1826 Handoff - Craft Start Facade Adapter Integration

Date: 2026-05-31
Unit of Work: UOW-1826
Status: Completed

## What Changed

- Integrated concrete disabled adapter plans into `CraftStartLiveExecutorFacadePlan`.
- Added `InventoryPersistenceAdapterPlan` to the facade.
- Added `InventoryPacketSendAdapterPlan` to the facade.
- Updated `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan(...)` so ready compositions create:
  - `CraftStartInventoryPersistenceAdapterPlanService.CreateDisabledPlan(...)`
  - `CraftStartInventoryPacketSendAdapterPlanService.CreateDisabledPlan(...)`
- Facade persistence and packet send booleans now derive from adapter plans.
- Missing/not-ready compositions still return no adapter plans.
- All behavior remains non-live and disabled by default.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused CM_CRAFT/craft/packet tests passed with 321 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4573 tests.

## Known Gaps

- The live executor facade and both adapter plans remain disabled and non-live.
- No live inventory mutation is applied.
- No live inventory packets are sent.
- No item persistence is written to the database.
- No transaction, commit/rollback, Java autocommit behavior, or object-id release is executed.
- No DP spend, `CraftingTask` creation, or task start occurs.
- Java storage delete quest callbacks/logging are not executed.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Next sequential task: add a live-safe craft finish cooldown application mutation plan using Java `CraftService.finishCrafting` cooldown behavior as source of truth, without executing live cooldown updates by default.

Safe alternative candidates:

- Begin live-enabled craft persistence adapter design only after explicit transaction/rollback scope.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.
- Add disabled DP-spend adapter wiring into the craft-start facade.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.finishCrafting`, recipe cooldown handling, C# craft finish planners, and existing cooldown model/service classes.
- Keep the next unit non-live unless explicitly scoping and verifying live cooldown state mutation.
