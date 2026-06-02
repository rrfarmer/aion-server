# Phase 6 Session 2135 Handoff - FindGroup Concurrent Mutation Readiness

Date: 2026-06-02
Unit of Work: UOW-2135
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite or full solution build unless a documented broad-validation trigger applies. Each completion/handoff should record the exact focused command and the reason broad validation was skipped or run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live boundary plans but is not invoked live.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct/broadcast execution order, but not live boundary execution.
- `FindGroupDirectPacketTriggerOrderingReadinessService` blocks live direct-packet ordering claims.
- `FindGroupWorldBroadcastFanoutReadinessService` blocks live world-broadcast fanout claims for actions `1` and `5`.
- `FindGroupConcurrentMutationOrderingReadinessService` now blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2135 Summary

This UOW reviewed Java `FindGroupService` concurrent state shape and `onJoinedTeam` method order:

- Java stores recruitments, applications, and instance groups in independent `ConcurrentHashMap` instances.
- Java `onJoinedTeam` reads/removes instance groups, removes applications, removes solo recruitment with `unknown3=16`, then either re-adds leader recruitment or removes full-team recruitment.
- C# has independent `ConcurrentDictionary` stores and focused sequential/basic concurrent tests, but still lacks live singleton caller interleaving evidence.

New C# readiness evidence separates available map-shape/method-order evidence from missing live interleaving proof.

## Validation In UOW-2135

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupJoinedTeamLifecycleRecorderTests" --no-restore
```

Result:

- Passed: 42
- Failed: 0
- Skipped: 0

Full .NET validation was skipped intentionally because this was a readiness-report/docs unit with adjacent focused tests and no live dispatch or shared packet primitive change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this C# readiness-report surface.

## Files Changed In UOW-2135

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConcurrentMutationOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConcurrentMutationOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2135-Completion.md`
- `docs/Phase-6-Session-2135-Handoff.md`

## Next Recommended Unit of Work

Next sequential task:

- Add one focused, non-live or connection-adjacent interleaving test fixture for shared `FindGroupRecruitmentPlanService` callers before live dispatch.

Safe candidates:

- Add focused live-boundary readiness evidence for one simple direct-packet action.
- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout.
- Add focused action `12` live invite-dispatch failure/result readiness.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2135] Add find group concurrent mutation readiness
```
