# Phase 6 Session 1829 Completion - Compose Disabled Craft Finish Cooldown Plans

Date: 2026-05-31
Unit of Work: UOW-1829
Status: Complete

## Scope

Compose the existing non-live finish cooldown timestamp, application projection, persistence descriptors, and disabled persistence adapter into one inspectable finish-cooldown plan. This unit does not mutate cooldown state, execute database writes, or send packets.

## Completed Work

- Added `CraftService.CreateFinishCooldownCompositionPlan(...)`.
- Added `CraftFinishCooldownCompositionPlan`.
- Added `CraftFinishCooldownCompositionStatus`.
- Composed:
  - `CraftFinishCooldownPlan`
  - `CraftFinishCooldownApplicationPlan`
  - `CraftCooldownPersistencePlan`
  - `CraftCooldownPersistenceAdapterPlan`
- Kept all live side effects disabled:
  - no `Player.CraftCooldowns` mutation
  - no DB connection
  - no SQL execution
- Added focused tests for delayed-recipe ready composition and no-delay not-ready composition.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 329 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4581 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.finishCrafting`
- `com.aionemu.gameserver.model.gameobjects.player.Cooldowns.put`
- `com.aionemu.gameserver.dao.CraftCooldownsDAO.storeCraftCooldowns`

## Migration Parity Table - UOW-1829

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.finishCrafting` craft delay branch | `CraftService.CreateFinishCooldownCompositionPlan` | Cooldown Composition Planner | Partial | Unit Tested | Partial Parity | C# composes timestamp, application, and persistence intent for delayed recipes; no live cooldown mutation or persistence occurs. |
| `Cooldowns.put` projection boundary | `CraftFinishCooldownCompositionPlan.ApplicationPlan` | Cooldown Composition Planner | Partial | Unit Tested | Partial Parity | Composition carries the disabled `Cooldowns.put` projection and did/would flags; player state remains unchanged. |
| `CraftCooldownsDAO.storeCraftCooldowns` persistence boundary | `CraftFinishCooldownCompositionPlan.PersistenceAdapterPlan` | Cooldown Composition Planner | Partial | Unit Tested | Partial Parity | Composition carries delete-all/insert-active SQL intent through the disabled adapter; no live database execution occurs. |

## Risks / Gaps

- The finish cooldown composition is disabled and does not mutate `Player.CraftCooldowns`.
- No live craft cooldown DB writes occur.
- `SavePlayerLogoutAsync` still does not save `Player.CraftCooldowns`.
- No finish-time cooldown packet/fanout has been verified or sent.
- Finish-craft skill XP/common XP, reward insertion, recipe deletion, quest callback, and logging behavior remain incomplete or separately planned.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Inspect Java finish-craft packet behavior around craft cooldowns and add a disabled `SM_RECIPE_COOLDOWN` finish-time packet/fanout plan only if Java evidence confirms a packet is sent during or after `finishCrafting`.
- Safe alternatives:
  - add disabled finish-craft skill XP/common XP application planning from Java `finishCrafting`
  - begin live logout craft cooldown save design only after explicit connection/error behavior scoping
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CraftService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CraftServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1829-Completion.md`
- `docs/Phase-6-Session-1829-Handoff.md`
