# Next slop / gated targets

Branch: feature/object-spine-bigbang. Faithful 1:1, all-green-or-revert.

## RESOLVED — 2 of the 6 boot-init deferrals unblocked via fixture enrichment (commits 2c05275b8 / 713fef10a, 2026-06-16)

Enriched the GameServerBootstrapTests StaticDataFixture to seed the SPECIFIC REAL static_data files each
deferred service needs at init (copied verbatim from game-server/data/static_data via a FindRepoRoot walk;
never invented; skipped when the repo data tree is absent). The fixture's LoadLeafHoldersFromFiles reads each
holder from a fixed sub-path of the temp static_data dir, so dropping the real file there populates exactly that
holder and leaves every other holder empty (minimal). Then flipped each gated wire on at its Java-correct boot
site and re-gated on the 7/7 bootstrap test (all-green-or-revert). Per-class verify each commit: build 0,
bootstrap 7/7, golden 167/167, RealStaticDataLoad green.

NOW WIRED:
- **#3 PeriodicInstanceManager.getInstance() (main:166)** — UNBLOCKED. Fixture seeds the real
  auto_group/auto_group.xml (every static-init AutoGroupType maskId 1,2,3,21-45,101-103,107,108,109,111 is
  present), so the ctor's GetAGTByMaskId -> GetTemplate resolves non-null templates and schedules the
  dredgion/kamar/ophidan/iron-wall/idgel registration crons. Commit 2c05275b8.
- **#4 HTMLCache.getInstance() (main:163)** — UNBLOCKED. Fixture copies the real static_data/HTML tree to a
  temp dir and points HTMLConfig.HTML_ROOT/HTML_CACHE_FILE at temp paths, so the ctor's Reload(false) ->
  ParseDir caches the real .xhtml files instead of throwing DirectoryNotFoundException. Commit 713fef10a.

STILL DEFERRED after attempt (precise blockers, each faithful 1:1 — NOT a port defect):
- **#1 SiegeService.initSieges() (main:142)** — ATTEMPTED + REVERTED. Seeding siege/siege_locations.xml fixes
  the original UpdateFortressNextState null-GetSiegeLocation NRE, but initSieges goes DEEPER: it StartSiege()s
  every standalone artifact, and ArtifactSiege.OnSiegeStart -> Siege.InitSiegeBoss throws
  `SiegeException: Siege Boss not found for siege 1012` because the artifact boss NPC is not spawned (empty
  SPAWNS_DATA => GetSiegeSpawnsByLocId null => SpawnNpcs no-op; no siege spawns, no artifact world map).
  Faithful (Java throws SiegeException there too without the boss spawn). BLOCKER = HEAVY-DATA/SPAWN: needs the
  full SPAWNS_DATA siege-spawn dir + the siege/artifact world maps loaded into World, not a leaf-holder seed —
  i.e. a real spawn/world boot, which the minimal bootstrap fixture deliberately does not load. Defer to a
  spawn-data-backed harness.
- **#5 PvpMapService.getInstance().init() (main:176)** — NOT ATTEMPTED-to-green (analysis defer). Init ->
  InstanceService.GetNextAvailableInstance(301220000) needs world map 301220000 (=> the full real world_maps.xml
  loaded into World.LoadWorldMaps) AND PvpMapHandler.OnInstanceCreate actively SPAWNS keymasters/treasure
  chests/NPCs (Spawn/BringIntoWorld) — needing NPC_DATA + spawn infra, AND it materializes world objects which
  would break the bootstrap's empty-world invariant (Assert.Equal(0, world.ObjectCount) after stop). BLOCKER =
  HEAVY-DATA/SPAWN + invariant-conflict: same floor as #1. Defer to a spawn-data-backed harness.
- **#2 Housing (main:119-123)** — CONFIRMED DB-HARNESS-GATED (checked Java). Java PlayerDAO.getUsedIDs()
  returns NULL on SQLException (no DB), and revokeOwnershipOfDeletedPlayers does IntStream.of(null) -> Java
  NPEs identically. Java is DB-REQUIRED here; it does NOT guard null. Per the hard rule (DB-required init that
  Java can't run without a DB -> defer to a DB harness, don't fake), NOT wired and NO un-faithful guard added.
  Defer to a DB harness.
- **#6 PeriodicSaveService (main:156)** — RESOLVED (commit 62c408390, 2026-06-16). Re-ported the faithful Java
  singleton 1:1: PeriodicSaveService.GetInstance() + SingletonHolder + the inner PeriodicSaveTask base and the
  TWO tasks — LegionWarehouseSaveTask (period PeriodicSaveConfig.LEGION_ITEMS * 1000 ms = 1200*1000;
  LegionService.GetInstance().GetCachedLegions() -> per-legion GetLegionWarehouse().GetItemsWithKinah() +
  AddRange(GetDeletedItems()) -> InventoryDAO.Store(items, null, null, legionId) + ItemStoneListDAO.Save(items),
  try/catch-logged) and ServerRunTimeSaveTask (period 2 min; ServerVariablesDAO.Store("serverLastRun", nowMillis)).
  Scheduling uses ThreadPoolManager.GetInstance().ScheduleAtFixedRateTask (the faithful Future analogue,
  returns ScheduledTask with Cancel(bool)); OnShutdown() stores+cancels each task. The intervals are the
  PeriodicSaveConfig @Property defaults (LEGION_ITEMS=1200) + the literal TimeUnit.MINUTES.toMillis(2) — never
  invented. The reworked DI GameEngine + its Program.cs:74/77 registration were RETIRED, and the slop-shape test
  (PeriodicSaveService_StoresServerLastRunPeriodicallyAndOnShutdown, which exercised the IServerVariablesRepository
  DI abstraction) was REPLACED with a faithful singleton test (PeriodicSaveService_GetInstanceSchedulesSaveTasks:
  GetInstance() non-null + same-instance, OnShutdown no-throw with empty legion cache). Wired
  PeriodicSaveService.GetInstance() at GameServer.main:156 in GameServerBootstrapService. Task bodies are
  boot-safe: GetCachedLegions() is an in-memory empty map (no DB) and ServerVariablesDAO.Store is fully
  try/catch-guarded (no-DB => logged false). Build 0, golden 167/167, bootstrap 7/7, RealStaticDataLoad green.

SUMMARY: 3/6 deferrals now unblocked (AUTO_GROUP + HTML leaf reads, and #6 PeriodicSaveService re-port). The
remaining 3 sit at a real floor — #1/#5 need the heavy SPAWNS_DATA + world-map boot (and #5 also conflicts with
the empty-world invariant), #2 is DB-required (no Java guard to faithfully port).
RECOMMENDED NEXT: (a) a spawn-data-backed bootstrap harness (load the real spawns/ + world_maps.xml into a
test World) would unblock BOTH #1 and #5 at once — scoped below; (b) a DB-backed test harness unblocks #2.

## SCOPE — spawn-data-backed bootstrap harness for #1 SiegeService.initSieges + #5 PvpMapService (read-only assessment, 2026-06-16)

GOAL: evolve GameServerBootstrapTests so the boot SpawnEngine.SpawnAll() actually populates the test World
(siege/artifact bosses for #1, pvp keymasters/chests for #5), then flip #1/#5 on and change the empty-world
assert (`Assert.Equal(0, world.ObjectCount)`) to an expected populated count.

WHAT THE SUBSTRATE ALREADY GIVES US (no new loader work):
- SPAWNS_DATA and WORLD_MAPS_DATA already load 1:1 from the real XML — proven green by
  RealStaticDataLoadIntegrationTests (SpawnsDh.GetSpawnsByWorldId(110010000) non-empty incl. siege spawn maps;
  WorldMaps2 from world_maps.xml). StaticData.LoadLeafHoldersFromFiles reads `spawns/` (TryLoadMergedHolder,
  singleRootTag) + `world_maps.xml` from fixed sub-paths of the static_data dir.
- SpawnEngine.SpawnAll() is already wired at boot (GameServerBootstrapService:165) and is faithful — it iterates
  WORLD_MAPS_DATA -> per non-instance map SpawnInstance -> SPAWNS_DATA.GetSpawnsByWorldId. It is dormant in the
  bootstrap test ONLY because the minimal StaticDataFixture seeds neither holder.
- The fixture already has the exact mechanism: CopyRealFile(realStaticData, fixtureDir, relativePath) +
  FindRepoRoot walk (used today for auto_group/auto_group.xml + the HTML tree). Same move seeds spawns + world maps.

STEPS:
1. Fixture seed (StaticDataFixture.Create): CopyRealFile the real `world_maps.xml`, and copy the `spawns/` dir
   (every spawn_map file — it's a multi-file merged holder, so the whole dir, like the HTML tree copy). NPC_DATA
   is also needed for SpawnEngine to resolve npc templates when bringing spawns into the world — seed npc_skills/
   the npc data files too (verify which holder SpawnInstance actually dereferences; SpawnAll itself only needs
   SPAWNS_DATA+WORLD_MAPS, but BringIntoWorld/VisibleObject may touch NPC_DATA). Skip-guard when repo data absent
   (same `if (repoRoot != null)` pattern already there) so the test still runs in a data-less checkout.
2. #1 SiegeService.initSieges(): with the siege spawn maps + siege/artifact world maps now in World, the
   ArtifactSiege.OnSiegeStart -> Siege.InitSiegeBoss path finds its boss (no more `SiegeException: Siege Boss not
   found for siege 1012`). Also seed siege_locations.xml (the original UpdateFortressNextState null-GetSiegeLocation
   guard) — already identified. Then flip the initSieges() wire on at main:142.
3. #5 PvpMapService.init(): InstanceService.GetNextAvailableInstance(301220000) needs world map 301220000 (now
   loaded), and PvpMapHandler.OnInstanceCreate spawns keymasters/chests into that instance. Flip init() on at main:176.
4. Assert evolution: SpawnAll + siege + pvp now populate World, so `Assert.Equal(0, world.ObjectCount)` after
   StopAsync must become a populated expectation. Two options: (a) assert a STABLE lower-bound
   (`Assert.True(world.ObjectCount > 0)` after StartAsync, before StopAsync) since the exact count is data-version
   sensitive; (b) pin an exact count from a single known seeded world (e.g. assert N npc objects on map 110010000)
   the way RealStaticDataLoad pins specific npc ids — more brittle but exact. Recommend (a) for the boot test +
   keep the exact-id pins in RealStaticDataLoad. The post-StopAsync assert should verify the world is TORN DOWN
   (objects despawned) rather than "never populated" — change it from `== 0 always` to `populated during run,
   cleared on stop`.

EFFORT: MEDIUM. No new deserialization/loader code (the holders + SpawnEngine are done). The work is fixture
data-seeding (copy real spawns/ + world_maps.xml + NPC_DATA into the temp dir), flipping 2 wires, and reworking
the world-count assertion. The siege-boss spawn dependency chain (artifact world map + siege spawn map both
present) is the one thing to verify end-to-end — if a specific artifact's boss spawn lives in a spawn map the
SpawnEngine doesn't reach (instance-only map, or a handler-gated spawn), initSieges may still throw and that
artifact's siege must be confirmed against Java (Java also requires the spawn).

RISK:
- MEDIUM data-coupling: copying the full spawns/ dir + world_maps.xml makes the bootstrap test load a large
  data set (slower; closer to a real boot). Mitigate by only seeding the maps the asserts touch if SpawnEngine
  tolerates a partial WORLD_MAPS_DATA (it iterates whatever's present — a subset is fine and keeps the test fast).
- LOW-MED test-pollution: RealStaticDataLoad already documented a DataManager static-singleton cross-test
  pollution when run in the same process as the bootstrap test. Populating the bootstrap World from the same real
  holders increases shared-static surface; keep per-class verification (the task contract) and watch the combined
  filter.
- LOW invariant churn: every other assert in GameServerBootstrap_LoadsDataInitializesWorldAndStartsGameTime keys
  off the minimal fixture (e.g. GetElementCount("item")==1). Seeding more holders may change those counts — audit
  each `Assert.Equal(1, ...)` against the enriched fixture or scope the seeding to a SEPARATE new test method
  (recommended: add `GameServerBootstrap_SpawnsSiegeAndPvpWorld` rather than mutating the existing minimal test,
  preserving the minimal-fixture invariants for the other asserts).

RECOMMENDED: add a NEW bootstrap test (spawn-data-backed) seeded with spawns/ + world_maps.xml + NPC_DATA that
asserts world populates + siege/pvp wire cleanly, leaving the existing minimal test (and its empty-world
invariant) intact. This unblocks #1 + #5 together and is the highest-value remaining boot-init move.

## RESOLVED — Second-pass + trailing main services wired (commits 5fdff6c10 / 5a2fe74c4 / 334a07ef4 / 3fc0b0857 / f44838a62, 2026-06-16)

Drained the bounded boot-init long tail across GameServer.main. All in exact Java order, each gated on the
GameServerBootstrapTests (7/7) safety net (wire -> ~Bootstrap -> revert+defer if it NREs). Per-class verify
all-green every commit: build 0, bootstrap 7/7, golden 167/167, RealStaticDataLoad green.

NOW WIRED (in main order):
- DropRegistrationService.getInstance() (main:108) — empty ctor; drop-table singleton.
- HousingService block (main:119-123) — DEFERRED (no-DB edge, see below).
- ChallengeTaskService.getInstance() (main:124) — empty task maps.
- LimitedItemTradeService.getInstance().start() (main:136) — limited-trade NPC collection + reset crons (empty data => no-op).
- PlayerLimitService.getInstance().scheduleUpdate() (main:137-138, guarded LIMITS_ENABLED) — daily sell-limit reset cron.
- SiegeService.initSieges() (main:142) — DEFERRED (fixture data gap, see below).
- BaseService.getInstance().initBases() (main:144) — starts casual/stained/panesterra bases.
- WorldRaidService.getInstance().initWorldRaids() (main:146) — schedules world raids via cron.
- ConquerorAndProtectorService.getInstance().init() (main:148) — registers CP worlds + kills-decrease task.
- AnnouncementService.getInstance() (main:150) — loads announcements (DAO-guarded).
- WeatherService.getInstance() (main:152) — per-zone weather state/rotation.
- BrokerService.getInstance() (main:153) — auction broker load + expiry/save schedules.
- Influence.getInstance() (main:154) — abyss influence ratios.
- ExchangeService.getInstance() (main:155) — empty ctor.
- PeriodicSaveService (main:156) — DEFERRED (reworked DI GameEngine, not faithful 1:1, see below).
- AtreianPassportService.getInstance() (main:157) — passport expire + daily-09:00 reset cron.
- AbyssRankingCache.getInstance() (main:164) — ranking-window packet cache (DAO-guarded).
- AbyssRankUpdateService.scheduleUpdate() (main:165) — rank-update + daily-GP-loss crons.
- PeriodicInstanceManager.getInstance() (main:166) — DEFERRED (AUTO_GROUP_DATA fixture gap, see below).
- EventService.getInstance().start() (main:167) — active-event collection + 5-min check cron.
- AdminService.getInstance() (main:169) — item-restriction list (IOException-guarded).
- CommandsAccessService.loadAccesses() (main:170) — command ACLs (DAO-guarded).
- PlayerTransferService.getInstance() (main:172) — REMOVE_SKILL_LIST '*' default no-op.
- CustomInstanceService.getInstance() (main:177) — empty ctor.

NULL-DEP / FIDELITY FIXES (Java @Property defaults as field initializers via CronExpressions.GetOrCreate, OR
XmlSerializer member public-ization; no invented values):
- SiegeSchedules + WorldRaidSchedules: [XmlElement]/[XmlAttribute] PRIVATE fields -> widened to public (XmlSerializer
  binds public members only; were returning null lists => InitSieges/InitWorldRaids foreach NRE). Real deser bug.
- RankingConfig.TOP_RANKING_UPDATE_RULE='0 0 0 ? * *', TOP_RANKING_DAILY_GP_LOSS_TIME='0 0 12 ? * *'.
- AutoGroupConfig.{DREDGION,KAMAR_BATTLEFIELD,ENGULFED_OPHIDAN_BRIDGE,IRON_WALL_WARFRONT,IDGEL_DOME}_TIMES =
  1-element CronExpression[] (ArrayTransformer splits on commas OUTSIDE quotes => quote-wrapped default = 1 element).
- HousingConfig.HOUSE_AUCTION_END_TIME='0 0 12 ? * SUN', AUCTION_AUTO_FILL_TIME / HOUSE_MAINTENANCE_TIME='0 0 0 ? * MON'.
- CustomConfig.LIMITS_UPDATE='0 0 0 ? * *'.
- EventsConfig.DISABLED_EVENTS = empty set (Java @Property no defaultValue but events.properties is empty =>
  CommaSeparatedValueTransformer('') => empty set, not null).
- EventData.events = new() (empty-but-non-null when timed_events XML absent; matches BaseData.baseTemplates convention).

STILL DEFERRED (each faithful 1:1 — blocked ONLY by a bootstrap test-fixture data gap or a reworked type, NOT a
port defect; the real server has the data/DB and these run):
1. **SiegeService.initSieges() (main:142)** — UpdateFortressNextState() does GetSiegeLocation(scheduledLocId).SetNextState
   with no null guard (Java has none either). The fixture loads EMPTY SIEGE_LOCATION_DATA while siege_schedule.xml
   is the full real file => GetSiegeLocation returns null => NRE. Wire once the fixture seeds matching siege location data.
2. **HousingService + HousingBidService + AuctionEndTask/AuctionAutoFillTask/MaintenanceTask (main:119-123)** —
   HousingService ctor does new HashSet<int>(PlayerDAO.GetUsedIDs()); GetUsedIDs returns NULL on DB failure (Java
   NPEs identically). No-DB fixture => ArgumentNullException. Wire once the harness provides a DB (or GetUsedIDs
   returns empty on no-DB). The 3 auction-task cron DEFAULTS are already populated (fix stands).
3. **PeriodicInstanceManager.getInstance() (main:166)** — ScheduleRegistration log line -> AutoGroupType.GetTemplate()
   -> data[self].Template needs AUTO_GROUP_DATA (absent in fixture). Cron-array hazard already fixed. Wire once the
   fixture seeds AUTO_GROUP_DATA.
4. **HTMLCache.getInstance() (main:163)** — ctor ParseDir('./data/static_data/HTML/') throws DirectoryNotFoundException
   (Java NPEs on null listFiles too). Fixture ships no HTML/ dir or html.cache. Wire once the fixture seeds an HTML dir.
5. **PvpMapService.getInstance().init() (main:176)** — unconditional InstanceService.GetNextAvailableInstance(301220000,...)
   needs that world map; fixture loads empty WORLD_MAPS_DATA. Wire once the fixture carries the pvp-map world.
6. **PeriodicSaveService (main:156)** — the C# type is a reworked DI-constructed GameEngine (only schedules a
   server-last-run variable), NOT a faithful 1:1 of Java's singleton (player/legion periodic saves). Needs a DI
   instance + faithful re-port; out of scope for a bounded getInstance() wire.

RECOMMENDED NEXT: the 5 fixture-data-gap deferrals (#1-5) all unblock with the SAME move — enrich the
GameServerBootstrapTests StaticDataFixture (or add a DB-backed harness) so SIEGE_LOCATION_DATA / AUTO_GROUP_DATA /
WORLD_MAPS(pvp-map) / an HTML dir / a (test) DB are present, then flip each deferred wire on and re-gate. #6
(PeriodicSaveService faithful re-port) is a separate scoped task. All remaining boot-init GAPs are now either wired
or one of these 6 documented deferrals — the boot-init long tail is drained to its data/DB/reworked floor.

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
- DropRegistrationService.getInstance() — DONE (wired 2026-06-16; empty ctor, bounded singleton touch).
- BaseService.getInstance() — DONE (base location registry; wired 2026-06-16 location-init cluster).
- SiegeService.getInstance() — DONE (siege location data; wired 2026-06-16).
- WorldRaidService.initWorldRaidLocations() — DONE (world-raid locations; wired 2026-06-16).
- VortexService.initVortexLocations() — DONE (vortex locations + invasion cron; wired 2026-06-16, null-cron fixed).
- RiftService.initRiftLocations() — DONE.
- LegionDominionService.initLocations() — DONE (legion-territory locations; wired 2026-06-16).
- HousingService.getInstance() — GAP? (faithful HousingService exists + runs per-instance on spawn; explicit
  boot getInstance() touch not in StartAsync — verify it self-inits via spawn path; likely effectively DONE).
- HousingService/HousingBidService/AuctionEndTask/AuctionAutoFillTask/MaintenanceTask — DEFERRED (no-DB edge:
  HousingService ctor new HashSet(PlayerDAO.GetUsedIDs()) NREs when GetUsedIDs returns null on no-DB; cron defaults fixed).
- ChallengeTaskService.getInstance() — DONE (wired 2026-06-16; empty task maps).
- SpawnEngine.spawnAll() — DONE.
- TownService.getInstance() — DONE (town registry; wired 2026-06-16). NOTE: town NPC SPAWNING still depends on
  the gated SPAWNS_DATA/TOWN_SPAWNS path (#1); this wire restores the town-level/points registry only.
- FlyRingService.getInstance() — DONE.
- RiftService.initRifts() — DONE.
- ratio-limitation block (GSConfig.ENABLE_RATIO_LIMITATION) — N/A by default (config-gated off).
- LimitedItemTradeService.start() — DONE (wired 2026-06-16; empty data => no-op).
- PlayerLimitService.scheduleUpdate() (CustomConfig.LIMITS_ENABLED) — DONE (wired 2026-06-16; LIMITS_UPDATE cron default fixed).
- SiegeService.initSieges() — DEFERRED (fixture gap: UpdateFortressNextState NREs on null GetSiegeLocation when
  SIEGE_LOCATION_DATA empty but siege_schedule.xml full).
- BaseService.initBases() — DONE (wired 2026-06-16; starts casual/stained/panesterra bases).
- WorldRaidService.initWorldRaids() — DONE (wired 2026-06-16; schedules raids via cron).
- ConquerorAndProtectorService.init() — DONE (wired 2026-06-16).
- AnnouncementService.getInstance() — DONE (wired 2026-06-16; DAO-guarded).
- DebugService.getInstance() — DONE.
- WeatherService.getInstance() — DONE (wired 2026-06-16; per-zone weather state/rotation).
- BrokerService.getInstance() — DONE (wired 2026-06-16; broker load + schedules).
- Influence.getInstance() — DONE (wired 2026-06-16; abyss influence ratios).
- ExchangeService.getInstance() — DONE (wired 2026-06-16; empty ctor).
- PeriodicSaveService.getInstance() — DONE (wired 2026-06-16, commit 62c408390; faithful singleton re-port,
  reworked DI GameEngine retired + slop test replaced).
- AtreianPassportService.getInstance() — DONE (wired 2026-06-16; expire + daily reset cron).
- CronJobService.getInstance() — DONE (this tick).
- CuringZoneService.getInstance() (guarded !GEO_MATERIALS_ENABLE; default off) — DONE (guarded, matches Java).
- RoadService.getInstance() — DONE.
- HTMLCache.getInstance() — DONE (wired 2026-06-16, commit 713fef10a; fixture seeds real HTML dir).
- AbyssRankingCache.getInstance() — DONE (wired 2026-06-16; DAO-guarded). AbyssRankUpdateService.scheduleUpdate() —
  DONE (wired 2026-06-16; ranking cron defaults fixed).
- PeriodicInstanceManager.getInstance() — DONE (wired 2026-06-16, commit 2c05275b8; fixture seeds real AUTO_GROUP_DATA).
- EventService.start() — DONE (wired 2026-06-16; empty EVENT_DATA => no-op, DISABLED_EVENTS/events null-fixes).
- AdminService.getInstance() — DONE (wired 2026-06-16; IOException-guarded file read).
- CommandsAccessService.loadAccesses() — DONE (wired 2026-06-16; DAO-guarded).
- PlayerTransferService.getInstance() — DONE (wired 2026-06-16; '*' default no-op).
- GameTimeService.startClock() — DONE.
- PvpMapService.init() — DEFERRED (fixture gap: needs world map 301220000 in WORLD_MAPS_DATA).
- CustomInstanceService.getInstance() — DONE (wired 2026-06-16; empty ctor).
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
