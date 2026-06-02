# Phase 6 Session 2158 Completion - FindGroup Live Dispatch Required Gate Checklist

Date: 2026-06-02
Unit of Work: UOW-2158
Status: Completed

## Scope

This unit tightened the non-live `CM_FIND_GROUP` go/no-go checklist so future live `GameServerConnection.ProcessPacketAsync` wiring has a concrete required-gate surface.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.runImpl` dispatches executable actions `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17` through `FindGroupService.getInstance()`.
- Actions `20` and `25` are parsed in `readImpl` but have no `runImpl` branch.
- `FindGroupService` uses one singleton across client packet actions, logout cleanup, joined-team cleanup, and disband cleanup.
- `FindGroupService` emits direct packets through `PacketSendUtility.sendPacket`, world broadcasts through `PacketSendUtility.broadcastToWorld`, and action `12` accepted invite side effects through group/alliance invite services.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim runtime/socket parity.

## Changes

- Added an explicit required live-dispatch gate set to `FindGroupLiveDispatchGoNoGoChecklist`.
- Added computed checklist helpers:
  - `HasAllRequiredLiveDispatchGates`
  - `BlockingRequiredGateKinds`
  - `LiveWiringDecision`
- Tightened `IsReadyForLiveDispatch` so readiness also requires all required gates to be present and no required gate to be blocking.
- Added focused tests asserting that live `ProcessPacketAsync` wiring remains blocked by:
  - connection boundary wiring,
  - shared singleton lifecycle,
  - live direct packet ordering,
  - live world-broadcast fanout,
  - action `12` live invite dispatch,
  - runtime/socket comparison.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` to document the required-gate surface and to preserve parsed-only actions `20`/`25` as ready no-ops that do not satisfy live side-effect gates.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness service, focused tests, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchActionGateMatrixServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this readiness/checklist unit used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService` behavior as the oracle. No narrow executable Java fixture was identified for this non-live C# checklist surface.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the changed checklist plus adjacent action-gate, readiness-report, and boundary-aggregate surfaces, and the filtered test command built the affected project/dependencies.

Result:

- Passed: 15
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupLiveDispatchGoNoGoChecklistService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Java `runImpl` actions and parsed-only actions are represented as readiness gates. Live `ProcessPacketAsync` execution remains disabled and unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupLiveDispatchGoNoGoChecklistService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Readiness | Partial | Unit Tested | Partial Parity | Required gates now explicitly include singleton lifecycle, direct packet dispatch, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison. Live singleton execution and runtime/socket comparison remain blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_KeepsLiveDispatchBlockedUntilEveryGateIsReady` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.getInstance` source review | Checklist status remains blocked, required gate inventory exists, and live wiring decision says not to wire `ProcessPacketAsync`. | Focused non-live readiness assertion. | Does not execute live client packets or sockets. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_SeparatesEvidenceAvailableGatesFromReadyGates` | Unit | Java `FindGroupService` send/broadcast/invite call sites | Disabled evidence for direct packets, world broadcasts, and action `12` invite dispatch is not treated as ready live dispatch. | Focused non-live readiness assertion. | No live connection-registry or runtime comparison. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_RequiresEveryLiveSideEffectGateBeforeProcessPacketAsyncWiring` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` singleton/send/broadcast/invite call sites | Required blocking gates are explicit: boundary wiring, singleton lifecycle, direct packet ordering, world fanout, action `12` invite dispatch, and runtime/socket comparison. | Focused non-live checklist assertion. | Does not prove any blocker resolved. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_MarksParsedOnlyActionsAsReadyNoOps` | Unit | Java `CM_FIND_GROUP.readImpl`/`runImpl` source review | Actions `20` and `25` stay ready parsed-only no-ops and do not satisfy live side-effect gates. | Focused non-live readiness assertion. | No live adapter execution. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Required go/no-go gates are explicit but not resolved.
- Disabled evidence remains non-live and cannot prove packet order, world fanout, action `12` invite side effects, singleton interleavings, or runtime/socket parity.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add a narrow non-live `CM_FIND_GROUP` live-wiring dry-run plan that enumerates the required executors and result surfaces for the required gates without invoking live sends or request mutations.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add focused live-boundary trace scaffolding for direct packet ordering while keeping dispatch disabled.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2158-Completion.md`
- `docs/Phase-6-Session-2158-Handoff.md`
