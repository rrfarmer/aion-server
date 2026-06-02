# Phase 6 Session 2105 Handoff - FindGroup Production Singleton Graph Evidence

Date: 2026-06-02
Unit of Work: UOW-2105
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
- Production DI now registers the FindGroup singleton graph through `AddFindGroupSingletonGraph`.
- Focused tests prove DI-resolved group/alliance runtimes and invite services share one `FindGroupRecruitmentPlanService` for joined-team and disband planning.
- Logout cleanup remains observer-only and is the next lifecycle singleton gap.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.

## Current UOW Commit Message

- `[Phase 6][UOW-2105] Add find group production singleton graph`

## Validation In UOW-2105

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - Final result: 10 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW added C# DI graph evidence for reviewed Java singleton call sites.
- Broad .NET validation was skipped:
  - The UOW did not enable live `CM_FIND_GROUP`, live packet sends, packet primitives, crypto, persistence, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph`; `FindGroupRecruitmentPlanService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Production DI now creates one shared FindGroup service graph for joined-team and disband callers. Live `CM_FIND_GROUP` and logout cleanup still are not proven against the graph. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime` registered through `AddFindGroupSingletonGraph` | Team Lifecycle | Partial | Unit Tested | Partial Parity | DI-resolved group runtime removes team-keyed recruitment from the shared FindGroup service during disband planning. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime` registered through `AddFindGroupSingletonGraph` | Team Lifecycle | Partial | Unit Tested | Partial Parity | DI-resolved alliance runtime removes alliance-keyed recruitment from the shared FindGroup service during disband planning. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundaryDispatchAdapterService` registered through `AddFindGroupSingletonGraph` | Client Packet Boundary | Partial | Unit Tested for DI registration only | Needs Verification | Non-live planner/adapter services are registered in the graph, but `GameServerConnection` still defers `CmFindGroup` and does not invoke them. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Logout cleanup remains observer-only; production DI registration alone does not make it normal live singleton behavior.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but not consumed by `GameServerConnection`.
- Socket-level ordering, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2105 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add logout cleanup singleton wiring evidence so `PlayerEnterWorldService.LeaveWorldAsync` can use the shared `FindGroupRecruitmentPlanService` without requiring an observer-only test hook, while keeping packet sends disabled.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live `FindGroupConnectionBoundaryDispatchAdapterService` without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
- Add focused Java/Maven parity fixture for one FindGroup branch if an executable Java test target can be identified.

## Files Changed In UOW-2105

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2105-Completion.md`
- `docs/Phase-6-Session-2105-Handoff.md`
