# Phase 6 Session 2583 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2583: Persist quest work-item inventory deletions during live abandon. See
[Phase-6-Session-2583-Completion.md](Phase-6-Session-2583-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- Current commit - `[Phase 6][UOW-2583] Persist quest work item deletes`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.
- UOW-2578 persisted the live quest abandon mutation to `player_quests` delete/update rows.
- UOW-2579 persisted the live NPC-faction abort mutation to `player_npc_factions`.
- UOW-2580 sent Java `SM_QUEST_ACTION(int questId)` for the reusable assigned NPC-faction daily quest branch after abort.
- UOW-2581 supported Java's random NPC-faction daily replacement branch after abort, including assignment state mutation, packet send, and persistence.
- UOW-2582 loaded quest-handler availability into runtime static data and wired it into the live random NPC-faction daily selector.
- UOW-2583 persisted live quest work-item inventory deletions through the existing inventory repository delete path.

## Files Changed In UOW-2583

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2583-Completion.md`
- `docs/Phase-6-Session-2583-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#abandonQuest`
- `com.aionemu.gameserver.services.QuestService#removeQuestWorkItems`
- `com.aionemu.gameserver.model.items.storage.Storage#decreaseByItemId`
- `com.aionemu.gameserver.dao.InventoryDAO#store(Player)`

## C# Artifacts Touched

- `Aion.GameServer.Model.GameObjects.Player.MarkDeletedInventoryItemsPersisted`
- `Aion.GameServer.Services.PlayerEnterWorldService.PersistQuestAbandonAsync`
- `Aion.GameServer.Services.QuestAbandonService` result consumption
- `Aion.GameServer.Data.PlayerEnterWorldRepository.DeleteInventoryItemAsync` existing path
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 80/80. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live persistence behavior changed, but the
focused filter built Aion.GameServer and directly covered abandon work-item deletion plus adjacent quest/faction
abandon persistence behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit/Manual | Partial Parity | Live dispatch wired; timer-clear, reusable/random NPC-faction daily packet with handler availability filter, work-item delete/persistence, NPC-faction abort, work-order recipe delete, quest/faction persistence, and abandon packet ordering implemented; task-map cancellation still missing. |
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` / `PlayerEnterWorldService.PersistQuestAbandonAsync` | Service | Partial | Unit Tested | Partial Parity | Quest list mutation, NPC-faction abort/reassignment, work-item deletion packets, work-order recipe deletion, and quest/faction/work-item persistence are live; timer task-map cancellation and quest-engine callbacks remain missing. |
| `QuestService.removeQuestWorkItems` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | C# removes all matching cube stacks for configured quest work item ids and tracks deleted items; Java `QuestEngine.onItemRemoved` callback is not executed yet. |
| `Storage.decreaseByItemId` deleted-item dirty tracking | `Player.TrackDeletedItem` / `Player.MarkDeletedInventoryItemsPersisted` | Model/state | Partial | Unit Tested | Partial Parity | Deleted cube rows are tracked and cleared only after successful direct persistence; full Java storage semantics for all storage kinds remain partial. |
| `InventoryDAO.store(Player)` deleted item branch | `PlayerEnterWorldRepository.DeleteInventoryItemAsync` via `PlayerEnterWorldService.PersistQuestAbandonAsync` | Repository/persistence | Partial | Unit Tested | Partial Parity | The live abandon path deletes existing `inventory` rows and `item_stones` through the existing repository shape; no live MySQL integration was run for this UOW. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PersistQuestAbandon_DeletesQuestWorkItemInventoryRows` | Unit | `QuestService.removeQuestWorkItems`, `InventoryDAO.store(Player)` | Abandon work-item deletions are persisted through inventory row delete calls and deleted markers are cleared after success | Source-reviewed Java + focused C# service assertion | Repository is captured fake, not live MySQL. |

## Known Gaps

- Quest timer task-map cancellation remains missing. Discovery found no real C# quest timer task owner yet; do not implement this as a report-only UOW.
- Java `QuestEngine.onItemRemoved` from work-item deletion remains missing because C# quest-handler execution is not available yet.
- Full real-client abandon flow and live MySQL integration were not run.
- The direct inventory delete path clears only successfully persisted work-item delete markers; failed deletes remain queued for later dirty persistence.

## Remaining Risks

- `Player.MarkDeletedInventoryItemsPersisted` is currently cube-deletion focused. It should not be reused for warehouse/account warehouse deletes without extending the storage-specific behavior.
- Live DB delete failure during abandon is still only reflected by the persistence return value; `GameServerConnection.HandleDeleteQuestAsync` currently ignores that return and continues packet output.
- Real Java storage deletion also notifies quest handlers; that callback remains blocked until a concrete C# quest-engine handler execution surface exists.

## Next Recommended Runtime UOW

**UOW-2584 candidate: wire a narrow `CM_USE_ITEM` quest-start action branch, only if discovery confirms live prerequisites.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: Java quest-start item use can invoke QuestEngine.onItemUseEvent / QuestStartAction and start or offer a quest from a real item-use packet.
- Java source method or runtime path: CM_USE_ITEM.runImpl -> QuestEngine.onItemUseEvent and model/templates/item/actions/QuestStartAction.act.
- C# runtime artifact to wire or fix: ItemTemplateSummary/static item-action loading if quest-start action data exists, GameServerConnection.HandleUseItemAsync, existing quest start/SM_QUEST_ACTION state mutation and persistence surfaces.
- Client-visible/state/persistence effect expected: using a quest-start item should send/trigger the Java-equivalent quest start flow and mutate/persist player quest state when accepted, rather than silently doing nothing.
- Why this is not preview-only/test-only/documentation-only: it would wire a real client item-use packet to live quest packet/state/persistence effects.
```

Java artifacts to inspect:

- `CM_USE_ITEM.runImpl`
- `QuestEngine.onItemUseEvent`
- `QuestStartAction.canAct`
- `QuestStartAction.act`
- Any Java quest start/accept path reached by `DialogAction.ASK_QUEST_ACCEPT`

C# artifacts likely involved:

- `ItemTemplateSummary` and static item XML extraction
- `GameServerConnection.HandleUseItemAsync`
- Existing quest start/accept service or packet/state mutation surface, if present
- `SmQuestAction` and quest persistence helpers

Focused validation recipe:

```text
- Behavior/contract: quest-start item action from CM_USE_ITEM reaches live quest start packet/state/persistence path without changing unrelated item actions.
- Focused C# command: start with dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~GamePacketTests" --no-restore; after adding a dedicated quest-start use-item test, narrow the filter to that new class plus any directly reused quest state/persistence class.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of CM_USE_ITEM and QuestStartAction is expected.
- Broad-validation trigger: live packet dispatch, quest state mutation, and persistence if the UOW proceeds; start focused and document whether broader .NET validation is skipped after focused evidence.
```

If discovery shows item quest-start metadata or quest start acceptance is absent, do not continue with scaffolding. Choose
another live packet/state/persistence branch from current `GameServerConnection` deferred handlers.

## Safe Runtime Candidates

- `CM_USE_ITEM` quest-start action, after prerequisite discovery above.
- Quest timer cancellation only after a real C# quest timer task owner exists.
- `QuestEngine.onItemRemoved` callback only after a concrete C# quest-handler execution surface exists.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, timer-clear packet, reusable/random NPC-faction daily packet, NPC-faction abort, work-item inventory cleanup and persistence, work-order recipe delete, quest/faction persistence, and final abandon packet.
- `SmQuestAction.Unknown` is Java `SM_QUEST_ACTION(int questId)`, not the normal quest-state ADD packet.
- Random daily replacement mutates `Player.NpcFactions`, sends action id 6, persists final faction state, and filters through `StaticData.QuestHandlers.IsHaveHandler`.
- Quest work-item delete rows are now directly persisted in `PersistQuestAbandonAsync`; successfully persisted delete markers are removed from `Player.DeletedInventoryItems`.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
