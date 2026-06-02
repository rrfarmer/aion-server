# Phase 6 Session 2191 Handoff - FindGroup Mutation Java Trace Capture Fixture Scaffold

Date: 2026-06-02
Unit of Work: UOW-2191
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
- Full .NET suite/full solution build is not routine validation. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2191 Summary

This UOW added `FindGroupMutationPostTraceCaptureTest`, a Java Maven-runnable fixture scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post artifact capture.

The scaffold names:

- capture flag `aion.findGroupMutationPost.capture`,
- trace name `cm-find-group-direct-mutation-post-boundary`,
- artifact root `parity-artifacts/find-group/mutation-post/java`,
- action `2` recruitment scenario metadata,
- action `6` application scenario metadata,
- stable action-specific artifact file names.

The scaffold is runnable with the capture flag enabled, but it does not instrument Java runtime behavior, write artifacts, serialize rows, or prove parity.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`
- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests`
- `Aion.GameServer.Tests.FindGroupMutationPostTraceRowReadinessAggregateServiceTests`

## Validation In UOW-2191

Validation decision:

- Changed surface: focused Java test fixture scaffold, focused C# non-live readiness metadata/tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests" --no-restore
```

- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected C# project and dependencies.
- Why this scope is sufficient: the Java command proves the new scaffold is discoverable and runnable through the real Maven test path with the capture flag enabled; the C# filter covers the updated runbook/readiness/aggregate metadata and immediate schema/validator neighbors.

Results:

- Java/Maven: passed 4, failed 0, skipped 0.
- C#: passed 35, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings and C# nullable/analyzer warnings were emitted.

Validation correction:

- First Java attempt failed because `captureFlagDefaultsToDisabled` asserted the flag was false while the focused command intentionally passed `-Daion.findGroupMutationPost.capture=true`.
- The test was corrected to clear and restore the system property for the default-flag assertion, then the same focused Java command passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService` | Java Fixture Scaffold / Readiness Metadata | Partial | Unit Tested | Partial Parity | Java packet source for actions `2` and `6` was reviewed and the fixture scaffold now names action scenarios and artifact targets, but it does not execute `readImpl/runImpl`, capture runtime rows, or prove Java/C# parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Mutation-Post Java Capture Scaffold | Partial | Unit Tested | Partial Parity | The scaffold preserves Java-derived `addRecruitment`/`addApplication` mappings for posted system message ids `1400392`/`1400393` and refreshed list actions `0`/`4`, but Java instrumentation, serializer, generated artifacts, live C# rows, registry observation, and comparison execution are missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java fixture scaffold is not runtime parity evidence; it only proves the Maven-runnable capture-gated scaffold and scenario metadata.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java mutation-post trace instrumentation hook design-to-code scaffolding for `CM_FIND_GROUP`/`FindGroupService` behind the capture flag, still without changing send ordering or writing artifact files.

Safe candidates:

- Add a Java trace serializer design-to-code checklist before writing serializer code.
- Add Java fixture helper methods for deterministic payload/scenario construction while keeping runtime execution disabled.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed In UOW-2191

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2191-Completion.md`
- `docs/Phase-6-Session-2191-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2191] Add find group mutation Java trace capture fixture scaffold
```
