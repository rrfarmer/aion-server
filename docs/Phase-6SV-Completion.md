# Phase 6SV Completion - UOW-1004 Nearby NPC Faction

## Scope

UOW-1004 adds a staged NPC faction state snapshot and hooks it into the offline nearby quest start-condition predicate. This unit intentionally does not enable repository hydration, live nearby quest packet sends, `CM_LEVEL_READY` dispatch, delayed NPC-spawn refresh, production `StaticData` loading, daily faction assignment, or production `CM_ITEM_PURIFICATION` quest refresh dispatch.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Staged NPC faction snapshot predicate | `NpcFactions`, `NpcFaction`, `ENpcFactionQuestState`, `QuestService.checkStartConditions` | `Player.cs`, new NPC faction model, `NearbyQuestTemplate*`, `NearbyQuestStartConditionService`, nearby tests, docs | Service/model port | No with other nearby predicate edits | Medium | Central nearby predicate and player model need one writer. |
| B | Master-crafting XML required-count audit | `QuestTemplate.getRequiredConditionCount`, `CraftConfig` | Read-only report | Java analysis | Yes | Low | Independent, no writes. |
| C | Repository hydration for `player_npc_factions` | `PlayerNpcFactionsDAO`, SQL schema | Player enter-world repository files/tests | Data port | Not with A today | Medium | Depends on settled model shape. |
| D | Start-quest faction assignment guard | `QuestService.startQuest`, `NpcFactions.startQuest` | Future quest start service | Service port | Yes as read-only only | Medium | Different runtime surface than nearby marker projection. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Result |
|---|---|---|---|---|---|
| Orchestrator | Implement staged NPC faction nearby predicate | Service/model port | C# nearby predicate/model/tests/docs | Repository hydration and live sends | Complete |
| Explorer | Audit master-crafting XML required-count adjustment | Java analysis | Read-only repo inspection | All writes | Complete; next unit recommended |

## Java Breadcrumbs

- `QuestService.checkStartConditions` checks NPC faction requirements after inventory and combine-skill predicates.
- `NpcFactions.canStartQuest(template)` uses the active mentor/non-mentor slot and slot-level `timeLimit`.
- Non-time-based NPC faction quests must pass `canStartQuest(template)` and then require the exact faction row to exist and be active.
- Time-based NPC faction quests skip the daily slot cooldown but still require the exact active faction.
- `QuestTemplate.isMentor()` returns true when `mentor_type != NONE`.

## Implementation

- Added `PlayerNpcFactionState`, `PlayerNpcFactionsSnapshot`, and `PlayerNpcFactionQuestState`.
- Added `Player.NpcFactions` with an empty snapshot default.
- Added `NearbyQuestTemplateSummary.IsMentorQuest` and XML extraction from Java `mentor_type`.
- Replaced fail-closed `UnsupportedNpcFaction` behavior with modeled `NpcFaction` rejection.
- Removed NPC faction from `NearbyQuestRefreshPlan.HasUnsupportedDependencies`.

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests"` | Passed, 24 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1710 tests |

## Migration Parity Table - UOW-1004

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionsSnapshot` | Player State / Predicate Dependency | Partial | Unit Tested | Partial Parity | Models active mentor/non-mentor slots, slot-level `timeLimit`, and Java `canStartQuest` behavior for nearby checks. Repository hydration, mutation methods, daily assignment, packets, and persistence states remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionState` | DTO / Player State | Partial | Unit Tested | Partial Parity | Staged DTO carries faction id, active flag, mentor flag, time, state, and assigned quest id. Mentor flag is supplied by the snapshot caller because C# does not yet load `npc_factions.xml`. |
| `com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState` | `Aion.GameServer.Model.GameObjects.PlayerNpcFactionQuestState` | Enum | Partial | Unit Tested | Partial Parity | Preserves Java states `NOTING`, `START`, and `COMPLETE` as C# enum values. Serialization/database enum mapping is not wired. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Implements staged NPC faction branch for nearby checks after combine-skill: non-time-based slot cooldown, exact active faction, and time-based cooldown skip. Warning packets, exception/log behavior, start-quest assigned-id guard, and production send integration remain out of scope. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor` | Dataholder / XML Extractor | Partial | Unit Tested | Partial Parity | Adds `IsMentorQuest` from `mentor_type != NONE` for faction slot selection. Full `QuestMentorType` enum, JAXB runtime comparison, and production static-data loading remain unwired. |
| `com.aionemu.gameserver.dao.PlayerNpcFactionsDAO` | Future repository hydration for `Player.NpcFactions` | Repository | Not Started | Manual Only | Needs Verification | Java schema and load behavior are source-audited, but C# still does not hydrate `player_npc_factions` from the database. |
| `com.aionemu.gameserver.configs.main.CraftConfig`; `QuestTemplate.getRequiredConditionCount` | Future nearby master required-count parameter/config | Config / Predicate Dependency | Not Started | Manual Only | Needs Verification | Read-only sidecar confirmed default C# behavior matches Java only when `MAX_MASTER_CRAFTING_SKILLS == 1`; configurable master adjustment remains the next narrow slice. |

## Tests Added Or Updated

| Test Name | Type | What It Validates | Java Comparison |
|---|---|---|---|
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaNpcFactionGate` | Unit | Exact active faction, inactive/missing faction rejection, non-time-based mentor cooldown, and time-based cooldown skip. | Source-reviewed Java predicate; no runtime Java comparison. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_ReportsUnsupportedJavaDependenciesInsteadOfAssumingParity` | Unit | NPC faction now returns modeled `NpcFaction` rejection instead of unsupported. | Source-reviewed Java order. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | `mentor_type="MENTOR"` maps to `IsMentorQuest`. | Source-reviewed Java `QuestTemplate.isMentor`. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Missing `mentor_type` defaults to non-mentor. | Source-reviewed Java field default. |
| Read-only master-crafting sub-agent analysis | Manual | Next configurable master required-count slice. | Source-reviewed Java `QuestTemplate`/`CraftConfig`. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Repository hydration from `player_npc_factions` is not implemented.
- `npc_factions.xml` static data is not modeled; C# callers must supply `IsMentor`.
- Daily assignment, `questId` start guard, join/leave mutation, packet fanout, and persistence states remain unported.
- Configurable master-crafting required-count adjustment remains unimplemented.
- Live nearby sends and ItemPurification dispatch remain disabled.

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported: 4 staged partial artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked artifacts: 5 blocked/not-started categories
- Estimated overall migration completion: Phase 6 remains about 70%

## Next Recommended Unit Of Work

Implement configurable master-crafting XML required-count parity:

- Add a `maxMasterCraftingSkills` parameter or staged options object for nearby checks.
- Apply Java `result += 1 - CraftConfig.MAX_MASTER_CRAFTING_SKILLS` when `CombineSkillPoint == 499`.
- Add tests for default cap, relaxed cap, non-master quests, and any `requiredCount <= 0` edge if applicable.
- Keep packet sends, production integration, and production ItemPurification dispatch disabled.
