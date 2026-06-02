# Phase 6 Session 2109 Completion - FindGroup Joined-Team Mutation Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2109
Status: Completed

## Scope

- Added focused C# evidence for a Java `FindGroupService.onJoinedTeam` multi-step mutation priority.
- Covered the combined branch where instance-group cleanup, application cleanup, solo recruitment removal, and leader re-add happen together.
- Verified that leader solo-recruitment re-add takes priority over full-team recruitment removal, matching Java's `if (recruitment != null && team.isLeader(player)) ... else if (team.isFull()) ...` order.
- Updated readiness/design evidence without enabling live `CM_FIND_GROUP` dispatch.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onJoinedTeam(Player)` reads/removes the player's instance-group registration first when min members are reached.
  - Calls `removeApplication(player)`.
  - Calls `removeRecruitment(player.getObjectId(), GAMESERVER_ID, 0, 0, 16)`.
  - If that solo recruitment existed and the player is team leader, calls `addRecruitment(player, oldMessage, oldGroupType)`.
  - Otherwise, if the team is full, removes the team recruitment with unknown bytes `0,0,0`.

## What Changed

- Added `FindGroupRecruitmentPlanServiceTests.OnJoinedTeam_LeaderSoloRecruitmentReaddTakesPriorityOverFullTeamRemovalLikeJava`.
- Updated `FindGroupLiveDispatchReadinessReportService` and its tests to record the new ordering evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` with the new non-live ordering evidence.

## Validation

- Changed surface:
  - Test plus readiness/design evidence text.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests" --no-restore`
  - Final result: passed, 36 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed; no focused Java test target was identified for `FindGroupService.onJoinedTeam`. This UOW used reviewed Java source and focused C# evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not change live dispatch, packet primitives, crypto, persistence schema, scheduling, broad world-state behavior, or production mutation logic. Filtered tests built the affected project and covered the scoped ordering evidence.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Partial | Unit Tested | Partial Parity | New focused test covers Java branch priority when a leader's removed solo recruitment is re-added as team recruitment before the full-team removal branch can run. Live dispatch, Java runtime comparison, and concurrency remain unverified. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam.isLeader/isFull` | `FindGroupJoinedTeamLifecycleRecorder`; `PlayerGroupRuntime`; `PlayerAllianceRuntime`; `FindGroupRecruitmentSubject` | Team Facts / Lifecycle | Partial | Unit Tested | Partial Parity | C# supplies leader/full facts into `OnJoinedTeam`; combined Java branch priority is now covered at planner level. Runtime event ordering relative to live packet fanout remains unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.OnJoinedTeam_LeaderSoloRecruitmentReaddTakesPriorityOverFullTeamRemovalLikeJava` | Unit | Java `FindGroupService.onJoinedTeam` source review | Instance-group cleanup, application cleanup, solo recruitment removal, and leader team re-add occur together; full-team removal is skipped when solo recruitment existed and player is leader | Focused C# unit test plus reviewed Java source | No Java runtime trace; no concurrent mutation stress; no live packet dispatch |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_RecordsLifecycleObserverEvidenceWithoutMarkingLiveDispatchReady` | Unit | Java `FindGroupService.onJoinedTeam` source review | Readiness report includes the new mutation-priority evidence while keeping live dispatch blocked | Focused C# unit test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` still lacks Java runtime comparison and concurrency stress evidence.
- Direct packet sends, race-filtered world broadcasts, action 11/12 side effects, socket-level order, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2109-Completion.md`
- `docs/Phase-6-Session-2109-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add connection-registry ordering audit evidence for future direct sends and race-filtered world broadcasts before any live `ProcessPacketAsync` call.

Safe alternative candidates:

- Add a disabled action 12 connection-helper test using the connection resolver and injected group/alliance runtimes.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
