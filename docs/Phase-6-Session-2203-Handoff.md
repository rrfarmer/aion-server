# Phase 6 Session 2203 Handoff - FindGroup Mutation Java Repository Trace Artifacts

Date: 2026-06-02
Unit of Work: UOW-2203
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
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Broad .NET validation is not routine. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts now exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Shape-valid Java artifacts are not verified parity. Live C# rows, registry observation, projected-row comparison, and live dispatch remain missing.

## UOW-2203 Summary

This UOW added an explicit command-gated repository artifact capture path and checked in generated Java action `2`/`6` mutation-post fixture artifacts.

Repository artifacts:

```text
parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-2-java.json
parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-6-java.json
```

Important notes:

- Default focused Maven validation skips the repository-write test when `aion.findGroupMutationPost.artifactRoot` is missing.
- Intentional capture must pass an absolute repository-root artifact path. A relative path writes beneath the Surefire module working directory.
- `serverEpochSeconds` is runtime/context fixture data and must not be treated as deterministic parity proof.
- C# focused reader/preflight tests prove shape validity and gate behavior only, not live runtime parity.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInMemoryArtifactBridge`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureArtifactWriter`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureArtifactValidator`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`
- `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService`

## Files Changed

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-2-java.json`
- `parity-artifacts/find-group/mutation-post/java/cm-find-group-direct-mutation-post-boundary-action-6-java.json`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2203-Completion.md`
- `docs/Phase-6-Session-2203-Handoff.md`

## Validation In UOW-2203

Validation decision:

- Changed surface: Java test-only artifact capture hook plus repository JSON fixture artifacts and non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostArtifactComparisonPreflightServiceTests" --no-restore
```

- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Intentional artifact capture command:

```powershell
$artifactRoot = Join-Path (Get-Location) 'parity-artifacts\find-group\mutation-post\java'
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.artifactRoot=$artifactRoot" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the Java commands prove default no-write behavior and intentional guarded artifact generation; the focused C# command proves the repository artifacts are accepted by the artifact reader/preflight surface.

Result:

- Java/Maven default focused command: passed 29, failed 0, skipped 1.
- Java/Maven intentional capture command: passed 30, failed 0, skipped 0.
- C# focused artifact reader/preflight command: passed 19, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched Java/documentation files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`; `Aion.GameServer.Services.FindGroupMutationPostArtifactComparisonPreflightService` | Java Fixture Test / Artifact Capture | Partial | Unit Tested | Partial Parity | Default Maven skips repository writes unless an explicit artifact root is supplied. Intentional capture produced shape-valid Java action `2`/`6` artifacts and focused C# readers accepted them. This is Java fixture/runtime-prep evidence only; live C# rows, registry observation, comparison execution, and live dispatch remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInMemoryArtifactBridge` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Test Fixture Bridge | Partial | Unit Tested | Partial Parity | Guarded artifact-root property can write expected repository files when explicitly supplied. File paths must use an absolute repository-root path to avoid Surefire module-local output. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks` | `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`; future C# live trace row emitter | Hook Utility | Partial | Unit Tested | Partial Parity | Java in-memory rows can produce shape-valid artifacts. Runtime fields such as `serverEpochSeconds` are context fields and are not parity proof; C# comparison metadata must keep runtime-only fields out of exact key comparison until live traces exist. |
| Repository artifacts under `parity-artifacts/find-group/mutation-post/java` | `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService` | Golden Fixture Artifact | Partial | Unit Tested | Partial Parity | Files are shape-valid action `2`/`6` Java fixture artifacts. They are not verified parity because no C# live row or projected-row comparison exists. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Shape-valid Java fixture artifacts are not Java/C# runtime comparison evidence.
- Runtime/context field `serverEpochSeconds` is not deterministic parity evidence.
- Live C# mutation-post rows, registry observation, projected-row comparison, comparison execution, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a focused C# live trace-row fixture or non-live row-emitter test surface for action `2`/`6` mutation-post rows, consuming the checked-in Java artifacts only as shape-valid Java input while keeping runtime comparison blocked until registry observation exists.

Safe candidates:

- Add an artifact-backed comparison preflight assertion that repository Java artifacts satisfy the Java-reader gate while live C# rows and registry observation remain blocking.
- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.

## Commit

Commit message:

```text
[Phase 6][UOW-2203] Add find group mutation Java trace artifacts
```
