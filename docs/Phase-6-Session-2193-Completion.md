# Phase 6 Session 2193 Completion - FindGroup Mutation Java Trace Serializer Scaffold

Date: 2026-06-02
Unit of Work: UOW-2193
Status: Completed

## Scope

This unit added a test-side Java in-memory serializer scaffold for future `CM_FIND_GROUP` action `2` and `6` mutation-post trace artifacts.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInstrumentation.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

C# schema/validator source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactSchemaReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`

This UOW does not edit production Java source, does not call hooks from `CM_FIND_GROUP` or `FindGroupService`, does not write artifact files, does not generate artifact-backed Java rows, does not compare Java/C# rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureSerializer` under `game-server/test/com/aionemu/gameserver/services/findgroup`.
- The serializer scaffold:
  - uses schema version `1`,
  - uses trace name `cm-find-group-direct-mutation-post-boundary`,
  - emits `traceSource` as `Java`,
  - preserves the 22-field C# mutation-post schema order,
  - serializes a top-level `schemaVersion`, `traceName`, and `traces` array shape expected by the C# Java-artifact validator,
  - maps action `2` to `Recruitment`, posted system message id `1400392`, and refreshed list action `0`,
  - maps action `6` to `Application`, posted system message id `1400393`, and refreshed list action `4`,
  - rejects unsupported mutation-post actions,
  - refuses non-Java trace source/name/version and nonzero broadcast/invite side-effect rows,
  - no-ops through `trySerializeArtifact` unless `aion.findGroupMutationPost.capture` is enabled.
- Extended `FindGroupMutationPostTraceCaptureTest` with focused serializer tests for field order, capture gating, recruitment/application action mappings, unsupported action rejection, and no artifact writer.
- Updated live-dispatch design notes to record the serializer scaffold while keeping production Java hooks, generated artifacts, live C# rows, and comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused Java test-side serializer scaffold plus non-live design/session documentation.
- Focused C# command: not run. No C# code or C# test changed in this UOW; reviewed C# schema/validator contracts only.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, production Java source, or broad behavior surface changed.
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

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.serializerScaffoldListsStableSchemaFieldsInComparisonOrder` | Unit | C# mutation-post schema contract and Java action `2`/`6` review | Serializer field order matches the required 22 schema fields. | Focused Java fixture assertion against reviewed C# schema field order. | No runtime rows. |
| `FindGroupMutationPostTraceCaptureTest.serializerNoOpsWhenCaptureFlagIsDisabled` | Unit | Capture-gated Java fixture design | Serializer returns no artifact text when the capture flag is absent. | Focused capture-gate assertion. | No artifact writer. |
| `FindGroupMutationPostTraceCaptureTest.serializerEmitsRecruitmentRowWhenCaptureFlagIsEnabledWithoutWritingArtifacts` | Unit | `FindGroupService.addRecruitment` review | Action `2` row emits Java trace source, `Recruitment`, `1400392`, refreshed action `0`, visible ids, and zero broadcast/invite counts in order. | Focused Java source-derived mapping assertion. | No production hook calls or generated file. |
| `FindGroupMutationPostTraceCaptureTest.serializerEmitsApplicationActionMappingWithoutWritingArtifacts` | Unit | `FindGroupService.addApplication` review | Action `6` row emits `Application`, `1400393`, refreshed action `4`, visible ids, and false live executor/registry observations. | Focused Java source-derived mapping assertion. | No production hook calls or generated file. |
| `FindGroupMutationPostTraceCaptureTest.serializerRejectsUnsupportedMutationPostAction` | Unit | `CM_FIND_GROUP.runImpl` action `2`/`6` mutation-post scope review | Serializer sample row factory rejects unsupported action `3`. | Focused guard assertion. | Other actions remain outside this serializer scope. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 production artifacts and 3 Java test scaffold artifacts
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 production artifacts plus Java scaffold artifacts
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Production Java hook integration, generated Java artifacts, artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- The Java serializer scaffold is test-side and in-memory only; it is not runtime parity evidence.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add capture-gated Java artifact writer scaffolding that can write the serializer output to the stable action `2`/`6` artifact paths from the fixture only, while still avoiding production Java hooks and documenting that generated rows are fixture rows rather than runtime parity evidence.

Safe candidates:

- Add Java fixture helper methods for deterministic payload/scenario construction while keeping runtime execution disabled.
- Add production hook integration only after deciding exact no-op hook placement and proving it does not alter `PacketSendUtility` ordering.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2193-Completion.md`
- `docs/Phase-6-Session-2193-Handoff.md`
