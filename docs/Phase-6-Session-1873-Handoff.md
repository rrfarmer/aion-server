# Phase 6 Session 1873 Handoff - Opt-In Conditioned Stat Preview

Date: 2026-05-31
Unit of Work: UOW-1873
Status: Completed

## What Changed

- Integrated `SkillStatConditionEvaluatorService` into `SkillBuffStatChangeEvaluatorService` as an opt-in pure-preview path.
- Added `SkillBuffStatConditionEvaluationContext` with optional creature and item-owner snapshots.
- Preserved old conservative behavior when no context is supplied: conditioned changes still return `UnsupportedConditions`.
- Added per-step `ConditionResults` for preview evidence.
- Added `ConditionMissingInput` and `ConditionNotSatisfied` evaluator statuses.
- Implemented source-derived Java condition flow in the pure evaluator:
  - sorted stat changes still apply by Java-like function priority,
  - condition children are evaluated in XML/list order,
  - first not-satisfied child short-circuits remaining children for that step,
  - a not-satisfied step is skipped without applying,
  - missing or unsupported condition inputs stop preview conservatively.
- Kept this work isolated. No live condition validator, `Conditions.validate` provider, active effect, stat apply path, stat cap path, or drop workflow execution was enabled.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~SkillStatConditionEvaluatorServiceTests|FullyQualifiedName~SkillStatConditionInputSnapshotServiceTests|FullyQualifiedName~SkillStatChangeConditionReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFormulaServiceTests|FullyQualifiedName~SkillBuffStatChangeEvaluatorServiceTests|FullyQualifiedName~SkillBuffStat2EvaluationReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionRegistryReadinessReportServiceTests|FullyQualifiedName~SkillBuffStatFunctionPlanServiceTests|FullyQualifiedName~WorldNpcDropBoostActiveStatProviderReadinessReportServiceTests|FullyQualifiedName~WorldNpcDropBoostStatProviderReadinessReportServiceTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused condition/stat evaluator slice passed with 37 tests.
- Standard Phase 6 slice passed with 551 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4707 tests.

## Known Gaps

- This remains pure-preview only and is not live gameplay parity.
- No live `Conditions.validate` provider or Java condition subclass registry is wired.
- Only `weapon`, `charge`, and `onfly` can be evaluated through `SkillStatConditionEvaluatorService`.
- Pass-through condition names are still unsupported in the pure preview path unless a future unit explicitly adds conservative pass-through evidence.
- C# still lacks live `CreatureGameStats` storage, insertion/removal, snapshot locking/copying, condition validators, stat caps, max-stat synchronization, and active-effect lifecycle.
- The pure evaluator does not model Java `CreatureGameStats` owner-specific `EnchantEffect` hand filtering or `StatCapUtil.calculateBaseValue`.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat providers for the drop registration workflow.
- No Java runtime/golden comparison was produced.
- C# still lacks modeled/persisted player salvation points and Java's exact lifecycle around reset after 10 minutes offline.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Parity Table Updates

- Added Session 1873 rows in `docs/PHASE-6-PROGRESS.md` for:
  - `BufEffect.getModifiers` conditioned stat-function path
  - `Conditions.validate(Stat2, IStatFunction)`
  - `CreatureGameStats.getStat` validate-before-apply loop
- All rows remain `Partial Parity`, `Unit Tested`, and explicitly isolated from live runtime parity.

## Next Recommended Unit of Work

- Next sequential task: add readiness evidence/tests for pass-through condition names in the pure preview path, or explicitly keep them unsupported until a broader condition registry exists; avoid live gameplay wiring until Java runtime/golden evidence is available.

Safe alternative candidates:

- Add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#.
- Run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring.
- Produce Java runtime/golden values for the isolated stat formula helper before using it in broader live code.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/SkillBuffStatChangeEvaluatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillBuffStatChangeEvaluatorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillStatConditionEvaluatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatConditionEvaluatorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/SkillStatConditionInputSnapshotService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SkillStatConditionInputSnapshotServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, the latest completion document, and this handoff before choosing the next UOW.
- For the next sequential task, inspect:
  - Java `Condition.validate(Stat2, IStatFunction)`
  - Java mapped condition subclasses that do not override stat validation
  - C# `SkillStatConditionEvaluatorService`
  - C# `SkillBuffStatChangeEvaluatorService`
  - C# condition readiness tests from UOW-1870
- Keep workflow execution blocked until live effect state, stat calculation, conditions, stat caps, and drop registration can be compared against Java.
