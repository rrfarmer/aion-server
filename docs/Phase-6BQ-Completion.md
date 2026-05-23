# Phase 6BQ Completion Handoff

**Created**: May 23, 2026
**Status**: Phase 6 remains in progress; this handoff follows 6BP and covers Sessions 442-445.
**Project rule**: This is a 1:1 parity rewrite from the Java project to C#. Java remains the source of truth for packet layouts, guard order, world/known-list behavior, persistence behavior, scheduling, stat formulas, observer side effects, and combat math.
**Workflow rule**: Do one focused unit of work, validate it, update the migration parity table, commit it, then repeat for as long as useful work remains.
**Code trace rule**: New C# GameServer parity methods should include a short `Java parity: path::method` comment pointing at the Java source behavior being mirrored.
**Current validation baseline**: `dotnet test dotnetConversion\AionServer.slnx` passes with 957 tests.

---

## Recent Work Completed

- Added connection-level regression tests for Java `PlayerController.onLeaveFlyArea` fanout order.
- Exposed `GameServerConnection.RevalidatePlayerFlightZonesAsync` internally to the test assembly through `InternalsVisibleTo`, avoiding reflection while keeping the helper out of the public API.
- Verified the two currently ported fly-zone leave branches:
  - flying+gliding downgrade emits owner `SM_STATS_INFO`, visible-player `SM_EMOTION(CHANGE_SPEED)`, then `SM_EMOTION(STOP_FLY)`.
  - flying-only forced end emits owner `SM_STATS_INFO`, visible-player `SM_EMOTION(CHANGE_SPEED)`, then `SM_EMOTION(LAND)`.
- Added `FlightZoneSummary.CanFly` / `CanGlide` to mirror Java `ZoneInstance.canFly` / `canGlide` option precedence without changing `ZoneType.FLY` / `NO_FLY` membership behavior.
- Added the remaining Java `WorldMap` option readers to `WorldMapSummary`: kisk/bind, recall, ride, fly-ride, PvP, duel, and return-to-battle.
- Added pure-return `WorldMapSummary.SetWorldOption` / `RemoveWorldOption` helpers for Java `WorldMap.setWorldOption` / `removeWorldOption`.
- Added `WorldMapRuntimeState` as the first runtime owner for Java `WorldMap.worldOptions`, with current-flag mutation and option readers.
- Updated `docs/PHASE-6-PROGRESS.md` through Session 445 with migration parity tables, risks, metrics, validation, and next recommended work.

---

## Commits In This Handoff

- `da317d8ef` - `Test flight zone transition fanout order`
- `e6f33f582` - `Add flight zone option precedence helpers`
- `89e2af380` - `Add world map option readers`
- `ae7b3736e` - `Add world map runtime option state`

---

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|
| `controllers/PlayerController.onLeaveFlyArea` | `GameServerConnection.RevalidatePlayerFlightZonesAsync` / `ApplyFlightZoneTransitionFanoutAsync` | Partial | Regression Tested | Partial Parity | Connection-level tests now assert stats, speed, and `STOP_FLY` / `LAND` ordering for the two ported leave branches. Full encrypted socket path and spawned guard remain needs-verification. |
| `model/stats/container/PlayerGameStats.updateStatsAndSpeedVisually` | `PlayerVisualStatsUpdateService.UpdateStatsAndSpeedVisuallyAsync` | Partial | Regression Tested | Partial Parity | Fanout tests verify the connection path calls visual stats before the terminal flight emotion. Full stat/effect/equipment formulas are still partial. |
| `network/aion/serverpackets/SM_STATS_INFO` | `SmStatsInfo` | Partial | Regression Tested | Partial Parity | Fanout order is now tested. Payload parity remains covered by existing packet tests rather than new Java golden vectors. |
| `network/aion/serverpackets/SM_EMOTION` | `SmEmotion` | Partial | Regression Tested | Partial Parity | Fanout tests parse C# payloads for `CHANGE_SPEED`, `STOP_FLY`, and `LAND` ids. Exact live socket ordering remains incomplete. |
| `world/zone/ZoneInstance.canFly` / `canGlide` | `FlightZoneSummary.CanFly` / `CanGlide` | Partial | Unit Tested | Partial Parity | Java option precedence is ported for `FLY` / `GLIDE`. These helpers are not used for fly/no-fly membership and should not be wired there blindly. |
| `model/templates/zone/ZoneTemplate.flags` | `FlightZoneSummary.Flags` | Partial | Unit + Integration Tested | Partial Parity | Existing parser retains Java flag integers; new helpers consume them for option checks. Broader zone options still need live consumers. |
| `world/WorldMap.worldOptions` | `WorldMapRuntimeState.CurrentFlags` | Partial | Unit Tested | Partial Parity | Runtime state initializes from static map flags and supports Java-style option mutation. It is not yet owned by a map registry. |
| `world/WorldMap.setWorldOption` / `removeWorldOption` | `WorldMapSummary.SetWorldOption` / `RemoveWorldOption`; `WorldMapRuntimeState` mutation methods | Partial / Refactored | Unit Tested | Partial Parity / Intentional Difference | Runtime state mutates current flags; immutable summary helpers return updated values for composition. |
| `world/WorldMap` option readers | `WorldMapSummary` and `WorldMapRuntimeState` option readers | Partial | Unit Tested | Partial Parity | Flight, glide, kisk, recall, ride, fly-ride, PvP, duel, and return-to-battle readers are modeled. Live callers remain mostly unwired. |
| `world/WorldMap` instance/iterator APIs | No C# equivalent in this handoff | Not Started | No Tests | Needs Verification | Java also owns world-map instances, next instance id, instance lookup/removal, and object iteration. |
| `network/aion/clientpackets/CM_MOVE` / `CM_MOVE_IN_AIR` / `CM_SUBZONE_CHANGE` | `GameServerConnection` packet switch call sites | Partial | Source-Derived Integration Path | Needs Verification | The tested helper is the same one these packets call, but the encrypted packet loop itself is not directly tested. |

Metrics from the current handoff window:

- Total focused sessions covered: 4
- Total commits covered: 4
- Total artifacts with verified runtime parity: 0
- Total blocked artifacts: 0 in these slices
- Estimated overall migration completion: Phase 6 remains about 56% complete as a conservative game-core estimate; shared mutable world-map registries, world-map instance ownership, live option consumers, full socket-order harnesses, full zone lifecycle handlers, full movement-controller parity, full audit subsystem, full transform model, full stat-function/effect resolution, attack-speed extraction, DP cap extraction, group/alliance/GM state fanout, live HP/MP/FP max-resource lookup, full reward-loop orchestration, team distribution, PVP AP/XP reward branches, quest reward pipeline, full effect runtime, scheduled callbacks, AI handlers, team loot, dynamic handlers, instances, and quests remain broad open areas.

---

## Important Limits

- `WorldMapRuntimeState` is standalone. `World`, `GameServerRuntimeContext`, and data loading do not yet expose a map-id runtime registry.
- Most newly ported world-option readers have no live caller yet.
- Java `WorldMap` instance ownership is still absent: instance maps, next instance id, accessible instance lookup, removal, and object iteration remain unported.
- Zone membership remains current-position recomputation, not Java synchronized `ZoneInstance` membership collections.
- `FlightZoneSummary.CanFly` / `CanGlide` are option helpers only. Java `FlyZoneInstance.onEnter` sets `ZoneType.FLY` from zone type, so do not use these helpers to filter `zone_type="FLY"` polygon membership without a proven Java call site.
- Fly-zone fanout is connection-level regression tested, but not driven through the encrypted client socket loop.
- FP reduce/restore remains task-intent flags on `Player`, not scheduled Java `PlayerLifeStats` tasks.
- The audit bridge is still an `ILogger` breadcrumb, not Java `AuditLogger` with staff notification and punishment hooks.
- `InternalsVisibleTo` is used for test-only access to connection revalidation; reflection is not used.
- Serialization is unchanged except for parsing existing C# packet payloads in tests. Date/time is not introduced. Threading for runtime world options is simple mutable in-memory state and needs review before becoming shared live registry state.

---

## Next Unit Of Work

Recommended next unit: wire `WorldMapRuntimeState` into a small map-id registry owned by the game runtime so future option consumers can read current world options from one place. Keep it scoped to construction from loaded `WorldMapSummary` rows, lookup by map id, and set/remove option mutation.

Strong alternatives:

1. Wire the first live Java option consumer:
   - `model/templates/item/actions/RideAction.java` and `PlayerController.onEnterZone` for ride restrictions.
   - `model/templates/item/actions/ToyPetSpawnAction.java` for kisk/pet spawn restrictions.
   - `data/handlers/admincommands/Zone.java` for admin zone-info output.
2. Extend flight verification down to an actual `CM_MOVE`, `CM_MOVE_IN_AIR`, or `CM_SUBZONE_CHANGE` packet-loop test.
3. Continue Java `PlayerLifeStats` FP scheduling: reduce/restore amounts, periods, max-FP guards, dead checks, unlimited-flight-time access, task locks, and scheduler callbacks.

Suggested order:

1. Re-read:
   - `game-server/src/com/aionemu/gameserver/world/WorldMap.java`
   - `game-server/src/com/aionemu/gameserver/world/World.java`
   - `game-server/src/com/aionemu/gameserver/world/zone/ZoneInstance.java`
   - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/RideAction.java`
   - `game-server/src/com/aionemu/gameserver/model/templates/item/actions/ToyPetSpawnAction.java`
   - Current C# `WorldMapSummary`, `WorldMapRuntimeState`, `World`, `GameServerRuntimeContext`, `FlightZoneSummary`, and related tests.
2. Pick one focused Java-parity unit.
3. Keep Java breadcrumbs in code for each behavior copied.
4. Update `PHASE-6-PROGRESS.md` with the next session, migration parity table, tests, risks, metrics, and next recommended unit.
5. Commit the unit before moving on.

Useful validation commands:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~StaticDataLoadingTests|FullyQualifiedName~PlayerZoneStateServiceTests"
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFlightZoneFanoutTests|FullyQualifiedName~PlayerVisualStatsUpdateServiceTests|FullyQualifiedName~GamePacketTests"
dotnet test dotnetConversion\AionServer.slnx
```

---

## Resume Checklist

- Start from branch `4.8`.
- Confirm `git status --short` is clean.
- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md` Sessions 442-445, `docs/Phase-6BP-Completion.md`, and this handoff.
- Continue with one focused Java-parity unit.
- Update the migration parity table before committing.
- Commit the unit, then repeat until the next handoff is needed.
