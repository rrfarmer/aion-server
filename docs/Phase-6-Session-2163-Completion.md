# Phase 6 Session 2163 Completion - FindGroup Runtime Comparison Preflight Contract

Date: 2026-06-02
Unit of Work: UOW-2163
Status: Completed

## Scope

This unit added a non-live runtime/socket comparison preflight contract for future `CM_FIND_GROUP` parity evidence. It defines the trace fields and scenario groups that must be captured and compared before runtime/socket evidence can support live parity claims.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior covered by the preflight contract:

- Parsed action and action-specific payload fields from `readImpl`.
- Active player and singleton `FindGroupService` state before and after `runImpl`.
- Direct packet sends, world broadcasts, action `12` invite requests, missing-branch no-side-effect outcomes, parsed-only actions `20`/`25`, and lifecycle interleavings with logout/joined-team/disband cleanup.

This UOW does not execute live dispatch, does not capture runtime/socket frames, and does not claim runtime parity.

## Changes

- Added `FindGroupRuntimeComparisonPreflightContractService`.
- Added `FindGroupRuntimeComparisonPreflightContract`, trace field records, and scenario records.
- Required trace fields now include:
  - parsed client action,
  - active player facts,
  - parsed payload fields,
  - singleton state before/after,
  - direct packets,
  - world broadcasts,
  - action `12` invite requests,
  - no-side-effect branches,
  - encrypted socket frames.
- Required scenario groups now include:
  - show-list direct packets,
  - mutation direct packets,
  - world broadcasts,
  - instance application action `11`,
  - action `12` invite/decline outcomes,
  - parsed-only no-run actions `20`/`25`,
  - shared singleton lifecycle interleavings.
- Updated the live dispatch go/no-go checklist and dry-run plan to reference the runtime comparison preflight contract.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live preflight contract used reviewed Java `CM_FIND_GROUP.readImpl/runImpl` plus `FindGroupService` side-effect behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new preflight contract and the adjacent go/no-go and dry-run readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Final run passed: 10
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.
- Note: the first focused run caught an assertion drift in `FindGroupLiveDispatchGoNoGoChecklistServiceTests`; the assertion was updated and the same focused command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService` | Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Required trace fields and scenario groups are represented, but no Java/C# runtime trace or encrypted socket capture has executed. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService` | Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Singleton state, direct packet, broadcast, invite, no-side-effect, and lifecycle interleaving capture requirements are represented. Runtime/socket parity evidence remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_KeepsRuntimeComparisonBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.readImpl/runImpl`; `FindGroupService` source review | Contract remains blocked and non-live while requiring Java trace, C# trace, and encrypted socket capture. | Focused non-live readiness assertion. | Does not capture or compare runtime traces. |
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_RequiresTraceFieldsForPayloadStatePacketsFanoutAndSocketFrames` | Unit | Java packet parse and side-effect source review | Required trace fields cover payload, singleton state, direct packets, world broadcasts, invite requests, no-side-effect branches, and encrypted frames. | Focused preflight assertion. | No trace harness exists yet. |
| `FindGroupRuntimeComparisonPreflightContractServiceTests.Create_CoversFindGroupScenarioMatrixConservatively` | Unit | Java action switch and `FindGroupService` source review | Scenario matrix covers direct packets, world broadcasts, action `11`, action `12`, parsed-only actions, and lifecycle interleavings. | Focused preflight assertion. | No runtime comparison has executed. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_KeepsLiveDispatchBlockedUntilEveryGateIsReady` | Unit | Java source review | Runtime comparison gate references the new preflight contract while keeping live dispatch blocked. | Focused readiness-report assertion. | Report evidence only. |
| `FindGroupLiveDispatchDryRunPlanServiceTests.CreatePlan_MapsRequiredGatesToExecutorsAndResultSurfaces` | Unit | Java source review | Dry-run plan names the preflight contract as the runtime comparison result surface. | Focused readiness-report assertion. | Report evidence only. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 runtime/socket comparison gate
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Runtime/socket comparison preflight is non-live; no Java runtime trace, C# runtime trace, encrypted socket frame capture, or real-client comparison has executed.
- Direct packet ordering, world-broadcast fanout, action `12` invite mutation, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-packet live-boundary trace implementation scaffolding for one low-risk direct-packet action set while keeping `ProcessPacketAsync` disabled.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a trace schema/export DTO for the runtime comparison preflight contract without capturing live traffic.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRuntimeComparisonPreflightContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRuntimeComparisonPreflightContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchDryRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchDryRunPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2163-Completion.md`
- `docs/Phase-6-Session-2163-Handoff.md`
