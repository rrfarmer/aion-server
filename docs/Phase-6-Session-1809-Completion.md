# Phase 6 Session 1809 Completion - Add Craft Consumption Planning

Date: 2026-05-31
Unit of Work: UOW-1809
Status: Complete

## Scope

Port the non-mutating successful-tail consumption plan for Java `CraftService.checkCraft`: after validation succeeds, plan bonus item decrease first for `craftType == 1`, then plan decreases for each component in the selected `components_data` group. This unit intentionally does not mutate live player inventory, persist item changes, emit inventory packets, spend DP, start scheduler work, or complete crafting.

## Completed Work

- Added `CraftService.CreateStartConsumptionPlan(...)`.
- Added `CraftStartConsumptionPlan`, `CraftStartConsumptionStatus`, `CraftStartConsumedItemPlan`, and `CraftStartConsumedItemKind`.
- Reused selected component group resolution from material validation.
- Planned Java consumption order:
  - bonus item via `getBonusReqItem(skillId)` when `craftType == 1`
  - selected component group items in recipe component order
- Added focused tests proving bonus-before-component order, selected group behavior without bonus, and no plan when validation failed.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 279 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4544 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `com.aionemu.gameserver.services.craft.CraftService.getBonusReqItem`
- `com.aionemu.gameserver.model.templates.recipe.ComponentsData`
- `com.aionemu.gameserver.model.templates.recipe.Component`

## Migration Parity Table - UOW-1809

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.checkCraft` successful bonus decrease | `CraftService.CreateStartConsumptionPlan` bonus decrease row | Consumption Planner | Partial | Unit Tested | Partial Parity | C# plans bonus item decrease before material components for `craftType == 1`; no live mutation occurs. |
| `CraftService.checkCraft` successful component decreases | `CraftService.CreateStartConsumptionPlan` component decrease rows | Consumption Planner | Partial | Unit Tested | Partial Parity | C# plans decreases for the selected component group in recipe order; no live mutation occurs. |
| `ComponentsData.getComponent` ordering | `RecipeComponentDataSummary.Components` through consumption plan | Data Ordering | Partial | Unit Tested | Partial Parity | Tests cover selected group order with multiple components. Static data projection was added in UOW-1806. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No live inventory mutation, persistence, or inventory update packets.
- No DP spend, task interval, scheduler startup, or craft completion.
- Live CM_CRAFT parsing still does not feed selected materials or craft type into this planner.

## Next Recommended Unit of Work

- Add start-craft success task interval planning from Java `CraftService.startCrafting`, including quality-based interval cap, skill-level difference, morph interval, and bonus craft crit modifier.
- Safe alternatives:
  - start live CM_CRAFT selected-material/craft-type adapter work
  - begin non-live inventory mutation plan for material/bonus consumption
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1809-Completion.md`
- `docs/Phase-6-Session-1809-Handoff.md`
