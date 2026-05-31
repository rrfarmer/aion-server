# Phase 6 Session 1814 Completion - Add Static-Data CM_CRAFT Ready-Path Coverage

Date: 2026-05-31
Unit of Work: UOW-1814
Status: Complete

## Scope

Add static-data-backed integration coverage for the CM_CRAFT composition observer seam using a real recipe/product from loaded game data. This unit intentionally does not execute live `CraftService.startCrafting`, spend DP, mutate inventory, send packets, create `CraftingTask`, or start scheduler work.

## Completed Work

- Added a CM_CRAFT integration test that loads real `DataManager` static data.
- Used real recipe `155000001` from `recipe_templates.xml`:
  - morph skill `40009`
  - product `152000401`
  - required DP `200`
  - component `152000901 x1`
- Sent a real CM_CRAFT packet through `GameServerConnection.ProcessPacketAsync`.
- Verified the observer emits `CmCraftStartCompositionPlanStatus.ReadyForDpSpendAndTaskStart`.
- Verified Java-derived planner outputs from the ready path:
  - validation ready
  - component consumption planned for `152000901 x1`
  - morph task interval `200`
  - DP spend requirement recorded as `200`
- Verified no packets are sent and no live side effects are dispatched.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 305 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4557 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_CRAFT.runImpl`
- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.services.craft.CraftService.checkCraft`
- `game-server/data/static_data/recipe/recipe_templates.xml` recipe `155000001`

## Migration Parity Table - UOW-1814

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_CRAFT.runImpl` successful guard forwarding for real recipe `155000001` | `GameServerConnection.HandleCraftAsync` + `CmCraftStartCompositionPlan` observer | Handler Adapter | Partial | Integration Tested | Partial Parity | Real packet processing with loaded static data reaches ready composition; live `startCrafting` remains deferred. |
| `CraftService.checkCraft` selected material path for recipe `155000001` | `CraftStartConsumptionPlan` via handler observer | Handler Adapter | Partial | Integration Tested | Partial Parity | C# plans component decrease for `152000901 x1`; no live inventory mutation occurs. |
| `CraftService.startCrafting` morph interval and DP requirement for recipe `155000001` | `CraftStartTaskPlan.Interval` and `CmCraftStartCompositionPlan.RequiredDp` via handler observer | Handler Adapter | Partial | Integration Tested | Partial Parity | C# records morph interval `200` and required DP `200`; no DP spend or task start occurs. |

## Risks / Gaps

- No live `CraftService.startCrafting` execution.
- No DP spend, live inventory mutation, persistence, packet sending, live `CraftingTask`, or scheduler startup is wired.
- Coverage uses a morph recipe; non-morph static-object ready-path coverage remains pending because C# still lacks a direct Java `StaticObject` model.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Begin non-live inventory mutation planning for material/bonus consumption so the successful `checkCraft` consumption plan can be turned into explicit updated/deleted inventory item intents.
- Safe alternatives:
  - add a live-safe craft finish cooldown application mutation plan
  - broaden CM_CRAFT handler composition coverage for non-morph target facts when StaticObject modeling is available
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionCraftTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1814-Completion.md`
- `docs/Phase-6-Session-1814-Handoff.md`
