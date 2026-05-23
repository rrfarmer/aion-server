# Phase 6CB Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6CA and covers Sessions 498-499.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1047 tests.

---

## Recent Work Completed

- Added compact SIEGE/FORT formation walker variant coverage for `WorldNpcSpawnService.TrySwapInactiveWalkerFormationVariant`.
- Extended the synthetic versioned formation fixture with an optional Java-style `zone_type="FORT"` polygon, which the C# static-data bridge maps to `CreaturePvpZoneType.Siege`.
- Verified formation variant swaps clear modeled SIEGE counters for parked active members and re-enter SIEGE counters for activated inactive members.
- Wired `GameServerConnection.HandleLevelReadyAsync` to revalidate modeled creature PVP/FORT counters after the client reports that the destination map has loaded.
- Kept existing level-ready packet ordering intact; the new revalidation has no packet side effects and runs after the currently modeled baseline player-info/account-properties/motion sends.
- Made `HandleLevelReadyAsync` internal so the real map-load completion boundary can be regression-tested directly.
- Added a real static-data regression that mutates the player position into `PVP_87_210040000` before level-ready and verifies modeled PVP counters enter on level-ready.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 499 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `ca17fdb4f` - `Cover formation variant siege counters`
- `23942ada7` - `Revalidate pvp zones on level ready`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `InstanceWalkerFormations.changeCluster` | `WorldNpcSpawnService.TrySwapInactiveWalkerFormationVariant` | Partial | Regression Tested | Partial Parity | Formation variant swaps now have direct PVP and SIEGE/FORT counter coverage for parked and activated members. Java trigger callers from death/AI/instance callbacks remain future work. |
| `WalkerGroup.spawn/despawn` | `WorldNpcSpawnService` formation add/remove hooks | Partial | Regression Tested | Partial Parity | Regression proves active formation members enter counters and parked members clear counters for synthetic PVP and synthetic FORT geometry. Java full group movement state, instance callbacks, and controller side effects remain incomplete. |
| `ZoneClassName.FORT` / `FortressLocation` | `StaticData.CreaturePvpZones` + `CreaturePvpZoneType.Siege` | Partial | Regression Tested | Partial Parity | Synthetic FORT fixture verifies the static-data mapping used by formation revalidation. Full fortress observers, ownership, shield, vulnerability, balance buffs, and siege handlers remain unported. |
| `CM_LEVEL_READY` | `GameServerConnection.HandleLevelReadyAsync` | Partial | Regression Tested | Partial Parity | Level-ready now revalidates modeled PVP/FORT counters after the C# player position has already moved to destination coordinates. Many Java level-ready callbacks remain unported. |
| `TeleportService.SpawnTask.run` | Position-before-level-ready model plus `HandleLevelReadyAsync` revalidation | Partial | Regression Tested | Needs Verification | Java mutates position in `SpawnTask.run` and full-map teleports complete player spawn through `CM_LEVEL_READY`. C# still lacks generic pending teleport task state and `CM_TELEPORT_ANIMATION_DONE` execution. |
| `World.spawn` / `World.despawn` | Direct C# world/connection hooks around formation and level-ready boundaries | Partial | Regression Tested | Intentional Difference | C# uses immediate modeled counter revalidation/cleanup because Java map regions, zone instances, known lists, and controller callbacks are not fully ported. |
| `Creature.revalidateZones` / `MapRegion.revalidateZones` | `CreaturePvpZoneRevalidationService.Revalidate` | Partial | Unit + Regression Tested | Partial Parity | Synthetic FORT geometry covers formation swaps; real Java PVP geometry covers level-ready player map-load completion. Zone priorities, handlers, neighboring regions, full-map zones, and live fortress state remain incomplete. |
| `PvPZoneInstance.onEnter/onLeave` with PVP/SIEGE counters | `CreaturePvpZoneCounterService` | Partial | Unit + Regression Tested | Partial Parity | Tests prove formation SIEGE transitions and level-ready PVP entry update modeled counters. Java handler ordering and side effects remain unverified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1047 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: generic pending teleport task state, `CM_TELEPORT_ANIMATION_DONE`, full level-ready Java side effects, SIEGE/FORT level-ready coverage, live fortress/siege side effects, Java zone handlers/controller callbacks, formation trigger callers, Java queued scheduler/levels, full formation AI/movement parity, and live runtime ordering
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change zone revalidation, remaining direct-removal cleanup, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- SIEGE/FORT formation coverage uses synthetic geometry. Full Java fortress/siege behavior, observers, ownership, shields, vulnerability windows, balance buffs, and controller callbacks remain unported.
- Formation swap trigger callers from Java death/AI/instance callbacks remain absent; current tests validate the service boundary once called.
- C# level-ready still omits many Java side effects: world-region spawn, known-list setup, house/instance/windstream/siege/conqueror/rift/quest/weather/town/event callbacks, pet spawn, protection tasks, effect icon refresh, and team brand scheduling.
- Generic Java `TeleportService.sendLoc` pending task state and `CM_TELEPORT_ANIMATION_DONE` delayed completion are still not ported.
- C# still clears/revalidates modeled counters directly rather than executing Java zone `onEnter`/`onLeave` handlers, controller callbacks, quest/material handlers, fortress observers, or instance callbacks.

---

## Next Unit Of Work

Recommended next unit: model a narrow pending teleport completion slice corresponding to Java `TeleportService.sendLoc` + `CM_TELEPORT_ANIMATION_DONE`.

1. Re-read Java:
   - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
   - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TELEPORT_ANIMATION_DONE.java`
   - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEVEL_READY.java`
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
2. Re-read C#:
   - `GameServerConnection` switch case for `CmTeleportAnimationDone`
   - `GameServerConnection.HandleLevelReadyAsync`
   - `PlayerTeleportService`
   - `Player` state model
   - `CreaturePvpZoneRevalidationService`
   - `CreaturePvpZoneCounterService`
3. Keep the next unit narrow:
   - add a small pending teleport state to the C# player or teleport service
   - add a helper that mirrors Java `SpawnTask.run` at the modeled level: execute once, mutate player position, reset movement, clear pending state
   - make `CmTeleportAnimationDone` execute that pending state when present
   - revalidate modeled PVP/FORT counters after the position mutation
   - preserve existing packet ordering and document any intentional differences
   - add a focused regression with real Java PVP geometry proving counters clear/enter after delayed teleport completion

Strong alternatives:

1. Add SIEGE/FORT level-ready coverage using real `ABYSS_CASTLE_AREA_2011_210050000` or a compact synthetic map-load fixture.
2. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
3. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.
4. Audit static object, gatherable, town, pooled respawn, and per-instance pool state spawn families.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerTeleportServiceTests|FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 498-499, `docs/Phase-6CA-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
