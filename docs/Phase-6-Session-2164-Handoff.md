# Phase 6 Session 2164 Handoff - FindGroup Direct Show-List Boundary Trace Scaffold

Date: 2026-06-02
Unit of Work: UOW-2164
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
- Direct-packet, world-broadcast, action `12` invite, and runtime/socket comparison contracts exist as non-live readiness artifacts.
- `FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService` now scopes the first low-risk direct-packet trace scaffold to actions `0` and `4`.

## UOW-2164 Summary

This UOW added a direct show-list boundary trace scaffold:

- Covered actions: `0`, `4`.
- Excluded direct-packet actions: `2`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, `17`.
- Required ordered trace milestones:
  1. triggering client packet accepted,
  2. show-list plan composed,
  3. direct packet intent materialized,
  4. direct packet executor invoked from the boundary,
  5. registry send observed,
  6. boundary trace captured.

The scaffold keeps `ShouldInvokeLiveSideEffects=false`, `IsCmFindGroupBoundaryWired=false`, and `IsReadyForLiveShowListBoundary=false`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService`
- `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffold`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Tests.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`

## Validation In UOW-2164

Validation decision:

- Changed surface: focused production readiness/scaffold service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketLiveBoundaryTraceContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# scaffold used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new scaffold plus adjacent direct-packet readiness and direct-packet live-boundary contract surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 9
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` show-list trace milestones are represented, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list direct-send branches have a focused scaffold. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Show-list scaffold is non-live; no live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutating direct-packet actions, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a trace schema/export DTO for `CM_FIND_GROUP` direct show-list boundary traces so future action `0`/`4` live trace captures have a stable comparison format.

Safe candidates:

- Add focused direct-packet live-boundary trace scaffolding for mutating direct-packet actions `2` and `6` without live dispatch.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2164

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2164-Completion.md`
- `docs/Phase-6-Session-2164-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2164] Add find group show-list boundary trace scaffold
```
