# Phase 6 Session 2062 Handoff - Portal-Specific Find Group Mask Planning

Date: 2026-06-01
Unit of Work: UOW-2062
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
- Action `10` composition can include optional action `26` mask-list intent when caller-supplied config/data says Java would send it.
- Java `FindGroupService.showInstanceGroups(Player, Npc portalNpc)` is now represented as a disabled portal-specific action `26` plan boundary.

## Latest Completed Work

- UOW-2059: focused test policy; broad .NET validation is no longer routine for small scoped units.
- UOW-2060: disabled `CM_FIND_GROUP` action composition planner and focused action-routing tests.
- UOW-2061: disabled action `26` mask-list planning for `showInstanceGroups(player, isUpdate)`.
- UOW-2062: disabled portal-specific action `26` mask-list planning for `showInstanceGroups(player, portalNpc)`.

## Recent Commits

- `05b594e93 [Phase 6][UOW-2061] Add find group mask list planning`
- `eb9fd9a34 [Phase 6][UOW-2060] Add find group action composition planner`
- `fef9036ee [Phase 6][UOW-2059] Prefer focused validation by default`

## Validation In UOW-2062

- Focused C# `FindGroupRecruitmentPlanServiceTests` and `SmFindGroupTests` passed:
  - 39 tests passed.
- Focused Java `SM_FIND_GROUP_GoldenTest` passed:
  - 13 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit only changed disabled planning/tests; it did not alter shared infrastructure, live dispatch, packet primitives, or live side effects.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No actual `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The disabled planner and composition layer use deterministic timestamps and caller-supplied runtime facts; live wiring must source Java-equivalent facts from runtime services.
- Live `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, target NPC, portal NPC, and `DataManager.AUTO_GROUP` sourcing remain unimplemented.
- Prepare-window actions, ban action, logout cleanup, and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: inspect prepare-window actions `18`, `22`, `23`, and `24` as disabled packet-plan boundaries.

Safe alternative candidates:

- Inspect action `25` ban behavior and confirm whether Java intentionally leaves it unhandled in `CM_FIND_GROUP.runImpl`.
- Inspect logout cleanup parity for recruitment/application/instance-group maps.
- Continue toward live `CM_FIND_GROUP` handler composition only after runtime dependency sourcing is explicitly planned.

## Files Changed In UOW-2062

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2062-Completion.md`
- `docs/Phase-6-Session-2062-Handoff.md`
