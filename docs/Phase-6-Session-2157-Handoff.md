# Phase 6 Session 2157 Handoff - Focused Validation Policy Tightening

Date: 2026-06-02
Unit of Work: UOW-2157
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Start from the changed files and directly related test classes, then escalate only when the focused result or scoped change justifies it.

Each completion/handoff must record:

- changed surface,
- exact focused C# command or documentation hygiene command,
- exact focused Java/Maven command or skip rationale,
- broad-validation trigger, or `none`,
- broad .NET skip/run decision,
- why the selected scope was sufficient.

Filtered `dotnet test` commands already build the affected project and dependencies. Do not run `dotnet build dotnetConversion\AionServer.slnx` just to get a compile check after a narrow documentation, planner, packet, service, or test unit.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remains an archive and was untouched.
- Completion/handoff docs remain the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- No runtime code changed in UOW-2157.
- The required docs now contain stronger focused-validation guidance and a reusable validation decision template.

## UOW-2157 Summary

This UOW updated the required migration docs to avoid expensive default validation:

- Use the narrowest command tied to the changed surface.
- Prefer filtered C# tests by class name and targeted Maven tests where Java fixtures exist.
- Treat successful filtered `dotnet test` as the compile signal for the affected project/dependencies.
- Name a broad-validation trigger before running unfiltered project tests, full solution tests, or full solution builds.
- For documentation-only units, use hygiene checks and mark runtime tests not applicable.

## Validation In UOW-2157

Validation decision:

- Changed surface: documentation-only.
- Focused C# command: not applicable; no C# code or test code changed.
- Focused Java/Maven command: not applicable; no Java source changed and no Java behavior was under test.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the unit only edits Markdown policy and the selected hygiene command validates the changed surface without the broad runtime cost this policy avoids.

Documentation hygiene:

```powershell
git diff --check
```

Result:

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| N/A | `docs/orchestration-rules.md`; `docs/parity-verification.md`; `docs/csharp-port.md` | Documentation Policy | N/A | Hygiene Checked | N/A | Documentation-only testing policy update. No Java or C# behavior changed, so no parity claim is made. |

## Known Gaps

- No FindGroup runtime parity evidence changed in this unit.
- Future sessions must still choose and document focused validation instead of falling back to broad `.NET` commands.
- Full .NET validation is still appropriate when a documented broad trigger applies, the user requests it, or focused tests expose wider risk.

## Next Recommended Unit of Work

Next sequential task:

- Add focused go/no-go checklist coverage for the remaining `CM_FIND_GROUP` live-dispatch blockers before any `ProcessPacketAsync` wiring attempt, making direct packet ordering, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison gates explicit.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a narrow non-live `CM_FIND_GROUP` live-wiring dry-run plan that enumerates required executors without invoking them.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2157

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2157-Completion.md`
- `docs/Phase-6-Session-2157-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2157] Tighten focused validation policy
```
