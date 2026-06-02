# Phase 6 Session 2106 Handoff - FindGroup Logout Singleton Cleanup Evidence

Date: 2026-06-02
Unit of Work: UOW-2106
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

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Filtered `dotnet test` commands already build affected projects and dependencies, so a full solution build needs its own documented broad-validation trigger.

Use the narrowest command that proves the scoped change.

Avoid broad .NET commands unless a broad-validation trigger is documented first.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Production DI registers the FindGroup singleton graph through `AddFindGroupSingletonGraph`.
- Logout, joined-team, and disband lifecycle callers now have production singleton graph evidence.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but still not consumed by `GameServerConnection`.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.
- UOW-2106: logout cleanup now uses the injected shared FindGroup service without requiring observer activation.

## Current UOW Commit Message

- `[Phase 6][UOW-2106] Add find group logout singleton cleanup evidence`

## Validation In UOW-2106

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - Final result: 49 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW added C# logout singleton behavior from reviewed Java logout call sites.
- Broad .NET validation was skipped:
  - The UOW did not enable live `CM_FIND_GROUP`, live packet sends, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | C# now runs injected FindGroup logout cleanup before pending question denial without requiring observer activation. Broader Java leave-world side effects remain outside this FindGroup slice. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Partial | Unit Tested | Partial Parity | Removes recruitment/application/instance-group state keyed by player id and dispatches no live FindGroup packets. Java runtime comparison and concurrency stress remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.denyAll` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.DenyAll`; `PlayerEnterWorldService.ClearPendingQuestionResponsesAsync` | Request Registry / Logout Order | Partial | Unit Tested | Partial Parity | Existing and updated tests prove FindGroup cleanup observes pending responses before C# denial clears them. Full Java handler callback identity/reflection behavior remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph`; `PlayerEnterWorldService`; `FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Logout, joined-team, and disband lifecycle callers now have production singleton graph evidence. Live `CM_FIND_GROUP` boundary still does not consume the shared service. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but not consumed by `GameServerConnection`.
- Socket-level order, real-client behavior, Java runtime packet traces, race-filtered world fanout, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2106 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add a non-live `GameServerConnection` adapter-consumer slice proving how the connection could compose `CmFindGroup` through `FindGroupConnectionBoundaryDispatchAdapterService` without executing live sends.

Safe alternative candidates:

- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
- Add focused Java/Maven parity fixture for one FindGroup branch if an executable Java test target can be identified.
- Add a connection-registry ordering audit plan for future live direct sends and race-filtered world broadcasts.

## Files Changed In UOW-2106

- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2106-Completion.md`
- `docs/Phase-6-Session-2106-Handoff.md`
