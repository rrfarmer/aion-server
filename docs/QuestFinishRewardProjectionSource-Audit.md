# Quest Finish Reward Projection Source Audit

Date: May 26, 2026

## Scope

UOW-1114 audits how the future C# quest-finish path can source `QuestFinishRewardTemplateProjection` from Java `quest_data.xml` parity data before any live `CM_DIALOG_SELECT` wiring is attempted.

This is a read-only source audit. It does not enable live reward mutation, packet sends, persistence, or production quest-finish dispatch.

## Java Source Of Truth

Java finish reward source chain:

1. `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT.runImpl`
   - Reads `targetObjectId`, `dialogActionId`, `extendedRewardIndex`, `lastPage`, and `questId`.
   - If `targetObjectId == 0 || targetObjectId == player.getObjectId()`, resolves `QuestTemplate questTemplate = DataManager.QUEST_DATA.getQuestById(questId)`.
   - For `questTemplate.isCanReport()` and selected auto-reward dialog actions, creates `new QuestEnv(null, player, questId, dialogActionId)` and calls `QuestService.finishQuest(env)`.
   - For NPC targets, delegates to `target.getController().onDialogSelect(dialogActionId, lastPage, player, questId, extendedRewardIndex)`.

2. `com.aionemu.gameserver.services.QuestService.finishQuest`
   - Resolves the same quest id through `DataManager.QUEST_DATA.getQuestById(id)`.
   - Rejects missing/non-`REWARD` quest state and repeat mission finish attempts.
   - Calls `validateAndFixRewardGroup(qs, id)`.
   - Adds extended reward items when `template.getExtendedRewards() != null && qs.getCompleteCount() == template.getRewardRepeatCount() - 1`.
   - Adds regular reward items when `!template.getRewards().isEmpty() || template.getBonus() != null`.
   - Calls `getRewardItems(env, template, extended, rewardGroup)`.
   - Calls `giveReward(env, rewards)` and `giveReward(env, extendedRewards)`.

3. `com.aionemu.gameserver.services.QuestService.getRewardItems`
   - Regular fixed rewards come from `template.getRewards().get(rewardGroup).getRewardItem()`.
   - Regular selectable rewards use `getRewardIndex(env.getDialogActionId())`, where Java maps `SELECTED_QUEST_REWARD1` through `SELECTED_QUEST_REWARD15` to indexes `0..14`.
   - Class selectable rewards use `template.getSelectableRewardByClass(playerClass)` when `isLastRepeat && template.isSingleTimeClassReward()` or `template.isClassRewardOnEveryRepeat()`.
   - `SELECTED_QUEST_NOREWARD` can still select a class reward through `env.getExtendedRewardIndex() - 8`.
   - Extended fixed rewards come from `template.getExtendedRewards().getRewardItem()`.
   - Extended selectable rewards use `env.getExtendedRewardIndex()`, trying `index - 8` first and then `index - 1`.
   - Bonus rewards run through `QuestEngine.onBonusApplyEvent(...)`; only `HandlerResult.FAILED` suppresses `BonusService.getQuestBonus(...)`.

4. `com.aionemu.gameserver.dataholders.QuestsData`
   - JAXB unmarshals full `QuestTemplate` objects.
   - `afterUnmarshal` builds an indexed `Map<Integer, QuestTemplate>`.
   - `getQuestById(int id)` is the runtime source used by both `CM_DIALOG_SELECT` and `QuestService`.

## Current C# State

Current C# reward projection pieces:

- `Aion.GameServer.Services.QuestFinishRewardTemplateProjection` models the non-live finish reward projection.
- `Aion.GameServer.Services.QuestFinishRewardTemplateXmlProjectionExtractor` can parse reward metadata from XML into projections.
- `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor` parses nearby/summary quest metadata from XML into `NearbyQuestTemplateSummary`.
- `Aion.GameServer.Dataholders.NearbyQuestTemplateTable` indexes summaries by quest id.
- `QuestFinishOperationPlanService.CreatePlan(...)` accepts an optional `QuestFinishRewardTemplateProjection` and composes non-live reward projection descriptors when supplied.
- `QuestFinishStaticRewardProjectionCompositionTests` prove XML-derived projection composition for regular items, extended items, class selectable items, bonus metadata, and regular/extended non-item rewards.

Current production gaps:

- `Aion.GameServer.Dataholders.StaticData` does not expose a `NearbyQuestTemplateTable` or a `QuestFinishRewardTemplateProjection` table.
- `QuestFinishRewardTemplateXmlProjectionExtractor` is used by tests, not by the runtime static-data loader.
- `GameServerConnection.HandleDialogSelectAsync` handles portals, recovery, crafting, storage expansion, and item charge actions; it does not route Java auto-reward quest actions into `QuestFinishOperationPlanService`.
- `CmDialogSelect` includes `ExtendedRewardIndex`, but the current production method never maps it into a `QuestFinishRewardTemplateProjection`.
- The C# projection extractor currently creates default projections at reward group index `0`; Java first validates/corrects `QuestState.rewardGroup` and then reads the selected group. Multi-reward-group lookup therefore needs an explicit per-quest/per-group projection contract before production wiring.

## Required Production Lookup Contract

Before any live socket wiring, add an explicit source contract that can answer:

| Input | Java Source | C# Source Needed | Status |
|---|---|---|---|
| `questId` | `CM_DIALOG_SELECT.questId`; `QuestEnv.getQuestId()` | Packet field plus player quest-state lookup | Partial |
| `dialogActionId` | `CM_DIALOG_SELECT.dialogActionId`; `QuestEnv.getDialogActionId()` | Packet field copied into projection input | Partial |
| `extendedRewardIndex` | `CM_DIALOG_SELECT.extendedRewardIndex`; NPC controller passes it into quest env | Packet field copied into projection input | Partial |
| `QuestTemplate` summary | `DataManager.QUEST_DATA.getQuestById(id)` | Runtime indexed `NearbyQuestTemplateTable` or equivalent | Missing from `StaticData` |
| full reward projection | `QuestTemplate.getRewards()`, `getExtendedRewards()`, class reward fields, `getBonus()` | Runtime indexed `QuestFinishRewardTemplateProjection` lookup | Missing from `StaticData` |
| selected reward group | `QuestState.getRewardGroup()` after `validateAndFixRewardGroup` | Reward-group-aware projection lookup or factory | Missing for production |
| complete count | `QuestState.getCompleteCount()` | Existing `PlayerQuestState.CompleteCount` | Partial |
| player class | `player.getCommonData().getPlayerClass()` | C# player class projection/input mapping | Needs Verification |
| target npc id/l10n | `QuestEnv.getTargetId()` for XP l10n | Packet/target lookup into NPC template | Needs Verification |
| quest category/rates | `QuestTemplate.getCategory()` plus rates | Summary/projection category plus rate services | Partial/non-live |

Recommended C# shape:

- Add a dedicated non-live lookup surface rather than embedding XML parsing in `GameServerConnection`.
- Candidate service/table: `QuestFinishRewardProjectionTable` or `QuestFinishRewardProjectionLookupPlanService`.
- Key the base projection by quest id and reward group index.
- Return diagnostics for missing quest, missing projection, missing requested reward group, missing player class, unsupported extended index, and missing target NPC template.
- Keep the operation planner pure: it should consume a prepared projection and report lookup diagnostics; it should not parse XML or reach into static data.
- Keep production `CM_DIALOG_SELECT` disabled until the static-data table and lookup diagnostics are unit tested against real `quest_data.xml`.

## Known Differences And Risks

- Serialization: Java JAXB materializes full `QuestTemplate` objects with lists, enum fields, and defaults. C# currently uses hand-written `XElement` extractors with selected fields only.
- Reward group selection: Java mutates/corrects `QuestState.rewardGroup` before reward selection. C# must preserve this order when lookup becomes production-adjacent.
- Extended reward index: Java has two index translations for extended selectable rewards (`index - 8` and fallback `index - 1`). C# tests cover projection behavior, but production packet-to-projection sourcing is absent.
- Class reward logic: Java uses `PlayerClass` enum fields and `use_class_reward` semantics. C# maps XML element names to string class names; production player-class normalization is not verified.
- Bonus behavior: Java may mutate `questItems` through handlers before `BonusService`; C# only carries non-live metadata/report descriptors.
- Threading/player ordering: Java executes inside packet handling and mutates live player state. C# remains non-live; no concurrency, persistence, rollback, or packet-order parity is claimed.
- Date/time: repeat-time calculations are staged elsewhere; this audit does not verify time-based completion.

## Tests Reviewed

Existing tests, no new tests in this unit:

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests.*` | XML reward projection extraction for non-item, item, extended, class, and bonus metadata. | Source-reviewed Java `QuestTemplate`/`QuestService`; no Java runtime comparison. |
| `QuestFinishStaticRewardProjectionCompositionTests.*` | XML-derived reward projection can be composed into non-live operation descriptors. | Source-reviewed Java finish ordering; no production socket wiring or Java runtime comparison. |
| `QuestFinishOperationPlanServiceTests.*` | Operation planner accepts caller-supplied reward projections and orders descriptors around state mutation. | Deterministic C# assertions; no live reward mutation. |

## Migration Parity Table - UOW-1114

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync`; `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Packet / Production Caller | Partial | Manual Only | Needs Verification | Java auto-reward dialog actions call `QuestService.finishQuest` for reportable quests. C# production dialog select does not route quest finish actions, does not source quest templates, and does not pass reward projection. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService` | Service / Finish Planner | Partial | Existing Unit Coverage | Needs Verification | C# planner consumes an optional projection and composes non-live descriptors. It does not lookup templates, mutate rewards, send packets, persist state, or run callbacks live. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService`; `QuestFinishRewardTemplateProjection` | Reward Service / Projection Planner | Partial | Existing Unit Coverage | Needs Verification | Projection covers regular, extended, class-selectable, and bonus metadata in tests. Production input sourcing for reward group, extended index, player class, and live bonus handler mutation remains missing. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `NearbyQuestTemplateTable`; future `QuestFinishRewardProjectionTable` or lookup service | Static Data Repository | Partial | Manual Only | Needs Verification | Java indexes full `QuestTemplate` by quest id. C# has separate summary/projection extractors but no runtime static-data table exposing full finish reward projections. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardTemplateProjection` | Static Template DTO | Partial | Existing Unit Coverage | Needs Verification | C# splits Java template concerns across summary and reward projection records. XML extraction covers selected fields only; unsupported Java template fields and JAXB defaults remain risks. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardNonItemTemplateProjection` | Static Reward DTO | Partial | Existing Unit Coverage | Needs Verification | Regular/extended non-item and item metadata are projected in tests. Runtime lookup by corrected reward group is not implemented. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItem`; `QuestFinishRewardItemProjectionDescriptor` | Reward Item DTO | Partial | Existing Unit Coverage | Needs Verification | Item id/count metadata is projected without live `ItemService.addItem`; Java count defaults and selectable indexing are source-reviewed but not runtime-compared. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection`; `QuestBonusRewardPlanningReport` | Bonus DTO / Reward Dependency | Partial | Existing Unit Coverage | Needs Verification | Bonus metadata can be projected and carried, but production handler dispatch, Java RNG/Chance selection, and selected bonus item mutation remain disabled. |

## Remaining Risks

- No production `questId -> QuestFinishRewardTemplateProjection` lookup exists.
- No production `questId -> NearbyQuestTemplateSummary` table is exposed through `StaticData`.
- Multi-reward-group projection requires an explicit reward-group-aware lookup; default group `0` extraction is not enough for Java parity.
- C# has not runtime-compared XML projection output against Java JAXB `QuestTemplate` objects.
- Production player-class normalization, target NPC l10n lookup, rate application, bonus handler side effects, live inventory mutation, packet ordering, persistence, rollback, threading, serialization, and date/time completion behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0; 1 read-only production reward-projection source audit added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production quest-template table, production reward-projection table, reward-group-aware lookup, socket quest-finish routing, player-class normalization, dynamic bonus handler execution, live reward mutation/persistence, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete; this unit documents the missing production lookup contract before socket wiring.

## Next Recommended Unit Of Work

Add a non-live `QuestFinishRewardProjectionLookupPlanService` or equivalent lookup table contract that accepts quest id, corrected reward group, dialog action id, extended reward index, player class, complete count, and target NPC context, then returns a prepared `QuestFinishRewardTemplateProjection` plus diagnostics. Keep XML parsing and live socket wiring outside `GameServerConnection` until the lookup is covered with unit tests and a real-data smoke test.
