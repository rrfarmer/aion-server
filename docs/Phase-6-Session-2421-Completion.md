# Phase 6 Session 2421 Completion

Status: Phase 6 continues; UOW-2421 added a non-live C# readiness planner for Java legion logout cleanup prerequisites. The planner does not execute persistence, mutate runtime legion state, or send packets.

## Scope

- UOW: UOW-2421 legion logout cleanup readiness planner.
- Added a small non-live service that records whether Java legion logout cleanup can be safely wired.
- Added focused tests for no-legion skip, missing prerequisite enumeration, partial prerequisite satisfaction, and all-prerequisite readiness.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `LegionWhUpdate(player)` is called before effect cleanup.
  - `LegionService.onLogout(player)` is called only for legion members after `setOnline(false)` and `setLastOnline(...)`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `LegionWhUpdate(Player)` saves current/deleted legion warehouse items and item stones.
  - `onLogout(Player)` unsets warehouse in-use state, updates/broadcasts member info, stores legion, stores legion member, and removes bonus.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - `unsetInUse(int)` compare-and-sets the current warehouse user to zero.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - `removeBonus()` clears the bonus and sends `SM_ICON_INFO(1, false)` when online member count drops below ten.
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - `storeLegion` updates legion metadata and dominion fields.
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
  - `storeLegionMember` updates nickname, rank, self intro, and challenge score.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerLegionLogoutCleanupReadinessPlanService.cs`
  - New non-live planner added in this unit.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - Existing logout service remains unchanged; no live legion hook was added.
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
  - Existing repository remains unchanged; no legion repository contract was added.

## Implemented

- Added `PlayerLegionLogoutCleanupReadinessPlanService.CreatePlan(Player?, PlayerLegionLogoutCleanupPrerequisites)`.
- Added `PlayerLegionLogoutCleanupPrerequisites` for the modeled prerequisites:
  - legion warehouse runtime
  - warehouse in-use state
  - warehouse item persistence
  - item-stone persistence
  - legion member runtime
  - legion repository
  - legion member repository
  - legion member info fanout
  - legion bonus fanout
  - logout hook
- Added `PlayerLegionLogoutCleanupReadinessPlan` with status, missing criteria, Java source note, C# evidence note, and `IsLive = false`.
- Added `PlayerLegionLogoutCleanupReadinessPlanServiceTests`.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `CreatePlan_NoLegionPlayer_SkipsJavaLegionCleanup` | Unit | Java `LegionWhUpdate` null-legion return and `PlayerLeaveWorldService` member guard | No-legion C# player produces skipped, non-live plan. | Focused C# assertion based on Java source review. | Does not prove live logout behavior. |
| `CreatePlan_LegionMemberWithNoPrerequisites_EnumeratesAllBlockers` | Unit | Java `LegionWhUpdate` and `LegionService.onLogout` source review | Legion member without modeled prerequisites stays not ready and lists every blocker. | Focused C# assertion based on Java source review. | No live repositories or packets. |
| `CreatePlan_PartialPrerequisites_RemoveOnlySatisfiedBlockers` | Unit | Java prerequisite list from reviewed methods | Satisfied prerequisites are removed while remaining Java-required surfaces still block live wiring. | Focused C# assertion. | Planner only. |
| `CreatePlan_AllPrerequisitesReady_MarksReadyButStaysNonLive` | Unit | Java prerequisite list from reviewed methods | All modeled prerequisites mark the plan ready for future wiring, but still non-live. | Focused C# assertion. | Does not execute wiring. |

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

## Validation Decision

- Changed surface: one non-live C# planner service plus focused tests.
- Specific behavior/contract: Java legion logout cleanup remains blocked until all modeled warehouse runtime, member runtime, repository, item-stone, fanout, and logout hook prerequisites are available.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerLegionLogoutCleanupReadinessPlanServiceTests" --no-restore`
- Result:
  - Passed: 4
  - Failed: 0
  - Skipped: 0
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled affected project/dependencies and covered the new non-live planner.
- Why this scope is sufficient:
  - The unit changed only a standalone non-live planner and its test class, with no shared repository contract, live dispatch, packet primitive, or scheduler change.

## Known Remaining Gaps

- No live legion logout hook.
- No C# legion warehouse runtime or current-user state.
- No C# legion warehouse item/deleted item persistence.
- No C# item-stone persistence for legion warehouse items.
- No C# legion member runtime suitable for logout updates.
- No C# legion or legion-member repository contracts.
- No C# legion member info broadcast or bonus removal fanout.

## Summary Metrics

- Java artifacts reviewed: 6.
- C# artifacts reviewed: 3.
- Production files changed: 1.
- Test files changed: 1.
- New focused tests: 4.
- Focused validation commands passed: 1.
- Artifacts with verified parity: 0.
- Artifacts needing verification: 7.
- Blocked artifacts: 0.
- Estimated Phase 6 completion: unchanged, still conservatively partial.
