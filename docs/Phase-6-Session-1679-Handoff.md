# Phase 6 Session 1679 Handoff

Date: 2026-05-28
Previous Unit: UOW-1679 (`StaggerStumbleEndEffectPlanService`)

## Current State

- Java `SM_FORCED_MOVE` packet shape is ported and unit-tested.
- A non-live packet-plan helper exists for `broadcastPacketAndReceive(..., new SM_FORCED_MOVE(...))`.
- The non-live forced-move start-effect planner covers `PulledEffect`, `OpenAerialEffect`, `StaggerEffect`, and `StumbleEffect`.
- The non-live calculate-phase planner covers `StaggerEffect.calculate` and `StumbleEffect.calculate`.
- The non-live end-effect cleanup planner covers `StaggerEffect.endEffect` and `StumbleEffect.endEffect`.
- Progress and parity documentation were updated in `docs/PHASE-6-PROGRESS.md`.

## Blockers

1. No blocker for another non-live planning or golden-vector unit.
2. Live controller/world/packet/effect wiring remains intentionally deferred because it crosses shared movement-system boundaries.
3. Stronger parity evidence would benefit from Java runtime/golden `SM_FORCED_MOVE` and stagger/stumble calculate vector capture.
4. `GeoService.getClosestCollision` and `EffectTemplate.calculate` are still modeled as inputs/boundaries rather than executed behavior.
5. `StumbleEffect` has a Java TODO noting some skills do not send packets; skill-specific no-send behavior is not modeled yet.

## Next Sequential Task (Recommended)

1. Capture Java runtime/golden vectors for `SM_FORCED_MOVE` or for stagger/stumble calculate probe inputs/outputs.
2. If vector work is deferred, continue with another isolated packet parity unit or a non-live planner for another small movement/combat effect.
3. Keep live movement/controller/world integration out of scope until enough deterministic planning and vector coverage exists.

## Safe Parallel Candidates For Next Session

| Candidate | Scope | Allowed Files | Risk | Notes |
|---|---|---|---|---|
| A | Java runtime/golden `SM_FORCED_MOVE` vectors | vector artifacts/tests only | Low | Best next evidence-focused step for packet parity. |
| B | Java runtime/golden stagger/stumble calculate vectors | vector artifacts/tests only | Low | Useful to harden heading/angle/probe math and GeoService assumptions. |
| C | Another isolated packet parity unit | new packet + dedicated tests + docs | Low | Safe if forced-move vector work is deferred. |
| D | Non-live planner for another small movement/combat effect | new service + dedicated tests + docs | Low/Medium | Inspect Java first; avoid live world/controller/dispatch mutation. |

## Do Not Start Yet

- Live `PacketSendUtility` forced-move dispatch wiring.
- Live `World.updatePosition` forced-move effect wiring.
- Live `GeoService` integration for these effect planners.
- Live controller/effect mutations for pulled/open-aerial/stagger/stumble effects.
- Broad movement-system refactors.
- Any Java source edits.

## Files Added Or Updated In This Unit

- `docs/Phase-6-Session-1679-Completion.md`
- `docs/Phase-6-Session-1679-Handoff.md`
- `docs/PHASE-6-PROGRESS.md`
- `dotnetConversion/src/Aion.GameServer/Services/StaggerStumbleEndEffectPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaggerStumbleEndEffectPlanServiceTests.cs`
