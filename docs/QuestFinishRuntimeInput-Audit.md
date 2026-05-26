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

## C# State After UOW-1076

- Added `QuestFinishCustomRewardRuntimeInputAssemblerService`.
- The assembler creates `QuestXpCustomRewardRuntimeInputAdapterOptions` without calling repositories, mutating player state, or invoking quest-finish gameplay.
- Disabled input returns `QuestXpCustomRewardRuntimeInputAdapterOptions.Disabled` without requiring account creation time, object-id allocation, or item templates.
- Enabled input stops before adapter execution when any required runtime dependency is missing:
  1. `accountCreationEpochMillis`;
  2. `nextObjectId`;
  3. `itemTemplates`.
- The assembler converts login-server account creation epoch milliseconds to a local `DateTime` through `GameServerOptions.Core.GetTimeZone()`, mirroring Java `ServerTime.ofEpochMilli(...).toLocalDateTime()` for future faction-pack window checks.
- Tests cover the UTC Asmodian window-start timestamp and a fixed-offset server-time conversion, but no production account/session wiring was added.

## Remaining Runtime Wiring Blockers After UOW-1076

1. Active C# account/session state still does not retain login-server account creation time after `CM_L2AUTH_LOGIN_CHECK`.
2. Production quest-finish reward mutation remains non-live; no socket or quest handler invokes the assembler.
3. Java `PlayerCommonData.setExp` live mutation is still absent, so custom reward level checks must not be enabled from stale pre-mutation player snapshots.
4. Transaction/failure ordering between custom reward receipt writes and system-mail persistence is still unresolved.

## C# State After UOW-1077

- Added `Player.AccountCreationEpochMillis` as a nullable runtime field.
- Added `PlayerAccountRuntimeStateService` to apply authenticated account access level, membership, and login-server account creation milliseconds to the active `Player`.
- `GameServerConnection` now retains positive `AccountAuthResult.CreationDate` after `CM_L2AUTH_LOGIN_CHECK` and copies it to the active player after successful enter-world.
- Missing or fake-auth account creation remains `null` instead of silently becoming Unix epoch zero; this keeps `QuestFinishCustomRewardRuntimeInputAssemblerService` able to refuse enabled execution when the Java account-creation dependency is absent.
- Tests cover the state applier only. They do not perform an encrypted socket login/enter-world integration run or compare against a Java runtime capture.

## Remaining Runtime Wiring Blockers After UOW-1077

1. Production quest-finish reward mutation remains non-live; no socket or quest handler invokes the assembler.
2. Java `PlayerCommonData.setExp` live mutation is still absent, so custom reward level checks must not be enabled from stale pre-mutation player snapshots.
3. Transaction/failure ordering between custom reward receipt writes and system-mail persistence is still unresolved.
4. The account-creation retention path is unit tested at the state-applier level only; end-to-end login-server auth, reconnect, and enter-world socket ordering still need integration coverage.

## C# State After UOW-1078

- Added `QuestFinishCustomRewardSessionRuntimeInputAdapterService`.
- The adapter bridges active player/session-shaped state into `QuestFinishCustomRewardRuntimeInputAssemblerService` without invoking quest finish, repositories, system mail, or socket sends.
- Disabled input returns inert assembler options without requiring a player, `IDFactory`, or item templates, and tests confirm it does not allocate object ids while disabled.
- Enabled input reads:
  1. `Player.AccountCreationEpochMillis`;
  2. `IDFactory.NextId` as the future Java `IDFactory.nextId` source;
  3. caller-supplied item templates intended to be `runtimeContext.DataManager.StaticData.ItemTemplates`.
- Enabled input still stops before executable options when account creation milliseconds, id factory, or item templates are missing.
- Tests cover missing dependencies and a created options path using the retained account creation timestamp, but no production quest-finish/socket path calls the adapter.

## Remaining Runtime Wiring Blockers After UOW-1078

1. Production quest-finish reward mutation remains non-live; no socket or quest handler invokes the session adapter or side-effect adapter.
2. Java `PlayerCommonData.setExp` live mutation is still absent, so custom reward level checks must not be enabled from stale pre-mutation player snapshots.
3. Transaction/failure ordering between custom reward receipt writes and system-mail persistence is still unresolved.
4. The item-template source is represented as an explicit input for testability; production wiring must pass `runtimeContext.DataManager.StaticData.ItemTemplates` and stay guarded when static data is unavailable.
5. End-to-end login-server auth/reconnect/enter-world socket ordering still needs integration coverage before claiming runtime parity.

## C# State After UOW-1079

- Added a non-live composition regression test for session-assembled disabled custom reward options.
- The test feeds `QuestFinishCustomRewardSessionRuntimeInputAdapterService` disabled options into `QuestFinishCustomRewardRuntimeSideEffectAdapterService`, then into quest-finish XP metadata composition.
- The disabled path keeps the original `QuestFinishRewardSideEffectContext`, does not call custom reward repositories, does not allocate object ids, and leaves custom reward sub-plan metadata non-live/default.
- This proves the new runtime-input bridge remains inert by default when composed with the existing quest-finish XP metadata path.

## Remaining Runtime Wiring Blockers After UOW-1079

1. Production quest-finish reward mutation remains non-live; no socket or quest handler invokes the session adapter or side-effect adapter.
2. Java `PlayerCommonData.setExp` live mutation is still absent, so custom reward level checks must not be enabled from stale pre-mutation player snapshots.
3. Transaction/failure ordering between custom reward receipt writes and system-mail persistence is still unresolved.
4. End-to-end login-server auth/reconnect/enter-world socket ordering still needs integration coverage before claiming runtime parity.
5. Java runtime comparison remains unavailable locally, so date/time and custom reward execution ordering stay source-derived only.

## C# State After UOW-1080

- Added `docs/QuestFinishProductionCallSite-Audit.md`.
- The audit maps the future production chain from Java `CM_DIALOG_SELECT.runImpl` self/reportable auto-reward handling through `QuestService.finishQuest`, `QuestService.giveReward`, `PlayerCommonData.addExp/setExp`, `PlayerController.onLevelChange`, bonus/faction custom reward services, and `SystemMailService.sendMail`.
- The audit identifies `GameServerConnection.HandleDialogSelectAsync` as the future C# socket entry point, but confirms it currently does not implement the Java self auto-reward branch or call `QuestFinishOperationPlanService`.
- The audit documents the future context assembly points for:
  1. `QuestFinishRewardTemplateProjection`;
  2. `QuestFinishRewardSideEffectContext`;
  3. `QuestFinishCustomRewardSessionRuntimeInputAdapterService`;
  4. disabled options for `QuestFinishCustomRewardRuntimeSideEffectAdapterService`;
  5. `QuestFinishOperationPlanService.CreatePlan`.
- No production code was changed and no live execution was enabled.

## Remaining Runtime Wiring Blockers After UOW-1080

1. Production `CM_DIALOG_SELECT` still does not detect Java's self/reportable auto-reward branch in C#.
2. Quest reward template projection from live static quest data is not wired into a socket path.
3. Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
4. Custom reward receipt store-before-mail ordering and system-mail persistence/fanout remain gated behind explicit opt-in boundaries.
5. Per-player ordering, live packet ordering, and Java runtime comparison remain unverified.

## C# State After UOW-1081

- Added `QuestDialogAutoRewardGuardPlanService`.
- The planner creates a non-live planned intent for Java `CM_DIALOG_SELECT.runImpl` self/player-target reportable quest auto-reward actions.
- It does not call `QuestFinishOperationPlanService`, mutate quest state, mutate XP, execute custom rewards, allocate ids, persist mail, or send packets.
- Tests verify Java's guard order and action id set: `108` plus `110..124`, with `109`, normal reward ids, and out-of-range values rejected.

## Remaining Runtime Wiring Blockers After UOW-1081

1. Production `GameServerConnection.HandleDialogSelectAsync` still does not call the guard planner.
2. C# static quest data still does not expose a full `QuestTemplate.can_report`/reward projection surface for production quest-finish planning.
3. The planner returns only a disabled intent; no `QuestFinishRewardTemplateProjection` or `QuestFinishRewardSideEffectContext` is built from live static data.
4. Java `PlayerCommonData.setExp` live mutation remains absent, so custom rewards must stay disabled.
5. Custom reward receipt/mail execution and per-player live ordering remain gated and unverified.
