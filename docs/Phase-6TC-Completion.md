# Phase 6TC Completion - UOW-1011 Quest Repeat Timezone Plumbing

## Scope

UOW-1011 connects the existing Java `gameserver.timezone` option surface to the pure quest repeat-date calculator. It does not wire quest completion, persistence, packets, NPC faction completion, or live nearby refresh.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Production timezone resolver for repeat dates | `GSConfig.TIME_ZONE_ID`, `ServerTime`, `QuestService.calculateRepeatDate` | `GameServerOptions.cs`, `QuestRepeatDateService.cs`, option/repeat-date tests, docs | Utility / Config Port | Selected sequential | Medium | Touches shared config/time utility surface; kept exclusive. |
| B | DST edge tests for pure calculator | `ServerTime`, Java `ZonedDateTime` reset behavior | `QuestRepeatDateServiceTests.cs` | Test Creation | Included in A | Low | Test-only but depends on chosen timezone API. |
| C | Staged quest-finish mutation boundary | `QuestService.finishQuest`, `QuestState`, `PlayerQuestListDAO` | Future quest service/repository tests | Service Port | No | High | Too broad until timezone policy is pinned. |
| D | NPC faction completion lifecycle audit | `NpcFactions.completeQuest`, `reset`, assignment | docs/read-only | Java Analysis | Yes | Medium | Independent, but less immediately useful than timezone plumbing. |

Selected batch: local-only A with focused tests. No sub-agent was spawned because the selected work modified shared config/time helper APIs.

## Java Breadcrumbs

- `GSConfig.TIME_ZONE_ID` is bound from `gameserver.timezone`.
- Empty `gameserver.timezone` means system timezone in the Java configuration layer.
- `ServerTime.now()` uses `GSConfig.TIME_ZONE_ID`.
- `QuestService.calculateRepeatDate` uses `ServerTime.now()` and therefore must use the configured game-server timezone, not arbitrary local time.

## Implementation

- Added `GameServerCoreOptions.GetTimeZone()`.
- Added `QuestRepeatDateService.CalculateNextRepeatTime(DateTimeOffset, IReadOnlyList<string>, GameServerOptions)`.
- Extended game-server config tests to prove `mygs.properties` can override `gameserver.timezone`.
- Added repeat-date tests proving the calculator consumes configured options and preserves daylight-saving offset for a configured IANA timezone.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestRepeatDateServiceTests|FullyQualifiedName~GameServerOptionsTests"` | Passed, 15 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1726 tests |

## Migration Parity Table - UOW-1011

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.configs.main.GSConfig.TIME_ZONE_ID` | `Aion.GameServer.Configuration.GameServerCoreOptions.TimeZoneId`; `GetTimeZone()` | Config / Timezone | Partial | Unit Tested | Partial Parity | C# already loads `gameserver.timezone`; this unit exposes a resolver for production use. Invalid timezone exception parity and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.utils.time.ServerTime` | `GameServerCoreOptions.GetTimeZone()` plus `QuestRepeatDateService` option overload | Utility / Timezone | Partial | Unit Tested | Partial Parity | Repeat-date calculation can now use configured game-server timezone. Broader Java `ServerTime` helpers remain unported. |
| `com.aionemu.gameserver.services.QuestService.calculateRepeatDate` | `Aion.GameServer.Services.QuestRepeatDateService.CalculateNextRepeatTime` | Utility / Quest Timing | Partial | Unit Tested | Partial Parity | Adds options-based timezone path and DST-offset coverage. Still not wired to quest completion, reset messages, persistence, or Java runtime comparison. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `GameServerOptionsTests.LoadFromJavaConfig_AppliesMyGsOverridesLast` | Unit | `gameserver.timezone` can be overridden through `mygs.properties` and resolved to `TimeZoneInfo`. | Java config property source reviewed; no Java runtime comparison. |
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_UsesConfiguredGameServerTimezone` | Unit | Repeat-date calculator overload uses `GameServerOptions.Core.GetTimeZone()`. | Source-reviewed Java `ServerTime.now()` path through `GSConfig.TIME_ZONE_ID`. |
| `QuestRepeatDateServiceTests.CalculateNextRepeatTime_PreservesDaylightSavingOffsetForConfiguredTimezone` | Unit | Configured timezone offset is applied to generated 09:00 reset instants. | Deterministic C# test for the Java `ZonedDateTime` timezone requirement; no Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Full Java `ServerTime` utility is not ported.
- Quest completion mutation, persistence, reset system messages, packets, quest callbacks, NPC faction completion, and live nearby refresh remain unported.
- Timezone ID compatibility depends on .NET `TimeZoneInfo` support for the configured Java ID; invalid-ID behavior is not yet compared to Java config binding.

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported: 2 staged partial artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 71%

## Next Recommended Unit Of Work

Begin a staged quest-finish state mutation planner that calls the pure repeat-date calculator for time-based quests, but still does not persist, send packets, invoke quest callbacks, complete NPC faction state, or refresh nearby quests. Alternatively, audit NPC faction completion lifecycle before wiring quest-finish behavior.
