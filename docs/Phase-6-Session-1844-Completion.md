# Phase 6 Session 1844 Completion - Add Drop Active Palace Source

Date: 2026-05-31
Unit of Work: UOW-1844
Status: Complete

## Scope

Add the narrowest missing live input source for drop boost readiness by resolving Java's active-palace bonus from currently modeled C# player houses and housing templates, without wiring the disabled plan into live drop registration.

## Completed Work

- Re-read required orchestration/parity/progress docs and the UOW-1843 handoff/completion.
- Re-inspected Java `DropRegistrationService.calculateBoostDropRate`, Java `HousingService.findActiveHouse`, Java `HouseType.PALACE`, C# `PlayerHouse`, C# `HousingTemplateTable`, and C# active-house call sites.
- Added `PlayerActiveHouseResolverService.FindActiveHouse(...)`.
- Added `PlayerActiveHouseResolverService.HasActivePalace(...)`.
- Updated `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan(...)` to consume `HousingTemplateTable` when supplied.
- Added `WorldNpcDropBoostRateContextPlan.HasActivePalace`.
- Kept workflow readiness blocked for unresolved Java inputs:
  - NPC `BOOST_DROP_RATE` stat source
  - killer `BOOST_DROP_RATE` stat source
  - killer `DR_BOOST` stat source
  - killer salvation percent source
- Added focused tests proving active-house selection and active-palace planner consumption.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused drop/static-data tests passed with 71 tests.
- Standard Phase 6 slice passed with 469 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4620 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.drop.DropRegistrationService.calculateBoostDropRate`
- `com.aionemu.gameserver.services.HousingService.findActiveHouse`
- `com.aionemu.gameserver.model.templates.housing.HouseType`

## Migration Parity Table - UOW-1844

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `HousingService.findActiveHouse` | `PlayerActiveHouseResolverService.FindActiveHouse` | Source Resolver | Partial | Unit Tested | Partial Parity | C# resolves the first non-inactive loaded player house. This matches current C# login ordering comments but does not independently model Java's separate studio/custom-house maps. |
| `HouseType.PALACE` active-house boost branch | `PlayerActiveHouseResolverService.HasActivePalace` | Drop Boost Input Source | Partial | Unit Tested | Partial Parity | C# uses `HousingTemplateTable.IsPalaceBuilding` to resolve the active-palace boolean from loaded houses/templates. |
| `DropRegistrationService.calculateBoostDropRate` active-palace input | `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan(..., housingTemplates)` | Readiness Planner Input | Partial | Unit Tested | Partial Parity | Planner now consumes one real modeled source and removes the active-palace-source blocker only when `Player` and `HousingTemplateTable` are supplied. Other Java live inputs still block workflow readiness. |

## Risks / Gaps

- The planner is disabled/readiness-only and is not wired into `WorldNpcDropRegistrationWorkflowService`.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat sources for this workflow.
- C# still lacks a modeled salvation percent source on `Player`.
- Configured `RatesConfig.DROP_RATES` still needs a live options/config path into drop registration.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Add the next narrow missing input source for drop boost readiness, preferably configured `RatesConfig.DROP_RATES` options binding or a modeled salvation-percent source, then keep workflow wiring blocked until all Java inputs are explicit.
- Safe alternatives:
  - run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - add a disabled finish-craft live-execution readiness checklist
  - inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcDropModifierService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcDropModifierServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1844-Completion.md`
- `docs/Phase-6-Session-1844-Handoff.md`
