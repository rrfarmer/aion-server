# Phase 6 Session 2242 Handoff - Focused Validation Testing Guidance

Date: 2026-06-02
Unit of Work: UOW-2242
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Use focused validation by default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit. A passing filtered `dotnet test` command is the compile signal for its affected project and dependencies unless a documented broad-validation trigger applies.

If a focused command is still expected to take several minutes, narrow the filter to the edited test class and closest adjacent class first. Document residual risk instead of using full .NET validation as a reassurance step.

Run Java/Maven only when Java source or fixtures changed, or when a narrow Java source-of-truth command exists for the touched behavior. Record the skip reason when Java is not run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Full .NET test/build commands are opt-in by documented broad-validation trigger, not routine validation.
- Documentation-only units should use `git diff --check` unless docs alter generated artifacts, test scripts, test inputs, or run commands.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorEvidenceSummaryContractService` rolls up blocked-output preview, runtime-evidence intake, materialization preflight, result-emission gate, and runtime comparison readiness into one non-live go/no-go report.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationReadinessAuditService` joins that evidence summary with the implementation plan to enumerate why every executable reader/comparator/emission step remains blocked.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2242 Summary

This UOW updated orchestration documentation to make focused validation cheaper and more explicit.

Key behavior:

- passing filtered `dotnet test` commands are the compile signal for affected projects/dependencies,
- slow focused commands should be narrowed before considering broader validation,
- full .NET suite/build commands require a documented broad-validation trigger,
- docs-only units should use repository hygiene checks and skip runtime tests unless they affect generated artifacts, scripts, test inputs, or run commands.

Important notes:

- This is documentation only.
- It does not change Java or C# behavior.
- It does not add parity evidence.
- It does not update `PHASE-6-PROGRESS.md`.

## Java Artifacts Reviewed

- None; documentation-only orchestration update.

## C# Artifacts Reviewed

- None; documentation-only orchestration update.

## Files Changed

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2242-Completion.md`
- `docs/Phase-6-Session-2242-Handoff.md`

## Validation In UOW-2242

Validation decision:

- Changed surface: documentation-only orchestration/testing guidance.
- Focused C# command: not run; no C# source, tests, fixtures, generated artifacts, or run scripts changed.
- Focused Java/Maven command: not run; no Java source or Java fixture changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally. Documentation-only units use repository hygiene checks unless they change generated artifacts, test inputs, run scripts, or source code.
- Why this scope is sufficient: the edited files are Markdown process documents. `git diff --check` is the appropriate validation for whitespace/patch hygiene.

Result:

- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| None | None | Documentation / Orchestration | Not Started | No Tests | Unknown | Documentation-only testing guidance update; no Java or C# parity artifact changed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Documentation guidance does not prove Java/C# parity.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- Future sessions must still document focused validation commands and skipped broad validation.
- Full validation may still be required when live handler wiring, shared state, packet primitives, persistence, scheduling, crypto, or connection dispatch changes.
- The next implementation UOW remains non-live unless runtime comparison evidence is generated and compared.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor runtime comparison handoff contract that names the exact Java artifact, C# boundary, value projection, materialization, and emission evidence required before any executable implementation can start.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2242] Tighten focused validation guidance
```
