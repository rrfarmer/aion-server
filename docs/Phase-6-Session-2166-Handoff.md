# Phase 6 Session 2166 Handoff - Focused Validation Documentation

Date: 2026-06-02
Unit of Work: UOW-2166
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

Quick validation rule:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single service/planner/schema/test change: run filtered `dotnet test` for that test class plus directly adjacent classes only.
- Packet/parser or boundary change: add only the immediately related packet/parser/boundary tests.
- Java parity check: run a targeted Maven test only when a narrow Java fixture or source-of-truth command exists.
- Full project test, solution test, or solution build: run only after documenting a broad-validation trigger.

Each completion/handoff must record the changed surface, focused C# command or hygiene command, Java/Maven command or skip rationale, broad-validation trigger or `none`, broad .NET skip/run decision, and why the selected scope was sufficient.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be part of normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Direct-packet, world-broadcast, action `12` invite, runtime/socket comparison, and direct show-list trace schema contracts remain non-live readiness artifacts.

## UOW-2166 Summary

This UOW updated process documentation so future sessions avoid expensive full .NET validation by default:

- `docs/orchestration-rules.md` now includes a quick focused-validation rule list.
- `docs/parity-verification.md` clarifies that broad .NET runs are blast-radius checks, not Java parity evidence.
- `docs/csharp-port.md` states that documentation-only Phase 6 units should use `git diff --check` and skip runtime tests unless docs alter generated artifacts or scripts.

No Java or C# runtime behavior changed.

## Java Artifacts Touched

- None.

## C# Artifacts Touched

- None.

## Validation In UOW-2166

Validation decision:

- Changed surface: documentation-only.
- Focused C# command: not applicable.
- Focused Java/Maven command: not applicable; no Java source or executable parity behavior changed.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the unit changed Markdown process guidance only; repository whitespace/hygiene validation is the relevant check.

Command:

```powershell
git diff --check
```

Result:

- Passed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| N/A | N/A | Documentation Process | N/A | Manual Only | N/A | Documentation-only testing workflow change; no Java or C# behavior changed. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- No Java/C# runtime trace capture, live registry send, encrypted socket capture, or real-client runtime comparison has executed for FindGroup direct actions.
- Mutating direct-packet actions, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-packet live-boundary trace scaffolding for mutating direct-packet actions `2` and `6` without live dispatch.

Safe candidates:

- Add a non-live trace export population helper for action `0`/`4` disabled boundary plans using the schema.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2166

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2166-Completion.md`
- `docs/Phase-6-Session-2166-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2166] Document focused validation policy
```
