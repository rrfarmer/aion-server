# Phase 6 Session 2104 Handoff - FindGroup Injected Connection Wiring Evidence

Date: 2026-06-02
Unit of Work: UOW-2104
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
- `GameServerConnection` and `GameClientSocketServer` can now consume injected group/alliance invite services.
- Focused connection tests prove injected invite services with a shared `FindGroupJoinedTeamLifecycleRecorder` can mutate the same `FindGroupRecruitmentPlanService` during group/alliance invite acceptance.
- `FindGroupLifecycleSingletonWiringReadinessService` still blocks live singleton readiness.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.

## Validation In UOW-2104

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionGroupInviteTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests" --no-restore`
  - Final result: 17 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW added C# injected wiring evidence from reviewed Java joined-team call sites and existing `FindGroupService.onJoinedTeam` behavior.
- Broad .NET validation was skipped:
  - The UOW did not enable `CM_FIND_GROUP`, packet primitives, crypto, persistence, scheduling, production DI registration, or live FindGroup packet dispatch.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerGroupService.addPlayerToGroup` plus `FindGroupService.onJoinedTeam` | `GameServerConnection` injected `PlayerGroupInviteRequestService` plus `FindGroupJoinedTeamLifecycleRecorder` | Team Lifecycle / Connection Wiring | Partial | Focused Connection Tested | Partial Parity | Group invite acceptance can use an injected shared FindGroup service to move a leader's solo recruitment to team recruitment. Production DI singleton registration and live `CM_FIND_GROUP` remain blocked. |
| `PlayerAllianceService.addPlayerToAlliance` plus `FindGroupService.onJoinedTeam` | `GameServerConnection` injected `PlayerAllianceInviteRequestService` plus `FindGroupJoinedTeamLifecycleRecorder` | Team Lifecycle / Connection Wiring | Partial | Focused Connection Tested | Partial Parity | Alliance invite acceptance can use an injected shared FindGroup service to move a leader's solo recruitment to alliance recruitment. Production DI singleton registration and live `CM_FIND_GROUP` remain blocked. |
| `FindGroupService.SingletonHolder` lifecycle | `FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Needs Verification | Group/alliance joined-team call sites now have injected connection evidence. Overall singleton readiness remains blocked by logout cleanup, `CM_FIND_GROUP`, production DI registration, and cross-caller runtime proof. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Production DI does not yet register one shared `FindGroupRecruitmentPlanService`, recorder, runtimes, invite services, logout service, and future boundary adapter as a proven singleton graph.
- Logout cleanup remains observer-only and is not normal live singleton behavior.
- Group/alliance disband cleanup remains non-live opt-in planning evidence.
- Socket-level ordering, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2104 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add production DI singleton graph evidence for `FindGroupRecruitmentPlanService`, `FindGroupJoinedTeamLifecycleRecorder`, group/alliance runtimes, group/alliance invite services, and the future `CM_FIND_GROUP` adapter without enabling live dispatch.

Safe alternative candidates:

- Add logout cleanup singleton wiring evidence and focused tests while keeping packet sends disabled.
- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.

## Files Changed In UOW-2104

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionGroupInviteTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2104-Completion.md`
- `docs/Phase-6-Session-2104-Handoff.md`
