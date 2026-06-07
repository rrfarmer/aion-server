# Port Fidelity & Remediation Plan

Date: 2026-06-07
Status: AUTHORITATIVE. This is the current loop driver for all game-server porting work. It supersedes the Phase-6 unit-of-work approach for *how* work is done (the phase roadmap in `csharp-port.md` still describes the macro sequence).

---

## 1. Mission (North Star)

Convert the legacy Java game server to C# **1:1**. The Java implementation is the **absolute source of truth** — it has worked in production for years. The C# port must reproduce Java behavior exactly, with **no invented abstractions** ("no slop").

The single rule that makes this concrete:

> **The C# structure mirrors the Java structure.** One Java file → one C# file. One Java class → one C# class. One Java method → one C# method. One Java packet → one C# handler. Nothing more.

This rule simultaneously resolves both architectural problems we have today (see §2).

---

## 2. Diagnosis: why this plan exists

A June 2026 parity re-evaluation found the port had drifted into two **opposite** anti-patterns that share one root cause — *the C# structure stopped mirroring the Java structure.*

**Anti-pattern A — plan-service sprawl (too many files).** Simple Java services were exploded into dozens of micro "services" with invented vocabulary (`Plan`, `Bridge`, `Adapter`, `Composition`, `Outcome`, `Integration`, `Owner`, `Policy`, `Executor`). Measured:

| Java source | Java size | C# result | Explosion |
| --- | --- | --- | --- |
| `services/teleport/BindPointTeleportService.java` | 1 file, 148 lines | 38 files, 4,921 lines, 38 test files | ~33× lines |
| `services/findgroup/` | 2 files, 453 lines | 101 files, 19,914 lines, 103 test files | ~44× lines |

Across `Aion.GameServer/Services`, **~249 of 733 files are non-live `*PlanService`**.

**Anti-pattern B — the god-class (too few files).** `GameServerConnection.cs` is **22,907 lines**, fusing packet handlers that Java keeps as ~200 individual `CM_*`/`SM_*` classes.

**The crucial nuance:** the *behavior* in the existing work is largely **faithful** — formulas are traced to Java, tests assert Java-derived values, files carry `Java parity:`/`JavaSource` breadcrumbs. The problem is **structure, not logic**. That is why remediation is "re-port fresh from the Java" (cheap, because the logic is simple and already understood) rather than "redesign behavior."

**Why the rules didn't prevent this:** `parity-verification.md` *already* states the correct doctrine ("planner-only tests … do not move the C# server toward replacement … treat such artifacts as scaffolding only, not Java parity completion"). The doctrine was right; **adherence and enforcement failed.** This plan therefore adds *mechanical guardrails* (§5), not just principles.

---

## 3. The Fidelity Doctrine (hard constraints)

Every unit of work must satisfy all of these. A reviewer/agent may reject work that violates any.

1. **File mapping.** One Java file → one C# file. Mirror the Java package tree as the C# folder/namespace tree (`services/teleport/BindPointTeleportService.java` → `Services/Teleport/BindPointTeleportService.cs`).
2. **Member mapping.** Java class → C# class; Java method → C# method, same names (idiomatic casing), same order, same guard order, same early-returns.
3. **No abstraction without a Java counterpart.** If the Java has no `Plan`/`Bridge`/`Adapter`/`Composition`/`Outcome`/`Integration`/`Owner`/`Policy`/`Executor`/`Projection`/`Snapshot`/`Preview`/`Fact` type, the C# must not introduce one. A private method in Java stays a private method in C# — it does **not** become its own service+test.
4. **Packets.** One C# handler/writer class per Java `CM_*`/`SM_*` class, under a namespace mirroring Java's network tree. **No packet-handling logic in `GameServerConnection.cs`.**
5. **Minimal idiom translation only.** `ThreadPoolManager.schedule` → the C# scheduler equivalent; Java statics → C# statics; `Map` → `Dictionary`. Translate the mechanism, never restructure the design.
6. **Breadcrumbs.** Keep a short `// Java parity: path::method` comment on each ported method.
7. **Done means live-or-deferred, never planner-only.** A unit is "done" only if it is wired into live runtime behavior, **or** it is explicitly marked deferred with a Java-referenced reason and a tracked follow-up. "Modeled + tested" is scaffolding, not completion.

---

## 4. Validation strategy (Java as oracle, no live client)

We cannot use a live game client until the port is complete, so we validate against the **Java implementation directly**. Two mechanisms, layered by where they fit (decision: golden for packets+formulas, audit for glue):

**4.1 Golden / differential tests (gold standard — packets & pure formulas).**
- The Java server compiles and runs. `SM_*.writeImpl(ByteBuffer)` emits exact bytes; formula methods are pure functions.
- Build a Java capture harness (§5, A2) that, for chosen inputs, dumps **real Java output** (packet byte arrays, formula return values) to fixture files.
- The C# test asserts its output **equals the captured Java bytes/values** byte-for-byte. This converts "author's reading of Java" into "provably equals Java."

**4.2 Structural fidelity audit (mechanical — every unit).**
- A tool (§5, A3) maps every Java file/class/method to its C# counterpart and reports: faithful, orphan (C# with no Java parent = slop), or gap (Java with no C# child = missing).

**4.3 Glue / control-flow (audit + Java-reading).**
- For logic that's hard to harness (handler orchestration, scheduling order), require the C# to cite the Java method and reproduce its statement/guard order; reviewer checks against Java line-by-line.

**Definition of "validated parity"** (binding, per `parity-verification.md`): live runtime behavior that is golden-matched or line-by-line audited against Java. Readiness reports, previews, and planner-only tests are **not** parity.

---

## 5. Phase A — Foundation (one-time, do first)

- **A1. Doctrine.** This document. (Done.)
- **A2. Java golden-capture harness.** A small Java entry point/test set under the Java tree (e.g. `game-server/test` or a `tools/parity-capture` module) that constructs packets/formula inputs and writes fixtures to a shared `parity-artifacts/golden/` directory. Start with: `SM_*` packet bytes for the already-ported packets, and the `StatFunctions`/formula methods already modeled in C#.
- **A3. Structural-audit tool.** A script (C# or Python) that walks `game-server/src/.../gameserver` and `dotnetConversion/src/Aion.GameServer`, builds the Java↔C# file/class map, and emits a scorecard (faithful / orphan / gap). Output committed to `docs/` or `parity-artifacts/`.
- **A4. Guardrails (enforcement).** A CI/check step that fails when a new C# file under `Services/` uses banned slop vocabulary without a matching Java type, or when a file has no resolvable Java parent. This is what was missing.

---

## 6. Phase B — Audit existing work (validation)

Run A3 + A4 across the whole game-server. Produce a per-Java-file **backlog scorecard** classifying every area as:
- **faithful** — keep as-is (spot-check with golden tests).
- **slop-cluster** — over-decomposed; queue for re-port (§7).
- **god-fragment** — logic living in `GameServerConnection.cs`; queue for extraction (§7).
- **missing** — no C# counterpart; queue for porting (§8).

This scorecard *is* the loop backlog. Seed it in dependency order; the worst known clusters today are: `findgroup` (101 files), `PlayerProtection` (42), `WorldNpc` (37), `BindPoint` (38), `PlayerKnown` (24), plus `GameServerConnection.cs` (god-class).

---

## 7. Phase C — Remediate (re-port fresh; the loop)

For each backlog item (one Java file or one tight Java cluster per iteration):

1. **Select** the next Java file (smallest, in dependency order).
2. **Read the Java source fully** — it is the spec.
3. **Harvest** expected values from the existing C# tests/plan-services (they encode Java-derived values) and, where possible, regenerate them from the A2 harness as golden fixtures.
4. **Write ONE C# file** mirroring the Java class 1:1 (Doctrine §3).
5. **Write golden/diff tests** anchored to Java output (§4).
6. **Delete the slop cluster** the new file replaces (and its now-redundant tests).
7. **Build + test green**, structural-audit clean. **Commit** with a `[Fidelity]` tag and the Java path.

**God-class extraction** uses the same loop, one packet at a time: move one `CM_*`/`SM_*`'s logic out of `GameServerConnection.cs` into its own handler class mirroring the Java packet class, leaving only thin dispatch behind. Stop when `GameServerConnection.cs` is dispatch-only.

---

## 8. Phase D — Resume porting (only after C is underway)

Port the still-missing surface in Java **dependency order**, each unit through the §7 loop and §4 validation:

1. `model` (801→89) — the data backbone that blocks everything.
2. `controllers` + runtime (KnownList, scheduler, world tick) — live creature behavior.
3. `skillengine` (292→0) — effects/skills → live combat.
4. `ai` (39 + scripts).
5. `questEngine` + quest scripts.
6. `data/handlers` content (instances, commands).
7. Remaining absent services: `siege`, `panesterra`, `worldraid`, `transfers`, `conquerorAndProtectorSystem`.

---

## 9. Agent loop protocol (the handoff)

An agent running this plan repeats:

```
1. Read this doc + the backlog scorecard (§6).
2. Pick the single next unit (one Java file). Prefer Phase C over D until C is clear.
3. Apply the §7 loop. Honor the Doctrine (§3) and Validation (§4) exactly.
4. Definition of done (ALL required):
   - C# mirrors Java 1:1 (file/class/method).
   - Golden or line-audited against Java; no planner-only "done".
   - Slop it replaces is deleted; structural audit clean for the area.
   - Build green; tests green; committed with [Fidelity] + Java path.
5. If a unit needs an unported dependency, record the dependency and pick the dependency instead (don't stub with a new abstraction).
6. Stop / escalate if: Java behavior is ambiguous, a unit can't be golden-validated, or the doctrine would have to be broken to proceed.
```

Commit message format: `[Fidelity] <Java path::class> — re-port 1:1 (was <N> slop files)`.

---

## 10. Definition of project done

Every file under `game-server/src/com/aionemu/gameserver` (and the `data/handlers` content) has **exactly one faithful C# counterpart**, golden-validated or explicitly deferred with a Java-referenced reason — and the C# server reproduces Java behavior with no abstraction that Java does not have.

---

## 11. Related docs (keepers)

- `parity-verification.md` — parity definitions; the doctrine this plan enforces.
- `orchestration-rules.md`, `parallelization-strategy.md` — agent/work-ownership process.
- `csharp-port.md` — macro phase roadmap (phases 0–8).
- `PHASE-*-COMPLETION.md` / `PHASE-*-PROGRESS.md` — record of work completed.
- `docs/discovery/game-server-services/Completion-Estimate.md` — full-surface parity picture and the modeled-vs-live framing.
- `Phase-6-Session-2822-Handoff.md` — last Phase-6 session handoff (retained for continuity; superseded by this plan).
