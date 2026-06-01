# Phase 6 Session 2067 Handoff - Find Group Runtime Facts Boundary

Date: 2026-06-01
Unit of Work: UOW-2067
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
- Recent find-group work is intentionally conservative: disabled planner and disabled composition/readiness evidence only.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupClientActionPlanService` composes disabled plans for Java `CM_FIND_GROUP.runImpl` actions.
- `FindGroupClientActionDispatchPrerequisites` names the runtime facts and side-effect dispatchers needed before live `CM_FIND_GROUP` dispatch can be enabled.
- `FindGroupClientActionRuntimeFacts` now packages caller-supplied active player, time, lookup delegates, team/member snapshots, and config/data snapshots into disabled composition.

## Latest Completed Work

- UOW-2064: action `25` no-run evidence for `CM_FIND_GROUP`.
- UOW-2065: disabled logout cleanup planning for find-group maps.
- UOW-2066: live-dispatch prerequisite/readiness map for `CM_FIND_GROUP`.
- UOW-2067: typed runtime-facts boundary for disabled `CM_FIND_GROUP` composition.

## Recent Commits

- `710deaa0f [Phase 6][UOW-2066] Map find group live dispatch prerequisites`
- `dd764bb2e [Phase 6][UOW-2065] Add find group logout cleanup plan`
- `223b48026 [Phase 6][UOW-2064] Document find group ban no-run action`

## Validation In UOW-2067

- Focused C# runtime-facts/prerequisite/composition tests passed:
  - 15 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit only added a typed disabled runtime-facts boundary/tests; it did not alter shared infrastructure, live dispatch, packet primitives, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupClientActionRuntimeFacts` | Composition DTO | Partial | Unit Tested | Partial Parity | Java runImpl obtains active player from connection and runtime facts from service/world/config/data managers. C# facts package those values for disabled planning only; no live sends are enabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.readImpl` | `FindGroupClientAction.FromPacket(CmFindGroup)` via runtime facts test | Parser Bridge | Partial | Unit Tested / Golden File Tested | Partial Parity | C# parsed packet action `2` flows into disabled planner. Java parser golden tests cover CM_FIND_GROUP payload layout. Full live handler remains deferred. |

## Summary Metrics

- Total Java artifacts discovered in UOW-2067: 1.
- Total artifacts ported or represented in UOW-2067: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; parser bridge has focused evidence.
- Total artifacts needing verification or partial parity: 2.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No live `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- `FindGroupClientActionRuntimeFacts` packages facts but does not source them from live `GameServerConnection`, `World`, `GroupConfig`, target NPC, `DataManager.AUTO_GROUP`, or team services.
- Live logout hook wiring and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: add a disabled `GameServerConnection`-adjacent adapter plan for `CmFindGroup` that extracts `ActivePlayer` and produces a disabled composition result, but still does not send packets.

Safe alternative candidates:

- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.
- Add runtime-fact sourcing tests for current-team/member snapshots before live dispatch.

## Files Changed In UOW-2067

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionRuntimeFacts.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionRuntimeFactsTests.cs`
- `docs/Phase-6-Session-2067-Completion.md`
- `docs/Phase-6-Session-2067-Handoff.md`
