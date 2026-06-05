# Phase 6 Session 2576 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2576: Abort NPC-faction quest state during live quest abandon. See
[Phase-6-Session-2576-Completion.md](Phase-6-Session-2576-Completion.md).

## Commits Made

- `4ed4c5c6f` - `[Phase 6][UOW-2575] Remove abandon quest work items`
- Current commit - `[Phase 6][UOW-2576] Abort NPC faction quests on abandon`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 wired the next Java abandon side effect: active matching NPC-faction quest state now resets to `NOTING`
  from the live abandon path.

## Files Changed In UOW-2576

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerNpcFactionState.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `docs/Phase-6-Session-2576-Completion.md`
- `docs/Phase-6-Session-2576-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#abandonQuest`
- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#abortQuest`
- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction#setState`

## C# Artifacts Touched

- `PlayerNpcFactionsSnapshot.AbortQuest`
- `PlayerNpcFactionAbortResult`
- `QuestAbandonService.AbortNpcFactionQuest`
- `QuestAbandonResult.NpcFactionAbort`

## Validation

```text
dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~NpcFaction" --no-restore
```

Result: passed, 31/31. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live player NPC-faction state mutation was added,
but the focused filter built the affected project and directly covered the new abandon/NPC-faction behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Manual Only | Partial Parity | Live dispatch wired; timer-clear, work-item delete, NPC-faction abort, abandon packet ordering implemented; task-map cancellation still missing. |
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` | Service | Partial | Unit Tested | Partial Parity | Guards, quest delete/reset, NPC-faction abort, work-item cleanup, ABANDON/TIMER covered; recipes and persistence still missing. |
| `NpcFactions.abortQuest` | `PlayerNpcFactionsSnapshot.AbortQuest` / `QuestAbandonService.AbortNpcFactionQuest` | Service/state | Partial | Unit Tested | Partial Parity | Active exact faction resets to `Noting`; Java `sendDailyQuest()` packet path and NPC-faction persistence are not live. |
| `QuestService.removeQuestWorkItems` | `QuestAbandonService.RemoveQuestWorkItems` | Service | Partial | Unit Tested | Partial Parity | Removes all matching cube stacks and tracks deletes; Java quest-engine item-removed callback not live. |

## Known Gaps

- Java `NpcFactions.sendDailyQuest()` after abort remains missing.
- NPC-faction persistence after abandon remains missing.
- TASK/work-order recipe deletion remains missing.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Quest persistence after abandon remains missing.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Abandon changes quest, NPC-faction, and inventory state in memory, but quest/NPC-faction persistence is still not wired.
- NPC-faction daily quest assignment packets are not emitted after abort.

## Next Recommended UOW

**UOW-2577: Wire TASK/work-order recipe deletion during live quest abandon.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest TASK branch -> WorkOrdersData recipe deletion.
- Java source method or runtime path: QuestService.abandonQuest, DataManager.XML_QUESTS.getQuest, WorkOrdersData.getRecipeId, RecipeList.deleteRecipe.
- C# runtime artifact to wire or fix: work-order recipe-id projection from Java XML/static data and Player recipe-list mutation invoked from QuestAbandonService/GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning a TASK/work-order quest removes the corresponding recipe from live player recipe state and records/persists the deletion if the existing recipe shape supports it.
- Why this is not preview-only/test-only/documentation-only: it mutates live player recipe state from the live CM_DELETE_QUEST path.
```

Java artifacts to inspect:

- `QuestService.abandonQuest` TASK branch
- `com.aionemu.gameserver.questEngine.model.xmlQuest.WorkOrdersData#getRecipeId`
- `com.aionemu.gameserver.model.gameobjects.player.RecipeList#deleteRecipe`
- recipe DAO/persistence methods if C# already has matching runtime shape

C# artifacts likely involved:

- `Player` recipe-list/runtime recipe state
- existing recipe/static XML extractor shape
- `QuestAbandonService`
- `GameServerConnection.HandleDeleteQuestAsync`

Focused validation recipe:

- Behavior/contract: abandoning a TASK/work-order quest removes the matching recipe from live player state.
- Focused C# command: start with `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~Recipe" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added; inspect Java source first.
- Broad-validation trigger: live player recipe-state mutation applies; start focused and broaden only if focused evidence exposes wider risk.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, NPC-faction abort, work-item inventory cleanup, and packet output.
- `QuestAbandonService.Abandon` owns the Java branch behavior and now returns ordered timer packets, NPC-faction abort result,
  work-item deletions, and an abandon packet.
- `PlayerNpcFactionsSnapshot.AbortQuest` only resets the active matching faction state to `Noting`; it preserves active flag,
  time, mentor flag, and assigned quest id like Java `setState(NOTING)`.
- Avoid preview/metadata/evidence-only work; the next UOW should mutate the next real abandon-path state.
