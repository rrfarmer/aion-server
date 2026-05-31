# Phase 6 Session 1804 Handoff - Craft Recipe and Cooldown Guards

Date: 2026-05-31
Unit of Work: UOW-1804
Status: Completed

## What Changed

- Added recipe delay metadata to `RecipeTemplateSummary` and static recipe loading.
- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with learned-recipe and craft-cooldown guards.
- Added `CraftStartValidationStatus.MissingKnownRecipe` and `CraftStartValidationStatus.CraftCooldownActive`.
- Added `SmSystemMessage.CombineCannotFindRecipe()` and `SmSystemMessage.ItemCantUseUntilDelayTime()`.
- Added focused tests for guard ordering and message IDs.

## Files Changed

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1804-Completion.md`
- `docs/Phase-6-Session-1804-Handoff.md`
- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.gameobjects.player.RecipeList.isRecipePresent`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns.hasCooldown`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Dataholders.RecipeTemplateSummary`
- `Aion.GameServer.Dataholders.StaticData`
- `Aion.GameServer.Services.CraftService`
- `Aion.GameServer.Services.CraftStartValidationPlan`
- `Aion.GameServer.Services.CraftStartValidationStatus`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.CraftServiceTests`
- `Aion.GameServer.Tests.GamePacketTests`

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Results:

- Focused craft/packet/static-data tests passed with 287 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4532 tests.

## Parity Table Summary

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` recipe ownership guard | `CraftService.CreateStartCraftingValidationPlan` `MissingKnownRecipe` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Checks `Player.Recipes` after inventory guard and attaches message id `1330043`; live fanout remains pending. |
| `CraftService.checkCraft` craft cooldown guard | `CraftService.CreateStartCraftingValidationPlan` `CraftCooldownActive` branch | Validation Guard | Partial | Unit Tested | Partial Parity | Checks `CraftDelayId` in `Player.CraftCooldowns`, matching Java `hasCooldown` presence semantics for this branch; live fanout remains pending. |
| `RecipeTemplate` `craft_delay_id` / `craft_delay_time` attributes | `RecipeTemplateSummary.CraftDelayId` / `CraftDelayTime` | Static Data Projection | Partial | Regression Tested | Partial Parity | Delay attributes are projected. Recipe components remain pending for material validation. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_CAN_NOT_FIND_RECIPE` | `SmSystemMessage.CombineCannotFindRecipe` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330043` verified. |
| `SM_SYSTEM_MESSAGE.STR_ITEM_CANT_USE_UNTIL_DELAY_TIME` | `SmSystemMessage.ItemCantUseUntilDelayTime` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1300494` verified. |

## Known Gaps

- No live `CraftService.startCrafting` execution.
- No live failure packet or cancel packet sending.
- Skill presence/level, material validation/consumption, bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- Recipe components and max-production fields are not yet projected.
- Cooldown expiration cleanup outside this branch is not modeled here.

## Risks

- Skill validation needs skill template l10n values for Java system-message parameters.
- Material validation requires recipe component projection before it can be responsibly modeled.
- Live failure fanout must preserve Java ordering: failing guard system message, then `sendCancelCraft` from `startCrafting`.

## Next Recommended Unit of Work

- Next sequential task: port skill presence and skill level guard planning from `CraftService.checkCraft`, including `STR_COMBINE_CANT_USE` and `STR_COMBINE_OUT_OF_SKILL_POINT`.

Safe alternative candidates:

- Wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending.
- Start recipe component projection needed for material validation.
- Investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`.
- Port Java `DropRegistrationService.calculateBoostDropRate`.

## Files To Avoid Editing Concurrently

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `docs/PHASE-6-PROGRESS.md`

## Suggested Next-Session Context

- Re-read `csharp-port.md`, `orchestration-rules.md`, `parity-verification.md`, `PHASE-6-PROGRESS.md`, and this handoff before choosing the next UOW.
- Re-inspect Java `CraftService.checkCraft`, `SkillList.isSkillPresent`, `SkillList.getSkillLevel`, `SkillTemplate.getL10n`, and `SM_SYSTEM_MESSAGE`.
- Keep material mutation out of scope until recipe component projection and failure packet orchestration are explicit.
