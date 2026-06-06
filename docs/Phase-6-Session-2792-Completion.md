# Phase 6 Session 2792 Completion

## Unit Of Work

`[Phase 6][UOW-2792] Wire live quest finish abyss point rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` abyss point rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward`, plus `services/abyss/AbyssPointsService.java#addAp`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.AbyssPoints` descriptors through `QuestRewardService.ApplyApReward`.
- Client-visible/state/persistence effect changed: supported quest finish AP rewards mutate live `Player.AbyssRank`, send `SmSystemMessage.CombatMyAbyssPointGain`, send `SmAbyssRank`, run existing rank-change side effects when rank changes, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player abyss-rank state and sends real AP reward packets during quest finish.

## Java Parity Notes

- Java `QuestService.giveReward` applies `Rates.AP_QUEST` unless the quest category is `NON_COUNT`, then calls `AbyssPointsService.addAp(player, ap)`.
- Java `AbyssPointsService.addAp` mutates the player's abyss rank, sends `STR_MSG_COMBAT_MY_ABYSS_POINT_GAIN` or `STR_MSG_USE_ABYSSPOINT`, sends `SM_ABYSS_RANK` when AP/rank changes, and performs rank-change side effects.
- C# reuses `QuestRewardService.ApplyApReward`, which already models the rate and `NON_COUNT` branch, then sends the AP plan's player packets and invokes existing rank-change side effects.
- The previous handoff suggested selectable auto rewards. Discovery found Java `QuestService.getRewardIndex` recognizes `SELECTED_QUEST_REWARD1..15` (`8..22`), while `CM_DIALOG_SELECT` self-report finish calls `QuestService.finishQuest` for `SELECTED_QUEST_AUTO_REWARD1..15` (`110..124`). Since Java source does not map `110..124` to selectable regular reward indexes, this UOW did not invent that behavior.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

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

## Test Evidence

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesAbyssPointsAndCompletesQuest` | Regression | Java source review: `QuestService.giveReward` and `AbyssPointsService.addAp` | Live socket quest finish applies AP reward, sends AP gain/rank packets, mutates player abyss rank, and completes quest | Filtered C# boundary test | Does not assert DB persistence or rank-change broadcast branch |
| `ApplyApReward_AppliesConfiguredQuestRateAndAddsApThroughPlanner` | Unit | Java source review: `Rates.AP_QUEST` and `AbyssPointsService.addAp` | Quest AP rate scaling and AP packet plan | Existing focused C# unit test | Service-level evidence only |
| `ApplyApReward_SkipsQuestRateForJavaNonCountCategory` | Unit | Java source review: `QuestCategory.NON_COUNT` bypass | NON_COUNT AP rewards skip quest AP rate | Existing focused C# unit test | Socket AP test uses regular quest category |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, work-item removal, title rewards, and AP rewards are live for guarded self-target reportable auto-reward quests. |
| `com.aionemu.gameserver.services.QuestService.giveReward` AP branch | `Aion.GameServer.Network.Aion.GameServerConnection.ApplyQuestFinishApRewardAsync` | Runtime player state mutation | Partial | Regression Tested | Partial Parity | Calls the existing AP reward service from live quest finish and sends AP packets before quest completion. |
| `com.aionemu.gameserver.services.abyss.AbyssPointsService.addAp` | `Aion.GameServer.Services.AbyssPointsService.AddAp` | Service | Partial | Unit Tested | Partial Parity | Mutates AP, sends player AP packets, and exposes rank-change side effects. Quest finish uses this service; DB persistence for AP quest finish is not newly wired. |

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, title rewards, and AP rewards now run live for the guarded branch. Selectable rewards, DP/GP/cube/warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, AP quest-finish persistence, and broad nearby quest fanout remain incomplete.
