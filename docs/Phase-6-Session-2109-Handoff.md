# Phase 6 Session 2109 Handoff - FindGroup Joined-Team Mutation Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2109
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

Before running expensive commands, record:

- Changed surface.
- Exact focused C#, Java/Maven, or hygiene command.
- Java/Maven command or the reason it is unavailable/not relevant.
- Broad-validation trigger, or `none`.
- Broad .NET suite/build decision.

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Production DI registers the FindGroup singleton graph through `AddFindGroupSingletonGraph`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` now has focused evidence for the Java priority where leader solo recruitment re-add skips the full-team removal branch.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.
- UOW-2106: logout cleanup now uses the injected shared FindGroup service without requiring observer activation.
- UOW-2107: active docs now require focused validation decisions before expensive broad .NET commands.
- UOW-2108: `GameServerConnection` can consume injected non-live FindGroup composition/adapter services to create disabled boundary plans.
- UOW-2109: focused `onJoinedTeam` mutation-priority evidence added for leader solo-recruitment re-add over full-team removal.

## Current UOW Commit Message

- `[Phase 6][UOW-2109] Add find group joined-team ordering evidence`

## Validation In UOW-2109

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests" --no-restore`
  - Final result: 36 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. No focused Java test target was identified for `FindGroupService.onJoinedTeam`; this UOW used reviewed Java source and focused C# evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not change live dispatch, packet primitives, crypto, persistence schema, scheduling, broad world-state behavior, or production mutation logic.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Partial | Unit Tested | Partial Parity | New focused test covers Java branch priority when a leader's removed solo recruitment is re-added as team recruitment before the full-team removal branch can run. Live dispatch, Java runtime comparison, and concurrency remain unverified. |
| `com.aionemu.gameserver.model.team.TemporaryPlayerTeam.isLeader/isFull` | `FindGroupJoinedTeamLifecycleRecorder`; `PlayerGroupRuntime`; `PlayerAllianceRuntime`; `FindGroupRecruitmentSubject` | Team Facts / Lifecycle | Partial | Unit Tested | Partial Parity | C# supplies leader/full facts into `OnJoinedTeam`; combined Java branch priority is now covered at planner level. Runtime event ordering relative to live packet fanout remains unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupRecruitmentPlanService.OnJoinedTeam` still lacks Java runtime comparison and concurrency stress evidence.
- Direct packet sends, race-filtered world broadcasts, action 11/12 side effects, socket-level order, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2109 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add connection-registry ordering audit evidence for future direct sends and race-filtered world broadcasts before any live `ProcessPacketAsync` call.

Safe alternative candidates:

- Add a disabled action 12 connection-helper test using the connection resolver and injected group/alliance runtimes.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.

## Files Changed In UOW-2109

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2109-Completion.md`
- `docs/Phase-6-Session-2109-Handoff.md`
