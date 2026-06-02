# Phase 6 Session 2160 Completion - FindGroup Direct Packet Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2160
Status: Completed

## Scope

This unit added a non-live direct-packet live-boundary trace contract for future `CM_FIND_GROUP` wiring. It defines what a future ordered boundary trace must prove before direct packet dispatch can be considered ready.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Direct-packet actions are `0`, `2`, `4`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, and `17`.
- Actions `20` and `25` are parsed-only no-ops and stay outside direct-packet ordering requirements.
- Server-packet-only action codes `14` and `16`, world-broadcast actions `1`/`5`, and action `12` invite dispatch are not direct-packet boundary trace actions.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketLiveBoundaryTraceContractService`.
- Added `FindGroupDirectPacketLiveBoundaryTraceContract` and ordered trace step records.
- The contract requires future live traces to record:
  1. triggering client packet accepted,
  2. shared singleton plan composed,
  3. direct packet intents materialized,
  4. direct packet executor invoked from the boundary,
  5. registry sends observed,
  6. one ordered boundary trace captured.
- The contract keeps:
  - `ShouldInvokeLiveSideEffects=false`,
  - `IsCmFindGroupBoundaryWired=false`,
  - `IsReadyForLiveDirectPacketBoundary=false`.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the contract as non-live evidence while keeping live trace blockers.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService` direct send behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new trace contract plus adjacent direct-packet boundary, trigger-order, dry-run, and go/no-go readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 16
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketLiveBoundaryTraceContractService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Direct-packet action inventory and trace milestones are represented, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketLiveBoundaryTraceContractService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java direct `PacketSendUtility.sendPacket` actions have a future trace contract. No live registry sends, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketLiveBoundaryTraceContractServiceTests.Create_KeepsContractBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Contract remains blocked and does not invoke live side effects or wire the boundary. | Focused non-live readiness assertion. | Does not execute live direct packets. |
| `FindGroupDirectPacketLiveBoundaryTraceContractServiceTests.Create_CoversJavaDirectPacketActionsAndExcludesParsedOnlyActions` | Unit | Java action switch source review | Direct-packet action inventory is exact and excludes parsed-only, world-broadcast, invite, and server-packet-only actions. | Focused non-live action inventory assertion. | No runtime packet comparison. |
| `FindGroupDirectPacketLiveBoundaryTraceContractServiceTests.Create_RequiresOrderedTraceMilestonesBeforeLiveReadiness` | Unit | Java synchronous `runImpl` and direct send source review | Future live trace must include ordered boundary acceptance, plan composition, intent materialization, boundary-invoked executor, registry sends, and captured trace. | Focused non-live trace contract assertion. | No live trace exists yet. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct send source review | Readiness report surfaces the new direct-packet live-boundary trace contract as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet trace contract is non-live; no direct registry sends, encrypted socket capture, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused world-broadcast live-boundary trace contract/scaffolding while keeping `CM_FIND_GROUP` live dispatch disabled.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add action `12` invite live-boundary trace contract/scaffolding without invoking request mutation.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2160-Completion.md`
- `docs/Phase-6-Session-2160-Handoff.md`
