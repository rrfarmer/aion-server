# Phase 6 Session 2202 Completion - FindGroup Mutation Java Guarded Artifact Root Property

Date: 2026-06-02
Unit of Work: UOW-2202
Status: Completed

## Scope

This unit added a guarded Java test-side artifact-root property path for future intentional action `2`/`6` mutation-post artifact capture runs.

Java source reviewed:

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactWriter.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

This UOW does not write repository artifact files, does not run C# repository artifact validation, does not compare Java/C# live rows, and does not wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.ARTIFACT_ROOT_PROPERTY` with value `aion.findGroupMutationPost.artifactRoot`.
- Added `tryWriteDrainedRowsFromArtifactRootProperty()`.
- If the property is absent or blank, no files are written and buffered in-memory rows remain available.
- If the property is supplied, drained in-memory rows are written only beneath the supplied root through the existing fixture writer.
- Added focused JUnit coverage proving:
  - capture enabled plus missing artifact-root property writes nothing and keeps rows buffered,
  - capture enabled plus temp artifact-root property writes action `2`/`6` files only under that temp root,
  - the existing Java artifact validator accepts those files as shape-valid,
  - runtime comparison readiness remains false.
- Updated live-dispatch design notes to record the guarded artifact-root property while keeping repository artifact generation and comparison blocked.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: Java test-only fixture bridge plus non-live design/session documentation.
- Focused C# command: not run; no C# files changed.
- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. No production Java source, C# production code, live connection dispatch, live side effects, packet primitive, common runtime base, persistence, or shared infrastructure changed in this UOW.
- Broad .NET decision: skipped intentionally because no C# files changed and no broad trigger applied.
- Why this scope is sufficient: the targeted Maven command runs the fixture class that exercises the missing-property no-write guard, supplied-temp-root write path, existing Java artifact validation, and runtime-comparison blocked status.

Result:

- Java/Maven: passed 29, failed 0, skipped 0. Existing Java `Unsafe` and deprecation warnings were emitted from unrelated golden-test helpers and the focused fixture player builder.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched Java test files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInMemoryArtifactBridge` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; future C# artifact comparison chain | Test Fixture Bridge | Partial | Unit Tested | Partial Parity | Guarded artifact-root property can write shape-valid action `2`/`6` Java fixture artifacts only to the supplied root. Missing property writes nothing. Repository artifact generation, C# repository validation, live C# rows, registry observation, and comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Hook Utility | Partial | Unit Tested | Partial Parity | In-memory rows can now be bridged to fixture artifacts through an explicit root or guarded property path, but default artifact output remains disabled and no repository artifact evidence exists. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostTraceCaptureTest.guardedArtifactRootPropertyDoesNotWriteWhenPropertyIsMissing` | Java unit | Bridge/writer source review | Capture-enabled rows are not written when `aion.findGroupMutationPost.artifactRoot` is absent; rows remain buffered. | Focused Maven test. | No repository artifact generation or comparison. |
| `FindGroupMutationPostTraceCaptureTest.guardedArtifactRootPropertyWritesOnlyToSuppliedRoot` | Java unit | Bridge/writer/validator source review | Capture-enabled rows write action `2`/`6` artifacts only under the supplied temp root and validate as shape-valid with runtime comparison blocked. | Focused Maven test using JUnit temp directory. | Temp fixture files only; no repository artifacts, C# live rows, or comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4 Java fixture/hook artifacts
- Total artifacts ported in this UOW: 0 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 Java/C# trace-preparation artifacts
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary and 1 Java artifact capture/comparison chain
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Property-root temp fixture artifacts are not repository artifact evidence and are not Java/C# comparison evidence.
- Runtime-backed repository Java artifact generation, C# repository artifact validation from disk, C# live fixture, live emitter, registry observation, and deterministic comparison are missing.
- World-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Run an intentional Java artifact capture into `parity-artifacts/find-group/mutation-post/java` using the guarded artifact-root property, inspect the generated files, and only then add repository artifact files if they are shape-valid and clearly documented as Java fixture/runtime-prep evidence rather than Java/C# parity proof.

Safe candidates:

- Add C# repository artifact reader validation after intentional repository Java artifacts exist.
- Add a Java fixture test validating serializer output from in-memory rows without writing any files.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2202-Completion.md`
- `docs/Phase-6-Session-2202-Handoff.md`
