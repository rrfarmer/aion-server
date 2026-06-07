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

## Last unit (session 2026-06-07 batch)

Bottom-up scan of all zero-dep types needed on path to `SpawnGroup`/`VisibleObject`. All green (build + guardrail 363/6):

| Commit | File(s) |
|--------|---------|
| `31242003e` | `model/animations` — 6 animation enums |
| `608f4c2c3` | `model/templates/BoundRadius + L10n (IL10n) + VisibleObjectTemplate` |
| `c01d57edb` | `world/WorldType + WorldDropType + spawnengine/SpawnHandlerType` |
| `7c61a213d` | `ai/event/AIEventType + model/templates/zone/ZoneClassName` |
| `85f92e112` | `world/zone/ZoneName + ZoneAttributes` |
| `cdbfa72a2` | `model/Race + model/siege/SiegeModType + model/vortex/VortexStateType` |
| `14b529960` | `model/TribeClass` (748-line zero-dep enum; SCREAMING_SNAKE_CASE preserved for XML+isGuard) |
| `0388d9bf6` | `services/panesterra/ahserion/PanesterraFaction` |
| `d3f267c13` | `model/base/BaseOccupier` |
| `9db32c4bb` | `model/siege/SiegeRace` | `BoundRadius` is zero project-deps (JAXB XML annotations → `System.Xml.Serialization`); `L10n` depends only on `ChatUtil.L10n(int)` (exists); `VisibleObjectTemplate` depends on both. Build green, guardrail green (363/6). Commit: `[Fidelity] model/templates/BoundRadius+L10n+VisibleObjectTemplate — port 3 template foundation classes`.

Previous: Ported **`model/animations` package** (6 enums). Commit `31242003e`.

## How to choose the next unit (no-defer = strictly bottom-up)

**Never start a node that has an unmet dependency.** Build the dependency-free base and expand upward; you only reach a higher node once everything it needs already exists. The object-model spine (`AionObject→VisibleObject→Creature→Npc/Player` + `World`/`KnownList`/controllers) defines *which* foundation pieces are in scope (don't port unrelated dependency-free files); the no-defer rule defines the *order* (bottom-up). Note the trunk also needs *sideways* foundation (`KnownList`, controllers, templates, `MapRegion`) before each step up.

Algorithm each turn: from the in-scope spine, pick a unit whose dependencies **all already exist in C#**. If none do, pick the deepest still-missing dependency (it is itself such a unit). Read the Java fully, port 1:1, build, commit (code+HANDOFF). Never stub/defer.

## Next unit

`model/animations` (all 6 enums) — DONE. The next in-scope units, in dependency order toward `VisibleObject` (which needs `VisibleObjectController`, `VisibleObjectTemplate`, `SpawnTemplate`, `world/*` incl. missing `MapRegion`/`WorldMap`, `world/knownlist/KnownList`):

Dependency tree progress for `VisibleObject` (F2):
- `VisibleObject` directly needs: `AionObject` ✅, `ObjectDeleteAnimation` ✅, `VisibleObjectTemplate` ✅, `SpawnTemplate`, `WorldPosition`, `World`/`WorldMap`/`WorldMapInstance`, `KnownList`, `VisibleObjectController`.
- `SpawnTemplate` → `SpawnGroup` → needs: `BaseOccupier` ✅, `SiegeModType` ✅, `SiegeRace` ✅, `VortexStateType` ✅, `PanesterraFaction` ✅, **`EventTemplate`** (needs SpawnsData/GlobalRule/adapters), **`TemporarySpawn`** (needs GameTimeService/ServerTime/GameTime), and sub-templates.
- **Next**: read `GameTimeService`, `ServerTime`, `GameTime` for zero-dep status; also check `GlobalRule`, `SpawnsData` deps.

Fidelity Gate only (foundation/additive). Validation: `dotnet build src/Aion.GameServer` + targeted test; Java/Maven only if a golden/parity check applies.

- **Before F4** (reparent flat `Player` → `Creature`, 329 files), surface the strategy decision (big-bang vs gradual). F2/F3 do not depend on it.

For the chosen unit fill in: Fidelity Gate answers; "remediation/foundation — Fidelity only"; exact validation command (`dotnet build src/Aion.GameServer` + targeted test) + Java/Maven need.

## Blockers / risks

- **The biggest slop clusters (BindPointTeleport, and likely FindGroup, WorldNpc, PlayerKnown, summons, vortex) are substrate-blocked** — they fake unported runtime (teleport, controller tasks, KnownList, scheduler). Don't attempt them top-down by file count; port substrate first (Track 1) or pick Track 2 units.
- Golden harness covers deterministic, constructor-driven packets and pure formulas only; singleton/time-dependent packets need a deterministic config harness first.
- `check_fidelity.py` matches by simple class name (naming-normalized). A faithful C# port named differently from its Java class could trip it — fix by matching the Java name, not by editing the baseline.
