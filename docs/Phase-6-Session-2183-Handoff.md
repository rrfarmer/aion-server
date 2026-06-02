# Phase 6 Session 2183 Handoff - FindGroup Mutation CSharp Live Trace Row Fixture Plan

Date: 2026-06-02
Unit of Work: UOW-2183
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, mutation-post comparison key-projection metadata, mutation-post artifact comparison preflight, mutation-post Java artifact capture runbook, and mutation-post C# live trace-row fixture plan exist as non-live readiness artifacts.

## UOW-2183 Summary

This UOW added `FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`, a non-live plan for future action `2`/`6` C# live trace-row capture.

The plan records:

- planned fixture class `GameServerConnectionFindGroupMutationPostLiveTraceRowFixture`,
- live-dispatch guard preserving disabled production `CmFindGroup` dispatch,
- boundary acceptance trace fields,
- shared singleton mutation trace fields,
- direct packet intent trace fields,
- boundary executor evidence,
- registry-send observation evidence,
- runtime row serialization fields,
- artifact comparison preflight input.

The plan remains blocked until live boundary fixture, live emitter, registry observation, generated Java artifacts, and comparison execution exist.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlan`
- `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixtureStep`
- `Aion.GameServer.Tests.FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests`

## Validation In UOW-2183

Validation decision:

- Changed surface: focused non-live C# live trace-row fixture plan service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java fixture/instrumentation/serializer exists, and no generated Java artifacts exist.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new fixture plan and the immediate C# emitter design, registry contract, trace schema, and artifact preflight dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 27
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService` | C# Live Trace Row Fixture Plan | Blocked | Unit Tested | Partial Parity | The planned action `2`/`6` live row fixture is named, but live boundary fixture, live emitter, generated Java rows, registry observation, and comparison execution are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveTraceRowFixturePlanService`; `Aion.GameServer.Services.FindGroupMutationPostCSharpTraceEmitterDesignReportService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | C# Live Trace Row Fixture Plan / Registry Observation | Partial | Unit Tested | Partial Parity | The plan preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific packet ids, but it is design metadata only and does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- C# live trace-row fixture plan exists, but live boundary fixture, live emitter, generated Java artifacts, registry observation, and deterministic comparison are missing.
- Java fixture, Java instrumentation, Java serializer, artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a mutation-post trace row readiness aggregate that combines the Java capture runbook, C# live trace-row fixture plan, registry observation contract, and artifact comparison preflight into one conservative blocked report.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Implement the targeted Java/Maven fixture only when ready to edit Java source and capture artifacts.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2183

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2183-Completion.md`
- `docs/Phase-6-Session-2183-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2183] Add find group mutation CSharp live trace row fixture plan
```
