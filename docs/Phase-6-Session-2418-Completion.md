# Phase 6 Session 2418 Completion

Status: Phase 6 continues; C# logout now updates the represented group member last-online timestamp through the shared `PlayerGroupRuntime`, covering the narrow Java `PlayerGroupService.onPlayerLogout` timestamp mutation. Alliance disconnected fanout and legion warehouse/member logout cleanup remain explicit gaps.

## Scope

- UOW: UOW-2418 leave-world group/alliance/legion logout cleanup ordering.
- Implemented the smallest safe modeled slice: Java group member `lastOnlineTime` update on logout.
- Kept alliance and legion cleanup as source-reviewed gaps because their exact runtime/repository surfaces are not ready for a small live hook.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Calls `PlayerGroupService.onPlayerLogout(player)` and `PlayerAllianceService.onPlayerLogout(player)` after the immediate logout persistence band.
  - Calls `LegionService.getInstance().LegionWhUpdate(player)` before effect/task cleanup.
  - Calls `LegionService.getInstance().onLogout(player)` after setting the player offline and last-online data, guarded by `player.isLegionMember()`.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `onPlayerLogout` resolves `player.getPlayerGroup()`, updates `PlayerGroupMember.lastOnlineTime`, then fires `PlayerDisconnectedEvent`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `onPlayerLogout` resolves `player.getPlayerAlliance()`, updates `PlayerAllianceMember.lastOnlineTime`, then fires `PlayerDisconnectedEvent`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `LegionWhUpdate` stores legion warehouse items plus deleted items with `InventoryDAO.store(...)` and `ItemStoneListDAO.save(...)`.
  - `onLogout` unsets legion warehouse in-use state, updates member info, stores legion/member rows, and removes legion bonus.

## C# Surface Reviewed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
  - Existing logout flow had find-group cleanup, pending question denial, repurchase cleanup, world removal, and repository save.
  - New optional `PlayerGroupRuntime` hook updates grouped member last-online after the modeled persistence band.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
  - Already exposed `UpdateMemberLastOnlineTime(Player, DateTimeOffset)` with Java source breadcrumbs.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
  - Has alliance membership, leave, leader, ready, brand, and league support, but no exact logout last-online runtime method yet.
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceDisconnectedPlanner.cs`
  - Plans disconnected packet fanout from supplied snapshots, but is not wired into `LeaveWorldAsync`.
- Legion-related C# surfaces
  - Packet and item-storage slices exist for legion warehouse behavior, but no modeled `LegionService.LegionWhUpdate` or `LegionService.onLogout` repository/service contract exists.

## Implemented

- Added optional `PlayerGroupRuntime?` dependency to `PlayerEnterWorldService`.
- Added `RecordGroupLogoutLastOnline(...)` and call it from `LeaveWorldAsync` using the same `lastOnline` timestamp sent to logout persistence.
- Added `LeaveWorld_UpdatesGroupedMemberLastOnlineAfterPersistenceBandLikeJavaLogout`.

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `LeaveWorld_UpdatesGroupedMemberLastOnlineAfterPersistenceBandLikeJavaLogout` | Regression | Java `PlayerLeaveWorldService.leaveWorld` and `PlayerGroupService.onPlayerLogout` source review | A grouped logging-out player gets the runtime member last-online timestamp updated from the logout timestamp; unrelated group members are unchanged. | Focused C# service-boundary/runtime test. | Does not fire Java `PlayerDisconnectedEvent`; C# updates after the single C# logout repository save, while Java places this after the immediate persistence band but before final `PlayerService.storePlayer`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` group logout slice | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Regression Tested | Partial Parity | C# now updates represented group member last-online through `PlayerGroupRuntime`. Java disconnected event fanout is still not dispatched from logout. C# placement is after the aggregate repository save, not between Java's individual DAO band and final `PlayerService.storePlayer`. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.onPlayerLogout` | `Aion.GameServer.Services.PlayerGroupRuntime.UpdateMemberLastOnlineTime` | Runtime Service | Partial | Unit/Regression Tested | Partial Parity | Last-online mutation is covered. `PlayerDisconnectedEvent` packet/system-message fanout, leader handling, offline member behavior, and Java runtime comparison remain unverified. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.onPlayerLogout` | `PlayerAllianceRuntime`; `PlayerAllianceDisconnectedPlanner` | Runtime/Planner | Partial | Unit Tested for disconnected planner only | Needs Verification | C# can plan supplied-snapshot disconnected fanout, but logout does not update alliance member last-online or invoke the planner. Runtime lacks an exact `UpdateMemberLastOnlineTime` equivalent. |
| `com.aionemu.gameserver.services.LegionService.LegionWhUpdate` | No exact C# equivalent | Service/Repository | Not Started | No Tests | Needs Verification | Legion warehouse item/deleted-item persistence, stone save, exception behavior, owner/account/legion ids, and transaction behavior are unmodeled. |
| `com.aionemu.gameserver.services.LegionService.onLogout` | No exact C# equivalent | Service/Repository | Not Started | No Tests | Needs Verification | Warehouse in-use release, member info update, legion/member persistence, bonus removal, packets, and ordering after online/last-online updates remain unmodeled. |

## Validation Decision

- Changed surface: C# service hook plus focused service-boundary test.
- Specific behavior/contract: Java group logout updates the grouped member last-online timestamp.
- Focused C# command:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests" --no-restore`
- Result:
  - First run failed on local test compile issues (`CreatePlayer` argument and nullable `LogoutLastOnline` handling).
  - Second run failed on init-only teammate identity assignment.
  - Final run passed: 92 passed, 0 failed, 0 skipped.
- Focused Java/Maven command:
  - Not run; no Java source or fixture changed, and Java evidence was source review.
- Broad-validation trigger: none.
- Broad .NET decision:
  - Skipped; filtered test compiled the affected project and directly covered the edited logout/runtime behavior.

## Known Remaining Gaps

- C# logout still does not fire group `PlayerDisconnectedEvent` packet/system-message fanout.
- Alliance logout last-online mutation is not wired and lacks a direct runtime method.
- Alliance disconnected fanout planner remains supplied-snapshot only and not live logout dispatch.
- Legion warehouse save and legion member logout cleanup are not modeled.
- Exact Java ordering is only partial because C# aggregates much of logout persistence inside `SavePlayerLogoutAsync`.

## Summary Metrics

- Java artifacts reviewed: 4.
- C# artifacts reviewed: 5.
- Production files changed: 1.
- Test files changed: 1.
- New focused tests: 1.
- Focused validation commands completed: 1 passing final command after 2 local compile-fix reruns.
