# Next slop / gated targets

Branch: feature/object-spine-bigbang. Faithful 1:1, all-green-or-revert.

## RESOLVED — NpcSpawnTable dead-island retired (commit ec289dc00, 2026-06-16)

SPAWNS_DATA is now live-loaded (commit ae2e25a54), so the reworked spawn projection was orphaned and is
DELETED: `NpcSpawnTable`/`NpcRiftSpawnTable`/`NpcVortexSpawnTable` + their `*Summary` records +
`TemporarySpawnSchedule` (NpcSpawnTable.cs gone), the 4 spawn builder classes (StaticData.Builders.cs),
the StaticData streaming-spawn reader blocks (spawn_map/spawn/spot/rift_spawn/vortex_spawn/state_type/
temporary_spawn) + their ctor params / properties / build-call / locals, the now-orphaned
`ReadVortexStateTypeAttribute` helper (StaticData.Helpers.cs), and the slop `TemporarySpawnScheduleTests`.
0-consumer proof: grep PascalCase whole tree found only the island's own definition/builder/reader/test.
`VortexStateType` (Model/Vortex, faithful) and the generic Read*Attribute helpers were KEPT (shared).
Build 0, golden 167/167, bootstrap 7/7, RealStaticDataLoad green. 1140 deletions.

## PART B VERDICT — Housing SmHouse* subsystem = DEAD-ISLAND, clean-deletable NEXT TICK

Same shape as the proven NpcTemplateSummary / SkillTemplateSummary / ItemTemplateSummary / NpcSpawnTable
dead-islands: a reworked golden-blind projection running in parallel to a fully faithful pillar that is the
live path. Verified READ-ONLY (grep PascalCase whole src+tests).

### Per-element 0-consumer proof
- **Reworked SmHouse* packets** (PascalCase): `SmHouseRegistry`, `SmHouseBids`, `SmHouseEdit`,
  `SmObjectUseUpdate` — NONE are in the opcode table; ZERO external `new SmHouse*()` senders.
  `SmHouseRegistry.CreateRegisteredObjects`/`SmHouseBids.*`/`SmHouseEdit`/`SmObjectUseUpdate` are referenced
  only inside their own files (self-recursive factories). The FAITHFUL SCREAMING_CASE packets own the
  opcodes and are the live senders, 1:1 with Java:
  - `SM_HOUSE_EDIT` (opcode 82) — sent by CM_HOUSE_EDIT, CM_HOUSE_DECORATE, HouseObject, DyeAction.
  - `SM_HOUSE_REGISTRY` (opcode 116) — sent by CM_HOUSE_EDIT.
  - `SM_HOUSE_BIDS` (opcode 256) — sent by CM_GET_HOUSE_BIDS.
  - `SM_OBJECT_USE_UPDATE` (opcode 264) — sent by PostboxObject, StorageObject, UseableItemObject.
- **`HousingObjectTemplateTable` / `HousingObjectTemplateSummary`**: built in StaticData (ctor param :48,
  property :186, list :789, reader :1636-, build-call :2229) but `.HousingObjectTemplates` has ZERO readers.
  Faithful `DataManager.HOUSING_OBJECT_DATA => SD.HousingObjectDataDh` (HousingObjectData) is the live path.
- **`IHousingRepository` / `MySqlHousingRepository` / `EmptyHousingRepository`** (Data/HousingRepository.cs):
  DI-registered in Program.cs:97 (`AddSingleton<IHousingRepository, MySqlHousingRepository>`) but NO
  injection point anywhere — no ctor param, no `GetService<IHousingRepository>`, no field. Its async
  LoadWorld*/etc. methods have 0 live callers. Faithful `PlayerRegisteredItemsDAO` (Dao/) is the live
  registry persistence path (used by faithful HouseRegistry/House/HouseObjectFactory).
- **`HouseRegistryEntries`** (Model/GameObjects): read by NOTHING outside itself; its `GetSpawnedObjects`/
  `GetNotSpawnedObjects` take the reworked `PlayerHouse` record. The live faithful path is
  `HousingService.FindPlayerHouses` -> `List<House>` (faithful House/HouseRegistry), called by
  Player.Part4.cs:362.
- **`PlayerHouse`** (reworked record): referenced only by HouseRegistryEntries + itself. Faithful `House`
  is the live type.
- **Support summary types** `RegisteredHouseObjectSummary` / `HouseRegistrySummary` /
  `PlacedHouseObjectSummary`: referenced only within the island (SmHouse*/HouseRegistryEntries/PlayerHouse/
  HousingObjectTemplateTable) + ONE bleed: faithful `HousingTemplateTable.GetDecorIds(int, HouseRegistrySummary?)`
  — but `GetDecorIds` itself has 0 callers, so that method is island-coupled dead code.

### Clean-delete file/edit list (safe to execute next tick, all-green-or-revert)
- DELETE: `Network/Aion/ServerPackets/SmHouseRegistry.cs`, `SmHouseBids.cs`, `SmHouseEdit.cs`,
  `SmObjectUseUpdate.cs` (+ check `SmHousePayRent.cs`/`SmHouseObjects.cs`/`SmHouseAcquire.cs` —
  same PascalCase pattern, grep senders before deleting each).
- DELETE: `Dataholders/HousingObjectTemplateTable.cs` (+ `HousingObjectTemplateSummary` + the support
  summary records if co-located).
- DELETE: `Data/HousingRepository.cs` (IHousingRepository + Empty + MySql) + remove Program.cs:97 DI line.
- DELETE: `Model/GameObjects/HouseRegistryEntries.cs` + `Model/GameObjects/PlayerHouse.cs`.
- EDIT (relocate-or-remove, ItemStatModifier precedent): remove the `housing_objects` reader block +
  `HousingObjectTemplateTable` ctor-param/property/list/build-call from StaticData.cs/.Builders.cs/.Helpers.cs;
  remove `HousingTemplateTable.GetDecorIds(int, HouseRegistrySummary?)` (0-caller, island-coupled). KEEP
  faithful HousingTemplateTable/HousingService/HouseController/HouseRegistry/House/HouseData/
  PlayerRegisteredItemsDAO/HousingObjectData and ALL SM_HOUSE_*/SM_OBJECT_USE_UPDATE faithful packets.
- DELETE any slop-test-of-slop for these (grep tests/ for the reworked type names before executing).

VERDICT: **DEAD-ISLAND — do-next-tick clean delete, no user go-ahead required.** No live consumer; faithful
pillar already owns every opcode + the registry persistence + the template data. One care-point: scope the
StaticData/HousingTemplateTable edits to the island-coupled members only (the file itself is faithful/live).

## (historical) SPAWNS_DATA: NPC spawning was SILENTLY BROKEN (0 regular NPCs spawn at boot) — now FIXED upstream

`SpawnEngine.SpawnAll()` IS wired at boot (GameServerBootstrapService, after RiftService.InitRiftLocations,
before InitRifts) and is a faithful 1:1 port of Java spawnAll. It iterates `DataManager.WORLD_MAPS_DATA`
(loaded) -> per non-instance WorldMap -> `SpawnInstance` -> **`DataManager.SPAWNS_DATA.GetSpawnsByWorldId(mapId)`**.

`DataManager.SPAWNS_DATA` (DataManager.cs:32) is the ONLY `*_DATA` accessor that is a self-instantiated
hollow object: `public static SpawnsData SPAWNS_DATA { get; } = new();`. Every other holder delegates to
`SD.*` populated by `StaticData.LoadLeafHoldersFromFiles`. Nothing ever populates this singleton's
`Templates` / `_allSpawnMaps`. The only writer is `Event.cs:86 AddRegularSpawns` (event-driven, runtime).

Therefore `GetSpawnsByWorldId()` returns `[]` for every world -> `worldSpawns` empty -> the spawn loop body
never runs -> **zero regular NPCs and gatherables spawn at boot.** (StaticDoorSpawnManager + HousingService
still run per-instance, and rift/siege/vortex/base spawns load via their own holders, but the regular NPC
population is dead.)

### The reworked parallel that exists but is ORPHANED
`StaticData` DOES stream-parse `spawns/*.xml` at boot into reworked summary tables — `NpcSpawnTable` /
`NpcRiftSpawnTable` / `NpcVortexSpawnTable` (NpcSpawnTable.cs, `*Summary` records) built at
StaticData.cs:2475-2478. These tables are **consumed by NOTHING in src/** (only referenced inside
Dataholders/StaticData themselves). All ~20 live consumers use the faithful `DataManager.SPAWNS_DATA`
(SpawnEngine, VortexService, SiegeService, RiftService, MercenaryLocation, AgentSiege, AhserionRaid,
Base, Town(TOWN_SPAWNS), TeleportService, QuestTasks, QuestSpawnAnalyzer, KillSpawned, several quest
handlers, CM_OBJECT_SEARCH, MoveTo, ConquestOfferingPortalAI, Event). So the reworked summary tables are
dead weight; the faithful `SpawnsData` (Initialize/AddRegularSpawns/AddBase/Rift/Siege/Vortex/Mercenary/
Ahserion + all queries + spawn-search) is ALREADY fully written — it just has no loader feeding it.

### Scoped re-port (DO NOT START without user go-ahead — heavy)
Goal: at boot, deserialize `game-server/data/static_data/spawns/**/*.xml` (and the imported spawn map files)
into a real `SpawnsData` and call `Initialize()`, then have `DataManager.SPAWNS_DATA` delegate to it (drop
the hollow `= new()`).

- **Loader work (the actual gap):** SpawnsData uses `[XmlRoot("spawns")]` + `SpawnMap`/`Spawn`/`SpawnGroup`
  polymorphic model. Either (a) wire a JaxbHolderLoader/merged-holder load of the spawns dir into a
  `StaticData.Spawns` property + `DataManager.SPAWNS_DATA => SD.Spawns`, mirroring TOWN_SPAWNS_DATA, then
  call `Spawns.Initialize()` after merge; OR (b) feed the already-parsed streaming builder output
  (NpcSpawnBuilder etc.) into `SpawnsData.AddRegularSpawns/AddRift/...`. Path (a) is cleaner/faithful.
- **Model fidelity risk:** the spawn XML is large + polymorphic (rift/siege/vortex/base/mercenary/ahserion/
  temporary/pool/handler/static-door variants). Need to confirm every `[XmlElement(typeof(...))]` element-name
  binding on SpawnMap/Spawn covers the real files (same sweep discipline as SKILL_DATA). Nullable-enum and
  @XmlList proxies likely needed (handler type, difficult_id, temporary schedule, walker refs).
- **Consumers:** ~20 live (listed above) — all already on the faithful API, so NO consumer rewrite; they
  light up for free once the holder loads.
- **Orphan cleanup:** delete `NpcSpawnTable`/`NpcRiftSpawnTable`/`NpcVortexSpawnTable` + their `*Summary`
  records + the StaticData streaming-spawn builder block (StaticData.cs ~815-1605 spawn portions,
  2475-2478) once the faithful loader replaces them. (TemporarySpawnSchedule helper may be reusable.)
- **Est:** multi-batch heavy re-port (loader + element-name fidelity sweep + golden for spawn counts +
  orphan deletion). Needs a golden/integration assert on "N npc spawns loaded" per world to prove parity.
- **Why user go-ahead:** it's the last hollow holder, flagged heavy/reworked; touches the streaming
  StaticData loader spine and deletes a parallel subsystem — a coordinated big-bang, not an isolated fix.

This is the #1-value fix: until done, the server world is empty of NPCs.

## PART B — faithful-defer service wiring results

WIRED (commit 7c2935abd) — GameServerBootstrapService.StartAsync, 1:1 with GameServer.main:
- ThreadPoolManager singleton bridge bound early (Java initUtilityServicesAndConfig parity) so scheduling
  services resolve `ThreadPoolManager.GetInstance()`.
- **FlyRingService** — `GetInstance()` after SpawnAll (Java main:128). Spawns fly_rings/ templates.
- **DebugService** — `GetInstance()` after initRifts (Java main:151). Periodic world-player analysis task.
- **CuringZoneService** — GUARDED `if (!GeoDataConfig.GEO_MATERIALS_ENABLE)` (Java main:160-161). Default
  GEO_MATERIALS_ENABLE=true => not started by default, exactly like Java.
- **RoadService** — `GetInstance()` (Java main:162). Spawns roads/ templates per instance.

DEFERRED:
- **CronJobService** (Java main:158) — DEFER. Its ctor schedules Moltenus/Ahserion/LegionDominion cron jobs
  via `CronService.Schedule(..., SiegeConfig.MOLTENUS_SPAWN_SCHEDULE / AHSERION_START_SCHEDULE)`. Those
  config `CronExpression` values are NULL because the cron-config-transform surface is unported: Java
  registers `PropertyTransformers.register(new CronExpressionTransformer())` in initUtilityServicesAndConfig
  and the @Property loader converts the schedule strings into CronExpression. In C# the SiegeConfig cron
  fields aren't populated => `CronService.Schedule` NREs on `cronExpression.CronExpressionString`
  (verified: bootstrap test failed with CronServiceException "Failed to start job"). Wiring it would need
  the config-cron-transform pillar ported (SiegeConfig.MOLTENUS_SPAWN_SCHEDULE / AHSERION_START_SCHEDULE
  populated from the .properties via a CronExpression transformer). Until then, half-wiring it throws.
- **DatabaseCleaningService** (Java initUtilityServicesAndConfig:227, guarded CleaningConfig.CLEANING_ENABLE
  =false) — DEFER. (1) Its Java boot site is the pre-DataManager utility-init phase; the C# hosted bootstrap
  has no faithful pre-DataManager utility seam (DatabaseFactory.Initialize happens in Program.cs ConfigureServices,
  not a runnable init step). (2) The body requires `Thread.CurrentThread.ManagedThreadId == 1` (throws
  otherwise) which the hosted StartAsync thread cannot satisfy. (3) Default CLEANING_ENABLE=false so a
  guarded-off call restores no observable subsystem. A faithful wire needs a dedicated thread-1 pre-boot
  utility step; out of scope for a bounded wire.
- **CurrentThreadRunnableRunner** — NOT a boot-started service in Java. It is a `RunnableRunner` strategy
  (services/cron/), used as the synchronous-execution variant passed to/used by CronService; GameServer.main
  never calls a getInstance()/start on it. Faithful 1:1 => there is no call site to wire. NO ACTION (the
  class already exists for when CronService selects synchronous execution). Not a defect.

## Gated list (need user decision)
1. **SPAWNS_DATA re-port** (above) — silently-broken, #1 value. Heavy big-bang.
2. **CronJobService cron-config-transform** — port SiegeConfig cron-schedule property transform so
   CronJobService can be wired faithfully.
3. **DatabaseCleaningService thread-1 utility-init seam** — only if a faithful pre-boot utility phase is added.
4. **Housing SmHouse* subsystem** — RESOLVED to DEAD-ISLAND (see PART B VERDICT above). Clean-delete
   next tick, NO user go-ahead needed; all-green-or-revert.
