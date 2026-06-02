# Phase 6 Session 2266 Handoff - Runtime Handoff Consistency Audit Gate

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2266-Completion.md`
- `docs/Phase-6-Session-2266-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

## Current State

UOW-2266 added the projected-value executor consistency audit as an explicit prerequisite in the value-reader runtime-comparison handoff for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Updated C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`

Updated test artifacts:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`

Updated adjacent artifact:

- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The runtime-comparison handoff now has:

- `BlockedExecutorConsistencyAuditNotReady`
- `ExecutorConsistencyAudit`
- `BlockedExecutorConsistencyAuditNotReady` row status
- `HasExecutorConsistencyAudit`

Important implementation note: the handoff default path uses a local non-live consistency-audit blocker to avoid recursive construction through downstream capture/runbook metadata. Callers that need the full consistency audit must pass it explicitly.

It does not execute `ProcessPacketAsync`, send packets, invoke readers, read Java JSON values, read C# trace-export values, compare rows, attach context, materialize output, emit results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation initially failed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorEvidenceBridgeServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Initial result: failed with a test-host stack overflow caused by recursive provider construction. This was fixed by replacing the handoff's default full consistency-audit construction with a local non-live blocker.

Focused C# validation re-run passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorEvidenceBridgeServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 22, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Adjacent focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: passed 14, failed 0, skipped 0.

Java/Maven was not run because no Java source or fixture changed in UOW-2266. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test commands built the affected project and dependencies.

Commit made:

```text
[Phase 6][UOW-2266] Gate runtime handoff on executor consistency
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Runtime-comparison handoff changes: run the edited handoff test class plus executor consistency audit, executor evidence bridge, live-capture preflight, capture acceptance matrix, capture execution blocker summary, and runtime evidence checklist tests.
- Live-capture preflight changes: run the edited runbook test class plus runtime handoff, capture acceptance matrix, and capture execution blocker summary tests.
- Capture acceptance matrix or blocker summary changes: run the edited class plus live-capture preflight and runtime handoff tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add an explicit executor-consistency field to the live-capture preflight or capture acceptance matrix so capture execution blocker summaries can name the consistency audit as a visible acceptance blocker, not only as an upstream handoff status.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only expose the consistency audit as an acceptance/preflight blocker.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a documentation-only cleanup that confirms future sessions should continue using focused test recipes from handoff docs instead of broad `.NET` validation.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, projected-value materialization blockers, projected-value result-emission blockers, executor evidence bridge metadata, projected-value executor consistency audit metadata, and a runtime-comparison handoff gate for that consistency audit, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
