# Phase 6 Session 2258 Handoff - Value Reader Function Execution Preflight

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2258-Completion.md`
- `docs/Phase-6-Session-2258-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2258 added a non-live value-reader function execution preflight for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderFunctionExecutionPreflightService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2258-Completion.md`

The new preflight consumes typed-reader implementation gate and comparator preflight metadata. It names invocation preconditions for planned Java/C# reader functions against paired runtime rows.

It does not execute `ProcessPacketAsync`, send packets, invoke readers, read Java JSON values, read C# trace-export values, project values, compare rows, attach context, emit results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 35, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused hygiene validation passed:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing files, but no whitespace errors.

Java/Maven was not run because no Java source or fixture changed in UOW-2258. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Value-reader function execution preflight changes: run the edited preflight test class plus typed-reader implementation gate, comparator preflight, executor readiness gate, executor implementation plan, executor runtime-evidence intake, and runtime evidence checklist tests.
- Typed value-reader implementation gate changes: run the edited gate test class plus runtime-row-value intake, value-reader preflight, implementation readiness checklist, implementation runbook, comparator preflight, and runtime evidence checklist tests.
- Runtime-row-value intake gate changes: run the edited gate test class plus value-projection handoff, value-reader preflight/skeleton/blocked-result, and runtime evidence checklist tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add a non-live value-reader projected-value row contract that consumes `FindGroupMutationPostValueReaderFunctionExecutionPreflightService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService`, then defines the shape of per-field projected Java/C# value rows without invoking readers, comparing values, or emitting results.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostValueReaderFunctionExecutionPreflightService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live contract that names projected-value row fields such as action, mutation kind, field name, Java value, C# value, value type, read status, and blocker.
- Run filtered C# tests for the new contract plus function execution preflight, executor implementation plan, result schema, materialization preflight, and result-emission gate tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, and reader function invocation preflight metadata, but verified parity is still blocked by missing runtime-backed Java artifacts, missing actual accepted live C# boundary rows from production dispatch, missing runtime row values, concrete typed value-reader implementation, reader invocation, value projection/materialization/emission evidence, runtime comparison execution, and live dispatch.
