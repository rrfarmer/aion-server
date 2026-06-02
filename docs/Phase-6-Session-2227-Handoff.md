# Phase 6 Session 2227 Handoff - Focused Validation Documentation Optimization

Date: 2026-06-02
Unit of Work: UOW-2227
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

Do not run `dotnet build dotnetConversion\AionServer.slnx` after a passing filtered test command merely to confirm compilation.

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
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` enumerates concrete schema-v1 typed readers for future Java JSON and C# trace-export value projection without reading values.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2227 Summary

This UOW updated orchestration documentation to make focused validation the durable default.

Key behavior:

- full .NET tests/builds are not routine session health checks,
- a passing filtered `dotnet test` command is the compile signal for affected project/dependency scope,
- unfiltered project tests, full solution tests, and full solution builds require a named broad-validation trigger in the active docs,
- documentation-only units should use `git diff --check` and skip runtime tests unless docs modify generated artifacts, scripts, test inputs, or run commands.

Important notes:

- This was documentation-only.
- No Java source, C# source, fixtures, generated artifacts, scripts, or run commands changed.
- No Java/C# parity was verified by this unit.

## Java Artifacts Reviewed

- None. Documentation-only testing-policy update.

## C# Artifacts Reviewed

- None. Documentation-only testing-policy update.

## Files Changed

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`
- `docs/Phase-6-Session-2227-Completion.md`
- `docs/Phase-6-Session-2227-Handoff.md`

## Validation In UOW-2227

Validation decision:

- Changed surface: documentation-only orchestration/startup guidance.
- Focused C# command: not run; runtime tests are not applicable for this documentation-only unit.
- Focused Java/Maven command: not run; no Java source, fixture, generated artifact, test script, or run script changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. Full .NET tests/builds are not applicable for this docs-only unit and no broad trigger was present.
- Why this scope is sufficient: the unit only edits Markdown guidance, so repository whitespace/hygiene validation is the relevant check.

Result:

- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| None | None | Documentation | Not Started | Manual Only | Unknown | Documentation-only testing-policy update. No Java or C# behavior was changed or verified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader preflight rows, value-reader handoff/checklist rows, value-reader readiness summary rows, value-reader blocked report rows, value-reader skeleton attempts, value-reader design rows, execution-readiness gate rows, runtime evidence checklist rows, live-input handoff rows, readiness summary rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The new testing-policy docs reduce future validation cost but cannot enforce agent behavior by themselves.
- Future sessions must still document why focused validation is sufficient and why broad .NET validation was skipped or run.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add the value-reader preflight contract to the value-reader readiness summary and runtime evidence checklist as existing non-live metadata, still without enabling Java/C# value reads.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2227] Tighten focused validation documentation
```
