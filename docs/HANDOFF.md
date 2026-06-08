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
- `c1ff3c9c1`+`f55e20a9a` item enums: `ItemQuality`,`ItemType`,`WeaponType`,`LeftHandSlot`,`AcquisitionType`,`RandomType`,`ExceedEnchantSkillSetType`(116)
- item data-classes (13): `Acquisition`,`GodstoneInfo`,`WeaponStats`,`Improvement`,`Idian`,`Disposition`,`RequireSkill`,`AssemblyItem`,`AssembledItem`,`TradeinItem`,`TradeinList`,`ExtraInventory`,`RandomItem`
- `Gender`,`ItemId`,`ItemMask`; NpcTemplate-cone (7): `AbyssNpcType`,`NpcRank`,`NpcTemplateType`,`SubDialogType`,`MassiveLoot`,`TalkInfo`,`KiskStatsTemplate`; `CreatureType`
- **`Race` re-ported to SCREAMING_SNAKE** (XML data parity; restored `LIVINGWATER(28)` alias; migrated `BaseOccupier`/`SiegeRace`) + `ItemActivationTarget` unblocked
- **`DialogAction`** — 1:1 port of the 6246-line Java file (6205 `const int` + reflection name lookup); guardrail refined to exempt faithful large Java-mirrored files (baseline 6→5 god-classes)

**Leaf-vein status — clean leaves essentially EXHAUSTED.** skillengine/stats/effect/AI/item-enum/item-data/npc-template-enum veins all harvested. Remaining cone nodes block on the SCC core or are special cases (see TODOs). **No further trivially-clean leaves to port without a decision or the big-bang.** Out-of-cone, deliberately NOT ported: `PlumStatEnum`,`DropRewardEnum`,`XPLossEnum`,`XPRewardEnum`.

## ⭐ BIG-BANG IN PROGRESS — branch `feature/object-spine-bigbang`

Executing the SCC on a branch via the **compiler-error-frontier** method: port faithful (pure-Java) files in dependency order; the branch build stays **red until the SCC converges**, then merge to main only when green + guardrail + golden pass. **No transitional fakery** — since nobody else is blocked and we merge only when faithful+green, `instanceId` derives from `MapRegion` per Java (the old stored-InstanceId struct was a non-Java artifact). `main` stays green; do NOT merge until convergence.

**Done on branch:** `World/WorldPosition` struct → faithful Java **class** (mutable, holds `MapRegion`, derived `GetInstanceId`/`IsInstanceMap`; C# WorldId/X/Y/Z/Heading/InstanceId convenience props kept for legacy readers).
**Current error frontier (next to port):** `MapRegion` (8 refs), `WorldMapInstance` (2 refs).
**Convergence reconciliation (will resurface once WorldPosition resolves):** 4 legacy sites set the old stored InstanceId — `GameServerConnection.cs:14718/14722`, `InstanceRuntimeService.cs:147` (`with { InstanceId = … }`), `PlayerTeleportService.cs:159` (6-arg ctor). Rework to set position via the spawn/instance flow (derived).
**Build order (bottom-up):** WorldPosition✅ → MapRegion → WorldMapInstance(+2D/3D+factory) → WorldMap → ZoneInstance(+ZoneHandler/AdvancedZoneHandler) → VisibleObjectController → VisibleObject → KnownList/KnownObject → InstanceHandler/GeneralInstanceHandler → GeneralTeam → Creature(+CreatureController) + Tier-B subsystem containers → Npc/NpcController/Pet/StaticObject/StaticDoor → World singleton (+PlayerContainer/SiegeNpc/services) → reparent Player (F4) → reconcile the 4 InstanceId sites + legacy WorldPosition readers → green → merge.

## ⭐ BIG-BANG SCOPE (tiers — full membership)

The object-model spine is one strongly-connected component: in a single C# assembly a partial SCC won't compile, so it lands as **one large red-until-green effort** (recommend a dedicated branch/worktree). Build it bottom-up internally; commit only when the whole assembly builds + guardrail passes. Validate against golden packet/formula fixtures where they exist.

**Tier A — core spine (~3.7k Java L, ~20 files):** `WorldPosition`(class,224) · `MapRegion`(223) · `WorldMap`(215) · `WorldMapInstance`(349)+`WorldMap2D/3DInstance`+`WorldMapInstanceFactory` · `World`(350) · `VisibleObject`(256)+`VisibleObjectController`(124) · `Creature`(522)+`CreatureController`(568) · `Npc`(393)+`NpcController`(340) · `Pet`(49) · `StaticObject`(18)+`StaticDoor`(87) · `KnownList`(269)+`KnownObject`(51) · `ZoneInstance`(177)+`ZoneHandler`/`AdvancedZoneHandler` · `InstanceHandler`+`GeneralInstanceHandler` · `GeneralTeam` · support: `RespawnService`/`GeoService`/`ThreadPoolManager`/`ZoneService`/`QuestEngine` (minimal), `PositionUtil` game-object overloads.

**Tier B — Creature field subsystem cones (~5.5k+ Java L *before* their own sub-cones; the real bulk):** `AbstractAI`(465)+`AIEngine`(151) · `CreatureGameStats`(396)+`CreatureLifeStats`(377)+`Stat2`(113)+stat-functions/modifiers · `EffectController`(778)+`EffectTemplate`(572)+`Effect`+effect-subclass cone · `Skill`(1023)+`SkillTemplate`(409)+skill action/condition/property cone · `CreatureMoveController`(103) · `ObserveController`(270)+observer cone · `AggroList`(216)+`AggroInfo`(64) · `TransformModel`(220). Each drags in further subclasses — Tier B is the dominant cost and should be sized again before starting.

**Tier C — F4 Player reparent (highest risk):** flat C# `Player` (~1442L, sealed, own `ObjectId`/`Name`/`Position`) → `Creature`. Java `Player` 1655L. **329 referencing files.** Reconcile inherited vs flat fields.

**WorldPosition migration (part of Tier A):** struct→class touches **64 files / ~273 `.WorldId` + ~101 `.InstanceId` + ~72 `.Heading` + `with{InstanceId=…}` sites**; behavior-affecting (value→ref, `with`→setters, **stored→derived InstanceId** via `mapRegion.getParent()`).

**Open scoping question (decide before starting):** can the new spine land *parallel* (new abstract types + Tier B containers) while flat `Player`/`WorldNpc` keep working, deferring Tier C (F4) — OR must `WorldPosition` struct→class (used by both new `VisibleObject` and legacy flat code) force F4 simultaneously? The single `WorldPosition` type is the coupling point. Resolve this first; it determines whether the big-bang is "spine+subsystems" (Tier A+B) with F4 as a follow-on, or all three tiers at once.

**Item/npc cone nodes blocked on the SCC core (port during/after big-bang):** `ResultedItem`→`ResultedItemsCollection`/`ExtractedItemsCollection`/`ReturnLocList`/`MultiReturnItem`/`DecomposableItemInfo` (need `Player`+`DataManager`); `Stigma` (`SkillTemplate`); `ItemUseLimits` (`AbyssRankEnum`→`Player`); `ItemActions` (action hierarchy→`Player`/`Item`); `ModifiersTemplate`/`StatFunction` (→`Stat2`); `ItemSetTemplate` (`ItemPart`/`PartBonus`/`FullBonus`); then `ItemTemplate` itself → unblocks `NpcEquippedGear` → `Npc`. `CustomConfig` (port like `WorldConfig`). `AbyssRankEnum` (needs `Player`+`RankingConfig`+`ChatUtil`).

## Backlog / TODOs (come back to these)

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
