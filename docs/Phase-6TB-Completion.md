# Phase 6TB Completion - UOW-1010 Quest Repeat Date Calculator

## Scope

UOW-1010 implements a pure C# calculator for Java `QuestService.calculateRepeatDate` daily/weekly reset selection. It remains detached from quest-finish mutation, persistence, system messages, packets, NPC faction completion, and live nearby refresh.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Pure repeat-date calculator | `QuestService.calculateRepeatDate`, `QuestRepeatCycle`, `ServerTime` | `QuestRepeatDateService.cs`, focused tests, docs | Utility implementation | Selected local-only | Low | Direct follow-up to UOW-1009 audit; no runtime mutation wiring. |
| B | Quest-finish mutation boundary | `QuestService.finishQuest`, `QuestState`, DAO store, packets | Future service/repository/packet tests | Service port | Not with A | High | Requires reward, quest-state, packet, callback, persistence, and nearby refresh wiring. |
| C | Server-timezone production policy | `GSConfig.TIME_ZONE_ID`, `ServerTime` | options/runtime time service | Config/runtime port | Yes after A | Medium | Needs Java config mapping and DST coverage before production use. |
| D | NPC faction completion lifecycle | `NpcFactions.completeQuest`, reset/assignment | Future faction lifecycle files | Service port | No | High | Depends on quest completion and persistence write paths. |

Selected batch: local-only A. No sub-agent was spawned because the change was a narrow pure utility plus tests.

## Java Breadcrumbs

- `QuestService.calculateRepeatDate` starts from the next server-time 09:00 reset candidate.
- Java checks `now.isAfter(repeatDate)`, so exactly 09:00 uses the same-day reset.
- `QuestTemplate.isDaily()` is true when `repeat_cycle` contains `ALL`.
- Weekly cycles sort configured weekdays and choose the first day greater than or equal to the reset candidate's weekday, wrapping to the first configured day next week when needed.

## Implementation

- Added `QuestRepeatDateService.CalculateNextRepeatTime`.
- The method accepts an explicit `DateTimeOffset now`, repeat-cycle token list, and `TimeZoneInfo serverTimeZone`.
- Implemented Java day mapping for `MON` through `SUN`; `ALL` returns the daily 09:00 reset even when other tokens are present.
- Invalid/empty weekly cycles throw instead of guessing.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestRepeatDateServiceTests` | Passed, 9 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1724 tests |

## Migration Parity Table - UOW-1010

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.calculateRepeatDate` | `Aion.GameServer.Services.QuestRepeatDateService.CalculateNextRepeatTime` | Utility / Quest Timing | Partial | Unit Tested | Partial Parity | Implements source-reviewed reset-date selection for daily and weekly cycles. Not wired to quest completion, messages, persistence, or Java runtime comparison. |
| `com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle` | `QuestRepeatDateService` token mapping; `NearbyQuestTemplateSummary.RepeatCycle` | Enum / XML Token Dependency | Partial | Unit Tested | Partial Parity | Maps `MON` through `SUN` and treats `ALL` as daily. Unknown tokens are ignored for weekly selection, then empty weekly cycles fail closed. Full enum/l10n packet messaging remains missing. |
| `com.aionemu.gameserver.utils.time.ServerTime` | `TimeZoneInfo` argument to `QuestRepeatDateService` | Utility / Timezone | Partial | Unit Tested | Needs Verification | The calculator accepts an explicit timezone, but production `GSConfig.TIME_ZONE_ID` plumbing and DST edge tests remain missing. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_AppliesJavaDailyNineAmReset` | Unit | Daily reset before, exactly at, and after 09:00. | Source-reviewed Java `now.isAfter(repeatDate)` behavior. |
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_TreatsAllAsDailyEvenWhenWeekdaysArePresent` | Unit | `ALL` dominates other repeat-cycle tokens like Java `contains(ALL)`. | Source-reviewed Java `QuestTemplate.isDaily`. |
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_AppliesJavaWeeklySelectionFromResetCandidate` | Unit | Weekly weekday selection uses the 09:00 reset candidate's weekday and wraps when needed. | Source-reviewed Java `findNextRepeatDay`. |
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_RequiresWeekdayForWeeklyCycles` | Unit | Empty weekly cycles fail closed. | Defensive C# behavior for malformed staged input; Java valid JAXB data is expected to have repeat tokens. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Production server-timezone mapping is not wired.
- DST transition behavior needs dedicated tests once production timezone source is selected.
- Quest completion mutation, persistence, packets, quest callbacks, NPC faction completion, and live nearby refresh remain unported.

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported: 1 staged pure utility
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Begin a staged quest-finish state mutation planner that calls the pure repeat-date calculator without sending live nearby packets, or audit NPC faction completion lifecycle first. Keep persistence writes, packet sends, NPC faction completion writes, and ItemPurification dispatch disabled until each is separately covered.
