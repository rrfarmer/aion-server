# Quest-Finish Production Call-Site Audit

Date: May 26, 2026

## Scope

This audit identifies the production call sites that would eventually build a live quest-finish reward side-effect context and custom reward runtime options in the C# port.

Java source remains authoritative:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java#runImpl`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java#finishQuest`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java#giveReward`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#addExp`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#setExp`
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java#onLevelChange`
- `game-server/src/com/aionemu/gameserver/services/BonusPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/services/FactionPackService.java#sendRewards`
- `game-server/src/com/aionemu/gameserver/services/mail/SystemMailService.java#sendMail`

This unit does not enable production quest finish, XP mutation, custom reward DAO writes, system-mail persistence, or packet fanout.

## Java Production Call Chain

| Order | Java Artifact | Behavior | Runtime Inputs |
| --- | --- | --- | --- |
| 1 | `CM_DIALOG_SELECT.runImpl` | For self/reportable quest auto-reward dialog actions, creates `QuestEnv(null, player, questId, dialogActionId)` and calls `QuestService.finishQuest(env)`. | Active player, quest id, dialog action id, quest template from `DataManager.QUEST_DATA`. |
| 2 | `QuestService.finishQuest` | Validates quest status, reward group, extended rewards, item rewards, then calls `giveReward` for regular and extended rewards before removing work items and completing quest state. | `QuestState`, `QuestTemplate`, reward group, current server time for repeat date. |
| 3 | `QuestService.giveReward` | Applies kinah, XP, title, AP, DP, GP, cube, and warehouse side effects. XP calls `player.getCommonData().addExp(...)`. | Reward values, target NPC template/l10n, rates, player state. |
| 4 | `PlayerCommonData.addExp` | Calculates quest XP rate, repose, salvation, and calls `setExp(exp + reward)`. | Current XP, rate boost, repose/salvation state, no-exp guard. |
| 5 | `PlayerCommonData.setExp` | Mutates player XP and level, calls `player.getController().onLevelChange(oldLevel, level)`, then sends `SM_STATUPDATE_EXP`. | Experience table, Daeva cap, online/player reference. |
| 6 | `PlayerController.onLevelChange` | Performs level-up side effects in Java order, then calls `BonusPackService.addPlayerCustomReward` and `FactionPackService.addPlayerCustomReward`. | Updated player level, account state, world/spawn state, static data, service singletons. |
| 7 | `BonusPackService` / `FactionPackService` | Load/store one-per-account receipt rows, plan reward items, and call `SystemMailService.sendMail`. | Account id, account creation time, race, item templates, mail payloads. |
| 8 | `SystemMailService.sendMail` | Allocates item/letter ids, persists mail and attached item, updates mailbox/fanout. | `IDFactory.nextId`, `DataManager.ITEM_DATA`, recipient lookup, current time. |

Important ordering note: Java custom rewards run after XP/level mutation and before `SM_STATUPDATE_EXP`, XP gain messages, quest state completion packet, and quest completion callback. C# must not execute custom rewards from a stale pre-mutation player snapshot.

## Current C# Production Surface

| C# Artifact | Current State | Future Role | Current Gate |
| --- | --- | --- | --- |
| `GameServerConnection.HandleDialogSelectAsync` | Handles portals, recovery, crafting, storage expansion, and item charging. It does not call quest-finish planning for self auto-reward dialogs. | Future socket entry for `CM_DIALOG_SELECT` quest auto-reward handling. | No live quest-finish branch exists. |
| `QuestFinishOperationPlanService.CreatePlan` | Builds non-live quest-finish operation metadata and optional reward side-effect descriptors. | Future orchestrator for quest-finish ordering once live mutation is ready. | Descriptors default to non-live; callers must explicitly supply projections/context. |
| `QuestFinishRewardSideEffectContext` | Carries optional player, title/cube/XP/static context, and `QuestXpLevelChangeContextFactoryInput`. | Future single context for non-item reward side-effect metadata and eventual guarded execution. | Missing context leaves reward side-effect descriptors absent. |
| `QuestFinishCustomRewardSessionRuntimeInputAdapterService` | Converts session-shaped player/id/static-data inputs into disabled-by-default custom reward options. | Future bridge from socket/runtime context to custom reward adapter options. | `EnableCustomRewardExecution` must be explicit; disabled path needs no dependencies and allocates no ids. |
| `QuestFinishCustomRewardRuntimeSideEffectAdapterService` | Can replace `LevelChangeContextInput` with custom reward execution results only when options are enabled and dependencies exist. | Future adapter between quest-finish XP metadata and opt-in custom reward execution. | Disabled options preserve context and avoid repository calls. |
| `CustomLevelRewardExecutionService` | Opt-in bonus/faction execution boundary that can load/store receipts and plan system-mail payloads. | Future level-change custom reward executor. | Not invoked by production quest finish. |
| `SystemMailRewardPersistenceExecutionService` | Disabled-by-default mail persistence/fanout executor. | Future final persistence/fanout stage for custom reward mail. | Requires explicit enabled options and injected executor. |

## Future Wiring Points

The future production implementation should be staged in separate units, in this order:

1. Add a self/reportable quest auto-reward branch in `GameServerConnection.HandleDialogSelectAsync`.
   - Mirror Java `CM_DIALOG_SELECT` guards: target object id is zero or player id; quest template exists; `questTemplate.isCanReport()`; dialog action is one of the selected reward actions.
   - Do not call live reward mutation in the first branch. Start by producing a non-live plan and returning.

2. Build a `QuestFinishRewardTemplateProjection` from static quest data.
   - Must include regular rewards, extended rewards, reward group, target NPC id/name, and work-item inputs.
   - Missing C# static-data projection support should stop the plan before side effects.

3. Build `QuestFinishRewardSideEffectContext` from the active session.
   - Source `Player` from the active player.
   - Source `ExperienceTable`, `ItemTemplates`, quest/nearby/static tables, and world instance state from `runtimeContext.DataManager.StaticData`.
   - Source account creation time from `Player.AccountCreationEpochMillis`.
   - Source ids from injected `IDFactory.NextId`.
   - Source received time through an injectable time provider rather than direct `DateTime.Now` in tests.

4. Assemble custom reward options through `QuestFinishCustomRewardSessionRuntimeInputAdapterService`.
   - Production must pass `EnableCustomRewardExecution = false` until live XP mutation, repository transaction policy, and mail persistence/fanout readiness are complete.
   - Missing player, id factory, or static item templates must remain an explicit dependency gap when the option is enabled.

5. Optionally pass disabled options through `QuestFinishCustomRewardRuntimeSideEffectAdapterService`.
   - This is safe only while options are disabled; it preserves `QuestFinishRewardSideEffectContext` and avoids repositories/id allocation.
   - Enabled execution must wait for a later explicit unit.

6. Feed the resulting context into `QuestFinishOperationPlanService.CreatePlan`.
   - The plan can surface non-live XP/custom reward metadata for review.
   - Do not mutate player XP/level, quest state, inventory, AP/DP/GP, titles, cube, warehouse, receipts, or mail from this path until each side effect has a tested live boundary.

## Required Guards Before Any Live Execution

| Guard | Required Evidence Before Enabling | Risk If Skipped |
| --- | --- | --- |
| Live XP mutation boundary | C# implementation matching `PlayerCommonData.addExp` and `setExp`, including level cap, repose/salvation, packet order, and `onLevelChange` timing. | Custom rewards can evaluate the wrong level or send packets in the wrong order. |
| Quest static-data projection | Deterministic extraction of `QuestTemplate` rewards, reward groups, extended rewards, and target NPC context. | Wrong reward group, missing rewards, or incorrect work-item removal. |
| Per-player execution ordering | Socket/handler path must preserve Java-style sequential mutation for a player. | Race conditions between quest finish, XP update, mail, and persistence. |
| Account/session completeness | Active player must have account id, account creation epoch milliseconds, access level, membership, and future account aggregate fields as needed. | Faction windows and one-per-account gates can diverge from Java. |
| Custom reward receipt policy | DAO load/store ordering and failure behavior must be covered, including store-before-mail behavior. | Duplicate or missing custom rewards. |
| System-mail persistence/fanout | Letter/item/offline counter/online fanout ordering must be integration tested and packet-ordered. | Mail loss, duplicate fanout, or wrong mailbox state. |
| Java runtime comparison | Golden/runtime comparison should be generated when Java tooling is available. | Source-derived assumptions may hide packet/order mismatches. |

## Do Not Wire Yet

- Do not execute `QuestService.finishQuest` parity from `HandleDialogSelectAsync` as live gameplay.
- Do not enable `QuestXpCustomRewardRuntimeInputAdapterOptions.EnableCustomRewardExecution` in production.
- Do not call custom reward receipt repositories from production quest finish.
- Do not call system-mail persistence/fanout from production quest finish.
- Do not allocate object ids for custom reward mail from disabled quest-finish paths.
- Do not mutate XP/level from `QuestFinishOperationPlanService` until `PlayerCommonData.setExp` parity exists.
- Do not claim verified parity for this production path; this is source-reviewed planning only.

## Recommended Next Small Step

Add a non-live `CM_DIALOG_SELECT` self auto-reward guard planner or test helper that detects Java's reportable auto-reward dialog branch and returns a disabled quest-finish planning intent. Keep it separate from live `HandleDialogSelectAsync` mutation until quest template projection and XP mutation are ready.

## C# State After UOW-1081

- Added `QuestDialogAutoRewardGuardPlanService`.
- The planner detects Java's self/player-target reportable quest auto-reward guard without mutating live gameplay.
- It treats dialog action `108` and `110..124` as Java auto-reward actions and deliberately rejects `109`, normal selected reward ids `8..23`, and values outside the Java switch.
- It preserves Java guard order:
  1. non-self target returns before quest template checks;
  2. missing quest template returns before reportable/action checks;
  3. non-reportable quest returns before action switch;
  4. matching auto-reward action returns a non-live planned intent.
- Tests cover the guard order and constants. The planner is not wired into `GameServerConnection.HandleDialogSelectAsync`.

## Recommended Next Small Step After UOW-1081

Add a static-data projection prerequisite for the C# side of Java `QuestTemplate.can_report` and reward metadata, or add a non-live composition test that feeds the guard planner's planned intent into existing quest-finish operation planning with explicit mock projections. Keep production `HandleDialogSelectAsync` live quest finish disabled.

## C# State After UOW-1082

- Added a non-live composition regression in `QuestFinishOperationPlanServiceTests`.
- The test creates a planned `QuestDialogAutoRewardGuardPlanService` intent for Java action `108`, then feeds its dialog action id into `QuestFinishOperationPlanService.CreatePlan` with explicit mock reward projection data.
- The composed operation plan remains fully non-live, includes non-item reward projection metadata, the coarse non-item placeholder, and quest-state mutation metadata, and completes the in-memory planned quest state.
- No production socket path, static-data lookup, reward mutation, XP mutation, custom reward execution, or mail execution was enabled.

## Recommended Next Small Step After UOW-1082

Add a static-data projection prerequisite for Java `QuestTemplate.can_report` and reward metadata so the C# guard planner can eventually consume real static quest data. Keep production `HandleDialogSelectAsync` live quest finish disabled.

## C# State After UOW-1083

- Extended `NearbyQuestTemplateSummary` and `NearbyQuestTemplateXmlExtractor` with a small Java `QuestTemplate` projection prerequisite:
  - `can_report`;
  - `reward_repeat_count`;
  - direct child presence for `rewards`, `extended_rewards`, `bonus`, and `quest_work_items`.
- Updated `NearbyQuestTemplateXmlExtractorTests` to verify the fields on fixture XML, defaults when attributes/children are absent, and real Java XML counts.
- Real-data regression counts now confirm 8,043 quest templates, 64 reportable templates, 241 reward-repeat templates, 7,935 templates with direct `rewards`, 235 with `extended_rewards`, 782 with `bonus`, and 1,630 with `quest_work_items`.
- This is still only a static summary prerequisite. It does not build a full `QuestFinishRewardTemplateProjection`, parse reward items/non-item values, wire `GameServerConnection.HandleDialogSelectAsync`, call quest finish, mutate rewards/XP, execute custom rewards, persist mail, or send packets.

## Recommended Next Small Step After UOW-1083

Add a narrow adapter from `NearbyQuestTemplateSummary` into guard/planner prerequisite metadata so `QuestDialogAutoRewardGuardPlanService` can consume real `CanReport` and the operation planner can report missing full reward projection data without enabling live quest finish.

## C# State After UOW-1084

- Added `QuestDialogAutoRewardGuardPlanService.CreatePlanFromTemplateSummary`.
- The adapter accepts `NearbyQuestTemplateSummary?`, derives Java guard inputs from real static summary data, and keeps Java guard order:
  1. non-self target returns before using static metadata;
  2. missing quest template returns before reportable/action checks;
  3. non-reportable quest returns before auto-reward action checks;
  4. reportable auto-reward actions produce a non-live planned intent.
- Planned/self reportable paths can now carry coarse static metadata: `RewardRepeatCount`, `HasRewards`, `HasExtendedRewards`, `HasBonus`, and `HasQuestWorkItems`.
- The adapter still does not build a full `QuestFinishRewardTemplateProjection`, call production socket handling, mutate rewards/XP/quest state, execute custom rewards, persist mail, allocate ids, or send packets.

## Recommended Next Small Step After UOW-1084

Begin a full static `QuestFinishRewardTemplateProjection` extractor for Java quest reward XML, starting with non-item reward fields only and keeping operation-planner composition non-live.
