# Phase 6 Session 1864 Handoff - Negative Speed Rate Readiness

Date: 2026-05-31
Unit of Work: UOW-1864
Status: Completed

## What Changed

- Added readiness evidence for Java `StatRateFunction.apply` negative bonus `SPEED` handling.
- Added `NegativeSpeedRateFunctionCount`, `RequiresNegativeSpeedRateFunctionHandling`, and `HasLiveNegativeSpeedRateFunctionProvider` to `SkillBuffStat2EvaluationReadinessReport`.
- Added `BlockedMissingNegativeSpeedRateFunctionProvider`.
- Documented the Java branch: for bonus `SPEED` rate functions with a negative value and an already negative stat bonus, Java uses `stat.getCurrent()` as the rate base instead of `stat.getBaseWithoutBaseRate()`.
- Kept everything readiness-only. No live `StatRateFunction`, live `Stat2`, stat-cap, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused Stat2 readiness tests passed with 23 tests.
- Standard Phase 6 slice passed with 521 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4677 tests.

## Known Gaps

- Negative speed-rate handling remains report-only.
- C# still lacks live `StatRateFunction.apply`.
- C# still lacks live `Stat2` state/formula mutation, Java truncation-order proof, and function ordering behavior.
- C# still lacks live `StatCapUtil.calculateBaseValue`.
- Active drop-boost readiness remains report-only.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, and active-effect lifecycle.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` providers for the drop registration workflow.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Next sequential task: inspect Java `StatAddFunction`, `StatSetFunction`, `AdditionStat`, and `ReverseStat` apply/math differences and decide whether a small source-derived formula-readiness report or a narrow live formula test helper can be added without wiring active gameplay.

Safe alternative candidates:

- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
- Start a tiny live `StatCapUtil` formula slice only if it can be compared against Java source-derived cases without touching active drop workflow execution.

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
  - Java `com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction`
  - Java `com.aionemu.gameserver.model.stats.calc.AdditionStat`
  - Java `com.aionemu.gameserver.model.stats.calc.ReverseStat`
  - Java `com.aionemu.gameserver.model.stats.calc.Stat2`
  - C# `SkillBuffStat2EvaluationReadinessReportService`
  - C# `SkillBuffStatFunctionPlanService`
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
