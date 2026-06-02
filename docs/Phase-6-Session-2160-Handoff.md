# Phase 6 Session 2160 Handoff - FindGroup Direct Packet Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2160
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
- `FindGroupLiveDispatchDryRunPlanService` enumerates required executors/result surfaces without invoking live side effects.
- `FindGroupDirectPacketLiveBoundaryTraceContractService` now defines the ordered direct-packet live-boundary trace milestones but remains non-live.

## UOW-2160 Summary

This UOW added a direct-packet trace contract for future live boundary evidence:

- Direct-packet actions covered: `0`, `2`, `4`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, `17`.
- Parsed-only actions excluded: `20`, `25`.
- Also excluded from direct-packet trace actions: world-broadcast actions `1`/`5`, action `12` invite dispatch, and server-packet-only action codes `14`/`16`.
- Required ordered trace milestones:
  1. triggering client packet accepted,
  2. shared singleton plan composed,
  3. direct packet intents materialized,
  4. direct packet executor invoked from the boundary,
  5. registry sends observed,
  6. one boundary trace captured.

The contract keeps `ShouldInvokeLiveSideEffects=false`, `IsCmFindGroupBoundaryWired=false`, and `IsReadyForLiveDirectPacketBoundary=false`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketLiveBoundaryTraceContractService`
- `Aion.GameServer.Services.FindGroupDirectPacketLiveBoundaryTraceContract`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Tests.FindGroupDirectPacketLiveBoundaryTraceContractServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`

## Validation In UOW-2160

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService` direct send behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Direct-packet trace contract is non-live; no direct registry sends, encrypted socket capture, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused world-broadcast live-boundary trace contract/scaffolding while keeping `CM_FIND_GROUP` live dispatch disabled.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add action `12` invite live-boundary trace contract/scaffolding without invoking request mutation.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2160

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2160-Completion.md`
- `docs/Phase-6-Session-2160-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2160] Add find group direct packet trace contract
```
