# Phase 6 Session 2178 Handoff - FindGroup Mutation Runtime Comparison Readiness

Date: 2026-06-02
Unit of Work: UOW-2178
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, and mutation-post runtime comparison readiness aggregate exist as non-live readiness artifacts.

## UOW-2178 Summary

This UOW added `FindGroupMutationPostRuntimeComparisonReadinessReportService`, a conservative aggregate report for action `2`/`6` mutation-post runtime comparison readiness.

The aggregate tracks:

- mutation-post schema metadata,
- Java instrumentation design,
- Java artifact reader status,
- C# trace-emitter design,
- live C# boundary capture,
- deterministic runtime comparison execution.

It keeps runtime comparison blocked until generated Java artifacts, live C# boundary/runtime rows, and deterministic comparison evidence exist.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReportService`
- `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReport`
- `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessRow`
- `Aion.GameServer.Tests.FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests`

## Validation In UOW-2178

Validation decision:

- Changed surface: focused non-live runtime-comparison readiness aggregate service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this aggregate metadata UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new readiness aggregate and each direct dependency it summarizes; the filtered command builds the affected project/dependencies.

Result:

- Passed: 37
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReportService` | Runtime Comparison Readiness | Blocked | Unit Tested | Partial Parity | Action `2`/`6` comparison blockers are aggregated, but generated Java artifacts, live C# trace rows, live boundary capture, and deterministic runtime comparison are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService` | Runtime Comparison Readiness / Trace Readiness | Partial | Unit Tested | Partial Parity | The readiness report tracks mutation-post schema and hook metadata while preserving Java mutation-before-posted-message-before-refreshed-list requirements. It does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Runtime comparison readiness aggregate exists, but generated Java artifacts, live C# boundary capture, live C# runtime rows, and deterministic comparison are missing.
- Java instrumentation, serializer, generated artifacts, artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a live-boundary trace contract extension that names the exact registry observation evidence needed for action `2`/`6` before enabling live direct-packet dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add comparison key-projection metadata for mutation-post action `2`/`6` rows after actual Java/C# trace row shapes exist.

## Files Changed In UOW-2178

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostRuntimeComparisonReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2178-Completion.md`
- `docs/Phase-6-Session-2178-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2178] Add find group mutation runtime comparison readiness
```
