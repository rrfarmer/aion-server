# HANDOFF — current state

The single rolling state doc. Updated **in place** every Unit of Work — keep it lean: detail only the most recent round, collapse older batches to one-liners (git history holds the rest), but **never drop a TODO/backlog/blocker**. Must be usable with zero prior conversation. Read after the canonical docs in `csharp-port.md`.

Last updated: 2026-06-08

## Current position

- Phase 6, **re-baselined** to the Port Fidelity & Remediation Plan (`docs/Port-Fidelity-Remediation-Plan.md`). Earlier Phase-6 work is behaviorally faithful but structurally slop (plan-service sprawl + a god-class `GameServerConnection.cs`); being re-ported to 1:1 Java fidelity.
- **Phase A (foundation): DONE except formula golden capture.** Golden packet pipeline proven (Java generator → `parity-artifacts/golden/packets/`; C# `GoldenPacketFixtureTests` asserts byte-for-byte). Structural audit + fidelity guardrail (`scripts/parity/check_fidelity.py`, baseline 363 slop / 6 god-classes, in CI) done. **TODO: formula golden capture not built** (last foundation item).
- Build: C# GameServer green (nullable warnings only).
- **Active work: building the missing object-model spine bottom-up** (see below).

## Direction: FOUNDATION-FIRST (bottom-up), 1:1 Java

There is NO object hierarchy in C#: `Player` is a flat `sealed class`; `VisibleObject`/`Creature`/`Npc`/`World`/`WorldMap` are MISSING. The C# model is a flat parallel collection built for the packet path. Build the spine bottom-up so the faked feature clusters can later collapse.

**Spine sequence** (each a faithful 1:1 port; Java line counts in parens):
- **F1 `AionObject`** (79) — **DONE** (`Model/GameObjects/AionObject.cs`).
- **F2 `VisibleObject`** (256) + `VisibleObjectController` (124).
- **F3 `Creature`** (522) + `CreatureController` (568) — incl. the TaskId task system.
- **F4 reparent flat `Player` → `Creature`** (Java 1655; C# 1442, **329 referencing files**) — high-risk integration.
- **F5 `Npc`** (393) + `NpcController` (340).
- **F6 `KnownList`**; **F7 `World`/`WorldMap`/`MapRegion`**.

**How to choose the next unit:** never start a node with an unmet dependency. From the in-scope spine cone, pick a unit whose deps all already exist in C#; if none, port the deepest still-missing dep. Read the Java fully, port 1:1, build, commit (code + HANDOFF). **Never stub/defer.** Don't port dependency-free files unrelated to the spine cone.

## Strategy resolved (user, 2026-06-07): big-bang + SCC-leaves-first

- **Doctrine rule 8** (in `Port-Fidelity-Remediation-Plan.md` §3 + memory): when a new faithful port conflicts with existing C# code, **default to 1:1 Java parity and replace the existing code**; only when the conflict is a genuine **C#-vs-Java foundational language difference** take the closest-to-1:1 path instead.
- **Execution model:** `VisibleObject ↔ Creature ↔ World/MapRegion/WorldMapInstance/WorldPosition ↔ KnownList ↔ subsystems` form one big SCC. In a single assembly a partial SCC won't compile, so the SCC core lands as **one final big-bang commit**. Until then, port the SCC's **dependency-free leaves** bottom-up (green each batch). The `WorldPosition` struct→class swap (below) is part of that closing big-bang.

## Creature SCC cone — leaf progress

Older spine/leaf batches (git has detail): animations, templates (`BoundRadius`/`IL10n`/`VisibleObjectTemplate`), world/zone enums (`WorldType`,`WorldDropType`,`ZoneName`,`ZoneAttributes`,`ZoneClassName`,`AIEventType`), `Race`/`TribeClass`/siege/vortex, gametime, **F2 spawn-data cluster (50 files)**, **world-infra batch (6)**, **geometry+zone-template batch (19)**.

Recent (this program of work):
- `22e208019` Creature prereqs: `CreatureState`,`CreatureVisualState`,`TaskId`,`NpcObjectType`,`CreatureTemplate`,`RegionZone`
- `3994a5c75` stats/skill enums: `StatEnum`,`SkillElement`,`ItemAttackType`,`AbnormalState`
- `fb0b2091c` AI/observer: `AISubState`,`AIState`,`ObserverType`
- `449d687e5` `TransformType`,`HopType`,`ShieldType`,`CalculationType`,`IStatOwner`
- `effb209b7` SkillTemplate enums (9): `ActivationAttribute`,`DispelCategoryType`,`HostileType`,`SkillCategory`,`SkillSubType`,`SkillType`,`StigmaType`,`SkillTargetSlot`,`DispelSlotType`
- `3538242ab` effect enums: `SpellStatus`,`EffectResult`,`HitType`,`AttackType`,`EffectType`
- `28e8fc389` skill condition/properties/change: `TargetAttribute`,`AreaDirections`,`FirstTargetAttribute`,`TargetRangeAttribute`,`TargetRelationAttribute`,`TargetSpeciesAttribute`,`Func`
- `634e6196a` effect-controller: `CumulativeResistType`,`CumulativeResist`
- `310ce8879` item enums: `ArmorType`,`EquipType`,`ItemSubType`
- `17c02776a` item: `ItemSlot` (long-mask), `ItemGroup`
- `dc5c40c03` stats-template: `StatsTemplate`,`CreatureSpeeds`
- `6d981ed5d` `PlayerClass` + `PlayerStatCalculator` (mutual SCC) — **PlayerClass unblocked**. (Java `implements L10n`; C# enums can't implement interfaces → `GetL10nId()` extension per rule 8. New `enum PlayerClass` coexists additively with legacy `string Player.PlayerClass`, reconciled at F4.)

**Leaf-vein status:**
- **skillengine** enum leaves EXHAUSTED — `Skill`/`SkillTemplate`/`Effect`/`EffectTemplate`/`Condition(s)`/`Change`/`Action(s)`/`PeriodicAction(s)` all block on the SCC core (`Stat2`/`Effect`/`Skill`/`Creature`).
- **stats** cone leaf-complete for spine (`Stat2`/`StatCapUtil` block on `Creature`). Out-of-cone, deliberately NOT ported: `PlumStatEnum`,`DropRewardEnum`,`XPLossEnum`,`XPRewardEnum`.
- **ACTIVE VEIN → item cone** (required: `Creature`→`NpcEquippedGear`→`ItemTemplate`). **NEXT:** remaining zero-dep item enums (`ItemQuality`,`ItemType`,`WeaponType`,`LeftHandSlot`,`RandomType`,`AcquisitionType`,`ExceedEnchantSkillSetType`) + small item data-classes (`Acquisition`,`GodstoneInfo`,`WeaponStats`,`Improvement`,…), then climb to `ItemTemplate` itself.

## Backlog / TODOs (come back to these)

**The final big-bang (closes the SCC):**
- **`WorldPosition` struct→class swap.** Java `WorldPosition` is a *mutable class* holding a `MapRegion` ref + `isSpawned`, with `instanceId` **derived** from `mapRegion.getParent().getInstanceId()`. Existing C# is a `readonly record struct (WorldId,X,Y,Z,Heading,InstanceId=1)`: immutable, InstanceId stored, copied via `with{}`. **Blast radius: 64 files / ~273 `.WorldId` + ~101 `.InstanceId` + ~72 `.Heading` reads + `with{InstanceId=…}` sites.** Replacing is behavior-affecting (value→ref, `with`→setters, stored→derived InstanceId), not a rename. Resolved = **big-bang replace** (rule 8).
- **Creature field-cone (2nd fork):** `Creature`'s fields `AbstractAI`/`CreatureGameStats`/`CreatureLifeStats`/`EffectController`/`CreatureMoveController`/`ObserveController`/`AggroList`/`TransformModel`/`Skill` are *downward* deps (its methods call them), so a faithful `Creature` compile pulls in the AI/stats/effect/skill/move containers. Scope these into the big-bang.
- **F2-F5 big-bang membership:** `WorldPosition`(class)+`MapRegion`+`WorldMap`+`WorldMapInstance`(+factory/2D/3D); `ZoneInstance`,`ZoneHandler`/`AdvancedZoneHandler`,`InstanceHandler`/`GeneralInstanceHandler`; `GeneralTeam`; `KnownList`+`KnownObject`; `VisibleObjectController`; `VisibleObject`(F2); `Creature`+`CreatureController`(F3); `StaticObject`/`StaticDoor`; `Npc`+`NpcController`(F5); `Pet`; reparent `Player`(F4); `World` singleton; `RespawnService`,`GeoService`,`ThreadPoolManager`; `PositionUtil` game-object overloads.

**Owed upward features (port when their prereq lands):**
- **FR-1 `AionObject` GC objectId auto-release** — needs process-wide IDFactory accessor + `RespawnService.setAutoReleaseId` (Java `AionObject(int,boolean)` Cleaner branch).
- `Buff.BuffMapTypeExtensions.Matches(WorldMapInstance)` — needs `WorldMapInstance`.
- `GlobalDropItem` `DataManager.ITEM_DATA` validation — needs DI DataManager in load pipeline.
- `SpawnsData`: `saveSpawn`,`getNearestSpawnByNpcId`,`getFirstSpawnByNpcId`,`getRelativePath`,`loadSpawnsFromTemplateFiles`,`findSpawnTemplate`,`positionMatches`,`getNearestSpawn`,`toSpawnSearchResult` — need `VisibleObject`/`Player`/`WorldMapInstance`.
- `WorldConfig` config-framework loading (currently hardcoded Java defaults).
- `PositionUtil` game-object-aware overloads (only pure-coordinate methods ported) — at F2/F3.
- `WorldMapTemplate.GetTwinCount`/`GetBeginnerTwinCount` — WorldConfig cap.
- `DuplicateAionObjectException` — `Player.GetPosition()` detail at F4.
- `Polygon2D` rendering methods (`getPolyline2D`/`getPolygon`/`getBounds`/`getPathIterator`) and `RectangleArea.IntersectsRectangle` (Java TODO stub preserved) — no server callers.

## Validation

Fidelity Gate (foundation/additive): `dotnet build src/Aion.GameServer` + `python scripts/parity/check_fidelity.py` after each batch commit. Commit author `rrfarmer <ryanfarmer@mac.com>`, no AI co-author.

## Blockers / risks

- Big slop clusters (BindPointTeleport, FindGroup, WorldNpc, PlayerKnown, summons, vortex) are **substrate-blocked** — they fake unported runtime; don't attempt top-down by file count. Port substrate first.
- Golden harness covers deterministic, constructor-driven packets + pure formulas only; singleton/time-dependent packets need a deterministic config harness first.
- `check_fidelity.py` matches by simple (normalized) class name — a faithful port named differently from its Java class can trip it; fix by matching the Java name, not by editing the baseline.
