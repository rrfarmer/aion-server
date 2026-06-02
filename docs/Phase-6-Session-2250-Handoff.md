# Phase 6 Session 2250 Handoff - Explicit Root Java Capture Dry Run

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2250-Completion.md`
- `docs/Phase-6-Session-2250-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2250 added a non-live explicit-root Java capture dry-run report for Java action `2`/`6` mutation-post artifact generation.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2250-Completion.md`

The report records the exact narrow Java command for intentional temp-root artifact generation:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Daion.findGroupMutationPost.artifactRoot=<temporary-artifact-root>" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

It blocks missing roots and the default repository artifact root. It does not run Maven from C#, execute C# live boundary capture, read projected values, materialize results, or compare Java/C# runtime rows.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 14, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation passed:

```powershell
$artifactRoot = Join-Path $env:TEMP ('aion-find-group-explicit-root-' + [guid]::NewGuid().ToString('N'))
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Daion.findGroupMutationPost.artifactRoot=$artifactRoot" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: Maven build success; tests run 1, failures 0, errors 0, skipped 0. The command wrote only the expected action `2` and action `6` JSON files under the temporary root, and that root was cleaned up.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

For this artifact family:

- C# command/report changes: run the edited test class plus directly adjacent command-provider/checklist tests with a filtered `dotnet test`.
- Java explicit-root capture command changes: run only `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` with a temporary artifact root.
- C# post-capture artifact-reader changes: run the edited test class plus `FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests`, `FindGroupMutationPostJavaTraceArtifactValidatorServiceTests`, and directly adjacent command-report tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a C# post-capture validator summary that consumes a supplied explicit artifact root and reports whether the Java generated files are present and shape-valid while still blocking runtime comparison until accepted live C# boundary rows exist.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
  - `FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`
  - `FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
  - `FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
  - `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- Add a small non-live summary over a supplied artifact root.
- Run filtered C# tests for the new summary plus directly adjacent directory/validator/command report tests.
- Run targeted Java/Maven only if generating explicit-root artifacts is part of the UOW evidence; otherwise document why Java/Maven is not needed.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The explicit-root Java fixture command can generate shape-valid Java artifacts in a temporary root, but verified parity is still blocked by missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
