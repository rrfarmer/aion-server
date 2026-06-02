# Phase 6 Session 2110 Handoff - FindGroup Side-Effect Executor Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2110
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

Focused validation is the default. Do not run the broad .NET suite or full solution build without a documented broad-validation trigger.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` now records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, for future live-boundary audits.
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
- UOW-2110: opt-in FindGroup side-effect executor now records direct/broadcast execution order.

## Current UOW Commit Message

- `[Phase 6][UOW-2110] Add find group side-effect ordering evidence`

## Validation In UOW-2110

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests" --no-restore`
  - Final result: 15 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `PacketSendUtility` and `FindGroupService` source and added C# opt-in executor ordering evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `FindGroupSideEffectDispatchExecutorService.ExecuteAsync`; `IGameClientConnectionRegistry.SendPacketToPlayerAsync` | Send Utility / Executor | Partial | Unit Tested | Partial Parity | C# opt-in executor records direct packet send order and sent/unsent results. Live `CM_FIND_GROUP` boundary does not invoke it; Java runtime comparison and online-state edge cases remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` | `FindGroupSideEffectDispatchExecutorService.ExecuteAsync`; `IGameClientConnectionRegistry.BroadcastToWorldAsync` | Broadcast Utility / Executor | Partial | Unit Tested | Partial Parity | C# opt-in executor applies Java-shaped race predicate and records broadcast order/count. Live connection ordering relative to triggering packet remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` direct/broadcast call sites | `FindGroupSideEffectDispatchExecutionPlan.ExecutionOrder` | Side-Effect Ordering Evidence | Partial | Unit Tested | Partial Parity | Direct-before-broadcast audit evidence is available for future live boundary review. No live sends or full socket runtime comparison were enabled. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Ordering relative to the triggering encrypted client packet is not proven.
- Direct packet sends, race-filtered world broadcasts, action 11/12 side effects, socket-level order, real-client behavior, Java runtime packet traces, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2110 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add a disabled action 12 connection-helper test using the connection resolver and injected group/alliance runtimes.

Safe alternative candidates:

- Continue reviewing `FindGroupRecruitmentPlanService` enumeration snapshot behavior against Java stream snapshots under concurrent map state.
- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add non-live live-boundary failure result shapes for missing active player, missing recipient, and parsed-only no-op branches before enabling any `ProcessPacketAsync` call.

## Files Changed In UOW-2110

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSideEffectDispatchExecutorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSideEffectDispatchExecutorServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2110-Completion.md`
- `docs/Phase-6-Session-2110-Handoff.md`
