# Phase 6BV Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BU and covers Sessions 475-480.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1025 tests.

---

## Recent Work Completed

- Added `CreaturePvpZoneStateService` for Java `Creature.isInsidePvPZone()` counter semantics: SIEGE-positive returns true, PVP count `0` or `2` returns true, and PVP count `1` returns false.
- Extended `PlayerKiskAttackabilityService` with a Java-shaped counter overload so kisk `ATTACKABLE(0)` / `SUPPORT(54)` selection can consume player and kisk PVP/SIEGE counts.
- Added `PlayerKiskNpcInfoPacketService` to plan viewer-specific `SM_NPC_INFO(Npc, Player)` packets for registered kisks while preserving safe default output when counters are unavailable.
- Added `CreaturePvpZoneCounterService` as the C# owner for per-creature PVP/SIEGE counters, including nested counter behavior and Java `ZoneInstance` duplicate enter / not-inside leave guards.
- Registered the counter service in game-server DI, threaded it through socket/runtime kisk cleanup, and clear kisk zone counters during runtime kisk removal.
- Wired live NPC visibility fanout through `GameClientSocketServer.CreateNpcInfoPacketForViewer`, so appeared kisk NPCs can now use viewer-aware `SmNpcInfo` packet selection when registry/counter data is present.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 480 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `31479160c` - `Add creature pvp zone counter rule`
- `ec2a13670` - `Plan kisk npc info creature type`
- `0ad2e0a28` - `Track creature pvp zone counters`
- `2389c81d1` - `Clear kisk pvp zone counters`
- `67d0df399` - `Use viewer kisk npc info packets`
- `76a5fd86b` - `Guard pvp zone membership counters`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `Creature.isInsidePvPZone()` | `CreaturePvpZoneStateService.IsInsidePvpZone` | Partial | Unit Tested | Partial Parity | Mirrors Java PVP/SIEGE counter semantics. Live zone callbacks are still pending. |
| `Kisk.getType(Player)` | `PlayerKiskAttackabilityService.GetCreatureType(... counters ...)` | Partial | Unit Tested | Partial Parity | Kisk attackability can consume Java-shaped zone counts and return `ATTACKABLE(0)` / `SUPPORT(54)`. |
| `SM_NPC_INFO(Npc, Player)` | `PlayerKiskNpcInfoPacketService` + `GameClientSocketServer.CreateNpcInfoPacketForViewer` | Partial | Unit + Packet Tested | Partial Parity | Live visibility packet selection is viewer-aware for registered kisks when counter data exists; ordinary NPCs remain default `FRIEND(38)`. |
| `Creature.setInsideZoneType` / `unsetInsideZoneType` | `CreaturePvpZoneCounterService.EnterZone` / `LeaveZone` | Partial | Unit Tested | Partial Parity | Per-creature nested PVP/SIEGE counters exist in C# and are available through DI. |
| `ZoneInstance.onEnter` / `onLeave` | `CreaturePvpZoneCounterService.ApplyZoneEnter` / `ApplyZoneLeave` | Partial | Unit Tested | Partial Parity | Duplicate enter and not-inside leave guards are modeled. Real geometry revalidation, controller callbacks, and handlers are not wired. |
| `KiskService.removeKisk` / `KiskController.delete` cleanup | `PlayerKiskRemovalRuntimeCleanupService` + counter cleanup | Partial | Workflow Tested | Partial Parity | Runtime kisk removal now clears PVP/SIEGE counters along with member bind, revive option, creator update, and NPC visibility cleanup. |

Metrics from this handoff window:

- Total focused sessions covered: 6
- Total commits covered: 6
- Current full validation baseline: 1025 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: real PVP/SIEGE zone template/callback ingestion, generic world-object zone counter cleanup, full zone handlers, full encrypted socket-order validation, dedicated kisk controller/AI, combat attackability enforcement
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, real zone template/callback ingestion, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full socket-order harnesses, full zone lifecycle handlers, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- `CreaturePvpZoneCounterService` is ready for live callbacks, but no real movement/zone revalidation flow calls `ApplyZoneEnter` / `ApplyZoneLeave` yet.
- The membership key currently depends on caller-supplied zone ids. Future zone template ingestion must choose stable Java-equivalent identifiers.
- PVP/SIEGE are the only new counter types modeled here. FLY/NO_FLY, BIND, GLIDE, item-use, quest/material handlers, and Java zone option precedence remain separate work.
- Viewer-specific kisk `SmNpcInfo` is wired into the live visibility fanout, but tests still validate packet selection/payloads directly rather than a full encrypted client session sequence.
- Loot-status packets still follow NPC-info in the existing fanout; mixed packet ordering is source-preserved but not real-client verified.
- Kisk runtime remains a lightweight `WorldNpc` plus `PlayerKiskRuntimeState`, not a full Java `Kisk`/`SummonedObject` with dedicated controller, effect controller, known-list ownership, AI, attack ingress, or lifecycle callback layering.
- Generic world-object despawn/delete paths outside kisk removal do not clear PVP/SIEGE counters yet.

---

## Next Unit Of Work

Recommended next unit: continue zone/kisk parity by adding stable PVP/SIEGE zone identifiers and wiring real movement/zone revalidation callbacks into `CreaturePvpZoneCounterService.ApplyZoneEnter` / `ApplyZoneLeave`.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/world/zone/ZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/ZoneService.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/PvPZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/SiegeZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/model/siege/FortressLocation.java`
   - `game-server/src/com/aionemu/gameserver/model/gameobjects/Creature.java`
2. Re-read C#:
   - `CreaturePvpZoneCounterService`
   - `CreaturePvpZoneStateService`
   - `PlayerZoneStateService`
   - `StaticData` flight-zone parsing as the nearest local zone template pattern
   - `GameClientSocketServer.CreateNpcInfoPacketForViewer`
   - `PlayerKiskNpcInfoPacketService`
3. Keep the first unit narrow:
   - choose stable zone ids for future PVP/SIEGE callbacks
   - feed `ApplyZoneEnter` / `ApplyZoneLeave` from a minimal revalidation boundary
   - preserve current FLY/NO_FLY behavior and avoid broad zone refactors
   - update tests for duplicate callback safety and kisk packet output after revalidation

Strong alternatives:

1. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
2. Add generic world-object cleanup that clears `CreaturePvpZoneCounterService` state for non-kisk NPC despawn/delete once the right lifecycle hook is identified.
3. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.
4. Wire live team membership into `PlayerKiskAuthorizationService` only after a faithful group/alliance model exists.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CreaturePvpZoneCounterServiceTests|FullyQualifiedName~GameClientSocketServerNpcVisibilityTests|FullyQualifiedName~PlayerKiskNpcInfoPacketServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcDeathDropWorkflowServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 475-480, `docs/Phase-6BU-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
