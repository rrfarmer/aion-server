# Phase 6 Session 2106 Completion - FindGroup Logout Singleton Cleanup Evidence

Date: 2026-06-02
Unit of Work: UOW-2106
Status: Completed

## Scope

- Changed logout cleanup from observer-activated evidence to normal injected singleton behavior.
- Preserved Java ordering: FindGroup logout cleanup happens before pending question-response denial.
- Kept live `CM_FIND_GROUP` dispatch blocked.
- Kept FindGroup logout packet sends disabled; Java `FindGroupService.onLogout` removes state and sends no packets.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - `leaveWorld(Player)` calls `FindGroupService.getInstance().onLogout(player)` before `player.getResponseRequester().denyAll()`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onLogout(Player)` removes entries keyed by player object id from recruitments, applications, and instance groups.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/ResponseRequester.java`
  - `denyAll()` denies and clears pending requests after FindGroup logout cleanup in the Java leave-world flow.

## What Changed

- `PlayerEnterWorldService.RecordFindGroupLogoutCleanup` now runs when a shared `FindGroupRecruitmentPlanService` is injected, even when no observer is supplied.
- The optional `Action<FindGroupLogoutCleanupPlan>` observer remains telemetry only and no longer gates cleanup.
- Existing fallback behavior avoids creating an isolated FindGroup service when neither shared service nor observer is supplied.
- Added a focused leave-world test proving injected FindGroup state is removed without an observer.
- Updated lifecycle/live-dispatch readiness reports and the live-dispatch design note to reflect production singleton graph evidence for logout, joined-team, and disband cleanup.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - Final result: passed, 49 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW added C# logout singleton behavior from reviewed Java `PlayerLeaveWorldService.leaveWorld`, `FindGroupService.onLogout`, and `ResponseRequester.denyAll` order.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: this changed a narrow logout service path and readiness reports. It did not enable live `CM_FIND_GROUP`, live packet sends, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected projects and covered the scoped behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService.leaveWorld` | `Aion.GameServer.Services.PlayerEnterWorldService.LeaveWorldAsync` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | C# now runs injected FindGroup logout cleanup before pending question denial without requiring observer activation. Broader Java leave-world side effects remain outside this FindGroup slice. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Partial | Unit Tested | Partial Parity | Removes recruitment/application/instance-group state keyed by player id and dispatches no live FindGroup packets. Java runtime comparison and concurrency stress remain unverified. |
| `com.aionemu.gameserver.model.gameobjects.player.ResponseRequester.denyAll` | `Aion.GameServer.Model.GameObjects.QuestionResponseRegistry.DenyAll`; `PlayerEnterWorldService.ClearPendingQuestionResponsesAsync` | Request Registry / Logout Order | Partial | Unit Tested | Partial Parity | Existing and updated tests prove FindGroup cleanup observes pending responses before C# denial clears them. Full Java handler callback identity/reflection behavior remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph`; `PlayerEnterWorldService`; `FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Logout, joined-team, and disband lifecycle callers now have production singleton graph evidence. Live `CM_FIND_GROUP` boundary still does not consume the shared service. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerEnterWorldServiceTests.LeaveWorld_UsesInjectedFindGroupLogoutCleanupWithoutObserverLikeJavaSingleton` | Unit | Java `PlayerLeaveWorldService.leaveWorld` and `FindGroupService.onLogout` source review | Injected shared FindGroup service removes logout player recruitment/application/instance-group state without observer activation | Focused C# behavior test plus reviewed Java source | No Java runtime trace; no concurrency stress |
| `PlayerEnterWorldServiceTests.LeaveWorld_RecordsFindGroupLogoutCleanupBeforeQuestionDenyLikeJava` | Unit | Java `FindGroupService.onLogout` before `ResponseRequester.denyAll` source review | FindGroup cleanup runs while pending question responses still exist, before denial clears them | Existing focused C# order test retained | Observer still used for order observation only |
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_RecordsObserverOnlyAndNonLivePlanGaps` | Unit | Java singleton lifecycle call-site source review | Readiness report now marks logout as production singleton graph evidence while keeping live boundary blocked | Focused C# unit test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but not consumed by `GameServerConnection`.
- Socket-level order, real-client behavior, Java runtime packet traces, race-filtered world fanout, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2106 because no broad-validation trigger applied.

## Files Changed

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

## Next Recommended Unit of Work

- Next sequential task: add a non-live `GameServerConnection` adapter-consumer slice proving how the connection could compose `CmFindGroup` through `FindGroupConnectionBoundaryDispatchAdapterService` without executing live sends.

Safe alternative candidates:

- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
- Add focused Java/Maven parity fixture for one FindGroup branch if an executable Java test target can be identified.
- Add a connection-registry ordering audit plan for future live direct sends and race-filtered world broadcasts.
