# Phase 6 Session 2059 Handoff - Focused Test Policy

Date: 2026-06-01
Unit of Work: UOW-2059
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

For ordinary small Units of Work, run:

- Focused C# tests for the edited class/service/packet/parser/composition area.
- Focused Java/Maven tests where they provide direct parity evidence for the touched Java source, packet, or parser behavior.

Do not run the broad .NET suite by default.

Run broad C# validation only when:

- Shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, or connection dispatch changes.
- Live handler wiring or live side effects are enabled.
- A change touches common model/state used across many systems.
- Focused tests expose suspicious behavior and broader blast-radius checking is needed.
- The user explicitly asks for broad validation.
- A release/readiness checkpoint requires it.

When broad validation is skipped, document the focused commands that ran and why they were sufficient for the scoped risk.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent work has been intentionally conservative: disabled planner evidence for find-group behavior, not live handler parity.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.

## Latest Completed Work

- UOW-2053: disabled recruitment add/update/remove/show planner evidence.
- UOW-2054: disabled application add/update/remove/show planner evidence.
- UOW-2055: disabled joined-team callback planner evidence.
- UOW-2056: disabled instance-group register/update/remove/show/member-info planner evidence.
- UOW-2057: disabled instance application send/result planner evidence.
- UOW-2058: startup context compaction; `PHASE-6-PROGRESS.md` moved out of normal required reads.
- UOW-2059: focused test policy; broad .NET validation is no longer routine for small scoped units.

## Recent Commits

- `251c9c44b [Phase 6][UOW-2058] Compact startup handoff context`
- `59b7d1cdb [Phase 6][UOW-2057] Add find group application response planner`
- `70e992b39 [Phase 6][UOW-2056] Add find group instance planner`

## Validation Baseline From UOW-2057

- Focused C# `FindGroupRecruitmentPlanServiceTests` passed with 24 tests.
- Focused C# find-group/message planner/packet/parser tests passed with 41 tests.
- Focused Java `SM_FIND_GROUP_GoldenTest` and `CM_FIND_GROUP_ReadPayloadGoldenTest` passed with 25 test methods.
- Full scoped Maven reactor passed with 1 commons test and 126 game-server tests.
- Broad C# validation passed with 5201 tests.

UOW-2059 was documentation-only; no build/test run was required.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No actual `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The disabled planner uses deterministic timestamps and caller-supplied runtime facts; live wiring must source those facts from Java-equivalent runtime services.
- Prepare-window actions, ban action, logout cleanup, action `26` mask-list routing, and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: inspect CM_FIND_GROUP action `0`-`17` composition with the disabled planner, adding handler-composition tests that choose the right planner method without live sends.

Safe alternative candidates:

- Inspect `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` action `26` mask-list planning.
- Inspect prepare-window actions `18`/`22`/`23`/`24` as disabled packet-plan boundaries.
- Return to alliance/group recipient filtering only with objective packet/fanout evidence.

## Files Changed In UOW-2059

- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2059-Completion.md`
- `docs/Phase-6-Session-2059-Handoff.md`
