# Phase 6 Session 1846 Completion - Add Drop Salvation Resolved Input

Date: 2026-05-31
Unit of Work: UOW-1846
Status: Complete

## Scope

Add an explicit resolved salvation-percent input to the disabled drop boost readiness planner, after confirming C# has no persisted/player-owned salvation-point surface equivalent to Java `PlayerCommonData.salvationPoint`.

## Completed Work

- Re-read required orchestration/parity/progress docs and the UOW-1845 handoff/completion.
- Re-inspected Java `PlayerCommonData.getCurrentSalvationPercent`, Java `DropRegistrationService.calculateBoostDropRate`, C# `Player`, and C# quest XP salvation call sites.
- Confirmed Java `getCurrentSalvationPercent()` returns 0 for no salvation points, computes `salvationPoint / 1000`, and caps at 30.
- Confirmed C# currently has resolved salvation-percent parameters in quest XP planning, but no player-owned salvation-point state.
- Added optional `salvationPercent` input to `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan(...)`.
- Added `WorldNpcDropBoostRateContextPlan.SalvationPercent`.
- Added `WorldNpcDropBoostRateContextPlan.HasSalvation`.
- Planner treats a supplied zero percent as explicit source evidence without adding the Java +5 drop boost.
- Planner treats a supplied positive percent as explicit source evidence and sets `WorldNpcDropBoostRateContext.HasSalvation`.
- Added focused tests for zero and nonzero resolved salvation percent.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDropModifierServiceTests|FullyQualifiedName~DropChanceFormulaServiceTests|FullyQualifiedName~WorldNpcGlobalDropServiceTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused drop tests passed with 54 tests.
- Standard Phase 6 slice passed with 472 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4623 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData.getCurrentSalvationPercent`
- `com.aionemu.gameserver.services.drop.DropRegistrationService.calculateBoostDropRate`

## Migration Parity Table - UOW-1846

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerCommonData.getCurrentSalvationPercent` | `WorldNpcDropBoostRateContextPlanService.CreateDisabledPlan(..., salvationPercent)` | Resolved Input Adapter | Partial | Unit Tested | Partial Parity | C# can consume an explicitly resolved salvation percent for drop boost planning, but still lacks a persisted/player-owned salvation-point source equivalent to Java `PlayerCommonData.salvationPoint`. |
| `DropRegistrationService.calculateBoostDropRate` salvation branch | `WorldNpcDropBoostRateContextPlan.HasSalvation` / `WorldNpcDropBoostRateContext.HasSalvation` | Readiness Planner Input | Partial | Unit Tested | Partial Parity | Planner applies the Java drop-boost condition `salvationPercent > 0` when supplied. Live workflow wiring remains blocked by stat sources. |

## Risks / Gaps

- The planner is disabled/readiness-only and is not wired into `WorldNpcDropRegistrationWorkflowService`.
- C# still lacks live NPC/player `BOOST_DROP_RATE` and `DR_BOOST` stat sources for this workflow.
- C# still lacks a modeled/persisted player salvation-point source equivalent to Java `PlayerCommonData.salvationPoint`.
- Active-house parity depends on C# login house ordering, not a separate Java-style studio/custom-house map.
- UOW-1840 live DB verification remains pending because Docker/MySQL was unavailable in prior work.

## Next Recommended Unit of Work

- Inspect C# stat containers/effect stat models for a narrow `BOOST_DROP_RATE` / `DR_BOOST` source, then add a resolved-stat planner adapter without wiring workflow execution.
- Safe alternatives:
  - add a real player salvation-point/current-percent surface only if persistence and lifecycle sources are identified from Java and C#
  - run the opt-in logout craft cooldown DB integration suite once Docker/MySQL is available
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcDropModifierService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldNpcDropModifierServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1846-Completion.md`
- `docs/Phase-6-Session-1846-Handoff.md`
