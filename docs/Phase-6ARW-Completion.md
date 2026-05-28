# Phase 6ARW Completion - Material Zone Environment Boundary

Date: 2026-05-28
Unit of Work: UOW-1655
Status: Complete after focused unit tests

## Scope

This unit added a non-live material-zone environment boundary for Java `GameTimeService`, `GameTime`, `DayTime`, `WeatherService`, and `WeatherEntry`.

The C# code models the time/weather metadata consumed by material actors. It does not start the Java clock, persist server variables, broadcast weather/time packets, mutate weather arrays, or query live creature zones.

## Completed Work

- Added `WorldMapRegionMaterialZoneEnvironmentPlanService`.
- Added environment context, weather-zone snapshot, weather-entry snapshot, environment plan, and environment status DTOs/enums.
- Modeled Java negative `GameTime` rejection as blocked metadata.
- Modeled Java day-time thresholds from in-game minutes.
- Corrected `WorldMapRegionMaterialZoneDayTime` to Java's four values: `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT`.
- Modeled Java `WeatherService.findWeatherEntry` first non-null WEATHER-zone selection and `WeatherEntry.NONE` fallback.
- Modeled Java `SUNNY` condition rain-prefix and before-state behavior.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, file ownership, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- `GameTime` rejects negative constructor input.
- `GameTime.calculateDayTime` returns `NIGHT` for hours greater than 21 or less than 4, `EVENING` for greater than 16, `AFTERNOON` for greater than 8, otherwise `MORNING`.
- `WeatherService.findWeatherEntry` scans creature zones, considers WEATHER zones, asks `DataManager.ZONE_DATA` for weather zone id, and returns the first non-null weather entry.
- If no WEATHER-zone entry is found, Java returns `WeatherEntry.NONE`.
- Material `SUNNY` means weather name is not `RAIN*` or the weather entry is in the before state.
- Java string matching is case-sensitive; lowercase `rain` is not treated as rain.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionMaterialZoneEnvironmentPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneActorPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneHandlerPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSerializationPlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneSavePlanServiceTests|FullyQualifiedName~WorldMapRegionMaterialZoneConstructionServiceTests"
```

Result: passed 38 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Material time/weather boundary model | new environment-plan service/tests plus actor day-time enum fix | Medium | Yes | Next material actor boundary after actor task metadata. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe candidate; deferred after sidecar notes from UOW-1654. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Broader zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
| --- | --- | --- | --- | --- |
| Orchestrator | Material time/weather boundary helper, tests, actor day-time enum correction, docs, commit | `WorldMapRegionMaterialZoneEnvironmentPlanService.cs`, `WorldMapRegionMaterialZoneEnvironmentPlanServiceTests.cs`, `WorldMapRegionMaterialZoneActorPlanService.cs`, `WorldMapRegionMaterialZoneActorPlanServiceTests.cs`, progress/handoff docs | Java source writes, live actor mutation, unrelated services/tests | Implemented and documented UOW-1655. |
| Sub-agents | None | None | All files | Not spawned because selected work required a small correction in an existing shared material actor enum/test. |

No sub-agent was spawned for UOW-1655 because the selected work touched an existing material actor enum/test in addition to new files, making single-owner edits safer.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlan_MapsGameMinutesToJavaDayTimeThresholds` | Added | In-game minute thresholds for night, morning, afternoon, evening, and night. | Static source review of Java `GameTime.calculateDayTime`. |
| `CreatePlan_BlocksNegativeGameTimeLikeJavaConstructor` | Added | Negative game time is blocked. | Static source review of Java `GameTime` constructor. |
| `CreatePlan_UsesFirstNonNullWeatherEntryFromWeatherZones` | Added | Non-weather zones are ignored, null weather entries are skipped, first non-null WEATHER entry wins. | Static source review of Java `WeatherService.findWeatherEntry`. |
| `CreatePlan_ModelsJavaSunnyConditionRainPrefixCaseSensitivity` | Added | Null/non-rain/rain-before/rain-active/lowercase-rain sunny matching. | Static source review of Java `AbstractMaterialSkillActor.matchActConditions`. |
| `WorldMapRegionMaterialZoneActorPlanServiceTests` day-time updates | Updated | Actor condition tests use `Morning` instead of the previous coarse `Day` value. | Static source review of Java `DayTime` enum. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.GameTimeService` | `WorldMapRegionMaterialZoneEnvironmentPlanService` | Time Service Boundary Plan | Partial | Unit Tested Metadata | Partial Parity | C# models the material-actor-facing `getGameTime().getDayTime()` boundary from supplied minutes. It does not load/store server variable `time`, start the clock, broadcast `SM_GAME_TIME`, or schedule periodic saves. |
| `com.aionemu.gameserver.utils.time.gametime.GameTime` | `WorldMapRegionMaterialZoneEnvironmentPlanService`; `WorldMapRegionMaterialZoneDayTime` | Game Time Utility Boundary | Partial | Unit Tested | Partial Parity | C# models negative-time rejection and day-time threshold calculation. It does not model year/month/day arithmetic, addMinutes side effects, equality/hash, clone, or weather updates on hour changes. |
| `com.aionemu.gameserver.utils.time.gametime.DayTime` | `WorldMapRegionMaterialZoneDayTime` | Enum | Partial | Unit Tested | Partial Parity | C# now exposes Java's four day-time values. Existing actor tests were updated from coarse `Day` to `Morning`. Serialization of enum names is not exercised. |
| `com.aionemu.gameserver.services.WeatherService.findWeatherEntry` | `WorldMapRegionMaterialZoneEnvironmentPlanService` | Weather Boundary Plan | Partial | Unit Tested Metadata | Partial Parity | C# models WEATHER-zone scan with first non-null entry and fallback to `WeatherEntry.NONE`. It does not query live creature zones, `DataManager.ZONE_DATA`, weather arrays, or broadcast `SM_WEATHER`. |
| `com.aionemu.gameserver.model.templates.world.WeatherEntry` | `WorldMapRegionMaterialZoneWeatherEntrySnapshot` | Weather DTO | Partial | Unit Tested Metadata | Partial Parity | C# carries weather name and before/after metadata needed by material SUNNY logic. It does not model rank/code/zone id behavior beyond DTO storage or JAXB serialization. |
| `com.aionemu.gameserver.model.templates.materials.MaterialActCondition` | `WorldMapRegionMaterialZoneEnvironmentPlan.SunnyConditionMatches`; `WorldMapRegionMaterialZoneActorPlanService` | Condition Boundary | Partial | Unit Tested | Partial Parity | C# models SUNNY's Java rain-prefix and before-state behavior plus NIGHT through day-time enum. It does not call live services or model future condition enum expansion. |

## Remaining Risks

- Environment planning is non-live and does not call actual `GameTimeService` or `WeatherService`.
- Java clock scheduling, server variable persistence, `SM_GAME_TIME`, `SM_WEATHER`, random weather generation, and weather broadcast behavior remain unported.
- `GameTime` year/month/day arithmetic, `addMinutes`, clone, equals/hash, and weather-on-hour-change behavior remain outside this material actor boundary.
- `WeatherService` random selection, before/after transition chains, snow condition, table lookup, and map weather arrays remain unported.
- C# returns blocked metadata for negative time rather than throwing Java `IllegalArgumentException`, because this helper is a non-live planner; live parity remains Needs Verification if/when implemented.
- Live material actors still consume supplied DTOs rather than live services.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 1 non-live material-zone environment-boundary helper, 1 enum correction, and 5 focused tests/updates.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 0 grouped rows explicitly marked Needs Verification; all rows are Partial Parity with documented non-live gaps.
- Total blocked artifacts: live `GameTimeService` clock/persistence/broadcasts, live `WeatherService` random weather/table/broadcast logic, live `GameTime` addMinutes and date arithmetic, live `WeatherEntry` table serialization, live material actor service integration, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Material weather transition plan | new helper/tests | Model `WeatherTable.getWeatherAfter`, `WeatherService.getRandomWeather`, before/after chains, snow filtering, and afternoon correction metadata. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live material weather transition plan.
- Why: material actors now have a boundary for current day/weather lookup; the next weather behavior is transition/random-selection metadata used by the weather service.
- Files: likely new helper/tests plus docs.
- Java source to read: `WeatherTable.getWeatherAfter`, `WeatherService.getRandomWeather`, `WeatherService.checkSnowCondition`, `WeatherEntry`, `GameTime.getMonth`, and `GameTime.getDayTime`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from material-zone environment files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Material weather transition plan | new helper/tests, docs | Java writes, live weather mutation |
| Read-only Agent | Charge-all DB rollback candidate audit | read-only docs/Java/C# test inspection | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live actor mutation, live weather mutation, live generated-zone writes, and live nearby dispatch: still high risk and intentionally disabled.
- Shared material helper files: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1655] Model material zone environment boundary
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneEnvironmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneEnvironmentPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionMaterialZoneActorPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionMaterialZoneActorPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARW-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
