# Phase 6 Session 1828 Completion - Add Disabled Craft Cooldown Persistence Plan

Date: 2026-05-31
Unit of Work: UOW-1828
Status: Complete

## Scope

Add exact Java SQL descriptors and a disabled persistence adapter for craft cooldown saves using Java `CraftCooldownsDAO.storeCraftCooldowns` as source of truth. This unit does not wire live logout persistence or execute database writes.

## Completed Work

- Added exact Java craft cooldown insert/delete SQL constants.
- Added `CraftCooldownPersistencePlanService.CreateDisabledPlan(...)`.
- Added `CraftCooldownPersistencePlan`.
- Added `CraftCooldownPersistenceSqlDescriptor`.
- Added `CraftCooldownPersistenceAdapterPlanService.CreateDisabledPlan(...)`.
- Added `CraftCooldownPersistenceAdapterPlan`.
- Added `CraftCooldownPersistenceAdapterOperation`.
- Added plan, adapter, and SQL operation enums.
- Modeled Java persistence order:
  - delete all existing player craft cooldown rows first
  - insert active cooldown entries second
  - skip entries where `reuseTime < System.currentTimeMillis()`
- Kept all persistence behavior disabled and non-live.
- Added focused tests for SQL descriptor shape/order, disabled adapter operations, and delete-only behavior when all cooldowns are expired.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 327 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4579 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.dao.CraftCooldownsDAO`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.storeCraftCooldowns`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.deleteCraftCoolDowns`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns.entrySet`

## Migration Parity Table - UOW-1828

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftCooldownsDAO.DELETE_QUERY` | `CraftCooldownPersistencePlanService.JavaCraftCooldownDeleteSql` | SQL Descriptor | Partial | Unit Tested | Partial Parity | C# records the exact Java delete-all SQL for player craft cooldown rows; no DB execution occurs. |
| `CraftCooldownsDAO.INSERT_QUERY` | `CraftCooldownPersistencePlanService.JavaCraftCooldownInsertSql` | SQL Descriptor | Partial | Unit Tested | Partial Parity | C# records the exact Java active-cooldown insert SQL and parameter identity fields; no DB execution occurs. |
| `CraftCooldownsDAO.storeCraftCooldowns` delete-before-insert behavior | `CraftCooldownPersistencePlan.SqlDescriptors` | Persistence Planner | Partial | Unit Tested | Partial Parity | C# plans delete-all before active inserts and skips expired entries using Java's `< currentTimeMillis` check; live logout persistence remains unwired. |
| `CraftCooldownsDAO.storeCraftCooldowns` SQL execution boundary | `CraftCooldownPersistenceAdapterPlanService.CreateDisabledPlan` | Persistence Adapter | Partial | Unit Tested | Partial Parity | C# records would-open-connection and would-execute-SQL flags; no connection is opened and no SQL is executed. |

## Risks / Gaps

- The craft cooldown persistence adapter is disabled and does not execute database writes.
- `SavePlayerLogoutAsync` still does not call a live craft cooldown save path.
- No transaction/rollback behavior is modeled for craft cooldown persistence; Java opens separate connections for delete and each insert.
- No `SM_RECIPE_COOLDOWN` finish-time packet/fanout has been verified or sent.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Integrate the disabled craft cooldown persistence descriptor/adapter into a non-live finish-craft cooldown composition plan so application projection and persistence intent can be inspected together without wiring live logout/database writes.
- Safe alternatives:
  - add a disabled `SM_RECIPE_COOLDOWN` finish-time packet/fanout plan if Java evidence confirms packet dispatch timing
  - add disabled finish-craft skill XP/common XP application planning from Java `finishCrafting`
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1828-Completion.md`
- `docs/Phase-6-Session-1828-Handoff.md`
