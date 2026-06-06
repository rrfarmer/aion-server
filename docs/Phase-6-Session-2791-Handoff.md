# Phase 6 Session 2791 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2791] Wire live quest finish title rewards`

Commit made in this session:

- `[Phase 6][UOW-2791] Wire live quest finish title rewards`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, work-item removal, and title rewards.
- Title rewards are now admitted by the live auto-reward descriptor allow-list.
- New quest titles mutate live `Player.Titles`, attempt persistence through `PlayerEnterWorldService.SaveTitleAddActionMutationAsync` when available, register expiration bookkeeping through `ExpirableTaskService`, send `SmSystemMessage.QuestGetRewardTitle`, send `SmTitleInfo`, and then complete the quest with `SmQuestAction.Update`.
- Already-known quest title rewards send `SmSystemMessage.TooltipLearnedTitle` and still complete the quest, matching Java's duplicate-title branch where `QuestService.giveReward` ignores the returned `false`.
- Race-mismatched title rewards do not send the planner-only plain text race failure from live quest finish; Java `TitleList.addTitle` returns false silently for this path.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.
- `com.aionemu.gameserver.services.QuestService.giveReward`.
- `com.aionemu.gameserver.model.gameobjects.player.title.TitleList.addTitle`.
- `com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO`.
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_QUEST_GET_REWARD_TITLE`.
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_TOOLTIP_LEARNED_TITLE`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2791-Completion.md`.
- `docs/Phase-6-Session-2791-Handoff.md`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests"
```

Result: Passed, 13 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side title mutation and packet fanout.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward branch | `GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, and title rewards are live for guarded self-target reportable auto-reward quests. |
| `QuestService.giveReward` title branch | `GameServerConnection.TryApplyQuestFinishTitleRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Adds live titles, sends Java-equivalent quest title packets, and handles duplicate/race guard outcomes. Repository-backed socket persistence was not separately asserted. |
| `TitleList.addTitle(title, true, 0)` | `QuestRewardSideEffectPlanService.CreateTitleRewardPlan` plus live socket application | Runtime title acquisition | Partial | Regression Tested | Partial Parity | Planner provides Java decision shape; live handler now executes state and packet side effects for quest finish. |

## Known Gaps

- Live quest finish still does not support selectable item rewards, class-selectable rewards, bonus rewards, AP/DP/GP rewards, cube/warehouse expansions, challenge task completion, quest-completed callbacks, NPC faction completion, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- Title reward persistence is attempted when `PlayerEnterWorldService` is available; this UOW did not add a repository-backed socket persistence test.
- Missing title templates abort the guarded live reward branch; Java would throw `IllegalArgumentException`, so this is conservative partial parity rather than verified parity.
- No Java runtime/golden fixture exists for quest finish socket title behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2792] Wire live quest finish selectable item rewards`

- Deferred/live behavior advanced: expand quest finish live execution from fixed item rewards to Java-selected reward groups for `SELECTED_QUEST_AUTO_REWARD1..15`.
- Java source of truth: `CM_DIALOG_SELECT` selected quest auto-reward action mapping, `QuestService.getRewardItems`, and `QuestService.giveReward` item reward selection behavior.
- C# runtime artifact to wire/fix: `GameServerConnection.TryCreateQuestFinishAutoRewards` / `TryAddQuestFinishItemRewards` should map the client-selected reward action to the projected selected item reward and apply it through the existing live inventory reward mutation path.
- Client-visible/state/persistence effect expected: completing a supported selected-reward quest grants the client-selected item, persists or tracks inventory state through the existing reward mutation shape when available, sends inventory add/update/cube packets, and then sends `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: it will mutate live inventory state and send real reward packets from the socket handler for a currently deferred reward path.

## Safe Runtime Candidates

- Wire selectable item rewards for `SELECTED_QUEST_AUTO_REWARD1..15` if the existing projection can map action id and reward group without ambiguity.
- Wire AP/DP/GP quest finish rewards if existing player stat mutation and packet helpers are ready for Java `QuestService.giveReward`.
- Wire challenge task completion from quest finish if `ChallengeTaskService` live state and persistence are ready.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 7.
- Total artifacts ported or wired in latest UOW: 1 live socket branch extension for title rewards.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
