# Phase 6 Session 2260 Handoff - Projected Value Row Contract

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2260-Completion.md`
- `docs/Phase-6-Session-2260-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

## Current State

UOW-2260 added a non-live projected-value row contract for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2260-Completion.md`

The projected-value row contract consumes the value-reader function execution preflight, executor implementation plan, and typed-reader preflight. It emits 42 per-field row shapes: 38 required equality rows with `<not-read>` values and 4 ignored runtime-context rows with `<ignored-runtime-context>` values.

It does not execute `ProcessPacketAsync`, send packets, invoke readers, read Java JSON values, read C# trace-export values, compare rows, attach context, materialize output, emit results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests|FullyQualifiedName~FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 37, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused hygiene validation passed:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing files, but no whitespace errors.

Java/Maven was not run because no Java source or fixture changed in UOW-2260. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Projected-value row contract changes: run the edited projected-value row contract test class plus function execution preflight, executor implementation plan, result schema, materialization preflight, result-emission gate, and runtime evidence checklist tests.
- Value-reader function execution preflight changes: run the edited preflight test class plus typed-reader implementation gate, comparator preflight, executor readiness gate, executor implementation plan, executor runtime-evidence intake, and runtime evidence checklist tests.
- Runtime-row-value intake gate changes: run the edited gate test class plus value-projection handoff, value-reader preflight/skeleton/blocked-result, and runtime evidence checklist tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add a non-live projected-value materialization blocker report that consumes `FindGroupMutationPostValueReaderProjectedValueRowContractService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService`, then records why each output kind still cannot materialize from the unread projected-value rows.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live blocker report that joins projected-value row readiness to output materialization requirements without reading values or emitting results.
- Run filtered C# tests for the new report plus projected-value row contract, materialization preflight, blocked-output preview, result-emission gate, and runtime evidence checklist tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Add a focused runtime evidence checklist update for projected-value row contract placement in value projection and result emission readiness.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, and projected-value row shape metadata, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
