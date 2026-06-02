# Phase 6 Session 2170 Handoff - FindGroup Mutation Trace Projection

Date: 2026-06-02
Unit of Work: UOW-2170
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

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold, mutation-post trace schema, and mutation-post trace projection contracts exist as non-live readiness artifacts.

## UOW-2170 Summary

This UOW added `FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.CreateExportFromDisabledPlan`, a non-live export projection helper for disabled action `2`/`6` boundary plans.

Supported projected actions:

- `2`: recruitment mutation, posted system message id `1400392`, refreshed `SM_FIND_GROUP` action `0`.
- `6`: application mutation, posted system message id `1400393`, refreshed `SM_FIND_GROUP` action `4`.

The helper requires disabled non-live composition plans, exactly two direct packet intents, no world broadcast, no invite intent, an added mutation plan, and a refreshed show-list plan. It fills the C# trace export fields but keeps executor and registry observations false until a live boundary trace exists.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceExportProjection`
- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionStatus`
- `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`
- `Aion.GameServer.Tests.FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests`
- `Aion.GameServer.Tests.FindGroupDirectPacketBoundaryTraceReadinessServiceTests`

## Validation In UOW-2170

Validation decision:

- Changed surface: focused production schema/projection service, focused tests, readiness-report text, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live projection used reviewed Java `CM_FIND_GROUP.runImpl` actions `2`/`6` plus `FindGroupService.addRecruitment/addApplication` behavior as the oracle. No narrow executable Java fixture was identified for this projection artifact.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new projection plus adjacent mutation-post schema, readiness, and disabled side-effect composition surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 34
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Trace Export Projection / Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `2`/`6` C# trace export rows can be populated from disabled boundary plans, but no Java/C# trace capture or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Trace Export Projection / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message ordering, refreshed show-list ordering, and post-mutation race-filtered visible ids are represented in disabled C# exports. No live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- The mutation-post projection is non-live; no Java/C# trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- The projection does not prove `GameServerConnection.ProcessPacketAsync` invokes the direct-packet executor from the triggering packet boundary.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

Safe candidates:

- Add a runtime comparison fixture contract row for action `2`/`6` mutation-post traces now that schema and projection exist.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a non-live trace export projection for another direct-packet subgroup only if a stable schema already exists.

## Files Changed In UOW-2170

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketMutationPostBoundaryTraceExportProjectionTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2170-Completion.md`
- `docs/Phase-6-Session-2170-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2170] Add find group mutation trace projection
```
