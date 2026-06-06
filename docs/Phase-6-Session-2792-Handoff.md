# Phase 6 Session 2792 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2792] Wire live quest finish abyss point rewards`

Commit made in this session:

- `[Phase 6][UOW-2792] Wire live quest finish abyss point rewards`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, work-item removal, title rewards, and AP rewards.
- AP rewards are now admitted by the live auto-reward descriptor allow-list.
- Live AP quest rewards call `QuestRewardService.ApplyApReward`, which applies Java quest AP rates unless the quest category is `NON_COUNT`.
- The live handler sends AP gain/rank packets from `AbyssPointsService.AddAp`, runs existing rank-change side effects when rank changes, and then sends `SmQuestAction.Update`.
- Selectable auto rewards were investigated and not wired: Java `QuestService.getRewardIndex` recognizes `SELECTED_QUEST_REWARD1..15` (`8..22`), while the self-report branch passes `SELECTED_QUEST_AUTO_REWARD1..15` (`110..124`) into `finishQuest`. No Java source path was found that maps `110..124` to selectable regular reward indexes.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.model.DialogAction`.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.
- `com.aionemu.gameserver.services.QuestService.getRewardItems`.
- `com.aionemu.gameserver.services.QuestService.giveReward`.
- `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp`.
- `com.aionemu.gameserver.model.gameobjects.player.Rates.AP_QUEST`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2792-Completion.md`.
- `docs/Phase-6-Session-2792-Handoff.md`.

## Validation Decision

- Changed surface: live socket-side quest finish reward execution.
- Specific behavior/contract: Java `QuestService.giveReward` AP reward branch mutates player AP, sends AP gain/rank packets, and completes the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live AP packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited socket boundary plus adjacent AP reward service.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with a live AP reward projection and observes player AP state, AP packets, and quest completion ordering.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

Result: Passed, 28 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side AP mutation and packet fanout.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward branch | `GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, and AP rewards are live for guarded self-target reportable auto-reward quests. |
| `DialogAction` reward action ids | `CmDialogSelect` constants plus `QuestFinishSocketInputAssemblyPlanService` | Constants / packet input | Partial | Manual Only | Partial Parity | Source review found `SELECTED_QUEST_AUTO_REWARD1..15` is not equivalent to `SELECTED_QUEST_REWARD1..15` for `QuestService.getRewardIndex`; do not wire selectable auto rewards without stronger Java runtime evidence. |
| `QuestService.giveReward` AP branch | `GameServerConnection.ApplyQuestFinishApRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls existing quest AP service from live quest finish and sends AP packets before quest completion. |
| `AbyssPointsService.addAp` | `AbyssPointsService.AddAp` | Service | Partial | Unit Tested | Partial Parity | Mutates AP and exposes packets/rank side effects. Quest finish uses this service; AP quest-finish persistence was not newly wired. |

## Known Gaps

- Live quest finish still does not support selectable item rewards, class-selectable rewards, bonus rewards, DP/GP rewards, cube/warehouse expansions, challenge task completion, quest-completed callbacks, NPC faction completion, NPC-target dialog quest paths, or broad nearby quest refresh fanout.
- AP reward persistence is not newly wired in quest finish. Periodic player saves can persist abyss rank later, but this UOW did not add a direct quest-finish AP persistence mutation.
- Rank-change side effects reuse existing C# behavior but were not forced by the AP boundary fixture because the fixture reward does not cross an abyss rank threshold.
- No Java runtime/golden fixture exists for quest finish socket AP behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2793] Wire live quest finish divine point rewards`

- Deferred/live behavior advanced: expand quest finish live execution to Java `QuestService.giveReward` DP rewards.
- Java source of truth: `QuestService.giveReward` branch `if (rewards.getDp() != 0) player.getCommonData().addDp(rewards.getDp())`.
- C# runtime artifact to wire/fix: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` should admit `QuestFinishRewardNonItemAction.DivinePoints` and call the existing `QuestRewardService.ApplyDpRewardAsync` runtime path.
- Client-visible/state/persistence effect expected: completing a supported auto-reward quest mutates live player DP, sends existing DP/stat packets from the live resource-change path, and then sends `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: it will mutate live player DP state and send real resource/stat packets from the socket handler for a currently deferred reward path.

## Focused Validation Recipe For Next UOW

- Specific behavior/contract to prove: Java `QuestService.giveReward` DP reward branch mutates live player DP and emits the existing DP resource packets before quest completion.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"`
- Focused Java/Maven command: not expected unless a narrow Java fixture is added; no existing Java socket fixture was found.
- Broad-validation trigger: none.
- Broad .NET decision: do not run unless focused evidence exposes wider risk.

## Safe Runtime Candidates

- Wire DP quest finish rewards through existing `QuestRewardService.ApplyDpRewardAsync`.
- Wire GP quest finish rewards through existing `QuestRewardService.ApplyGpReward`, while checking persistence through `IAbyssRankRepository`/`GloryPointsService`.
- Wire cube/warehouse quest reward expansions if existing expansion services can be called from quest finish without NPC-target assumptions.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 7.
- Total artifacts ported or wired in latest UOW: 1 live socket branch extension for AP rewards.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 4.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
