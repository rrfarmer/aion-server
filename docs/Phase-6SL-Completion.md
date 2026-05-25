# Phase 6SL Completion - Nearby Quest Template Real-Data Audit

Date: May 25, 2026
Unit of Work: UOW-994

## Session Summary

This unit added a real-data regression audit for the staged `NearbyQuestTemplateXmlExtractor`.

Java remains the source of truth. This unit does not wire production `StaticData`, `DataManager`, player-controller sends, `SM_NEARBY_QUESTS`, or production ItemPurification dispatch.

## Completed Work

- Extended `NearbyQuestTemplateXmlExtractorTests`.
- Added `RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring`.
- Pinned current `game-server/data/static_data/quest_data/quest_data.xml` staged extraction counts.
- Updated nearby-refresh/readiness/progress docs.

## Audit Counts

| Metric | Count |
|---|---:|
| Quest template summaries | 8043 |
| Templates with XML start conditions | 2427 |
| Templates with inventory item preconditions | 363 |
| Templates with combine-skill requirements | 628 |
| Templates with NPC faction requirements | 365 |
| Time-based repeat templates | 927 |
| Race-restricted templates | 7431 |
| Class-restricted templates | 483 |
| Gender-restricted templates | 18 |
| Abyss-rank restricted templates | 12 |
| Templates with nonzero min level | 8043 |
| Templates with nonzero max level | 1355 |
| Largest class-permitted list | 16 |

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests` passed with 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1694 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor`; `NearbyQuestTemplateXmlExtractorTests` | Dataholder / XML Extractor | Partial | Regression Tested | Needs Verification | Real-data audit pins staged extractor counts over current `quest_data.xml`. JAXB validation, production `StaticData` integration, enum mapping verification, optional condition counting, category defaults, master-crafting adjustment, collect/inventory details, and repeat-cycle timing remain unported. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `NearbyQuestTemplateXmlExtractorTests` | Dataholder / Index | Partial | Regression Tested | Needs Verification | Audit proves 8043 extracted summaries can feed the staged table count. It does not execute Java `afterUnmarshal`, sorted NPC-faction indexing, or production C# static-data loading. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `NearbyQuestTemplateSummary` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Real-data audit highlights how many templates still hit unsupported staged dependencies. Predicate is not invoked over real player state in this unit. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestTemplateXmlExtractorTests.RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring` | Regression | Real repository `quest_data.xml` and Java `QuestTemplate` fields | Pins staged extractor counts: 8043 summaries; 2427 XML-condition templates; 363 inventory templates; 628 combine-skill templates; 365 NPC-faction templates; 927 time-based templates; race/class/gender/rank/min/max-level counts. | Deterministic C# audit over current repository XML. | Does not run Java JAXB, production `StaticData`, XML condition predicates, or packet sends. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged extractor and audit are not integrated into production `StaticData`, `DataManager`, player-controller refresh, packet sends, or ItemPurification dispatch.
- XML start-condition, inventory item, combine-skill, NPC faction, and repeat-cycle semantics remain unsupported beyond presence/count flags.
- Java JAXB schema validation and post-unmarshal behavior are not executed.
- Enum/string mapping for Java `Race`, `PlayerClass`, and `Gender` is counted but not runtime-verified against Java JAXB output.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 0 new production artifacts in this unit; 1 real-data regression audit added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories, including production quest-template loading, XML start conditions, repeat timing, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit pins staged quest-template extraction counts without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged candidate-marker bridge that combines world-instance quest ids, `NearbyQuestTemplateTable`, and `NearbyQuestStartConditionService` into candidate markers without sending `SM_NEARBY_QUESTS`. Keep XML start conditions, NPC faction, combine skill, packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Add staged candidate-marker bridge.
- Why: The source ids, quest-template summaries, and partial predicate now exist, but no offline bridge produces marker DTOs yet.
- Files: new service/test files only; avoid packet sends and production dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Candidate-marker bridge | new service/test files | Medium | Sequential if it uses current predicate service. |
| B | Read-only XMLStartCondition dependency expansion | read-only Java/docs | Low | Can run independently. |
| C | ItemPurification side-effect persistence gap audit | docs/read-only repository inspection | Medium | Far from nearby predicate files. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add candidate-marker bridge and docs | New service/test files and Phase 6 docs | Production `StaticData.cs`, packet sends, ItemPurification dispatch |
| Agent A | Read-only XMLStartCondition dependency expansion | Read-only Java/docs | All writes |

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestTemplateXmlExtractor.cs`: avoid parser edits while bridge consumes its output.
- Phase 6 progress/handoff docs: orchestrator-owned.
