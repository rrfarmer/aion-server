# Phase 6 Session 2193 Handoff - FindGroup Mutation Java Trace Serializer Scaffold

Date: 2026-06-02
Unit of Work: UOW-2193
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
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, direct show-list trace schema/projection, mutation-post scaffold/schema/projection, mutation-post Java artifact schema/validator/instrumentation/file/directory/runbook, Java capture fixture scaffold, Java test-side instrumentation scaffold, Java test-side serializer scaffold, C# trace-emitter design, C# live trace-row fixture plan, registry observation, key projection, artifact comparison preflight, trace-row readiness aggregate, comparison contracts/envelope/blockers/dry-run/result skeleton, and Java artifact capture implementation readiness exist as non-live readiness artifacts.

## UOW-2193 Summary

This UOW added `FindGroupMutationPostTraceCaptureSerializer`, a test-side Java in-memory serializer scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace artifacts.

The scaffold:

- is gated by `aion.findGroupMutationPost.capture` through `trySerializeArtifact`,
- writes no files,
- uses top-level `schemaVersion`, `traceName`, and `traces`,
- preserves the C# schema-v1 22-field row order,
- emits Java trace rows only,
- maps action `2` to `Recruitment`, system message id `1400392`, refreshed list action `0`,
- maps action `6` to `Application`, system message id `1400393`, refreshed list action `4`,
- rejects unsupported mutation-post actions and invalid row mappings.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInstrumentation`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureSerializer`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactSchemaReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`

No C# code changed in this UOW.

## Validation In UOW-2193

Validation decision:

- Changed surface: focused Java test-side serializer scaffold plus non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW; reviewed C# schema/validator contracts only.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because no broad trigger applied and no C# runtime surface changed.
- Why this scope is sufficient: the focused Maven command compiles the new Java serializer scaffold and proves the fixture can exercise the schema order, capture gate, and Java-derived action mappings without touching unrelated Java tests or broad .NET validation.

Result:

- Java/Maven: passed 13, failed 0, skipped 0.
- Existing unrelated Java `Unsafe`/deprecation warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService` | Test-Side Java Serializer Scaffold | Partial | Unit Tested | Partial Parity | The scaffold serializes Java-labeled schema-v1 rows for future action `2`/`6` mutation-post traces and preserves the reviewed C# field order, but production `CM_FIND_GROUP` does not call hooks and no runtime row is captured. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`; `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Mutation-Post Row Serializer Scaffold | Partial | Unit Tested | Partial Parity | The scaffold preserves Java-derived action mappings for `addRecruitment` and `addApplication` posted-message/refreshed-list rows, but production hook integration, generated artifacts, artifact validation from disk, live C# rows, registry observation, and comparison execution are missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Production Java hook integration, Java artifact file writing, generated Java artifacts, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java serializer scaffold emits in-memory fixture rows only; it is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add capture-gated Java artifact writer scaffolding that can write the serializer output to the stable action `2`/`6` artifact paths from the fixture only, while still avoiding production Java hooks and documenting that generated rows are fixture rows rather than runtime parity evidence.

Safe candidates:

- Add Java fixture helper methods for deterministic payload/scenario construction while keeping runtime execution disabled.
- Add production hook integration only after deciding exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed In UOW-2193

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2193-Completion.md`
- `docs/Phase-6-Session-2193-Handoff.md`

## Commit

Commit message:

```text
[Phase 6][UOW-2193] Add find group mutation Java trace serializer scaffold
```
