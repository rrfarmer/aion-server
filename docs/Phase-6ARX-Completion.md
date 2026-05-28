# Phase 6ARX Completion - Material Weather Transition Plan

Date: 2026-05-28
Unit of Work: UOW-1656
Status: Complete after focused unit tests

## Scope

This unit added a non-live weather transition plan for Java `WeatherTable.getWeatherAfter` and deterministic portions of `WeatherService.getRandomWeather`.

The C# code models transition decisions from supplied metadata. It does not call Java-equivalent RNG, mutate weather arrays, load weather XML, broadcast packets, or schedule weather changes.

## Completed Work

- Added `WorldMapRegionMaterialZoneWeatherTransitionPlanService`.
- Added weather transition context, entry snapshot, transition plan, and transition status DTOs/enums.
- Modeled before/active/after chained transitions.
- Modeled weighted rank selection from supplied `Rnd.get(0, 6)` metadata.
- Modeled rank fallback and rank `-1` constant weather.
- Modeled snow filtering outside winter months.
- Modeled selected-weather before-entry preference.
- Modeled afternoon day-time correction and Java integer division in chance-clearing thresholds.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- `WeatherService.nextWeather` first asks `WeatherTable.getWeatherAfter`.
- `WeatherTable.getWeatherAfter` transitions before weather to active weather and active weather to after weather when names and zone ids match.
- `WeatherService.getRandomWeather` maps `Rnd.get(0, 6)` to initial rank: `0 -> rank 0`, `1..2 -> rank 1`, `3..6 -> rank 2`.
- Rank `-1` returns immediately as constant weather.
- `SNOW` and `SNOW_BEACH` are excluded when snow is not allowed.
- If selected weather is not a before entry, Java searches the same zone weather list for a matching before entry and uses it.
- Afternoon correction uses integer division in expressions like `33 / dayTimeCorrection`.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneWeatherTransitionPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneEnvironmentPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneActorPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneHandlerPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSerializationPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests"
```

Result: passed 43 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material weather transition plan | new weather-transition service/tests | Medium | Yes | Next weather behavior boundary after current environment lookup metadata. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe candidate; deferred after sidecar notes from UOW-1654. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Broader zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Weather transition helper, tests, docs, commit | `WorldMapRegionMaterialZoneWeatherTransitionPlanService.cs`, `WorldMapRegionMaterialZoneWeatherTransitionPlanServiceTests.cs`, progress/handoff docs | Java source writes, live weather mutation, unrelated services/tests | Implemented and documented UOW-1656. |
| Sub-agents | None | None | All files | Not spawned because selected work was small and Orchestrator-owned. |

No sub-agent was spawned for UOW-1656 because selected implementation and docs were small and shared docs remained Orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_UsesJavaBeforeAndAfterWeatherChainBeforeRandomSelection` | Added | Before-to-active and active-to-after transitions are preferred before random selection. | Static source review of Java `WeatherTable.getWeatherAfter`. |
| `CreatePlan_ReturnsConstantWeatherForRankMinusOne` | Added | Rank `-1` entries return immediately as constant weather. | Static source review of Java `WeatherService.getRandomWeather`. |
| `CreatePlan_FiltersSnowOutsideWinterAndFallsBackToLowerRank` | Added | Summer `SNOW` is excluded and lower-rank weather can be selected. | Static source review of Java `checkSnowCondition` and rank fallback loop. |
| `CreatePlan_PrefersBeforeWeatherForSelectedActiveWeather` | Added | Active selected weather is replaced by matching before entry when present. | Static source review of Java before-entry preference loop. |
| `CreatePlan_UsesJavaIntegerAfternoonCorrectionWhenChanceClearsWeather` | Added | Afternoon correction uses Java integer division and can clear rank-0 weather at chance `16`. | Static source review of Java chance clearing expression. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.templates.world.WeatherTable` | `WorldMapRegionMaterialZoneWeatherTransitionPlanService` | Weather Table Boundary Plan | Partial | Unit Tested Metadata | Partial Parity | C# models `getWeatherAfter` and zone-filtered weather data selection. It does not model JAXB loading, table counts, or live `DataManager.MAP_WEATHER_DATA`. |
| `com.aionemu.gameserver.services.WeatherService.getRandomWeather` | `WorldMapRegionMaterialZoneWeatherTransitionPlanService.CreatePlan` | Weather Randomization Plan | Partial | Unit Tested Metadata | Partial Parity | C# models rank choice from supplied random metadata, rank fallback, constant weather, snow filtering, before-entry preference, afternoon correction, and chance clearing. It does not call live RNG or mutate weather arrays. |
| `com.aionemu.gameserver.services.WeatherService.checkSnowCondition` | `WorldMapRegionMaterialZoneWeatherTransitionPlanService` | Weather Filter Utility | Partial | Unit Tested | Partial Parity | C# models Java exclusion of `SNOW` and `SNOW_BEACH` when snow is not allowed. Special always-valid snow names remain represented only by not matching those exact names. |
| `com.aionemu.gameserver.model.templates.world.WeatherEntry` | `WorldMapRegionMaterialZoneWeatherTransitionEntrySnapshot` | Weather DTO | Partial | Unit Tested Metadata | Partial Parity | C# carries zone id, weather name, rank, before, and after metadata used by transitions. It does not model weather code, JAXB serialization, or singleton `WeatherEntry.NONE` object identity. |
| `com.aionemu.commons.utils.Rnd` | `WorldMapRegionMaterialZoneWeatherTransitionContext` supplied random metadata | Random Utility Boundary | Not Started | Unit Tested Metadata | Needs Verification | C# does not implement Java `L64X256MixRandom`, thread-local splitting, or exact RNG streams. Rank/chance/selection are supplied deterministically for testable planning. |
| `com.aionemu.gameserver.utils.time.gametime.GameTime.getMonth` | `WorldMapRegionMaterialZoneWeatherTransitionContext.GameMonth` | Time Boundary DTO | Not Started | Unit Tested Metadata | Needs Verification | C# uses supplied month to model snow eligibility. Live month arithmetic remains in the environment/time backlog. |

## Remaining Risks

- Weather transition planning is non-live and does not mutate `worldZoneWeathers` arrays or broadcast `SM_WEATHER`.
- Java RNG stream parity is not attempted; random choices are supplied as deterministic metadata.
- Live `DataManager.MAP_WEATHER_DATA`, JAXB weather table loading, and weather code serialization remain unported.
- `GameTime.getMonth` is supplied metadata here; full date arithmetic remains outside this helper.
- Exact weather packet output, random delay scheduling, and player filtering in `checkWeathersTime` remain unported.
- Live material actors still consume supplied DTOs rather than live services.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live weather transition helper plus 5 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live RNG stream parity, live weather arrays, `DataManager.MAP_WEATHER_DATA`, JAXB weather loading, full `GameTime` date arithmetic, `SM_WEATHER` broadcast output, weather scheduling delay, live material actor service integration, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Weather broadcast/change plan | new helper/tests | Model `WeatherService.checkWeathersTime`, `loadWeather`, `changeWeather`, packet broadcast metadata, player filtering, random-delay scheduling metadata, and weather-code override behavior. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live weather broadcast/change plan.
- Why: transition/random-selection rules are now modeled; the next Java weather boundary is scheduling, override changes, load-weather packet metadata, and broadcast filters.
- Files: likely new helper/tests plus docs.
- Java source to read: `WeatherService.checkWeathersTime`, `WeatherService.loadWeather`, `WeatherService.changeWeather`, `WeatherService.getOrCreateWeatherEntry`, `SM_WEATHER`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from material-zone weather files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Weather broadcast/change plan | new helper/tests, docs | Java writes, live weather mutation |
| Read-only Agent | Charge-all DB rollback candidate audit | read-only docs/Java/C# test inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live weather mutation, live actor mutation, live generated-zone writes, and live nearby dispatch: still high risk and intentionally disabled.
- Shared material/weather helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1656] Model material weather transitions
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneWeatherTransitionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneWeatherTransitionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARX-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
