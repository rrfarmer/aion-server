# Phase 6 Session 1870 Handoff - Condition Stat Override Classification

Date: 2026-05-31
Unit of Work: UOW-1870
Status: Completed

## What Changed

- Classified mapped Java condition classes by stat-validation behavior in `SkillStatChangeConditionReadinessReportService`.
- Added override evidence for:
  - `ItemChargeCondition.validate(Stat2, IStatFunction)`: requires `statFunction.getOwner()` to be an `Item`, checks `item.getChargeLevel() >= value`, and returns false for non-`Item` owners.
  - `OnFlyCondition.validate(Stat2, IStatFunction)`: checks `stat.getOwner().isFlying()`.
- Kept existing `WeaponCondition` override evidence from UOW-1869.
- Added pass-through evidence for mapped condition classes that do not override `validate(Stat2, IStatFunction)`, including geometry/charge skill conditions that only implement Skill or Effect validation.
- Added focused tests for override and pass-through examples: `charge`, `onfly`, `back`, and `chargeweapon`.
- Kept this work readiness-only. No live condition validator, item charge lookup, flying-state lookup, active effect, stat apply path, stat cap path, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFormulaServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Initial focused run caught a test assertion shape issue and was corrected before final validation.
- Focused condition/drop-boost/evaluator slice passed with 23 tests after the fix.
- Standard Phase 6 slice passed with 527 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4683 tests.

## Known Gaps

- Condition classification remains report-only.
- No live `Conditions.validate` provider or Java condition subclasses are ported.
- C# still lacks live item `StatOwner`, item charge-level, and creature flying-state surfaces for these validators.
- `SkillBuffStatChangeEvaluatorService` still returns `UnsupportedConditions` for conditioned changes.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat providers for the drop registration workflow.
- No Java runtime/golden comparison was produced.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Next sequential task: inspect C# equipment/item/flying-state model surfaces and determine whether a tiny isolated condition input snapshot can be added for future `WeaponCondition`, `ItemChargeCondition`, and `OnFlyCondition` validators without wiring live gameplay.

Safe alternative candidates:

- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
- Produce Java runtime/golden values for the isolated stat formula helper before using it in broader live code.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/SkillStatChangeConditionReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatChangeConditionReadinessReportServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatChangeEvaluatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatFormulaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillBuffStatFormulaServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For the next sequential task, inspect:
  - Java `WeaponCondition`
  - Java `ItemChargeCondition`
  - Java `OnFlyCondition`
  - C# equipment and item models
  - C# player/creature flying-state model surfaces
  - C# `SkillStatChangeConditionReadinessReportService`
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
