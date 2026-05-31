# Phase 6 Session 1831 Completion - Add Disabled Finish Craft Work-Order Plan

Date: 2026-05-31
Unit of Work: UOW-1831
Status: Complete

## Scope

Add disabled finish-craft work-order recipe deletion and fail-craft quest callback planning from Java `CraftService.finishCrafting` `maxProductionCount` branch. This unit does not mutate player recipes, write `player_recipes`, send recipe packets, or execute quest handlers.

## Completed Work

- Inspected Java `CraftService.finishCrafting`, `RecipeTemplate`, `RecipeList.deleteRecipe`, and `QuestEngine.onFailCraft`.
- Confirmed the Java work-order branch runs before XP/reward when `recipetemplate.getMaxProductionCount() != null`.
- Added `RecipeTemplateSummary.MaxProductionCount`.
- Updated static recipe loading to read XML `max_production_count`.
- Added `CraftService.CreateFinishWorkOrderPlan(...)`.
- Added `CraftFinishWorkOrderPlan`.
- Added `CraftFinishWorkOrderStatus`.
- Modeled Java behavior without live mutation:
  - recipe delete attempt for max-production recipes
  - projected recipe-list removal only when the player knows the recipe
  - projected `SM_RECIPE_DELETE` intent only when deletion would succeed
  - fail-craft quest callback intent only when `critCount == 0`
  - combo-product fallback to `0` when `getComboProduct(1)` is absent
- Added focused tests for failed work-order planning, critical-success planning, and non-work-order skip behavior.
- Added static-data coverage for a real `max_production_count` recipe.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/static-data tests passed with 91 tests.
- Standard CM_CRAFT/craft/packet/craft-XP/static-data tests passed with 363 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4587 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.finishCrafting`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate`
- `com.aionemu.gameserver.model.gameobjects.player.RecipeList.deleteRecipe`
- `com.aionemu.gameserver.questEngine.QuestEngine.onFailCraft`

## Migration Parity Table - UOW-1831

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `RecipeTemplate.maxProductionCount` | `RecipeTemplateSummary.MaxProductionCount` | Static Data Model | Partial | Unit Tested | Partial Parity | C# reads Java XML `max_production_count`; broad JAXB/default parity remains under static-data test coverage. |
| `CraftService.finishCrafting` work-order branch | `CraftService.CreateFinishWorkOrderPlan` | Work-Order Planner | Partial | Unit Tested | Partial Parity | C# plans the Java branch but does not perform live recipe deletion, DB writes, packet sends, or quest callbacks. |
| `RecipeList.deleteRecipe` finish-craft call site | `CraftFinishWorkOrderPlan.ProjectedRecipes` / delete flags | Recipe Deletion Planner | Partial | Unit Tested | Partial Parity | C# projects known-recipe deletion and recipe-delete packet intent; live mutation is disabled. |
| `QuestEngine.onFailCraft` finish-craft call site | `CraftFinishWorkOrderPlan.WouldCallQuestEngineOnFailCraft` | Quest Callback Planner | Partial | Unit Tested | Partial Parity | C# records callback intent and item fallback; registered quest lookup and inventory-count gating remain non-live. |

## Risks / Gaps

- Finish work-order planning is disabled and does not mutate `Player.Recipes`.
- No `PlayerRecipesDAO.delRecipe` equivalent write is executed.
- No `SM_RECIPE_DELETE` packet is sent from finish-craft work-order handling.
- `QuestEngine.onFailCraft` is not executed.
- Registered quest lookup, quest id assignment, and Java inventory-count gating remain future work.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add disabled finish-craft logging intent planning from Java `CraftService.finishCrafting` `LoggingConfig.LOG_CRAFT` branch.
- Safe alternatives:
  - begin live logout craft cooldown save design only after explicit connection/error behavior scoping
  - add a non-live finish-craft orchestration composition after logging intent exists
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1831-Completion.md`
- `docs/Phase-6-Session-1831-Handoff.md`
