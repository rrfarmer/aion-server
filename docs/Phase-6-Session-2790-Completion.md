# Phase 6 Session 2790 Completion

## Unit Of Work

`[Phase 6][UOW-2790] Wire live quest finish work-item removal`

## Runtime Progress Gate

- Deferred/live behavior advanced: live self-target reportable auto-reward quest finish now executes Java `QuestService.removeQuestWorkItems` behavior after reward grants and before quest completion.
- Java source of truth: `game-server/src/com/aionemu/gameserver/services/QuestService.java` `finishQuest` and `removeQuestWorkItems`, plus `Storage.decreaseByItemId(..., QuestStatus)`.
- C# runtime artifact wired: `GameServerConnection.TryHandleQuestFinishAutoRewardAsync` calls `QuestWorkItemRemovalService.RemoveQuestWorkItems` and sends live delete/cube packets.
- Client-visible/state/persistence effect changed: quest work-item cube stacks are removed from live `Player.InventoryItems`, tracked as deleted inventory rows, persisted through `PlayerEnterWorldService.DeleteInventoryItemAsync` when available, and fanned out as `SmDeleteItem` plus `SmCubeUpdate` before `SmQuestAction.Update`.
- Why this is not preview-only/test-only/documentation-only: the socket handler mutates live inventory state and sends real inventory packets during quest finish.

## Java Parity Notes

- Java removes all current cube stacks matching each quest work-item id by first reading the full live count, not by consuming only the XML `count`.
- During finish, Java passes the current quest status `REWARD` into `Storage.decreaseByItemId`; `ItemDeleteType.fromQuestStatus(REWARD)` resolves to the default delete mask `0`.
- Existing abandon behavior was refactored to reuse the same runtime removal service without changing its expected START/COMPLETE delete mask behavior.

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestWorkItemRemovalService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionQuestFinishDialogBoundaryTests.cs`

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

Result: Failed, 315 passed and 5 failed. The failures were in pre-existing broad `GameServerConnectionInventoryExpansionUseItemTests` cases outside this UOW's quest finish and work-item code paths.

Java/Maven validation: not run; no narrow Java fixture exists for `CM_DIALOG_SELECT` plus `QuestService.finishQuest` live work-item packet fanout.

## Parity Status

Partial parity improved for the self-target/reportable auto-reward quest finish branch. XP, kinah, fixed item rewards, and quest work-item removal now run live in Java order for the guarded branch. Selectable rewards, title/AP/DP/GP/cube/warehouse rewards, challenge tasks, quest completion callbacks, NPC faction completion, NPC-target dialog paths, and broad nearby quest fanout remain incomplete.
