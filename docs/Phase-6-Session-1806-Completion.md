# Phase 6 Session 1806 Completion - Add Craft Component Material Planning

Date: 2026-05-31
Unit of Work: UOW-1806
Status: Complete

## Scope

Port the next deterministic Java `CraftService.checkCraft` slice after skill validation: recipe component projection and non-mutating material validation for the selected `components_data` group. This unit intentionally stops before bonus item checks, inventory consumption, DP spend, task interval calculation, scheduler startup, live failure fanout, and craft completion.

## Completed Work

- Added `RecipeComponentDataSummary` and `RecipeComponentSummary` to model Java `components_data/component` recipe entries.
- Updated static recipe XML loading to project `components_data` groups and nested `component itemid/quantity` rows.
- Extended `CraftService.CreateStartCraftingValidationPlan(...)` with optional selected-material data matching Java `sendMaterialsData` group selection.
- Added `CraftStartValidationStatus.MissingComponentItem` and missing-component evidence fields on `CraftStartValidationPlan`.
- Added `SmSystemMessage.CombineNoComponentItemSingle()` for Java message `1330046`.
- Added `SmSystemMessage.CombineNoComponentItemMultiple()` for Java message `1330047`.
- Added focused tests for selected group validation, skill-before-material ordering, exact missing-material message IDs, and real static recipe component projection.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet/static-data tests passed with 291 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4536 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate.getComponents`
- `com.aionemu.gameserver.model.templates.recipe.ComponentsData.getComponent`
- `com.aionemu.gameserver.model.templates.recipe.Component`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## Migration Parity Table - UOW-1806

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `RecipeTemplate` `components_data/component` | `RecipeTemplateSummary.ComponentGroups` | Static Data Projection | Partial | Regression Tested | Partial Parity | Real static data projection verified for recipe `155000001`; broader max-production and live craft input use remain pending. |
| `CraftService.checkCraft` selected material group validation | `CraftService.CreateStartCraftingValidationPlan` `MissingComponentItem` branch | Validation Guard | Partial | Unit Tested | Partial Parity | C# validates only the group whose first component id appears in selected material data, matching Java `sendMaterialsData.containsKey(firstComponent.getItemId())`. Live fanout and mutation remain pending. |
| `Inventory.getItemCountByItemId` for craft materials | `CraftService` cube item count helper | Inventory Read | Partial | Unit Tested | Partial Parity | Counts non-equipped cube items by item id. Full Java storage behavior and consumption are not claimed. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_NO_COMPONENT_ITEM_SINGLE` | `SmSystemMessage.CombineNoComponentItemSingle` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330046` verified through packet/system-message tests. |
| `SM_SYSTEM_MESSAGE.STR_COMBINE_NO_COMPONENT_ITEM_MULTIPLE` | `SmSystemMessage.CombineNoComponentItemMultiple` | Server Packet Factory | Complete | Unit Tested | Verified Parity | Message id `1330047` and parameter order verified through packet/system-message tests. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- `FailurePacket` remains planner evidence only; no live system-message or cancel packet fanout is wired.
- Material consumption is not implemented; this unit only plans missing-material failures.
- Bonus item consumption, DP spend, task interval, scheduler startup, and craft completion remain pending.
- The selected-material map is a planner input and is not yet parsed from the live client packet path.

## Next Recommended Unit of Work

- Port the bonus craft item requirement guard from Java `CraftService.checkCraft`, including `getBonusReqItem(skillId)` and single-item missing message planning, still without consuming inventory.
- Safe alternatives:
  - wire non-live validation failure orchestration that combines `FailurePacket` and cancel packet plans without live sending
  - start live CM_CRAFT selected-material data adapter work
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/RecipeTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GamePacketTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1806-Completion.md`
- `docs/Phase-6-Session-1806-Handoff.md`
