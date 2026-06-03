# Phase 6 Session 2423 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2423 disabled item-stone persistence plan.

## Last Completed UOW

- UOW-2423 added `LegionWarehouseItemStonePersistencePlanService`.
- The planner records Java item-stone category coverage, enum ordinals, save order, blank-list no-op behavior, and repository blockers.
- No repository method, SQL, live logout hook, periodic task, or packet fanout was enabled.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2423] Add legion warehouse item-stone plan`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/LegionWarehouseItemStonePersistencePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/LegionWarehouseItemStonePersistencePlanServiceTests.cs`
- `docs/Phase-6-Session-2423-Completion.md`
- `docs/Phase-6-Session-2423-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/dao/ItemStoneListDAO.java`
- `game-server/src/com/aionemu/gameserver/model/items/ItemStone.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.LegionWarehouseItemStonePersistencePlanService`
- `Aion.GameServer.Services.LegionWarehouseItemStonePersistencePlan`
- `Aion.GameServer.Services.LegionWarehouseItemStoneCategoryPlan`
- `Aion.GameServer.Tests.LegionWarehouseItemStonePersistencePlanServiceTests`

## What Changed

- Added disabled item-stone persistence metadata for future legion warehouse persistence.
- Preserved Java category facts:
  - Enum ordinals: `MANASTONE=0`, `GODSTONE=1`, `FUSIONSTONE=2`, `IDIANSTONE=3`.
  - Save order: `MANASTONE`, `FUSIONSTONE`, `GODSTONE`, `IDIANSTONE`.
- Recorded blank item-list no-op and disabled repository blockers.
- Added four focused tests.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehouseItemStonePersistencePlanServiceTests|FullyQualifiedName~LegionWarehousePersistenceContractPlanServiceTests" --no-restore`
- Result:
  - Passed: 8
  - Failed: 0
  - Skipped: 0
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.save(List<Item>)` | `Aion.GameServer.Services.LegionWarehouseItemStonePersistencePlanService` | Repository / Planner | Partial | Unit Tested | Needs Verification | C# records blank-list guard, category extraction, save order, and blockers. It does not persist item stones. |
| `com.aionemu.gameserver.dao.ItemStoneListDAO.store(Set<? extends ItemStone>, ItemStoneType)` | `Aion.GameServer.Services.LegionWarehouseItemStonePersistencePlanService` | Repository / Planner | Partial | Unit Tested | Needs Verification | C# records repository/SQL/persistent-state prerequisites only. It does not implement delete/add/update SQL or state mutation. |
| `com.aionemu.gameserver.model.items.ItemStone.ItemStoneType` | `Aion.GameServer.Services.LegionWarehouseItemStoneCategoryPlan` | Enum / DTO | Partial | Unit Tested | Needs Verification | C# records Java ordinals for the four categories; no runtime enum mapping or serialization exists. |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` item-stone follow-up | `Aion.GameServer.Services.LegionWarehousePersistenceContractPlanService` plus `LegionWarehouseItemStonePersistencePlanService` | Service / Planner | Partial | Unit Tested | Needs Verification | Adjacent warehouse persistence tests still pass. No live warehouse save calls the item-stone plan. |

## Known Gaps

- Item-stone persistence remains non-live.
- No item-stone SQL or repository method exists.
- Persistent-state mutation after store attempts is not implemented.
- No live legion warehouse persistence caller exists.

## Remaining Risks

- Java `store` opens a connection per category and marks stones updated even after logged SQL exceptions; C# must decide whether to preserve that exact behavior before live wiring.
- Java save order differs from enum ordinal order for godstone/fusionstone; future SQL or DTO work must not assume ordinal order is save order.
- This planner is useful only if it leads to concrete repository or runtime work; avoid expanding report chains without unlocking a runtime step.

## Next Recommended UOW

UOW-2424: Choose the next concrete parity step from one of these safe candidates:

- Legion bonus fanout readiness report only if it directly maps Java `Legion.removeBonus()` to a future runtime prerequisite.
- Group/alliance disconnected-event observer planning, still without live packet dispatch.
- A focused audit of C# repository support needed for a future legion warehouse item save, but do not add interface methods unless SQL behavior and tests are scoped.

Recommended first choice:
- Group/alliance disconnected-event observer planning, because the legion warehouse path now has several non-live prerequisites recorded and risks becoming report-only unless a repository implementation is ready.

## Focused Validation Recipe

- If choosing group/alliance observer planning:
  - Specific behavior/contract: Java logout disconnected-event fanout remains unwired; the planner should record required packet/runtime prerequisites without live dispatch.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupDisconnectedPlannerTests|FullyQualifiedName~PlayerAllianceDisconnectedPlannerTests" --no-restore`
- If choosing legion bonus readiness:
  - Specific behavior/contract: Java `Legion.removeBonus()` clears bonus only below ten online members and sends `SM_ICON_INFO(1, false)` to remaining online members.
  - Focused C# command:
    - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionBonusLogoutFanoutReadinessServiceTests" --no-restore`
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review in the test/document notes.
- Broad-validation trigger:
  - none unless the unit adds repository interface methods, SQL execution, shared runtime wiring, or live logout/periodic hooks.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Prefer concrete runtime-enabling work over additional metadata unless it removes a specific blocker.
