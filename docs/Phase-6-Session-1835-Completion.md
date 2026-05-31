# Phase 6 Session 1835 Completion - Add Logout Craft Cooldown Save Plan

Date: 2026-05-31
Unit of Work: UOW-1835
Status: Complete

## Scope

Begin live logout craft cooldown save design only after explicit connection/error behavior scoping, using existing disabled craft cooldown persistence planners as evidence. This unit intentionally does not wire live database writes.

## Completed Work

- Inspected Java `PlayerService.storePlayer`.
- Inspected Java `CraftCooldownsDAO.storeCraftCooldowns`, `deleteCraftCoolDowns`, and `loadCraftCooldowns`.
- Inspected Java `Cooldowns` expiry/removal behavior.
- Inspected C# `PlayerEnterWorldService.LeaveWorldAsync`.
- Inspected C# `PlayerEnterWorldRepository.SavePlayerLogoutAsync` and existing skill/item/house-object/portal cooldown save paths.
- Added `PlayerLogoutCraftCooldownSavePlanService.CreateDisabledPlan(...)`.
- Added `PlayerLogoutCraftCooldownSavePlan`.
- Added `PlayerLogoutCraftCooldownSavePlanStatus`.
- Recorded Java logout craft cooldown persistence behavior without writing:
  - `PlayerService.storePlayer` calls `CraftCooldownsDAO.storeCraftCooldowns` after `PortalCooldownsDAO.storePortalCooldowns`.
  - Java stores craft cooldowns before `HouseObjectCooldownsDAO.storeHouseObjectCooldowns`.
  - Java deletes all existing craft cooldown rows first.
  - Java opens one connection for the delete and one connection per active insert.
  - Java logs and swallows delete and insert `SQLException`s.
  - Existing C# logout save path still does not execute craft cooldown persistence.
- Added focused tests for active/expired cooldown planning, delete-only planning, and missing-player behavior.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~CmCraft|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CraftingXpFormulaServiceTests|FullyQualifiedName~StaticDataLoadingTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused logout/craft tests passed with 106 tests.
- Standard Phase 6 slice passed with 398 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4599 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.player.PlayerService.storePlayer`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.storeCraftCooldowns`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.deleteCraftCoolDowns`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns`

## Migration Parity Table - UOW-1835

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerService.storePlayer` craft cooldown call order | `PlayerLogoutCraftCooldownSavePlanService.CreateDisabledPlan` | Logout Persistence Planner | Partial | Unit Tested | Partial Parity | C# records Java call order after portal cooldowns and before house-object cooldowns; live logout save remains unwired. |
| `CraftCooldownsDAO.storeCraftCooldowns` | `PlayerLogoutCraftCooldownSavePlan` plus `CraftCooldownPersistencePlanService` | Persistence Descriptor | Partial | Unit Tested | Partial Parity | C# records delete-first and active-insert SQL intent using existing descriptors; no SQL is executed. |
| `CraftCooldownsDAO` connection/error behavior | `PlayerLogoutCraftCooldownSavePlan` | Persistence Diagnostic | Partial | Unit Tested | Partial Parity | C# records Java one-connection-per-operation and swallowed/logged `SQLException` behavior; live exception handling remains future work. |

## Risks / Gaps

- Logout craft cooldown persistence remains disabled and does not write to the database.
- C# `SavePlayerLogoutAsync` still saves skill, item, house-object, and portal cooldowns but not craft cooldowns.
- Java opens separate connections for delete and each insert; current C# cooldown saves usually reuse one connection, so live craft wiring needs an explicit intentional-difference decision or a Java-shaped adapter.
- Java logs and swallows SQL errors per operation; live C# error propagation/return behavior is not wired.
- Full logout persistence parity remains unverified.

## Next Recommended Unit of Work

- Add a live-execution readiness checklist for logout craft cooldown persistence that gates whether to preserve Java's separate connection/error-swallowing behavior or document an intentional C# repository difference before adding `SavePlayerCraftCooldownsAsync`.
- Safe alternatives:
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`
  - add a disabled finish-craft live-execution readiness checklist
  - inspect Java `ItemService.addItem` storage insertion/packet behavior in more detail before reward live wiring

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1835-Completion.md`
- `docs/Phase-6-Session-1835-Handoff.md`
