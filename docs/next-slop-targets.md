# Next slop / gated targets

Branch: feature/object-spine-bigbang. Faithful 1:1, all-green-or-revert.

## RESOLVED — Location-init cluster wired (commit pending, 2026-06-16)

The boot location-init cluster (GameServer.main lines 111-117 + TownService :127) is now WIRED in
GameServerBootstrapService.StartAsync, in exact Java order. Each is dep-clean: ctors iterate the
faithfully-loaded *_DATA holders (live via StaticData.TryLoadHolder) and any DAO read is try/catch-guarded
(no DB => empty + logged, never NRE). Bounded null-deps fixed with Java @Property defaults (no invented values).

WIRED (all build 0, per-class golden 167/167 + 5/5 LS, bootstrap 7/7, RealStaticDataLoad green):
- **BaseService.getInstance()** (main:111) — ctor builds BaseLocation per BASE_DATA template. Restores base
  capture/spawn location registry.
- **SiegeService.getInstance()** (main:112) — loads SIEGE_LOCATION_DATA fortress/artifact/outpost (guard
  SiegeConfig.SIEGE_ENABLED=true) + SiegeDAO.LoadSiegeLocations (try/catch). Restores fortress-siege location data.
- **WorldRaidService.getInstance().initWorldRaidLocations()** (main:113) — loads WORLD_RAID_DATA (guard
  EventsConfig.ENABLE_WORLDRAID). Restores world-raid locations.
- **VortexService.getInstance().initVortexLocations()** (main:115) — spawns peace-state vortex NPCs +
  schedules Theobomos/Brusthonin invasions (guard CustomConfig.VORTEX_ENABLED=true). Restores dimensional-vortex
  invasion lifecycle.
- **LegionDominionService.getInstance().initLocations()** (main:117) — builds LegionDominionLocation per
  LEGION_DOMINION_DATA + LegionDominionDAO (try/catch). Restores legion-territory-control locations.
- **TownService.getInstance()** (main:127, after SpawnEngine.spawnAll) — loads per-race towns via TownDAO
  (try/catch) + seeds from HOUSE_DATA when empty. Restores town registry.

NULL-DEP FIXES (faithful @Property defaults, same shape as the CronJobService/SiegeConfig fix):
- **CustomConfig.VORTEX_THEOBOMOS_SCHEDULE / VORTEX_BRUSTHONIN_SCHEDULE** were null Quartz.CronExpression
  fields (no initializer) — VortexService.initVortexLocations NRE'd on CronService.Schedule(..., null) (the
  CronJobService failure mode). Initialized from the Java @Property defaultValue cron strings (identical to
  config/main/custom.properties): theobomos `"0 0 16 ? * SUN"`, brusthonin `"0 0 16 ? * SAT"` via
  CronExpressions.GetOrCreate. No invented value.
- **BaseData.baseTemplates** + **LegionDominionData.ldl** ([XmlElement] List fields) were null when the XML
  is absent (e.g. minimal bootstrap-test fixture) — GetAllBaseTemplates()/GetLocationTemplates() returned
  null => foreach NRE. Initialized `= new()` (XmlSerializer add-to-existing / JAXB-faithful: stays empty when
  the element is absent, populated when present). Matches the established pattern on HouseData.lands /
  SiegeLocationData/VortexData/WorldRaidData [XmlIgnore] derived maps.

ORDERING FIX: **CronService.InitSingleton** moved from its late position (after SpawnAll) up to right after
GameTimeService.InitAsync (before the location-init cluster) — faithful to Java, where CronService.initSingleton
runs in initUtilityServicesAndConfig BEFORE the main-body services that schedule through it. VortexService
(main:115) / WorldRaidService.initWorldRaids / RiftService.initRifts all need a live CronService.getInstance().

PRE-EXISTING (NOT introduced here): GoldenStatsInfoFixtureTests fails ONLY when run in the same process as
the bootstrap test (combined --filter), due to shared DataManager static-singleton pollution + run order.
Confirmed identical failure on clean HEAD (1 failed / 174 passed for the combined filter, pre-change). Passes
in isolation. Per-class verification (the task contract) is all-green. This is a test-harness isolation issue,
out of scope for this wire.

## RESOLVED — CronJobService wired + SiegeConfig cron schedules populated (commit pending, 2026-06-16)

The CronJobService deferral (cron-config-transform unported) is FIXED and the service is now wired at boot.

Root cause was bounded, not a subsystem: C# `Config.Load()` is a deferred no-op, so config-holder classes
carry their `@Property` defaults as field initializers. Every SiegeConfig bool/int/float field already had its
default; only the two `Quartz.CronExpression` fields (`MOLTENUS_SPAWN_SCHEDULE`, `AHSERION_START_SCHEDULE`)
were left uninitialized (null) — there was no field initializer for them. When CronJobService's ctor passed
the null CronExpression to `CronService.Schedule`, it NRE'd on `cronExpression.CronExpressionString`
(prior bootstrap test failed with CronServiceException "Failed to start job").

FIX (faithful 1:1): the Java `@Property` defaultValue strings (and config/main/siege.properties — identical)
are `"0 0 22 ? * SUN"` (moltenus) and `"0 50 18 ? * SUN"` (ahserion). Java's `CronExpressionTransformer`
turns these into a CronExpression via `CronExpressions.getOrCreate(value)`. So the two C# SiegeConfig fields
are now initialized with `Aion.GameServer.Services.Cron.CronExpressions.GetOrCreate("0 0 22 ? * SUN")` /
`("0 50 18 ? * SUN")` — exactly what Config.Load + CronExpressionTransformer would produce given no override.
No invented values; defaultValue == properties-file value.

WIRED: `CronJobService.GetInstance()` in GameServerBootstrapService.StartAsync at the Java-correct boot site
(GameServer.main:158 — after AtreianPassportService/DebugService, before CuringZoneService/RoadService, and
after CronService.InitSingleton). Its ctor schedules the Moltenus spawn + Ahserion flight cron jobs, runs the
IdianDepthPortal spawner synchronously, and schedules the weekly LegionDominion calculation ("0 0 9 ? * WED *").

Restored at boot: Moltenus (Berserker Sunayaka) Sunday-22:00 spawn cron, Ahserion Panesterra raid Sunday-18:50
cron, Idian Depth portal spawns (Levinshor/Kaldor/Cygnea/Enshar entrances), weekly Legion Dominion calc.
Verify: build 0, golden 167/167, bootstrap 7/7 (now exercises CronJobService through StartAsync — no throw),
RealStaticDataLoad green.

## BOOT-COMPLETENESS CENSUS (Java GameServer.main vs C# GameServerBootstrapService.StartAsync, 2026-06-16)

initUtilityServicesAndConfig (Java pre-DataManager utility phase):
- UncaughtExceptionHandler set — N/A (infra; .NET host handles unobserved-exception policy).
- PropertyTransformers.register(CronExpressionTransformer) — N/A as a runtime step (Config.Load is a deferred
  no-op); the transform's EFFECT is now reproduced by SiegeConfig field initializers (see RESOLVED above).
- Config.load() — DEFERRED no-op (config-holders carry @Property defaults as field initializers). Inert for
  boot today; live consumers read the hardcoded defaults. (Infra per gameplay-faithful/infra-idiomatic.)
- DatabaseFactory.init() — DONE (Program.cs ConfigureServices / DI).
- PlayerDAO.setAllPlayersOffline() — GAP (inert at boot; matters only with a populated players table — sets
  the online flag false for all rows. No live in-process consumer at boot; cosmetic until real logins persist).
- DatabaseCleaningService.deletePlayersOnInactiveAccounts() (guarded CLEANING_ENABLE=false) — GAP/DEFER
  (default-off; needs a thread-1 pre-boot utility seam — see DEFERRED below). Inert by default.
- ThreadPoolManager.getInstance() — DONE (RegisterInstance bridge early in StartAsync).
- CronService.initSingleton(...) — DONE (guarded once-only init in StartAsync).

main (post-utility):
- JAXBUtil.preLoadContextAsync — N/A (JAXB warmup; C# uses XmlSerializer + cache, no equivalent prewarm needed).
- IDFactory.getInstance() — DONE.
- DataManager.getInstance() — DONE (StaticDataLoader, 13 holders live).
- QuestEngine/AIEngine/InstanceEngine/ChatProcessor/ZoneService/GeoService init (parallel) — DONE (engine list).
- World.getInstance() — DONE (LoadWorldMaps + RegisterInstance).
- GameTimeService.getInstance() — DONE.
- DropRegistrationService.getInstance() — GAP (drop-table registration; live consumers = loot on NPC death.
  Bounded singleton-touch; likely next bounded wire — needs DropRegistrationService ported/verified first).
- BaseService.getInstance() — DONE (base location registry; wired 2026-06-16 location-init cluster).
- SiegeService.getInstance() — DONE (siege location data; wired 2026-06-16).
- WorldRaidService.initWorldRaidLocations() — DONE (world-raid locations; wired 2026-06-16).
- VortexService.initVortexLocations() — DONE (vortex locations + invasion cron; wired 2026-06-16, null-cron fixed).
- RiftService.initRiftLocations() — DONE.
- LegionDominionService.initLocations() — DONE (legion-territory locations; wired 2026-06-16).
- HousingService.getInstance() — GAP? (faithful HousingService exists + runs per-instance on spawn; explicit
  boot getInstance() touch not in StartAsync — verify it self-inits via spawn path; likely effectively DONE).
- HousingBidService / AuctionEndTask / AuctionAutoFillTask / MaintenanceTask — GAP (housing auction tasks).
- ChallengeTaskService.getInstance() — GAP.
- SpawnEngine.spawnAll() — DONE.
- TownService.getInstance() — DONE (town registry; wired 2026-06-16). NOTE: town NPC SPAWNING still depends on
  the gated SPAWNS_DATA/TOWN_SPAWNS path (#1); this wire restores the town-level/points registry only.
- FlyRingService.getInstance() — DONE.
- RiftService.initRifts() — DONE.
- ratio-limitation block (GSConfig.ENABLE_RATIO_LIMITATION) — N/A by default (config-gated off).
- LimitedItemTradeService.start() — GAP.
- PlayerLimitService.scheduleUpdate() (CustomConfig.LIMITS_ENABLED) — GAP (config-gated).
- SiegeService.initSieges() — GAP (second-pass: despawns spawn-engine NPCs + spawns siege NPCs + schedules
  fortress sieges through CronService. getInstance() prereq now DONE; this is the next bounded second-pass wire,
  gated on confirming the SpawnNpcs/DeSpawnNpcs path + SiegeSchedules.Load don't NRE on unported deps).
- BaseService.initBases() — GAP (second-pass: starts casual/stained/panesterra bases. getInstance() prereq DONE).
- WorldRaidService.initWorldRaids() — GAP (second-pass: schedules raids via CronService. getInstance() prereq DONE).
- ConquerorAndProtectorService.init() — GAP.
- AnnouncementService.getInstance() — GAP.
- DebugService.getInstance() — DONE.
- WeatherService.getInstance() — GAP (weather scheduling). Live consumers: zone weather.
- BrokerService.getInstance() — GAP (auction broker). Live consumers: broker UI/persistence.
- Influence.getInstance() — GAP (abyss influence ratio).
- ExchangeService.getInstance() — GAP (player trade).
- PeriodicSaveService.getInstance() — GAP (periodic player/legion save scheduling). Notable: real persistence.
- AtreianPassportService.getInstance() — GAP.
- CronJobService.getInstance() — DONE (this tick).
- CuringZoneService.getInstance() (guarded !GEO_MATERIALS_ENABLE; default off) — DONE (guarded, matches Java).
- RoadService.getInstance() — DONE.
- HTMLCache.getInstance() — GAP (HTML dialog cache). Live consumers: NPC dialog HTML.
- AbyssRankingCache / AbyssRankUpdateService.scheduleUpdate() — GAP.
- PeriodicInstanceManager.getInstance() — GAP.
- EventService.start() — GAP (event spawns/schedules).
- AdminService.getInstance() — GAP.
- CommandsAccessService.loadAccesses() — GAP (admin command ACLs). Live consumers: chat command auth.
- PlayerTransferService.getInstance() — GAP.
- GameTimeService.startClock() — DONE.
- PvpMapService.init() — GAP.
- CustomInstanceService.getInstance() — GAP.
- DataManager.waitForValidationToFinishAndShutdownOnFail() — DONE (ValidationTask await).
- System.gc() — N/A.
- VersionInfo/SystemInfo logAll — N/A (logging).
- PetFeedUnusualStorageArtifactCapture.installIfEnabled() — N/A by default (parity-capture seam, off).
- initNioServer() — DONE-elsewhere (network host startup is the LS/GS/CS stack, not StartAsync).
- ShutdownHook register — partial (StopAsync mirrors orderly shutdown).
- LoginServer.connect / ChatServer.connect — DONE-elsewhere (3-server stack).

GAPs ordered by gameplay impact (those with live consumers = real silent-skip):
1. **SPAWNS_DATA regular-NPC spawns** — already documented/gated below (#1, heavy).
2. **Location-init cluster** (SiegeService/BaseService/VortexService/WorldRaidService/LegionDominion
   getInstance()/initLocations) — DONE (wired 2026-06-16). Remaining: the **second-pass init*()** calls
   (SiegeService.initSieges / BaseService.initBases / WorldRaidService.initWorldRaids /
   ConquerorAndProtectorService.init) which spawn NPCs + schedule sieges/raids — the next bounded batch,
   gated on confirming the spawn/schedule paths are dep-clean.
3. **TownService** — DONE (registry wired 2026-06-16). Town NPC spawning still gated on SPAWNS_DATA (#1).
4. **PeriodicSaveService** — periodic persistence not scheduled (matters once real logins persist).
5. **CommandsAccessService.loadAccesses** — admin/chat command authorization unloaded.
6. **HTMLCache** — NPC dialog HTML uncached.
7. **EventService.start / WeatherService / BrokerService / ExchangeService / AbyssRanking / etc.** — feature
   services, each a bounded getInstance() wire pending port verification.
8. **PlayerDAO.setAllPlayersOffline / DatabaseCleaning** — DB-state, inert until players persist.

Most remaining GAPs are individually bounded getInstance()/init() wires (the same shape as this tick), each
gated only on confirming the target service is faithfully ported and its ctor doesn't NRE on an unported dep
(the CronJobService failure mode). The recommended next bounded tick: audit the location-init cluster (#2)
service-by-service and wire the ones whose ctors are dep-clean.

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

## RESOLVED — Housing SmHouse* dead-island retired (commit pending, 2026-06-16)

The housing registry-summary dead-island is DELETED. 0-consumer re-confirmed (PascalCase grep whole
src+tests): every reference to the reworked types lived inside the island's own files; the faithful
SCREAMING_CASE pillar (SM_HOUSE_EDIT/REGISTRY/BIDS + SM_OBJECT_USE_UPDATE, House/HouseObject/
HousingService/HousingBidService/HOUSING_OBJECT_DATA/PlayerRegisteredItemsDAO) owns every opcode +
registry persistence + template data and is untouched/live.

DELETED (16 files + edits): ServerPackets SmHouseRegistry/SmHouseBids/SmHouseEdit/SmHouseObjects/
SmHouseObject/SmHouseAcquire/SmHousePayRent/SmObjectUseUpdate + HouseObjectPacketWriter;
Model/GameObjects HouseRegistryEntries (all *Summary records: RegisteredHouseObjectSummary/
RegisteredHouseDecorationSummary/HouseRegistrySummary/PlacedHouseObjectSummary) + PlayerHouse +
HouseAuctionBid (HouseAuctionBidPage/Summary/Context — island-only, consumed solely by SmHouseBids);
Dataholders/HousingObjectTemplateTable (+ HousingObjectTemplateSummary); Data/HousingRepository
(IHousingRepository/Empty/MySql). EDITS: Program.cs DI line removed; StaticData ctor-param/assignment/
property/list-decl/two housing_objects reader blocks/build-call removed; StaticData.Builders
IsHousingObjectTemplateElement + GetHousingObjectTypeId helpers removed (island-only);
HousingTemplateTable.GetDecorIds(int, HouseRegistrySummary?) overload removed (0-caller, island-coupled).
KEPT: the rest of faithful HousingTemplateTable (incl. GetPart/TryGetDecorPacketIndex public surface),
faithful PlayerHouse readers live via HousingService.FindPlayerHouses -> List<House> (faithful House is
the live type; reworked PlayerHouse record was island-only and deleted). InventoryItem DTO untouched.
Verify: build 0, golden 167/167, bootstrap 7/7, RealStaticDataLoad green.

ALL dead-island slop is now retired (NpcTemplateSummary/SkillTemplateSummary/ItemTemplateSummary/
NpcSpawnTable/Housing-SmHouse*). The big slop-retirement/correctness arc is COMPLETE. Remaining work
is NOT slop — it is the 2 peripheral service deferrals below (CronJobService cron-config-transform,
DatabaseCleaningService thread-1 seam) + the gated SPAWNS_DATA re-port, all requiring a user decision.

## (historical) PART B VERDICT — Housing SmHouse* subsystem = DEAD-ISLAND, clean-deletable NEXT TICK

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
- **CronJobService** (Java main:158) — RESOLVED 2026-06-16 (see RESOLVED section at top). The cron-config
  values are now populated via SiegeConfig field initializers (CronExpressions.GetOrCreate of the Java
  @Property defaultValue strings) and the service is wired at the Java-correct boot site. No longer deferred.
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
2. **CronJobService cron-config-transform** — RESOLVED 2026-06-16 (cron schedules populated via SiegeConfig
   field initializers, service wired at boot). See RESOLVED section at top. No longer gated.
3. **DatabaseCleaningService thread-1 utility-init seam** — only if a faithful pre-boot utility phase is added.
4. **Housing SmHouse* subsystem** — RESOLVED + DELETED (see RESOLVED section at top, 2026-06-16).
   Dead-island retired; faithful pillar is the sole live path.
5. **Boot location-init cluster** (SiegeService/BaseService/VortexService/WorldRaidService/LegionDominion +
   TownService getInstance/initLocations) — RESOLVED 2026-06-16 (see RESOLVED section at top; all wired
   dep-clean). Remaining bounded getInstance() wires: the second-pass init*() (SiegeService.initSieges /
   BaseService.initBases / WorldRaidService.initWorldRaids / ConquerorAndProtectorService.init) +
   PeriodicSaveService/CommandsAccessService/HTMLCache/EventService/WeatherService/BrokerService/etc., each
   gated on per-service ctor dep-clean verification. NOT slop. See BOOT-COMPLETENESS CENSUS above.
