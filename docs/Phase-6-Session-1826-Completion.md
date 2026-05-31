# Phase 6 Session 1826 Completion - Integrate Craft Start Disabled Adapters Into Facade

Date: 2026-05-31
Unit of Work: UOW-1826
Status: Complete

## Scope

Integrate the concrete disabled craft-start persistence and packet-send adapter plans into the disabled live executor facade. This unit keeps the facade non-live and does not execute inventory mutation, packet dispatch, database persistence, DP spend, or task startup.

## Completed Work

- Extended `CraftStartLiveExecutorFacadePlan` with `InventoryPersistenceAdapterPlan`.
- Extended `CraftStartLiveExecutorFacadePlan` with `InventoryPacketSendAdapterPlan`.
- Updated `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan(...)` to create concrete disabled adapter plans from the ready CM_CRAFT composition.
- Derived persistence write flags from `CraftStartInventoryPersistenceAdapterPlan`.
- Derived packet send flags from `CraftStartInventoryPacketSendAdapterPlan`.
- Preserved Java success boundary order in facade operations.
- Kept missing and not-ready composition paths free of adapter plans.
- Added focused test coverage proving the facade carries both adapter plans while all live dispatch flags remain false.

## Validation

Executed:

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmCraft|FullyQualifiedName~CraftServiceTests|FullyQualifiedName~GamePacketTests" --no-restore`
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName!~GameServerConnectionInventoryExpansionUseItemTests" --no-restore`

Result:

- Focused CM_CRAFT/craft/packet tests passed with 321 tests.
- Broad game-server suite excluding `GameServerConnectionInventoryExpansionUseItemTests` passed with 4573 tests.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.craft.CraftService.startCrafting`
- `com.aionemu.gameserver.dao.InventoryDAO.store`
- `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount`
- `com.aionemu.gameserver.model.items.storage.Storage.delete`

## Migration Parity Table - UOW-1826

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CraftService.startCrafting` live success side-effect sequence | `CraftStartLiveExecutorFacadePlanService.CreateDisabledPlan` | Execution Facade | Partial | Unit Tested | Partial Parity | C# facade now carries concrete disabled persistence and packet-send adapter plans; no side effects dispatch. |
| `InventoryDAO.store` persistence execution boundary | `CraftStartLiveExecutorFacadePlan.InventoryPersistenceAdapterPlan` | Execution Facade | Partial | Unit Tested | Partial Parity | Facade consumes the disabled persistence adapter and derives write flags from it; no DB connection, transaction, SQL, commit, or object-id release occurs. |
| `Storage.decreaseItemCount` / `Storage.delete` packet send boundary | `CraftStartLiveExecutorFacadePlan.InventoryPacketSendAdapterPlan` | Execution Facade | Partial | Unit Tested | Partial Parity | Facade consumes the disabled packet-send adapter and derives send flags from it; no live packet dispatch occurs. |

## Risks / Gaps

- The live executor facade and both adapter plans remain disabled and non-live.
- No live inventory mutation is applied to `Player.InventoryItems`.
- No live inventory packets are sent.
- No item persistence is written to the database.
- No transaction, commit/rollback, Java autocommit behavior, or object-id release is executed.
- No DP spend, live `CraftingTask` creation, scheduler startup, or craft completion is wired.
- Java storage delete quest callbacks/logging are not executed.
- Full start-to-finish craft runtime parity remains unverified.

## Next Recommended Unit of Work

- Add a live-safe craft finish cooldown application mutation plan using Java `CraftService.finishCrafting` cooldown behavior as source of truth, without executing live cooldown updates by default.
- Safe alternatives:
  - begin live-enabled craft persistence adapter design only after explicit transaction/rollback scope
  - investigate and stabilize `GameServerConnectionInventoryExpansionUseItemTests`
  - port Java `DropRegistrationService.calculateBoostDropRate`
  - add disabled DP-spend adapter wiring into the craft-start facade

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/CmCraftStartCompositionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmCraftStartCompositionPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6-Session-1826-Completion.md`
- `docs/Phase-6-Session-1826-Handoff.md`
