# Phase 6 Session 2575 Completion

## UOW

[Phase 6] UOW-2575: Remove quest work items during live quest abandon

## Status

Completed and validated with the focused abandon/extractor test filter. The live `CM_DELETE_QUEST` path now projects
`quest_work_items` from Java quest XML, removes all matching cube inventory stacks when an abandon succeeds, tracks the
deleted items for existing dirty-item persistence, sends `SM_DELETE_ITEM` with the Java quest delete type, sends cube
size updates in per-stack Java order, and then sends the ABANDON quest packet.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.removeQuestWorkItems side effect during live quest abandon.
- Java source method or runtime path: QuestService.abandonQuest -> removeQuestWorkItems -> Storage.decreaseByItemId.
- C# runtime artifact to wire or fix: NearbyQuestTemplateXmlExtractor quest_work_items projection, QuestAbandonService inventory mutation, GameServerConnection delete/cube packet fanout.
- Client-visible/state/persistence effect expected: abandoning a quest removes matching quest work-item stacks from cube inventory, marks deleted items dirty, and sends item-delete/cube-size packets.
- Why this is not preview-only/test-only/documentation-only: this mutates live Player.InventoryItems from the live CM_DELETE_QUEST path.
```

## Java Source Reviewed

- `QuestService.removeQuestWorkItems(Player, QuestState)`:
  - get `QuestTemplate.getQuestWorkItems()`;
  - for each `quest_work_item`, read `item_id`;
  - get full inventory count by item id;
  - if count > 0, call `player.getInventory().decreaseByItemId(itemId, count, qs.getStatus())`.
- `Storage.decreaseByItemId(itemId, count, questStatus, actor)`:
  - walks matching storage stacks in storage order;
  - calls `decreaseItemCount(item, count, DEC_ITEM_USE, questStatus, actor)` until count reaches zero.
- `Storage.decreaseItemCount`:
  - uses `ItemDeleteType.fromQuestStatus(questStatus)` for deletes when quest status is supplied;
  - START -> `QUEST_START` (0x34), COMPLETE -> `QUEST_COMPLETE` (0x31), otherwise DEFAULT (0);
  - sends item delete/update packets and cube-size updates at the storage packet boundary.

## C# Changes

- `NearbyQuestTemplateSummary` now carries `QuestWorkItems`.
- `NearbyQuestTemplateXmlExtractor` reads `<quest_work_items>/<quest_work_item item_id=... count=...>`.
- `QuestAbandonService` removes all matching non-equipped cube stacks for each quest work-item item id, tracks deleted
  items with `Player.TrackDeletedItem`, and records per-deletion cube counts.
- `GameServerConnection.HandleDeleteQuestAsync` sends work-item `SmDeleteItem` and `SmCubeUpdate` packets before
  `SM_QUEST_ACTION.ABANDON`, preserving Java packet ordering.
- `SmDeleteItem` now exposes Java quest delete type constants.

## Known Gaps

- Java's `QuestEngine.onItemRemoved` callback from storage delete is not live.
- NPC-faction abort, TASK/work-order recipe deletion, timer task-map cancellation, and quest persistence remain gaps.
- Handler packet fanout is not directly socket-tested; service mutation and packet types/counts are focused-tested.

## Validation Decision

```text
Validation decision:
- Changed surface: production-code live inventory state mutation, packet fanout, static XML projection, focused tests.
- Specific behavior/contract: removeQuestWorkItems removes all matching cube stacks, uses quest-status delete types, records per-stack cube counts, and extracts quest_work_item rows.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests" --no-restore -> 14/14 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live inventory mutation applies.
- Broad .NET decision: skipped after focused pass; filtered test built Aion.GameServer and directly covered the changed live service/extractor behavior. Shared inventory persistence primitives were reused, not changed.
- Why this scope is sufficient: tests cover deletion of multiple matching stacks, exclusion of unrelated/equipped items, quest START and COMPLETE delete types, and XML projection of quest_work_items.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.removeQuestWorkItems` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | Removes all matching cube stacks and tracks deletes; Java `QuestEngine.onItemRemoved` callback not live. |
| `Storage.decreaseByItemId(..., QuestStatus)` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | Full-count delete behavior and quest delete types covered; generic partial decrease/update path not ported here. |
| `QuestWorkItems` / `QuestItems` XML | `NearbyQuestTemplateSummary.QuestWorkItems` | DTO/static data | Complete | Unit Tested | Verified Parity | XML projection of quest_work_item item id/count covered. |
| `ItemPacketService.ItemDeleteType` quest values | `SmDeleteItem.QuestStartDeleteType` / `QuestCompleteDeleteType` | Packet constants | Complete | Unit Tested | Verified Parity | START=0x34 and COMPLETE=0x31 covered by service tests. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Abandon_RemovesAllMatchingQuestWorkItemStacksFromCube` | Unit | Java source review | Deletes all matching cube stacks, leaves unrelated/equipped items, tracks dirty deletes | Java source + live C# state assertions | No socket packet capture. |
| `Abandon_PreviouslyCompletedQuestWorkItemsUseQuestCompleteDeleteType` | Unit | Java source review | Reset-to-COMPLETE branch uses QUEST_COMPLETE delete type | Java source + constants | No generic Storage decrease port. |
| `NearbyQuestTemplateXmlExtractorTests` updates | Unit | Java JAXB templates | `quest_work_items` row projection | XML fixture assertions | Real-data only counts templates with work items, not row total. |

## Summary Metrics

- Focused validation: 14 tests passed.
- Live abandon path now mutates both quest state and quest work-item inventory state.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Quest persistence remains incomplete; deleted quest/work-item state depends on existing dirty item persistence only.
- NPC-faction and recipe side effects are still absent.
- Full real-client abandon flow remains unvalidated.

## Next Recommended UOW

UOW-2576: Wire the next live abandon-path side effect: NPC-faction abort for templates with `npcfaction_id`.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getNpcFactions().abortQuest(template).
- Java source method or runtime path: QuestService.abandonQuest and NpcFactions.abortQuest.
- C# runtime artifact to wire or fix: PlayerNpcFactionsSnapshot/runtime mutation service invoked from QuestAbandonService or GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning an NPC-faction quest clears or deactivates the player's matching faction quest state, with persistence status documented or wired if existing shape supports it.
- Why this is not preview-only/test-only/documentation-only: it mutates live player NPC-faction state from the live CM_DELETE_QUEST path.
```

Focused validation recipe:

- Behavior/contract: abandoning a quest with `NpcFactionId != 0` aborts the matching active faction state like Java.
- Focused C# command: `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added; inspect `NpcFactions.abortQuest` first.
- Broad-validation trigger: live player NPC-faction state mutation applies; start focused and broaden only if focused evidence exposes wider risk.
