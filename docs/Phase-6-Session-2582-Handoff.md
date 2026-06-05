# Phase 6 Session 2582 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2582: Filter NPC-faction daily selection by handler availability. See
[Phase-6-Session-2582-Completion.md](Phase-6-Session-2582-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- Current commit - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`

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

## Files Changed In UOW-2582

- `dotnetConversion/src/Aion.GameServer/Dataholders/DataManager.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/LoadingUtils/XmlDataLoader.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestHandlerAvailabilityTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestHandlerAvailabilityTableTests.cs`
- `docs/Phase-6-Session-2582-Completion.md`
- `docs/Phase-6-Session-2582-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.handlers.QuestHandlerLoader#postLoad`
- `com.aionemu.gameserver.questEngine.handlers.QuestHandlerLoader#isValidClass`
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler#AbstractQuestHandler(int)`
- `com.aionemu.gameserver.questEngine.QuestEngine#init`
- `com.aionemu.gameserver.questEngine.QuestEngine#addQuestHandler`
- `com.aionemu.gameserver.questEngine.QuestEngine#isHaveHandler`
- `com.aionemu.gameserver.dataholders.XMLQuests#afterUnmarshal`
- `com.aionemu.gameserver.questEngine.handlers.models.XMLQuest#register`
- `com.aionemu.gameserver.dataholders.QuestsData#getQuestsByNpcFaction`

## C# Artifacts Touched

- `QuestHandlerAvailabilityTable`
- `XmlDataLoaderOptions.QuestHandlerDirectory`
- `DataManager.LoadAsync`
- `StaticData.QuestHandlers`
- `GameServerConnection.HandleDeleteQuestAsync`
- `QuestAbandonServiceTests`
- `QuestHandlerAvailabilityTableTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~QuestHandlerAvailabilityTableTests|FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests|FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests|FullyQualifiedName~NearbyQuestTemplateTableTests" --no-restore
```

Result: passed, 34/34. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because runtime static-data loading and live
packet/state/persistence selection behavior changed, but the focused filter built Aion.GameServer and directly covered
handler availability loading plus abandon selection filtering.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit/Manual | Partial Parity | Live dispatch wired; timer-clear, reusable/random NPC-faction daily packet with handler availability filter, work-item delete, NPC-faction abort, work-order recipe delete, quest/faction persistence, and abandon packet ordering implemented; task-map cancellation still missing. |
| `QuestHandlerLoader.postLoad` / `QuestEngine.addQuestHandler` | `QuestHandlerAvailabilityTable.TryReadJavaHandlerQuestId` / `Load` | Runtime loading | Partial | Unit Tested | Partial Parity | Public concrete Java handler ids are loaded from source into runtime availability data; dynamic instantiation/execution is not ported. |
| `XMLQuests.afterUnmarshal` / `XMLQuest.register` | `QuestHandlerAvailabilityTable.Load` XML quest-script scan | Runtime loading | Partial | Unit Tested | Partial Parity | XML quest script ids are included in the runtime availability table; full XML quest handler execution remains unported. |
| `QuestEngine.isHaveHandler` | `QuestHandlerAvailabilityTable.IsHaveHandler` | Service/table | Partial | Unit Tested | Partial Parity | Live selector consumes this predicate; count parity against Java runtime is not yet verified. |
| `QuestsData.getQuestsByNpcFaction` handler filter | `GameServerConnection.HandleDeleteQuestAsync` / `QuestAbandonService.Abandon` | Live handler/selection | Partial | Unit Tested | Partial Parity | Random NPC-faction daily selection now filters through runtime handler availability before packet/state/persistence effects. |

## Known Gaps

- `QuestHandlerAvailabilityTable` is a source/static-data availability table, not dynamic C# quest handler execution.
- Full Java/C# loaded handler count parity was not verified.
- Java handlers with unsupported constructor expressions fail closed in the C# source scanner.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Random NPC-faction daily selection may under-select if a Java handler source uses a quest id pattern not yet recognized by the scanner.
- No Java runtime handler count comparison was performed; parity remains partial.

## Next Recommended Runtime UOW

**UOW-2583: Persist quest work-item inventory deletions during live abandon.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.removeQuestWorkItems deletes all matching quest work-item cube stacks and Java later persists inventory storage changes.
- Java source method or runtime path: QuestService.abandonQuest -> removeQuestWorkItems -> Storage.decreaseByItemId/delete item row persistence through InventoryDAO.store.
- C# runtime artifact to wire or fix: GameServerConnection.HandleDeleteQuestAsync work-item deletion loop, PlayerEnterWorldService.DeleteInventoryItemAsync, existing inventory repository delete path.
- Client-visible/state/persistence effect expected: quest work-item stacks removed during live abandon will be deleted from the existing inventory database rows immediately alongside the current item-delete/cube-size packets.
- Why this is not preview-only/test-only/documentation-only: it persists a live inventory state mutation through the existing database shape from the live CM_DELETE_QUEST path.
```

Java artifacts to inspect:

- `QuestService.abandonQuest`
- `QuestService.removeQuestWorkItems`
- `Storage.decreaseByItemId`
- `InventoryDAO.store` delete behavior if needed

C# artifacts likely involved:

- `QuestAbandonService.RemoveQuestWorkItems`
- `QuestAbandonResult.WorkItemDeletions`
- `GameServerConnection.HandleDeleteQuestAsync`
- `PlayerEnterWorldService.DeleteInventoryItemAsync`
- `PlayerEnterWorldRepository.DeleteInventoryItemAsync`

Focused validation recipe:

- Behavior/contract: live abandon persists quest work-item inventory row deletion while preserving existing item-delete/cube-size packet output and final abandon ordering.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GameServerConnectionQuest" --no-restore`; narrow further to edited tests if slow or if no direct `GameServerConnectionQuest` coverage exists.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of the Java methods listed above is expected.
- Broad-validation trigger: live persistence and handler dispatch behavior changes; start focused and document whether broader .NET validation is skipped after focused evidence.

## Safe Runtime Candidates

- Persist quest work-item deletion from live abandon as above.
- Timer task cancellation can become a runtime UOW only after discovery identifies or implements a real quest timer scheduler/task-owner path.
- Wire `QuestEngine.onItemRemoved` callback effects only after a concrete C# quest-handler execution surface exists.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, timer-clear packet, reusable/random NPC-faction daily packet, NPC-faction abort, work-item inventory cleanup, work-order recipe delete, quest/faction persistence, and final abandon packet.
- `SmQuestAction.Unknown` is Java `SM_QUEST_ACTION(int questId)`, not the normal quest-state ADD packet.
- Random daily replacement now mutates `Player.NpcFactions`, sends action id 6, persists final faction state, and filters through `StaticData.QuestHandlers.IsHaveHandler`.
- `QuestHandlerAvailabilityTable` is loaded with static data from merged quest scripts plus Java handler source ids. It is not dynamic handler execution.
- Avoid preview/metadata/evidence-only work; the next UOW should persist the next live abandon-path effect or choose another live runtime branch.
