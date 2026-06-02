# Phase 6 Session 2175 Handoff - FindGroup Mutation Java Trace Artifact File Targets

Date: 2026-06-02
Unit of Work: UOW-2175
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, and mutation-post Java artifact file target report exist as non-live readiness artifacts.

## UOW-2175 Summary

This UOW added `FindGroupMutationPostJavaTraceArtifactFileReportService`, a non-live report that fixes target paths for future generated Java action `2`/`6` mutation-post trace artifacts.

Default artifact root:

```text
parity-artifacts/find-group/mutation-post/java
```

Expected files:

```text
parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-2-java.json
parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-6-java.json
```

Both targets remain blocked until generated Java artifacts exist and validate through `FindGroupMutationPostJavaTraceArtifactValidatorService`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReport`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileRow`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactFileReportServiceTests`

## Validation In UOW-2175

Validation decision:

- Changed surface: focused non-live Java trace artifact file target report service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, and no generated Java artifact exists to execute or validate through Maven.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new file-target report plus adjacent schema, validator, and Java instrumentation design reports, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 20
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService` | Java Trace Artifact File Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` generated artifact paths and names are represented, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService` | Java Trace Artifact File Target / Instrumentation Design | Partial | Unit Tested | Partial Parity | File targets are tied to mutation kind, posted system message ids, refreshed show-list actions, and validator target. They are future landing paths only and do not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java trace artifact file targets exist, but Java instrumentation, serializer, generated artifacts, and artifact reader/discovery are missing.
- No live C# trace row, encrypted socket capture, registry-send observation, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a blocked Java trace artifact reader/discovery service that can consume the chosen action `2`/`6` generated artifact paths and run `FindGroupMutationPostJavaTraceArtifactValidatorService` once files exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a C# live trace-emitter design companion for action `2`/`6` mutation-post rows, still blocked until live boundary capture exists.

## Files Changed In UOW-2175

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactFileReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactFileReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2175-Completion.md`
- `docs/Phase-6-Session-2175-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2175] Add find group mutation Java artifact file targets
```
