# Phase 6SJ Completion - Staged Nearby Predicate Boundary

Date: May 25, 2026
Unit of Work: UOW-992

## Session Summary

This unit added a staged C# nearby quest-template boundary and a partial nearby start-condition predicate for the deterministic early gates in Java `QuestService.checkStartConditions`.

Java remains the source of truth. This unit does not wire production `StaticData`, `DataManager`, player-controller sends, `SM_NEARBY_QUESTS`, or production ItemPurification dispatch.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Minimal nearby quest-template boundary plus level diff | `QuestTemplate`, `QuestService.getLevelRequirementDiff` | New dataholder/service plus isolated tests | Implementation/Test | Partly | Medium | Small and directly next, but implementation and tests needed to be designed together. |
| B | StaticData quest-template extraction | `QuestsData`, `QuestTemplate` XML/JAXB fields | `StaticData.cs`, `StaticDataLoadingTests.cs` | Integration | No | High | Shared loader file needs exclusive ownership. |
| C | XMLStartCondition dependency audit or DTO sketch | `XMLStartCondition`, `FinishedQuestCond` | Docs or isolated DTOs | Analysis/DTO | Yes | Low-Med | Independent, but predicate needed the minimal template boundary first. |
| D | ItemPurification side-effect persistence gap | Storage/AP docs and repository paths | Docs/read-only or separate repo tests | Analysis | Yes | Medium | Far apart from current nearby predicate chain. |
| E | Java observer tooling | Packet/DB observer design | Java tooling/docs | Analysis/Tooling | No | Blocked | Still blocked by local Java/Maven tooling constraints. |

Selected unit: Candidate A, executed sequentially by the orchestrator.

## File Ownership Map

| Agent | Scope | Allowed Files | Forbidden Files | Expected Output |
|---|---|---|---|---|
| Orchestrator | Staged nearby predicate boundary | `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`; `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`; `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`; Phase 6 docs | Production `StaticData.cs` integration; packet send paths; ItemPurification dispatch | Code, tests, docs, commit |

No sub-agents were spawned because there was no safe multi-file implementation split that avoided shared design and documentation ownership.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/model/templates/QuestTemplate.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestState.java`
- `game-server/src/com/aionemu/gameserver/questEngine/model/QuestStatus.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/XMLStartCondition.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`

## Completed Work

- Added `NearbyQuestTemplateTable` and `NearbyQuestTemplateSummary`.
- Added `NearbyQuestStartConditionService.CheckNearbyStartConditions`.
- Added `NearbyQuestStartConditionService.GetLevelRequirementDiff`.
- Added focused tests for:
  - Java's early template gates
  - quest state and repeat-count gates
  - unsupported Java dependencies
  - level-diff behavior
- Updated nearby-refresh/readiness/progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestStartConditionServiceTests` passed with 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1690 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Stages only the nearby UI early gates. XML start conditions, inventory item checks, combine-skill checks, NPC faction checks, warning packets, exception/log behavior, and time-based repeat cooldowns are unsupported and explicitly reported. No production send path uses this service. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `Aion.GameServer.Services.NearbyQuestStartConditionService.GetLevelRequirementDiff` | Utility / Quest Predicate | Partial | Unit Tested | Partial Parity | Tests Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior. Production quest-template loading and packet marker generation remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `Aion.GameServer.Dataholders.NearbyQuestTemplateTable` | Dataholder / DTO | Partial | Unit Tested | Needs Verification | Staged DTO covers only early nearby predicate fields and unsupported-dependency flags. Production XML/JAXB loading, enum mapping from static data, optional condition counting, category defaults, master-crafting adjustment, collect/inventory details, and repeat-cycle timing remain unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | No Tests | Unknown | Still not implemented. The staged predicate reports `UnsupportedXmlStartConditions` when the staged template says XML conditions exist. Equipped-item `warn = false` behavior remains documented but untested in executable predicate code. |
| `com.aionemu.gameserver.model.gameobjects.player.QuestStateList`; `com.aionemu.gameserver.questEngine.model.QuestState` | `Aion.GameServer.Model.GameObjects.PlayerQuestState`; `NearbyQuestStartConditionService` | Player Quest State | Partial | Unit Tested | Partial Parity | Active and reward states block, and max-repeat exhaustion is staged from complete count/template max count. Java `nextRepeatTime`, repeat-cycle timing, complete timestamps, reward-group behavior, and persistence state are not modeled. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaBasicQuestTemplateGates` | Unit | Java `QuestService.checkStartConditions` early template gates | Validates race, min/max level with nearby grace, class, gender, and abyss-rank gate behavior. | Deterministic C# test from source-reviewed Java predicate order. | Does not load real quest XML or evaluate XML start conditions. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively` | Unit | Java `QuestState.canRepeat` and active-state checks | Validates `START`/`REWARD` blocks, nonrepeat complete blocks, repeat-count allowance, and time-based repeat cooldown is reported unsupported. | Deterministic C# test from source-reviewed Java state/repeat branches. | Does not model next-repeat timestamps. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_ReportsUnsupportedJavaDependenciesInsteadOfAssumingParity` | Unit | Java XML/inventory/combine-skill/NPC-faction predicate dependencies | Validates staged predicate fails explicitly for dependencies not yet ported. | Conservative C# test preventing optimistic parity claims. | Does not implement those dependencies. |
| `NearbyQuestStartConditionServiceTests.GetLevelRequirementDiff_MatchesJavaMissingTemplateAndMinLevelBehavior` | Unit | Java `QuestService.getLevelRequirementDiff` | Validates missing-template `99`, positive min-level diff, and negative min-level diff behavior. | Deterministic C# test from source-reviewed Java utility. | Production quest-template loading remains unwired. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged predicate is not integrated into production `StaticData`, `DataManager`, player-controller refresh, packet sends, or ItemPurification dispatch.
- Production quest-template static data loading for nearby fields is not ported.
- Time-based repeat cooldowns and full Java `QuestState.canRepeat()` remain unsupported.
- XML start-condition, inventory item, combine-skill, and NPC faction checks remain unsupported.
- Enum/string mapping for Java `Race`, `PlayerClass`, and `Gender` is currently staged with ordinal string values and needs real XML loader tests before production use.
- Java warning packet behavior is not modeled because nearby UI passes `warn = false`; other callers cannot use this staged service as a general quest predicate.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 2 staged partial artifacts in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including production quest-template loading, XML start conditions, repeat timing, and NPC faction/combine-skill dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds a staged partial predicate boundary without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged XML/static-data extractor for `NearbyQuestTemplateSummary` over `quest_data.xml`, starting with the fields already represented by the staged DTO. Keep XML start conditions, NPC faction, combine skill, packet sends, production `StaticData` integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Add a staged XML extractor for `NearbyQuestTemplateSummary`.
- Why: The staged predicate now needs real quest-template field input before candidate filtering can move closer to runtime parity.
- Files: likely a new extractor/table test file under `Dataholders` and `Aion.GameServer.Tests`; avoid production `StaticData.cs` until the extractor behavior is pinned.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Analyze Java `XMLStartCondition` details for future predicate | read-only Java/docs | Low | Can run independently of extractor implementation. |
| B | Add staged XML extractor tests for quest-template scalar attributes | isolated new test file | Medium | Safe if production extractor file ownership is exclusive. |
| C | ItemPurification side-effect persistence gap audit | docs/read-only repository inspection | Medium | Far from nearby predicate files. |
| D | Java observer artifact generation | Java tooling/docs | Blocked | Requires Java/Maven tooling resolution. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement staged quest-template XML extractor | New extractor file and tests only | `StaticData.cs`, packet sends, ItemPurification dispatch |
| Agent A | Read-only `XMLStartCondition` behavior expansion | Read-only Java/docs | All writes |

### Do Not Parallelize

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`: shared loader file; production integration needs exclusive ownership.
- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- Phase 6 progress/handoff docs: orchestrator-owned.
