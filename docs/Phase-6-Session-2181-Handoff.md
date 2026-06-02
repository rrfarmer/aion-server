# Phase 6 Session 2181 Handoff - FindGroup Mutation Artifact Comparison Preflight

Date: 2026-06-02
Unit of Work: UOW-2181
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, and mutation-post artifact comparison preflight exist as non-live readiness artifacts.

## UOW-2181 Summary

This UOW added `FindGroupMutationPostArtifactComparisonPreflightService`, a guarded aggregate for future action `2`/`6` mutation-post Java/C# artifact comparison readiness.

The preflight records:

- expected Java artifact targets,
- generated Java artifact reader status,
- live C# trace-row availability,
- comparison key projection availability,
- live registry-observation availability,
- comparison execution and projected-row match result.

The default report remains blocked on missing generated Java artifacts. Shape-valid Java artifacts move the blocker forward only to missing live C# rows; live rows still require registry observation and comparison execution. Verified parity is not claimed.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`
- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightReport`
- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightRow`
- `Aion.GameServer.Tests.FindGroupMutationPostArtifactComparisonPreflightServiceTests`

## Validation In UOW-2181

Validation decision:

- Changed surface: focused non-live artifact comparison preflight service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactFileReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live preflight metadata UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new preflight and the immediate Java artifact target/reader, key projection, and registry-observation dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 26
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Runtime Comparison Preflight | Blocked | Unit Tested | Partial Parity | Action `2`/`6` comparison gates are aggregated, but generated Java rows, live C# rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService` | Mutation-Post Artifact Comparison Preflight | Partial | Unit Tested | Partial Parity | The preflight preserves Java mutation-before-posted-message-before-refreshed-list requirements and action-specific ids, but it performs no row comparison and proves no runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Artifact comparison preflight exists, but generated Java artifacts, live C# runtime rows, registry observation, and deterministic comparison are missing.
- Java instrumentation, serializer, generated artifacts, artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a generated Java artifact capture runbook or fixture-plan metadata for action `2`/`6` mutation-post traces, naming the exact Java hook, serializer shape, artifact paths, and focused Maven command once a narrow Java fixture exists.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2181

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostArtifactComparisonPreflightService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostArtifactComparisonPreflightServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2181-Completion.md`
- `docs/Phase-6-Session-2181-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2181] Add find group mutation artifact comparison preflight
```
