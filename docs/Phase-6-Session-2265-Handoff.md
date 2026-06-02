# Phase 6 Session 2265 Handoff - Projected Value Executor Consistency Audit

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2265-Completion.md`
- `docs/Phase-6-Session-2265-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless a broad-validation trigger from `docs/orchestration-rules.md` is named before the command runs.

## Current State

UOW-2265 added a non-live projected-value executor consistency audit for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueExecutorConsistencyAuditService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests.cs`

Updated adjacent artifact:

- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The audit consumes materialization blockers, result-emission gate rows, result-emission blockers, executor evidence summary metadata, and the executor evidence bridge. It records whether those non-live reports are internally consistent before implementation readiness or runtime-comparison handoff proceeds.

It does not execute `ProcessPacketAsync`, send packets, invoke readers, read Java JSON values, read C# trace-export values, compare rows, attach context, materialize output, emit results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorEvidenceBridgeServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueResultEmissionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorEvidenceSummaryContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 34, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2265. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

Commit made:

```text
[Phase 6][UOW-2265] Add find group executor consistency audit
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Projected-value executor consistency audit changes: run the edited audit test class plus executor evidence bridge, result-emission blocker, materialization blocker, result-emission gate, evidence summary, and runtime evidence checklist tests.
- Projected-value executor evidence bridge changes: run the edited bridge test class plus result-emission blocker, evidence summary, implementation readiness audit, and runtime evidence checklist tests.
- Projected-value result-emission blocker report changes: run the edited blocker report test class plus projected-value materialization blocker, result-emission gate, executor evidence bridge, evidence summary, and runtime evidence checklist tests.
- Evidence summary or implementation readiness changes: run the edited class plus executor evidence bridge, consistency audit, and runtime evidence checklist tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add a focused runtime-comparison handoff update that consumes the executor consistency audit before any capture execution blocker or implementation readiness handoff can proceed.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueExecutorConsistencyAuditService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueExecutorEvidenceBridgeService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add the consistency audit as an explicit provider/blocker in the next handoff-facing metadata slice if that slice consumes implementation/runtime-comparison readiness.
- Keep the unit non-live unless a separate broad-validation trigger is documented before implementation.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorEvidenceBridgeServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueExecutorConsistencyAuditServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a documentation-only cleanup that confirms future sessions should continue using focused test recipes from handoff docs instead of broad `.NET` validation.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, projected-value materialization blockers, projected-value result-emission blockers, executor evidence bridge metadata, and projected-value executor consistency audit metadata, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
