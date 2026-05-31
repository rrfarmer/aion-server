# Phase 6 Session 1804 Completion - Add Craft Recipe and Cooldown Guards

Date: 2026-05-31
Unit of Work: UOW-1804
Status: Complete

## Scope

Port the next deterministic Java `CraftService.checkCraft` validation slice after target, DP, stance, and inventory guards: learned-recipe validation and craft cooldown validation. This unit remains planner-level and intentionally stops before live failure fanout, skill presence/level checks, material validation/consumption, bonus item consumption, DP spend, task interval calculation, scheduler startup, and craft completion.

## Completed Work

- Extended `RecipeTemplateSummary` with `CraftDelayId` and `CraftDelayTime`.
- Updated static recipe XML loading to read Java `craft_delay_id` and `craft_delay_time` attributes.
- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with Java guard ordering for:
  - `!player.getRecipeList().isRecipePresent(recipeTemplate.getId())`
  - `recipeTemplate.getCraftDelayId() != null && player.getCraftCooldowns().hasCooldown(recipeTemplate.getCraftDelayId())`
- Added `CraftStartValidationStatus.MissingKnownRecipe` and `CraftStartValidationStatus.CraftCooldownActive`.
- Added `SmSystemMessage.CombineCannotFindRecipe()` for Java message `1330043`.
- Added `SmSystemMessage.ItemCantUseUntilDelayTime()` for Java message `1300494`.
- Added focused tests proving inventory-before-recipe ordering, recipe-before-cooldown ordering, cooldown presence behavior, and exact system-message IDs.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet/static-data tests passed with 287 tests.
- Broad game-server suite excluding the previously order-sensitive `GameServerConnectionInventoryExpansionUseItemTests` passed with 4532 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.player.RecipeList.isRecipePresent`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns.hasCooldown`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## Migration Parity Table - UOW-1804

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` recipe ownership guard | `CraftService.CreateStartCraftingValidationPlan` `MissingKnownRecipe` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# checks `Player.Recipes` after inventory guard and attaches `STR_COMBINE_CAN_NOT_FIND_RECIPE`. Live packet/cancel fanout remains pending. |
| `CraftService.checkCraft` craft cooldown guard | `CraftService.CreateStartCraftingValidationPlan` `CraftCooldownActive` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Java source reviewed; C# checks `RecipeTemplateSummary.CraftDelayId` presence in `Player.CraftCooldowns`, matching Java `Cooldowns.hasCooldown` presence semantics for this branch. Live packet/cancel fanout remains pending. |
| `RecipeTemplate` `craft_delay_id` / `craft_delay_time` attributes | `RecipeTemplateSummary.CraftDelayId` / `CraftDelayTime` | Static Data Projection | Partial | Regression Tested | Partial Parity | XML attributes are now projected into C# summaries. Broader recipe component and max-production data remain outside this unit. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_CAN_NOT_FIND_RECIPE` | `SmSystemMessage.CombineCannotFindRecipe` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330043` verified through packet/system-message tests. |
| `SM_SYSTEM_MESSAGE.STR_ITEM_CANT_USE_UNTIL_DELAY_TIME` | `SmSystemMessage.ItemCantUseUntilDelayTime` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1300494` verified through packet/system-message tests. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateStartCraftingValidationPlan_RejectsMissingKnownRecipeAfterInventoryValidation` | Unit Added | Java `checkCraft` inventory guard before recipe-list guard | Inventory-full failure wins before missing recipe; missing learned recipe attaches message id `1330043`. | Source-derived planner regression plus system-message ID evidence. | Live send/cancel orchestration remains pending. |
| `CreateStartCraftingValidationPlan_RejectsCraftCooldownAfterRecipeValidation` | Unit Added | Java `checkCraft` recipe-list guard before cooldown guard | Missing recipe wins before cooldown; learned recipe with active craft cooldown attaches message id `1300494`. | Source-derived planner regression plus system-message ID evidence. | Cooldown expiration cleanup semantics outside this branch remain broader than this planner. |
| `GamePacketTests` system-message assertions | Unit Updated | Java `SM_SYSTEM_MESSAGE` constants | New C# factories preserve Java message ids `1330043` and `1300494`. | Packet/system-message assertion evidence. | No runtime Java packet capture in this unit. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- `FailurePacket` remains planner evidence only; no live system-message or cancel packet fanout is wired.
- Skill presence, skill level, material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Recipe components and max-production recipe data are still not modeled in `RecipeTemplateSummary`.
- Java cooldown expiration cleanup is broader than this branch; this unit only matches the `hasCooldown` presence check used by `checkCraft`.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows in this unit.
- Total artifacts ported: 2 validation branches, 2 status values, 2 system-message factories, recipe delay metadata projection, and 3 focused test updates.
- Total artifacts with verified parity: 2 system-message factory rows.
- Total artifacts needing verification: 3 grouped validation/static-data rows pending live orchestration and broader recipe data.
- Total blocked artifacts: live start-craft execution, live validation failure fanout, skill/material validation, DP spend, and scheduler startup.
- Estimated overall migration completion: Phase 6 remains about 73%.

## Next Recommended Unit of Work

- Port the next smallest `CraftService.checkCraft` validation slice: skill presence and skill level guard planning, including `STR_COMBINE_CANT_USE` and `STR_COMBINE_OUT_OF_SKILL_POINT`, still without material mutation or scheduler startup.
- Safe alternatives:
  - wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending
  - start recipe component projection needed for material validation
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1804-Completion.md`
- `docs/Phase-6-Session-1804-Handoff.md`
