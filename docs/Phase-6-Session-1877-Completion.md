# Phase 6 Session 1877 Completion - Condition Preview Golden Fixture Plan

Date: 2026-05-31
Unit of Work: UOW-1877
Status: Completed

## Scope

- Performed Work Discovery against the latest handoff, Java condition sources, C# isolated condition-preview services, and existing tests.
- Added a source-derived fixture plan for later Java runtime/golden capture of the isolated condition/stat preview path.
- Kept the work limited to planning/reporting and tests. No live gameplay wiring was enabled.

## What Changed

- Added `SkillStatConditionPreviewGoldenFixturePlanService`.
- Added ten planned Java comparison fixtures covering:
  - `weapon` player match, player mismatch, and non-player pass-through
  - `front` base stat-validation pass-through
  - `charge` Item-owner satisfied, Item-owner failed, and non-Item-owner failed
  - `onfly` flying and not-flying owners
  - mixed `weapon -> charge` short-circuit ordering
- Each fixture records:
  - fixture name
  - condition sequence
  - Java source artifacts
  - required Java inputs
  - expected child-condition statuses
  - expected pure-preview status
  - Java source rule used to derive the expectation
- The plan explicitly reports that Java runtime/golden evidence is still missing.
- Added focused unit tests for the fixture plan and required missing-evidence markers.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionPreviewGoldenFixturePlanServiceTests|FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionPreviewCoverageReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionPreviewGoldenFixturePlanServiceTests|FullyQualifiedName~SkillStatConditionPreviewCoverageReportServiceTests|FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFormulaServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused condition preview/fixture-plan slice passed with 36 tests.
- Standard Phase 6 slice passed with 566 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4722 tests.

## Known Gaps

- This unit produced a fixture plan only. It is not Java runtime/golden evidence.
- No Java runtime harness was executed.
- No golden output was captured for any fixture.
- Planned expectations are source-derived from reviewed Java code and still need objective Java execution evidence.
- No live `Conditions.validate` provider or Java condition subclass registry is wired.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- The pure evaluator does not model Java `CreatureGameStats` owner-specific `EnchantEffect` hand filtering or `StatCapUtil.calculateBaseValue`.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat providers for the drop registration workflow.

## Parity Table Updates

- Added Session 1877 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `WeaponCondition.validate(Stat2, IStatFunction)`
  - `ItemChargeCondition.validate(Stat2, IStatFunction)` and `Item.getChargeLevel`
  - `OnFlyCondition.validate(Stat2, IStatFunction)`, base `Condition.validate`, and `Conditions.validate` ordering
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly plan-only without Java runtime/golden evidence.

## Next Recommended Unit of Work

- Next sequential task: implement or script Java-side runtime/golden capture for the planned isolated condition preview fixtures, starting with `weapon`, `front`, `charge`, `onfly`, and mixed short-circuit cases, then compare captured outputs to the C# preview path.

Safe alternative candidates:

- Investigate the transient `WorldNpcWalkerRouteWalkingServiceTests.TargetReachedAsync_SchedulesBroadcastAfterRestTime` double-broadcast failure if it recurs.
- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
