# Phase 6 Session 2790 Handoff

## Current Phase

Phase 6: Port Game Core

## Latest Completed UOW

`[Phase 6][UOW-2790] Wire live quest finish work-item removal`

Commit made in this session:

- `[Phase 6][UOW-2790] Wire live quest finish work-item removal`

## Current State

- `GameServerConnection.HandleDialogSelectAsync` handles the Java self-target/reportable quest auto-reward branch for supported XP, kinah, fixed item rewards, and work-item removal.
- Work-item removal now happens after reward grants and before `QuestFinishStateMutationService.ApplyRewardCompletion`, matching `QuestService.finishQuest`.
- `QuestWorkItemRemovalService.RemoveQuestWorkItems` centralizes Java `QuestService.removeQuestWorkItems` behavior for finish and abandon paths.
- Live quest finish removes all current cube stacks matching each quest work-item id, tracks deleted inventory rows, attempts persistence through `PlayerEnterWorldService.DeleteInventoryItemAsync` when available, and sends `SmDeleteItem` plus `SmCubeUpdate` before `SmQuestAction.Update`.
- The finish-path delete mask for `REWARD` status is Java default `0`, matching `ItemPacketService.ItemDeleteType.fromQuestStatus`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` self-target `SELECTED_QUEST_AUTO_REWARD*` branch.
- `com.aionemu.gameserver.services.QuestService.finishQuest`.
- `com.aionemu.gameserver.services.QuestService.removeQuestWorkItems`.
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseByItemId`.
- `com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType.fromQuestStatus`.

## C# Artifacts Touched

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestWorkItemRemovalService.cs`.
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`.
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`.
- `docs/Phase-6-Session-2790-Completion.md`.
- `docs/Phase-6-Session-2790-Handoff.md`.

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests"
```

Result: Passed, 4 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests"
```

Result: Passed, 27 total, 0 failed, 0 skipped.

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestWorkItem|FullyQualifiedName~Inventory"
```

Result: Failed, 315 passed and 5 failed. Failures were in `GameServerConnectionInventoryExpansionUseItemTests`:

- `ProcessPacketAsync_CompositeStonesWritesCleanupSealFlagsForRemainingConsumedInputs`
- `HandleUseItemAsync_ApExtractHonorsConfiguredAbyssPointCap`
- `HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava`
- `HandleUseItemAsync_ApExtractSendsAbyssPointsPlannerPackets`
- `HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava`

Java/Maven: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` socket-side work-item mutation.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DIALOG_SELECT` self-target auto-reward branch | `GameServerConnection.HandleDialogSelectAsync` | Socket handler | Partial | Regression Tested | Partial Parity | Supported XP, kinah, fixed item rewards, and work-item removal are live for guarded self-target reportable auto-reward quests. |
| `QuestService.finishQuest/removeQuestWorkItems` | `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` and `QuestWorkItemRemovalService` | Runtime quest/inventory mutation | Partial | Regression Tested | Partial Parity | Work items are removed in Java order before completion update; challenge tasks, callbacks, NPC faction completion, and remaining reward kinds are still incomplete. |
| `Storage.decreaseByItemId(..., QuestStatus)` | `QuestWorkItemRemovalService.RemoveQuestWorkItems` | Runtime inventory mutation | Partial | Regression Tested | Partial Parity | Removes all matching cube stacks and maps START/COMPLETE/other quest statuses to Java delete masks. |

## Known Gaps

- Live quest finish supports only XP, kinah, fixed item rewards, and work-item deletion descriptors.
- Selectable item rewards, class-selectable rewards, bonus rewards, title rewards, AP/DP/GP rewards, cube/warehouse expansions, challenge task completion, quest-completed callbacks, NPC faction completion, NPC-target dialog quest paths, and broad nearby quest refresh fanout remain deferred.
- Work-item deletion persistence is attempted when `PlayerEnterWorldService` is available; this UOW did not add a repository-backed socket persistence test.
- No Java runtime/golden fixture exists for quest finish socket work-item behavior.

## Runtime Progress Gate For Next UOW

Recommended next UOW: `[Phase 6][UOW-2791] Wire live quest finish title rewards`

- Deferred/live behavior advanced: expand quest finish live execution beyond inventory/currency/XP to Java `QuestService.giveReward` title acquisition.
- Java source of truth: `game-server/src/com/aionemu/gameserver/model/templates/rewards/QuestRewards.java` title reward data and `QuestService.giveReward` handling of title rewards.
- C# runtime artifact to wire/fix: same guarded quest finish path, using existing player title state and packet helpers if present.
- Client-visible/state/persistence effect expected: completing a supported auto-reward quest grants a live title, persists or tracks title state through the existing player data shape when available, and sends Java-equivalent title acquisition packets/messages before `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: it would mutate live player title state and send real reward packets from the socket handler.

## Safe Runtime Candidates

- Wire title reward acquisition from live quest finish if existing title runtime state and packet helpers are ready.
- Wire selectable item rewards if `SELECTED_QUEST_AUTO_REWARD1..15` can be mapped to Java reward selection semantics without partial reward drops.
- Wire challenge task completion from quest finish if `ChallengeTaskService` live state and persistence are ready.

## Summary Metrics

- Total Java artifacts touched/discovered in latest UOW: 5.
- Total artifacts ported or wired in latest UOW: 1 live socket branch extension plus 1 shared work-item removal service.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification/partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged conservatively; Phase 6 remains in progress.
