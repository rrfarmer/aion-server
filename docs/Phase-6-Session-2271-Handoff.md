# Phase 6 Session 2271 Handoff - Observation Blocker Wording

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2271-Completion.md`
- `docs/Phase-6-Session-2271-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2271 tightened observation blocker wording in command-decision metadata.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests.cs`

The command-decision report now distinguishes:

- Boundary executor observation: guarded boundary invoked the side-effect executor after packet acceptance.
- Registry send observation: posted system message and refreshed `SM_FIND_GROUP` list were observed through registry sends in Java order.

No Java source, fixtures, live dispatch, packet sends, runtime comparison, capture execution, executable implementation, or verified parity status changed.

## Java Source Context

Reviewed source-of-truth Java:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Relevant Java facts:

- `addRecruitment` records recruitment mutation and posted-message hooks, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_OFFER_PARTY_POSTED()`, then calls `showRecruitments(player)`.
- `showRecruitments` records refreshed-list hook and sends `SM_FIND_GROUP(0, recruitments)`.
- `addApplication` records application mutation and posted-message hooks, sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()`, then calls `showApplications(player)`.
- `showApplications` records refreshed-list hook and sends `SM_FIND_GROUP(4, applications)`.

## Validation From Last Session

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests" --no-restore
```

Result: passed 8, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2271; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed command-decision report plus the directly adjacent blocker summary.

Commit made:

```text
[Phase 6][UOW-2271] Clarify observation blocker wording
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Command-decision wording changes: run the command-decision tests plus capture execution blocker summary tests.
- Runtime evidence checklist changes: run the edited checklist tests plus directly adjacent command-decision and command-consistency tests when command provider inventory changes.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Begin the next safe evidence step by adding a non-live handoff/checklist row that defines the exact accepted C# boundary row fields required before any guarded live boundary capture can be consumed by runtime comparison. Keep the unit metadata-only unless a current handoff explicitly authorizes capture execution.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should define/clarify accepted C# boundary row evidence fields only.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
- Review checklist strings for excessive length only if a future focused test or handoff becomes hard to read.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, projected-value materialization blockers, projected-value result-emission blockers, executor evidence bridge metadata, projected-value executor consistency audit metadata, a runtime-comparison handoff gate for that consistency audit, capture acceptance visibility for that gate, command-decision metadata for the next evidence command, checklist visibility for that command-decision gate, and clearer observation blocker wording, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
