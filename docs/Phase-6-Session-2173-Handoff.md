# Phase 6 Session 2173 Handoff - FindGroup Mutation Java Trace Artifact Validator

Date: 2026-06-02
Unit of Work: UOW-2173
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, and mutation-post Java trace artifact validator target exist as non-live readiness artifacts.

## UOW-2173 Summary

This UOW added `FindGroupMutationPostJavaTraceArtifactValidatorService`, a JSON validator target for future Java action `2`/`6` mutation-post trace artifacts.

The validator rejects:

- invalid JSON,
- unsupported schema version,
- unexpected trace name,
- missing trace rows,
- missing required fields,
- invalid field types,
- non-Java `traceSource`,
- unsupported actions outside `2`/`6`,
- action mapping mismatches,
- nonzero world-broadcast or invite counts.

No Java artifacts were generated and no live C# trace was captured.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidationReport`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidationIssue`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactMetadata`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidationTraceRow`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactValidatorServiceTests`

## Validation In UOW-2173

Validation decision:

- Changed surface: focused production Java trace artifact validator service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed and no generated Java trace artifact exists yet. This UOW validates the future artifact shape from reviewed Java source and the schema target.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new validator plus adjacent Java trace schema target, runtime preflight, and mutation-post comparison schema surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 18
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Java Trace Artifact Validator Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post Java trace artifact validator is represented and tied to the schema target, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Java Trace Artifact Validator Target / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Validator enforces Java action `2`/`6` mutation kind, posted system message id, refreshed show-list action, Java trace source, and zero broadcast/invite counts. No Java artifact, live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- The Java trace artifact validator is non-live contract tooling; no Java instrumentation, Java serializer, generated Java artifact, C# live trace row, encrypted socket capture, or real-client runtime comparison has executed.
- Representative validator JSON is synthetic and does not prove Java can emit the row shape without altering behavior.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java instrumentation design/runbook rows for `FindGroupService.addRecruitment/addApplication` mutation-post trace emission, including where to emit rows without changing Java map/send ordering.

Safe candidates:

- Add a file/directory report service for generated action `2`/`6` Java trace artifacts once artifact paths are chosen.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2173

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactValidatorServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2173-Completion.md`
- `docs/Phase-6-Session-2173-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2173] Add find group mutation Java trace artifact validator
```
