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
| `f2d0c1f35` | **World infrastructure batch — 6 files** (see below) |
| `0d4cacf69` | **Geometry + zone-template batch — 19 files** (see below) |
| `22e208019` | **Creature spine prerequisites — 6 zero-dep files** (see below) |
| `b16f2db8c` | docs: Fidelity Doctrine rule 8 (conflicts default to 1:1 Java; replace existing) |
| `3994a5c75` | **Stats/skill enum leaves — StatEnum, SkillElement, ItemAttackType, AbnormalState** |

### Commit `3994a5c75` — Stats/skill enum leaves (4 files)

First batch of the **Creature SCC cone** (see "Strategy resolved" below), all dependency-free leaves:
- `Model/Stats/Container/StatEnum` — full stat-id enum (SCREAMING_SNAKE_CASE for XML); per-constant `itemStoneMask`+`sign` and static `GetModifier` in `StatEnumExtensions`
- `Model/SkillElement` — element→resistance-stat via `GetStatForElement()`
- `Model/Templates/Item/ItemAttackType` — `IsMagical()`/`GetMagicalElement()`
- `SkillEngine/Effect/AbnormalState` — bit-flag + compound masks; `int` base preserves `SANCTUARY=1<<31`

### Commit `22e208019` — Creature spine prerequisites (6 zero-dep files)

The remaining dependency-free leaves needed by the `Creature`/`Npc` spine, ported ahead of the (now blocked) F2 cluster:
- `Model/GameObjects/State/CreatureState` — bit-flag + multibit states; `GetId()` and `MustMatchExact()` extensions (CHAIR/PRIVATE_SHOP exact). Multibit ids are distinct so a single enum suffices.
- `Model/GameObjects/State/CreatureVisualState` — hide/blink states; `GetId()` extension
- `Model/TaskId` — controller task-slot enum (zero-dep)
- `Model/GameObjects/NpcObjectType` — npc-derived object kinds; `GetId()` extension
- `Model/GameObjects/CreatureTemplate` — abstract `VisibleObjectTemplate` subclass adding `GetAiName()` (virtual, returns null)
- `World/Zone/RegionZone` — `RectangleArea` region square spanning one `WorldConfig.WorldRegionSize`; passes `null!` ZoneName / worldId 0 per Java

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

### Commit `0d4cacf69` — Geometry + zone-template batch (19 files)

Zone template data classes (`Model/Templates/Zone/`):
- `Point2D` — float x/y XML attribute pair
- `AreaType` — Polygon/Cylinder/Sphere/Semisphere enum
- `Cylinder`, `Sphere`, `Semisphere` — XML geometry descriptors
- `Points` — polygon boundary points list with top/bottom Z
- `ZoneTemplate` — full XML zone descriptor; `name` attr → `ZoneName.CreateOrGet()`; `ZoneClassName.Sub` default
- `ZoneInfo` — `Area` + `ZoneTemplate` container pair

Geometry package (`Model/Geometry/`):
- `Point3D` — float x/y/z, `ICloneable`, `GetHashCode` matching Java `(int)(result * 100)`
- `Area` — interface (all `IsInside2D/3D`, `IsInsideZ`, `GetDistance2D/3D`, `GetClosestPoint`, `IntersectsRectangle`)
- `AbstractArea` — base implementation, `GetClosestPoint(float,float,float)` z-clamping
- `RectangleArea` — axis-aligned rect; `GetClosestPoint` via four edge walk; `IntersectsRectangle` is stub (Java TODO preserved)
- `CylinderArea` — circular cylinder; all geometry via `PositionUtil` pure methods
- `SphereArea` — sphere; 2D methods `@Deprecated` (return false/0/null matching Java)
- `SemisphereArea` — upper half-sphere extending `SphereArea`; `virtual` on overridden methods
- `Polygon2D` — float polygon; ray-casting (even-odd) for `Contains()` matches `GeneralPath.WIND_EVEN_ODD`; edge-intersection for `Intersects()`; rendering TODO-backlog
- `PolyArea` — free-form polygon area using `WorldConfig.WorldRegionSize`

Support:
- `Configs/Main/WorldConfig` — static with Java default values; TODO-backlog config-framework loading
- `Utils/PositionUtil` — pure coordinate methods only (2D/3D distance, angle/heading, `GetClosestPointOnSegment`, `NormalizeAngle`); game-object-aware methods TODO-backlog at F2/F3

### Commit `f2d0c1f35` — World infrastructure batch (6 files)

Zero-dep preparatory batch ahead of the F2-F5 world-object spine:
- `World/WorldMapType` — 207-member enum (all world IDs as PascalCase); extensions: `GetId()`, `IsPersonal()`, `GetWorld(int)`, `IsPanesterraMap(int)`
- `Model/Templates/Zone/ZoneType` — `Fly/NoFly/Siege/Pvp` enum
- `Utils/Collections/CollectionUtil` — safe `ForEach<T>` with error logging (two overloads)
- `World/Exceptions/DuplicateAionObjectException` — extends Exception; takes two `AionObject` args; TODO-backlog `Player.GetPosition()` at F4
- `Model/Templates/World/AiInfo` — `ChaseTarget=50`, `ChaseHome=200`, static `Default`
- `Model/Templates/World/WorldMapTemplate` — full XML data holder; `flags` `@XmlList @XmlAttribute` → `FlagsRaw` string parsed to `List<ZoneAttributes>`; `GetTwinCount`/`GetBeginnerTwinCount` TODO-backlog WorldConfig cap; bit-check methods via `(int)ZoneAttributes.*` casts

## How to choose the next unit (no-defer = strictly bottom-up)

**Never start a node that has an unmet dependency.** Build the dependency-free base and expand upward; you only reach a higher node once everything it needs already exists. The object-model spine (`AionObject→VisibleObject→Creature→Npc/Player` + `World`/`KnownList`/controllers) defines *which* foundation pieces are in scope (don't port unrelated dependency-free files); the no-defer rule defines the *order* (bottom-up). Note the trunk also needs *sideways* foundation (`KnownList`, controllers, templates, `MapRegion`) before each step up.

Algorithm each turn: from the in-scope spine, pick a unit whose dependencies **all already exist in C#**. If none do, pick the deepest still-missing dependency (it is itself such a unit). Read the Java fully, port 1:1, build, commit (code+HANDOFF). Never stub/defer.

## Next unit

All dependency-free leaves of the spine are now ported. The next node is `VisibleObject` (F2) — and it is **BLOCKED on a pivotal architectural decision** (see below). The zero-dep leaf supply is exhausted; we cannot make further bottom-up progress on the spine without resolving the `WorldPosition` fork.

### ✅ STRATEGY RESOLVED (user, 2026-06-07): big-bang replace + SCC-leaves-first

**Decision:** the `WorldPosition` fork (below) is resolved by **big-bang replace** — port the faithful Java `WorldPosition` class and fix all consumers. New permanent doctrine (Plan rule 8 / memory): *conflicts default to 1:1 Java parity, replacing existing C# code; a genuine C#-vs-Java foundational language difference instead takes the closest-to-1:1 path.* struct-vs-class is a C# idiom choice → class wins.

**Execution insight:** the big-bang `WorldPosition` swap is the **CLOSING move** of the Creature SCC, not the next move. The SCC (`VisibleObject ↔ Creature ↔ World/MapRegion/WorldMapInstance/WorldPosition ↔ KnownList ↔ subsystems`) only reaches a green build once it fully closes (single assembly → partial SCC = red). But its **leaves are dependency-free and ported bottom-up, green each batch**, until only the tightly-coupled core remains for one final big-bang commit (which includes the struct→class swap + 64-file migration). So the loop stays productive without further decisions.

**Creature SCC cone — leaf progress:**
- ✅ `CreatureState`, `CreatureVisualState`, `TaskId`, `NpcObjectType`, `CreatureTemplate`, `RegionZone` (commit `22e208019`)
- ✅ `StatEnum`, `SkillElement`, `ItemAttackType`, `AbnormalState` (commit `3994a5c75`)
- ⏭️ NEXT leaves to port (verify deps first): stat helper types (`StatOwner`/`Stat2`/modifiers), `TransformType`+`TransformModel`-prereqs, `NpcEquippedGear`, AI enums (`AISubState`, `AIState`), movement enums, `Skill`/`SkillTemplate` cone. Keep porting until only `VisibleObject`/`Creature`/`World*`/`MapRegion`/`KnownList`/controllers + stats/effect/ai/move *containers* remain → final big-bang.

### ⛔ The `WorldPosition` class-vs-struct fork (resolved above; details retained)

`VisibleObject` (F2) holds a `WorldPosition` and calls `getMapRegion()`, `getWorldMapInstance()`, `isSpawned()`, `setPosition()`, `getInstanceId()`. The faithful Java `WorldPosition` is a **mutable class** that:
- holds a mutable `MapRegion` reference + `isSpawned` flag,
- **derives** `instanceId` from `mapRegion.getParent().getInstanceId()` (not stored),
- exposes `setMapRegion/setXYZH/setZ/setH/setIsSpawned`.

The existing C# `World/WorldPosition.cs` is the opposite design — a **`readonly record struct`** `(int WorldId, float X, float Y, float Z, byte Heading, int InstanceId = 1)`: immutable value type, **InstanceId stored**, copied via `with { … }`, no `MapRegion`/`isSpawned` concept. It was built for the packet path.

**Blast radius (measured):** 64 files reference `WorldPosition`; ~273 reads of `.WorldId`, ~101 of `.InstanceId`, ~72 of `.Heading`; multiple `portalLocation with { InstanceId = … }` expressions; services that store/compare a literal `InstanceId` on a position (e.g. `InstanceRuntimeService`, `PlayerTeleportService`, `WorldNpcSpawnService`). Replacing the struct with the Java class is **not** a pure rename: value→reference semantics, `with`→constructor, and **stored→derived InstanceId** all change behavior, not just syntax.

No-defer forbids stubbing past it; foundation-first forbids skipping it. So this is a genuine fork that must be decided before F2 can proceed. Candidate strategies:
- **A. Big-bang replace** — delete the struct, port the Java `WorldPosition` class into the same namespace, and fix all 64 files (convert `with` to setters, reconcile stored-vs-derived InstanceId). One large, behavior-affecting commit.
- **B. Strangler / parallel** — port the Java class under a distinct name/namespace for the new spine, leave the struct for the legacy packet path, migrate callers incrementally. Risk: two `WorldPosition` types; guardrail matches by Java name (the spine one should own the name).
- **C. Adapter** — keep the struct as a pure coordinate value, and put `MapRegion`/`isSpawned`/derived-InstanceId on `VisibleObject` itself (where Java keeps them on the position). Diverges from 1:1 field placement.

**Resolved: option A (big-bang replace).** Executed as the closing move of the Creature SCC (see "Strategy resolved" above).

### Remaining path to `VisibleObject` (F2) once unblocked

`VisibleObject` directly needs (besides `WorldPosition`):
- `AionObject` ✅, `ObjectDeleteAnimation` ✅, `VisibleObjectTemplate` ✅, `SpawnTemplate` ✅, `WorldMapTemplate` ✅
- `World` / `WorldMap` / `WorldMapInstance` / `MapRegion` — **MISSING** (large spatial-container cluster; needs `ThreadPoolManager`, `ZoneService`, `QuestEngine`, `Creature`, `Player`)
- `KnownList` (+`KnownObject`) — **MISSING** (needs `Npc`, `Pet`, `Player`, `MapRegion`, `WorldPosition`, `PositionUtil` game-object overloads)
- `VisibleObjectController` — **MISSING** (needs `RespawnService` + `GeoService`)

**Creature (F3) sub-blocker:** `Creature`'s *fields* (not just upward behavior) are `AbstractAI`, `CreatureGameStats`, `CreatureLifeStats`, `EffectController`, `CreatureMoveController`, `ObserveController`, `AggroList`, `TransformModel`, `Skill` — and Creature's own methods call them (`isDead()`→lifeStats, `canAttack()`→effectController, ctor news `ObserveController`/`AggroList`/`AIEngine.newAI`). These are **downward** deps of Creature, so a faithful Creature compile pulls in the AI/stats/effects/skill/movement subsystems. This is the second large fork (how much of those subsystems to port as the Creature foundation) and should be scoped right after the `WorldPosition` decision.

### F2-F5 world-object circular cluster (one large batch, once unblocked)
- `WorldPosition` (class) + `MapRegion` + `WorldMap` + `WorldMapInstance` (abstract) + factory/2D/3D instances
- `ZoneInstance`, `ZoneHandler`/`AdvancedZoneHandler` interfaces, `InstanceHandler`/`GeneralInstanceHandler`
- `GeneralTeam` abstract; `KnownList` + `KnownObject`; `VisibleObjectController`
- `VisibleObject` (F2); `Creature` + `CreatureController` (F3); `StaticObject`/`StaticDoor`; `Npc` + `NpcController` (F5); `Pet`
- reparent `Player` → `Creature` (F4); `World` singleton; `RespawnService`, `GeoService`, `ThreadPoolManager`; `PositionUtil` game-object overloads

Fidelity Gate only (foundation/additive). Validation: `dotnet build src/Aion.GameServer` + guardrail after each batch commit.

## Blockers / risks

- **⛔ `WorldPosition` class-vs-struct fork blocks all further spine progress** — see the PIVOTAL BLOCKER section above. Needs a user strategy decision before F2.

- **The biggest slop clusters (BindPointTeleport, and likely FindGroup, WorldNpc, PlayerKnown, summons, vortex) are substrate-blocked** — they fake unported runtime (teleport, controller tasks, KnownList, scheduler). Don't attempt them top-down by file count; port substrate first (Track 1) or pick Track 2 units.
- Golden harness covers deterministic, constructor-driven packets and pure formulas only; singleton/time-dependent packets need a deterministic config harness first.
- `check_fidelity.py` matches by simple class name (naming-normalized). A faithful C# port named differently from its Java class could trip it — fix by matching the Java name, not by editing the baseline.
