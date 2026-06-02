# Phase 6 Session 2292 Handoff - Focused Validation Rules

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2292-Completion.md`
- `docs/Phase-6-Session-2292-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. Before running validation, name the specific behavior, packet shape, metadata contract, or documentation invariant being checked. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active notes already name a broad-validation trigger, focused evidence of wider risk, an explicit user request, or a release/readiness checkpoint.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2292 tightened documentation rules for focused validation.

Updated artifacts:

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`
- `docs/parity-verification.md`

The docs now require every Unit of Work to state the specific behavior or contract under validation before selecting commands. Full `.NET` project tests, solution tests, and solution builds remain exceptional and require an explicit exception reason before execution.

No Java source, C# product code, C# tests, fixtures, generated artifacts, scripts, live dispatch, packet sends, runtime comparison, capture execution, or verified parity status changed.

## Validation From Last Session

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for existing docs.

Focused C# validation: not run. This was a documentation-only unit and no generated artifacts, C# source, C# tests, fixtures, or scripts changed.

Focused Java/Maven validation: not run. No Java source or fixtures changed.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because runtime validation is not applicable to this documentation-only unit.

Commit to make:

```text
[Phase 6][UOW-2292] Tighten focused validation rules
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For future units, write this decision before commands:

```text
Validation decision:
- Changed surface:
- Specific behavior/contract:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

Full `.NET` project tests, solution tests, and solution builds require a documented exception reason before execution. If the command feels slow or overly broad, narrow the filter first and document residual risk.

## Next Recommended UOW

Surface capture command consistency evidence inside the explicit-root Java capture dry-run command report. Keep the unit metadata-only: the dry-run report should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReport`, but its evidence can preserve consistency evidence that now includes command-decision rows, capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update explicit-root Java capture dry-run report metadata and tests.
- Update completion/handoff docs and commit.

Specific behavior/contract for the next UOW:

- The explicit-root Java capture dry-run command report preserves the command consistency evidence chain without enabling Java capture, C# live capture, runtime comparison, executable implementation, or verified parity.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests" --no-restore
```

If that command is slow, first narrow to only `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests`.

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Surface explicit-root dry-run evidence inside the explicit-root Java post-capture validator summary if the dry-run report already carries the required consistency evidence.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Review checklist strings for readability only if future tests/handoffs become hard to maintain.

## Parity Caution

Current status remains partial parity only. UOW-2292 improved validation policy but added no runtime evidence. Verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
