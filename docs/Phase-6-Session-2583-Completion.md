# Phase 6 Session 2583 Completion

## UOW

[Phase 6] UOW-2583: Persist quest work-item inventory deletions during live abandon

## Status

Completed and validated with focused service/runtime-state coverage. The live `CM_DELETE_QUEST` abandon path already
removed Java quest work-item stacks from the in-memory cube and sent delete/cube-size packets; this UOW now persists
those deleted item rows through the existing `inventory` delete repository path and clears the successfully persisted
deleted-item markers from the player dirty queue.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.removeQuestWorkItems deletes all matching quest work-item cube stacks and Java persists those deleted rows through InventoryDAO.store(player).
- Java source method or runtime path: QuestService.abandonQuest -> QuestService.removeQuestWorkItems -> Storage.decreaseByItemId -> InventoryDAO.store(Player).
- C# runtime artifact wired or fixed: QuestAbandonService.WorkItemDeletions, PlayerEnterWorldService.PersistQuestAbandonAsync, Player.MarkDeletedInventoryItemsPersisted, PlayerEnterWorldRepository.DeleteInventoryItemAsync.
- Client-visible/state/persistence effect changed: quest work-item stacks removed during live abandon are now deleted from existing inventory database rows alongside the already-live item-delete/cube-size packet output.
- Why this is not preview-only/test-only/documentation-only: the live abandon persistence path now writes the inventory deletion side effect through the existing database shape.
```

## Java Source Reviewed

- `QuestService.abandonQuest`:
  - mutates the quest state/list;
  - aborts NPC-faction state when applicable;
  - calls `removeQuestWorkItems(player, qs)` before work-order recipe removal, timer clear, final abandon packet, and nearby quest refresh.
- `QuestService.removeQuestWorkItems`:
  - reads `QuestTemplate.getQuestWorkItems()`;
  - for each quest work item id, removes the player's full cube count through `player.getInventory().decreaseByItemId(qi.getItemId(), count, qs.getStatus())`.
- `Storage.decreaseByItemId`:
  - iterates matching item stacks and delegates count deletion/update through storage mutation.
- `InventoryDAO.store(Player)`:
  - stores `player.getDirtyItemsToUpdate()`;
  - deletes items whose persistent state is deleted through `deleteItems`.

## C# Changes

- `PlayerEnterWorldService.PersistQuestAbandonAsync` now deletes each distinct `QuestAbandonResult.WorkItemDeletions` item row through `DeleteInventoryItemAsync`.
- Added `Player.MarkDeletedInventoryItemsPersisted(IEnumerable<int>)` to clear only successfully persisted cube deleted-item markers while preserving dirty state for any remaining unpersisted delete markers or dirty inventory rows.
- Extended `PlayerEnterWorldServiceTests` with `PersistQuestAbandon_DeletesQuestWorkItemInventoryRows`.
- Extended the test repository capture to record inventory delete calls.

## Known Gaps

- This UOW persists full-stack quest work-item deletes. Partial stack count updates are not needed for the current `removeQuestWorkItems` abandon branch because Java removes the full count for each quest work item id.
- Java `QuestEngine.onItemRemoved` remains unported for these storage deletions because C# quest-handler execution is not available yet.
- Quest timer task-map cancellation remains missing; C# currently sends the timer-clear packet but has no modeled quest timer task owner.
- Full real-client abandon flow and live DB integration were not run.

## Validation Decision

```text
Validation decision:
- Changed surface: production runtime persistence, player dirty inventory state, focused service tests.
- Specific behavior/contract: live quest abandon persists quest work-item inventory row deletion and clears successfully persisted deleted-item markers while preserving existing abandon quest/faction persistence.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore -> 80/80 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live persistence behavior changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered abandon work-item deletion plus adjacent abandon persistence behavior.
- Why this scope is sufficient: the test exercises Java-shaped full-count work-item removal from `QuestAbandonService`, repository inventory-row deletion from `PersistQuestAbandonAsync`, and deleted-marker cleanup on the live player model.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` / `PlayerEnterWorldService.PersistQuestAbandonAsync` | Service | Partial | Unit Tested | Partial Parity | Quest list mutation, NPC-faction abort/reassignment, work-item deletion packets, work-order recipe deletion, and quest/faction/work-item persistence are live; task-map cancellation and quest-engine callbacks remain missing. |
| `QuestService.removeQuestWorkItems` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | C# removes all matching cube stacks for configured quest work item ids and tracks deleted items; Java `QuestEngine.onItemRemoved` callback is not executed yet. |
| `Storage.decreaseByItemId` deleted-item dirty tracking | `Player.TrackDeletedItem` / `Player.MarkDeletedInventoryItemsPersisted` | Model/state | Partial | Unit Tested | Partial Parity | Deleted cube rows are tracked and now cleared only after successful direct persistence; full Java storage semantics for all storage kinds remain partial. |
| `InventoryDAO.store(Player)` deleted item branch | `PlayerEnterWorldRepository.DeleteInventoryItemAsync` via `PlayerEnterWorldService.PersistQuestAbandonAsync` | Repository/persistence | Partial | Unit Tested | Partial Parity | The live abandon path deletes existing `inventory` rows and `item_stones` through the existing repository shape; no live MySQL integration was run for this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PersistQuestAbandon_DeletesQuestWorkItemInventoryRows` | Unit | `QuestService.removeQuestWorkItems`, `InventoryDAO.store(Player)` | Abandon work-item deletions are persisted through inventory row delete calls and deleted markers are cleared after success | Source-reviewed Java + focused C# service assertion | Repository is captured fake, not live MySQL. |

## Summary Metrics

- Focused validation: 80 tests passed.
- Runtime progress: live abandon persistence now includes quest work-item inventory row deletes.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: 2 (`QuestEngine.onItemRemoved`, quest timer task-map cancellation).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- No real-client or live DB abandon scenario was executed.
- If an immediate direct inventory delete fails, the deleted item marker remains queued for later dirty-item persistence.
- C# still lacks Java quest-handler execution for item-removal callbacks.
- Timer task cancellation requires a real C# quest timer task owner before it can become a safe runtime UOW.

## Next Runtime Candidate

UOW-2584 candidate: wire a narrow `CM_USE_ITEM` quest-start action branch if source discovery confirms the item
template loader exposes `QuestStartAction` data and the existing C# quest start/accept mutation packet path can be
reused live. If those prerequisites are missing, skip this candidate and choose a different live packet/state branch;
do not create a planner/report-only slice.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: Java quest-start item use can invoke QuestEngine.onItemUseEvent / QuestStartAction and start or offer a quest from a real item-use packet.
- Java source method or runtime path: CM_USE_ITEM.runImpl -> QuestEngine.onItemUseEvent and model/templates/item/actions/QuestStartAction.act.
- C# runtime artifact to wire or fix: ItemTemplateSummary/static item-action loading if quest-start action data exists, GameServerConnection.HandleUseItemAsync, existing quest start/SM_QUEST_ACTION state mutation and persistence surfaces.
- Client-visible/state/persistence effect expected: using a quest-start item should send/trigger the Java-equivalent quest start flow and mutate/persist player quest state when accepted, rather than silently doing nothing.
- Why this is not preview-only/test-only/documentation-only: it would wire a real client item-use packet to live quest packet/state/persistence effects.
```

Suggested focused validation recipe:

```text
- Behavior/contract: quest-start item action from CM_USE_ITEM reaches live quest start packet/state/persistence path without changing unrelated item actions.
- Focused C# command: start with dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~QuestAbandonServiceTests" --no-restore, then replace/trim to the new quest-start item-use test class once added.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of CM_USE_ITEM and QuestStartAction is expected.
- Broad-validation trigger: live packet dispatch, quest state mutation, and persistence if the UOW proceeds; start focused and document whether broader .NET validation is skipped after focused evidence.
```
