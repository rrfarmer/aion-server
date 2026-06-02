# Phase 6 Session 2174 Handoff - FindGroup Mutation Java Instrumentation Design

Date: 2026-06-02
Unit of Work: UOW-2174
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, and mutation-post Java instrumentation design target exist as non-live readiness artifacts.

## UOW-2174 Summary

This UOW added `FindGroupMutationPostJavaInstrumentationDesignReportService`, a non-live report for where future Java action `2`/`6` mutation-post tracing should observe behavior.

The design points cover:

- `CM_FIND_GROUP.readImpl` action-specific payload capture,
- `CM_FIND_GROUP.runImpl` active-player boundary facts,
- action `2` recruitment mutation after `recruitments.put`,
- action `2` posted system message id `1400392`,
- action `2` refreshed `SM_FIND_GROUP` action `0` list send after Java `toList()`,
- action `6` application mutation after `applications.put`,
- action `6` posted system message id `1400393`,
- action `6` refreshed `SM_FIND_GROUP` action `4` list send after Java `toList()`,
- future trace artifact row serialization using `cm-find-group-direct-mutation-post-boundary` and `FindGroupMutationPostJavaTraceArtifactValidatorService`.

No Java instrumentation, serializer, artifact generation, or runtime comparison was added.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReport`
- `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationPoint`
- `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationCaveat`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaInstrumentationDesignReportServiceTests`

## Validation In UOW-2174

Validation decision:

- Changed surface: focused non-live Java instrumentation design report service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaInstrumentationDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation exists, and no narrow executable Java fixture or generated Java trace artifact exists for this design-only unit.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new design report and adjacent schema/validator surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 16
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService` | Java Instrumentation Design Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` payload and runImpl hook locations are documented for future Java trace emission, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaInstrumentationDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Java Instrumentation Design / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The report documents mutation-after-map-put, posted-message, and refreshed-list observation points for recruitment/application flows while preserving Java send ordering. It is design-only and does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java trace instrumentation design exists, but Java instrumentation, serializer, generated artifacts, and artifact-file discovery are missing.
- No live C# trace row, encrypted socket capture, registry-send observation, or real-client runtime comparison has executed.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a generated-artifact file/directory report service for action `2`/`6` Java mutation-post trace artifacts once artifact paths and naming are chosen, keeping it blocked until real Java artifacts exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a C# live trace-emitter design companion for action `2`/`6` mutation-post rows, still blocked until live boundary capture exists.

## Files Changed In UOW-2174

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaInstrumentationDesignReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaInstrumentationDesignReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2174-Completion.md`
- `docs/Phase-6-Session-2174-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2174] Add find group mutation Java instrumentation design
```
