# Phase 6 Session 1811 Completion - Add Craft Finish Cooldown Planning

Date: 2026-05-31
Unit of Work: UOW-1811
Status: Complete

## Scope

Port the non-live successful finish-craft cooldown planning branch from Java `CraftService.finishCrafting`. This unit intentionally does not mutate `Player.CraftCooldowns`, persist cooldowns, send finish packets, run scheduler work, or execute a live craft completion path.

## Completed Work

- Added `CraftService.CreateFinishCooldownPlan(...)`.
- Added `CraftFinishCooldownPlan` and `CraftFinishCooldownStatus`.
- Ported Java cooldown branch detection:
  - no cooldown when `recipeTemplate.getCraftDelayId() == null`
  - cooldown planned when `craftDelayId` exists
- Ported Java reuse timestamp formula:
  - `reuseTimeMillis = currentTimeMillis + craftDelayTime * 1000`
- Kept the planner non-live and non-mutating; tests confirm existing `Player.CraftCooldowns` entries are not changed.
- Added focused tests for planned cooldown timestamp arithmetic, no-delay recipes, and the conservative missing-delay-time guard.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 286 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4551 tests.
- The first broad attempt used a 120s timeout and timed out before producing a result; the 300s rerun passed.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.finishCrafting`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate.getCraftDelayId`
- `com.aionemu.gameserver.model.templates.recipe.RecipeTemplate.getCraftDelayTime`
- `com.aionemu.gameserver.model.gameobjects.player.Player.getCraftCooldowns`

## Migration Parity Table - UOW-1811

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.finishCrafting` cooldown branch | `CraftService.CreateFinishCooldownPlan` | Cooldown Planner | Partial | Unit Tested | Partial Parity | C# detects recipes with and without `craftDelayId`; no live finish path is wired. |
| `CraftService.finishCrafting` reuse timestamp formula | `CraftFinishCooldownPlan.ReuseTimeMillis` | Cooldown Planner | Complete | Unit Tested | Verified Parity | C# computes `currentTimeMillis + craftDelayTime * 1000` with deterministic test input. |
| `Player.getCraftCooldowns().put(craftDelayId, reuseTimeMillis)` | `CraftFinishCooldownPlan.ShouldApplyCooldown` and planned fields | Cooldown Planner | Partial | Unit Tested | Partial Parity | C# records the intended mutation without applying it to `Player.CraftCooldowns`. |

## Risks / Gaps

- No live `CraftService.finishCrafting` execution.
- No live mutation of `Player.CraftCooldowns`.
- No cooldown persistence or packet fanout is wired.
- No craft completion scheduler path invokes this planner.
- C# still does not execute the full Java start-to-finish craft runtime.

## Next Recommended Unit of Work

- Start live CM_CRAFT selected-material/craft-type adapter planning so client inputs can feed existing validation/consumption/task planners.
- Safe alternatives:
  - begin non-live inventory mutation plan for material/bonus consumption
  - add a live-safe craft finish cooldown application mutation plan
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1811-Completion.md`
- `docs/Phase-6-Session-1811-Handoff.md`
