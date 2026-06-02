# Phase 6 Session 2262 Handoff - Focused Validation Rules

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2262-Completion.md`
- `docs/Phase-6-Session-2262-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

## Current State

UOW-2262 was documentation-only. It tightened Phase 6 testing guidance so future sessions avoid slow full `.NET` project tests, solution tests, and full solution builds unless a documented broad-validation trigger applies.

The current implementation state remains the state from UOW-2261:

- Non-live projected-value row shape metadata exists.
- Non-live projected-value materialization blocker reporting exists.
- Result emission remains blocked.
- Runtime-backed Java artifacts, accepted live C# boundary rows, concrete runtime row values, reader invocation, row identity decisions, value comparison, materialization, emission, runtime comparison, and live dispatch remain incomplete.

## Files Changed In Last Session

Updated:

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`

Added:

- `docs/Phase-6-Session-2262-Completion.md`
- `docs/Phase-6-Session-2262-Handoff.md`

## Validation From Last Session

Focused hygiene validation:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing documentation files, but no whitespace errors.

Focused C# validation was not run because UOW-2262 was documentation-only and did not change C# source, tests, generated artifacts, scripts, fixtures, or run commands.

Focused Java/Maven validation was not run because no Java source or Java fixtures changed.

Broad `.NET` validation was skipped because there was no broad-validation trigger. Full project tests, solution tests, and solution builds are not appropriate for documentation-only guidance changes.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before every validation run.

Required rule for future handoffs:

- Include the next UOW's exact focused validation recipe.
- Include the expected Java/Maven command or explicit skip rationale.
- Include the broad-validation trigger as `none` unless a specific trigger applies.
- If the focused recipe is slow, narrow it before broadening it.
- Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies.
- Do not follow a passing filtered test with a full solution build just to confirm compilation.

## Next Recommended UOW

Add a non-live projected-value result-emission blocker report that consumes `FindGroupMutationPostProjectedValueMaterializationBlockerReportService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService`, then records why each output kind still cannot be emitted after materialization remains blocked.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedValueMaterializationBlockerReportService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorEvidenceSummaryContractService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live emission blocker report that joins materialization blockers to result-emission gate conditions without emitting results.
- Update runtime evidence checklist/design docs only if the new report becomes a provider or explicitly changes the evidence chain.
- Update completion/handoff docs and commit.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueResultEmissionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorEvidenceSummaryContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

If that command is slow, first narrow to:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedValueResultEmissionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedValueMaterializationBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests" --no-restore
```

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Add a focused evidence-summary update that includes the projected-value materialization blocker report.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, non-live Java/C# row-pairing readiness, a value-projection handoff gate, runtime-row-value intake metadata, typed-reader implementation function planning, reader function invocation preflight metadata, projected-value row shape metadata, and projected-value materialization blockers, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
