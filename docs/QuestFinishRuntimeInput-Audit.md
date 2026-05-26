# Quest-Finish Runtime Input Audit

Date: May 26, 2026

## Scope

This audit maps the runtime inputs needed before `QuestFinishCustomRewardRuntimeSideEffectAdapterService` can be called from a production quest-finish path.

Java source remains authoritative:

- `game-server/src/com/aionemu/gameserver/services/QuestService.java#finishQuest`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java#giveReward`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PlayerCommonData.java#setExp`
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java#onLevelChange`
- `game-server/src/com/aionemu/gameserver/services/BonusPackService.java#addPlayerCustomReward`
- `game-server/src/com/aionemu/gameserver/services/FactionPackService.java#sendRewards`

## Java Runtime Inputs

| Input | Java Source | Java Behavior | C# Current Home | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Player/account identity | `QuestEnv.getPlayer`; `player.getAccount().getId()` | Quest finish and custom reward DAO gates use live player and account ids. | `Player.ObjectId`; `Player.AccountId` | Available | Loaded by `PlayerEnterWorldRepository` and carried on `Player`. |
| Player account creation time | `player.getAccount().getCreationDate`; `ServerTime.ofEpochMilli(...).toLocalDateTime()` | Faction pack checks race-specific account creation windows before DAO receipt load/store. | `AccountAuthResult.CreationDate` from login server | Not attached to active player | `GameServerConnection` currently reads auth creation date but does not store it on connection or `Player`; `Player` has no account-creation field. |
| Item templates | `DataManager.ITEM_DATA.getItemTemplate` | Faction pack filters opposite-race items before system mail. | `_runtimeContext.DataManager.StaticData.ItemTemplates` | Available | Many existing handlers already pass static item templates from runtime context. |
| Object id allocation | `IDFactory.getInstance().nextId()` | System mail item/letter creation allocates ids during reward mail send. | injected `IDFactory.NextId()` | Available with null guard | `GameServerConnection` and `Program` already have `IDFactory`; some tests construct connections without it. |
| Received mail time | `System.currentTimeMillis` / DAO letter creation time through mail service | System mail persists a received timestamp. | caller-supplied `QuestXpCustomRewardRuntimeInputAdapterOptions.ReceivedTime` | Needs policy | Existing adapter requires explicit time; production caller must decide local server time source and timezone. |
| XP level mutation snapshot | `PlayerCommonData.setExp` mutates level before `PlayerController.onLevelChange` | Custom rewards see `player.getLevel() == 65` after the level update. | `QuestXpRewardPlan` is non-mutating and uses supplied `Player` snapshot | Blocked for live parity | UOW-1074 tests document the gap by simulating pre/post mutation snapshots. |
| Quest reward template context | `QuestTemplate`, `QuestState`, `QuestEnv` | Quest finish selects reward group, reward items, non-item rewards, and target NPC name. | non-live `QuestFinishRewardTemplateProjection` plus `QuestFinishRewardSideEffectContext` | Partial | Existing planners can compose metadata, but production quest handler extraction is not live. |
| Opt-in execution policy | Java executes custom reward DAO/mail side effects automatically on level change. | C# deliberately requires explicit disabled-by-default options. | `QuestXpCustomRewardRuntimeInputAdapterOptions.EnableCustomRewardExecution` | Intentional Difference | Keep disabled until transaction, mail persistence, packet, and XP mutation boundaries are ready. |

## C# Wiring Findings

- `GameServerConnection` receives `AccountAuthResult.CreationDate` during `CM_L2AUTH_LOGIN_CHECK`, but currently stores only account id, account name, access level, and membership.
- `Player` currently stores account id, access level, and membership, but not account creation time.
- `PlayerEnterWorldRepository.LoadPlayerAsync` reads the `players` row and does not hydrate account creation time from login auth or an account table.
- `IDFactory` is registered in DI and available to `GameServerConnection`; `NextId` already mirrors Java invalid-id skipping and locking.
- `runtimeContext.DataManager.StaticData.ItemTemplates` is the established C# source for item templates in packet handlers and services.
- No production C# quest-finish packet path currently calls `QuestFinishOperationPlanService` for live reward mutation. The planner remains metadata-only.

## Recommended Runtime Input Contract

Before production invocation, add a narrow input assembler rather than calling the adapter directly from a socket handler.

Suggested shape:

| Field | Source | Required Before Repository Access? | Notes |
| --- | --- | --- | --- |
| `QuestFinishRewardSideEffectContext` | existing quest-finish planner input | Yes | Must already include XP `LevelChangeContextInput` and an experience table. |
| `EnableCustomRewardExecution` | explicit config/test gate | Yes | Default must remain false. |
| `NextObjectId` | `IDFactory.NextId` | Yes when enabled | Missing id factory must stop before custom reward DAO access. |
| `ReceivedTime` | server-local time provider | Yes when enabled | Should be deterministic in tests; Java uses server-local time for mail creation. |
| `FactionPackAccountCreationLocalTime` | login auth creation millis converted through configured server timezone | Yes for faction parity | Needs a C# equivalent to Java `ServerTime.ofEpochMilli`. |
| `ItemTemplates` | `runtimeContext.DataManager.StaticData.ItemTemplates` | Required for full faction filtering | Missing table should be treated as a dependency gap before live execution. |

## Blockers Before Production Wiring

1. Preserve account creation time after login.
   - Option A: add an account creation field to `GameServerConnection` and transfer it to `Player` on enter world.
   - Option B: add an active account/session snapshot object and pass it into quest-finish runtime input assembly.
   - Do not use `players.creation_date`; Java faction pack uses account creation date.

2. Define Java-equivalent server-time conversion.
   - Java uses `ServerTime.ofEpochMilli(...).toLocalDateTime()`.
   - C# should use configured game-server timezone and must test boundary timestamps around the faction pack windows.

3. Keep XP mutation non-live until `PlayerCommonData.setExp` behavior has a C# execution boundary.
   - Java custom rewards run after level mutation.
   - The current C# planner cannot safely execute level-65 custom reward checks from a level-64 player snapshot.

4. Define transaction/failure ordering for custom reward receipt rows and system mail.
   - Java stores receipt before sending all system mails.
   - C# execution currently can write receipt rows while mail remains metadata unless a later explicit mail executor is called.

5. Add production quest-finish handler extraction.
   - Existing C# quest-finish reward work is planner metadata; live `CM_DIALOG_SELECT` quest reward handling is not the same as Java `QuestService.finishQuest`.

## Safe Next Implementation Step

Add a small, test-only or disabled-by-default runtime input assembler that:

1. refuses to enable custom rewards when account creation time is missing;
2. refuses to enable custom rewards when `IDFactory` or item templates are missing;
3. converts login-server account creation milliseconds using `GameServerOptions.Core.GetTimeZone()`;
4. returns `QuestXpCustomRewardRuntimeInputAdapterOptions.Disabled` by default;
5. does not call repositories or mutate player state.

This should be implemented and tested before any production socket/quest handler calls `QuestFinishCustomRewardRuntimeSideEffectAdapterService`.
