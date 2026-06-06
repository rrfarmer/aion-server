# Phase 6 Session 2793 Completion

## Unit Of Work

`[Phase 6][UOW-2793] Wire live quest finish divine point rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` divine point rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward` branch `if (rewards.getDp() != 0) player.getCommonData().addDp(rewards.getDp())`, plus `PlayerCommonData.addDp/setDp`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.DivinePoints` descriptors through `QuestRewardService.ApplyDpRewardAsync`.
- Client-visible/state/persistence effect changed: supported quest finish DP rewards mutate live `Player.Dp`, broadcast `SmDpInfo`, send the visual stats/speed packets from the existing resource-change path, send `SmStatUpdateDp`, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player DP state and sends real DP/stat packets during quest finish.

## Java Parity Notes

- Java `QuestService.giveReward` applies reward DP directly through `PlayerCommonData.addDp`.
- Java `PlayerCommonData.setDp` broadcasts `SM_DP_INFO`, updates stats visually, then sends `SM_STATUPDATE_DP`.
- C# reuses `QuestRewardService.ApplyDpRewardAsync`, which delegates to `WorldNpcResourceStatsService.AddPlayerDpAsync`; that service carries the Java packet-order breadcrumb and owns the live DP mutation/packet dispatch.
- This UOW did not add direct quest-finish DP persistence. It mutates the live player model; any later persistence remains the existing player-save responsibility.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Validation Decision

- Changed surface: live socket-side quest finish reward execution.
- Specific behavior/contract: Java `QuestService.giveReward` DP reward branch mutates player DP, emits the DP/stat packet chain, and completes the quest.
- Focused C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"`
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live DP packet fanout.
- Broad-validation trigger: none.
- Broad .NET decision: skipped; the filtered command compiled the affected project and exercised the edited socket boundary plus adjacent DP reward service.
- Why this scope is sufficient: the test drives `HandleDialogSelectAsync` with a live DP reward projection and observes player DP state, DP/stat packets from the registry path, and quest completion ordering.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

Result: Passed, 29 total, 0 failed, 0 skipped.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` socket-side DP mutation and packet fanout.

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesDivinePointsAndCompletesQuest` | Regression | Java source review: `QuestService.giveReward` and `PlayerCommonData.addDp/setDp` | Live socket quest finish applies DP reward, sends DP info/stats/speed/DP stat packets, mutates player DP, and completes quest | Filtered C# boundary test | Does not assert DB persistence |
| `ApplyDpRewardAsync_AddsQuestDpThroughPacketedBoundary` | Unit | Java source review: `PlayerCommonData.addDp/setDp` | Quest DP service mutates DP and emits the Java-ordered packet chain | Existing focused C# unit test | Service-level evidence only |
| `ApplyDpRewardAsync_RequiresPlayerAndUsesOnlineMaxDp` | Unit | Java source review: online `PlayerGameStats.getMaxDp()` cap | Online DP rewards clamp to the current max DP default | Existing focused C# unit test | Full MAXDP modifiers remain deferred |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, and DP rewards are live for guarded self-target reportable auto-reward quests. |
| `com.aionemu.gameserver.services.QuestService.giveReward` DP branch | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls the existing DP reward service from live quest finish and emits DP/stat packets before quest completion. |
| `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.addDp/setDp` | `Aion.GameServer.Services.WorldNpcResourceStatsService.AddPlayerDpAsync` | Service | Partial | Unit Tested | Partial Parity | Mutates DP and sends the Java-ordered DP info, visual stats/speed, and DP stat packets. Full MAXDP modifier parity remains incomplete. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, title rewards, AP rewards, and DP rewards now run live for the guarded branch. Selectable rewards, GP/cube/warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, direct quest-finish reward persistence, and broad nearby quest fanout remain incomplete.
