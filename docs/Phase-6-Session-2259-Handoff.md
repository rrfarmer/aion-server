# Phase 6 Session 2259 Handoff - Focused Validation Documentation

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2259-Completion.md`
- `docs/Phase-6-Session-2259-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

## Current State

UOW-2259 was documentation-only. It optimized the Phase 6 testing guidance so ordinary sessions do not spend 5-10 minutes on full `.NET` validation when a focused command is enough.

Updated artifacts:

- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2258-Handoff.md`
- `docs/Phase-6-Session-2259-Completion.md`
- `docs/Phase-6-Session-2259-Handoff.md`

No Java source, C# source, C# tests, fixtures, scripts, generated artifacts, or executable run commands changed.

## Validation From Last Session

Focused hygiene validation passed:

```powershell
git diff --check
```

Runtime tests were not run because this was a documentation-only UOW. Java/Maven was not run because no Java source or fixture changed.

Broad `.NET` validation was skipped because there was no broad-validation trigger. Documentation-only changes do not require full project tests, solution tests, or full solution builds unless they alter generated artifacts, scripts, fixtures, executable run commands, or the user explicitly asks for broad validation.

## Focused Testing Rule

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

Startup and Work Discovery are not validation triggers. Do not run full `.NET` project tests, solution tests, or solution builds just because a new session started, a handoff was read, or a narrow service changed.

Before any expensive command, write this decision into the active completion/handoff draft:

- Changed surface.
- Exact focused C# command, Java/Maven command, or documentation hygiene command.
- Broad-validation trigger, or `none`.
- Broad `.NET` decision: skipped unless the trigger is named.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

If the first focused command is still expected to take several minutes, narrow the filter to the edited test class and the closest adjacent class first. A narrow command with documented residual risk is preferred over an unfiltered project test or full build used for reassurance.

## Next Recommended UOW

UOW-2260: Add a non-live value-reader projected-value row contract that consumes `FindGroupMutationPostValueReaderFunctionExecutionPreflightService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService`, then defines the shape of per-field projected Java/C# value rows without invoking readers, comparing values, or emitting results.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderFunctionExecutionPreflightService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
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
