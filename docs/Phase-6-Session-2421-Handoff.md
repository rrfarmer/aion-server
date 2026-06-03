# Phase 6 Session 2421 Handoff

## Current Phase

- Phase 6 Java-to-C# parity migration continues on branch `4.8`.
- Latest completed UOW: UOW-2421 legion logout cleanup readiness planner.

## Last Completed UOW

- UOW-2421 added a non-live `PlayerLegionLogoutCleanupReadinessPlanService`.
- The planner records Java legion logout cleanup prerequisites and keeps live logout wiring gated.
- No live persistence, runtime state mutation, or packet fanout was enabled.

## Commits Made

- Pending commit for this handoff: `[Phase 6][UOW-2421] Add legion logout readiness plan`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerLegionLogoutCleanupReadinessPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerLegionLogoutCleanupReadinessPlanServiceTests.cs`
- `docs/Phase-6-Session-2421-Completion.md`
- `docs/Phase-6-Session-2421-Handoff.md`

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService`
- `Aion.GameServer.Services.PlayerLegionLogoutCleanupPrerequisites`
- `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlan`
- `Aion.GameServer.Tests.PlayerLegionLogoutCleanupReadinessPlanServiceTests`

## What Changed

- Added a non-live planner for Java legion logout cleanup readiness.
- Added explicit readiness criteria for warehouse runtime, in-use state, item persistence, item-stone persistence, member runtime, repositories, fanout, and logout hook.
- Added four focused tests.

## Tests Run

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Result:
  - Passed: 4
  - Failed: 0
  - Skipped: 0
- Pre-existing nullable/analyzer warnings remain.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` legion calls | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` plus `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService` | Logout Lifecycle / Planner | Partial | Unit Tested | Partial Parity | Planner records Java ordering and readiness prerequisites, but `LeaveWorldAsync` still does not invoke live legion cleanup. |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService` | Service / Planner | Partial | Unit Tested | Needs Verification | C# models prerequisites only. No warehouse item/deleted item save, kinah item aggregation, or item-stone persistence exists. |
| `com.aionemu.gameserver.services.LegionService.onLogout` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessPlanService` | Service / Planner | Partial | Unit Tested | Needs Verification | C# models prerequisites only. No in-use CAS, member info broadcast, legion/member DAO writes, or bonus fanout exists. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse.unsetInUse` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessCriterion.LegionWarehouseInUseStateAvailable` | Runtime Model / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the missing C# prerequisite; no runtime state implementation exists. |
| `com.aionemu.gameserver.model.team.legion.Legion.removeBonus` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessCriterion.LegionBonusFanoutAvailable` | Runtime Model / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the missing bonus fanout prerequisite; no online-member count or `SM_ICON_INFO` dispatch exists. |
| `com.aionemu.gameserver.dao.LegionDAO.storeLegion` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessCriterion.LegionRepositoryAvailable` | Repository / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the missing repository prerequisite; no C# SQL contract exists. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | `Aion.GameServer.Services.PlayerLegionLogoutCleanupReadinessCriterion.LegionMemberRepositoryAvailable` | Repository / Readiness Criterion | Partial | Unit Tested | Needs Verification | Criterion tracks the missing repository prerequisite; no C# SQL contract exists. |

## Known Gaps

- The new planner is non-live and is not called from `LeaveWorldAsync`.
- No C# legion warehouse runtime, current-user CAS, or warehouse persistence exists.
- No C# item-stone persistence for legion warehouse items exists.
- No C# legion member runtime/repository suitable for Java `onLogout` exists.
- No C# legion bonus runtime or `SM_ICON_INFO` logout fanout exists.

## Remaining Risks

- Java warehouse save error behavior logs and swallows exceptions; C# repository behavior still needs an explicit decision.
- Java warehouse logout save passes player object id/account id/legion id to `InventoryDAO.store`, while periodic save passes null player/account ids; C# must preserve that distinction.
- Live wiring must preserve Java ordering around effect cleanup, online/last-online mutation, member info broadcast, and aggregate player persistence.

## Next Recommended UOW

UOW-2422: Add a disabled legion warehouse persistence contract plan, still non-live.

Suggested scope:
- Java:
  - `LegionService.LegionWhUpdate(Player)`
  - `PeriodicSaveService.LegionWarehouseSaveTask`
  - `InventoryDAO.store(...)` call signatures as used by those methods
  - `ItemStoneListDAO.save(...)` only to capture the required item-stone follow-up
- C#:
  - Add a small planner or contract descriptor in `Aion.GameServer.Services` for the future repository method and Java parameter differences.
  - Add focused tests for logout save versus periodic save parameter modes and exception-policy documentation.
  - Do not add repository interface methods or live SQL yet unless the scope is explicitly narrowed and tested.

## Focused Validation Recipe

- Specific behavior/contract:
  - The disabled contract plan should distinguish Java logout warehouse save parameters from periodic warehouse save parameters and keep live repository wiring disabled until item persistence and item-stone persistence contracts are available.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LegionWarehousePersistenceContractPlanServiceTests|FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Java/Maven:
  - Not expected unless Java source or fixtures change; Java evidence should come from source review in the test/document notes.
- Broad-validation trigger:
  - none unless the unit adds repository interface methods, SQL execution, shared persistence wiring, or live logout hooks.

## Safe Candidate UOWs

- UOW-2422 disabled legion warehouse persistence contract plan.
- Group/alliance disconnected-event observer planning, still without live packet dispatch.
- Legion bonus fanout readiness report only, if kept non-live and independently tested.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, and the latest completion document before work.
- Do not use `docs/PHASE-6-PROGRESS.md` during normal startup.
- Java remains the source of truth.
- Use the exact focused recipe above unless the actual edited surface is narrower.
