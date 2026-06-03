# Phase 6 Session 2422 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2422 disabled legion warehouse persistence contract plan.

## Last Completed UOW

- UOW-2422 added `LegionWarehousePersistenceContractPlanService`.
- The planner distinguishes Java logout warehouse save parameters from periodic warehouse save parameters.
- No repository method, SQL, live logout hook, periodic task, or packet fanout was enabled.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2422] Add legion warehouse persistence contract plan`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/LegionWarehousePersistenceContractPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/LegionWarehousePersistenceContractPlanServiceTests.cs`
- `docs/Phase-6-Session-2422-Completion.md`
- `docs/Phase-6-Session-2422-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/PeriodicSaveService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService`
- `Aion.GameServer.Services.LegionWarehousePersistenceContractPlan`
- `Aion.GameServer.Services.LegionWarehouseJavaStoreCallDescriptor`
- `Aion.GameServer.Tests.LegionWarehousePersistenceContractPlanServiceTests`

## What Changed

- Added disabled contract metadata for future legion warehouse item persistence.
- Preserved the Java distinction:
  - Logout: `InventoryDAO.store(allItems, player.getObjectId(), player.getAccount().getId(), legion.getLegionId())`.
  - Periodic: `InventoryDAO.store(allItems, null, null, legion.getLegionId())`.
- Recorded that both paths combine `getItemsWithKinah()` and deleted items, call `ItemStoneListDAO.save(allItems)` after inventory persistence, and catch/log exceptions.
- Added four focused tests.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehousePersistenceContractPlanServiceTests|FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Result:
  - Passed: 8
  - Failed: 0
  - Skipped: 0
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService` | Service / Contract Planner | Partial | Unit Tested | Needs Verification | C# records Java logout parameter mode and item-stone follow-up but does not persist items. |
| `com.aionemu.gameserver.services.PeriodicSaveService.LegionWarehouseSaveTask` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService` | Scheduled Service / Contract Planner | Partial | Unit Tested | Needs Verification | C# records Java periodic parameter mode with null player/account ids but has no live periodic task. |
| `com.aionemu.gameserver.dao.InventoryDAO.store(List<Item>, Integer, Integer, Integer)` | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService.FutureRepositoryMethod` | Repository / Contract Planner | Partial | Unit Tested | Needs Verification | Future method signature is descriptor-only. No repository method, SQL, transaction behavior, persistent-state mutation, or object-id release behavior was implemented. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.save(List<Item>)` | `Aion.GameServer.Services.LegionWarehousePersistenceContractCriterion.ItemStonePersistenceContractAvailable` | Repository / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the required follow-up persistence; no C# item-stone contract exists. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` legion warehouse save slice | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService` plus `LegionWarehousePersistenceContractPlanService` | Logout Lifecycle / Planner | Partial | Unit Tested | Partial Parity | Adjacent readiness tests still pass. `LeaveWorldAsync` remains unwired. |

## Known Gaps

- Legion warehouse item persistence remains non-live.
- Item-stone persistence remains unmodeled for C# warehouse items.
- Java transaction/autocommit, persistent-state mutation, object-id release, and exception behavior are documented but not implemented.
- No live logout or periodic save hook exists.

## Remaining Risks

- Future live persistence must preserve Java owner-id resolution for legion warehouse items.
- Java `InventoryDAO.store` marks all items updated even after SQL exceptions; this subtle behavior still needs explicit C# handling before live wiring.
- Java transaction behavior commits inside insert/update/delete helpers; C# SQL behavior should be audited before adding repository methods.

## Next Recommended UOW

UOW-2423: Add a disabled item-stone persistence prerequisite/contract plan for legion warehouse items.

Suggested scope:
- Java:
  - `ItemStoneListDAO.save(List<Item>)`
  - `ItemStoneListDAO.store(...)`
  - `InventoryDAO.store(...)` only as the caller-order source
- C#:
  - Add a small non-live planner that records item-stone categories and the fact that legion warehouse persistence needs the item-stone follow-up after inventory persistence.
  - Add focused tests for blank-item no-op, category coverage, and disabled repository wiring.
  - Do not add SQL or live repository interfaces.

## Focused Validation Recipe

- Specific behavior/contract:
  - The disabled item-stone plan should record Java category coverage for mana stones, fusion stones, godstones, and idian stones, and keep live repository wiring disabled.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehouseItemStonePersistencePlanServiceTests|FullyQualifiedName~LegionWarehousePersistenceContractPlanServiceTests" --no-restore`
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review in the test/document notes.
- Broad-validation trigger:
  - none unless the unit adds repository interface methods, SQL execution, shared persistence wiring, or live logout/periodic hooks.

## Safe Candidate UOWs

- UOW-2423 disabled item-stone persistence plan for legion warehouse items.
- Legion bonus fanout readiness report only, if kept non-live and independently tested.
- Group/alliance disconnected-event observer planning, still without live packet dispatch.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Use the exact focused recipe above unless the actual edited surface is narrower.
