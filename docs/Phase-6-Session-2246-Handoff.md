# Phase 6 Session 2246 Handoff - Value Reader Capture Execution Blocker Summary

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2246-Completion.md`
- `docs/Phase-6-Session-2246-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2246 added a non-live go/no-go summary for the future `CM_FIND_GROUP` action `2`/`6` value-reader runtime comparison path.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The new summary consumes:

- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService`
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService`

It keeps runtime comparison, executable implementation, live dispatch, and verified parity disabled. Its default smallest next evidence-producing command is the capture-enabled Java fixture command from the live-capture preflight runbook.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 14, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

Use focused `.NET` test filters for the changed test class plus directly adjacent service tests. A passing filtered `dotnet test` is the compile signal for the affected project and dependencies.

Run Java/Maven tests only when Java source/fixtures changed or when a narrow Java source-of-truth command is required for the UOW.

Use a full `.NET` build or full `.NET` test suite only when one of these broad triggers applies:

- Shared project configuration changed.
- A shared service, packet reader/writer primitive, entity mapping primitive, or test infrastructure primitive changed.
- The UOW touches multiple unrelated feature areas.
- Focused validation exposes a failure that may be caused by broader project state.
- A release/merge gate explicitly requires broad validation.

When broad validation is skipped, document the reason in the completion file.

## Next Recommended UOW

Add a deterministic Java artifact timestamp override for `serverEpochSeconds` in the action `2`/`6` capture fixture path so future runtime-backed artifact rows can avoid nondeterministic timestamp churn.

Suggested scope:

- Read the latest startup docs listed above.
- Inspect Java capture fixture/test-side row builders and serializers:
  - `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java`
  - `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`
  - `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- Add the smallest deterministic timestamp override that preserves Java production behavior and only affects capture fixture/test artifact generation.
- Run a targeted Maven command such as `mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Dsurefire.failIfNoSpecifiedTests=false"` if the Java fixture changes.
- Run focused C# artifact validator tests only if generated artifact shape or expected C# validation text changes.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow command-oriented artifact-root validation report that checks generated Java files without running broad tests.

## Parity Caution

Current status remains partial parity only. Verified parity is still blocked by missing live Java artifacts, missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
