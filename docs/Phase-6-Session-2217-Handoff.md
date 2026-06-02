# Phase 6 Session 2217 Handoff - Focused Validation Policy Tightening

Date: 2026-06-02
Unit of Work: UOW-2217
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Session startup is not a validation trigger. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit.

Use focused validation by default:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single service/planner/schema/test change: run filtered `dotnet test` for that test class plus directly adjacent classes only.
- Packet/parser or boundary change: run only the immediately related packet/parser/boundary tests.
- Java parity check: run a targeted Maven test only when a narrow Java fixture or source-of-truth command exists.
- Full project test, solution test, or solution build: run only after documenting a broad-validation trigger from `docs/orchestration-rules.md`.

Filtered `dotnet test` commands already build the affected project and dependencies. Treat a passing filtered test command as the compile signal unless a named broad-validation trigger requires wider validation.

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService` consumes paired-readiness rows and emits blocked planned rows for missing Java input, missing C# input, or deferred value comparison.
- `FindGroupMutationPostProjectedRowComparisonValueContractService` names required Java/C# value sources for every required equality field and keeps runtime-only fields as ignored context.
- `FindGroupMutationPostProjectedRowComparisonBlockedResultReportService` combines the executor skeleton and value contract into a final non-live pre-execution report with all planned output rows marked unavailable.
- `FindGroupMutationPostProjectedRowComparisonReadinessSummaryService` links the dry-run, executor skeleton, value contract, and blocked-result report into one top-level readiness summary.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2217 Summary

This UOW made the validation policy more explicit and easier to carry forward:

- startup and handoff review are not broad-validation triggers,
- documentation-only units use `git diff --check`,
- narrow service/test units use filtered `dotnet test`,
- packet/parser/boundary units use only the directly related tests,
- broad .NET validation requires a documented trigger.

No Java or C# runtime behavior changed.

## Java Artifacts Reviewed

- None. Documentation-only validation policy update.

## C# Artifacts Reviewed

- None. Documentation-only validation policy update.

## Files Changed

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2217-Completion.md`
- `docs/Phase-6-Session-2217-Handoff.md`

## Validation In UOW-2217

Validation decision:

- Changed surface: documentation-only instruction updates.
- Focused C# command: not run; no C# source, test, generated artifact, script, or test input changed.
- Focused Java/Maven command: not run; no Java source, Java fixture, or Java behavior changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. Full .NET tests/builds are not applicable to documentation-only edits and would directly contradict this UOW's policy.
- Why this scope is sufficient: `git diff --check` verifies repository patch hygiene for the documentation-only changes.

Result:

- `git diff --check`: passed.

## Migration Parity Table

No Java or C# runtime artifacts were touched in this documentation-only unit.

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| N/A | N/A | Documentation / Validation Policy | N/A | Manual Only | N/A | No Java behavior or C# runtime behavior changed. This unit only updates validation selection rules. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Readiness summary rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- Future sessions still need to choose focused validation commands after Work Discovery rather than defaulting to broad validation.
- Broad validation remains appropriate when a documented broad-validation trigger applies or the user explicitly asks for it.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison live-input handoff contract that enumerates the exact runtime artifacts still required to move from summary metadata to real comparison execution.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2217] Tighten focused validation policy
```
