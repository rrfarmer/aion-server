# Phase 6 Session 2250 Completion - Explicit Root Java Capture Dry Run

## Scope

Added a non-live explicit-root Java capture dry-run report for `CM_FIND_GROUP` action `2`/`6` mutation-post artifacts.

Java source reviewed:

- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactWriter.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureArtifactValidator.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactFileReportService.cs`

## Changes

Added `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService`.

The report records:

- The narrow Java selector `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts`.
- The exact Maven command for an intentional explicit-root capture run.
- The selected artifact root and expected action `2`/`6` artifact file paths.
- Capture flag, deterministic timestamp, and artifact-root command-property gates.
- The focused C# artifact validator command to run after generation.
- A repository-root block so the dry-run report does not authorize normal repository artifact output.
- Runtime comparison and verified parity as still blocked.

Updated `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the Java runtime trace artifact evidence chain names the new explicit-root dry-run report.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched. Current working context remains in latest completion/handoff docs.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.
- Targeted Java fixture command execution against a temporary artifact root.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 14, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation:

```powershell
$artifactRoot = Join-Path $env:TEMP ('aion-find-group-explicit-root-' + [guid]::NewGuid().ToString('N'))
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Daion.findGroupMutationPost.artifactRoot=$artifactRoot" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: Maven build success; tests run 1, failures 0, errors 0, skipped 0. The command wrote only these two temp-root artifacts, then the temp root was cleaned up:

- `cm-find-group-direct-mutation-post-boundary-action-2-java.json`
- `cm-find-group-direct-mutation-post-boundary-action-6-java.json`

The Maven run emitted existing Java `sun.misc.Unsafe` terminal-deprecation warnings from the fixture's test-side object construction.

Broad-validation trigger: none. Full `.NET` build/suite was skipped because the filtered C# command built the affected project and dependencies, and the targeted Java command validated the exact explicit-root fixture method.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService` | Java Capture Command Metadata | Partial | Unit Tested; Java Targeted Test | Partial Parity | Records and validates the exact focused Maven selector for explicit-root action `2`/`6` artifact generation. The Java method passed against a temp root and wrote the two expected files. Does not execute C# live boundary capture or compare Java/C# runtime rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureInMemoryArtifactBridge` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService` | Explicit Root Artifact Handoff Metadata | Partial | Unit Tested; Java Targeted Test | Partial Parity | Documents the guarded `aion.findGroupMutationPost.artifactRoot` property path and blocks repository-root output in C# metadata. Generated shape-valid Java artifacts still are not live C# rows or runtime comparison evidence. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_TemporaryRootNamesFocusedJavaCommandAndAcceptanceGates` | Unit | Java fixture method and explicit-root artifact bridge | The dry-run report names the single-method Maven selector, capture flag, deterministic timestamp, explicit root, expected files, validator command, and blocked runtime comparison. | Non-live command metadata tied to Java fixture source. | Does not execute Maven inside the C# test. |
| `Create_DefaultRepositoryRootBlocksIntentionalCaptureCommand` | Unit | Java artifact writer default root and guarded explicit-root path | Repository artifact root is rejected for intentional dry-run capture command authorization. | Guardrail metadata. | Does not inspect filesystem state. |
| `Create_MissingRootBlocksBeforeCommandCanBeUsed` | Unit | Java bridge requires nonblank artifact-root property | Missing root blocks command use. | Guardrail metadata. | Does not execute Java. |
| `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` | Java Targeted Test | Java test-side fixture, bridge, writer, validator, production hooks | With capture enabled and explicit artifact root supplied, Java drains in-memory action `2`/`6` rows, writes the two expected files, validates shape, and keeps runtime comparison false. | Targeted Java fixture evidence. | Still fixture/test-side artifact generation only; no C# live boundary rows or comparison. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader`

## Summary Metrics

- Java artifacts reviewed: 5
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves safe Java artifact generation workflow and adds targeted Java fixture evidence, but it does not add live C# runtime parity evidence.

## Next Recommended UOW

Add a C# post-capture validator summary that consumes a supplied explicit artifact root and reports whether the Java generated files are present and shape-valid while still blocking runtime comparison until accepted live C# boundary rows exist.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.
