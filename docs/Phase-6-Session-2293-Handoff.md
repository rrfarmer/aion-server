# Phase 6 Session 2293 Handoff - Dry-Run Consistency Evidence

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2293-Completion.md`
- `docs/Phase-6-Session-2293-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. Before running validation, name the specific behavior, packet shape, metadata contract, or documentation invariant being checked. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active notes already name a broad-validation trigger, focused evidence of wider risk, an explicit user request, or a release/readiness checkpoint.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2293 surfaced capture command consistency evidence inside the explicit-root Java capture dry-run command report.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`

The dry-run report now includes `CommandConsistencyEvidence`, preserving command consistency status, provider consistency rows, selected command-decision metadata, and command-decision row evidence that includes capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff metadata where available.

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

Specific behavior/contract validated:

- The explicit-root Java capture dry-run command report preserves the command consistency evidence chain without enabling Java capture, C# live capture, runtime comparison, executable implementation, or verified parity.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2293; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed explicit-root dry-run command report plus the directly adjacent capture command consistency report.

Commit made:

```text
[Phase 6][UOW-2293] Surface consistency evidence in dry-run report
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Capture command consistency report changes: run capture command consistency report tests plus directly adjacent capture command decision report tests.
- Explicit-root Java capture dry-run report changes: run explicit-root Java capture dry-run command report tests plus directly adjacent capture command consistency report tests.
- Explicit-root Java post-capture validator summary changes: run explicit-root Java post-capture validator summary tests plus directly adjacent explicit-root Java capture dry-run command report tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Surface explicit-root Java capture dry-run evidence inside the explicit-root Java post-capture validator summary. Keep the unit metadata-only: the summary should continue consuming `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReport`, but preserve dry-run command consistency evidence so post-capture artifact validation summaries carry the command gate and blocker evidence that led to the capture command.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update explicit-root Java post-capture validator summary metadata and tests.
- Update completion/handoff docs and commit.

Specific behavior/contract for the next UOW:

- The explicit-root Java post-capture validator summary preserves dry-run command consistency evidence while continuing to block runtime comparison and verified parity until accepted live C# boundary rows and runtime comparison evidence exist.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests" --no-restore
```

If that command is slow, first narrow to only `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests`.

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Review checklist strings for readability only if future tests/handoffs become hard to maintain.
- Continue surfacing existing evidence into downstream runtime-comparison planning reports, provided each unit remains metadata-only and has a focused test recipe.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates with required field metadata, accepted-boundary-row handoff metadata visible in checklist inventory, Java/C# row-pairing readiness that consumes that handoff, runtime checklist row identity metadata that names that consumer relationship, value-projection handoff metadata that preserves row-pairing handoff evidence, runtime-row-value intake metadata that preserves value-projection handoff row evidence, typed-reader implementation readiness metadata that preserves runtime-row-value intake row evidence, value-reader invocation preflight metadata that preserves typed-reader gate row evidence, projected-value row metadata that preserves function preflight row evidence, materialization blocker metadata that preserves projected-value row evidence, result-emission blocker metadata that preserves materialization blocker evidence, executor evidence bridge metadata that preserves result-emission blocker row evidence, projected-value executor consistency audit metadata that preserves executor evidence bridge row evidence, runtime-comparison handoff metadata that preserves executor consistency audit row evidence, live-capture preflight runbook metadata that preserves runtime-comparison handoff row evidence, capture acceptance matrix metadata that preserves live-capture preflight row evidence, capture execution blocker summary metadata that preserves capture acceptance matrix row evidence, capture command decision metadata that preserves capture execution blocker summary row evidence, capture command consistency metadata that preserves capture command decision row evidence, and explicit-root Java capture dry-run metadata that preserves command consistency evidence, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
