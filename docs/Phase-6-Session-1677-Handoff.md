# Phase 6 Session 1677 Handoff

Date: 2026-05-28
Previous Unit: UOW-1677 (`ForcedMoveStartEffectPlanService` stagger/stumble extension)

## Current State

- Java `SM_FORCED_MOVE` packet shape is ported and unit-tested.
- A non-live packet-plan helper exists for `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))`.
- The non-live forced-move start-effect planner now covers:
  - `PulledEffect.startEffect`
  - `OpenAerialEffect.startEffect`
  - `StaggerEffect.startEffect`
  - `StumbleEffect.startEffect`
- Planner coverage includes reflected-pull packet-source behavior, open-aerial paralyze-removal intent, stagger paralyze-removal intent, and stumble paralyze-plus-stun-removal intent.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for another non-live planning or golden-vector unit.
2. Live controller/world/packet wiring remains intentionally deferred because it crosses shared movement-system boundaries.
3. Stronger parity evidence would benefit from Java runtime/golden `SM_FORCED_MOVE` capture.
4. `StumbleEffect` has a Java TODO noting some skills do not send packets; skill-specific no-send behavior is not modeled yet.

## Next Sequential Task (Recommended)

1. Capture Java runtime/golden vectors for `SM_FORCED_MOVE`.
2. If packet evidence is sufficient, add a non-live calculate-phase planner for `StaggerEffect` / `StumbleEffect` heading and geo target-location selection.
3. Keep live movement/controller/world integration out of scope until enough deterministic planning coverage exists.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime/golden `SM_FORCED_MOVE` vectors | vector artifacts/tests only | Low | Best next evidence-focused step. |
| B | Non-live `StaggerEffect` / `StumbleEffect` calculate planner | new service + dedicated tests + docs | Low | Should model abnormal pre-checks, resistance gate result input, heading/angle, 2m offset, geo target-location intent, and sub-effect typing. |
| C | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if forced-move follow-up is deferred. |

## Do Not Start Yet

- Live `PacketSendUtility` forced-move dispatch wiring.
- Live `World.updatePosition` forced-move effect wiring.
- Live controller mutations for pulled/open-aerial/stagger/stumble effects.
- Broad movement-system refactors.
- Any Java source edits.

## Files Added Or Updated In This Unit

- `docs/Phase-6-Session-1677-Completion.md`
- `docs/Phase-6-Session-1677-Handoff.md`
- `docs/PHASE-6-PROGRESS.md`
- `dotnetConversion/src/Aion.GameServer/Services/ForcedMoveStartEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ForcedMoveStartEffectPlanServiceTests.cs`
