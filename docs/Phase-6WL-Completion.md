# Phase 6WL Completion - UOW-1098 Bonus Candidate Filtering Planner

Date: May 26, 2026

## Unit Of Work

UOW-1098: `[Phase 6][UOW-1098] Plan quest bonus candidate filtering`

## Summary

UOW-1098 adds a deterministic, non-live candidate planner for Java-supported quest bonus item groups. It filters projected item-group rows by explicit bonus type, bonus level, player race, craft skill, and craft skill point inputs.

No weighted random selection, random count roll, handler event, production quest-finish wiring, or live item reward mutation was enabled.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestBonusCandidatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestBonusCandidatePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6WL-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusCandidatePlanServiceTests" --nologo` | Passed: 4 tests. |
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestBonusCandidatePlanServiceTests\|QuestBonusItemGroupXmlProjectionExtractorTests" --nologo` | Passed: 6 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,159 tests. |

## Migration Parity Table - UOW-1098

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.reward.BonusService` | `QuestBonusCandidatePlanService` | Reward Service Dependency | Partial | Unit Tested / Regression Tested | Partial Parity | Deterministic candidate filtering is now modeled. `Chance.selectElement`, selected-group retry order, `QuestEngine.onBonusApplyEvent`, selected `QuestItems`, live mutation, production wiring, and Java runtime comparison remain incomplete. |
| `com.aionemu.gameserver.model.Chance` | `QuestBonusCandidateGroupDescriptor.Chance`; `QuestBonusCandidateItemDescriptor.EffectiveChance` | Utility / RNG Dependency | Not Started | Manual Only | Needs Verification | Chance values are metadata only. Weighted RNG, non-positive chance edge cases, float RNG precision, and remove-on-select behavior remain unimplemented. |
| `com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry` | `QuestBonusCandidatePlanService`; candidate/skipped descriptors | DTO / Filter Dependency | Partial | Unit Tested / Regression Tested | Partial Parity | Template race, XML race, and base template-level filters are covered. JAXB invalid-item throwing, enum parser behavior, DataManager singleton behavior, and live reward creation remain unverified. |
| `com.aionemu.gameserver.model.templates.rewards.IdLevelReward` | `QuestBonusCandidatePlanService.MatchesLevel` | DTO / Filter Dependency | Partial | Unit Tested | Partial Parity | XML level matching is modeled for subclasses. Missing required XML level/schema validation and JAXB failure behavior remain unverified. |
| `com.aionemu.gameserver.model.templates.rewards.FullRewardItem` | `QuestBonusCandidateItemDescriptor.CountMin/CountMax/EffectiveChance` | DTO / Reward Metadata | Partial | Unit Tested | Partial Parity | Fixed count/chance metadata is projected, including Java primitive default `0` when missing. Live `QuestItems` and inventory mutation remain disabled. |
| `com.aionemu.gameserver.model.templates.rewards.CraftReward` | `QuestBonusCandidatePlanService` craft skill filter | Abstract DTO / Filter Dependency | Partial | Unit Tested | Partial Parity | Explicit input models `questTemplate.getCombineSkill() == skill`. Missing skill defaults to `0` in planner metadata; schema/JAXB validation remains unverified. |
| `com.aionemu.gameserver.model.templates.rewards.CraftItem` | `QuestBonusCandidatePlanService`; `QuestBonusCandidateCountMode.RandomInclusiveRange` | DTO / Filter + Count Metadata | Partial | Unit Tested | Partial Parity | Min/max combine-skill-point filters and random count range `3..5` are represented. No `Rnd.get(3,5)` roll or live item creation. |
| `com.aionemu.gameserver.model.templates.rewards.CraftRecipe` | `QuestBonusCandidatePlanService` craft recipe filter | DTO / Filter Dependency | Partial | Unit Tested | Partial Parity | Skill, lower-bound, and Java max-level formula are represented. Recipe reward side effects remain outside this planner. |
| `com.aionemu.gameserver.model.templates.rewards.FoodItem` | `QuestBonusCandidateCountMode.RandomChoice` | DTO / Count Metadata | Partial | Unit Tested | Needs Verification | Random count choice `5`/`10` is metadata only. RNG and live mutation remain unimplemented. |
| `com.aionemu.gameserver.model.templates.rewards.MedicineItem` | `QuestBonusCandidateCountMode.RandomInclusiveRange` | DTO / Count Metadata | Partial | Unit Tested | Needs Verification | Random count range `1..3` is metadata only. RNG and live mutation remain unimplemented. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `QuestBonusCandidatePlanInput` | Quest Template Dependency | Partial | Unit Tested | Needs Verification | Planner uses explicit inputs instead of production `QuestTemplate`. Production integration, null/default behavior, and handler sequencing remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `Aion.GameServer.Dataholders.ItemTemplateTable` | Static Data Dependency | Partial | Regression Tested | Needs Verification | Real-data audit loads C# static item templates and confirms supported MANASTONE/TASK pools have no missing templates. Java JAXB/DataManager runtime object comparison remains blocked. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestBonusCandidatePlanServiceTests.CreatePlan_FiltersRaceAndBonusLevelLikeJavaItemRaceEntryAndIdLevelReward` | Template race, XML race, base template-level filtering, and base count/chance metadata. | Source-reviewed from `ItemRaceEntry.matches` and `IdLevelReward.matchesLevel`; no Java runtime comparison. |
| `QuestBonusCandidatePlanServiceTests.CreatePlan_FiltersCraftGroupsBySkillAndSkillPointLikeJavaCraftRewards` | Craft skill, `CraftItem` skill-point bounds, `CraftRecipe` skill-point bounds and max-level formula, and craft count metadata. | Source-reviewed from `CraftReward`, `CraftItem`, and `CraftRecipe`; no Java runtime comparison. |
| `QuestBonusCandidatePlanServiceTests.CreatePlan_ReportsFullFoodMedicineCountAndChanceMetadataWithoutSelectingRewards` | `FullRewardItem` fixed count/chance plus food/medicine random count metadata without selecting a reward. | Source-reviewed from reward subclass methods; no Java RNG/runtime comparison. |
| `QuestBonusCandidatePlanServiceTests.RealDataAudit_ComputesDeterministicCandidatePoolForSupportedBonusTypes` | Real Java XML supports candidate pools for representative MANASTONE and TASK inputs with no missing item-template skips. | C# extraction over repository Java XML and static item templates; not a Java object/runtime comparison. |

## Remaining Risks

- Weighted `Chance.selectElement` behavior remains unimplemented: group removal/retry order, non-positive chance behavior, float precision, selected item chance, and Java RNG are all pending.
- Random count rolls are metadata only; no selected `QuestItems` are created.
- `QuestEngine.onBonusApplyEvent` remains unmodeled and may alter or reject bonus reward application.
- Production quest-finish/socket wiring is still disabled.
- Live `ItemService.addItem`, inventory capacity, stacking, packet emission, persistence, rollback, and reward failure ordering remain unimplemented for this path.
- JAXB/schema validation, invalid item IDs, invalid race combinations, missing XML fields, enum parsing, reflection/dynamic handler behavior, threading/player-ordering, and Java runtime comparison remain open.

## Summary Metrics

- Total Java artifacts discovered: 12 in this unit
- Total artifacts ported: 1 partial non-live bonus candidate filtering planner
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 12
- Total blocked artifacts: 9 blocked/partial categories: production socket integration, live item reward mutation, live non-item reward mutation, weighted bonus item selection, `QuestEngine.onBonusApplyEvent`, Java RNG/count rolls, JAXB validation parity, custom reward/mail execution, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a non-live bonus selection envelope over `QuestBonusCandidatePlan`: preserve Java `Chance.selectElement` inputs, selected-group retry semantics, and selected-item/count metadata without performing RNG or live item rewards.

Keep handler events, Java RNG execution, `QuestItems` creation, inventory mutation, packet sends, and production quest-finish wiring disabled until deterministic selection boundaries and failure ordering are documented.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live bonus selection envelope | new focused service/tests | Medium | Recommended next unit; depends on `QuestBonusCandidatePlanService`. |
| B | Bonus handler event analysis | read-only Java handler analysis | Low/Medium | Map `QuestEngine.onBonusApplyEvent` registrations before live handler modeling. |
| C | Production quest-finish runtime input audit | read-only or isolated audit doc | Medium | Useful before wiring candidate planner into production operation planning. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add selection-envelope service/tests | New service/test files plus Phase 6 docs | Production `GameServerConnection`, shared reward planners unless explicitly selected |
| Explorer A | Read-only handler event analysis | Java source only | All writes |

## Do Not Parallelize

- `QuestBonusCandidatePlanService.cs`: owns the new candidate filtering contract.
- `QuestBonusItemGroupXmlProjectionExtractor.cs`: owns supported group projection contract.
- `QuestFinishRewardPlanService.cs` and `QuestFinishOperationPlanService.cs`: shared quest reward planner contracts.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- Java source remains authoritative.
- UOW-1098 is non-live only and must not be treated as production reward parity.
- Candidate filtering takes explicit `QuestBonusCandidatePlanInput`; production quest template/player/runtime integration is still absent.
- Real-data audit only proves C# can load current Java XML and item templates for representative inputs; it is not Java runtime validation.
