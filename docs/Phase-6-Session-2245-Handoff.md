# Phase 6 Session 2245 Handoff - Value Reader Executor Capture Acceptance Matrix

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2245-Completion.md`
- `docs/Phase-6-Session-2245-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is now historical archive material; current working state should live in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2245 added a non-live acceptance matrix for the future `CM_FIND_GROUP` action `2`/`6` value-reader executor path.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The matrix currently blocks all evidence fields:

- Java artifact rows
- Java artifact shape validation
- C# boundary rows
- Boundary executor observation
- Registry send observation
- Row identity matching
- Value projection
- Result materialization
- Result emission
- Runtime comparison execution
- Executable implementation gate

This is intentional. The implementation is non-live metadata only and does not prove runtime parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 19, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed.

`git diff --check` passed with the repository's usual CRLF warnings.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a broad trigger is present.

Use focused `.NET` test filters for the changed test class plus directly adjacent service tests. A filtered `dotnet test` is an acceptable compile signal for the affected project and dependencies.

Run Java/Maven tests only when Java source/fixtures changed or when a narrow Java source-of-truth command is needed for the UOW.

Use a full `.NET` build or full `.NET` test suite only when one of these broad triggers applies:

- Shared project configuration changed.
- A shared service, packet reader/writer primitive, entity mapping primitive, or test infrastructure primitive changed.
- The UOW touches multiple unrelated feature areas.
- Focused validation exposes a failure that may be caused by broader project state.
- A release/merge gate explicitly requires broad validation.

When broad validation is skipped, document the reason in the completion file.

## Next Recommended UOW

Add a non-live value-reader executor capture execution blocker summary that rolls the acceptance matrix into a single go/no-go report for runtime comparison execution and names the smallest next evidence-producing command.

Suggested scope:

- Read the latest startup docs listed above.
- Inspect Java action `2`/`6` source and C# value-reader executor metadata services.
- Add a small service that consumes the acceptance matrix and emits a one-record execution decision.
- Include the exact minimal next evidence command or artifact root requirement in the contract.
- Add focused tests for default preflight-blocked and runtime-evidence-missing states.
- Update session docs and commit.

Safe alternate candidates:

- Add a deterministic Java artifact timestamp override for `serverEpochSeconds`.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.

## Parity Caution

Current status remains partial parity only. Verified parity is still blocked by missing live Java artifacts, missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, and no executable value-reader implementation.
