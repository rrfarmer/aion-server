# Phase 6 Session 2066 Handoff - Find Group Live Dispatch Prerequisites

Date: 2026-06-01
Unit of Work: UOW-2066
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
- `FindGroupClientActionDispatchPrerequisites` now names the runtime facts and side-effect dispatchers needed before live `CM_FIND_GROUP` dispatch can be enabled.
- Parsed-but-no-run actions `20` and `25` continue to require no runtime dispatch.

## Latest Completed Work

- UOW-2063: disabled prepare-window packet planning for `SM_FIND_GROUP` actions `18`, `22`, `23`, and `24`.
- UOW-2064: action `25` no-run evidence for `CM_FIND_GROUP`.
- UOW-2065: disabled logout cleanup planning for find-group maps.
- UOW-2066: live-dispatch prerequisite/readiness map for `CM_FIND_GROUP`.

## Recent Commits

- `dd764bb2e [Phase 6][UOW-2065] Add find group logout cleanup plan`
- `223b48026 [Phase 6][UOW-2064] Document find group ban no-run action`
- `f3ed13993 [Phase 6][UOW-2063] Add find group prepare window planning`

## Validation In UOW-2066

- Focused C# prerequisite/composition/parser tests passed:
  - 12 tests passed.
- Focused Java `CM_FIND_GROUP_ReadPayloadGoldenTest` passed:
  - 12 tests passed.
- Broad .NET validation was skipped under the focused validation policy because this unit only added disabled readiness mapping/tests; it did not alter shared infrastructure, live dispatch, packet primitives, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupClientActionDispatchPrerequisites` | Composition / Readiness Utility | Partial | Unit Tested | Partial Parity | Java run switch reviewed. C# readiness map names runtime facts and side-effect dispatchers required before live dispatch. It does not execute actions. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` dependencies | `FindGroupClientActionRuntimeRequirement` | Enum | Partial | Unit Tested | Needs Verification | Requirements identify active player, state store, player lookup, config/data lookup, packet dispatch, broadcast dispatch, team/member snapshots, and invite dispatch. Live sourcing remains unimplemented. |

## Summary Metrics

- Total Java artifacts discovered in UOW-2066: 2.
- Total artifacts ported or represented in UOW-2066: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts; readiness mapping has source-reviewed unit evidence.
- Total artifacts needing verification or partial parity: 3.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- No live `FindGroupService` singleton runtime, `World.getPlayer`, `PacketSendUtility.sendPacket`/`broadcastToWorld`, group/alliance invite side effects, response requester mutation, encrypted socket frame, real-client behavior, or service concurrency parity has been proven for find-group.
- The disabled planner and composition layer use deterministic timestamps and caller-supplied runtime facts; live wiring must source Java-equivalent facts from runtime services.
- Live `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE`, target NPC, portal NPC, and `DataManager.AUTO_GROUP` sourcing remain unimplemented.
- Live logout hook wiring and live handler composition remain unported or unverified.

## Next Recommended Unit of Work

- Next sequential task: inspect and model the smallest runtime fact source for live `CM_FIND_GROUP` dispatch, likely active-player plus deterministic current-time/state-store access, without enabling packet sends.

Safe alternative candidates:

- Add a disabled end-to-end action composition test that uses `FindGroupClientAction.FromPacket` plus runtime prerequisite inspection.
- Inspect Java call sites for prepare-window actions `18`-`24` beyond packet serialization.
- Inspect group/alliance ban services separately from `CM_FIND_GROUP` only if a Java caller is identified.

## Files Changed In UOW-2066

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupClientActionDispatchPrerequisites.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupClientActionDispatchPrerequisitesTests.cs`
- `docs/Phase-6-Session-2066-Completion.md`
- `docs/Phase-6-Session-2066-Handoff.md`
