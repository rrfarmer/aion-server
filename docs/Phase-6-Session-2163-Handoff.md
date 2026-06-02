# Phase 6 Session 2163 Handoff - FindGroup Runtime Comparison Preflight Contract

Date: 2026-06-02
Unit of Work: UOW-2163
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
- Direct-packet, world-broadcast, action `12` invite, and runtime/socket comparison contracts now exist as non-live readiness artifacts.

## UOW-2163 Summary

This UOW added a runtime/socket comparison preflight contract for future `CM_FIND_GROUP` evidence:

- Required trace fields:
  - parsed client action,
  - active player facts,
  - parsed payload fields,
  - singleton state before/after,
  - direct packets,
  - world broadcasts,
  - action `12` invite requests,
  - no-side-effect branches,
  - encrypted socket frames.
- Required scenario groups:
  - show-list direct packets,
  - mutation direct packets,
  - world broadcasts,
  - instance application action `11`,
  - action `12` invite/decline outcomes,
  - parsed-only no-run actions `20`/`25`,
  - shared singleton lifecycle interleavings.

The contract keeps `ShouldInvokeLiveSideEffects=false`, `IsCmFindGroupBoundaryWired=false`, and `IsReadyForRuntimeComparison=false`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContractService`
- `Aion.GameServer.Services.FindGroupRuntimeComparisonPreflightContract`
- `Aion.GameServer.Services.FindGroupLiveDispatchGoNoGoChecklistService`
- `Aion.GameServer.Services.FindGroupLiveDispatchDryRunPlanService`
- `Aion.GameServer.Tests.FindGroupRuntimeComparisonPreflightContractServiceTests`
- `Aion.GameServer.Tests.FindGroupLiveDispatchGoNoGoChecklistServiceTests`
- `Aion.GameServer.Tests.FindGroupLiveDispatchDryRunPlanServiceTests`

## Validation In UOW-2163

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live preflight contract used reviewed Java `CM_FIND_GROUP.readImpl/runImpl` plus `FindGroupService` side-effect behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Runtime/socket comparison preflight is non-live; no Java runtime trace, C# runtime trace, encrypted socket frame capture, or real-client comparison has executed.
- Direct-packet live ordering, world-broadcast live fanout, action `12` live invite mutation, shared singleton live interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-packet live-boundary trace implementation scaffolding for one low-risk direct-packet action set while keeping `ProcessPacketAsync` disabled.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a trace schema/export DTO for the runtime comparison preflight contract without capturing live traffic.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2163

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRuntimeComparisonPreflightContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRuntimeComparisonPreflightContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchDryRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchDryRunPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2163-Completion.md`
- `docs/Phase-6-Session-2163-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2163] Add find group runtime comparison preflight contract
```
