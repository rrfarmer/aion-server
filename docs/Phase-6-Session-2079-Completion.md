# Phase 6 Session 2079 Completion - Find Group Logout Cleanup Observer

Date: 2026-06-01
Unit of Work: UOW-2079
Status: Completed

## Scope

- Added observer-only find-group logout cleanup composition to the C# leave-world path.
- Preserved Java logout call order for the represented slice: `FindGroupService.onLogout(player)` before `ResponseRequester.denyAll()`.
- Kept live `CM_FIND_GROUP` dispatch and packet side effects disabled.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld` calls `FindGroupService.getInstance().onLogout(player)` before `player.getResponseRequester().denyAll()`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onLogout` removes entries keyed by `player.getObjectId()` from `recruitments`, `applications`, and `instanceGroups`.
  - It sends no packets.

## What Changed

- Extended `PlayerEnterWorldService` with optional constructor dependencies:
  - `FindGroupRecruitmentPlanService? findGroupService`
  - `Action<FindGroupLogoutCleanupPlan>? findGroupLogoutCleanupPlanObserver`
- Added `RecordFindGroupLogoutCleanup(player)` before pending question-response denial in `LeaveWorldAsync`.
- Added focused test coverage proving:
  - seeded find-group recruitment/application/instance-group entries are removed by the disabled plan;
  - no direct packet intents or live side effects are emitted;
  - the observer runs while `ResponseRequester` still contains the pending request, matching Java order before `denyAll`.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests" --no-restore`
  - Result: passed, 69 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no narrow `PlayerLeaveWorldService`/logout Java unit test exists under `game-server/src/test` in this workspace. Java evidence for this UOW is source review of `PlayerLeaveWorldService.leaveWorld` and `FindGroupService.onLogout`.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: change is an optional observer-only logout plan hook plus focused tests. It does not enable live packet dispatch, change packet primitives, change persistence, mutate shared world state beyond existing logout behavior, or introduce broad side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` find-group slice | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onLogout` cleanup before question denial, matching Java call order for this slice. Broader leave-world order and live side effects remain partial. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout`; `FindGroupLogoutCleanupPlan` | Service Cleanup / Plan | Partial | Unit Tested | Partial Parity | Removes player-object-id keyed recruitment, application, and instance-group entries without packets. Existing tests also show team-keyed recruitment is not removed by player logout. Live singleton wiring remains observer-only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerEnterWorldServiceTests.LeaveWorld_RecordsFindGroupLogoutCleanupBeforeQuestionDenyLikeJava` | Unit | Java `PlayerLeaveWorldService.leaveWorld` and `FindGroupService.onLogout` source review | C# leave-world records find-group cleanup before `ResponseRequester` is denied and emits no live find-group side effects | Focused C# unit test | Does not execute Java runtime, live packet sends, or full logout workflow |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Full Java leave-world ordering is still broader than this slice.
- Direct packet sends, world broadcasts, group/alliance invite execution, encrypted socket behavior, real-client behavior, and service concurrency remain unverified for find-group.
- `FindGroupService.onJoinedTeam` lifecycle wiring remains a separate candidate.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2079-Completion.md`
- `docs/Phase-6-Session-2079-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect `FindGroupService.onJoinedTeam` live call sites and compare them to existing C# team/group lifecycle surfaces before deciding whether an observer-only disabled hook can be safely wired.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the new readiness report to define the final pre-live checklist.
