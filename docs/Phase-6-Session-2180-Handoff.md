# Phase 6 Session 2180 Handoff - FindGroup Mutation Comparison Key Projection Metadata

Date: 2026-06-02
Unit of Work: UOW-2180
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post runtime fixture contract, mutation-post Java trace artifact schema target, mutation-post Java trace artifact validator target, mutation-post Java instrumentation design target, mutation-post Java artifact file target report, mutation-post Java artifact directory reader, mutation-post C# trace-emitter design report, mutation-post runtime comparison readiness aggregate, mutation-post registry-observation trace contract, and mutation-post comparison key-projection metadata exist as non-live readiness artifacts.

## UOW-2180 Summary

This UOW added `FindGroupMutationPostComparisonKeyProjectionMetadataService`, a conservative non-live contract for future action `2`/`6` mutation-post Java/C# trace row comparison keys.

The metadata defines:

- compatibility gates: `schemaVersion`, `traceName`,
- row identity: `action`, `mutationKind`, `activePlayerObjectId`, `mutatedEntryObjectId`,
- equality projection fields for mutation state, direct packet shape, registry observation, and side-effect guards,
- runtime-only ignored fields: `traceSource`, raw `serverEpochSeconds`.

Action-specific Java evidence captured:

- action `2`: recruitment mutation, `SmSystemMessage` id `1400392`, then `SmFindGroup` action `0`.
- action `6`: application mutation, `SmSystemMessage` id `1400393`, then `SmFindGroup` action `4`.

The metadata remains blocked and non-live until generated Java rows, live C# rows, registry observation, and comparison execution exist.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadata`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionFieldRow`
- `Aion.GameServer.Tests.FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests`

## Validation In UOW-2180

Validation decision:

- Changed surface: focused non-live comparison key-projection metadata service, focused tests, and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests|FullyQualifiedName~FindGroupMutationPostRegistryObservationTraceContractServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeComparisonReadinessReportServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, no Java instrumentation/serializer exists, no generated Java artifacts exist, and no narrow Java fixture exists for this non-live projection metadata UOW.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new key-projection metadata and the immediate mutation-post schema, registry-observation contract, and runtime-readiness dependencies; the filtered command builds the affected project/dependencies.

Result:

- Passed: 21
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService` | Runtime Comparison Key Projection Metadata | Blocked | Unit Tested | Partial Parity | Action `2`/`6` comparison keys are named, but generated Java rows, live C# rows, and runtime comparison are missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupMutationPostRegistryObservationTraceContractService` | Mutation-Post Comparison Projection | Partial | Unit Tested | Partial Parity | The projection preserves Java mutation-before-posted-message-before-refreshed-list facts and action-specific message/list ids, while ignoring source/runtime-only fields that cannot prove equality. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Comparison key-projection metadata exists, but generated Java artifacts, live C# runtime rows, registry observation, and deterministic comparison are missing.
- Java instrumentation, serializer, generated artifacts, artifact-backed comparison, encrypted socket capture, and registry-send observation are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add Java artifact comparison preflight metadata tying expected action `2`/`6` Java files, C# live rows, key projection, and blocked comparison execution into one guarded contract.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.
- Add a live direct-packet boundary test or trace for action `0`/`4` before mutation actions if a lower-risk live observation path is preferred.

## Files Changed In UOW-2180

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonKeyProjectionMetadataService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostComparisonKeyProjectionMetadataServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2180-Completion.md`
- `docs/Phase-6-Session-2180-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2180] Add find group mutation comparison key projection metadata
```
