# Phase 6 Session 2787 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

[Phase 6][UOW-2787] Wire live quest finish XP application

Commit made in this session:

- `[Phase 6][UOW-2787] Wire live quest finish XP application`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` now handles the Java self-target/reportable quest auto-reward branch for XP-only rewards.
- The live handler uses `QuestFinishSocketGuardedInputAssemblyPlanService` to preserve existing target/template/can-report/auto-reward guards.
- XP reward application is live through `QuestRewardService.ApplyXpReward`, so player XP/level/repose state mutates and XP packets are sent from the socket handler.
- Quest finish state mutation is live for this branch: the player's quest state is replaced with `COMPLETE`, vars are cleared, complete count increments, and `SmQuestAction.Update` is sent.
- The branch intentionally refuses mixed reward projections so Java rewards are not partially dropped.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.services.QuestService` `finishQuest`.
- `com.aionemu.gameserver.services.QuestService` `giveReward`.
- `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData` `addExp/setExp`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2787-Completion.md`.
- `docs/Phase-6-Session-2787-Handoff.md`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

Result: Passed, 22 total, 0 failed, 0 skipped. Existing nullable/analyzer warnings remain outside this UOW.

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` socket-side XP mutation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target auto-reward branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | XP-only reportable self-target auto-reward quests are live; NPC-target dialog quest paths and mixed reward branches remain deferred. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Network.Aion.GameServerConnection.TryHandleQuestFinishAutoRewardAsync` plus quest finish services | Runtime service/socket path | Partial | Regression Tested | Partial Parity | Applies XP and completes quest state in Java order for XP-only rewards; item rewards, work items, challenge tasks, callbacks, NPC faction completion, and full nearby quest runtime fanout remain incomplete. |
| `com.aionemu.gameserver.services.QuestService.giveReward` XP branch | `Aion.GameServer.Services.QuestRewardService.ApplyXpReward` called by `GameServerConnection` | Runtime reward application | Partial | Regression Tested | Partial Parity | The live quest finish socket path now calls the XP application boundary; other non-item reward branches are still not live from quest finish. |

## Known Gaps

- XP-only quest finish works from the self-target auto-reward dialog path; mixed reward quests are still intentionally skipped.
- Live kinah/item/title/AP/DP/GP quest finish rewards remain deferred.
- Work-item removal, challenge task completion, quest-completed callbacks, NPC faction completion, and broad nearby quest refresh fanout remain partial.
- Quest persistence uses the existing update shape when `PlayerEnterWorldService` is available, but this UOW did not add a repository-backed socket test.
- No Java runtime/golden fixture exists for quest finish socket XP behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2788] Wire live quest finish kinah rewards`

- Deferred/live behavior advanced: expand quest finish live rewards from XP-only to Java `QuestService.giveReward` kinah application for self-target reportable auto-reward quests.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward` kinah branch and Java storage/inventory packet behavior for `increaseKinah(..., INC_KINAH_QUEST)`.
- C# runtime artifact to wire/fix: add a narrow live kinah branch to the same guarded quest finish path, using existing `QuestRewardService.CreateKinahRewardPlan` and inventory packet helpers to mutate the player's kinah item and emit `SmInventoryUpdateItem`/`SmInventoryAddItem` as appropriate.
- Client-visible/state/persistence effect expected: completing a kinah-only quest increases live cube kinah state and sends the Java-equivalent inventory update packet before quest completion update; persistence should use the existing inventory/quest repository shape if available.
- Why this is not preview-only/test-only/documentation-only: it will mutate live inventory state and send real inventory packets from the live quest finish socket handler.

## Safe Runtime Candidates

- Wire kinah-only quest finish rewards from the live self-target auto-reward branch.
- Wire item-only quest finish rewards using `ItemService.addItem` equivalent behavior and inventory capacity guards.
- Add repository-backed focused coverage for the already-live XP-only quest finish branch if paired with a runtime persistence fix, not as standalone evidence.

## Suggested Focused Validation

Start with the quest finish socket boundary test and reward service tests:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardServiceTests"
```

For kinah wiring, add or extend the same boundary class with a kinah-only quest and assert live inventory mutation plus `SmInventoryUpdateItem`/`SmInventoryAddItem` packet ordering before `SmQuestAction.Update`.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 4.
- Total artifacts ported or wired in latest UOW: 1 live socket branch plus existing XP reward and quest-state services.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
