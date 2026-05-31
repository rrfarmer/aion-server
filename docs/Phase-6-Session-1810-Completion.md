# Phase 6 Session 1810 Completion - Add Craft Task Interval Planning

Date: 2026-05-31
Unit of Work: UOW-1810
Status: Complete

## Scope

Port the non-live start-craft task timing plan from Java `CraftService.startCrafting`: skill-level difference, quality-based interval cap, morph interval override, and bonus craft crit modifier. This unit intentionally does not create a live `CraftingTask`, start scheduler work, send craft packets, spend DP, mutate inventory, or complete crafting.

## Completed Work

- Added `CraftService.CreateStartTaskPlan(...)`.
- Added `CraftStartTaskPlan` and `CraftStartTaskPlanStatus`.
- Ported Java interval cap rules:
  - default `1200`
  - `UNIQUE` / `EPIC` cap `1500`
  - `MYTHIC` cap `1700`
- Ported Java interval formula:
  - morph skill `40009` uses fixed interval `200`
  - other craft skills use `max(intervalCap, 2500 - skillLvlDiff * 60)`
- Ported bonus craft modifier value `15` when `craftType == 1`.
- Added focused tests for the Java interval formula, quality caps, morph fixed interval, bonus modifier, and failed-validation no-op behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused craft/packet tests passed with 283 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4548 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.skillengine.task.CraftingTask`
- `com.aionemu.gameserver.skillengine.task.AbstractCraftTask`
- `com.aionemu.gameserver.model.templates.item.ItemTemplate.getItemQuality`

## Migration Parity Table - UOW-1810

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` skill-level difference | `CraftService.CreateStartTaskPlan` `SkillLevelDiff` | Task Planner | Partial | Unit Tested | Partial Parity | C# computes `CurrentSkillLevel - RequiredSkillPoint` from validation evidence; no live task is created. |
| `CraftService.startCrafting` quality interval cap | `CraftService.CreateStartTaskPlan` `IntervalCap` | Task Planner | Complete | Unit Tested | Verified Parity | Java cap values for default, `UNIQUE`/`EPIC`, and `MYTHIC` are covered by tests. |
| `CraftService.startCrafting` interval selection | `CraftService.CreateStartTaskPlan` `Interval` | Task Planner | Partial | Unit Tested | Partial Parity | C# ports formula and morph override; scheduler timing/live task startup remains pending. |
| `CraftingTask` bonus constructor argument | `CraftService.CreateStartTaskPlan` `BonusCritModifier` | Task Planner | Partial | Unit Tested | Partial Parity | C# records `15` for `craftType == 1`; no live `CraftingTask` exists. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No live `CraftingTask` instance or scheduler startup.
- No craft update/animation success/failure packet loop.
- No DP spend, inventory mutation, persistence, or craft completion path.

## Next Recommended Unit of Work

- Start live CM_CRAFT selected-material/craft-type adapter planning so client inputs can feed existing validation/consumption/task planners.
- Safe alternatives:
  - begin non-live inventory mutation plan for material/bonus consumption
  - plan craft cooldown application after successful finish
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1810-Completion.md`
- `docs/Phase-6-Session-1810-Handoff.md`
