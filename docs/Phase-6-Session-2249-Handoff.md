# Phase 6 Session 2249 Handoff - Capture Command Consistency Report

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2249-Completion.md`
- `docs/Phase-6-Session-2249-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2249 added a non-live capture-command consistency report for Java action `2`/`6` mutation-post value-reader capture commands.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2249-Completion.md`

The report is non-live. It audits command strings only and verifies that the Java artifact capture runbook, live-capture preflight runbook, capture execution blocker summary, and artifact-root validation command report all carry the same deterministic timestamp fragment:

```text
-Daion.findGroupMutationPost.serverEpochSeconds=1700000000
```

Root-aware providers are also checked for:

```text
-Daion.findGroupMutationPost.artifactRoot=<artifactRoot>
```

It does not run Maven, generate Java artifacts, execute C# live boundary capture, read projected values, materialize results, or compare Java/C# runtime rows.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 26, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2249.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

Use this default selection:

- C# non-live report/service change: run the edited test class plus directly adjacent command-provider/checklist tests with a filtered `dotnet test`.
- C# packet/parser/boundary change: add only immediately related packet/parser/boundary tests.
- Java source or fixture change: run the narrow Maven test for that fixture/source-of-truth behavior where available.
- Documentation-only change: run `git diff --check`; skip runtime tests unless docs changed generated artifacts, scripts, test inputs, or run commands.

Do not run unfiltered project tests, full solution tests, or full solution builds unless the active notes first name the broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a focused explicit-root Java capture dry-run command example/report that records the exact temporary-root command and acceptance gates for intentionally generating action `2`/`6` artifacts without touching repository artifact output.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
  - `FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
  - `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- Add a tiny non-live report or handoff command table for intentional explicit-root capture.
- Run filtered C# tests for the new report plus directly adjacent command-report tests.
- Run Java/Maven only if the Java fixture/source changes or if the unit intentionally executes the explicit-root capture command.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. Verified parity is still blocked by missing generated runtime-backed Java artifacts, missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
