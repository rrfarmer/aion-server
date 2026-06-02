# Phase 6 Session 2186 Handoff - FindGroup Mutation Comparison Input Envelope

Date: 2026-06-02
Unit of Work: UOW-2186
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, mutation-post artifact comparison preflight, mutation-post Java artifact capture runbook, mutation-post C# live trace-row fixture plan, mutation-post trace-row readiness aggregate, mutation-post comparison execution result contract, and mutation-post comparison input envelope exist as non-live readiness artifacts.

## UOW-2186 Summary

This UOW added `FindGroupMutationPostComparisonInputEnvelopeService`, a guarded non-live envelope for future action `2`/`6` Java/C# mutation-post row comparison inputs.

The envelope collects:

- shape-valid Java row references,
- live C# row references,
- comparison projection metadata,
- trace-row readiness aggregate,
- comparison result contract.

It remains blocked by default. C# rows count only when they include boundary acceptance, executor invocation, and registry-send observation; disabled sample projections are not accepted as live rows.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelope`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeGateRow`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeRowReference`
- `Aion.GameServer.Tests.FindGroupMutationPostComparisonInputEnvelopeServiceTests`

## Validation In UOW-2186

Validation decision:

- Changed surface: focused non-live comparison input envelope service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist for row comparison.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new envelope and its immediate Java artifact reader, readiness, preflight, and result-contract dependencies without spending time on unrelated suites.

Result:

- Passed: 29
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService` | Comparison Input Envelope | Blocked | Unit Tested | Partial Parity | The envelope defines future comparison inputs for action `2`/`6`, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostComparisonInputEnvelopeService`; `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`; `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Mutation-Post Comparison Input Envelope | Partial | Unit Tested | Partial Parity | The envelope preserves Java mutation-before-posted-message-before-refreshed-list row references and action-specific packet ids, but it only guards inputs and proves no runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a guarded comparison execution blocker report for action `2`/`6` mutation-post rows that consumes the input envelope and explains why comparison is not executed until all gates are ready.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2186

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonInputEnvelopeService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonInputEnvelopeServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2186-Completion.md`
- `docs/Phase-6-Session-2186-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2186] Add find group mutation comparison input envelope
```
