# Phase 6 Session 2577 Completion

## UOW

[Phase 6] UOW-2577: Delete work-order recipes during live quest abandon

## Status

Completed and validated with the focused abandon/work-order/recipe/extractor test filter. The live `CM_DELETE_QUEST`
handler now receives Java `WorkOrdersData.recipe_id` through C# static data, deletes the matching known recipe using the
existing recipe delete path, and sends `SM_RECIPE_DELETE` before the abandon packet when the delete succeeds.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.abandonQuest TASK branch -> player.getRecipeList().deleteRecipe(...).
- Java source method or runtime path: QuestService.abandonQuest, DataManager.XML_QUESTS.getQuest, WorkOrdersData.getRecipeId, RecipeList.deleteRecipe.
- C# runtime artifact wired or fixed: WorkOrderRecipeTable, NearbyQuestTemplateSummary.WorkOrderRecipeId, QuestAbandonService.WorkOrderRecipeId result, GameServerConnection recipe delete/send branch.
- Client-visible/state/persistence effect changed: abandoning a TASK work-order quest now removes the corresponding live player recipe through PlayerEnterWorldService.DeleteRecipeAsync or in-memory fallback, then sends SM_RECIPE_DELETE.
- Why this is not preview-only/test-only/documentation-only: the live CM_DELETE_QUEST handler now mutates Player.Recipes and can persist the deletion using the existing player_recipes table path.
```

## Java Source Reviewed

- `QuestService.abandonQuest(Player, int)`:
  - after quest mutation, NPC-faction abort, and work-item deletion;
  - checks `template.getCategory() == QuestCategory.TASK`;
  - obtains `DataManager.XML_QUESTS.getQuest(questId)`;
  - if the XML quest is `WorkOrdersData`, calls `player.getRecipeList().deleteRecipe(player, recipeId)`.
- `WorkOrdersData#getRecipeId()`:
  - reads required `recipe_id` from `static_data/quest_script_data/work_order.xml`.
- `RecipeList.deleteRecipe(Player, int)`:
  - requires the recipe to be known;
  - deletes from `PlayerRecipesDAO`;
  - removes from the in-memory set;
  - sends `SM_RECIPE_DELETE`.

## C# Changes

- Added `WorkOrderRecipeTable` to load Java `work_order.xml` quest id to recipe id mappings.
- `StaticData` now loads `WorkOrderRecipeTable` from imported files and joins recipe ids into `NearbyQuestTemplateSummary`.
- `NearbyQuestTemplateSummary` now carries `WorkOrderRecipeId`.
- `QuestAbandonService.Abandon` now returns a work-order recipe delete candidate only for `TASK` summaries with a work-order recipe id.
- `GameServerConnection.HandleDeleteQuestAsync` now deletes the recipe through `PlayerEnterWorldService.DeleteRecipeAsync` when available, or the existing in-memory fallback, and sends `SmRecipeDelete` on success.

## Known Gaps

- Quest persistence after abandon remains missing.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `NpcFactions.sendDailyQuest()` after NPC-faction abort remains missing.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production static-data loading, live CM_DELETE_QUEST recipe delete/send branch, abandon service contract, focused tests.
- Specific behavior/contract: Java work_order recipe ids load from real XML; TASK work-order abandon exposes a recipe delete candidate; non-work-order templates do not; existing recipe delete persistence/memory semantics are reused by the handler.
- Focused C# command: dotnet test tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~WorkOrderRecipeTableTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~Recipe" --no-restore -> 55/55 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live player recipe-state mutation and StaticData loading changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered loader, packet, recipe, and abandon contracts around the changed branch.
- Why this scope is sufficient: tests prove real work_order.xml loading, candidate selection guards, recipe delete packet serialization via existing packet tests, and existing PlayerEnterWorldService recipe deletion behavior through the recipe filter.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.abandonQuest` TASK branch | `QuestAbandonService.Abandon` / `GameServerConnection.HandleDeleteQuestAsync` | Handler/service | Partial | Unit Tested | Partial Parity | Work-order recipe delete now live; quest persistence and timer task cancellation still missing. |
| `WorkOrdersData.recipe_id` | `WorkOrderRecipeTable` / `NearbyQuestTemplateSummary.WorkOrderRecipeId` | Static data/runtime loading | Partial | Unit Tested; Regression Tested | Partial Parity | Loads real Java work_order.xml and joins to runtime quest summaries; full XMLQuests handler registry remains unported. |
| `RecipeList.deleteRecipe` | `PlayerEnterWorldService.DeleteRecipeAsync` / in-memory fallback / `SmRecipeDelete` | State, persistence, packet | Partial | Unit Tested | Partial Parity | Reuses existing recipe delete DB and packet path; no full socket capture for abandon branch. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `WorkOrderRecipeTableTests.Load_ReadsWorkOrderRecipeIdsLikeJavaXmlQuests` | Unit | `WorkOrdersData` JAXB fields | Fixture work_order id and recipe_id parsing | Source-reviewed Java XML shape + C# assertions | Does not run Java JAXB. |
| `WorkOrderRecipeTableTests.RealDataAudit_LoadsJavaWorkOrderRecipeIds` | Regression | Real Java `work_order.xml` | Current real-data count 574 and sample recipe ids | Real repository XML loaded by C# table | Does not instantiate Java `XMLQuests`. |
| `Abandon_TaskWorkOrderQuestReturnsRecipeDeleteCandidateLikeJava` | Unit | `QuestService.abandonQuest` TASK branch | TASK work-order abandon returns the recipe delete candidate | Source-reviewed Java branch + C# contract assertion | Handler socket branch not captured. |
| `Abandon_NonWorkOrderTemplatesDoNotReturnRecipeDeleteCandidate` | Unit | `QuestService.abandonQuest` TASK/WorkOrdersData guards | Non-TASK or missing work-order recipe id does not delete recipe | Source-reviewed Java branch + C# guard assertion | No full client flow. |

## Summary Metrics

- Focused validation: 55 tests passed.
- Live abandon path now mutates quest state, NPC-faction state, quest work-item inventory state, and work-order recipe state.
- Real Java work-order script rows loaded: 574.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client TASK/work-order abandon flow remains unvalidated.
- Quest persistence itself remains incomplete; recipe persistence uses the existing `player_recipes` delete path.
- Full Java `XMLQuests` handler registry is not ported; this UOW loads only the recipe id needed by live abandon.

## Next Recommended UOW

UOW-2578: Cancel the live quest timer task-map entry during quest abandon.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getController().hasTask(TaskId.QUEST_TIMER) -> questTimerEnd/cancelTask.
- Java source method or runtime path: QuestService.abandonQuest, QuestService.questTimerEnd, PlayerController.cancelTask(TaskId.QUEST_TIMER).
- C# runtime artifact to wire or fix: player/controller scheduled task state for quest timers, or the existing live timer scheduler if present, invoked from QuestAbandonService/GameServerConnection.
- Client-visible/state/persistence effect expected: abandoning a timed quest clears the live quest timer task so it cannot fire later, in addition to the already live timer-clear packet.
- Why this is not preview-only/test-only/documentation-only: it mutates live scheduler/controller task state from the live CM_DELETE_QUEST path.
```
