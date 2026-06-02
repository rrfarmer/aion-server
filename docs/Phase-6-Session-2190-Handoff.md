# Phase 6 Session 2190 Handoff - FindGroup Mutation Java Artifact Capture Implementation Readiness

Date: 2026-06-02
Unit of Work: UOW-2190
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2190 Summary

This UOW added `FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`, a concrete non-live implementation checklist for future Java `CM_FIND_GROUP` action `2` and `6` mutation-post artifact capture.

The checklist names:

- future fixture class `FindGroupMutationPostTraceCaptureTest`,
- deterministic action `2` recruitment and action `6` application scenarios,
- Java hook points across `CM_FIND_GROUP` and `FindGroupService`,
- trace serializer requirements,
- expected generated artifact file paths,
- artifact validator flow,
- focused Maven command,
- comparison preflight handoff.

It remains blocked by default and does not generate Java artifacts or prove runtime parity.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadiness`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationTask`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests`

## Validation In UOW-2190

Validation decision:

- Changed surface: focused non-live Java artifact capture implementation-readiness service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, the planned `FindGroupMutationPostTraceCaptureTest` fixture does not exist, and there is no Java serializer or generated artifact to validate.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new checklist and its immediate Java capture runbook, instrumentation design, schema, and validator dependencies without spending time on unrelated suites.

Result:

- Passed: 29
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService` | Java Artifact Capture Implementation Readiness Checklist | Blocked | Unit Tested | Partial Parity | The checklist names future action `2`/`6` fixture and instrumentation tasks from reviewed Java packet behavior, but Java fixture, instrumentation, serializer, generated artifacts, live C# rows, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Mutation-Post Java Artifact Capture Readiness | Partial | Unit Tested | Partial Parity | The checklist preserves Java-derived mutation-before-posted-message-before-refreshed-list ordering requirements for `addRecruitment` and `addApplication`, but it is non-live metadata only and proves no runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Implement the targeted Java fixture skeleton for `FindGroupMutationPostTraceCaptureTest` behind the `aion.findGroupMutationPost.capture` flag, without changing gameplay behavior or generating artifacts until the fixture/hook strategy is reviewed.

Safe candidates:

- Add a Java trace serializer design-to-code checklist if the team wants one more non-live guard before editing Java source.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2190

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2190-Completion.md`
- `docs/Phase-6-Session-2190-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2190] Add find group mutation Java artifact capture readiness checklist
```
