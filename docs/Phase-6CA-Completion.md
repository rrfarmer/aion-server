# Phase 6CA Completion Handoff

**Created**: May 23, 2026  
**Status**: Phase 6 remains in progress; this handoff follows 6BZ and covers Sessions 496-497.  
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.  
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.  
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.  
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 1045 tests.

---

## Recent Work Completed

- Audited the remaining direct `World.TryRemoveObject` callers called out in 6BZ.
- Confirmed walker swap rollback paths do not create modeled PVP/FORT counters before rollback removals because revalidation occurs only after swap success.
- Wired `GameServerConnection.RemoveRuntimeKiskAsync` to invoke `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` even when no `IGameClientConnectionRegistry` is available, allowing no-registry counter cleanup.
- Added a no-registry runtime kisk cleanup regression using real Java PVP geometry.
- Added compact formation walker variant PVP coverage for `TrySwapInactiveWalkerFormationVariant`.
- Extended the synthetic versioned formation fixture with deterministic PVP geometry covering active and parked formation members.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 497 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `7253e5bc4` - `Clear kisk pvp counters without registry`
- `b99ab32e5` - `Cover formation variant pvp counters`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `Kisk.KiskLifeTask` | `GameServerConnection.RemoveRuntimeKiskAsync` + `PlayerKiskLifetimeService.DespawnExpiredKisk` | Partial | Regression Tested | Partial Parity | Connection-level runtime kisk removal now clears modeled counters even without a connection registry. Java scheduled task/controller delete flow, exact timer semantics, and full controller callbacks remain incomplete. |
| `KiskService.removeKisk` | `PlayerKiskRemovalRuntimeCleanupService.ApplyAsync` | Partial | Regression Tested | Partial Parity | Cleanup now runs in registry-less contexts, clearing counters without player packet fanout. Java offline bind removal, creator update, member bind reset, and resurrection refresh are only possible when registry data is available. |
| `World.removeObject` / `World.despawn` for kisk lifetime | `PlayerKiskLifetimeService.DespawnExpiredKisk` plus cleanup apply | Partial | Regression Tested | Intentional Difference | C# removes the world object directly and then runs modeled cleanup. Java despawn flows through controller/map-region internals and executes zone handlers/callbacks. |
| `InstanceWalkerFormations.changeCluster` | `WorldNpcSpawnService.TrySwapInactiveWalkerFormationVariant` | Partial | Regression Tested | Partial Parity | Formation variant swaps now have direct PVP counter coverage for parked and activated members. Java trigger callers from death/AI/instance callbacks remain future work. |
| `WalkerGroup.spawn/despawn` | `WorldNpcSpawnService` formation add/remove hooks | Partial | Regression Tested | Partial Parity | Regression proves active formation members enter counters and parked members clear counters. Java full group movement state, instance callbacks, and controller side effects remain incomplete. |
| `Creature.revalidateZones` / `MapRegion.revalidateZones` | `CreaturePvpZoneRevalidationService.Revalidate` from kisk and formation boundaries | Partial | Unit + Regression Tested | Partial Parity | Real PVP geometry covers no-registry kisk cleanup; synthetic PVP geometry covers formation variant swaps. Zone priorities, handlers, neighboring regions, full-map zones, and SIEGE/FORT formation coverage remain incomplete. |
| `PvPZoneInstance.onEnter/onLeave` | `CreaturePvpZoneCounterService` | Partial | Unit + Regression Tested | Partial Parity | Tests prove stale modeled PVP counters clear after no-registry kisk removal and formation variant swap lifecycle transitions. Java handler ordering and side effects remain unverified. |

Metrics from this handoff window:

- Total focused sessions covered: 2
- Total commits covered: 2
- Current full validation baseline: 1045 tests passing
- Total artifacts with verified parity newly promoted in this window: 0
- Total blocked artifacts: Java zone handlers/controller callbacks, Java kisk controller/timer ordering, registry packet fanout when no registry exists by design, SIEGE/FORT formation coverage, formation trigger callers, Java queued scheduler/levels, full formation AI/movement parity, and live socket ordering
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; remaining kisk revive cleanup, live team membership wiring, production socket-order validation, broader teleport/map-change zone revalidation, remaining direct-removal cleanup, generic visible-object cleanup, full dedicated kisk controller/AI, full NPC/dialog AI, resurrection skill/effect callers, per-zone bind membership, live option mutation callers, admin option consumers, world-map instance ownership, object iteration, full movement-controller parity, full audit subsystem, full stat/effect runtime, combat, loot distribution, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- Registry-less kisk cleanup cannot send Java-equivalent `SM_KISK_UPDATE`, bind-point reset, or resurrection-option refresh packets because there is no player registry to address. It now guarantees modeled counter cleanup only.
- C# still clears modeled counters directly instead of executing Java zone `onLeave` handlers, controller callbacks, quest/material handlers, fortress observers, or instance callbacks.
- Formation variant trigger callers from Java death/AI/instance callbacks remain absent; the current regression validates the service boundary once called.
- Formation coverage uses synthetic PVP geometry. SIEGE/FORT formation variant coverage remains open.
- Timer scheduling semantics, Java controller delete ordering, full group AI movement, route event state, and live encrypted packet ordering remain unverified.

---

## Next Unit Of Work

Recommended next unit: continue remaining teleport/map-change completion paths outside kisk revive, or add SIEGE/FORT formation variant coverage if a compact fixture can be made without pulling in broader siege behavior.

1. Re-read Java for teleport/map-change:
   - `game-server/src/com/aionemu/gameserver/services/teleport/TeleportService.java`
   - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_TELEPORT_ANIMATION_DONE.java`
   - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEVEL_READY.java`
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
2. Re-read C#:
   - `GameServerConnection.TeleportPlayerToKiskPositionAsync`
   - other teleport/map-change helpers in `GameServerConnection`
   - `PlayerTeleportService`
   - `CreaturePvpZoneRevalidationService`
   - `CreaturePvpZoneCounterService`
3. Keep the next unit narrow:
   - choose one non-kisk teleport or map-change completion path
   - mutate player position first, then revalidate modeled PVP/FORT counters
   - preserve existing packet ordering
   - add focused regression and progress parity table

Strong alternatives:

1. Add SIEGE/FORT formation variant coverage using synthetic or real FORT geometry.
2. Add broader socket-order tests around viewer-specific kisk `SmNpcInfo` followed by loot-status/deletion packets.
3. Continue dedicated `KiskController`/AI/dialog/death layering beyond the generic death bridge.
4. Audit static object, gatherable, town, pooled respawn, and per-instance pool state spawn families.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~CreaturePvpZoneRevalidationServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldNpcSpawnServiceTests|FullyQualifiedName~CreaturePvpZoneCounterServiceTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 496-497, `docs/Phase-6BZ-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
