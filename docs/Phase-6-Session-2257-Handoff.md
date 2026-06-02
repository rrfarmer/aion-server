# Phase 6 Session 2257 Handoff - Typed Value Reader Implementation Readiness Gate

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2257-Completion.md`
- `docs/Phase-6-Session-2257-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2257 added a non-live typed value-reader implementation readiness gate for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostTypedValueReaderImplementationReadinessGateService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2257-Completion.md`

The new gate consumes runtime-row-value intake, implementation runbook, and typed-reader preflight metadata. It names planned Java/C# reader functions for int scalar, bool scalar, ordered int-list, string scalar, enum string, and mismatch-context families.

It does not execute `ProcessPacketAsync`, send packets, implement readers, read Java JSON values, read C# trace-export values, materialize comparison results, execute runtime comparison, enable live dispatch, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostTypedValueReaderImplementationReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostRuntimeRowValueEvidenceIntakeGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 34, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2257. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Typed value-reader implementation gate changes: run the edited gate test class plus runtime-row-value intake, value-reader preflight, implementation readiness checklist, implementation runbook, comparator preflight, and runtime evidence checklist tests.
- Runtime-row-value intake gate changes: run the edited gate test class plus value-projection handoff, value-reader preflight/skeleton/blocked-result, and runtime evidence checklist tests.
- Value-projection handoff gate changes: run the edited gate test class plus row-pairing readiness, value contract, value-reader preflight/readiness, and runtime evidence checklist tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Add a non-live value-reader function execution preflight that consumes `FindGroupMutationPostTypedValueReaderImplementationReadinessGateService` and `FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService`, then records the exact preconditions for invoking the planned reader functions against paired runtime Java/C# rows while keeping invocation, projection, comparison, and result emission disabled.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostTypedValueReaderImplementationReadinessGateService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorReadinessGateService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live gate/report that names invocation preconditions for planned reader functions.
- Run filtered C# tests for the new gate plus typed-reader implementation gate, comparator preflight, executor readiness gate, executor implementation plan, and runtime-evidence intake tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, and typed-reader implementation function planning, but verified parity is still blocked by missing runtime-backed Java artifacts, missing actual accepted live C# boundary rows from production dispatch, missing runtime row values, concrete typed value-reader implementation, value projection/materialization/emission evidence, runtime comparison execution, and live dispatch.
