# Phase 6 Session 1678 Handoff

Date: 2026-05-28
Previous Unit: UOW-1678 (`StaggerStumbleCalculatePlanService`)

## Current State

- Java `SM_FORCED_MOVE` packet shape is ported and unit-tested.
- A non-live packet-plan helper exists for `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))`.
- The non-live forced-move start-effect planner covers `PulledEffect`, `OpenAerialEffect`, `StaggerEffect`, and `StumbleEffect`.
- A non-live calculate-phase planner now covers `StaggerEffect.calculate` and `StumbleEffect.calculate`.
- Calculate planning includes abnormal pre-checks, base-calculate gate input, sub-effect type intent, heading/angle conversion, two-meter collision probe metadata, and supplied collision target-location output.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for another non-live planning or golden-vector unit.
2. Live controller/world/packet wiring remains intentionally deferred because it crosses shared movement-system boundaries.
3. Stronger parity evidence would benefit from Java runtime/golden `SM_FORCED_MOVE` and stagger/stumble calculate vector capture.
4. `GeoService.getClosestCollision` and `EffectTemplate.calculate` are still modeled as inputs/boundaries rather than executed behavior.
5. `StumbleEffect` has a Java TODO noting some skills do not send packets; skill-specific no-send behavior is not modeled yet.

## Next Sequential Task (Recommended)

1. Capture Java runtime/golden vectors for `SM_FORCED_MOVE` or for stagger/stumble calculate probe inputs/outputs.
2. If vector work is deferred, add a non-live end-effect cleanup planner for `StaggerEffect.endEffect` and `StumbleEffect.endEffect`.
3. Keep live movement/controller/world integration out of scope until enough deterministic planning and vector coverage exists.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime/golden `SM_FORCED_MOVE` vectors | vector artifacts/tests only | Low | Best next evidence-focused step for packet parity. |
| B | Java runtime/golden stagger/stumble calculate vectors | vector artifacts/tests only | Low | Useful to harden heading/angle/probe math and GeoService assumptions. |
| C | Non-live `StaggerEffect` / `StumbleEffect` end-effect planner | new service + dedicated tests + docs | Low | Should model abnormal unset intent only, unless Java source reveals additional cleanup. |
| D | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if forced-move follow-up is deferred. |

## Do Not Start Yet

- Live `PacketSendUtility` forced-move dispatch wiring.
- Live `World.updatePosition` forced-move effect wiring.
- Live `GeoService` integration for these effect planners.
- Live controller mutations for pulled/open-aerial/stagger/stumble effects.
- Broad movement-system refactors.
- Any Java source edits.

## Files Added Or Updated In This Unit

- `docs/Phase-6-Session-1678-Completion.md`
- `docs/Phase-6-Session-1678-Handoff.md`
- `docs/PHASE-6-PROGRESS.md`
- `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleCalculatePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleCalculatePlanServiceTests.cs`
