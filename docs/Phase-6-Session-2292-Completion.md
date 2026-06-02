# Phase 6 Session 2292 Completion - Focused Validation Rules

## Scope

Tightened the Phase 6 testing policy so future sessions choose specific validation commands instead of routine full `.NET` project tests, solution tests, or solution builds.

This was a documentation-only Unit of Work. `docs/PHASE-6-PROGRESS.md` was intentionally not read or updated; current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`
- `docs/parity-verification.md`

The durable startup and verification docs now require each Unit of Work to name the specific behavior, packet shape, metadata contract, or documentation invariant under validation before choosing commands. Full `.NET` project tests, solution tests, and solution builds now require an explicit exception reason in the active completion/handoff notes before execution: a broad-validation trigger, focused evidence of wider risk, an explicit user request, or a release/readiness checkpoint.

No Java source, C# product code, C# tests, fixtures, generated artifacts, scripts, runtime behavior, packet sends, live dispatch, reader invocation, value reads, materialization, result emission, runtime comparison, capture execution, or verified parity status changed.

## Validation Decision

Changed surface:

- Documentation-only orchestration, startup, and parity-verification rules.

Specific behavior/contract:

- Future Phase 6 sessions must select the narrowest command that proves a named behavior or contract and must not run full `.NET` project tests, solution tests, or solution builds unless an exception reason is documented before execution.

Focused C# command:

- Not applicable; no C# source, tests, fixtures, generated artifacts, or scripts changed.

Focused Java/Maven command:

- Not applicable; no Java source or fixtures changed.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for existing docs.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds are not applicable for this documentation-only unit.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| None | None | Documentation | N/A | Documentation Hygiene | N/A | Documentation-only testing-policy update. No Java or C# behavior changed and no parity status changed. |

## Test Documentation

No tests were added or changed.

## Summary Metrics

- Java artifacts reviewed: 0
- C# artifacts updated: 0
- Documentation files updated: 3
- Verified parity rows added: 0
- Partial parity rows updated: 0
- Blocked by missing evidence: unchanged from UOW-2291.
- Estimated Phase 6 completion: unchanged; this UOW improves validation targeting policy only.

## Commit

Commit message:

```text
[Phase 6][UOW-2292] Tighten focused validation rules
```

## Next Recommended UOW

Surface capture command consistency evidence inside the explicit-root Java capture dry-run command report. Keep the unit metadata-only: the dry-run report should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReport`, but it can preserve consistency evidence that now includes command-decision rows, capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.
