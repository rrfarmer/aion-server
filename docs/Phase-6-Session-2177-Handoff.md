# Phase 6 Session 2177 Handoff - FindGroup Mutation CSharp Trace Emitter Design

Date: 2026-06-02
Unit of Work: UOW-2177
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, and mutation-post C# trace-emitter design report exist as non-live readiness artifacts.

## UOW-2177 Summary

This UOW added `FindGroupMutationPostCSharpTraceEmitterDesignReportService`, a non-live C# trace-emitter design companion for future action `2`/`6` mutation-post runtime rows.

The design rows cover:

- artifact shape validation boundary,
- live `GameServerConnection.ProcessPacketAsync` boundary acceptance,
- singleton mutation projection,
- direct packet intent materialization,
- boundary executor invocation,
- registry send observation,
- runtime trace row serialization.

No live boundary capture, live emitter, C# runtime row, generated Java artifact, or runtime comparison was added.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReport`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignRow`
- `Aion.GameServer.Tests.FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests`

## Validation In UOW-2177

Validation decision:

- Changed surface: focused non-live C# trace-emitter design report service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, and this design-only C# report depends on already-reviewed Java source and schema contracts.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new emitter design plus adjacent mutation-post schema, Java artifact reader/file target, validator, and Java instrumentation design dependencies, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 31
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService` | C# Trace Emitter Design Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` future C# trace-emitter hook sites are represented, but live boundary capture, live emitter implementation, C# runtime rows, generated Java artifacts, and runtime comparison are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | C# Trace Emitter Design / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The design requires singleton mutation projection, posted-message-before-refreshed-list ordering, and registry send observation matching Java action `2`/`6` behavior. It is design-only and does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- C# trace-emitter design exists, but live boundary capture, live emitter implementation, C# runtime rows, and runtime comparison are missing.
- Java instrumentation, serializer, generated artifacts, and artifact-backed runtime comparison are missing.
- No encrypted socket capture, registry-send observation, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add comparison-readiness rows that aggregate the mutation-post schema, Java artifact reader, C# trace-emitter design, and runtime comparison blockers for action `2`/`6`.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live-boundary trace contract extension that names the exact registry observation evidence needed for action `2`/`6`.

## Files Changed In UOW-2177

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpTraceEmitterDesignReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2177-Completion.md`
- `docs/Phase-6-Session-2177-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2177] Add find group mutation CSharp trace emitter design
```
