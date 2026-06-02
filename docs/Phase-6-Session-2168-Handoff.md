# Phase 6 Session 2168 Handoff - FindGroup Show-List Trace Export Projection

Date: 2026-06-02
Unit of Work: UOW-2168
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

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, and mutation-post trace scaffold contracts exist as non-live readiness artifacts.

## UOW-2168 Summary

This UOW added non-live C# export projection for the action `0`/`4` show-list trace schema.

The helper:

- accepts disabled `CM_FIND_GROUP` boundary composition plans,
- supports actions `0` and `4` only,
- projects active player object id/race, server epoch seconds, list kind, visible entry object ids, direct recipient, packet type/action, and zero broadcast/invite counts,
- keeps `executorInvokedFromBoundary=false` and `registrySendObserved=false`,
- rejects unsupported actions, missing active player/client action plan, live-side-effect plans, unexpected side-effect shapes, and missing show-list plans.

The helper is non-live and does not populate Java traces or observe live C# registry sends.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceExportProjection`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Tests.FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`
- `Aion.GameServer.Tests.FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests`

## Validation In UOW-2168

Validation decision:

- Changed surface: focused production schema/projection helper, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketShowListBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live projection used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the projection helper, stable schema fields, direct-packet readiness report, and adjacent disabled boundary composition evidence, and the filtered command built the affected project/dependencies.

Result:

- Passed: 34
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService` | Trace Export Projection / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` C# trace export rows can be populated from disabled boundary plans, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Trace Export Projection / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list filtering and direct-send comparison fields are represented in disabled C# exports. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Export projection is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutation-post trace exports, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a trace schema/export DTO for action `2`/`6` mutation-post boundary traces.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a non-live export projection helper for action `2`/`6` mutation-post disabled boundary plans after the schema exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2168

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListBoundaryTraceExportProjectionTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2168-Completion.md`
- `docs/Phase-6-Session-2168-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2168] Add find group show-list trace projection
```
