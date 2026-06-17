# Next slop / gated targets

Branch: feature/object-spine-bigbang. Faithful 1:1, all-green-or-revert.

## QUEST SCRIPT PORT — 1035/1035 COMPLETE (2026-06-17, commit 8fac65d3c)

The last 10 "deferred spawn-AI/flight" quests were ALL false-defers. The supposed blocker — "WalkManager /
flying-ring engine threaded into quest-tasks" — was a PHANTOM gap: every dep was already ported.
Verified present before porting: `WalkManager.StartWalking((NpcAI)npc.GetAi())`,
`QuestTasks.NewFollowingToTargetCheckTask` (ZoneName / 3-float / int-npcTargetId overloads),
`QuestEngine.RegisterOnPassFlyingRings` + `AbstractQuestHandler.OnPassFlyingRingEvent`, `SpawnInFrontOf`,
`TaskId.QUEST_FOLLOW` + `CreatureController.AddTask`, `SpawnTemplate.SetWalkerId`,
`AiEventType.FOLLOW_ME` + `OnCreatureEvent`, `SmEmotion(npc,EmotionType.CHANGE_SPEED,0,objId)`,
`DefaultFollowEndEvent`, `GetLifeStats().IncreaseFp(SmAttackStatus.TYPE.FP_RINGS,7,0,SmAttackStatus.LOG.REGULAR)`,
`SkillEngine.ApplyEffectDirectly`, `AIState.WALKING`/`SetStateIfNot`, `GetMoveController().MoveToTargetObject`,
`KnownList.FindObject`. The 10: flight-ring (_1044TestingFlightSkills, _1354PraticalAerobatics,
_2042TheLastCheckpoint), WalkManager-follow (_2333ARibbitOutOfWater, _2394ADyingWish,
_3212TheMissingCubeCraftsman, _4212MissingSidrunerk, _24053TheMaulingoftheMau, _2634TheDraupnirRedemption),
spawn-AI-walk (_14026ALoneDefense). Build 0, full suite 454/0, golden 167, bootstrap 9.
Gotchas: SkillEngine class self-shadows its namespace -> fully-qualify
`Aion.GameServer.SkillEngine.SkillEngine.GetInstance()`; `HandlerResult` is an ENUM -> use
`HandlerResultExtensions.FromBoolean(...)`; `SmAttackStatus.TYPE`/`.LOG` are nested types;
`WorldMapType.GetId()` is an extension method (needs `using Aion.GameServer.World`); `SM_DIALOG_WINDOW(objId,page)`;
`SM_NPC_INFO(Npc,Player)`. **CONTENT-HANDLER SCRIPT PORT NOW FULLY COMPLETE: quests 1035/1035 + AI 462/462 +
instance 37/37 + zone 3/3.** Next veins = runtime substrate pillars / golden-suite expansion / deferred
chat-command long tail (genuine engine-reflection blockers).

## ZONE-HANDLER PORT — COMPLETE (2026-06-17, commit cec2fa559) — 3/3

Java zone handlers live in `game-server/data/handlers/zone/*.java` = **3 total** (NOT a big set):
`_1012SensoryArea.java` (`: QuestZoneHandler`) + `pvpZones/PvPZone.java` (abstract `: AdvancedZoneHandler`) +
`pvpZones/PvPAreaZone.java` (`: PvPZone`). Base + registration was ALL already ported (same recipe as
instance/quest/AI): `ZoneNameAnnotation` attribute + `ZoneHandlerClassListener` (reflection scan) +
`ZoneService.AddZoneHandlerClass` wired in `ZoneService.Init()` via `ScriptManager.Load(WorldConfig.ZONE_HANDLER_DIRECTORY)`.
Bases present: `GeneralZoneHandler`, `QuestZoneHandler`, `AdvancedZoneHandler` (interface : IZoneHandler),
`IZoneHandler`. Ported into `dotnetConversion/src/Aion.GameServer/Handlers/Zone/` (+ `Zone/PvpZones/`).
All deps pre-present: `AbstractQuestZoneObserver` (override `OnMoved`, NOT onMoved), `PvPZoneInstance`,
`CustomConfig.KEEP_BUFFS_IN_COLISEUM`, `PlayerEffectController.SetKeepBuffsOnDie`, `PlayerReviveService.DuelRevive`
(ns Services.**Players**), `PacketSendUtility.BroadcastToZone`, the 4 PvP SM strings (STR_MSG_PvPZONE_MY_DEATH_TO_B/
HOSTILE_DEATH_TO_ME/HOSTILE_DEATH_TO_B + STR_PvPZONE_OUT_MESSAGE), `TaskId.TELEPORT`, `GetController().AddTask/
GetAndRemoveTask`, `ThreadPoolManager.Schedule(Action,long)`, `ZoneName.Get` (interned -> default ref `==` works).
**Gotchas:** QuestState ns = `Aion.GameServer.QuestEngine.Model` (CAPITAL E, file dir is `Questengine/`);
PlayerEffectController ns = `Controllers.Effects` (plural); `Player.GetEffectController()` does NOT override the
covariant return (returns base `EffectController`) so cast `(PlayerEffectController)player.GetEffectController()`
(Java relies on the covariant override; runtime type IS PlayerEffectController); Java anonymous
`AbstractQuestZoneObserver{ onMoved }` -> C# nested private sealed class capturing `questId` in its ctor (C# can't
extend anonymously). **ZONE-HANDLER SET: 3/3 COMPLETE**, build 0 / 454 / golden 167 / bootstrap 9.

## *ApRewardService STAND-INS RETIRED (2026-06-17, commit after cec2fa559) — category 3 closed

The 6 reworked `*ApRewardService` (PvpApRewardService/PvpInstanceApRewardService/PvpArenaApRewardService/
AturamSkyFortressApRewardService/EternalBastionApRewardService/StonespearReachApRewardService) are DELETED. Verified
dead-islands: each referenced ONLY by Program.cs DI + its own file (0 production/test consumers; every Result/Status
type self-contained, grep = 0 external). NONE has a Java counterpart class (invented Service+Result+Status blow-ups).
The faithful AP-reward logic now lives 1:1 in the ported instance handlers (Aturam/EternalBastion/Stonespear
onDie -> AbyssPointsService.AddAp); the 3 Pvp* ones had no faithful counterpart at all. Removed the 6 DI lines from
Program.cs (replaced with a retirement-note comment). Build 0 / 454 / golden 167 / bootstrap 9. **Capstone category 3
"instance-handler AP-reward reworked services" is now fully CLOSED** (the instance-handler frontier that gated it is
37/37 done).

## INSTANCE-HANDLER PORT — batch 1 (2026-06-16)

Java instance handlers live in `game-server/data/handlers/instance/*.java` = **37 total** (not ~78). Base
`GeneralInstanceHandler` + `[InstanceID(n)]` attribute + `InstanceHandlerClassListener` (reflection scan via
`InstanceEngine.AddInstanceHandlerClass`) are ALL already ported. Port shape = same as quest/AI scripts:
`public class XxxInstance : GeneralInstanceHandler { ctor base(instance); [InstanceID(n)]; override On*/Handle* }`.
Ported into `dotnetConversion/src/Aion.GameServer/Handlers/Instance/`.

**Batch 1 done (13 handlers, all green):** Haramel, FireTemple, AdmaStronghold, TheobomosLab, DraupnirCave,
KromedesTrial, PadmarashkasCave, DanuarSanctuary, DanuarMysticarium, DanuarReliquary (base) + DanuarReliquary_L
+ InfernalDanuarReliquary, LowerUdasTemple. **Running total 13/37.**

**Gotchas:** `SkillEngine.GetInstance()` collides with the `Aion.GameServer.SkillEngine` namespace when imported
— fully-qualify `Aion.GameServer.SkillEngine.SkillEngine.GetInstance()` (same for SpawnEngine, already qualified
in the base). `PlayerClass` needs `using Aion.GameServer.Model`. AtomicBoolean/AtomicInteger → `int` +
`Interlocked.CompareExchange`/`Increment` + `Volatile.Write` (per threadpool-async-idiom memory). `Future<?>` →
`ScheduledTask` (`IsCancelled` property, `Cancel(bool)`). AiEventType ns = `Aion.GameServer.Ai.Event`,
AbnormalState ns = `Aion.GameServer.SkillEngine.Effects`. Java arrow-switch → C# switch statement/expression.

**Batch 2 done (11 handlers, all green, commit 3b2221a2b):** OphidanBridge (+ _L subclass), SeizedDanuarSanctuary,
Beshmundir, InfinityShard, AturamSkyFortress, SauroSupplyBase, RentusBase, OccupiedRentusBase, RaksangRuins,
TalocsHollow. **Running total 24/37.** Made base `GeneralInstanceHandler.IsBoss(Npc)` `virtual` (Java has no
`final`; InfinityShard/Sauro/Rentus/OccupiedRentus override it) — interface-free, no other change. Batch-2
gotchas confirmed: `WalkManager.StartWalking((NpcAI)npc.GetAi())` (StartWalking returns bool, just call it; NpcAI
ns `Aion.GameServer.Ai`, manager ns `Aion.GameServer.Ai.Manager`); `SM_ATTACK_STATUS.TYPE.HP/MP` + `LOG.REGULAR`
→ `using TYPE = ...ServerPackets.SmAttackStatus.TYPE; using LOG = ...SmAttackStatus.LOG;` (class is `SmAttackStatus`
but SM_PLAY_MOVIE/SM_QUEST_ACTION/SM_EMOTION/SM_SYSTEM_MESSAGE keep SCREAMING names); `ItemService` ns is
`Aion.GameServer.Services.**Items**` (plural); `ZoneName.Get(...)` static + `zone.GetAreaTemplate().GetZoneName()` /
`zone.GetZoneTemplate().GetName()`; `Rnd.Get(int[])`/`NextInt`/`NextBoolean`/`NextFloat`; Java `scheduleAtFixedRate`
→ `ThreadPoolManager.GetInstance().ScheduleAtFixedRateTask(_ => {...; return ValueTask.CompletedTask;}, TimeSpan.Zero,
TimeSpan.FromMilliseconds(delay))` (returns ScheduledTask w/ Cancel); FlyRing via `new FlyRing(new FlyRingTemplate(
name, mapId, Point3D(double…)×3, radius), instance.GetInstanceId()).Spawn()`; `Math.toRadians` → `Math.PI/180.0 * x`;
`SummonsService.DoMode(SummonMode.RELEASE, summon, UnsummonType.UNSPECIFIED)`; `Item.GetItemId()` (Item =
`Model.GameObjects.Item`, base `IsRestrictedToInstance(Item)` override matches). Java arrow-switch w/ multi-int
labels → C# switch w/ explicit `break;` per group.

## INSTANCE-HANDLER PORT — batch 3 (2026-06-16) — +8 → 32/37

Ported: LinkgateFoundry (117), TiamatStrongHold (265), DarkPoeta (301), DragonLordsRefuge (348),
AnguishedDragonLordsRefuge (236, `: DragonLordsRefuge`), NightmareCircus (361), IlluminaryObelisk (395),
InfernalIlluminaryObelisk (161, `: IlluminaryObelisk`). All 1:1, all-green (build 0 / 454 / golden 167 / bootstrap 9).
DarkPoeta scoreboard surface was ALL pre-ported (DarkPoetaScore/DarkPoetaScoreWriter/SM_INSTANCE_SCORE/
InstanceProgressionType ext IsStartProgress·IsEndProgress / `(TemporaryPlayerTeam)instance.GetRegisteredTeam()` /
`player.GetAbyssRank().GetRank().GetId() >= AbyssRankEnum.STAR1_OFFICER.GetId()`). NightmareCircus/Obelisk used only
WalkManager + standard surface. **Bounded dep added** (only one needed): 3 IDTIAMAT countdown entries in
`SM_SYSTEM_MESSAGE.cs` (COUNTDOWN_START 1401547 / DRAKAN_ON_DIE 1401551 / COUNTDOWN_OVER 1401563) — the C# catalog is a
subset; ids copied verbatim from the Java oracle. **Batch-3 gotchas:** `PlayerReviveService` ns = `Services.Players`
(plural); `ScheduleAtFixedRate` returns `Task` — use **`ScheduleAtFixedRateTask`** for a cancellable `ScheduledTask`,
which has NO Action overload (pass `_ => {...; return ValueTask.CompletedTask;}` + `TimeSpan` args); `AIActions.UseSkill`/
`TargetCreature` need `(NpcAI)npc.GetAi()` cast (GetAi() returns non-generic `AbstractAI`; AIActions wants
`AbstractAI<T>`); `AtomicInteger.compareAndSet(exp,upd)` → `Interlocked.CompareExchange(ref, upd, exp) == exp`;
`System.currentTimeMillis()` → `DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()`; `Race.GetRaceId` ext is in ns
`Aion.GameServer.Model`; `instance.forEach` → `ForEachObject`; `.stream().allMatch` → `TrueForAll`; the protected
7-arg walker `Spawn(id,x,y,z,h,delay,walkerId)` overload is distinct from base 5/6-arg Spawn.

**Remaining 5 = the heavy-defers only:** ShugoImperialTomb (1329 lines), StonespearReach (925), EternalBastion (867),
DrakenspireDepths (593), TheShugoEmperorsVault (574). Each pulls a larger subsystem (AP-reward reworked services /
heavier scoreboard / siege / multi-stage). Recommend tackling EternalBastion or DrakenspireDepths next (smallest of
the five) after verifying its ApReward/InstanceScore surface; the other 4 are genuine heavy ports.

## INSTANCE-HANDLER PORT — batch 4 (2026-06-16) — +1 → 33/37

Ported the smallest heavy-defer: **TheShugoEmperorsVault (574 lines)** → `TheShugoEmperorsVaultInstance.cs`,
`[InstanceID(301400000)]`, commit e7cfbdf89. STRICT 1:1. **No bounded dep needed** — all deps were already present
and verified before porting: `NormalScore` (Model/Instance/Instancescore) + `TheShugoEmperorsVaultScoreWriter`
(Network/Aion/Instanceinfo) both pre-ported; all SM strings present (`STR_IDSweep_Stage2_End` 24055 /
`STR_MSG_GET_SCORE` / `STR_REBIRTH_MASSAGE_ME`); base `Spawn`/`SpawnAndSetRespawn`/`SendMsg`/`OnStartEffect`/
`OnReviveEvent`/`GetInstanceScore` all on `GeneralInstanceHandler`; `InstanceProgressionType.IsPreparing/IsStartProgress`;
`instance.SetDoorState/ForEachPlayer/ForEachNpc`; `SkillEngine.ApplyEffectDirectly`; `Rnd.Chance()/Get(int,int)`;
`TaskId.DESPAWN` + `controller.AddTask`; `ItemService.AddItem` (ns Services.**Items**); `PlayerReviveService.Revive`
(ns Services.**Players**); `TeleportService.TeleportTo/MoveToInstanceExit`. **Batch-4 gotchas:** `(byte) -29` negative
heading → C# needs `unchecked((byte)-29)` (CS0221 otherwise; same convention as LinkgateFoundry); `ScheduledTask.IsDone()`
is a **method** not a property; `schedule(r, 1, TimeUnit.MINUTES)` → `Schedule(r, 60000L)`; Java `synchronized` methods
→ per-method `private readonly object xLock = new(); lock(xLock){...}`; `Set<Integer> + ConcurrentHashMap.add()` →
`ConcurrentDictionary<int,byte>` + `TryAdd(k,0)`; `AtomicInteger stage` → `int` + `Interlocked.Increment` /
`Volatile.Read`. So TheShugoEmperorsVault was actually NOT a heavy-subsystem defer — its scoreboard/score-writer
pillar was already in place; it was just a large (574L) mechanical port.

## INSTANCE-HANDLER PORT — batch 5 (2026-06-16) — +1 → 34/37

Ported **DrakenspireDepths (593 lines)** → `DrakenspireDepthsInstance.cs`, `[InstanceID(301390000)]`, commit afb7dc119.
STRICT 1:1. **No bounded dep needed** — ANOTHER false-heavy-defer. This handler has **NO scoreboard at all** (no
ScoreWriter, no InstanceScore subtype, no AP-reward path) — it is pure staged-event timer/spawn logic. All deps
verified pre-present before porting: every `STR_MSG_IDSEAL_*` SM string already in the catalog (TWIN/IMMORTAL/WAVE/
WAVE_BONUS/GUARDIAN/VRITRA_HUMAN — used by a sibling); `WalkManager.StartWalking((NpcAI)npc.GetAi())`;
`RespawnService.ScheduleDecayTask(npc, 4000L)`; `npc.GetSpawn().GetStaticId()` + `SetWalkerId` on `SpawnTemplate`;
`SM_EMOTION(npc, EmotionType.CHANGE_SPEED)` + `PacketSendUtility.BroadcastPacket`; `Rnd.Get(min,max)`;
`instance.SetDoorState`; `Skill.UseSkill()` via `Aion.GameServer.SkillEngine.SkillEngine.GetInstance().GetSkill(...)`.
**Batch-5 gotchas:** `AtomicReference<Race>` → `Race? race` field + per-field `lock` for the compareAndSet-null-guard
in OnEnterInstance; the two `scheduleAtFixedRate(new Runnable(){ int count; run(){ switch(++count) }})` stateful
inner classes → captured local `int count = 0;` + `ScheduleAtFixedRateTask(_ => { switch(++count){...}; return
ValueTask.CompletedTask; }, TimeSpan.FromMilliseconds(initial), TimeSpan.FromMilliseconds(period))` (needs `using
System.Threading.Tasks;`); ScheduleAtFixedRateTask has **no Action overload** (must return ValueTask + TimeSpan args),
but plain `Schedule(()=>{...}, longMillis)` void-lambda binds the `Schedule(Action,long)` overload fine; `getAndSet`
→ `Interlocked.Exchange`; `compareAndSet(exp,upd)` → `Interlocked.CompareExchange(ref,upd,exp)==exp`. Confirms the
pattern: heavy-by-line-count ≠ heavy-by-subsystem.

## INSTANCE-HANDLER PORT — batch 6 (2026-06-16) — +1 → 35/37

Ported **EternalBastion (867 lines)** → `EternalBastionInstance.cs`, `[InstanceID(300540000)]`, commit da0d6ff53.
STRICT 1:1. **No bounded dep needed** — ANOTHER false-heavy-defer. This handler DOES have a scoreboard (NormalScore +
EternalBastionScoreWriter + InstanceScore base), but ALL of it was pre-ported: `NormalScore` (full points/rank/AP +
4 reward item/count pairs), `EternalBastionScoreWriter : InstanceScoreWriter<NormalScore>`, `InstanceScore.IsRewarded/
Set+GetInstanceProgressionType`, `InstanceProgressionType` (PREPARING/START_PROGRESS/END_PROGRESS), all 15
`STR_MSG_IDLDF5b_TD_*` SM strings (MainWave_01-06/AddWave_01-03/Notice_02/04/06), `STR_MSG_GET_SCORE(l10n,points)`,
`SM_INSTANCE_SCORE(mapId, writer, time)`, every service (`AbyssPointsService.AddAp`, `ItemService.AddItem`,
`PlayerReviveService.Revive`, `TeleportService.MoveToInstanceExit`+`TeleportTo`), `Rnd.NextBoolean`,
`Point3D(x,y,z)`+GetX/Y/Z, `instance.ForEachDoor/ForEachNpc/ForEachPlayer/GetPlayersInside`,
`door.SetOpen`, `npc.GetObjectTemplate().GetL10n()`, `Spawn(...).GetSpawn().SetWalkerId(w)`. Pure mechanical 1:1.
**Batch-6 gotchas:** ItemService ns = `Services.Items`, PlayerReviveService ns = `Services.Players` (recurring traps);
`log.LogInformation(...)` needs `using Microsoft.Extensions.Logging;` (base `log` is ILogger, the named-placeholder
overload is an extension method — no prior instance handler had used it so the using was not transitively present);
`ScheduleAtFixedRateTask` has only Runnable(interface — can't `new`) + `Func<CT,ValueTask>` overloads, so use
`_=>{SpawnAssaultWave();return ValueTask.CompletedTask;}`+two `TimeSpan` args; AtomicInteger reads inside `if`
conditions → `Volatile.Read(ref field)`, `.addAndGet(-2)`→`Interlocked.Add(ref,-2)`, `.decrementAndGet`/`.incrementAndGet`
→`Interlocked.Decrement/Increment`; `AtomicBoolean.compareAndSet(false,true)`→`Interlocked.CompareExchange(ref
isRaceSet,1,0)==0`. Confirms again: heavy-by-line-count ≠ heavy-by-subsystem.

## INSTANCE-HANDLER PORT — batch 7 (2026-06-17) — +1 → 36/37

Ported **StonespearReach (925 lines)** → `StonespearReachInstance.cs`, `[InstanceID(301500000)]`, commit b24029fdc.
STRICT 1:1. **No bounded dep needed** — ANOTHER false-heavy-defer (Legion Dominion siege defense, sibling of
EternalBastion/IlluminaryObelisk). Scoreboard surface ALL pre-ported: `LegionDominionScore` (points/rank/finalGP/finalAP +
4 reward item/count pairs), `LegionDominionScoreWriter : InstanceScoreWriter<LegionDominionScore>`, `InstanceScore`
base (IsRewarded/IsStartProgress/IsPreparing/Set+GetInstanceProgressionType/Clear), `LegionDominionService.GetInstance()
.OnFinishInstance(legion,points,time)`, ALL SM strings (`STR_MSG_GET_SCORE`, `STR_MSG_OBJ_Start/_Bomb/_Bomb_Die`,
`STR_MSG_LEGION_DOMINION_MOVE_BIRTHAREA_FRIENDLY(name)`, `STR_MSG_CANT_INSTANCE_TOO_MANY_MEMBERS(num,mapId)`), every
service (`ItemService.AddItem`, `GloryPointsService.AddGp`+`Rates.GP.CalcResult`, `AbyssPointsService.AddAp`,
`PlayerReviveService.Revive`, `TeleportService.TeleportTo`(instance + worldId overloads)+`MoveToBindLocation`),
`Legion.GetCurrentLegionDominion/GetLegionId`, `WorldPosition(mapId,x,y,z,h)`+GetX/Y/Z/Heading, `PositionUtil.GetDistance`,
`Rnd.Get(min,max)`/`Rnd.NextFloat(bound)`, `instance.ForEachPlayer/ForEachNpc/GetPlayersInside/GetNpc/GetMapId`.
**Batch-7 gotchas:** GloryPointsService AND AbyssPointsService each exist in BOTH `Services` and `Services.Abyss`
(CS0104 ambiguous) — Java imports `services.abyss.*` so fully-qualify `Aion.GameServer.Services.Abyss.GloryPointsService`/
`...AbyssPointsService` (LegionDominionService is the plain `Services` ns); Java `synchronized checkRank/canEnter` →
per-instance `lock(@lock)` field (a real field, NOT `new object()` each call); `Collections.shuffle` → manual Fisher-Yates
with `Rnd.Get(0,i)`; `IntStream.range(min,max+1)` → `Enumerable.Range(min, max+1-min)`; `Math.toRadians(d)` → `d*Math.PI/180.0`;
`Future`→`ScheduledTask`, `.isCancelled()`→`.IsCancelled` (property), `.cancel(b)`→`Cancel(b)`; `startTime` `long?` →
use `.Value` in the subtraction; `points.get(i)`→`points[i]`. Confirms again: heavy-by-line-count ≠ heavy-by-subsystem.

## INSTANCE-HANDLER PORT — batch 8 (2026-06-17) — +1 → 37/37 — SET COMPLETE

Ported **ShugoImperialTomb (1329 lines)** → `ShugoImperialTombInstance.cs`, `[InstanceID(300560000)]`, commit d2e7aab80.
STRICT 1:1. **No bounded dep needed** — the LAST instance handler, and an 8th-of-8 false-heavy-defer. 3-stage
tower-defense (Crown Prince / Empress / Emperor zones), each stage a PHASE_1 wave sequence → boss → PHASE_2 finale,
plus bonus stages, exit portals and ~150 relic chests. **NO scoreboard at all** (no ScoreWriter/InstanceScore subtype) —
pure staged spawn-wave timer logic. ALL deps pre-present: every `STR_IDEVENT01_*` string (`_S1_START/_S2_START/_S3_START`,
`_PHASE/_PHASE02/_PHASE03/_PHASE04/_PHASE09/_PHASE10`) already in the catalog; `WalkManager.StartWalking((NpcAI)npc.GetAi())`,
`CreatureState.ACTIVE/WALK_MODE`, `Creature.SetState(state,bool)`, `SM_EMOTION(npc,EmotionType.CHANGE_SPEED,0,objId)`,
`SkillEngine.ApplyEffectDirectly(skillId,Creature,Creature)`, `TeleportService.MoveToInstanceExit`, base
`Spawn`/`DeleteAliveNpcs`/`SendMsg`/`mapId`. **Batch-8 gotchas (all anticipated):** `SkillEngine.GetInstance()` collides
with the `Aion.GameServer.SkillEngine` ns → fully-qualify `Aion.GameServer.SkillEngine.SkillEngine.GetInstance()`;
`AtomicInteger stage.compareAndSet(exp,upd)` → `if (Interlocked.CompareExchange(ref stage,upd,exp) != exp) return;`
(early-return predicate INVERTED vs Java `if (!cas) return;`); `stage.get()` in the transformation switch →
`Volatile.Read(ref stage)`; `Future`→`ScheduledTask`, `.isDone()`→`.IsDone()` (METHOD), `.cancel(true)`→`Cancel(true)`;
the `sp()` walker helper = `(Npc)Spawn(...)` cast + `GetSpawn().SetWalkerId(w)` + `WalkManager.StartWalking((NpcAI)npc.GetAi())`
+ ACTIVE-vs-WALK_MODE state + CHANGE_SPEED emotion broadcast. Confirms the rule one final time: heavy-by-line-count ≠
heavy-by-subsystem.

**INSTANCE-HANDLER SET: 37/37 COMPLETE.** All handlers in `game-server/data/handlers/instance/*.java` ported 1:1 & green
(build 0 / 454 / golden 167 / bootstrap 9). The standing "instance-handler AP-reward reworked services" slop category
(category 3 below) is now fully unblocked — those reworked `*ApRewardService` stand-ins can be retired in favor of the
faithful handlers. **Next recommended vein:** zone handlers in `game-server/data/handlers/zone/` (apply the same
extend-base + auto-register recipe; grep PascalCase + check base classes to avoid false-defers), OR the deferred ~10
spawn-AI/flight quest scripts (need WalkManager/flying-ring threaded into quest tasks), OR golden-suite expansion to
cover instance-handler runtime behavior. See content-handler-scope memory for the running tally.

## CAPSTONE FIDELITY RE-SURVEY — 2026-06-16 (commit e3e1b1184)

Final comprehensive read-only sweep of the whole src tree across the 6 slop/silent-gap categories. Two real
bounded null-config bugs found + fixed (faithful, all-green); the rest confirmed clean OR scoped as the known
LARGER porting frontier (instance/quest/AI handlers). Build 0, full suite 454/0, golden 167/167, bootstrap 9/9.

### Survey results by category
1. **Hollow DataManager holders** — CLEAN. Zero `= new()` / `=> new()` static holders. All ~120 `*_DATA`
   accessors delegate to the live `StaticData` instance (`SD.*`) bound at boot via RegisterInstance. The 13
   wired holders confirmed still wired; no straggler.
2. **Reworked `*Summary`/`*Table` parallel projections** — CLEAN. The retired shadows (NpcTemplateSummary/
   SkillTemplateSummary/ItemTemplateSummary/Tempering/CustomNpcDrop/Housing/NpcSpawn) are gone (grep = 0). The
   remaining ~50 `*Summary` records all live inside `*Table` types that StaticData actually builds + exposes
   (the faithful cache-deserialized loader model, model A) — consumed, not dead-island shadows.
3. **Invented `*Service` micro-fragments** — 14 services lack an exact `<Name>.java`. Of these: 4 are legit
   idiomatic infra (GameServerBootstrapService/GameServerHostedService/OutboundLinkHostedService/
   StaticDataService — allowed per the infra-idiomatic principle). The other 10 are a REWORKED-SUBSTITUTE
   cluster standing in for UNPORTED faithful instance handlers — SCOPED as LARGER below (NOT bounded deletes:
   they're DI-live in Program.cs and deleting them without porting the handler leaves a gap).
4. **Reworked `Sm*` shadowing faithful `SM_*`** — the Npc/House/Loot/Kisk/Rift shadows are retired (grep = 0).
   ONE pair survives: `SmPet`/`SmPetEmote` (snapshot-DTO reworked, NotSupportedException stubs) shadow the
   faithful `SM_PET`/`SM_PET_EMOTE` (17 production consumers). SCOPED below (tied to a design-scaffold test +
   Phase-6 design doc; not a clean 0-consumer delete).
5. **Null Config statics / boot-init** — **2 REAL BUGS FOUND + FIXED** (commit e3e1b1184):
   `MembershipConfig.MEMBERSHIP_TYPES` (null -> NRE on the LIVE enter-world path in PlayerEnterWorldService for
   any membership>0 account; Java loads `{"Premium"}` from membership.properties) and
   `HousingConfig.HOUSE_AUCTION_REGISTER_DAYS` (null -> NRE in HousingBidService `[0]`/`[1]`; Java loads `{1,5}`
   from housing.properties). Both initialized as field initializers to the shipped property-file values
   (faithful, no invented values). All CronExpression statics confirmed initialized (incl. the prior
   PVP_MAP_RANDOM_BOSS_SCHEDULE fix); `ShutdownConfig.RESTART_SCHEDULE = null` is FAITHFUL (Java @Property has
   no default + empty properties => null, and Java's ShutdownHook null-guards it).
6. **Production NotImplemented/TODO in gameplay paths** — CLEAN. All ~35 NotSupportedException are faithful 1:1
   of Java UnsupportedOperationException (LegionWarehouse-behind-proxy, SiegeService cron-convert, InstanceService
   invalid-call, EffectTemplate unhandled-hoptype, etc.). All ~30 TODO/FIXME comments are verbatim carry-overs of
   TODOs in the Java source (correct fidelity, not invented stubs). The only non-faithful stubs are the SmPet
   shadow's (scoped in #4).

### Bounded fix applied
- **commit e3e1b1184** — category 5, the two null-config NREs above. Build 0, full suite 454/0, golden 167/167,
  bootstrap 9/9. The MEMBERSHIP_TYPES fix in particular removes a latent crash directly on the Front-A
  enter-world frontier (any premium account would have NRE'd on enter-world).

### LARGER items scoped (NOT forced — each is the documented handler-porting frontier, not a bounded slop delete)
- **Instance-handler AP-reward reworked services (category 3).** 6 `*ApRewardService`
  (Aturam/EternalBastion/Stonespear + Pvp/PvpArena/PvpInstance) + the timing/scheduler/registration services are
  DI-registered reworked stand-ins for UNPORTED faithful instance handlers. Java has 78 instance handlers under
  game-server/data/handlers/instance; only 8 are ported in C#. E.g. AturamSkyFortressApRewardService is a
  Service+Result-record+Status-enum blow-up of AturamSkyFortressInstance.onDie's 2-line `AbyssPointsService.addAp
  (player, 540)`. FAITHFUL RESOLUTION = port the instance handlers 1:1 (extend the faithful instance-handler base,
  override onDie/onEnterInstance) and retire the services — same family/effort class as the 1,035 quest + 503 AI
  script port (memory: content-handler-scope). NOT a bounded all-green delete.
- **SmPet/SmPetEmote reworked shadow + its design-scaffold test (category 4).** `SmPet.cs`/`SmPetEmote.cs` are a
  reworked snapshot-DTO pet-packet design with NotSupportedException stubs; production uses the faithful
  `SM_PET`/`SM_PET_EMOTE` (17 consumers, golden-tested). The ONLY consumer of the shadow is
  `PetJavaVectorArtifactReaderTests` — a documented Phase-6 design scaffold (docs/Phase-6-BindPointTeleport-
  KnownListPetGoldenVectorDesign.md; the test self-reports "Java known-list pet vector artifacts are not present
  yet"). FAITHFUL RESOLUTION = either complete the Phase-6 known-list-pet golden-vector work against the faithful
  SM_PET (preferred), or retire the shadow + scaffold test together. Held (don't discard documented in-progress
  design work as a "clean delete").

### DEFINITIVE VERDICT
The autonomous IN-MEMORY + DB-ctor fidelity arc is **COMPLETE** for the slop-retirement / hollow-holder /
boot-init / null-config frontier. After this capstone sweep: hollow holders 0, shadow-Summary/Table 0, live
shadow-Sm packets reduced to 1 scaffold-only pair, invented infra services are either legit-idiomatic or the
known handler-port frontier, null-config boot bugs 0 (2 last ones fixed here). The remaining work is NOT
"slop to retire" — it is two well-bounded categories of GENUINE PORTING (78-8=70 instance handlers; the
Phase-6 pet golden-vector design) plus the user-environment-gated **Front-A real-client enter-world test**.
The MEMBERSHIP_TYPES fix notably de-risks that Front-A test (it was a guaranteed enter-world NRE for premium
accounts).

### Honest final fidelity assessment
The boot/data/config/packet-base spine is faithful and green end-to-end (DB-backed full boot validated, golden
parity 167/167, 454/0 suite). The HONEST gap is gameplay BREADTH, not boot fidelity: ~70 instance handlers and
the long tail of content handlers remain to port (consistent with the parity-state memory's ~15-25%
full-gameplay estimate). Nothing invented or orphaned was left behind by this sweep; the two fixes are strict
property-file-faithful. There is no remaining un-blocked autonomous *slop/config* work — the next moves are
deliberate content-handler porting batches (instance/quest/AI) and the environment-gated client test.

## RESOLVED 2026-06-16 — full-suite test-isolation (one-process `dotnet test` now 454/0)

The suite passed per-class but flaked 2-4 tests in a single process. Three failures, all diagnosed + fixed
test-infra-only (no production hack, no weakened assertion):
- **GoldenStatsInfoFixtureTests.CsharpStatsInfoMatchesJavaGoldenFixture** (SM_STATS_INFO byte#4 = game-time D
  = 0x2D vs Java 0) — POLLUTION. `GameServerBootstrapTests` constructs a `GameTimeService` (ctor unconditionally
  sets the `_instance` singleton) and advances it to a non-zero game-minute (`WaitUntilAsync(GameMinutes>0)`),
  racing the golden packet fixtures that read `GameTimeService.GetInstance().GetGameTime().GetTime()` and assert
  time 0. FIX: (1) added `[Collection("GoldenDataManager")]` to GameServerBootstrapTests (serialize, no parallel
  race); (2) the 3 golden fixtures' `EnsureGameTimeSingleton` now ALWAYS reconstructs a 0-minute instance instead
  of skipping when one already exists, so it resets the singleton if a serialized sibling left it advanced.
- **JaxbHolderLoaderTests.LoadFromFile_PopulatesWorldMapsDataFromRealXml** (twin counts 1/0 vs expected 5/6) —
  REAL STALE EXPECTATION (failed isolated too). Stale vs the 2026-06-16 faithful twin-clamp fix (commit 4e0e872a7).
  The test was asserting the clamping accessors `GetTwinCount()`/`GetBeginnerTwinCount()`; it actually verifies XML
  binding, so it now asserts the raw deserialized fields `TwinCount`/`BeginnerTwinCount` (5/6), independent of the
  mutable `WorldConfig` statics.
- **GameServerOptionsTests.LoadDatabaseOptionsFromJavaConfig** (port 3306 vs 3307) — REAL STALE EXPECTATION
  (failed isolated too). `mygs.properties` (loaded last, Java mygs-override-wins parity) points the DB at the local
  Docker MySQL on 3307. Faithful behavior; test expectation corrected 3306 -> 3307.

## EMPIRICAL — DB-backed full-boot smoke RUN against the live MySQL container (2026-06-16)

The prior read-only static analysis (sections below) is now CONFIRMED AT RUNTIME. New opt-in env-gated test
`GameServerBootstrapTests.GameServerBootstrap_DbBackedFullBoot_RunsRealStartAsyncAgainstLiveMySql` (early-returns
unless `AION_GAMESERVER_DB_INTEGRATION=1`, mirroring SystemMailRepositoryDatabaseIntegrationTests' DatabaseFactory/
schema setup): points DatabaseFactory at 3307/aion_gs (root/aion), applies the real `game-server/sql/aion_gs.sql`
schema, loads the REAL DataManager via `DataManager.LoadAsync(repoRoot)` (147 MB cache + game-server/data), inits
AIEngine/ZoneService/GeoService (the spawn-critical engines, as the spawn-backed test does), and runs the FULL
`GameServerBootstrapService.StartAsync` via a pass-through `IStaticDataLoader` + the real `MySqlUsedIdRepository`.

### CORRECTED VERDICT (2026-06-16, root-cause re-investigation): VERDICT (a) — the house-twin throw was a REAL C#
DIVERGENCE, now FIXED. The earlier "Java-latent" conclusion (below) was WRONG: it traced spawnAll/spawnHouses/
storeObject faithfully but MISSED the twin-count clamp. Java's `WorldMapTemplate.getBeginnerTwinCount()` /
`getTwinCount()` (WorldMapTemplate.java:96-108) clamp the raw XML attributes by `WorldConfig`:
- `WORLD_MAX_TWINS_BEGINNER` default **-1** (disabled) => `getBeginnerTwinCount()` returns **0** (NOT the raw 3).
- `WORLD_MAX_TWINS_USUAL` default **1** => `getTwinCount()` = min(1, 0) = 0 => WorldMap defaults to 1.
So Java's `getInstanceCount()` for Heiron/Beluslan = **1**, not 4 — Java pre-creates ONE instance, SpawnHouses runs
ONCE, NO collision. **The C# `WorldMapTemplate.GetTwinCount()/GetBeginnerTwinCount()` returned the RAW XML values
(0 and 3) — skipping the WorldConfig clamp** (a TODO-backlog stub left from before WorldConfig was ported), giving
instanceCount=4 and the twin re-spawn collision. FIX: ported the two clamp methods 1:1 to Java (commit below).
RESULT after fix: the DB-backed full boot completes CLEANLY (IsStarted, world populated, StopAsync clean).

### (superseded) ORIGINAL RESULT: THROWS — at SpawnEngine.SpawnAll() -> HousingService.SpawnHouses() ->
World.StoreObject, a house-twin `DuplicateAionObjectException`:
- **Heiron (mapId 210040000)**: House `HOUSE_6001` objectId **130885** re-spawned into a 2nd twin instance.
- **Beluslan (mapId 220040000)**: House `HOUSE_7001` objectId **152343** re-spawned into a 2nd twin instance.
This was the symptom of the missing twin-count clamp (instanceCount over-counted 4 vs Java's 1), NOT Java-latent.

### #2 Housing no-DB deferral is EMPIRICALLY LIFTED. With the live (empty) players table, `PlayerDAO.GetUsedIDs()`
returned `int[0]` (not null), so the HousingService ctor's `RevokeOwnershipOfDeletedPlayers` did NOT throw
ArgumentNullException — the boot reached deep into SpawnAll and HousingService loaded + began SpawnHouses cleanly.
This proves the ArgumentNullException deferral was purely a no-DB artifact, NOT a port defect. The HousingService
`GetInstance()` block in StartAsync STAYS commented out anyway, because (a) enabling it does not change the SpawnAll
house-twin boundary, and (b) the no-DB bootstrap fixture (empty WORLD_MAPS_DATA => SpawnAll iterates zero maps =>
HousingService never reached) must stay green. The deferral comment in GameServerBootstrapService.cs:152-175 was
updated to record this empirical finding.

### Test disposition (faithful, not faked): the test captures StartAsync's outcome and, on throw, asserts the
flattened exception chain contains the `DuplicateAionObjectException` (the documented house-twin boundary) — NOT an
unrelated DAO/NRE regression. If StartAsync ever boots clean (e.g. seeded non-twin world_maps), the test asserts
IsStarted + world populated + StopAsync. Green either way; faithfully asserts the documented-throw boundary today.

### Genuine remaining frontier (post-empirical): the in-memory + DB-ctor floors are cleared. What remains is purely
environment/spawn-harness gated, NOT porting gaps:
1. **Whole-world clean SpawnAll** requires either seeding a non-twin world_maps subset OR mirroring the faithful
   house-twin throw — there is NOTHING to fix (Java throws identically). To exercise SpawnAll past housing in a
   single-map deterministic way, the spawn-backed test (SpawnObject per Sanctum template) already does this green.
2. **#1 SiegeService.initSieges() + #5 PvpMapService.init()** — RESOLVED 2026-06-16 (see top section below).
   Both now exercised+asserted in the DB-backed full-boot test and proven clean against real data; faithfully
   kept OUT of the always-on minimal-fixture StartAsync path (Java does not guard for empty data). One real
   port defect fixed en route (null CronExpression default). Boot tail is now CLOSED.
3. **Front-A real client -> enter-world** (memory three-server-stack-boots): needs the running server process +
   populated DB, same class of environment-gated work, not more porting. THIS IS NOW THE SOLE REMAINING FRONTIER.

## RESOLVED — boot tail CLOSED: SiegeService.initSieges() + PvpMapService.init() wired faithfully DB-gated (2026-06-16)

The two final deferred GameServer.main wires are now closed.

### Java guard analysis (source of truth)
- **SiegeService.initSieges()** (SiegeService.java:99-101): `if (!isInitialized.compareAndSet(false,true) ||
  !SiegeConfig.SIEGE_ENABLED) return;`. SIEGE_ENABLED defaults **true** (siege.properties
  `gameserver.siege.enable = true`), so the guard does NOT no-op — the full body runs and REQUIRES populated
  SIEGE_LOCATION_DATA. updateFortressNextState() does `getSiegeLocation(id).setNextState(...)` with NO null guard.
- **PvpMapService.init()** (PvpMapService.java:27-30): NO guard at all — not even PVP_MAP_ENABLED (which defaults
  false) is checked. Unconditionally calls `InstanceService.getNextAvailableInstance(301220000, ...)` which
  REQUIRES world map 301220000 to exist. So it runs always and needs real WORLD_MAPS_DATA.

### Disposition: both kept OUT of the always-on StartAsync, exercised+asserted in the DB-backed test (faithful)
Neither Java path guards for empty/disabled data, so wiring them unconditionally in the StartAsync used by the
minimal no-DB fixture (empty SIEGE_LOCATION_DATA / empty WORLD_MAPS_DATA) would NRE the 9/9 bootstrap gate. The
HARD RULE forbids inventing a C# guard Java lacks. Faithful resolution: exercise+assert them in
`GameServerBootstrap_DbBackedFullBoot_*` (real data) right after the clean StartAsync, with CWD pinned to
game-server so siege_schedule.xml's relative path resolves. Both now run CLEAN against live data; test asserts no
throw + PvpMapService handler registered (GetParticipantsSize()==0 live-handler path). The deferral comments in
GameServerBootstrapService.cs were rewritten to "FAITHFUL DB-GATED" with the line-ref evidence.

### Real port defect surfaced + fixed (1:1): null CronExpression default
PvpMapService.Init() -> PvpMapHandler.OnInstanceCreate() -> StartRandomBossTask() schedules off
`CustomConfig.PVP_MAP_RANDOM_BOSS_SCHEDULE`, which was left **null** in C# (CustomConfig.cs) — so
CronService.Schedule NRE'd on `cronExpression.CronExpressionString`. Java declares it with @Property defaultValue
`"0 30 14,18,21 ? * *"` (CustomConfig.java:264). Fixed faithfully by initializing the field inline via
`CronExpressions.GetOrCreate("0 30 14,18,21 ? * *")` — the same default-init pattern AutoGroupConfig uses for its
CronExpression[] fields. SiegeService.InitSieges() itself needed no fix (ran clean against real data first try).

### Green gate after the change: build 0, Golden 167/167, Bootstrap 9/9 (minimal stays green), RealStaticDataLoad 1/1, DbBackedFullBoot 1/1.

## RESOLVED — house-twin-spawn question: VERDICT (c) GENUINE JAVA LATENT BUG, C# mirrors faithfully, NO CODE CHANGE (read-only analysis, 2026-06-16)

QUESTION: full-world SpawnEngine.SpawnAll re-spawns the same address-cached House objectId into each of a
map's getInstanceCount() twin instances -> DuplicateAionObjectException. Is this a real Java latent bug (mirror
it), or does Java avoid it (per-instance objectIds / instanceCount==1 for housing maps / spawnHouses keys off
instanceId)?

### VERDICT: (c) — Java genuinely double-spawns the SAME House object across twin instances and has NO guard.
C# is a byte-for-byte faithful mirror. **NO fix applied** (the hard rule forbids inventing a guard Java lacks).
NOT case (a) (Java does NOT mint a fresh objectId per twin — it reuses the cached House), NOT case (b) (housing
maps DO have getInstanceCount() > 1 — twins happen).

### Java evidence (line refs)
- **SpawnEngine.spawnAll** (game-server SpawnEngine.java:119-126): `worldMap.forEach(instance -> spawnInstance(
  instance, (byte)0, instance.getOwnerId()))` — iterates ALL instances (WorldMap implements Iterable over its
  `instances` map, which holds getInstanceCount() entries created in the WorldMap ctor :31-36). Guarded only by
  `if (!worldMap.isInstanceType())`.
- **spawnInstance** (SpawnEngine.java:187-188): `if (eventTemplate == null) HousingService.getInstance().
  spawnHouses(instance, ownerId);` — called once PER instance, with ownerId==0 at boot (so spawnHouses takes the
  customHouses branch, not spawnStudio).
- **HousingService.spawnHouses** (HousingService.java:149-175): for each HouseAddress on the map,
  `House customHouse = customHouses.get(address.getId());` — **the cache is keyed by address.getId(), NOT by
  instanceId**. First instance: customHouse==null -> `new House(address, instanceId)` (ONE IDFactory objectId,
  House.java:59-60 `this(IDFactory.getInstance().nextId(), ...)`) -> stored in customHouses. Subsequent twin
  instances: `customHouses.get(address.getId())` returns the SAME House -> `customHouse.setPosition(...)` (new
  instance position) -> `SpawnEngine.bringIntoWorld(customHouse)` re-stores the SAME objectId.
- **bringIntoWorld** (SpawnEngine.java:108-114) -> **World.storeObject** (World.java:75-82):
  `allObjects.putIfAbsent(object.getObjectId(), object); if (oldObject != null) throw new
  DuplicateAionObjectException(...)`. **NO `isInWorld(objId)` guard, no try/catch, no per-instance keying.** So on
  instance #2 of a housing map, Java throws DuplicateAionObjectException — identically to C#.
- **WorldMap.getInstanceCount** (WorldMap.java:125-131): `twinCount = twin_count; if (0) ->1; twinCount +=
  beginner_twin_count; return twinCount`.

### Housing-map instanceCount values (game-server/data/static_data/world_maps.xml + housing/houses.xml)
houses.xml carries non-studio addresses on exactly 8 maps. Their world_maps.xml twin config + computed
getInstanceCount():
| map | name | twin_count | beginner_twin_count | instance? | getInstanceCount() | #addresses |
|-----|------|-----------|--------------------|-----------|--------------------|-----------|
| 210040000 | Heiron   | (none) | **3** | no  | **4** | 9 |
| 220040000 | Beluslan | (none) | **3** | no  | **4** | 9 |
| 210050000 | Inggison   | (none) | (none) | no | 1 | (addr present) |
| 220070000 | Gelkmaros  | (none) | (none) | no | 1 | (addr present) |
| 700010000 | Oriel (land)  | (none) | (none) | no | 1 | many |
| 710010000 | Pernon (land) | (none) | (none) | no | 1 | many |
| 720010000 | Oriel (personal)  | — | — | **instance=true** | (skipped by !isInstanceType) | — |
| 730010000 | Pernon (personal) | — | — | **instance=true** | (skipped) | — |

=> **Heiron (210040000) and Beluslan (220040000) are the trigger maps**: getInstanceCount()==4, NOT instance-type,
9 cached Houses each. SpawnAll spawns instance #1 fine (9 Houses, 9 fresh objectIds), then DuplicateAionObjectException
on instance #2. The note in the prior section's (a) claiming Heiron=4 is now CONFIRMED exact, and the trigger is
the `beginner_twin_count="3"` (+1 base) = 4 instances, not `twin_count`.

### C# parity audit (confirms faithful mirror, no divergence to fix)
- HousingService.cs:151-176 SpawnHouses — identical: `customHouses.GetValueOrDefault(address.GetId())` (address-
  keyed cache), create-if-null with `new House(address, instance.GetInstanceId())`, `SetPosition` +
  `SpawnEngine.BringIntoWorld(customHouse)` re-store. 1:1.
- WorldMap.cs:147-153 GetInstanceCount — `twinCount = GetTwinCount(); if 0 ->1; += GetBeginnerTwinCount()`. 1:1.
- World.cs:70-78 StoreObject — `if (!_allObjects.TryAdd(objId, obj)) throw new DuplicateAionObjectException(...)`.
  NO isInWorld guard. 1:1.
- House objectId — `new House(HouseAddress, int)` -> `IDFactory.GetInstance().NextId()` once. 1:1 (no per-twin id).

### Disposition
No code change. This is a faithful reproduction of a Java latent bug. A full-world SpawnEngine.SpawnAll boot would
DuplicateAionObjectException on Heiron/Beluslan instance #2 in BOTH Java and C#. The implication for the DB-backed
full-StartAsync boot (Part 2 below): you CANNOT run an unmodified full SpawnAll over the real world_maps even WITH
a DB — the housing twin-spawn would throw, faithfully, in Java too. **Real Java avoids the crash only because the
live server does not run `spawnAll()` over a world where Heiron/Beluslan got 4 instances created AND houses spawned
into >1 of them in the same boot** — i.e. in the real binary this path is reached but the DuplicateAionObjectException
is a known faithful outcome; mirroring it (let it throw) is correct. Do NOT add an `if(!World.IsInWorld(objId))`
guard — Java has none. If a future DB-backed full-boot test wants to exercise SpawnAll past housing, it must either
(i) restrict the seeded world_maps to non-twin housing maps, or (ii) assert the DuplicateAionObjectException is the
faithful Java behavior — NEVER patch HousingService/World to dedupe.

## SCOPE — DB-backed full-StartAsync bootstrap harness (read-only assessment, 2026-06-16)

GOAL: run a FULL GameServerBootstrapService.StartAsync against the opt-in MySQL container (3307 / aion_gs /
root:aion, gated on AION_GAMESERVER_DB_INTEGRATION=1, the same env switch SystemMailRepositoryDatabaseIntegration
Tests use) and flip on the DB-required wires.

### What the DB unblocks (services that NRE/throw today only because PlayerDAO/etc. return null on no-DB)
- **#2 HousingService block (main:119-123) — the primary DB-gated unblock.** HousingService ctor ->
  RevokeOwnershipOfDeletedPlayers() -> `new HashSet<int>(PlayerDAO.GetUsedIDs())` (HousingService.cs:52;
  PlayerDAO.cs:359). GetUsedIDs() returns null on no-DB (Java identical, no guard) -> ArgumentNullException. WITH
  the DB up, GetUsedIDs returns the real (possibly empty) id array -> ctor completes -> HousingService.GetInstance()
  can be wired at main:119. Also HousesDAO.LoadHouses (HousingService.cs:43) reads the houses table. The prior
  section already CONFIRMED "with the DB up, the full StartAsync boot ran SpawnAll past HousingService" — so the DB
  satisfies the ctor; the remaining blocker past it is the house-twin-spawn (Part 1, faithful — let it throw / seed
  non-twin maps only).
- **PlayerDAO.setAllPlayersOffline() (initUtilityServicesAndConfig)** (PlayerDAO.cs:424) — currently a boot GAP
  (inert with no DB: UPDATE players SET online=0). With the DB it executes and flips the online flag for any
  persisted rows. Cosmetic until real logins persist, but it becomes a real, observable boot step under a populated
  players table.
- **player-offline init / persisted-player-dependent reads** — any boot read keyed off a populated players/
  legions/inventory table (LegionService.GetCachedLegions for PeriodicSaveService's LegionWarehouseSaveTask,
  ServerVariablesDAO for ServerRunTimeSaveTask, BrokerService/AnnouncementService/CommandsAccessService DAO loads)
  goes from "try/catch -> empty + logged" to actually returning seeded rows. None of these BLOCK boot today (all
  DAO-guarded), but with the DB they exercise their real query paths (real DB-fidelity coverage, not just no-DB
  no-op coverage).
- **PeriodicSave task BODIES (main:156)** — already wired + boot-safe (commit 62c408390). With the DB, the
  scheduled LegionWarehouseSaveTask (InventoryDAO.Store + ItemStoneListDAO.Save) and ServerRunTimeSaveTask
  (ServerVariablesDAO.Store "serverLastRun") actually WRITE to the DB instead of try/catch-logging false — turns
  the task-body assertions from "no-throw" into "row persisted".

### What stays DEFERRED even WITH the DB (heavy SPAWN/world-map, NOT DB-gated)
- **#1 SiegeService.initSieges() (main:142)** — needs the full SPAWNS_DATA siege-spawn dir + siege/artifact world
  maps loaded into World so ArtifactSiege.OnSiegeStart -> Siege.InitSiegeBoss finds its boss (else SiegeException
  "Siege Boss not found for siege 1012"). This is a SPAWN-DATA + world-map harness need (scoped in the spawn-data-
  backed harness section below), NOT a DB need. A DB alone does not satisfy it.
- **#5 PvpMapService.init() (main:176)** — needs world map 301220000 in WORLD_MAPS_DATA + spawns keymasters/chests
  (and conflicts with the bootstrap empty-world invariant). SPAWN/world-map need, not DB.
- **The house-twin-spawn (Part 1)** — faithful Java bug; even with the DB, a full SpawnAll over the real
  world_maps throws DuplicateAionObjectException on Heiron/Beluslan instance #2. Not DB-fixable; must seed non-twin
  housing maps or assert-the-throw. NEVER patch.

### What flips green WITH the DB present
- A new DB-gated test (`[Fact]` early-return unless AION_GAMESERVER_DB_INTEGRATION=1, mirroring
  SystemMailRepositoryDatabaseIntegrationTests' InitializeDatabaseFactory/InitializeSchema/Seed pattern) can:
  bring up DatabaseFactory against 3307, run StartAsync with HousingService wired at main:119-123, and assert the
  boot reaches SpawnAll past HousingService (it does — confirmed). To get a CLEAN full-SpawnAll it must seed a
  world_maps subset EXCLUDING the twin housing maps (210040000/220040000) OR expect the faithful
  DuplicateAionObjectException. Everything else (the ~30 already-wired getInstance/init services) is already green
  no-DB.

### Honest frontier assessment
**The autonomous IN-MEMORY work is essentially complete.** All bounded boot-init wires that can run without a DB
or a heavy spawn/world-map load are wired (BOOT-COMPLETENESS CENSUS below: only #1/#2/#5 remain, each at a real
data/DB floor). The three remaining frontiers are ALL user-environment-gated, NOT code gaps:
1. **DB frontier (#2 Housing + setAllPlayersOffline + persisted-player reads)** — requires the 3307 MySQL
   container RUNNING + AION_GAMESERVER_DB_INTEGRATION=1. The C# code is faithful and ready; only the environment
   (a live DB) is missing. This is the SAME frontier as memory three-server-stack-boots' real-client
   login->enter-world test (Front A): both need the populated DB + a running server process, not more porting.
2. **Heavy-spawn/world-map frontier (#1 Siege + #5 Pvp)** — requires the spawn-data-backed harness (scoped below:
   seed real spawns/ + world_maps.xml + NPC_DATA into the test World). Medium effort, IN-MEMORY-doable (no DB), but
   it is a sizeable fixture-data + assert-evolution task, the highest-value remaining bounded in-memory move.
3. **House-twin-spawn** — RESOLVED as faithful (Part 1); no work, just don't patch.

CONCLUSION: there IS one remaining bounded IN-MEMORY-testable move — the spawn-data-backed bootstrap harness
(unblocks #1 + #5 together, scoped in the existing section below). Beyond that, the frontier is genuinely
DB-environment (the 3307 container) and real-client (Front A login->enter-world), both user-environment-gated. The
porting/faithfulness arc has no remaining un-blocked autonomous code work other than that one spawn-harness task.

## RESOLVED — spawn-backed integration test proves NPCs spawn end-to-end (commit pending, 2026-06-16)

Added GameServerBootstrapTests.GameServerBootstrap_RealSpawnDataMaterializesNpcsIntoWorld — the END-TO-END
NPC-spawn proof. It loads the REAL game-server/data + 147MB cache via DataManager.LoadAsync(repoRoot) (same
real-data path as RealStaticDataLoadIntegrationTests), brings up the real boot machinery (DataManager + World
maps + IDFactory + ThreadPool singleton bridges + the AIEngine/ZoneService/GeoService engines the spawn path
needs), then drives the faithful SpawnEngine.SpawnObject path over Sanctum's (110010000) real SPAWNS_DATA spawn
groups and asserts the World store materializes real Npc instances. RESULT: 357 Npc objects (360 spawn calls;
delta = gatherables) materialized into Sanctum, incl. known NPC Euterpe (798173). Before the SPAWNS_DATA fix
this was 0 (hollow SpawnsData singleton). Skips (returns) when the real cache is absent. Per-class GREEN:
build 0, Bootstrap 8/8 (7 minimal + this), Golden 167/167, RealStaticDataLoad 1/1. (Combined-project run still
shows the PRE-EXISTING GoldenStatsInfo DataManager-singleton flake — 2 failures on clean HEAD too, passes
per-class; out of scope, gate per-class per the contract.)

THREE FAITHFULNESS FIXES landed alongside (each a real port bug the spawn path surfaced, not test scaffolding):
1. **GameServerBootstrapService engine-init ORDER** — the engine InitAsync block sat just before
   SpawnEngine.SpawnAll(), i.e. AFTER the location-init cluster's spawning services (VortexService.
   initVortexLocations spawns NPCs). Java (GameServer.main:101-102) inits the engines in PARALLEL right after
   DataManager.getInstance() and BEFORE every spawn path. Moved the C# engine-init up to right after
   DropRegistration-precursor (before the location-init cluster), matching Java. Every spawned Npc resolves its
   AI via AIEngine.NewAI, so the AIEngine MUST be up before any spawn — this was an ordering bug invisible until
   the spawn path ran with real data.
2. **GameServerBootstrapService IDFactory singleton bridge** — StartAsync locked the IDFactory's ids but never
   called IDFactory.RegisterInstance(_idFactory). Every VisibleObject ctor (Npc/Gatherable/...) takes its
   objectId from IDFactory.GetInstance().NextId(), so the FIRST boot spawn NRE'd "IDFactory singleton bridge not
   initialized" — a latent PRODUCTION boot bug (Program.cs didn't bind it either). Now bound right after LockIds,
   mirroring the ThreadPoolManager/World/DataManager bridges. Faithful (Java IDFactory.getInstance() is the
   singleton the spawn path uses).
3. **AIName attribute Inherited=false** — Java @AIName is NOT @Inherited, so AIEngine.getAnnotation(AIName.class)
   returns null for a subclass (SiegeNpcAI extends AggressiveNpcAI does NOT inherit "aggressive"). C# custom
   attributes default to Inherited=true, so GetCustomAttribute<AIName>() on SiegeNpcAI returned the base's
   "aggressive" and double-registered it ("Duplicate AIs with name aggressive"). Set
   [AttributeUsage(..., Inherited = false)] to match Java exactly. Plus a defensive guard in
   OnClassLoadUnloadListener.DoMethodInvoke: the C# ScriptManager scans EVERY loaded assembly (vs Java's source-
   dir scan), so reflecting custom attributes on test-platform methods can raise TypeLoad/FileNotFound for an
   unresolvable attribute type — skip those (never an Aion @OnClassLoad hook; production = game-server assemblies
   only, so behaviour is identical).

### Siege/PvP wire-flip: STILL DEFERRED (Java does NOT guard empty data; full SpawnAll needs a DB) — finding below
- **#1 SiegeService.initSieges() (main:142)** and **#5 PvpMapService.init() (main:176)** were NOT flipped on in
  the shared StartAsync. CONFIRMED via Java source: neither guards empty data. initSieges() guards only
  !SIEGE_ENABLED; its updateFortressNextState() does getSiegeLocation(scheduledLocId).setNextState() with NO null
  guard, and the FULL real siege_schedule.xml schedules SiegeStartRunnables even under the minimal fixture, so
  with empty SIEGE_LOCATION_DATA getSiegeLocation(...) is null -> NRE (Java NPEs identically). PvpMapService.init()
  unconditionally calls InstanceService.getNextAvailableInstance(301220000,...) — needs world map 301220000, absent
  under the minimal fixture. Both run for ALL boots if wired into StartAsync, so flipping them on would break the
  minimal-fixture bootstrap 7/7. Per the hard rule (no un-faithful guard Java lacks), they STAY deferred in
  StartAsync. They can only be wired once the SHARED boot path carries spawn+world+siege data AND a DB.
- **#2 Housing is the real blocker for a full-StartAsync spawn boot.** SpawnEngine.SpawnAll() -> per-instance
  HousingService.SpawnHouses() -> HousingService ctor -> RevokeOwnershipOfDeletedPlayers() ->
  new HashSet<int>(PlayerDAO.GetUsedIDs()); GetUsedIDs() returns null on no-DB (Java NPEs identically, no guard),
  so the full SpawnAll REQUIRES a DB. The opt-in MySQL integration harness (3307, aion_gs, gated on
  AION_GAMESERVER_DB_INTEGRATION=1) DOES satisfy HousingService — verified: with the DB up, the full StartAsync
  boot ran SpawnAll past HousingService. BUT a SECOND whole-world-boot issue then surfaced (see RECOMMENDED NEXT).

RECOMMENDED NEXT (whole-world full-StartAsync spawn boot, DB-backed): two pre-existing whole-world concerns block
a full SpawnAll boot even WITH the DB, and both are FAITHFUL (Java does the same) so they need investigation, not
a quick guard:
  (a) **House double-spawn across twin instances.** SpawnAll iterates worldMap.forEach over ALL getInstanceCount()
      instances (twin_count + beginner_twin_count; e.g. Heiron 210040000 = 4), and HousingService.spawnHouses
      re-uses the SAME address-cached House object per instance -> BringIntoWorld(sameHouse) collides on the House
      objectId in World.StoreObject (DuplicateAionObjectException). Java's worldMap.forEach + spawnHouses is
      identical, so this is either a Java latent bug, or houses-bearing maps actually have twin_count such that
      only one instance carries addresses — confirm against Java/real data before wiring full SpawnAll.
  Once (a) is understood, a DB-backed full-StartAsync boot test + the siege/pvp wire-flip can land together.

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
