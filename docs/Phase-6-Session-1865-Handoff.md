# Phase 6 Session 1865 Handoff - Stat2 Formula Readiness Evidence

Date: 2026-05-31
Unit of Work: UOW-1865
Status: Completed

## What Changed

- Added source-derived Java formula evidence to `SkillBuffStat2EvaluationReadinessReport`.
- Added `AdditionPercentFormula`, `ReversePercentFormula`, `AdditionBonusFormula`, `ReverseBonusFormula`, and `ReverseBaseFloorRule`.
- Documented that Java:
  - applies `AdditionStat.addToBonus` as `bonus += bonusRate * value`
  - applies `ReverseStat.addToBonus` as `bonus -= bonusRate * value`
  - applies `AdditionStat.calculatePercent` as `(100 + delta) / 100f`
  - applies `ReverseStat.calculatePercent` as `(100 - delta) / 100f`, floored at zero
  - applies `ReverseStat.addToBase` by subtracting from base and flooring at zero
- Kept everything readiness-only. No live `Stat2`, `StatAddFunction`, `StatRateFunction`, `StatSetFunction`, stat-cap, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Stat2 readiness tests passed with 17 tests.
- Standard Phase 6 slice passed with 521 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4677 tests.

## Known Gaps

- Formula evidence remains report-only.
- C# still lacks live `Stat2` state/formula mutation and Java float precision/truncation-order proof.
- C# still lacks live `StatAddFunction.apply`, `StatRateFunction.apply`, `StatSetFunction.apply`, `AdditionStat`, and `ReverseStat`.
- C# still lacks live `StatCapUtil.calculateBaseValue`.
- Active drop-boost readiness remains report-only.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` providers for the drop registration workflow.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Next sequential task: start a tiny live stat formula helper or calculator only if it can be kept isolated from gameplay and tested against Java source-derived cases for `AdditionStat`, `ReverseStat`, `StatAddFunction`, `StatRateFunction`, and `StatSetFunction`.

Safe alternative candidates:

- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
- Add more detailed readiness evidence for `StatFunction.validate` / condition ordering before live evaluator work.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStat2EvaluationReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillBuffStat2EvaluationReadinessReportServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatFunctionPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcDropBoostActiveStatProviderReadinessReportService.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For the next sequential task, inspect:
  - Java `com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction`
  - Java `com.aionemu.gameserver.model.stats.calc.functions.StatRateFunction`
  - Java `com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction`
  - Java `com.aionemu.gameserver.model.stats.calc.AdditionStat`
  - Java `com.aionemu.gameserver.model.stats.calc.ReverseStat`
  - Java `com.aionemu.gameserver.model.stats.calc.Stat2`
  - C# stat/effect services under `dotnetConversion/src/Aion.GameServer/Services`
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
