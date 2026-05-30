# Game-Server Services Completion Estimate

Date: 2026-05-29

## Purpose

This document answers two discovery questions for the Java game-server service surface:

1. what is most likely still missing or not yet discoverable in C#
2. what percentage of this specific service surface appears complete enough to count as ported or mostly ported

This is not a whole-project estimate. It applies only to the Java service surface under `game-server/src/com/aionemu/gameserver/services`.

## Counting Method

The discovery set covers:

- 59 Java top-level service files
- 25 Java service subpackages
- 168 weighted Java service files total

Weighted totals count each Java package by its Java file count and each top-level service as 1.

## Status Totals

### By Area Count

- `Present`: 7 areas
- `Refactored`: 17 areas
- `Partial`: 26 areas
- `Not obvious`: 34 areas

### By Weighted Java File Count

- `Present`: 11 of 168 weighted files
- `Refactored`: 48 of 168 weighted files
- `Partial`: 49 of 168 weighted files
- `Not obvious`: 60 of 168 weighted files

## What This Means

### Clearly Accounted For

If `Present` and `Refactored` count as meaningfully represented in C#, the currently discoverable coverage is:

- 59 of 168 weighted files
- 35.1%

This is the hard floor for "obviously represented" service coverage.

### Accounted For Including Partial Areas

If `Partial` areas are counted as having real but incomplete C# ownership, the currently discoverable coverage is:

- 108 of 168 weighted files
- 64.3%

This is the upper practical bound from the current discovery pass without assuming hidden ownership.

### Weighted Estimate Band

Three weighting models were applied:

- Conservative: `Present=100%`, `Refactored=80%`, `Partial=45%`, `Not obvious=0%` -> `42.5%`
- Balanced: `Present=100%`, `Refactored=90%`, `Partial=60%`, `Not obvious=10%` -> `53.3%`
- Optimistic: `Present=100%`, `Refactored=95%`, `Partial=70%`, `Not obvious=20%` -> `61.3%`

## Recommended Working Estimate

For the Java `gameserver/services` surface only, the current discovery supports a working estimate of:

- roughly `50%` to `65%` complete
- roughly `35%` to `50%` still missing, unverified, or hidden behind renamed ownership

The balanced estimate is `53.3%`.

This is materially lower than an `87%` whole-project intuition because this score is intentionally narrow and harsh:

- it only evaluates the Java service surface
- it treats missing discoverability as risk
- it does not give automatic credit for behavior that might live elsewhere in the C# project until that ownership is demonstrated

Recent ownership tracing modestly raised this estimate by converting the top-level `BonusPackService` and `FactionPackService` areas from unknown or partial into refactored-but-accounted-for reward ownership.

Additional tracing of `reward`, `duel`, `mail`, `trade`, `toypet`, `housing`, `summons`, `vortex`, `warehouse`, `respawn`, `skill learn`, and `weather` increases confidence inside this band, but does not move the band itself because the largest weighted unknowns remain unresolved.

## Highest-Signal Missing Areas

The largest `Not obvious` areas by weighted Java file count are:

- `siege` package: 14 weighted files
- `panesterra` package: 4 weighted files
- `transfers` package: 4 weighted files
- `ban` package: 3 weighted files
- `conquerorAndProtectorSystem` package: 3 weighted files
- `event` package: 3 weighted files
- `worldraid` package: 2 weighted files

These are the biggest discovery risks because they represent larger Java areas with no obvious C# service ownership.

## Important Partial Areas

These areas likely exist in some form but need a deeper pass before they can be counted as mostly done:

- `mail`
- `reward`
- `summons`
- `toypet`
- `trade`
- `vortex`
- `housing`
- `warehouse`
- `respawn`
- `skill learn`
- `weather`
- `duel`

## Active Work Estimates

These directional estimates turn the current discovery pass into explicit "complete versus remaining" numbers for the active manual queue.

They are intentionally conservative and based on discoverable C# ownership, not on assumed hidden parity.

| Area | Java weight | Status | Estimated complete | Estimated remaining | Interpretation |
| --- | ---: | --- | ---: | ---: | --- |
| `siege` | 14 | `Not obvious` | 10% | 90% | likely missing; only indirect fortress/AP traces are obvious |
| `worldraid` | 2 | `Not obvious` | 5% | 95% | likely missing; no obvious C# owner |
| `transfers` | 4 | `Not obvious` | 5% | 95% | likely missing; no character-transfer cluster is discoverable |
| `panesterra` | 4 | `Not obvious` | 5% | 95% | likely missing; no obvious renamed owner |
| `ban` | 3 | `Not obvious` | 10% | 90% | likely missing in game-server; may live partly outside this surface |
| `conquerorAndProtectorSystem` | 3 | `Not obvious` | 5% | 95% | likely missing; no concrete C# owner is visible |
| `event` | 3 | `Not obvious` | 20% | 80% | partial runtime ownership through event-drop support only |
| `reward` | 5 | `Partial` | 60% | 40% | hidden/renamed ownership is real, but advent, veteran, and web reward parity remain open |
| `duel` | 1 | `Partial` | 55% | 45% | request flow is present; scheduler and live death side effects remain incomplete |
| `mail` | 6 | `Partial` | 55% | 45% | system-mail and persistence ownership are clear; broader formatter/runtime parity is not |
| `trade` | 1 | `Partial` | 65% | 35% | hidden/renamed ownership is strong; live limited-item mutation still needs proof |
| `toypet` | 8 | `Partial` | 55% | 45% | feed, spawn, persistence, and visibility exist; adoption and full lifecycle remain open |
| `housing` | 1 | `Refactored` | 70% | 30% | strong renamed ownership cluster; broader visit and instance side effects still need tracing |
| `summons` | 2 | `Partial` | 40% | 60% | packet and execution surfaces exist; live lifecycle and controller behavior remain incomplete |
| `vortex` | 2 | `Partial` | 30% | 70% | location lookup exists; broader invasion lifecycle is still unclear |
| `warehouse` | 1 | `Partial` | 45% | 55% | expansion flow is represented; broader account and legion warehouse parity is not |
| `respawn` | 1 | `Partial` | 45% | 55% | revive and NPC spawn ownership exist; full timer and cleanup parity still needs work |
| `skill learn` | 1 | `Partial` | 70% | 30% | refactored ownership looks strong across skill, motion, and emotion learning |
| `weather` | 1 | `Partial` | 60% | 40% | renamed weather planning is clear; schedule breadth and zone fanout remain to verify |

### Active Queue Rollup

The table above covers `63` weighted Java files of the `168`-file service surface. Using the directional percentages in that table, the active queue currently rolls up to:

- about `19.2` weighted files complete
- about `43.9` weighted files remaining
- about `30.4%` complete inside the active queue itself
- about `69.6%` remaining inside the active queue itself

That means most of the unresolved work is still concentrated inside the manually tracked risk queue rather than in the already-credited `Present` and `Refactored` areas.

### Estimate Takeaways

- The strongest parity-risk areas now look like `housing`, `skill learn`, `trade`, `reward`, and `weather`.
- The heaviest remaining likely-missing work is still concentrated in `siege`, `worldraid`, `transfers`, `panesterra`, `ban`, and `conquerorAndProtectorSystem`.
- Because those unresolved areas carry a large share of the weighted Java surface, the working estimate remains `50%` to `65%` complete overall.

## Remaining Work Rollup

The current document supports three complementary ways to talk about remaining work:

- hard-floor remaining: `109` of `168` weighted files remain outside the clearly-accounted-for `Present` plus `Refactored` bucket -> `64.9%`
- upper-bound remaining after giving credit to all `Partial` areas: `60` of `168` weighted files remain in the `Not obvious` bucket -> `35.7%`
- balanced working remainder: about `46.7%` still missing, unverified, or hidden behind renamed ownership

For planning purposes, the most honest summary is:

- at least `35%` of the Java service surface still looks unresolved
- as much as `65%` remains unresolved if `Partial` areas fail deeper validation
- the active manual queue alone still represents about `43.9` weighted files of remaining work

This is why the estimate should still be read as a band, not as a single exact score.

## Interpretation Guidance

- Use `Not obvious` as the primary missing-work queue.
- Use `Partial` as the primary parity-risk queue.
- Use `Refactored` as "probably present but needs ownership tracing," not as automatically complete.

## Recommended Next Pass

The next manual long pass should prioritize this order:

1. `siege`
2. `worldraid`
3. `transfers`
4. `panesterra`
5. `reward`
6. `duel`
7. `mail`
8. `trade`

That pass should convert discovery status into a stricter split:

- clearly ported
- partially ported
- likely missing
- hidden/renamed but accounted for
