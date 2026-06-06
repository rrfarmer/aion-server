# Phase 6 Session 2787 Completion

## Completed UOW

[Phase 6][UOW-2787] Wire live quest finish XP application

## Runtime Progress Gate

- Deferred/live behavior advanced: reportable self-target quest auto-reward dialog selection now advances from a composed plan into live quest finish XP application.
- Java source of truth: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java` self-target `SELECTED_QUEST_AUTO_REWARD*` branch, `game-server/src/com/aionemu/gameserver/services/QuestService.java` `finishQuest` and `giveReward`, and `PlayerCommonData.addExp/setExp`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleDialogSelectAsync` now runs the guarded quest-finish input path, applies XP rewards through `QuestRewardService.ApplyXpReward`, mutates the player's quest state to `COMPLETE`, and sends XP plus `SmQuestAction.Update` packets for the XP-only safe branch.
- Client-visible/state effect: completing an XP-only auto-reward quest changes live `Player.Exp`/`Level`/`ReposeEnergy`, replaces the live `PlayerQuestState` with a completed state, and emits `SmStatUpdateExp`, the XP system message, and `SmQuestAction.Update` from the socket handler.
- Why this is runtime progress: this is not preview-only or test-only; it wires the live dialog-select packet path to Java-equivalent XP state mutation and real server packet sends.

## Implementation

- Added a guarded quest finish auto-reward branch near the start of `HandleDialogSelectAsync`, matching Java's self-target/reportable auto-reward entry point before NPC dialog routing.
- Restricted live execution to XP-only reward projections with no item rewards or non-XP side effects, so mixed reward quests remain deferred rather than silently dropping Java reward effects.
- Applied each XP reward through `QuestRewardService.ApplyXpReward`, then completed the quest state with `QuestFinishStateMutationService.ApplyRewardCompletion`, optionally persisted the quest row through the existing quest-update repository shape, and sent `SmQuestAction.Update`.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesXpAndCompletesQuest` | Runtime socket regression | `CM_DIALOG_SELECT.runImpl -> QuestService.finishQuest -> giveReward -> PlayerCommonData.addExp` | Live `CmDialogSelect` auto-reward path sends XP stat/system packets, mutates player XP, completes quest state, and sends `SmQuestAction.Update` | C# live handler test with Java-reviewed packet/state order | XP-only; item, kinah, title, AP/DP/GP, work-item removal, callback, NPC faction, and full persistence fanout remain partial |
| `QuestRewardServiceTests` | Runtime service regression | `QuestService.giveReward`, `Rates.XP_QUEST`, `PlayerCommonData.addExp/setExp` | XP rate, active legion bonus, player state mutation, repose, and XP packet creation remain intact | C# focused service tests | No Java executable fixture for live quest finish XP branch |

## Validation Decision

- Changed surface: live `CmDialogSelect` quest finish branch, quest XP state mutation, quest completion packet emission.
- Specific behavior/contract: Java self-target reportable auto-reward quest finish applies XP before quest completion update.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

- Result: Passed, 22 total, 0 failed, 0 skipped.
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` socket-side XP mutation.
- Broad .NET decision: skipped unfiltered solution validation; the filtered command compiled the affected server and covered the edited socket path plus the reused reward service.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | XP-only reportable self-target auto-reward quests are live; NPC-target dialog quest paths and mixed reward branches remain deferred. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` plus quest finish services | Runtime service/socket path | Partial | Regression Tested | Partial Parity | Applies XP and completes quest state in Java order for XP-only rewards; item rewards, work items, challenge tasks, callbacks, NPC faction completion, and full nearby quest runtime fanout remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.giveReward` XP branch | `Aion.GameServer.Services.QuestRewardService.ApplyXpReward` called by `GameServerConnection` | Runtime reward application | Partial | Regression Tested | Partial Parity | The live quest finish socket path now calls the XP application boundary; other non-item reward branches are still not live from quest finish. |

## Summary Metrics

- Total Java artifacts touched/discovered: 3.
- Total artifacts ported or wired this UOW: 1 live socket branch plus existing XP reward service and quest state mutation services.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.

## Remaining Gaps

- Quest finish live execution is limited to XP-only reward projections.
- Mixed rewards still need live kinah, item, title, AP/DP/GP, cube/warehouse, work-item removal, callback, NPC faction, and persistence fanout before broad quest completion parity can be claimed.
- No Java runtime/golden fixture exists for live quest finish socket XP behavior.
