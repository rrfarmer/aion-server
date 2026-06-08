# HANDOFF — current state

The single rolling state doc. Updated in place every Unit of Work (git history holds the past). Must be usable with zero prior conversation. Read after the other canonical docs in `csharp-port.md`.

Last updated: 2026-06-07

## Current position

- Phase 6, **re-baselined** to the Port Fidelity & Remediation Plan. The earlier Phase-6 work is behaviorally faithful but structurally over-decomposed (plan-service slop) and partly fused into a god-class; it is being re-ported to 1:1 Java fidelity.
- **Phase A (foundation) — DONE except formula golden capture:**
  - A1 doctrine: `Port-Fidelity-Remediation-Plan.md`. DONE.
  - A2 golden pipeline: DONE, proven end-to-end. Java generator `game-server/test/.../serverpackets/GoldenPacketFixtureGeneratorTest.java` → fixtures in `parity-artifacts/golden/packets/`; C# consumer `dotnetConversion/tests/Aion.GameServer.Tests/GoldenPacketFixtureTests.cs` asserts byte-for-byte. Covers SM_GROUP_DATA_EXCHANGE, SM_GF_WEBSHOP_TOKEN_RESPONSE.
  - A3 structural audit: DONE. `scripts/parity/structural_audit.py` → `Structural-Audit-Scorecard.md`.
  - A4 fidelity guardrail: DONE. `scripts/parity/check_fidelity.py` (ratchet; baseline `scripts/parity/fidelity-baseline.json` = 363 slop files + 6 god-classes), wired into CI `fidelity` job. Proven to fail on new slop and pass when clean.
  - **Formula golden capture: NOT built** (only remaining foundation item).
- Build: C# GameServer builds green (nullable warnings only). Golden test passes 2/2.

## Direction chosen: FOUNDATION-FIRST (bottom-up)

Decision (2026-06-07): build the missing runtime substrate bottom-up in Java dependency order, then the faked feature clusters collapse into faithful ~150-line services. The `Structural-Audit-Scorecard.md` is a magnitude map, not a work order.

**Why:** there is NO object hierarchy in C#. `Player` is a flat `sealed class` with no base; `VisibleObject`/`Creature`/`Npc`/`WorldObject`/`WorldMap` are MISSING. The whole C# model is a flat parallel collection built for the packet path (`Player`, `WorldNpc`, `WorldStaticObject`, `PlayerLifeStats`, …), separate from the Java tree. Everything above (teleport, combat, AI, known-list) is faked because there is no `Creature` to attach a controller/lifestats/effects/known-list to.

### Foundation sequence (the backlog)

Java spine sizes in parens. Each is a faithful 1:1 port of the named Java class.

- **F1. `AionObject`** (79) — base: objectId/name/equals/hashCode. **DONE** (`Model/GameObjects/AionObject.cs`). Trimmed to the dependency-free core per the no-defer rule — no stub/TODO. Its `autoReleaseObjectId` GC path is an *upward* behavior (respawn/id-release layer) with no caller; tracked as backlog item **FR-1** below, not deferred-in-place.
- **F2. `VisibleObject extends AionObject`** (256) + `VisibleObjectController` (124) — position/world membership/spawn/known-list+controller refs. Additive (new files).
- **F3. `Creature extends VisibleObject`** (522) + `CreatureController` (568) — **the task system** (`TaskId` enum + `addTask/hasTask/cancelTask` map) = the scheduler substrate the slop fakes; lifeStats/effect/move controller refs. Additive.
- **F4. Reparent flat C# `Player` → `Creature`** (Java Player 1655; C# Player 1442 lines, **329 referencing files**) — **THE high-risk integration + PIVOTAL DECISION** (big-bang vs gradual/strangler). Reconcile flat Player fields (objectId/name/position/world) with inherited ones.
- **F5. `Npc extends Creature`** (393) + `NpcController` (340) — reconcile flat `WorldNpc`/`WorldStaticObject`.
- **F6. `KnownList`** — visibility engine on VisibleObject.
- **F7. `World`/`WorldMap`/`MapRegion`** — faithful spatial container (current `World.cs` is flat).

After F1–F7, subsystems (teleport → its hotspot dataholder + the F3 task system) become portable, and BindPointTeleport/FindGroup/etc. collapse.

### Backlog (upward features owed, with their real prerequisite — not deferrals-in-place)

- **FR-1. `AionObject` GC objectId auto-release** — port when the respawn/id-release layer lands. Prereq: a process-wide IDFactory accessor + `RespawnService.setAutoReleaseId`. Java: `AionObject(int,boolean)` Cleaner branch.

## Last units (session 2026-06-07)

All green (build + guardrail 363/6):

| Commit | File(s) |
|--------|---------|
| `31242003e` | `model/animations` — 6 animation enums |
| `608f4c2c3` | `model/templates/BoundRadius + L10n (IL10n) + VisibleObjectTemplate` |
| `c01d57edb` | `world/WorldType + WorldDropType + spawnengine/SpawnHandlerType` |
| `7c61a213d` | `ai/event/AIEventType + model/templates/zone/ZoneClassName` |
| `85f92e112` | `world/zone/ZoneName + ZoneAttributes` |
| `cdbfa72a2` | `model/Race + model/siege/SiegeModType + model/vortex/VortexStateType` |
| `14b529960` | `model/TribeClass` (748-line; SCREAMING_SNAKE_CASE for XML compat) |
| `0388d9bf6` | `services/panesterra/ahserion/PanesterraFaction` |
| `d3f267c13` | `model/base/BaseOccupier` |
| `9db32c4bb` | `model/siege/SiegeRace` |
| `b32377ead` | `utils/time/gametime/DayTime + GameTime` |
| `e39b915cc` | `services/GameTimeService` — add GetInstance()/GetGameTime() |
| `22ebda2d1` | `utils/time/ServerTime` |
| `9620ea3f4` | `model/templates/spawns/TemporarySpawn` |
| `c11e46d09` | `model/templates/spawns/SpawnSpotTemplate` |
| `7e8906376` | **F2 spawn-data circular cluster — 50 files** (see below) |

### Commit `7e8906376` — F2 spawn-data circular cluster (50 files)

Entire mutually-referential cluster ported as one batch:
- `model/gameobjects/state/CreatureSeeState` — NPC sight-range enum
- `model/templates/npc/NpcRating` — quality enum (Junk→Legendary)
- `model/templates/npc/GroupDropType` — 300+ SCREAMING_SNAKE_CASE XML-compat enum
- `model/templates/globaldrops/StringFunction`
- `model/templates/globaldrops/GlobalDrop*` (20 files: Map, Maps, Npc, Npcs, NpcName, NpcNames, NpcGroup, NpcGroups, Race, Races, Rating, Ratings, Tribe, Tribes, World, Worlds, Zone, Zones, ExcludedNpcs, Item)
- `model/templates/globaldrops/GlobalRule`
- `model/templates/event/EventQuestList`
- `model/templates/event/InventoryDrop`
- `model/templates/event/Buff` (BuffMapType/TriggerCondition/Trigger; `Matches(WorldMapInstance)` TODO-backlog)
- `model/templates/event/BuffRestriction`
- `model/templates/event/EventTemplate` (LocalDateTimeAdapter → DateTime? + ISO-8601 string setter)
- `model/templates/spawns/SpawnType`
- `model/templates/spawns/SpawnSearchResult`
- `model/templates/spawns/basespawns/BaseSpawn`
- `model/templates/spawns/riftspawns/RiftSpawn`
- `model/templates/spawns/siegespawns/SiegeSpawn`
- `model/templates/spawns/vortexspawns/VortexSpawn`
- `model/templates/spawns/mercenaries/{MercenarySpawn, MercenaryRace, MercenaryZone}`
- `model/templates/spawns/panesterra/AhserionsFlightSpawn`
- `model/templates/spawns/SpawnMap`
- `model/templates/spawns/Spawn` (beforeMarshal → ShouldSerialize*)
- `model/templates/spawns/SpawnTemplate`
- `model/templates/spawns/SpawnGroup` (Rnd.get() → Random.Shared inline)
- `model/templates/spawns/basespawns/BaseSpawnTemplate`
- `model/templates/spawns/riftspawns/RiftSpawnTemplate`
- `model/templates/spawns/siegespawns/SiegeSpawnTemplate`
- `model/templates/spawns/vortexspawns/VortexSpawnTemplate`
- `model/templates/spawns/panesterra/AhserionsFlightSpawnTemplate`
- `dataholders/SpawnsData` (afterUnmarshal → `Initialize(parent?)`; saveSpawn / getNearestSpawnByNpcId / getFirstSpawnByNpcId / getRelativePath → TODO-backlog)

**Pending backlog additions from this batch:**
- TODO-backlog: `Buff.BuffMapTypeExtensions.Matches(WorldMapInstance)` — needs WorldMapInstance
- TODO-backlog: `GlobalDropItem` DataManager.ITEM_DATA validation — needs DI DataManager in load pipeline
- TODO-backlog in `SpawnsData`: saveSpawn, getNearestSpawnByNpcId, getFirstSpawnByNpcId, getRelativePath, loadSpawnsFromTemplateFiles, findSpawnTemplate, positionMatches, getNearestSpawn, toSpawnSearchResult (all need VisibleObject/Player/WorldMapInstance)

## How to choose the next unit (no-defer = strictly bottom-up)

**Never start a node that has an unmet dependency.** Build the dependency-free base and expand upward; you only reach a higher node once everything it needs already exists. The object-model spine (`AionObject→VisibleObject→Creature→Npc/Player` + `World`/`KnownList`/controllers) defines *which* foundation pieces are in scope (don't port unrelated dependency-free files); the no-defer rule defines the *order* (bottom-up). Note the trunk also needs *sideways* foundation (`KnownList`, controllers, templates, `MapRegion`) before each step up.

Algorithm each turn: from the in-scope spine, pick a unit whose dependencies **all already exist in C#**. If none do, pick the deepest still-missing dependency (it is itself such a unit). Read the Java fully, port 1:1, build, commit (code+HANDOFF). Never stub/defer.

## Next unit

`SpawnTemplate`/`SpawnGroup`/`SpawnsData` — **DONE** (commit `7e8906376`).

### Remaining path to `VisibleObject` (F2)

`VisibleObject` directly needs:
- `AionObject` ✅
- `ObjectDeleteAnimation` ✅
- `VisibleObjectTemplate` ✅
- `SpawnTemplate` ✅
- `WorldPosition` — **MISSING** (needs `WorldMapInstance` / `MapRegion`)
- `World` / `WorldMap` / `WorldMapInstance` — **MISSING**
- `KnownList` — **MISSING**
- `VisibleObjectController` — **MISSING** (needs `RespawnService` + `GeoService`)

**Next sub-cluster: world-object cluster** (port all together since they form another circular graph):
1. `WorldMapTemplate` (data holder for world config XML)
2. `WorldMap` (instance-maps per world)
3. `WorldPosition` (faithful: x/y/z/mapId/worldMapInstance ref)
4. `MapRegion` (spatial partition, ref to WorldMapInstance)
5. `WorldMapInstance` (world instance, ref to WorldMap + MapRegion list)
6. `VisibleObjectController` (needs `RespawnService` + `GeoService` — check if portable without them, or port those first)
7. `RespawnService` / `GeoService` (check deps)
8. `KnownList`

Then `VisibleObject` (F2), `Creature`/`CreatureController` (F3), etc.

Fidelity Gate only (foundation/additive). Validation: `dotnet build src/Aion.GameServer` + guardrail after each batch commit.

- **Before F4** (reparent flat `Player` → `Creature`, 329 files), surface the strategy decision (big-bang vs gradual). F2/F3 do not depend on it.

## Blockers / risks

- **The biggest slop clusters (BindPointTeleport, and likely FindGroup, WorldNpc, PlayerKnown, summons, vortex) are substrate-blocked** — they fake unported runtime (teleport, controller tasks, KnownList, scheduler). Don't attempt them top-down by file count; port substrate first (Track 1) or pick Track 2 units.
- Golden harness covers deterministic, constructor-driven packets and pure formulas only; singleton/time-dependent packets need a deterministic config harness first.
- `check_fidelity.py` matches by simple class name (naming-normalized). A faithful C# port named differently from its Java class could trip it — fix by matching the Java name, not by editing the baseline.
