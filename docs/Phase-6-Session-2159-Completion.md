# Phase 6 Session 2159 Completion - FindGroup Live Dispatch Dry-Run Plan

Date: 2026-06-02
Unit of Work: UOW-2159
Status: Completed

## Scope

This unit added a non-live `CM_FIND_GROUP` live-dispatch dry-run plan that enumerates the required executors and result surfaces for future wiring without invoking live sends, invite request mutation, or `GameServerConnection.ProcessPacketAsync` dispatch.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.runImpl` dispatches through `FindGroupService.getInstance()`.
- `FindGroupService` side effects include direct `PacketSendUtility.sendPacket`, race-filtered `PacketSendUtility.broadcastToWorld`, action `12` group/alliance invite calls, and shared singleton state across client actions and lifecycle cleanup.
- Actions `20` and `25` are parsed-only no-ops because Java `runImpl` has no branches for them.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim runtime/socket parity.

## Changes

- Added `FindGroupLiveDispatchDryRunPlanService`.
- Added `FindGroupLiveDispatchDryRunPlan` and `FindGroupLiveDispatchDryRunGatePlan` result records.
- The dry-run plan enumerates the required executor/result surfaces for:
  - `GameServerConnection.ProcessPacketAsync case CmFindGroup` boundary wiring,
  - `FindGroupRecruitmentPlanService` shared singleton lifecycle,
  - `FindGroupSideEffectDispatchExecutorService` direct packet phase,
  - `FindGroupSideEffectDispatchExecutorService` world-broadcast phase,
  - `FindGroupInstanceApplicationInviteDispatchPlanService` action `12` invite dispatch,
  - future runtime/socket comparison evidence.
- The dry-run plan keeps:
  - `ShouldInvokeLiveSideEffects=false`,
  - `IsCmFindGroupBoundaryWired=false`,
  - `IsReadyForLiveDispatch=false`.
- Updated `FindGroupLiveDispatchReadinessReportService` and tests to surface the dry-run plan as non-live observer evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# dry-run plan used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new dry-run service plus adjacent go/no-go, readiness-report, and boundary-aggregate surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 15
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupLiveDispatchDryRunPlanService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Dry-run plan enumerates future boundary wiring and result surfaces from Java `runImpl` call shape, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupLiveDispatchDryRunPlanService`; `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService` | Service Readiness | Partial | Unit Tested | Partial Parity | Required executor/result surfaces now include singleton lifecycle, direct packet dispatch, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison. They remain blockers, not verified live parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupLiveDispatchDryRunPlanServiceTests.CreatePlan_EnumeratesEveryRequiredGateWithoutLiveSideEffects` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Dry-run covers every required gate while keeping live side effects and boundary wiring disabled. | Focused non-live readiness assertion. | Does not execute live packets, broadcasts, invites, or sockets. |
| `FindGroupLiveDispatchDryRunPlanServiceTests.CreatePlan_MapsRequiredGatesToExecutorsAndResultSurfaces` | Unit | Java `FindGroupService` send/broadcast/invite/singleton call sites | Each required gate maps to the expected C# executor/result surface and remains blocking. | Focused non-live readiness assertion. | Does not prove any gate resolved. |
| `FindGroupLiveDispatchDryRunPlanServiceTests.CreatePlan_PreservesParsedOnlyActionsAsChecklistNoOpsOutsideLiveSideEffectGates` | Unit | Java `CM_FIND_GROUP.readImpl`/`runImpl` source review | Actions `20` and `25` remain parsed-only no-ops outside the live side-effect gate set. | Focused non-live readiness assertion. | No live adapter execution. |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_RecordsLifecycleObserverEvidenceWithoutMarkingLiveDispatchReady` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Readiness report surfaces the dry-run plan as observer evidence while keeping live dispatch blocked. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Dry-run executor/result surfaces are non-live; they do not prove packet order, fanout, invite mutation, singleton interleavings, or runtime/socket behavior.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary trace scaffolding for direct packet ordering while keeping `CM_FIND_GROUP` live dispatch disabled.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add focused world-broadcast live-boundary trace scaffolding while keeping dispatch disabled.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchDryRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchDryRunPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2159-Completion.md`
- `docs/Phase-6-Session-2159-Handoff.md`
