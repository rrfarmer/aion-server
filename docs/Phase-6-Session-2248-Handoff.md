# Phase 6 Session 2248 Handoff - Java Artifact Root Validation Command Report

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2248-Completion.md`
- `docs/Phase-6-Session-2248-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2248 added a non-live artifact-root validation command report for Java action `2`/`6` mutation-post trace artifacts.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The report is non-live. It reads current file status through `FindGroupMutationPostJavaTraceArtifactDirectoryReportService`, names the deterministic Java capture command for the selected artifact root, and names the focused C# validator command. It does not generate files, run validators itself, run Java capture, or execute runtime comparison.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 21, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2248.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

For C# non-live artifact-root/report changes, run the edited test class plus directly adjacent directory/validator/checklist tests with a filtered `dotnet test`.

Run Java/Maven only when Java source/fixtures change or when intentionally generating/checking Java artifacts. For Java capture fixture changes, use:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Use full `.NET` build/suite only when a broad-validation trigger from `docs/orchestration-rules.md` is explicitly documented first.

## Next Recommended UOW

Add a C# non-live report that records the deterministic timestamp property across all value-reader capture command providers and verifies the Java capture runbook, live-capture preflight, execution blocker summary, and artifact-root validation command report all agree on the same property/value.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
  - `FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`
- Add a small consistency report and focused tests.
- Run filtered C# tests for the new report plus the directly adjacent command providers.
- Skip Java/Maven unless Java code changes.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a command example for intentional explicit-root artifact capture using a temporary root and the deterministic timestamp property.

## Parity Caution

Current status remains partial parity only. Verified parity is still blocked by missing generated runtime-backed Java artifacts, missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
