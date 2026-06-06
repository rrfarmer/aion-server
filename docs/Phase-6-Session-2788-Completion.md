# Phase 6 Session 2788 Completion

## Completed UOW

[Phase 6][UOW-2788] Wire live quest finish kinah rewards

## Runtime Progress Gate

- Deferred/live behavior advanced: reportable self-target quest auto-reward dialog selection now advances from XP-only live rewards to Java-equivalent kinah reward mutation for supported non-item reward projections.
- Java source of truth: `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java` self-target `SELECTED_QUEST_AUTO_REWARD*` branch, `game-server/src/com/aionemu/gameserver/services/QuestService.java` `finishQuest` and `giveReward`, and Java `Storage.increaseKinah(..., INC_KINAH_QUEST)`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleDialogSelectAsync` now accepts kinah and XP non-item descriptors in the guarded quest finish branch, applies kinah through `QuestRewardService.CreateKinahRewardPlan`, persists reward inventory mutations when the live enter-world service is available, mutates `Player.InventoryItems`, and sends an inventory packet before quest completion update.
- Client-visible/state effect: completing a kinah-only auto-reward quest increases the live cube kinah item and emits `SmInventoryUpdateItem` with `IncreaseKinahQuest` before `SmQuestAction.Update`.
- Why this is runtime progress: this is not preview-only or test-only; it mutates live inventory and quest state and sends real server packets from the live dialog-select socket handler.

## Implementation

- Generalized the guarded quest finish auto-reward branch from XP-only descriptors to supported XP-or-kinah non-item descriptors while continuing to reject item rewards and unsupported side effects.
- Added live quest-finish kinah application through the existing `QuestRewardService.CreateKinahRewardPlan`, including Java quest kinah rate handling and the `INC_KINAH_QUEST` packet update type.
- Reused `PlayerEnterWorldService.SaveInventoryRewardMutationAsync` for reward inventory persistence when available, then replaced or added the live kinah item in `Player.InventoryItems`.

## Tests

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesKinahAndCompletesQuest` | Runtime socket regression | `CM_DIALOG_SELECT.runImpl -> QuestService.finishQuest -> giveReward -> Storage.increaseKinah(..., INC_KINAH_QUEST)` | Live `CmDialogSelect` auto-reward path mutates cube kinah, sends `SmInventoryUpdateItem` with `IncreaseKinahQuest`, then completes quest state and sends `SmQuestAction.Update` | C# live handler test with Java-reviewed packet/state order | Existing-kinah path covered; missing-kinah creation path is wired but not separately asserted |
| `HandleDialogSelectAsync_ReportableAutoRewardQuestAppliesXpAndCompletesQuest` | Runtime socket regression | `CM_DIALOG_SELECT.runImpl -> QuestService.finishQuest -> giveReward -> PlayerCommonData.addExp` | Existing XP live branch still applies XP and completes quest state | C# live handler regression | XP-only branch remains partial relative to all Java reward effects |
| `QuestRewardServiceTests` | Runtime service regression | `QuestService.giveReward`, `Rates.QUEST_KINAH`, `Rates.XP_QUEST`, `PlayerCommonData.addExp/setExp` | Existing reward rate and packet planning contracts remain intact | C# focused service tests | No Java executable fixture for the live quest finish socket branch |

## Validation Decision

- Changed surface: live `CmDialogSelect` quest finish branch, quest kinah inventory mutation, inventory packet emission, quest completion packet ordering.
- Specific behavior/contract: Java self-target reportable auto-reward quest finish applies kinah with `INC_KINAH_QUEST` before quest completion update.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

- Result: Passed, 23 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.
- Focused Java/Maven command: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` socket-side kinah mutation.
- Broad .NET decision: skipped unfiltered solution validation; the filtered command compiled the affected server and covered the edited socket path plus the reused reward service.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP and kinah-only reportable self-target auto-reward quests are live; NPC-target dialog quest paths and unsupported reward mixes remain deferred. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` plus quest finish services | Runtime service/socket path | Partial | Regression Tested | Partial Parity | Applies supported XP/kinah non-item rewards and completes quest state; item rewards, work items, challenge tasks, callbacks, NPC faction completion, and full nearby quest runtime fanout remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.giveReward` kinah branch | `Aion.GameServer.Services.QuestRewardService.CreateKinahRewardPlan` called by `GameServerConnection` | Runtime reward application | Partial | Regression Tested | Partial Parity | Existing cube kinah item update is verified from live quest finish; missing-kinah item creation uses existing plan/add packet path but still needs focused socket coverage. |

## Summary Metrics

- Total Java artifacts touched/discovered: 3.
- Total artifacts ported or wired this UOW: 1 live socket branch extension plus existing kinah reward service and inventory persistence hooks.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.

## Remaining Gaps

- Quest finish live execution supports only XP and kinah non-item descriptors and still rejects item rewards or unsupported non-item side effects.
- Missing-kinah item creation is wired through the existing reward plan/add-packet branch but not separately verified from the socket handler.
- Work-item removal, challenge task completion, quest-completed callbacks, NPC faction completion, broad nearby quest refresh fanout, and repository-backed socket persistence coverage remain partial.
- No Java runtime/golden fixture exists for live quest finish socket kinah behavior.
