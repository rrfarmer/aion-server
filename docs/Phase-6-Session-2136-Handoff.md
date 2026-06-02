# Phase 6 Session 2136 Handoff - FindGroup Shared Singleton Interleaving Fixtures

Date: 2026-06-02
Unit of Work: UOW-2136
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
- `FindGroupConcurrentMutationOrderingReadinessService` now records deterministic shared-singleton interleaving fixtures but still blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2136 Summary

This UOW added deterministic, non-live shared-service interleaving evidence:

- Logout before joined-team cleanup removes `CM_FIND_GROUP`-created player-keyed recruitment/application/instance-group state and joined-team cleanup does not recreate it.
- Joined-team before logout re-adds solo leader recruitment as team-keyed recruitment; logout removes only player-keyed state; disband cleanup removes the team-keyed recruitment.
- Readiness reports now distinguish this focused evidence from still-missing live boundary/runtime interleaving proof.

## Java Artifacts Touched

- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam`
- `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Services.FindGroupClientActionPlanService`
- `Aion.GameServer.Services.FindGroupConcurrentMutationOrderingReadinessService`
- `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`
- `Aion.GameServer.Tests.FindGroupSharedSingletonInterleavingTests`

## Validation In UOW-2136

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSharedSingletonInterleavingTests|FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests" --no-restore
```

Result:

- Passed: 89
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

Full .NET validation was skipped intentionally because this was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this non-live C# fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service | Partial | Unit Tested | Partial Parity | Deterministic shared-service interleaving evidence exists. Live boundary/runtime concurrency remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Complete | Unit Tested | Partial Parity | Logout-before-joined-team fixture confirms player-keyed removal and no resurrection. Live ordering remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Complete | Unit Tested | Partial Parity | Joined-team-before-logout fixture confirms leader team re-add and later disband cleanup. Live ordering remains unverified. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Runtime Service | Partial | Unit Tested | Partial Parity | Shared-service team recruitment removal evidence exists; live runtime/socket behavior remains unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` execution is still disabled.
- No test proves outgoing direct packets are ordered relative to a triggering live `CM_FIND_GROUP` client packet.
- No test proves live action `1`/`5` world-broadcast same-race fanout and opposite-race exclusion.
- No test proves live action `12` invite dispatch behavior.
- No encrypted socket or real-client comparison covers FindGroup.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary readiness evidence for one simple direct-packet action without enabling broad live `CM_FIND_GROUP` dispatch.

Safe candidates:

- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout.
- Add focused action `12` live invite-dispatch failure/result readiness.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2136

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConcurrentMutationOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConcurrentMutationOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSharedSingletonInterleavingTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2136-Completion.md`
- `docs/Phase-6-Session-2136-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2136] Add find group shared singleton interleaving fixtures
```
