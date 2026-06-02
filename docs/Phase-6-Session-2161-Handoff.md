# Phase 6 Session 2161 Handoff - FindGroup World Broadcast Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2161
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

Each completion/handoff must record the changed surface, focused C# command or hygiene command, Java/Maven command or skip rationale, broad-validation trigger or `none`, broad .NET skip/run decision, and why the selected scope was sufficient.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `FindGroupDirectPacketLiveBoundaryTraceContractService` defines the direct-packet ordered trace contract.
- `FindGroupWorldBroadcastLiveBoundaryTraceContractService` now defines the world-broadcast ordered trace contract but remains non-live.

## UOW-2161 Summary

This UOW added a world-broadcast trace contract for future live boundary evidence:

- World-broadcast actions covered: `1`, `5`.
- Other `CM_FIND_GROUP` actions are outside this fanout contract.
- Required ordered trace milestones:
  1. triggering client packet accepted,
  2. shared singleton removal evaluated,
  3. world-broadcast intent materialized only for removed branches,
  4. Java race filter applied,
  5. world-broadcast executor invoked from the boundary,
  6. registry broadcast observed,
  7. one boundary trace captured.

The contract keeps `ShouldInvokeLiveSideEffects=false`, `IsCmFindGroupBoundaryWired=false`, and `IsReadyForLiveWorldBroadcastBoundary=false`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContractService`
- `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContract`
- `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService`
- `Aion.GameServer.Tests.FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests`
- `Aion.GameServer.Tests.FindGroupWorldBroadcastFanoutReadinessServiceTests`

## Validation In UOW-2161

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService.removeRecruitment/removeApplication` broadcast behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new trace contract plus adjacent world-broadcast, dry-run, and go/no-go readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 13
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContractService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | World-broadcast action inventory and trace milestones are represented for actions `1` and `5`, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContractService`; `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService` | World Broadcast Readiness | Partial | Unit Tested | Partial Parity | Java `PacketSendUtility.broadcastToWorld` race-filter branches have a future trace contract. No live registry broadcasts, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- World-broadcast trace contract is non-live; no registry broadcasts, encrypted socket capture, or real-client runtime comparison has executed.
- Direct packet ordering, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add action `12` invite live-boundary trace contract/scaffolding without invoking request mutation or live dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add runtime/socket comparison preflight contract for `CM_FIND_GROUP` after action `12` trace prerequisites are explicit.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2161

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2161-Completion.md`
- `docs/Phase-6-Session-2161-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2161] Add find group world broadcast trace contract
```
