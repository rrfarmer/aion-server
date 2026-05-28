# Phase 6 Session 1676 Handoff

Date: 2026-05-28  
Previous Unit: UOW-1676 (`ForcedMoveStartEffectPlanService`)

## Current State

- Java `SM_FORCED_MOVE` packet shape is ported and unit-tested.
- A non-live packet-plan helper exists for `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))`.
- A non-live start-effect planner now exists for `PulledEffect` and `OpenAerialEffect`, including reflected-pull packet-source behavior and open-aerial paralyze-removal intent.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for another non-live planning or golden-vector unit.
2. Live controller/world/packet wiring remains intentionally deferred because it crosses shared movement-system boundaries.
3. Stronger parity evidence would benefit from Java runtime/golden `SM_FORCED_MOVE` capture.

## Next Sequential Task (Recommended)

1. Capture Java runtime/golden vectors for `SM_FORCED_MOVE`.
2. If packet evidence is sufficient, extend the same non-live planner pattern to `StaggerEffect` and `StumbleEffect`.
3. Keep live movement/controller/world integration out of scope until enough deterministic planning coverage exists.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime/golden `SM_FORCED_MOVE` vectors | vector artifacts/tests only | Low | Best next evidence-focused step. |
| B | Non-live `StaggerEffect` / `StumbleEffect` start planner | new service + dedicated tests + docs | Low | Reuses the same forced-move packet boundary and planner pattern. |
| C | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if forced-move follow-up is deferred. |

## Do Not Start Yet

- Live `PacketSendUtility` forced-move dispatch wiring.
- Live `World.updatePosition` forced-move effect wiring.
- Live controller mutations for pulled/open-aerial effects.
- Broad movement-system refactors.
- Any Java source edits.

## Files Added In This Unit

- `docs/Phase-6-Session-1676-Completion.md`
- `docs/Phase-6-Session-1676-Handoff.md`