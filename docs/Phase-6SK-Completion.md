# Phase 6SK Completion - Staged Nearby Quest Template XML Extractor

Date: May 25, 2026
Unit of Work: UOW-993

## Session Summary

This unit added a staged XML extractor for the `NearbyQuestTemplateSummary` fields consumed by the partial nearby start-condition predicate.

Java remains the source of truth. This unit does not wire production `StaticData`, `DataManager`, player-controller sends, `SM_NEARBY_QUESTS`, or production ItemPurification dispatch.

## Parallel Work Discovery

The selected unit was the staged extractor. Production `StaticData.cs` integration was deliberately excluded because it is a shared loader file and needs exclusive ownership after the extractor is proven.

No sub-agents were spawned.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/dataholders/QuestsData.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`

## Completed Work

- Added `NearbyQuestTemplateXmlExtractor`.
- Added tests for:
  - representative quest XML field extraction
  - default values for missing optional fields
  - stream input feeding `NearbyQuestTemplateTable`
- Updated nearby-refresh/readiness/progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests` passed with 3 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1693 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor`; `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary` | Dataholder / DTO / XML Extractor | Partial | Unit Tested | Needs Verification | Staged extractor reads only fields needed by the current nearby predicate boundary. JAXB validation, production `StaticData` integration, enum mapping from real data, optional condition counting, category defaults, master-crafting adjustment, collect/inventory details, and repeat-cycle timing remain unported. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `NearbyQuestTemplateXmlExtractor` | Dataholder / Index | Partial | Unit Tested | Needs Verification | Extracted summaries can feed the staged table boundary, matching the broad Java `QuestsData` index shape. It does not replace Java JAXB `afterUnmarshal`, sorted NPC-faction indexing, or production C# static-data loading. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `NearbyQuestTemplateSummary` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | The extractor supplies staged inputs for already-modeled early gates, but XML start conditions, inventory checks, combine skill, NPC faction, warning packets, exception/log behavior, and repeat timing remain unsupported. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate` JAXB attributes/elements | Validates staged extraction of quest id, min/max level, race, class list, gender, rank, repeat count, repeat-cycle presence, XML condition presence, inventory item presence, combine skill, and NPC faction id. | Deterministic C# test from reviewed Java `QuestTemplate` annotations/getters. | Does not run JAXB or real-data audit. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Java `QuestTemplate` primitive/default field values | Validates missing optional fields map to staged defaults, including max repeat count `1`. | Deterministic C# test from source-reviewed Java field defaults. | Does not validate all `QuestTemplate` fields. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_StreamInputFeedsNearbyQuestTemplateTableAndPredicate` | Unit | Java `QuestsData` indexing shape | Validates stream input can feed the staged table boundary. | C# staged boundary test informed by Java `QuestsData.afterUnmarshal`. | Not production `StaticData` integration. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged extractor is not integrated into production `StaticData`, `DataManager`, player-controller refresh, packet sends, or ItemPurification dispatch.
- Real repository quest-template extraction counts are not pinned yet.
- XML start-condition, inventory item, combine-skill, NPC faction, and repeat-cycle semantics remain unsupported beyond presence flags.
- Java JAXB schema validation and post-unmarshal behavior are not executed.
- Enum/string mapping for Java `Race`, `PlayerClass`, and `Gender` needs real XML data audit coverage before production use.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 3 in this unit
- Total artifacts ported: 1 staged XML extractor in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3
- Total blocked artifacts: 4 blocked/not-started categories, including production quest-template loading, real-data audit, XML start conditions, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds staged XML extraction for nearby predicate inputs without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a real-data audit for `NearbyQuestTemplateXmlExtractor` over repository quest XML, pinning counts for templates and unsupported-dependency flags before any production `StaticData` integration. Keep XML start conditions, NPC faction, combine skill, packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Real-data audit for `NearbyQuestTemplateXmlExtractor`.
- Why: Counts and unsupported-dependency flags need to be pinned before production `StaticData` integration.
- Files: likely a new test file or an extension to nearby template extractor tests plus docs.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Real-data extractor audit | isolated test file/docs | Medium | Sequential if it updates shared docs. |
| B | Read-only XMLStartCondition dependency expansion | read-only Java/docs | Low | Can run in parallel with extractor audit. |
| C | ItemPurification side-effect persistence gap audit | docs/read-only repository inspection | Medium | Far from nearby predicate files. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add real-data extractor audit and docs | New/selected test file and Phase 6 docs | Production `StaticData.cs`, packet sends, ItemPurification dispatch |
| Agent A | Read-only XMLStartCondition dependency expansion | Read-only Java/docs | All writes |

### Do Not Parallelize

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`: shared loader file; production integration needs exclusive ownership.
- `NearbyQuestTemplateXmlExtractor.cs`: one owner at a time while real-data audit may adjust parsing.
- Phase 6 progress/handoff docs: orchestrator-owned.
