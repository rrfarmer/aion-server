# Phase 6 Session 2262 Completion - Focused Validation Rules

## Scope

Tightened Phase 6 validation guidance so future sessions use specific focused test targets instead of slow full `.NET` project tests, solution tests, or full solution builds by default.

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in latest completion/handoff documents.

## Changes

Updated:

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`

Added:

- `docs/Phase-6-Session-2262-Completion.md`
- `docs/Phase-6-Session-2262-Handoff.md`

The shared orchestration rules now require every handoff to include the next UOW's exact focused validation recipe, including filtered C# test class names or the documentation hygiene command. The rules also require Java/Maven expectations, broad-validation trigger status, and residual-risk documentation when a focused command is narrowed further.

The C# port startup instructions now reiterate that full `.NET` project tests, solution tests, and solution builds are exceptional checks for documented broad-validation triggers, focused evidence that exposes wider risk, explicit user requests, or release/readiness checkpoints.

## Validation Decision

Changed surface:

- Documentation-only migration orchestration guidance.
- Latest completion/handoff session documentation.

Focused C# validation:

- Not applicable. No C# source, C# tests, generated artifacts, scripts, fixtures, or run commands changed.

Focused Java/Maven validation:

- Not applicable. No Java source or Java fixtures changed.

Focused hygiene validation:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing documentation files, but no whitespace errors.

Broad-validation trigger: none.

Broad `.NET` decision: skipped. Documentation-only guidance changes do not justify runtime tests, unfiltered project tests, full solution tests, or full solution builds.

## Parity Table Updates

No Java or C# parity artifacts changed in this UOW. No parity status changed.

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| N/A | N/A | Documentation | N/A | Manual Only | N/A | Documentation-only testing policy update. No Java behavior, C# behavior, packet shape, parser behavior, runtime state, or live dispatch changed. |

## Test Documentation

No runtime tests were added or changed.

Documentation hygiene check:

| Check Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `git diff --check` | Hygiene | N/A | Confirms documentation edits have no whitespace errors. | No parity evidence; docs-only hygiene. | Does not validate runtime behavior, by design. |

## Summary Metrics

- Java artifacts reviewed: 0
- C# artifacts changed: 0
- Documentation files changed/added: 4
- Verified parity rows added: 0
- Partial parity rows added: 0
- Estimated Phase 6 completion: unchanged. This UOW only reduces future validation overhead and context load.

## Next Recommended UOW

Add a non-live projected-value result-emission blocker report that consumes `FindGroupMutationPostProjectedValueMaterializationBlockerReportService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService`, then records why each output kind still cannot be emitted after materialization remains blocked.
