# Phase 6 Session 1675 Handoff

Date: 2026-05-28  
Previous Unit: UOW-1675 (`SmForcedMove` + `ForcedMovePacketPlanService`)

## Current State

- Java `SM_FORCED_MOVE` now has a C# packet writer with tested payload shape parity.
- A non-live `ForcedMovePacketPlanService` exists for the Java `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))` intent used by forced-move callers.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for continuing with the next non-live effect-planning slice.
2. Stronger parity evidence for `SM_FORCED_MOVE` would still benefit from Java runtime/golden packet capture.
3. Live effect/controller/world integration remains intentionally deferred.

## Next Sequential Task (Recommended)

1. Add a non-live `PulledEffect` / `OpenAerialEffect` start-effect planner that reuses `ForcedMovePacketPlanService`.
2. Cover Java ordering metadata conservatively:
   - cancel-current-skill intent
   - optional remove-paralyze intent (`OpenAerialEffect` only)
   - optional player stop-glide / stop-move intent
   - world-position update intent
   - player-only forced-move packet intent
   - abnormal-state set intent
3. Add focused planner tests.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live `PulledEffect` / `OpenAerialEffect` start planner | new service + dedicated tests + progress docs | Low | Best immediate follow-up; reuses the new forced-move packet boundary. |
| B | Java runtime/golden vectors for `SM_FORCED_MOVE` | vector artifacts/tests only | Low | Strengthens packet parity evidence without touching live gameplay. |
| C | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if the next planner unit is deferred. |

## Do Not Start Yet

- Live `PacketSendUtility` broadcast integration.
- Live `World.updatePosition` forced-move wiring.
- Live `EffectController` / `PlayerController` mutation wiring for pulled/open-aerial effects.
- Broad movement-system refactors.
- Any Java source edits.

## Files Added In This Unit

- `docs/Phase-6-Session-1675-Completion.md`
- `docs/Phase-6-Session-1675-Handoff.md`