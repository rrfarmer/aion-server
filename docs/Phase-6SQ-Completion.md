# Phase 6SQ Completion - Staged Nearby XML Start Conditions

Date: May 25, 2026
Unit of Work: UOW-999
Commit message: `[Phase 6][UOW-999] Stage nearby XML start conditions`

## Session Summary

This unit implemented the narrow staged XML start-condition subset needed by nearby quest filtering. Java remains the source of truth. The C# predicate now evaluates the reviewed `XMLStartCondition.check(player, warn=false)` subset, while unknown XML children and still-missing dependencies continue to fail closed.

Production `SM_NEARBY_QUESTS` sends, `CM_LEVEL_READY` nearby integration, NPC-spawn delayed refresh, production `StaticData`/`DataManager` wiring, and production ItemPurification dispatch remain disabled.

## Completed Work

- Added staged XML condition DTOs:
  - `NearbyQuestXmlStartCondition`
  - `NearbyQuestFinishedCondition`
- Extended `NearbyQuestTemplateXmlExtractor` to parse:
  - `finished`
  - `unfinished`
  - `noacquired`
  - `acquired`
  - `equipped`
  - `required_title`
- Added fail-closed tracking for unknown XML start-condition children.
- Extended `NearbyQuestStartConditionService` to evaluate:
  - `finished` COMPLETE state checks
  - `finished reward="..."` reward-group matching
  - repeatable prerequisite exact max-complete-count checks, except `255`
  - `unfinished` COMPLETE blocking
  - `noacquired` START/REWARD blocking
  - `acquired` non-LOCKED existing quest-state checks
  - Java nearby `equipped` no-op behavior because `warn = false`
  - `required_title` displayed-title checks
  - Java required-condition count behavior: all mandatory blocks plus one optional `finished` block
- Added nullable `RewardGroup` to `PlayerQuestState` for staged reward matching.
- Updated Phase 6 parity docs and nearby/dispatch readiness docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestMarkerProjectionServiceTests"` passed with 19 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1706 tests.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SQ-Completion.md`

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | `Aion.GameServer.Dataholders.NearbyQuestXmlStartCondition`; `Aion.GameServer.Services.NearbyQuestStartConditionService` | Dataholder / Predicate | Partial | Unit Tested | Partial Parity | Implements staged nearby `warn = false` support for finished/unfinished/noacquired/acquired/required-title checks, optional-block counting, and ignored equipped checks. Unknown XML children fail closed. Inventory items, combine skill, NPC faction, master-crafting required-count adjustment, warning packets, production static-data loading, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.templates.quest.FinishedQuestCond` | `Aion.GameServer.Dataholders.NearbyQuestFinishedCondition` | DTO / XML Predicate Dependency | Partial | Unit Tested | Partial Parity | Parses `quest_id` and default/explicit `reward`, and evaluates staged reward-group matching. Production reward-group loading into `PlayerQuestState` is not verified. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor`; `NearbyQuestTemplateTable` | Dataholder / XML Extractor | Partial | Unit Tested; Regression Tested | Needs Verification | Extractor now preserves supported XML start-condition fields and marks unknown XML children unsupported. Production JAXB/`StaticData`, enum mapping, master-crafting metadata, inventory/combine/NPC-faction data, and live loading remain unverified. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Predicate now covers early gates plus staged XML subset and still reports unsupported dependencies explicitly. Time-based repeat cooldowns, inventory checks, combine-skill, NPC faction, exception/log behavior, warning packets, and production integration remain unsupported. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` | Player Quest State | Partial | Unit Tested | Needs Verification | Adds nullable reward group for staged XML `finished reward` matching. Repository hydration/persistence and next-repeat-time behavior are not ported or verified. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestMarkerProjectionService`; `NearbyQuestRefreshPlanService` | Controller / Quest UI Projection Dependency | Partial | Unit Tested | Partial Parity | Existing projection/plan services automatically consume the broader staged predicate. No live map-region lookup, `SM_NEARBY_QUESTS` send, `CM_LEVEL_READY` trigger, delayed NPC-spawn refresh, or ItemPurification dispatch is enabled. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate` and `XMLStartCondition` JAXB fields | Now validates supported XML start-condition child parsing in addition to existing nearby template fields. | Deterministic C# test from source-reviewed Java annotations and fields. | No Java JAXB runtime comparison. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Java `QuestTemplate` defaults | Validates empty XML condition list and unsupported-child flag defaults. | Deterministic C# test. | Not all Java `QuestTemplate` fields are modeled. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_MarksUnknownXmlStartConditionChildrenUnsupported` | Unit | Conservative unsupported XML policy | Validates unknown XML condition children are not treated as supported. | Safety test preventing optimistic parity. | Unknown/future Java XML fields remain unimplemented. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesSupportedJavaXmlStartConditions` | Unit | Java `XMLStartCondition.check(player, warn=false)` | Validates finished/reward/repeat prerequisite, unfinished, noacquired, acquired, required-title, and equipped no-op behavior. | Deterministic C# test from source-reviewed Java predicate. | Production reward-group hydration and Java runtime comparison remain missing. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaXmlStartConditionFailures` | Unit | Java `XMLStartCondition` failure branches | Validates missing finished quest, wrong reward group, complete unfinished quest, acquired noacquired quest, locked acquired quest, and wrong title failures. | Deterministic C# test from source-reviewed Java predicate. | Warning packets intentionally absent for nearby `warn = false`. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_RequiresAllMandatoryAndOneOptionalXmlBlockLikeJava` | Unit | Java `QuestTemplate.getRequiredConditionCount` | Validates all mandatory XML blocks plus one optional finished block are required. | Deterministic C# test from source-reviewed Java formula. | Master-crafting adjustment remains blocked by combine-skill support. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_FailsClosedForUnknownXmlStartConditionChildren` | Unit | Conservative unsupported XML policy | Validates unknown XML children return `UnsupportedXmlStartConditions`. | C# fail-closed test. | Does not implement unknown/future Java fields. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- XML start-condition support is staged and partial, not production-wired.
- Production `StaticData`/`DataManager` loading for these new XML DTOs is not wired.
- `PlayerQuestState.RewardGroup` is staged, but repository load/save behavior for reward groups is not verified.
- Inventory item preconditions, combine-skill checks, NPC faction checks, and time-based repeat cooldowns remain unsupported.
- Master-crafting required-condition-count adjustment remains blocked by missing combine-skill/skill-point support.
- Warning packets are intentionally absent because nearby checks use `warn = false`; non-nearby quest acquisition remains outside this staged service.
- Packet sends, `CM_LEVEL_READY`, NPC-spawn delayed refresh, production player-controller refresh, and ItemPurification dispatch remain disabled.
- Java `HashMap`/set ordering is not claimed.
- Reflection/dynamic handler execution and production startup integration remain unported.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 3 staged partial artifacts in this unit: `NearbyQuestXmlStartCondition`, `NearbyQuestFinishedCondition`, and `PlayerQuestState.RewardGroup`
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 6 blocked/not-started categories: production static-data integration, reward-group repository hydration, inventory item predicates, combine-skill predicates, NPC faction predicates, and live nearby send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit reduces one nearby predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add the next narrow nearby predicate dependency: inventory item preconditions, combine-skill checks, NPC faction checks, production reward-group hydration for `PlayerQuestState`, or broader refresh-plan audits across representative player archetypes.

Keep packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Add production reward-group hydration for `PlayerQuestState` or stage inventory item preconditions.
- Why: XML `finished reward` is now supported only for staged in-memory quest states. Production loading still needs reward groups before live nearby filtering could rely on reward-gated prerequisites.
- Files: repository quest-state loading tests/service files if choosing hydration, or staged template extractor/predicate tests if choosing inventory preconditions.
- Guardrail: Do not wire live sends or production ItemPurification dispatch.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Reward-group repository hydration audit | repository/load tests and docs | Medium | Avoid if another task edits `PlayerQuestState`. |
| B | Inventory precondition staged extractor/predicate design | docs/read-only Java plus staged dataholder tests | Medium | Keep production inventory mutation untouched. |
| C | Broader refresh-plan archetype audit | audit tests/docs | Medium | Avoid central predicate edits in parallel. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement one selected predicate/hydration slice and update shared docs | selected service/dataholder/test files, Phase 6 docs | Production send paths unless this becomes the sole owner |
| Agent A | Read-only inventory/combine/NPC-faction predicate checklist | Java source and docs only | All writes |

If no sub-agent tool is available, do the recommended task sequentially.

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestTemplateXmlExtractor.cs`: central staged extractor; one owner at a time.
- `NearbyQuestTemplateTable.cs`: central staged template shape; one owner at a time.
- `PlayerQuestState.cs`: shared quest-state DTO; one owner at a time.
- `GameServerConnection.cs`: production send path; one owner only.
- `GameClientSocketServer.cs`: connection registry/send infrastructure; one owner only.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6SQ-Completion.md`, `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, `docs/QuestStartConditions-Nearby-Audit.md`, and `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- `docs/commit-conventions.md` is still missing; use the commit format in `docs/orchestration-rules.md`.
- Production `CM_ITEM_PURIFICATION` dispatch and real nearby-refresh packet sends must remain disabled.
- The next strongest nearby quest move is reward-group hydration or another staged predicate dependency, not live sends.
