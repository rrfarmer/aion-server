# Full-Parity Backlog — "Skip Nothing"

Date: 2026-06-17. Owner doc for **what remains to reach full 1:1 parity**. Authoritative companion to `Port-Fidelity-Remediation-Plan.md` §0 (which records that Phases A–C are COMPLETE and Phase D is structurally ~complete). This doc enumerates *every* remaining gap — large and small — because for parity **nothing may be skipped**.

Branch `feature/object-spine-bigbang` @ `f8a08c00c` · build 0 · golden 196/196 byte-exact · suite 459/0 · bootstrap 9/9 · full DB-backed boot validated.

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
