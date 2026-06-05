# Phase 6 Session 2577 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2577: Delete work-order recipes during live quest abandon. See
[Phase-6-Session-2577-Completion.md](Phase-6-Session-2577-Completion.md).

## Commits Made

- `b39d8f9` - `[Phase 6][UOW-2576] Abort NPC faction quests on abandon`
- Current commit - `[Phase 6][UOW-2577] Delete work order recipes on abandon`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.

## Files Changed In UOW-2577

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/WorkOrderRecipeTable.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorkOrderRecipeTableTests.cs`
- `docs/Phase-6-Session-2577-Completion.md`
- `docs/Phase-6-Session-2577-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#abandonQuest`
- `com.aionemu.gameserver.questEngine.handlers.models.WorkOrdersData#getRecipeId`
- `com.aionemu.gameserver.dataholders.XMLQuests#getQuest`
- `com.aionemu.gameserver.model.gameobjects.player.RecipeList#deleteRecipe`
- `com.aionemu.gameserver.dao.PlayerRecipesDAO#delRecipe`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_RECIPE_DELETE`

## C# Artifacts Touched

- `WorkOrderRecipeTable`
- `StaticData.WorkOrderRecipes`
- `NearbyQuestTemplateSummary.WorkOrderRecipeId`
- `QuestAbandonResult.WorkOrderRecipeId`
- `GameServerConnection.HandleDeleteQuestAsync`
- `PlayerEnterWorldService.DeleteRecipeAsync` (reused)
- `SmRecipeDelete` (reused)

## Validation

```text
dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~WorkOrderRecipeTableTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~Recipe" --no-restore
```

Result: passed, 55/55. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live player recipe state and StaticData loading
changed, but the focused filter built Aion.GameServer and directly covered loader/abandon/recipe packet and state
contracts.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit/Manual | Partial Parity | Live dispatch wired; timer-clear, work-item delete, NPC-faction abort, work-order recipe delete, abandon packet ordering implemented; task-map cancellation still missing. |
| `QuestService.abandonQuest` | `QuestAbandonService.Abandon` | Service | Partial | Unit Tested | Partial Parity | Guards, quest delete/reset, NPC-faction abort, work-item cleanup, work-order recipe candidate, ABANDON/TIMER covered; quest persistence still missing. |
| `WorkOrdersData.recipe_id` | `WorkOrderRecipeTable` / `NearbyQuestTemplateSummary.WorkOrderRecipeId` | Static data/runtime loading | Partial | Unit Tested; Regression Tested | Partial Parity | Loads real Java work_order.xml and joins to runtime quest summaries; full XMLQuests handler registry remains unported. |
| `RecipeList.deleteRecipe` | `PlayerEnterWorldService.DeleteRecipeAsync` / `GameServerConnection` / `SmRecipeDelete` | State, persistence, packet | Partial | Unit Tested | Partial Parity | Existing DB delete/in-memory mutation and packet are reused by abandon; full socket capture not added. |
| `NpcFactions.abortQuest` | `PlayerNpcFactionsSnapshot.AbortQuest` / `QuestAbandonService.AbortNpcFactionQuest` | Service/state | Partial | Unit Tested | Partial Parity | Active exact faction resets to `Noting`; Java `sendDailyQuest()` packet path and NPC-faction persistence are not live. |

## Known Gaps

- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Quest persistence after abandon remains missing.
- Java `NpcFactions.sendDailyQuest()` after abort remains missing.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Full Java `XMLQuests` handler registry is not ported; only work-order recipe id loading was added here.
- Work-order recipe delete socket ordering is covered by code review and packet serialization tests, not a full `CM_DELETE_QUEST` socket capture.

## Next Recommended UOW

**UOW-2578: Cancel live quest timer tasks during quest abandon.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getController().hasTask(TaskId.QUEST_TIMER) -> questTimerEnd/cancelTask.
- Java source method or runtime path: QuestService.abandonQuest, QuestService.questTimerEnd, PlayerController.cancelTask(TaskId.QUEST_TIMER).
- C# runtime artifact to wire or fix: player/controller scheduled task state for quest timers, or the existing live timer scheduler if present, invoked from QuestAbandonService/GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning a timed quest clears the live quest timer task so it cannot fire later, in addition to the already live timer-clear packet.
- Why this is not preview-only/test-only/documentation-only: it mutates live scheduler/controller task state from the live CM_DELETE_QUEST path.
```

Java artifacts to inspect:

- `QuestService.abandonQuest` timer branch
- `QuestService.questTimerEnd`
- `PlayerController.cancelTask`
- `TaskId.QUEST_TIMER`

C# artifacts likely involved:

- Player/controller task or scheduler state, if present
- Quest timer start path, if already ported
- `QuestAbandonService`
- `GameServerConnection.HandleDeleteQuestAsync`

Focused validation recipe:

- Behavior/contract: abandoning a timed quest cancels the live timer task and sends no later timer-end effect.
- Focused C# command: start with `dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~Timer|FullyQualifiedName~Quest" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added; inspect Java source first.
- Broad-validation trigger: live scheduler/controller task mutation applies; start focused and broaden only if focused evidence exposes wider risk.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, NPC-faction abort, work-item inventory cleanup, work-order recipe delete, and packet output.
- `WorkOrderRecipeTable` loads only `quest_script_data/work_order.xml`; it does not port the full Java `XMLQuests` registry.
- `QuestAbandonService.Abandon` returns `WorkOrderRecipeId` for TASK work-order summaries. `GameServerConnection` performs the live delete/send because Java deletes through DAO before mutating the recipe set.
- Avoid preview/metadata/evidence-only work; the next UOW should mutate the next real abandon-path state.
