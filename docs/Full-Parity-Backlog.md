# Full-Parity Backlog — "Skip Nothing"

Date: 2026-06-17. Owner doc for **what remains to reach full 1:1 parity**. Authoritative companion to `Port-Fidelity-Remediation-Plan.md` §0 (which records that Phases A–C are COMPLETE and Phase D is structurally ~complete). This doc enumerates *every* remaining gap — large and small — because for parity **nothing may be skipped**.

Branch `feature/object-spine-bigbang` @ `695f5be6f`+batch13 · build 0 · golden 213 test methods byte-exact (0 fidelity bugs) · suite 476/0 · bootstrap 9/9 · full DB-backed boot validated.

## Standing reality (so the backlog isn't misread)

- **De-slop + structural port is DONE.** 126→0 reworked duplicate packets, `GameServerPacket` dropped, object store unified to single `World._allObjects`, all reworked `*Summary`/`*Table`/WorldNpc/Kisk/Rift/Housing slop retired, `GameServerConnection` god-class gone, guardrail baseline empty (0/0). ~92% of Java types (2269/2456) have an exact-name C# file; content scripts 100% (quests 1035 · AI 462 · instance 37 · zone 3 · chat).
- **A faithful Java TODO is parity, not a gap.** Of 277 `TODO`/`FIXME` markers in C# src, the large majority are verbatim copies of TODOs in the Java source (e.g. "most retail npcs lose 364 hate. TODO: find formula"). The doctrine says mirror them; reproducing them *is* 1:1. Only the **C#-specific** subset (config framework, "port the rest") are real gaps — see §C.
- **The only thing that cannot be done here is the live-client test (§F)** — it needs the user's Aion 4.8 client. Everything else in this backlog is workable now.

---

## A. Runtime-parity VALIDATION depth — the #1 driver (bounded, provable without a client)

Structural 1:1 ≠ proven runtime parity. Absent the live client, the Java-oracle golden suite is how we *prove* parity. It is at 196 cases, **0 fidelity bugs across every probe** — strong, but not exhaustive.

- **A1. Un-golden'd `SM_*` packets: ~103 of 240.** 157 packet fixtures cover ~137 distinct faithful packets. The remaining ~103 read *live objects* (Player legion/group/store/flight graph, live World/WorldMapInstance, Summon/Pet/Skill graphs) and need a reusable **integration harness** (uninitialized-object Player/World/connection seams — precedents exist: `PacketHarnessCreature`, scalar/`HarnessPlayer`, DataManager-holder seam, real-`Npc` seam, item/`ItemInfoBlob` seam, GameCrypt Write seam). Build the harness increment, then golden ~1–3 packets/tick, byte-exact vs the mvn oracle. **Largest single remaining provable-parity vein.**
- **A2. Formula golden expansion.** 30 formula fixtures; remaining pure `StatFunctions`/`StatCapUtil`/calc methods that read only args are still capturable. Thin but non-zero.
- **A3. Behavioral/runtime golden gaps** the unit harness can't reach (Rnd-based combat, time-dependent) — document as not-golden-able; they fall to §F (live client) for ultimate proof.

### A1 per-packet triage (computed 2026-06-17, the authoritative un-golden'd list)

Ground truth from disk: **240** faithful `SM_*.cs`; **169** distinct names covered by a golden fixture (`parity-artifacts/golden/packets/*.json`) or a dedicated faithful golden/byte test (`SmFindGroupTests`, `SmLegionDominionRankTests`, `SmLegionHistoryTests`, the `Golden*FixtureTests` families). NOTE: the earlier reconciliation note guessing SM_GAME_TIME/SM_AUTO_GROUP already had dedicated golden tests was WRONG — they do NOT; only SM_FIND_GROUP/SM_LEGION_DOMINION_RANK/SM_LEGION_HISTORY do (correctly excluded below). **Un-golden'd = 80** (batch 13 golden'd SM_TUNE_RESULT/SM_SKILL_LIST/SM_WAREHOUSE_ADD_ITEM/SM_INVENTORY_INFO; golden suite 209->213 test methods, +6 cases byte-exact, 0 fidelity bugs).

Triage key: **T1** = TIER-1 runtime-golden reachable now (writeImpl is `con`-null-safe under `CaptureWriteImplPayload` and reads only scalars / simple DTOs / an existing-or-small-bounded seam); **T1-seam** = reachable via an existing heavy seam (item/ItemInfoBlob, real-Npc, DataManager-holder) — bounded but more work; **T2** = TIER-2 audit-only (writeImpl reads `con.getActivePlayer()`/`con.getAccount()`/`con.enableCryptKey()`, the Java static-final `World` singleton, a live `SiegeService`/`Influence`/`GameTimeService`/`LoginServer`/`GameServer` singleton, `System.currentTimeMillis`/JVM-uptime, or a full live Player/Legion/Group/Alliance graph the unit harness cannot bounded-build) — validate by line-by-line writeImpl audit + breadcrumb.

**T1 — pure scalar / simple-DTO, con-null-safe (golden these first):**
- `SM_CHARACTER_SELECT` — scalar type/messageType/wrongCount + `SecurityConfig.PASSKEY_WRONG_MAXCOUNT` const. **[DONE 2026-06-17 batch 10]**
- `SM_AFTER_SIEGE_LOCINFO_475` — pure const writeImpl (writeH 0 / writeC 0), default ctor. **[DONE batch 10]**
- `SM_NEARBY_QUESTS` — `Map<Integer,Integer>`; `-size&0xFFFF` + per-entry `questId|bit`. **[DONE batch 10]**
- `SM_MACRO_LIST` — playerObjectId + `List<Macros.Macro>` (public record) + clearList; `writeH(-size)`. **[DONE batch 10]**
- `SM_QUEST_COMPLETED_LIST` — updateMode + `List<QuestState>` (constructible ctor). **[DONE batch 10]**
- `SM_FIRST_SHOW_DECOMPOSABLE` / `SM_SECONDARY_SHOW_DECOMPOSABLE` — objectId + `Collection<ResultedItem>` (XML DTO; reflect-set itemId/minCount both sides). **T1, next batch.**
- `SM_ABYSS_RANKING_LEGIONS` / `SM_ABYSS_RANKING_PLAYERS` — `List<RankingListLegion/Player>` DTO ctor. **[DONE 2026-06-17 batch 11]** (records con-null-safe; byte-exact first capture.)
- `SM_GM_SHOW_PLAYER_SKILLS` — `List<PlayerSkillEntry>` DTO. **[DONE batch 11]** (PlayerSkillEntry(skillId,skillLvl,skillType,state) ctor; only non-normal entries used so getFlag()'s System-time path never fires -> deterministic.)
- `SM_GM_SHOW_LEGION_MEMBERLIST` — `List<LegionMember>` DTO. **[AUDITED 2026-06-17 batch 11 -> demoted T2]** writeLegionMember calls `PlayerService.getOrLoadPlayerCommonData` (live singleton/DB) -> audit-only breadcrumb, 1:1 confirmed.
- `SM_TOWNS_LIST` — `Map<Integer,Town>`; Town DTO. **[AUDITED 2026-06-17 batch 11 -> demoted T2]** Town ctor (faithful 1:1) runs `spawnNewObjects()` (DataManager+SpawnEngine) + `GeoService.updateTown` -> not bounded-constructible -> audit-only breadcrumb, 1:1 confirmed.
- `SM_MACRO_LIST` (paged) covered above.

**T1-seam — reachable via an existing heavy seam (item/ItemInfoBlob, real-Npc, DataManager-holder), bounded:**
- item/ItemInfoBlob seam: `SM_TUNE_RESULT`, `SM_INVENTORY_INFO`, `SM_WAREHOUSE_ADD_ITEM`, `SM_PRIVATE_STORE`, `SM_MAIL_SERVICE`, `SM_BROKER_SERVICE`, `SM_UPDATE_PLAYER_APPEARANCE` (several also touch `con.getActivePlayer` -> verify per-packet, may demote to T2). **[DONE 2026-06-17 batch 12]** `SM_EXCHANGE_ADD_ITEM`, `SM_WAREHOUSE_INFO`, `SM_WAREHOUSE_UPDATE_ITEM`, `SM_REPURCHASE` runtime-golden'd via the GENERAL_INFO simple-item seam (player null = blob owner only; `SM_REPURCHASE` allocated uninitialized to dodge the `RepurchaseService` singleton in its ctor, fields reflectively pinned); byte-exact first capture, 0 fidelity bugs. **[verified -> T2]** `SM_LOOT_ITEMLIST` reads `con.getActivePlayer()` -> moved to T2 audit (below). **[DONE 2026-06-17 batch 13]** `SM_TUNE_RESULT` (BuildSimpleItem + PendingTuneResult DTO -> the already-validated EnchantInfoBlobEntry.WriteInfo ENCHANT_INFO writer; con-null-safe, no player), `SM_WAREHOUSE_ADD_ITEM` (item seam, player==blob owner only -> null; writeImpl reads only addType.GetMask() scalar), `SM_INVENTORY_INFO` (NEW player-scalar seam: uninitialized Player + pinned playerAccountData->PlayerCommonData carrying npc/quest/itemExpands; GetFullBlob player==blob owner) all runtime-golden'd byte-exact first capture, 0 fidelity bugs.
- real-Npc seam: `SM_GATHERABLE_INFO`, `SM_NPC_ASSEMBLER`, `SM_TRADELIST` (TRADELIST also reads getLegion/GOODSLIST_DATA -> heavier).
- DataManager-holder seam: `SM_SKILL_LIST` **[DONE 2026-06-17 batch 13]** (silent ctor is DataManager-FREE — only the (skill,messageId) ctor reads SKILL_DATA; stigma entries skillType>0 -> IsNormalSkill() false -> GetFlag()==0 no clock -> deterministic; SKILL_DATA holder seam not even needed).
- new bounded Pet/Summon seam: `SM_PET_EMOTE`, `SM_SUMMON_PANEL`, `SM_SUMMON_UPDATE`.
- model-graph (Kisk/House — bounded model objects, no live World): `SM_KISK_UPDATE`, `SM_HOUSE_RENDER`, `SM_HOUSE_UPDATE`, `SM_HOUSE_OBJECTS`, `SM_HOUSE_SCRIPTS` (per-packet verify; `SM_HOUSE_OBJECT` reads con -> T2).

**T2 — audit-only (writeImpl needs con / live World / live singleton / full live-Player graph / time):**
- reads `con.getActivePlayer()`: `SM_PRICES`, `SM_PLAY_MOVIE`, `SM_DIALOG_WINDOW`, `SM_FRIEND_UPDATE`, `SM_CHALLENGE_LIST`, `SM_GROUP_INFO` **[AUDITED 1:1 2026-06-17]**, `SM_ALLIANCE_INFO` **[AUDITED 1:1 2026-06-17]**, `SM_LOOT_ITEMLIST` (writeImpl `con.getActivePlayer()` null-guard + per-drop loot-confirmation read — full live DropNpc/Player graph), `SM_HOUSE_OBJECT`, `SM_HOUSE_REGISTRY`, `SM_HOUSE_BIDS`, `SM_HOUSE_EDIT`, `SM_INSTANCE_INFO`, `SM_SIEGE_LOCATION_INFO`.
- reads `con.getAccount()` / `con.enableCryptKey()`: `SM_ACCOUNT_PROPERTIES`, `SM_KEY`.
- live World singleton (`World.getInstance()` / static-final): `SM_PLAYER_SPAWN`, `SM_DIE`.
- live `SiegeService`/`Influence`/`GameTimeService`/`TownService`/`GameServer`/`LoginServer` singleton or `System.currentTimeMillis`/JVM-uptime: `SM_GAME_TIME`, `SM_TIME_CHECK`, `SM_VERSION_CHECK`, `SM_INFLUENCE_RATIO`, `SM_FORTRESS_STATUS`, `SM_LEGION_DOMINION_LOC_INFO`, `SM_IN_GAME_SHOP_CATEGORY_LIST`, `SM_IN_GAME_SHOP_ITEM`, `SM_IN_GAME_SHOP_LIST`, `SM_ITEM_COOLDOWN`, `SM_RECIPE_COOLDOWN`.
- full live-Player/Legion/Group/Alliance graph: `SM_GM_SHOW_PLAYER_STATUS` **[AUDITED 1:1 2026-06-17]**, `SM_CHAT_WINDOW`, `SM_GROUP_MEMBER_INFO`, `SM_ALLIANCE_MEMBER_INFO`, `SM_LEGION_INFO`, `SM_LEGION_MEMBERLIST`, `SM_LEGION_ADD_MEMBER`, `SM_LEGION_UPDATE_MEMBER`, `SM_GM_SHOW_LEGION_INFO`, `SM_PRIVATE_STORE_NAME`, `SM_CASTSPELL_RESULT` (Skill/Effect graph), `SM_GATHER_UPDATE`, `SM_OBJECT_USE_UPDATE` (HouseObject), `SM_HOUSE_OWNER_INFO`, `SM_HOUSE_REGISTRY`, `SM_HOUSE_EDIT`.
- login/account/character graph: `SM_CHARACTER_LIST`, `SM_CHARACTER_SELECT`(done T1), `SM_CREATE_CHARACTER`, `SM_L2AUTH_LOGIN_CHECK`, `SM_VERSION_CHECK`.
- builder / no reachable plain-value public ctor: `SM_CUSTOM_PACKET`, `SM_LOGIN_QUEUE`.
- `SM_SYSTEM_MESSAGE` — generated 28k-line catalog + ChatType/sender graph; audit-only.
- abyss/fortress/instance status singleton readers: `SM_ABYSS_ARTIFACT_INFO3` (Collection ctor is T1 with a siege-template seam; `int loc` ctor reads SiegeService -> T2), `SM_FORTRESS_STATUS`, `SM_INSTANCE_SCORE`, `SM_ABYSS_RANKING_*` writeImpl is DTO-only (T1) — keep in T1.

(The above is a living split; packets move T2->T1 as bounded seams are built and T1->done as golden'd. Re-derive the un-golden'd set with: build the covered-name set from `parity-artifacts/golden/packets/*.json` + every `SM_*` token in `tests/.../Golden*.cs` + `Sm*Tests.cs`, diff against `find src -path '*ServerPackets/SM_*.cs'`.)

Recipe (proven): add `capture(...)` to the Java `Golden*FixtureGeneratorTest`, run `mvn -pl game-server -am test -Dtest=<Gen> -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false`, add C# `[InlineData]`+Reconstruct+`CaptureWriteImplPayload`; **Java oracle bytes are truth, never tune to C#**; any mismatch = a real fidelity bug to fix in the C# packet.

---

## B. Explicit `NotImplementedException` / `NotSupportedException` — triage all 14 files

Each is either a faithful guard (matches a Java `abstract`/`throw`/unsupported-op — keep) or a real unported branch (port 1:1). Read each against Java; port the real ones.

- **Likely REAL gaps (port 1:1):** `Model/Team/Legion/LegionWarehouse.cs`, `Model/Items/Storage/LegionStorageProxy.cs` (legion warehouse storage ops).
- **Likely FAITHFUL guards (verify vs Java, then leave + breadcrumb):** `GeoEngine/Collision/UnsupportedCollisionException.cs` (it IS a Java exception type), `GeoEngine/Scene/Node.cs`+`DespawnableNode.cs`, `GeoEngine/Math/FastMath.cs` (unsupported-op throw), `Instance/Handlers/GeneralInstanceHandler.cs`+`Services/Instance/InstanceService.cs` (abstract default-throw), `SkillEngine/Effect/EffectTemplate.cs`, `Network/Aion/Iteminfo/ItemInfoBlob.cs`, `Services/SiegeService.cs`, `Commons/Network/AcceptDispatcherImpl.cs`, `Taskmanager/Tasks/MoveTaskManager.cs`, `Handlers/Quest/pandaemonium/_2900NoEscapingDestiny.cs`.
- Action: 1 tick to read+classify all 14; port the (likely 2–4) real ones depth-first, all-green-or-revert.

---

## C. C#-specific deferrals (the *real* subset of the 277 TODOs)

These are gaps because Java does NOT have them — they're C#-side "wire later" notes. (Hard contract: `.properties` keys + override precedence must be preserved — so the config one matters for parity.)

- **C1. Config framework (systemic).** Configs currently load from `@Property` default values baked into C# field initializers, NOT the faithful Java path = `.properties` file load + `ConfigurableProcessor` reflection + active-event overrides. Markers: `Configs/Config.cs:27` ("wire ConfigurableProcessor reflection over loadProperties() + active-event overrides"), `Configs/Main/WorldConfig.cs:5` ("load from game server properties file via config framework when ported"). Until wired, server behavior is correct only at default config and ignores operator `.properties` overrides — a real parity gap. Scope: port `commons` `ConfigurableProcessor` + properties loader, wire each config class's `@Property` keys 1:1.
- **C2. The ~13 "TODO … Java" / "port from Java the rest" markers.** Enumerate (`grep -rIn "TODO" … | grep -i java`) and resolve each as a 1:1 port; these are explicit C#-deferred porting, not Java mirrors.
- **C3. Triage the rest of the 277.** Mechanically separate faithful-Java-mirror TODOs (keep verbatim — they are parity) from any remaining C#-specific deferrals; only the latter are backlog. Produce the split once so the count is honest.

---

## D. Editor / admin-save & DAO write-path deferrals (small, real)

- **D1. `SpawnsData` 4 editor/admin-save helper TODOs** (non-load-bearing spawn-save; faithful read path is complete).
- **D2. Any DAO/editor `// TODO` save/persist paths** surfaced in §C3 triage (e.g. admin-tool persistence) — port 1:1.

---

## E. Hygiene / tooling (small, keeps the audit honest)

- **E1. Delete 2 stray slop-named Java-tree files** if confirmed dead: `FindGroupMutationPostTraceCaptureHooks`, `PetFeedUnusualStorageArtifactCapture` (slop-vocabulary names that shouldn't be in `game-server/src`).
- **E2. Audit-tool hygiene.** Teach `scripts/parity/structural_audit.py` + `check_fidelity.py` to index `game-server/data/handlers/` as Java, so the ~1,534 faithfully-ported content scripts stop showing as "orphan" and the banned-vocab heuristic stops false-flagging legit Java names (`OphidanBridgeInstance`, `_4217TheImprisonedExecutor`, `_21217NewResearchPlan`, `ScaldingExecutorAI`, `WaveEventExecutorAI`). Until then those 6 guardrail "failures" are known false positives.
- **E3. Resolve the 6 name-mismatch "missing" engine classes** (all currently false positives): confirm `AIState`/`AISubState`/`AIEventType` are faithful nested enums, `CronExpressionTransformer`→`CronExpressions` is a faithful rename, and the 2 §E1 files. Net engine/service missing-surface = 0 after this.

---

## F. [USER-GATED] Live-client enter-world test — the ultimate parity proof

The one test that exercises the *real* end-to-end loop (client → login → enter-world → gameplay). **Environment-gated: needs the user's actual Aion 4.8 client.** The 3-server stack boots (LS/GS/CS authed, GS↔LS bridge stable); the only missing step is the real client connection. This is the gate on declaring *live* parity (vs structural+golden parity). Not autonomously doable.

---

## G. Deferred-data / runtime-load completeness — verify

Reconcile `datamanager-data-placeholders` (noted QUEST_DATA + 4 holders once wired EMPTY for compile-convergence) against `hollow-holder-census` (later: all 96 accessors delegate to loaded `SD.*Dh`). Confirm by boot-assert that every DataManager holder a live consumer reads is non-empty at boot; wire any still-empty placeholder. Believed DONE (RealStaticDataLoad + bootstrap tests assert real loads), but verify exhaustively since "skip nothing."

---

## Recommended order (no client; skip nothing)

1. **A1 integration harness + golden the ~103 live-object packets** — the main provable-parity vein; build the harness once, then grind packets (also re-confirms every live-object writer byte-exact, likely surfacing any remaining latent bugs as the 3 already found did).
2. **B triage + port the real NotImplemented (Legion warehouse) gaps** — 1–2 ticks.
3. **C1 config framework** — systemic; restores `.properties` override fidelity (a hard contract).
4. **G verify holders; D editor-save TODOs; E hygiene + audit-tool** — small, interleave.
5. **C2/C3 TODO triage** — produce the honest faithful-vs-real split, resolve the reals.
6. **A2 formula golden** — thin top-up.
7. **F live-client test** — when the user can run their client. Ultimate parity sign-off.

When A–E + G are clear and F passes, the §10 "project done" definition is met: every Java file has one faithful golden-validated-or-deferred C# counterpart, and the live client proves the runtime loop.
