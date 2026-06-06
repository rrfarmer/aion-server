# Phase 6 Session 2794 Completion

## Unit Of Work

`[Phase 6][UOW-2794] Wire live quest finish glory point rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` glory point rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward` branch `if (rewards.getGp() != 0) GloryPointsService.addGp(player.getObjectId(), Rates.GP.calcResult(player, rewards.getGp()))`, plus `GloryPointsService.addGp`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.GloryPoints` descriptors through `QuestRewardService.ApplyGpReward`.
- Client-visible/state/persistence effect changed: supported quest finish GP rewards mutate live `Player.AbyssRank` GP/daily GP/weekly GP state, send `SmSystemMessage.GloryPointGain`/`GloryPointLose`, send `SmAbyssRank` when current GP changes, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player abyss-rank GP state and sends real glory/rank packets during quest finish.

## Java Parity Notes

- Java `QuestService.giveReward` applies `Rates.GP.calcResult(player, rewards.getGp())` before calling `GloryPointsService.addGp`.
- Java `GloryPointsService.addGp` returns for zero GP, resolves the online player from `World`, mutates online `AbyssRank`, sends a glory gain/loss system message, and sends `SM_ABYSS_RANK` only when the current GP delta is non-zero.
- C# reuses `QuestRewardService.ApplyGpReward`, which applies configured GP rates and calls `GloryPointsService.AddGp`; the live handler now sends the returned player packets before quest completion.
- This UOW did not add direct quest-finish GP persistence. Java's online GP branch mutates the online player and does not call `AbyssRankDAO.addGp`; later player-save behavior remains the persistence path.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live socket-side quest finish reward execution.
- Specific behavior/contract: Java `QuestService.giveReward` GP reward branch applies GP rate, mutates player abyss-rank GP, emits the glory/rank packet chain, and completes the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests|FullyQualifiedName~GloryPointsServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live GP packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited socket boundary plus adjacent GP reward/rate services.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with a live GP reward projection and observes player GP state, glory/rank packets, and quest completion ordering.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests|FullyQualifiedName~GloryPointsServiceTests"
```

Result: Passed, 37 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side GP mutation and packet fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesGloryPointsAndCompletesQuest` | Regression | Java source review: `QuestService.giveReward` and `GloryPointsService.addGp` | Live socket quest finish applies GP reward, sends glory/rank packets, mutates player GP/daily GP/weekly GP, and completes quest | Filtered C# boundary test | Does not assert DB persistence |
| `ApplyGpReward_AppliesConfiguredGpRateAndAddsGpThroughPlanner` | Unit | Java source review: `Rates.GP` and `GloryPointsService.addGp` | Quest GP rate scaling, GP mutation, and packet plan | Existing focused C# unit test | Service-level evidence only |
| `AddGp_AddsPositiveGpDailyWeeklyStatsAndRankPacketLikeJava` | Unit | Java source review: `GloryPointsService.addGp` online branch | Online GP gain mutates stats and creates glory/rank packets | Existing focused C# unit test | Does not cover quest socket branch by itself |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, and GP rewards are live for guarded self-target reportable auto-reward quests. |
| `com.aionemu.gameserver.services.QuestService.giveReward` GP branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls the existing GP reward service from live quest finish and sends glory/rank packets before quest completion. |
| `com.aionemu.gameserver.services.abyss.GloryPointsService.addGp` | `Aion.GameServer.Services.GloryPointsService.AddGp` | Service | Partial | Unit Tested | Partial Parity | Mutates online GP and exposes player packets. Offline DAO branch is modeled separately and was not part of quest-finish online reward wiring. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, DP rewards, and GP rewards now run live for the guarded branch. Selectable rewards, cube/warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence, and broad nearby quest fanout remain incomplete.
