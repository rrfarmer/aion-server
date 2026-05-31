# Phase 6 Session 1827 Completion - Add Disabled Craft Finish Cooldown Application Plan

Date: 2026-05-31
Unit of Work: UOW-1827
Status: Complete

## Scope

Add a live-safe, disabled craft-finish cooldown application plan using Java `CraftService.finishCrafting` and `Cooldowns.put` behavior as source of truth. This unit does not mutate `Player.CraftCooldowns`, persist cooldowns, or send cooldown packets.

## Completed Work

- Added `CraftFinishCooldownApplicationPlanService.CreateDisabledPlan(...)`.
- Added `CraftFinishCooldownApplicationPlan`.
- Added `CraftFinishCooldownApplicationStatus`.
- Projected Java `Cooldowns.put` behavior:
  - future reuse times store `craftDelayId -> reuseTimeMillis`
  - expired or immediate reuse times remove the cooldown id
  - missing/unplanned cooldown plans do not mutate state
- Added existing/projected cooldown snapshots.
- Added would/did mutation flags for store and remove paths.
- Kept live cooldown mutation disabled by default.
- Added focused test coverage for future reuse projection, immediate reuse removal projection, and unplanned/no-cooldown skip behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 324 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4576 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.finishCrafting`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns.put`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO`

## Migration Parity Table - UOW-1827

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.finishCrafting` craft delay branch | `CraftFinishCooldownApplicationPlanService.CreateDisabledPlan` | Cooldown Application Planner | Partial | Unit Tested | Partial Parity | C# consumes the existing finish cooldown timestamp plan and records the Java application boundary; no live cooldown map mutation occurs. |
| `Cooldowns.put` future reuse storage | `CraftFinishCooldownApplicationPlan.ProjectedCooldowns` / `WouldStoreCooldown` | Cooldown Application Planner | Partial | Unit Tested | Partial Parity | C# projects storing `craftDelayId -> reuseTimeMillis` when reuse is in the future, while leaving `Player.CraftCooldowns` unchanged. |
| `Cooldowns.put` expired/immediate reuse removal | `CraftFinishCooldownApplicationPlan.ProjectedCooldowns` / `WouldRemoveCooldown` | Cooldown Application Planner | Partial | Unit Tested | Partial Parity | C# projects removing the cooldown id when Java `put` would remove instead of store; no live removal occurs. |

## Risks / Gaps

- The cooldown application plan is disabled and does not mutate `Player.CraftCooldowns`.
- No craft cooldown persistence write is executed through `CraftCooldownsDAO.storeCraftCooldowns` equivalent behavior.
- No `SM_RECIPE_COOLDOWN` finish-time packet/fanout has been verified or sent.
- Finish-craft reward, skill XP, work-order recipe deletion, quest callbacks, and logging are still only partially planned in separate slices.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a disabled craft cooldown persistence descriptor/adapter plan for Java `CraftCooldownsDAO.storeCraftCooldowns` delete-all-then-insert-active behavior, still without live DB writes.
- Safe alternatives:
  - add a disabled `SM_RECIPE_COOLDOWN` finish-time packet/fanout plan if Java evidence confirms packet dispatch timing
  - add disabled finish-craft skill XP/common XP application planning from Java `finishCrafting`
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1827-Completion.md`
- `docs/Phase-6-Session-1827-Handoff.md`
