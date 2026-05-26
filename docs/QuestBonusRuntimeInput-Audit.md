# Quest Bonus Runtime Input Availability Audit

Date: May 26, 2026

## Scope

This audit maps the Java inputs needed by `QuestService.getRewardItems` / `BonusService.getQuestBonus` to the current C# quest-finish and static-data surfaces.

No production quest-finish wiring, handler dispatch, RNG, selected item creation, live inventory mutation, packet sends, persistence, or rollback behavior is enabled by this audit.

## Input Mapping

| Required Input | Java Source | Current C# Source | Status | Notes |
|---|---|---|---|---|
| Bonus type and level | `com.aionemu.gameserver.model.templates.quest.QuestTemplate.getBonus()` / `com.aionemu.gameserver.model.templates.rewards.Bonus` | `Aion.GameServer.Services.QuestFinishRewardTemplateProjection.ItemProjection.BonusProjection` from `QuestFinishRewardTemplateXmlProjectionExtractor` | Available in staged projection | The XML projection exposes bonus type, level, and support status. Runtime quest-finish composition only has this when a `QuestFinishRewardTemplateProjection` is supplied. Production static-data integration for all quest reward projections remains incomplete. |
| Player race | `com.aionemu.gameserver.model.gameobjects.player.Player.getRace()` | `Aion.GameServer.Model.GameObjects.Player.Race`; reachable through `QuestFinishRewardSideEffectContext.Player` when context is supplied | Available with context | Candidate filtering needs race for `ALL` / race-specific bonus groups. The side-effect context can carry the player, but bonus planning is not currently composed from that context. |
| Combine skill | `com.aionemu.gameserver.model.templates.quest.QuestTemplate.getCombineSkill()` | `Aion.GameServer.Data.Npc.NearbyQuestTemplateSummary.CombineSkill` | Available | `QuestFinishOperationPlanService.CreatePlan` already receives the template summary. This can supply the combine-skill gate for bonus groups. |
| Combine skill point | `com.aionemu.gameserver.model.templates.quest.QuestTemplate.getCombineSkillPoint()` | `Aion.GameServer.Data.Npc.NearbyQuestTemplateSummary.CombineSkillPoint` | Available | Same surface as combine skill. Precision/rounding is not involved; this is an integer gate. |
| Quest state | `player.getQuestStateList().getQuestState(questId)` | `Aion.GameServer.Model.GameObjects.PlayerQuestState` argument to `QuestFinishOperationPlanService.CreatePlan` | Available | Status is modeled as a string. Java enum/string equivalence and invalid-state behavior are not fully verified. |
| Quest var0 | `QuestState.getQuestVarById(0)` | `PlayerQuestState.GetQuestVarById(0)` | Available | Needed by audited LUNAR/RIFT handler gates. Current C# bit extraction supports vars 0 through 5. Runtime comparison with Java is still missing. |
| Complete count | `QuestState.getCompleteCount()` | `PlayerQuestState.CompleteCount` | Available | Needed by audited MOVIE handler direct reward behavior. |
| Item templates | `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA` | `runtimeContext.DataManager.StaticData.ItemTemplates`; `Aion.GameServer.Data.Static.ItemTemplateTable` | Available at runtime, not plumbed | Runtime context can expose item templates, and tests use `ItemTemplateTable`. The quest-finish operation and reward side-effect contracts do not currently pass item templates into bonus report assembly. |
| Bonus item groups | `com.aionemu.gameserver.dataholders.DataManager.ITEM_GROUPS_DATA` / item group XML | `QuestBonusItemGroupXmlProjectionExtractor` in tests | Missing production surface | Supported group projection exists as a focused extractor, but there is no production `StaticData` item-group table or `DataManager` integration feeding bonus candidate planning. This is the biggest input blocker before any real composition. |
| Handler registrations | `QuestEngine.registerOnBonusApply` and `QuestEngine.onBonusApplyEvent` | `QuestBonusHandlerOutcomePlanService` audited static model | Available only in disabled model | C# statically models audited MOVIE/LUNAR/RIFT outcomes. It does not load dynamic handlers, use reflection, catch handler exceptions, or prove Java runtime registration order. |
| Handler direct reward list mutation | Java handlers mutate `List<QuestItems>` before `BonusService` | `QuestBonusHandlerOutcomePlan.DirectRewardItems` / report direct reward intents | Available only as intent | No live list mutation, inventory addition, object-id allocation, packet send, persistence, or rollback is implemented. |
| Movie side effect | MOVIE handlers call `playQuestMovie` with random movie candidates | `QuestBonusHandlerSideEffect` random-movie intent | Available only as intent | No RNG, movie packet, or client-facing side effect is implemented. |
| Bonus selected item and random count | `Chance.selectElement` and Java `Rnd` inside `BonusService` | Candidate/selection envelope metadata only | Not implemented | C# reports chance inputs and null-result statuses. It does not select groups/items, roll count ranges, or create live `QuestItems`. |

## Current Composition Readiness

The following inputs can be assembled today if a non-live adapter receives explicit arguments:

- `QuestFinishRewardTemplateProjection` for bonus type and level.
- `NearbyQuestTemplateSummary` for combine skill and combine skill point.
- `PlayerQuestState` for status, var0, complete count, and quest id.
- `Player` or a narrow player snapshot for race.
- `ItemTemplateTable`, if the adapter contract accepts it explicitly.
- Supported bonus item group projections, if supplied by the caller or a future static-data table.

The following inputs are not production-ready:

- Production item-group static-data loading equivalent to Java `ITEM_GROUPS_DATA`.
- A stable quest-finish bonus-report input contract carrying item templates and item groups.
- Guaranteed runtime availability of `QuestFinishRewardTemplateProjection` for the exact finish flow being planned.
- Dynamic quest handler dispatch/reflection and Java exception-to-failed behavior.
- Java RNG behavior, selected group retry/removal behavior, item count rolls, live `QuestItems`, and live reward mutation.
- Movie playback packets and any client-facing handler side effects.

## Recommended Integration Boundary

Before production wiring, add a disabled input adapter that accepts explicit, already-loaded inputs and returns either:

- a `QuestBonusRewardPlanningReport`, or
- diagnostics listing which inputs are unavailable.

The adapter should not discover global data on its own. Keeping item templates, supported item groups, player race, quest state, and reward projection explicit will make missing Java dependencies visible and avoid hidden live behavior.

## Remaining Risks

- Item group XML projection is test-only and not yet tied to C# static-data loading.
- Java `DataManager.ITEM_GROUPS_DATA` filtering behavior, invalid XML behavior, collection ordering, and chance ordering are not runtime-compared.
- `PlayerQuestState.Status` is string-based; Java enum edge cases and casing remain unverified.
- Dynamic quest handler loading/reflection, first-loaded registration order, handler exception behavior, threading/player ordering, and movie side effects are still disabled.
- Date/time handling is not central to this bonus path, but quest repeat and finish ordering can still affect when this path is invoked.
- Serialization/JAXB differences for quest bonus XML and item groups remain unverified.
- Precision/rounding risks are limited to Java float chance weights; C# currently only reports metadata and does not roll or compare random results.

## Next Recommended Unit Of Work

Add a disabled `QuestBonusRewardPlanningInputAdapterService` that accepts explicit inputs from the audited surfaces and returns either a composed `QuestBonusRewardPlanningReport` or missing-input diagnostics. Keep supported item groups and item templates caller-supplied. Do not wire it into production quest finish yet.
