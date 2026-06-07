# Game-Server Completion Estimate

Date: 2026-06-07
Supersedes: the 2026-05-29 revision (which is preserved only in git history).

## Why This Was Rewritten

The previous revision (2026-05-29) became misleading for two reasons:

1. **Stale.** There have been ~1,173 commits on the `4.8` branch since it was written.
2. **Wrong denominator.** It scored only the 169-file Java `services` surface and reported "≈53% balanced." But `services` is roughly 7% of the 2,324-file Java gameserver, and roughly 4% of the full ~4,056-file gameplay surface once the 1,732 content-handler scripts are included. The largest and hardest parity work lives almost entirely *outside* what that estimate counted.

This revision keeps the original services-only view (it is still useful) but reframes it inside the full gameserver surface and adds a **modeled-vs-live** distinction, which is the single most important correction.

## The Core Distinction: Modeled vs Live

The C# port uses a heavily decomposed **plan-service** pattern. Behavior is first modeled in small, non-live `*PlanService` classes (formulas, packet shapes, guard order) and validated with unit tests; live runtime wiring is wired in selectively and often deferred.

Counts as of this revision (`Aion.GameServer/Services`):

- 733 service files total
- **249 are `*PlanService`** (~34%) — non-live boundaries by design
- 40 `*Runtime*` files
- 5 `*ExecutionService`
- 787 GameServer test files

**Consequence: file-count coverage overstates runtime parity.** An area can have many files and a green test suite while remaining non-live. The clearest example is `vortex`: rated "Partial 30%" in May, it now has 34 files — but they are almost all `*PlanService`/dispatch/composition modeling, with no live invasion lifecycle. Coverage went up ~11×; live parity did not.

Every number below is therefore tagged as **modeled** (planned/tested, not necessarily live) or **live** (wired into the running server) where the distinction matters.

## Full Gameserver Surface (the real denominator)

Java `game-server/src/com/aionemu/gameserver` = 2,324 files. Plus 1,732 content-handler scripts under `game-server/data/handlers` (quests, AI, instances, admin/console commands) = ~4,056 gameplay-relevant Java files.

| Java area | Java files | C# state | Parity |
| --- | ---: | --- | --- |
| `model` | 801 | 89 C# `Model` files | low — ~11% surface, blocks much downstream work |
| `network` | 523 | 398 C# `Network` files | medium-high for packets (see Packet Layer below) |
| `skillengine` | 292 | 0 dedicated files | **none** — no live skill/effect engine |
| `services` | 169 | 733 C# `Services` (decomposed) | mixed — see Services Surface below |
| `dataholders` | 100 | 64 C# `Dataholders` | partial |
| `questEngine` | 79 | ~handful | **near zero** |
| `controllers` | 61 | 2 C# `Controllers` | **none** — no live creature behavior |
| `dao` | 57 | folded into repositories | partial |
| `world` | 41 | 7 C# `World` | low |
| `utils` | 41 | 5 C# `Utils` (+ inline) | partial |
| `configs` | 40 | scattered config classes | partial |
| `ai` | 39 | 0 live (keyword matches were false positives) | **none** |
| `geoEngine` | 29 | 2 | low |
| `taskmanager` | 14 | partial scheduler bridges | low |
| `spawnengine` | 14 | first-pass `SpawnEngine` bridge | low |
| `instance` | 5 (src) + 37 handlers | minimal | **near zero** |
| `data/handlers` content | 1,732 | ~11 reference `QuestHandler`; effectively unported | **near zero** |

The two pillars that gate a playable server — **`skillengine`** (combat/effects) and the **content handlers** (quests/AI/instances) — are effectively absent. `model` (the data backbone for both) is ~11% ported.

## Packet Layer (a genuine strength)

The client-facing protocol surface is well advanced, which is why the login→play path works as far as it does:

- Client→server: **186 of 202** Java `CM_*` packets have C# `Cm*` handlers (~92%)
- Server→client: **191 of 261** Java `SM_*` packets have C# `Sm*` writers (~73%)

Caveat: many of these handlers route into plan-services or intentional no-ops for deferred systems (revive, loot, zone change, channel, etc.), so a ported handler is not the same as a live behavior.

## Services Surface (the original scope, refreshed)

Along the **login → items → housing → social** axis the services surface is genuinely deep now (Phase 6): `CM_ENTER_WORLD` plus ~40 post-enter packets, item actions (enchant, manastone, idian polish, decompose, assemble, extract, AP-extract, remodel, charge), housing auctions/bids/rent, kisk lifecycle, mail/broker, stigma, friends/blocks, chat, movement broadcast, and a large body of combat/reward **formula** services.

The same surface is shallow or absent where it depends on the missing engine: combat resolution, skill effects, NPC AI, quest progression, and the large-area systems below.

### "Not obvious" service areas — still at or near zero

No meaningful progress since May; these remain the same gap:

| Area | Java weight | C# state |
| --- | ---: | --- |
| `siege` | 14 | 0 service files; 22 incidental mentions only (PvP-zone/AP/mail) |
| `panesterra` | 4 | 0 |
| `transfers` | 4 | 1 file |
| `conquerorAndProtectorSystem` | 3 | 0–2 mentions |
| `ban` | 3 | 3 files (partial) |
| `worldraid` | 2 | 0 |
| `event` | 3 | event-drop support only |

## Revised Parity Estimate

Two honest numbers for two different questions:

- **Services surface only (the docs' historical scope):** higher than May's ~53% along the login/items/housing axis — but *live* parity within it is materially lower than file counts imply because ~34% of services are plan-only and the combat-dependent slices are non-live.
- **Full gameplay parity (engine + content + model):** realistically **~15–25%**. `skillengine` and the 1,732 content handlers are effectively untouched and `model` is ~11% ported.

Treat both as bands, not points. The full-gameplay band is the one that matters for "is this a playable server."

## Architectural Risks

- **`GameServerConnection.cs` is 22,907 lines** — a god-class holding the bulk of packet handling. Maintainability and correctness risk that grows with every new handler.
- **Build is green (0 errors)** but carries nullable-reference warnings (13 in GameServer at this revision).
- **No real-client validation yet** — explicitly deferred. Nothing in the plan-service or live surface has been confirmed against a live client.
- **Plan-service sprawl** — extreme decomposition (e.g. multi-level `BindPointTeleportKinah*` chains) raises the cost of promoting modeled behavior to live and of reasoning about ordering/side-effects.

## What's Left, In Dependency Order

1. **Live creature/runtime layer** — controllers (61→2), KnownList persistence, scheduler/task model, movement→combat wiring. Prerequisite for everything below.
2. **SkillEngine** (292 files) — effect templates, abnormal effects, skill application. No live combat without it.
3. **AI** (39 src + 43 scripts + per-area) — NPC behavior.
4. **questEngine + ~1,153 quest scripts** — the single largest content body.
5. **`model` layer** (801→89) — backbone that blocks 1–4.
6. **Instances** (5 src + 37 handlers) and the large-area systems still at zero: **siege, panesterra, worldraid, transfers, conqueror/protector**.
7. **Promote the ~249 plan-services from modeled to live**, then validate against a real client.

## Per-Area Docs

The per-service and per-package docs under `packages/` and `top-level/` are individually still dated 2026-05-29. Their high-level statuses are directionally usable but their completion language predates this revision. Refresh them opportunistically; this document is the authoritative summary.
