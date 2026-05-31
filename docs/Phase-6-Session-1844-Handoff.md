# Phase 6 Session 1844 Handoff - Drop Active Palace Source

Date: 2026-05-31
Unit of Work: UOW-1844
Status: Completed

## What Changed

- Added `PlayerActiveHouseResolverService.FindActiveHouse(...)`.
- Added `PlayerActiveHouseResolverService.HasActivePalace(...)`.
- Updated `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan(...)` to consume `HousingTemplateTable` when supplied.
- Added `WorldNpcDropBoostRateContextPlan.HasActivePalace`.
- Planner now removes the `active palace source` missing input only when a `Player` and `HousingTemplateTable` produce an objective active-palace value.
- Added focused tests for active-house selection and active-palace planner consumption.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused drop/static-data tests passed with 71 tests.
- Standard Phase 6 slice passed with 469 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4620 tests.

## Known Gaps

- Planner is not wired into drop registration workflow.
- Live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat sources remain unavailable for this workflow.
- Player salvation percent is not modeled on the current C# player surface.
- Configured `RatesConfig.DROP_RATES` still needs live options/config binding into drop registration.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Next sequential task: add the next narrow missing input source for drop boost readiness, preferably configured `RatesConfig.DROP_RATES` options binding or a modeled salvation-percent source, then keep workflow wiring blocked until all Java inputs are explicit.

Safe alternative candidates:

- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Add a disabled finish-craft live-execution readiness checklist that gates recipe DB writes, quest callbacks, item insertion, packet sends, logging, cooldown persistence, and exception behavior.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcDropModifierService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcDropModifierServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For drop-rate options work, inspect:
  - Java `RatesConfig.DROP_RATES`
  - Java `Rates.get`
  - C# options/config binding surfaces in `dotnetConversion/src/Aion.GameServer`
  - current callers of `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan`
- For salvation work, inspect:
  - Java `PlayerCommonData.getCurrentSalvationPercent`
  - C# `Player` and common-data-equivalent surfaces
  - any existing repose/salvation services or packets
- Keep parity status partial until all Java live inputs feed the planner and workflow.
