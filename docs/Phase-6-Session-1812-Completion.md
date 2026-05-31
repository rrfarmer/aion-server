# Phase 6 Session 1812 Completion - Add CM_CRAFT Start Composition Adapter Planning

Date: 2026-05-31
Unit of Work: UOW-1812
Status: Complete

## Scope

Add a non-live CM_CRAFT start composition adapter that takes the parsed Java packet runtime guard result and feeds its `recipeId`, `targetObjId`, `craftType`, and `materialsData` into the existing craft validation, failure, consumption, and task planners. This unit intentionally does not execute live `CraftService.startCrafting`, spend DP, mutate inventory, send packets, create `CraftingTask`, or start scheduler work.

## Completed Work

- Added `CmCraftStartCompositionPlanService.CreatePlan(...)`.
- Added `CmCraftStartCompositionPlan`, `CmCraftStartCompositionPlanStatus`, and `CmCraftStartCompositionPlanStep`.
- Composed existing non-live planner surfaces from the Java packet path:
  - `CmCraftRuntimePlan`
  - `CraftStartValidationPlan`
  - `CraftStartCancelPacketPlan`
  - `CraftStartFailureOrchestrationPlan`
  - `CraftStartConsumptionPlan`
  - `CraftStartTaskPlan`
- Preserved Java packet inputs from `CM_CRAFT.runImpl` into the downstream planners:
  - `recipeId`
  - `targetObjId`
  - `craftType`
  - `materialsData`
- Added focused tests for successful composition, validation-failure/cancel orchestration composition, and runtime guard blocked behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 302 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4554 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT.readImpl`
- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT.runImpl`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`

## Migration Parity Table - UOW-1812

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` forwarding to `CraftService.startCrafting(player, recipeId, targetObjId, craftType, materialsData)` | `CmCraftStartCompositionPlanService.CreatePlan` | Adapter Planner | Partial | Unit Tested | Partial Parity | C# composes existing planners with the same packet inputs after runtime guard success; no live service dispatch occurs. |
| `CraftService.startCrafting` failed `checkCraft` branch then `sendCancelCraft` | `CmCraftStartCompositionPlan` validation failure path | Adapter Planner | Partial | Unit Tested | Partial Parity | C# composes validation, cancel packet planning, and failure orchestration without sending packets. |
| `CraftService.startCrafting` successful `checkCraft` path before DP spend/task start | `CmCraftStartCompositionPlan` ready path | Adapter Planner | Partial | Unit Tested | Partial Parity | C# composes validation, consumption, and task planners; DP spend, inventory mutation, live task creation, and scheduler start remain pending. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No `GameServerConnection` call into the composition adapter is wired yet.
- No DP spend, inventory mutation, persistence, live packet sending, `CraftingTask` creation, or scheduler start.
- C# target facts still depend on current world/NPC abstractions and do not yet model Java `StaticObject` directly.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Wire a non-live `GameServerConnection` observer/composition seam for `CmCraftStartCompositionPlan` so integration tests can observe the adapter from real CM_CRAFT packet processing without dispatching live side effects.
- Safe alternatives:
  - begin non-live inventory mutation plan for material/bonus consumption
  - add a live-safe craft finish cooldown application mutation plan
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1812-Completion.md`
- `docs/Phase-6-Session-1812-Handoff.md`
