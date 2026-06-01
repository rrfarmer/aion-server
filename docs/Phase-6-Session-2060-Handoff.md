# Phase 6 Session 2060 Handoff - CM_FIND_GROUP Composition Planner

Date: 2026-06-01
Unit of Work: UOW-2060
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

For ordinary small Units of Work, run focused C# tests for the edited area and focused Java/Maven parity tests where they directly evidence the touched Java source, packet, or parser behavior.

Do not run the broad .NET suite by default. Run broad C# validation only for shared infrastructure, packet primitives, serialization helpers, crypto, scheduling, world state, persistence, connection dispatch, live side effects, common state/model changes, suspicious focused failures, explicit user request, or release/readiness checkpoint.

When broad validation is skipped, document the focused commands that ran and why they were sufficient for the scoped risk.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Recent find-group work is intentionally conservative: disabled planner and disabled composition evidence only.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupClientActionPlanService` now maps parsed client action data into disabled planner calls for Java `CM_FIND_GROUP.runImpl` actions `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, `11`, `12`, `13`, `15`, and `17`.
- Actions `20` and `25` are parsed by Java/C# but have no Java `runImpl` branch; the C# composition planner records them as non-dispatching.

## Latest Completed Work

- UOW-2057: disabled instance application send/result planner evidence.
- UOW-2058: startup context compaction; `PHASE-6-PROGRESS.md` moved out of normal required reads.
- UOW-2059: focused test policy; broad .NET validation is no longer routine for small scoped units.
- UOW-2060: disabled `CM_FIND_GROUP` action composition planner and focused action-routing tests.

## Recent Commits

- `fef9036ee [Phase 6][UOW-2059] Prefer focused validation by default`
- `251c9c44b [Phase 6][UOW-2058] Compact startup handoff context`
- `59b7d1cdb [Phase 6][UOW-2057] Add find group application response planner`

## Validation In UOW-2060

- Focused C# `FindGroupClientActionPlanServiceTests`, existing `FindGroupRecruitmentPlanServiceTests`, and CM_FIND_GROUP parser tests passed:
  - 29 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` and `SM_FIND_GROUP_GoldenTest` passed:
  - 25 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit only added disabled composition and tests; it did not alter shared infrastructure, live dispatch, packet primitives, or live side effects.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No actual `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The disabled planner and composition layer use deterministic timestamps and caller-supplied runtime facts; live wiring must source Java-equivalent facts from runtime services.
- Action `13` currently routes to the same disabled show-instance-groups plan as action `10`; Java optional action `26` mask-list behavior remains unported/unverified.
- Prepare-window actions, ban action, logout cleanup, action `26` mask-list routing, and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: inspect `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` action `26` mask-list planning around Java `showInstanceGroups(player, isUpdate)`.

Safe alternative candidates:

- Inspect prepare-window actions `18`/`22`/`23`/`24` as disabled packet-plan boundaries.
- Inspect action `25` ban behavior and confirm whether Java intentionally leaves it unhandled in `CM_FIND_GROUP.runImpl`.
- Continue toward live `CM_FIND_GROUP` handler composition only after runtime dependency sourcing is explicitly planned.

## Files Changed In UOW-2060

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionPlanServiceTests.cs`
- `docs/Phase-6-Session-2060-Completion.md`
- `docs/Phase-6-Session-2060-Handoff.md`
