# Phase 6 Session 2268 Handoff - Focused Validation Documentation

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2268-Completion.md`
- `docs/Phase-6-Session-2268-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2268 was documentation-only. It tightened testing guidance so future Phase 6 sessions use targeted evidence instead of routine 5-10 minute `.NET` runs.

Updated documents:

- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/csharp-port.md`
- `docs/Phase-6-Session-2268-Completion.md`
- `docs/Phase-6-Session-2268-Handoff.md`

`docs/PHASE-6-PROGRESS.md` remains untouched and should not be part of normal startup.

No Java source, C# source, fixtures, generated artifacts, scripts, executable run commands, live dispatch, runtime comparison, packet behavior, or verified parity status changed.

## Validation From Last Session

Documentation hygiene:

```powershell
git diff --check
```

Result: passed. Existing line-ending warnings only.

Focused C# validation was not applicable because this was a documentation-only UOW.

Java/Maven validation was not applicable because no Java source or fixture changed.

Broad `.NET` validation was skipped because there was no broad-validation trigger and runtime tests/full builds are not applicable for this documentation-only UOW.

Commit made:

```text
[Phase 6][UOW-2268] Tighten focused validation guidance
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

Rules to apply before every test command:

- Start with the edited test class or documentation hygiene command.
- Add only the nearest adjacent contract/checklist classes that prove the changed behavior.
- Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies.
- If the focused recipe is slow, narrow the filter before running it.
- Do not run unfiltered project tests, full solution tests, or full solution builds unless the active notes already name a broad-validation trigger.
- For docs-only units, run `git diff --check`; runtime tests and full builds are not applicable unless the docs changed generated artifacts, scripts, fixtures, or executable run commands.

## Next Recommended UOW

Add a small command-decision report that consumes the capture execution blocker summary and explicitly chooses the next focused evidence command (`executorConsistencyAuditAccepted` first, then Java capture only after consistency is accepted). This keeps future sessions from jumping directly to Java/Maven capture while upstream consistency blockers remain visible.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should report commands only and must not run Java capture or C# capture.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Continue documentation-only cleanup only if the latest completion/handoff docs are missing startup-critical context.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, projected-value materialization blockers, projected-value result-emission blockers, executor evidence bridge metadata, projected-value executor consistency audit metadata, a runtime-comparison handoff gate for that consistency audit, and capture acceptance visibility for that gate, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
