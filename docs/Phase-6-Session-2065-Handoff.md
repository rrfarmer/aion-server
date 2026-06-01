# Phase 6 Session 2065 Handoff - Find Group Logout Cleanup Planning

Date: 2026-06-01
Unit of Work: UOW-2065
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
- Java `FindGroupService.showInstanceGroups(Player, Npc portalNpc)` is represented as a disabled portal-specific action `26` plan boundary.
- Prepare-window actions `18`, `22`, `23`, and `24` have disabled planner boundaries that select Java-equivalent `SM_FIND_GROUP` packet constructors.
- `CM_FIND_GROUP` action `25` is parsed but has no Java `runImpl` branch; C# composition represents it as `ParsedButNoRunImpl` with no planner payloads.
- `FindGroupService.onLogout(Player)` is represented as disabled cleanup that removes only player-object-id map keys and sends no packets.

## Latest Completed Work

- UOW-2062: disabled portal-specific action `26` mask-list planning for `showInstanceGroups(player, portalNpc)`.
- UOW-2063: disabled prepare-window packet planning for `SM_FIND_GROUP` actions `18`, `22`, `23`, and `24`.
- UOW-2064: action `25` no-run evidence for `CM_FIND_GROUP`.
- UOW-2065: disabled logout cleanup planning for find-group maps.

## Recent Commits

- `223b48026 [Phase 6][UOW-2064] Document find group ban no-run action`
- `f3ed13993 [Phase 6][UOW-2063] Add find group prepare window planning`
- `161393178 [Phase 6][UOW-2062] Add portal find group mask planning`

## Validation In UOW-2065

- Focused C# `FindGroupRecruitmentPlanServiceTests` passed:
  - 28 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` and `SM_FIND_GROUP_GoldenTest` passed:
  - 25 tests passed.
- Java logout cleanup itself has no dedicated Java unit in this repo; evidence is source review plus C# focused unit tests.
- Broad .NET validation was skipped under the focused validation policy because this unit only changed disabled planning/tests; it did not alter shared infrastructure, live dispatch, packet primitives, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout(Player)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout(Player)` | Service Method | Partial | Unit Tested | Partial Parity | Java source reviewed. C# removes recruitment, application, and instance-group entries keyed by player object id and sends no packets. Live singleton/service wiring and concurrency are not implemented. |
| Java `FindGroupService` maps | C# `FindGroupLogoutCleanupPlan` | Plan DTO | Partial | Unit Tested | Partial Parity | Plan records removed states and no direct packet intents. It is disabled evidence only, not live logout dispatch. |

## Summary Metrics

- Total Java artifacts discovered in UOW-2065: 1.
- Total artifacts ported or represented in UOW-2065: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; logout cleanup has source-reviewed unit evidence.
- Total artifacts needing verification or partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No actual live `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The disabled planner and composition layer use deterministic timestamps and caller-supplied runtime facts; live wiring must source Java-equivalent facts from runtime services.
- Live `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, target NPC, portal NPC, and `DataManager.AUTO_GROUP` sourcing remain unimplemented.
- Live logout hook wiring and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: inspect live `CM_FIND_GROUP` handler composition prerequisites and identify the smallest safe runtime dependency boundary, without enabling live dispatch prematurely.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add a disabled end-to-end action composition test that includes caller-supplied runtime facts for action `10`/`13`/`26`.

## Files Changed In UOW-2065

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2065-Completion.md`
- `docs/Phase-6-Session-2065-Handoff.md`
