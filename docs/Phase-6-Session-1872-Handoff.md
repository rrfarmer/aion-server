# Phase 6 Session 1872 Handoff - Isolated Stat Condition Evaluator

Date: 2026-05-31
Unit of Work: UOW-1872
Status: Completed

## What Changed

- Added isolated `SkillStatConditionEvaluatorService`.
- Added evaluation for the three audited Java stat-condition override names:
  - `weapon`
  - `charge`
  - `onfly`
- Added explicit evaluator statuses:
  - `Satisfied`
  - `NotSatisfied`
  - `MissingInput`
  - `UnsupportedCondition`
- `weapon` uses projected creature snapshot input to compare the XML `weapon` list against the player main-hand `ItemGroup`, and preserves Java's non-player owner pass-through only when creature input is known.
- `charge` uses projected item-owner snapshot input to compare Java charge level against the XML `value`, and returns not satisfied for non-Item owners like Java.
- `onfly` uses projected creature flying state.
- Kept this work isolated. No live condition validator, `Conditions.validate` provider, active effect, stat apply path, stat cap path, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFormulaServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused condition evaluator/snapshot/readiness slice passed with 36 tests.
- Standard Phase 6 slice passed with 547 tests.
- Initial broad run timed out at the 3-minute tool limit before returning a result.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4703 tests on rerun with a longer timeout.

## Known Gaps

- Evaluator remains isolated and does not execute live `Conditions.validate`.
- No Java condition subclass registry/provider is wired.
- `SkillBuffStatChangeEvaluatorService` still returns `UnsupportedConditions` for conditioned changes.
- Pass-through condition names are not evaluated by this helper; only `weapon`, `charge`, and `onfly` are implemented.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat providers for the drop registration workflow.
- No Java runtime/golden comparison was produced.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Parity Table Updates

- Added Session 1872 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `WeaponCondition.validate(Stat2, IStatFunction)`
  - `ItemChargeCondition.validate(Stat2, IStatFunction)`
  - `OnFlyCondition.validate(Stat2, IStatFunction)`
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly isolated from live runtime parity.

## Next Recommended Unit of Work

- Next sequential task: integrate the isolated `SkillStatConditionEvaluatorService` into a pure preview/evaluation path for conditioned stat changes only if it remains disconnected from live gameplay, applies Java list-order/short-circuit semantics, and continues to report missing inputs conservatively.

Safe alternative candidates:

- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
- Produce Java runtime/golden values for the isolated stat formula helper before using it in broader live code.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/SkillStatConditionEvaluatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatConditionEvaluatorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillStatConditionInputSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatConditionInputSnapshotServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatChangeEvaluatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillStatChangeConditionReadinessReportService.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For the next sequential task, inspect:
  - Java `Conditions.validate`
  - Java `StatFunction.validate`
  - Java `BufEffect.getModifiers`
  - Java `WeaponCondition`
  - Java `ItemChargeCondition`
  - Java `OnFlyCondition`
  - C# `SkillStatConditionEvaluatorService`
  - C# `SkillStatConditionInputSnapshotService`
  - C# `SkillBuffStatChangeEvaluatorService`
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
