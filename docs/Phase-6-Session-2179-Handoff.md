# Phase 6 Session 2179 Handoff - FindGroup Mutation Registry Observation Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2179
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, and mutation-post registry-observation trace contract exist as non-live readiness artifacts.

## UOW-2179 Summary

This UOW added `FindGroupMutationPostRegistryObservationTraceContractService`, a conservative contract for action `2`/`6` live-boundary registry observation evidence.

The contract requires:

- live `CmFindGroup` boundary executor invocation,
- accepted boundary trace,
- direct registry send #1 to the active player with the Java posted system message,
- direct registry send #2 to the active player with the refreshed `SM_FIND_GROUP` list,
- posted-message-before-refreshed-list ordering,
- zero world broadcasts,
- zero invite dispatches,
- schema-v1 runtime trace fields.

Action-specific Java evidence captured:

- action `2`: `SmSystemMessage` id `1400392`, then `SmFindGroup` action `0`.
- action `6`: `SmSystemMessage` id `1400393`, then `SmFindGroup` action `4`.

The contract remains blocked and non-live until actual registry-send observation and runtime trace rows exist.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService`
- `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContract`
- `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationRequirementRow`
- `Aion.GameServer.Tests.FindGroupMutationPostRegistryObservationTraceContractServiceTests`

## Validation In UOW-2179

Validation decision:

- Changed surface: focused non-live registry-observation trace contract service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpTraceEmitterDesignReportServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live contract metadata UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new registry-observation contract and the immediate mutation-post scaffold, trace schema, C# emitter design, and runtime-readiness dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 24
- Failed: 0
- Skipped: 0

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | Live Boundary Registry Observation Contract | Blocked | Unit Tested | Partial Parity | Action `2`/`6` registry observation evidence is named, but no live boundary invocation or connection-registry sends have been observed. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupMutationPostRuntimeComparisonReadinessReportService` | Registry Observation / Direct Packet Readiness | Partial | Unit Tested | Partial Parity | The contract preserves Java mutation-before-posted-message-before-refreshed-list ordering and action-specific message/list ids. It does not prove runtime parity. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Registry-observation trace contract exists, but live boundary invocation, connection-registry send observation, live runtime rows, and deterministic comparison are missing.
- Java instrumentation, serializer, generated artifacts, artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add comparison key-projection metadata for mutation-post action `2`/`6` rows after actual Java/C# trace row shapes exist.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2179

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostRegistryObservationTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostRegistryObservationTraceContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2179-Completion.md`
- `docs/Phase-6-Session-2179-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2179] Add find group mutation registry observation contract
```
