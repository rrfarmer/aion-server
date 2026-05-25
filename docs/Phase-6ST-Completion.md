# Phase 6ST Completion - UOW-1002 Nearby Repeat Timing

Date: May 25, 2026

## Scope

Implemented the narrow staged repeat-timing subset needed by nearby start checks, using Java `QuestState.canRepeat()` as the source of truth.

This unit intentionally did not enable live nearby quest packet sends, `CM_LEVEL_READY` dispatch, delayed NPC-spawn refresh, production `StaticData` loading, or production `CM_ITEM_PURIFICATION` quest refresh dispatch.

## Java Source Breadcrumbs

- `com.aionemu.gameserver.questEngine.model.QuestState.canRepeat()` returns false when `completeCount >= template.getMaxRepeatCount()` unless `maxRepeatCount == 255`.
- For time-based repeat quests, Java returns false only when `nextRepeatTime != null` and current wall-clock time is before that timestamp.
- A null `nextRepeatTime` is repeatable immediately, and equality at reset time passes.
- `com.aionemu.gameserver.dao.PlayerQuestListDAO.SELECT_QUERY` loads `next_repeat_time`, `reward`, and `complete_time`.
- `com.aionemu.gameserver.model.templates.QuestTemplate.repeat_cycle` is a list of `QuestRepeatCycle` enum tokens such as `ALL`, `MON`, and `WED`.

## Implementation

- Added nullable `NextRepeatTime` and `CompleteTime` to `PlayerQuestState`.
- Updated `MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` to select and hydrate `next_repeat_time` and `complete_time`.
- Added `RepeatCycle` token preservation to `NearbyQuestTemplateSummary` and `NearbyQuestTemplateXmlExtractor`.
- Updated `NearbyQuestStartConditionService` so completed time-based nearby quests evaluate Java repeat timing instead of returning `UnsupportedRepeatTiming`.
- Added deterministic `now` injection to the nearby start-condition check for repeat timing tests.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/PHASE-6-PROGRESS.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestMarkerProjectionServiceTests"` | Passed, 23 tests |

## Migration Parity Table - UOW-1002

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.model.QuestState.canRepeat` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Quest State / Predicate | Partial | Unit Tested | Partial Parity | C# now matches the deterministic nearby start subset: max-repeat exhaustion, unlimited `255`, null `nextRepeatTime`, future timestamp block, and equality-at-reset pass. Java runtime comparison is still blocked. |
| `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | DTO / Quest State | Partial | Unit Tested; Integration Tested when DB flag enabled | Partial Parity | Adds nullable `NextRepeatTime` and `CompleteTime`. Mutation on quest completion and persistence updates remain outside this slice. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.load` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` | Repository / Quest State Hydration | Partial | Integration Tested when DB flag enabled | Partial Parity | Selects Java-schema `next_repeat_time` and `complete_time`. The DB integration assertion is gated; normal local tests compile it and return early. Timezone interpretation of unspecified MySQL `DateTime` values needs live DB verification. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor` | Dataholder / Enum-like XML Dependency | Partial | Unit Tested | Partial Parity | Preserves parsed repeat-cycle tokens such as `ALL`, `MON`, and `WED`; C# does not yet calculate the next reset day at quest finish. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Completed time-based nearby quests now use staged repeat timing instead of failing closed. Combine-skill, NPC faction, warning packets, exception/log behavior, production static-data loading, and live send triggers remain unsupported. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively` | Unit | Java `QuestState.canRepeat` | Validates START/REWARD blocking, max-repeat exhaustion, `255` unlimited repeat count, time-based null next-repeat pass, future next-repeat block, and equality-at-reset pass. | Deterministic C# test from source-reviewed Java predicate. | Does not compare against a running Java server. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate.repeat_cycle` JAXB field and `QuestRepeatCycle` enum names | Validates repeat-cycle token preservation from XML. | Deterministic C# XML extractor test. | Does not validate JAXB runtime conversion. |
| `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAndRepeatTimesAgainstJavaSchema_WhenEnabled` | Integration | Java `PlayerQuestListDAO.SELECT_QUERY` | Validates reward group, next-repeat time, and complete time hydration when DB integration is enabled. | Gated Java-schema integration assertion. | Skips unless `AION_GAMESERVER_DB_INTEGRATION=1`; timezone behavior still needs live DB verification. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Repeat timing is staged for nearby start checks only; quest-finish repeat-date calculation and reset notification packets are not ported.
- `DateTimeKind.Unspecified` MySQL timestamp interpretation uses the local offset until repository-level server-timezone plumbing exists.
- Production `StaticData`/JAXB loading remains unwired.
- Combine-skill checks and NPC faction checks remain unsupported.
- Packet sends, `CM_LEVEL_READY`, NPC-spawn delayed refresh, production player-controller refresh, and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 3 staged partial artifacts in this unit: repeat predicate support, repeat-cycle token preservation, and quest timestamp hydration
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/not-started categories: quest-finish repeat-date calculation, server-timezone DB verification, combine-skill predicates, NPC faction predicates, and live nearby send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit removes the nearby start-check repeat timing blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add the next narrow nearby predicate dependency:

- Combine-skill/master-crafting checks.
- NPC faction checks.
- Broader representative-player refresh-plan audits across real quest data.

Keep packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.
