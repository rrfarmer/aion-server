# Phase 6 Session 1874 Handoff - Pass-Through Stat Condition Preview

Date: 2026-05-31
Unit of Work: UOW-1874
Status: Completed

## What Changed

- Added isolated pass-through condition handling to `SkillStatConditionEvaluatorService`.
- Pass-through handling covers mapped Java condition classes that do not override `validate(Stat2, IStatFunction)` and therefore inherit Java base `Condition.validate(...) == true`.
- Added Java class-name breadcrumbs for the mapped pass-through condition names.
- Updated unsupported-condition tests to use `unsupported_condition` instead of `front`.
- Added source-derived tests for `front`, `back`, and `chargeweapon`.
- Added pure stat-preview coverage showing `front` can now satisfy the opt-in condition context and allow the conditioned stat step to apply.
- Kept this work isolated. No live condition validator, `Conditions.validate` provider, active effect, stat apply path, stat cap path, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFormulaServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused condition/stat evaluator slice passed with 41 tests.
- Standard Phase 6 slice passed with 555 tests.
- Initial broad run failed once in unrelated `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` with two broadcasts instead of one.
- Isolated walker-route rerun passed with 1 test.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4711 tests on rerun.

## Known Gaps

- This remains pure-preview only and is not live gameplay parity.
- No live `Conditions.validate` provider or Java condition subclass registry is wired.
- Pass-through support covers stat-function validation only; Skill/Effect validation behavior for the same Java classes remains separate and is not modeled here.
- `weapon`, `charge`, and `onfly` remain the only mapped stat-validation override implementations.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- The pure evaluator does not model Java `CreatureGameStats` owner-specific `EnchantEffect` hand filtering or `StatCapUtil.calculateBaseValue`.
- Broad validation observed one transient unrelated walker-route test failure before isolated and broad reruns passed.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat providers for the drop registration workflow.
- No Java runtime/golden comparison was produced.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Parity Table Updates

- Added Session 1874 rows in `docs/PHASE-6-PROGRESS.md` for:
  - Java base `Condition.validate(Stat2, IStatFunction)`
  - mapped Java condition subclasses without stat-validation overrides
  - pass-through child behavior in the opt-in pure preview path
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly isolated from live runtime parity.

## Next Recommended Unit of Work

- Next sequential task: produce Java runtime/golden values for the isolated condition/stat formula preview path, or add a readiness report that enumerates exactly which static-data condition combinations are now preview-evaluable versus still blocked.

Safe alternative candidates:

- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/SkillStatConditionEvaluatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatConditionEvaluatorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatChangeEvaluatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillBuffStatChangeEvaluatorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillStatChangeConditionReadinessReportService.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For the next sequential task, inspect:
  - Java `Conditions.validate`
  - Java stat-function condition metadata in static data
  - C# `SkillStatConditionEvaluatorService`
  - C# `SkillBuffStatChangeEvaluatorService`
  - C# static-data condition summary model
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
