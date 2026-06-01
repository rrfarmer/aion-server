# Phase 6 Session 2059 Completion - Focused Test Policy

Date: 2026-06-01
Unit of Work: UOW-2059
Status: Completed

## Scope

- Updated Phase 6 orchestration guidance so focused tests are the default validation strategy.
- Documented when broad .NET validation is worth the time cost.
- Preserved the requirement to run Java/Maven parity tests where they provide direct evidence.

## What Changed

- Added `Test Selection` guidance to `orchestration-rules.md`.
- Made focused C# validation the normal Unit of Work target.
- Made focused Java/Maven parity tests the preferred Java-side evidence when packet/parser/source behavior is affected.
- Restricted broad C# validation to shared infrastructure, live wiring, common state/model changes, suspicious focused-test results, explicit user requests, or release/readiness checkpoints.
- Created this completion document and the matching UOW-2059 handoff as the latest compact rolling context.

## Validation

- Documentation-only change; no build or test run was required.
- `git status --short` was clean before the documentation edits.

## Known Gaps

- Older completion/handoff docs may list broad C# validation as routine evidence from prior sessions. UOW-2059 supersedes that habit for future small scoped units.
- Agents should still choose broader validation when the blast radius is genuinely broad.

## Files Changed

- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2059-Completion.md`
- `docs/Phase-6-Session-2059-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect CM_FIND_GROUP action `0`-`17` composition with the disabled planner, adding handler-composition tests that choose the right planner method without live sends.

Safe alternative candidates:

- Inspect `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` action `26` mask-list planning.
- Inspect prepare-window actions `18`/`22`/`23`/`24` as disabled packet-plan boundaries.
- Return to alliance/group recipient filtering only with objective packet/fanout evidence.
