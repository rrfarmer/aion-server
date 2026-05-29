# Phase 6 Session 1680 Handoff

Date: 2026-05-28
Previous Unit: UOW-1680 (`SmRideRobot` packet parity)

## Current State

- Java `SM_RIDE_ROBOT` packet shape is ported and unit-tested.
- A non-live ride-robot packet-plan helper exists for `broadcastPacketAndReceive(..., new SM_RIDE_ROBOT(player))`.
- Java `SM_FORCED_MOVE` packet shape and forced-move non-live planners remain in place from prior units.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for another non-live planning or golden-vector unit.
2. Live packet dispatch/effect wiring remains intentionally deferred because it crosses shared gameplay and known-list boundaries.
3. Stronger parity evidence would benefit from Java runtime/golden captures for newly ported movement/ride packets.
4. `RideRobotEffect` still needs non-live lifecycle planning before live integration.

## Next Sequential Task (Recommended)

1. Add a non-live `RideRobotEffect` start/end planner that composes `RideRobotPacketPlanService`.
2. Include player robot id mutation intent, weapon-skin robot id source, unequip observer intent, dismount robot id reset, and ride-robot-condition cleanup intent.
3. Keep live packet dispatch and effect-controller mutation out of scope.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live `RideRobotEffect` lifecycle planner | new service + dedicated tests + docs | Low | Natural follow-up to UOW-1680 packet boundary. |
| B | Java runtime/golden `SM_RIDE_ROBOT` vectors | vector artifacts/tests only | Low | Useful to harden packet evidence beyond reviewed source. |
| C | Java runtime/golden movement packet vectors | vector artifacts/tests only | Low | Covers `SM_FORCED_MOVE`, `SM_POSITION`, `SM_POSITION_SELF`. |
| D | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if ride-robot lifecycle work is deferred. |

## Do Not Start Yet

- Live `PacketSendUtility` ride-robot dispatch wiring.
- Live `RideRobotEffect` mutation of `Player.robotId`.
- Live observer/effect-controller cleanup.
- Broad movement/effect-system refactors.
- Any Java source edits.

## Files Added Or Updated In This Unit

- `docs/Phase-6-Session-1680-Completion.md`
- `docs/Phase-6-Session-1680-Handoff.md`
- `docs/PHASE-6-PROGRESS.md`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmRideRobot.cs`
- `dotnetConversion/src/Aion.GameServer/Services/RideRobotPacketPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/SmRideRobotPacketTests.cs`
