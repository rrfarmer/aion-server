# Phase 6 Session 2176 Handoff - FindGroup Mutation Java Trace Artifact Directory Reader

Date: 2026-06-02
Unit of Work: UOW-2176
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, and mutation-post Java artifact directory reader exist as non-live readiness artifacts.

## UOW-2176 Summary

This UOW added `FindGroupMutationPostJavaTraceArtifactDirectoryReportService`, a guarded reader for the chosen Java action `2`/`6` mutation-post artifact files.

The reader:

- reports missing artifact directories,
- reports missing expected action files,
- validates present files with `FindGroupMutationPostJavaTraceArtifactValidatorService`,
- rejects shape-valid files that do not contain the expected action row for their filename,
- keeps `ReadyForRuntimeComparison=false` even when all expected files are shape-valid.

No Java instrumentation, serializer, generated artifact, C# live trace row, or runtime comparison was added.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReport`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryFileRow`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests`

## Validation In UOW-2176

Validation decision:

- Changed surface: focused non-live Java trace artifact directory reader service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, and no generated Java artifact exists for Maven to produce or validate in this UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new reader plus its file-target, schema, validator, and instrumentation-design dependencies, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 25
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Java Trace Artifact Reader Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` generated artifact discovery and validation targets are represented, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Java Trace Artifact Reader Target / Validator | Partial | Unit Tested | Partial Parity | The reader enforces expected file presence, schema validation, and expected action rows for mutation-post traces. It is guarded discovery only and does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java trace artifact directory reading exists, but Java instrumentation, serializer, generated artifacts, and runtime comparison are missing.
- No live C# trace row, encrypted socket capture, registry-send observation, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a C# live trace-emitter design companion for action `2`/`6` mutation-post rows, still blocked until live boundary capture exists.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add comparison-readiness rows that aggregate the mutation-post schema, Java file reader, future C# trace emitter, and runtime comparison blockers.

## Files Changed In UOW-2176

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2176-Completion.md`
- `docs/Phase-6-Session-2176-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2176] Add find group mutation Java artifact reader
```
