# Phase 6 Session 2791 Completion

## Unit Of Work

`[Phase 6][UOW-2791] Wire live quest finish title rewards`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.giveReward` title acquisition for supported title rewards.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `giveReward`, plus `model/gameobjects/player/title/TitleList.addTitle(titleId, true, 0)`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` now allows and applies `QuestFinishRewardNonItemAction.Title` descriptors from the quest finish reward projection.
- Client-visible/state/persistence effect changed: supported quest finish title rewards mutate live `Player.Titles`, attempt title persistence through `PlayerEnterWorldService.SaveTitleAddActionMutationAsync` when available, register title expiration bookkeeping, send `SmSystemMessage.QuestGetRewardTitle`, send `SmTitleInfo`, and then send `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live player title state and sends real title reward packets during quest finish.

## Java Parity Notes

- Java `QuestService.giveReward` calls `player.getTitleList().addTitle(rewards.getTitle(), true, 0)` when a quest reward title id is present.
- Java `TitleList.addTitle` rejects race-mismatched titles silently, sends duplicate-title tooltip message for already-known titles, stores newly acquired titles, registers expirable title state, sends the quest title reward system message, and sends `SM_TITLE_INFO`.
- C# mirrors the live finish behavior for applied, duplicate, and race-mismatched title outcomes. Missing title templates abort the guarded live reward branch instead of claiming parity with Java's thrown `IllegalArgumentException`.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

## Tests Run

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionQuestFinishDialogBoundaryTests|FullyQualifiedName~QuestRewardSideEffectPlanServiceTests"
```

Result: Passed, 13 total, 0 failed, 0 skipped.

Java/Maven validation: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest/giveReward` live title packet fanout.

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, work-item removal, and title rewards now run live in Java order for the guarded branch. Selectable rewards, AP/DP/GP/cube/warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, and broad nearby quest fanout remain incomplete.
