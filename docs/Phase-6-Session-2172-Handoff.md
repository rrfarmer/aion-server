# Phase 6 Session 2172 Handoff - FindGroup Mutation Java Trace Artifact Schema

Date: 2026-06-02
Unit of Work: UOW-2172
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, and mutation-post Java trace artifact schema target exist as non-live readiness artifacts.

## UOW-2172 Summary

This UOW added `FindGroupMutationPostJavaTraceArtifactSchemaReportService`, a blocked Java trace artifact schema target for action `2`/`6` mutation-post traces.

The schema target:

- Reuses trace name `cm-find-group-direct-mutation-post-boundary`.
- Reuses schema version `1`.
- Reuses the C# mutation-post boundary comparison field order.
- Records action `2` mapping to Java `FindGroupService.addRecruitment(player, message, groupType)`, posted message id `1400392`, refreshed show-list action `0`.
- Records action `6` mapping to Java `FindGroupService.addApplication(player, message, groupType, classId, level)`, posted message id `1400393`, refreshed show-list action `4`.
- Keeps all fields/actions blocked pending Java instrumentation and a Java trace serializer.

No Java artifacts were generated and no live C# trace was captured.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReport`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactFieldRow`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactActionRow`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactInstrumentationCaveat`
- `Aion.GameServer.Tests.FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests`

## Validation In UOW-2172

Validation decision:

- Changed surface: focused production Java trace artifact schema target service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests|FullyQualifiedName~FindGroupRuntimeComparisonPreflightContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed. This UOW defines the future Java trace artifact schema target from reviewed Java source; no Java instrumentation or executable fixture exists yet.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new schema target plus adjacent runtime preflight and mutation-post comparison schema surfaces, and the filtered command builds the affected project/dependencies.

Result:

- Passed: 11
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService` | Java Trace Artifact Schema Target | Blocked | Unit Tested | Partial Parity | Action `2`/`6` mutation-post Java trace artifact schema target is represented and tied to the C# comparison schema, but no Java instrumentation, generated Java artifact, live C# trace capture, or live `ProcessPacketAsync` execution has occurred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Java Trace Artifact Schema Target / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java mutation, posted system message ordering, refreshed show-list ordering, and post-mutation visible ids are named as artifact fields and action mappings. No Java artifact, live registry send, socket comparison, or runtime trace has executed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- The Java trace artifact schema target is non-live contract metadata; no Java instrumentation, Java serializer, generated Java artifact, C# live trace row, encrypted socket capture, or real-client runtime comparison has executed.
- The schema target does not prove Java trace rows can be emitted without changing Java behavior.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a Java trace artifact validator for the action `2`/`6` mutation-post schema target, still without enabling live C# dispatch.

Safe candidates:

- Add Java instrumentation design/runbook rows for `FindGroupService.addRecruitment/addApplication` if validator scope is too large.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2172

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactSchemaReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaTraceArtifactSchemaReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2172-Completion.md`
- `docs/Phase-6-Session-2172-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2172] Add find group mutation Java trace artifact schema
```
