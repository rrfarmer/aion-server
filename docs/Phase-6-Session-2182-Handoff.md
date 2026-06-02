# Phase 6 Session 2182 Handoff - FindGroup Mutation Java Artifact Capture Runbook

Date: 2026-06-02
Unit of Work: UOW-2182
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, mutation-post artifact comparison preflight, and mutation-post Java artifact capture runbook exist as non-live readiness artifacts.

## UOW-2182 Summary

This UOW added `FindGroupMutationPostJavaArtifactCaptureRunbookService`, a non-live runbook metadata service for future Java action `2`/`6` mutation-post artifact capture.

The runbook records:

- planned Java fixture class `FindGroupMutationPostTraceCaptureTest`,
- capture flag `aion.findGroupMutationPost.capture`,
- payload/runImpl/mutation hook targets,
- trace serializer shape using all 22 schema fields,
- exact generated artifact paths,
- focused Maven command,
- validator and comparison-preflight flow.

The focused Maven command is design-only until the Java fixture, instrumentation, and serializer exist:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbook`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookStep`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests`

## Validation In UOW-2182

Validation decision:

- Changed surface: focused non-live Java artifact capture runbook metadata service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. The runbook names the planned Maven command, but the Java fixture, Java instrumentation, trace serializer, and generated artifacts do not exist yet.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new runbook metadata and the immediate instrumentation, schema, artifact target, and preflight dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 25
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService` | Java Artifact Capture Runbook | Blocked | Unit Tested | Partial Parity | The planned action `2`/`6` capture fixture and Maven command are named, but no Java fixture, instrumentation, serializer, generated artifacts, or runtime comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFileReportService` | Java Artifact Capture Runbook / Instrumentation Plan | Partial | Unit Tested | Partial Parity | The runbook preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific packet ids, but it is design metadata only. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java artifact capture runbook exists, but the Java fixture, instrumentation, serializer, generated artifacts, live C# runtime rows, registry observation, and deterministic comparison are missing.
- Artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a C# live trace-row fixture plan for action `2`/`6` mutation-post traces, naming how future live `CmFindGroup` rows will be captured without enabling dispatch prematurely.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2182

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2182-Completion.md`
- `docs/Phase-6-Session-2182-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2182] Add find group mutation Java artifact capture runbook
```
