# Phase 6 Session 2107 Handoff - Focused Validation Documentation Policy

Date: 2026-06-02
Unit of Work: UOW-2107
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

Before running expensive commands, record:

- Changed surface.
- Exact focused C#, Java/Maven, or hygiene command.
- Java/Maven command or the reason it is unavailable/not relevant.
- Broad-validation trigger, or `none`.
- Broad .NET suite/build decision.

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Filtered `dotnet test` commands already build affected projects and dependencies, so a full solution build needs its own documented broad-validation trigger.

Use the narrowest command that proves the scoped change.

Avoid broad .NET commands unless a broad-validation trigger is documented first.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Production DI registers the FindGroup singleton graph through `AddFindGroupSingletonGraph`.
- Logout, joined-team, and disband lifecycle callers now have production singleton graph evidence.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but still not consumed by `GameServerConnection`.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.
- UOW-2106: logout cleanup now uses the injected shared FindGroup service without requiring observer activation.
- UOW-2107: active docs now require focused validation decisions before expensive broad .NET commands.

## Current UOW Commit Message

- `[Phase 6][UOW-2107] Tighten focused validation documentation policy`

## Validation In UOW-2107

- Documentation hygiene passed:
  - `git diff --check`
- Focused C# was not run:
  - Documentation-only UOW; no generated artifacts, runtime code, test code, packet shape, service behavior, or scripts changed.
- Focused Java/Maven was not run:
  - Documentation-only UOW; no Java source-of-truth behavior changed or needed executable parity confirmation.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - Runtime tests and full solution build would not add relevant parity evidence for this docs-only policy update.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| Not applicable | Not applicable | Documentation Process | Not Started | Manual Only | Unknown | Docs-only validation-policy update. No Java or C# runtime artifact was changed, so no parity claim is made. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but not consumed by `GameServerConnection`.
- Socket-level order, real-client behavior, Java runtime packet traces, race-filtered world fanout, visibility filtering, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2107 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add a non-live `GameServerConnection` adapter-consumer slice proving how the connection could compose `CmFindGroup` through `FindGroupConnectionBoundaryDispatchAdapterService` without executing live sends.

Safe alternative candidates:

- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
- Add focused Java/Maven parity fixture for one FindGroup branch if an executable Java test target can be identified.
- Add a connection-registry ordering audit plan for future live direct sends and race-filtered world broadcasts.

## Files Changed In UOW-2107

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`
- `docs/Phase-6-Session-2107-Completion.md`
- `docs/Phase-6-Session-2107-Handoff.md`
