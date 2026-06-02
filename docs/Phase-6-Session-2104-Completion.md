# Phase 6 Session 2104 Completion - FindGroup Injected Connection Wiring Evidence

Date: 2026-06-02
Unit of Work: UOW-2104
Status: Completed

## Scope

- Added non-live connection/runtime wiring evidence for Java FindGroup singleton joined-team cleanup.
- Kept live `CM_FIND_GROUP` dispatch blocked.
- Kept production DI singleton registration, logout cleanup, and full live singleton parity blocked.
- Continued the focused-validation policy: no broad .NET build/test unless a broad trigger is documented first.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `addPlayerToGroup(...)` calls `FindGroupService.getInstance().onJoinedTeam(invited)` after membership mutation.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `addPlayerToAlliance(...)` calls `FindGroupService.getInstance().onJoinedTeam(invited)` after membership mutation.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onJoinedTeam(...)` removes solo recruitment/application state and can re-add a leader's solo recruitment as team recruitment.

## What Changed

- `GameServerConnection` now accepts optional injected:
  - `PlayerGroupInviteRequestService`
  - `PlayerAllianceInviteRequestService`
- `GameClientSocketServer` now accepts and passes shared invite request services into each `GameServerConnection`.
- Existing fallback constructors remain, so callers without injected services keep prior behavior.
- Added focused connection tests proving injected services with a shared `FindGroupJoinedTeamLifecycleRecorder` mutate the same `FindGroupRecruitmentPlanService` during group/alliance invite acceptance.
- Updated `FindGroupLifecycleSingletonWiringReadinessService` to mark group/alliance join call sites as `PartialInjectedConnectionWiring`, not ready.
- Updated the CM_FIND_GROUP live-dispatch design note with the new injected connection evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionGroupInviteTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests" --no-restore`
  - First run failed on xUnit `Assert.NotNull` return-value assumptions in the new tests.
  - Final result: passed, 17 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW added C# injected wiring evidence from reviewed Java joined-team call sites and existing `FindGroupService.onJoinedTeam` behavior.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: this did not enable `CM_FIND_GROUP`, packet primitives, crypto, persistence, scheduling, production DI registration, or live FindGroup packet dispatch. The affected connection invite slice was covered by filtered tests, which built affected projects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerGroupService.addPlayerToGroup` plus `FindGroupService.onJoinedTeam` | `GameServerConnection` injected `PlayerGroupInviteRequestService` plus `FindGroupJoinedTeamLifecycleRecorder` | Team Lifecycle / Connection Wiring | Partial | Focused Connection Tested | Partial Parity | Group invite acceptance can use an injected shared FindGroup service to move a leader's solo recruitment to team recruitment. Production DI singleton registration and live `CM_FIND_GROUP` remain blocked. |
| `PlayerAllianceService.addPlayerToAlliance` plus `FindGroupService.onJoinedTeam` | `GameServerConnection` injected `PlayerAllianceInviteRequestService` plus `FindGroupJoinedTeamLifecycleRecorder` | Team Lifecycle / Connection Wiring | Partial | Focused Connection Tested | Partial Parity | Alliance invite acceptance can use an injected shared FindGroup service to move a leader's solo recruitment to alliance recruitment. Production DI singleton registration and live `CM_FIND_GROUP` remain blocked. |
| `FindGroupService.SingletonHolder` lifecycle | `FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Needs Verification | Group/alliance joined-team call sites now have injected connection evidence. Overall singleton readiness remains blocked by logout cleanup, `CM_FIND_GROUP`, production DI registration, and cross-caller runtime proof. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_GroupInviteAcceptUsesInjectedFindGroupRecorder` | Focused connection regression | Java group invite accept -> `PlayerGroupService.addPlayerToGroup` -> `FindGroupService.onJoinedTeam` source review | Injected group invite service moves a shared FindGroup solo recruitment onto the new group during accept | Deterministic C# connection-level test plus reviewed Java source | No live `CM_FIND_GROUP`, no production DI singleton proof, no Java runtime packet trace |
| `GameServerConnectionGroupInviteTests.HandleQuestionResponseAsync_AllianceInviteAcceptUsesInjectedFindGroupRecorder` | Focused connection regression | Java alliance invite accept -> `PlayerAllianceService.addPlayerToAlliance` -> `FindGroupService.onJoinedTeam` source review | Injected alliance invite service moves a shared FindGroup solo recruitment onto the new alliance during accept | Deterministic C# connection-level test plus reviewed Java source | No live `CM_FIND_GROUP`, no production DI singleton proof, no Java runtime packet trace |
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_RecordsObserverOnlyAndNonLivePlanGaps` | Unit | Java singleton lifecycle call-site source review | Readiness report records injected connection evidence while keeping live singleton wiring blocked | Focused C# unit test | Report evidence only |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Production DI does not yet register one shared `FindGroupRecruitmentPlanService`, recorder, runtimes, invite services, logout service, and future boundary adapter as a proven singleton graph.
- Logout cleanup remains observer-only and is not normal live singleton behavior.
- Group/alliance disband cleanup remains non-live opt-in planning evidence.
- Socket-level ordering, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionGroupInviteTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2104-Completion.md`
- `docs/Phase-6-Session-2104-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add production DI singleton graph evidence for `FindGroupRecruitmentPlanService`, `FindGroupJoinedTeamLifecycleRecorder`, group/alliance runtimes, group/alliance invite services, and the future `CM_FIND_GROUP` adapter without enabling live dispatch.

Safe alternative candidates:

- Add logout cleanup singleton wiring evidence and focused tests while keeping packet sends disabled.
- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
