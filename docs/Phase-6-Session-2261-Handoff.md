# Phase 6 Session 2261 Handoff - Projected Value Materialization Blockers

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2261-Completion.md`
- `docs/Phase-6-Session-2261-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

## Current State

UOW-2261 added a non-live projected-value materialization blocker report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueMaterializationBlockerReportService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2261-Completion.md`

The new report consumes projected-value row shape and materialization preflight metadata. It records why `Matched`, `MissingJavaRow`, `MissingCSharpRow`, `FieldMismatch`, and `IgnoredRuntimeContext` output cannot materialize while equality values are unread, missing-row decisions do not exist, context has no parent result, and runtime comparison evidence is absent.

It does not execute `ProcessPacketAsync`, send packets, invoke readers, read Java JSON values, read C# trace-export values, compare rows, attach context, materialize output, emit results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 31, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused hygiene validation passed:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing files, but no whitespace errors.

Java/Maven was not run because no Java source or fixture changed in UOW-2261. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Projected-value materialization blocker report changes: run the edited blocker report test class plus projected-value row contract, materialization preflight, blocked-output preview, result-emission gate, and runtime evidence checklist tests.
- Projected-value row contract changes: run the edited projected-value row contract test class plus function execution preflight, executor implementation plan, result schema, materialization preflight, result-emission gate, and runtime evidence checklist tests.
- Result emission gate changes: run result-emission gate plus materialization preflight, blocked-output preview, evidence summary, and runtime evidence checklist tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add a non-live projected-value result-emission blocker report that consumes `FindGroupMutationPostProjectedValueMaterializationBlockerReportService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService`, then records why each output kind still cannot be emitted after materialization remains blocked.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostProjectedValueMaterializationBlockerReportService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorEvidenceSummaryContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live emission blocker report that joins materialization blockers to result-emission gate conditions without emitting results.
- Run filtered C# tests for the new report plus materialization blocker report, result-emission gate, materialization preflight, evidence summary, and runtime evidence checklist tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Add a focused evidence-summary update that includes the projected-value materialization blocker report.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, and projected-value materialization blockers, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
