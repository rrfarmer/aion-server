# Phase 6 Session 2290 Handoff - Command Decision Summary Evidence

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2290-Completion.md`
- `docs/Phase-6-Session-2290-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2290 surfaced capture execution blocker summary row evidence inside the value-reader executor capture command decision report.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`

The capture command decision report rows now include `captureExecutionBlockerSummaryRows`, preserving blocker summary row evidence that includes capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor evidence bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff metadata where available.

No Java source, fixtures, live dispatch, packet sends, reader invocation, value reads, runtime comparison, capture execution, executable implementation, materialization, result emission, or verified parity status changed.

## Java Source Context

Reviewed source-of-truth Java:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Relevant Java facts:

- Action `2` reads `playerOrTeamId`, `message`, `groupType`, then calls `FindGroupService.addRecruitment(player, message, groupType)`.
- Action `6` reads `playerOrTeamId`, `message`, `groupType`, `classId`, `level`, then calls `FindGroupService.addApplication(player, message, groupType, classId, level)`.
- `addRecruitment` sends posted system message `1400392` before refreshed `SM_FIND_GROUP` action `0`.
- `addApplication` sends posted system message `1400393` before refreshed `SM_FIND_GROUP` action `4`.
- Both mutation-post actions use direct sends, with zero world broadcasts and zero invite dispatches expected.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: final run passed 8, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2290; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed capture command decision report plus the directly adjacent capture execution blocker summary.

Commit made:

```text
[Phase 6][UOW-2290] Surface summary evidence in command decision
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Capture execution blocker summary changes: run capture execution blocker summary tests plus directly adjacent capture acceptance matrix tests.
- Capture command decision report changes: run capture command decision report tests plus directly adjacent capture execution blocker summary tests.
- Capture command consistency report changes: run capture command consistency report tests plus directly adjacent capture command decision report tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Surface capture command decision row evidence inside the value-reader executor capture command consistency report. Keep the unit metadata-only: the report should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReport`, but its evidence can preserve command-decision rows that now include capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update capture command consistency report metadata and tests.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests" --no-restore
```

If that command is slow, first narrow to only `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests`.

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Surface command-consistency evidence inside the explicit-root Java capture dry-run report if the consistency report already carries the required command-decision evidence.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Review checklist strings for readability only if future tests/handoffs become hard to maintain.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates with required field metadata, accepted-boundary-row handoff metadata visible in checklist inventory, Java/C# row-pairing readiness that consumes that handoff, runtime checklist row identity metadata that names that consumer relationship, value-projection handoff metadata that preserves row-pairing handoff evidence, runtime-row-value intake metadata that preserves value-projection handoff row evidence, typed-reader implementation readiness metadata that preserves runtime-row-value intake row evidence, value-reader invocation preflight metadata that preserves typed-reader gate row evidence, projected-value row metadata that preserves function preflight row evidence, materialization blocker metadata that preserves projected-value row evidence, result-emission blocker metadata that preserves materialization blocker evidence, executor evidence bridge metadata that preserves result-emission blocker row evidence, projected-value executor consistency audit metadata that preserves executor evidence bridge row evidence, runtime-comparison handoff metadata that preserves executor consistency audit row evidence, live-capture preflight runbook metadata that preserves runtime-comparison handoff row evidence, capture acceptance matrix metadata that preserves live-capture preflight row evidence, capture execution blocker summary metadata that preserves capture acceptance matrix row evidence, capture command decision metadata that preserves capture execution blocker summary row evidence, checklist visibility for that command-decision gate, and clearer observation blocker wording, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
