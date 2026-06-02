# Phase 6 Session 2184 Handoff - FindGroup Mutation Trace Row Readiness Aggregate

Date: 2026-06-02
Unit of Work: UOW-2184
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, mutation-post artifact comparison preflight, mutation-post Java artifact capture runbook, mutation-post C# live trace-row fixture plan, and mutation-post trace-row readiness aggregate exist as non-live readiness artifacts.

## UOW-2184 Summary

This UOW added `FindGroupMutationPostTraceRowReadinessAggregateService`, a non-live aggregate for future action `2`/`6` mutation-post trace row readiness.

The aggregate combines:

- Java artifact capture runbook readiness,
- C# live trace-row fixture plan readiness,
- registry observation contract readiness,
- artifact comparison preflight readiness.

It remains blocked by default, with missing Java capture inputs taking priority before C# live rows, registry observation, and comparison execution.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`
- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregate`
- `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessRow`
- `Aion.GameServer.Tests.FindGroupMutationPostTraceRowReadinessAggregateServiceTests`

## Validation In UOW-2184

Validation decision:

- Changed surface: focused non-live readiness aggregate service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTraceRowReadinessAggregateServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. The filtered `dotnet test` command built the affected project and dependencies.
- Why this scope is sufficient: the filtered tests cover the new aggregate and its immediate Java runbook, C# live row fixture plan, registry contract, and artifact comparison preflight dependencies without spending time on unrelated suites.

Result:

- Passed: 30
- Failed: 0
- Skipped: 0

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService` | Trace Row Readiness Aggregate | Blocked | Unit Tested | Partial Parity | The aggregate records action `2`/`6` mutation-post readiness inputs, but Java capture, C# live rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostTraceRowReadinessAggregateService`; `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Mutation-Post Trace Row Readiness | Partial | Unit Tested | Partial Parity | The aggregate preserves Java mutation-before-posted-message-before-refreshed-list requirements for action `2`/`6`, but it is non-live metadata and proves no runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Java fixture, Java instrumentation, Java serializer, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a comparison execution result contract for projected action `2`/`6` mutation-post rows, defining how differences will be represented without executing comparison until Java/C# rows exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2184

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTraceRowReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTraceRowReadinessAggregateServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2184-Completion.md`
- `docs/Phase-6-Session-2184-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2184] Add find group mutation trace row readiness aggregate
```
