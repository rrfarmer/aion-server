# Phase 6 Session 2135 Completion - FindGroup Concurrent Mutation Readiness

Date: 2026-06-02
Unit of Work: UOW-2135
Status: Completed

## Scope

This unit reviewed Java `FindGroupService` multi-step mutation behavior before live C# `CM_FIND_GROUP` dispatch, with Java remaining the source of truth.

The reviewed Java surface was:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Independent `ConcurrentHashMap` stores for recruitments, applications, and instance groups.
- `onJoinedTeam` ordering: instance-group removal, application removal, solo recruitment removal with `unknown3=16`, then leader re-add or full-team recruitment removal.

The C# port still must not claim live concurrency parity from `ConcurrentDictionary` store shape alone.

## Changes

- Added `FindGroupConcurrentMutationOrderingReadinessService`.
- Added focused tests for the readiness report.
- Wired the concurrent mutation blocker and observer evidence into `FindGroupLiveDispatchReadinessReportService`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the completed review is recorded and the next candidate no longer points at already-completed work.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupJoinedTeamLifecycleRecorderTests" --no-restore
```

Result:

- Passed: 42
- Failed: 0
- Skipped: 0

Full .NET validation:

- Skipped intentionally. This was a readiness-report and documentation unit plus adjacent focused service tests; no live dispatch, common packet primitive, shared runtime base, or solution-wide contract changed.
- Filtered `dotnet test` compiled the affected project and directly adjacent test surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW reviewed Java `FindGroupService` as source evidence for a C# readiness-report surface. No focused Java test target was identified for this documentation/reporting change.

## Parity Table

| Surface | Java Source | C# Evidence | Status |
| --- | --- | --- | --- |
| FindGroup state store shape | Independent `ConcurrentHashMap` stores | Independent `ConcurrentDictionary` stores already tested in focused C# state tests | Evidence available, not live proof |
| `onJoinedTeam` method order | Remove instance group, remove application, remove solo recruitment, then re-add leader recruitment or remove full team | Existing focused sequential C# tests plus new readiness evidence | Evidence available |
| Live singleton caller interleaving | Java singleton can be called by `CM_FIND_GROUP`, logout, joined-team, and disband paths | No focused C# live interleaving trace yet | Blocked |
| Runtime/socket comparison | Java live server behavior | No C# live socket/runtime comparison | Blocked |

## Remaining Blockers

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- C# has no objective evidence proving Java-equivalent interleavings between live `CM_FIND_GROUP` actions, logout cleanup, joined-team cleanup, and group/alliance disband cleanup on one singleton service.
- Direct-packet and world-broadcast ordering still need live connection-boundary proof.
- Action `12` invite dispatch still needs live boundary evidence before enabling.
- Real encrypted socket or real-client behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add one focused, non-live or connection-adjacent interleaving test fixture for shared `FindGroupRecruitmentPlanService` callers before live dispatch.

Safe candidates:

- Add focused live-boundary readiness evidence for one simple direct-packet action.
- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout.
- Add focused action `12` live invite-dispatch failure/result readiness.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConcurrentMutationOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConcurrentMutationOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2135-Completion.md`
- `docs/Phase-6-Session-2135-Handoff.md`
