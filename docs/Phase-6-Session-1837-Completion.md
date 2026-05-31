# Phase 6 Session 1837 Completion - Add Logout Craft Cooldown Repository Contract Plan

Date: 2026-05-31
Unit of Work: UOW-1837
Status: Complete

## Scope

Add a disabled repository contract plan for `SavePlayerCraftCooldownsAsync`, including exact SQL, repository interface shape, fake repository capture requirements, and database integration expectations before live implementation.

## Completed Work

- Re-read required orchestration/parity/progress docs and the UOW-1836 handoff.
- Inspected C# `IPlayerEnterWorldRepository` craft/portal cooldown method area.
- Inspected C# fake repository portal cooldown capture pattern.
- Inspected C# `CapturingEnterWorldRepository` capture pattern.
- Inspected C# `PlayerEnterWorldRepositoryDatabaseIntegrationTests` opt-in DB test style.
- Added `PlayerLogoutCraftCooldownRepositoryContractPlanService.CreateDisabledPlan(...)`.
- Added `PlayerLogoutCraftCooldownRepositoryContractPlan`.
- Added `PlayerLogoutCraftCooldownRepositoryContractPlanStatus`.
- Recorded the future repository contract without changing the interface:
  - exact future method signature
  - Java delete and insert SQL
  - target repository interface and implementation names
  - fake repository capture property name
  - opt-in database integration test name
  - whether Java connection/error behavior is preserved or requires intentional-difference documentation
- Added focused tests for Java-shaped contract planning, intentional-difference documentation requirements, and missing decision blocking.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused logout/craft tests passed with 112 tests.
- Standard Phase 6 slice passed with 404 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4605 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.dao.CraftCooldownsDAO.INSERT_QUERY`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.DELETE_QUERY`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.storeCraftCooldowns`

## Migration Parity Table - UOW-1837

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftCooldownsDAO.INSERT_QUERY` and `DELETE_QUERY` | `PlayerLogoutCraftCooldownRepositoryContractPlan` | Repository Contract Descriptor | Partial | Unit Tested | Partial Parity | C# records exact Java SQL for future `SavePlayerCraftCooldownsAsync`; no interface or SQL implementation is added. |
| `CraftCooldownsDAO.storeCraftCooldowns` repository boundary | `PlayerLogoutCraftCooldownRepositoryContractPlanService.CreateDisabledPlan` | Repository Contract Planner | Partial | Unit Tested | Partial Parity | C# records future interface/fake/integration-test expectations and blocks when connection/error decisions are missing. |
| `CraftCooldownsDAO.storeCraftCooldowns` connection/error behavior | `PlayerLogoutCraftCooldownRepositoryContractPlan` | Repository Diagnostic | Partial | Unit Tested | Partial Parity | C# records whether live implementation must preserve Java separate connections/per-operation swallowed SQL exceptions or document intentional differences. |

## Risks / Gaps

- Repository contract planning is disabled and does not alter `IPlayerEnterWorldRepository`.
- No `SavePlayerCraftCooldownsAsync` method or implementation exists yet.
- No fake repository capture or database integration test exists yet.
- Logout still does not persist craft cooldowns.
- Full logout craft cooldown database parity remains unverified.

## Next Recommended Unit of Work

- Add the disabled repository interface/fake capture slice for `SavePlayerCraftCooldownsAsync` without live MySQL implementation, using the contract plan as the checklist and keeping logout unwired.
- Safe alternatives:
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`
  - add a disabled finish-craft live-execution readiness checklist
  - inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1837-Completion.md`
- `docs/Phase-6-Session-1837-Handoff.md`
