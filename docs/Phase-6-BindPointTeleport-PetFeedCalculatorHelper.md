# Phase 6 Pet Feed Calculator Helper

Date: May 27, 2026
Unit of Work: UOW-1319
Status: Non-live pet feed progress calculator helper added; reward selection and live feeding remain disabled.

## Scope

This unit ports the deterministic progress-mutation portion of Java `PetFeedCalculator`.

It covers:

- Java `getPoints(feedPoints, maxFeedCount)` threshold math
- Java five-level item feed-point buckets
- normal feed progress point increments and regular consumed count updates
- hungry-level threshold switching and exact-threshold non-switch behavior
- loved-feed full-state transition and consumed-limit no-op behavior

It does not port `PetFeedCalculator.getReward`, `PetFlavour.processFeedResult`, static `DataManager` lookup, item-template level checks, random loved-reward selection, inventory mutation, scheduling, persistence, or packet dispatch.

## Migration Parity Table - UOW-1319

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator.getPoints` | `Aion.GameServer.Services.ToyPet.PetFeedCalculator.GetPoints` | Utility / Calculator | Complete for helper | Unit Tested | Partial Parity | Source-derived samples match Java's documented precomputed table. Full static initialization from `DataManager.PET_FEED_DATA` is not ported. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator.updatePetFeedProgress` | `Aion.GameServer.Services.ToyPet.PetFeedCalculator.UpdatePetFeedProgress` | Utility / Calculator | Complete for helper | Unit Tested | Partial Parity | Ports deterministic normal-feed and loved-feed progress mutation, Java threshold comparisons, point bucket calculation, and count increments. Live `PetFlavour` flow, inventory consumption, refeed scheduling, DAO writes, and packet dispatch remain unported. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `Aion.GameServer.Services.ToyPet.PetFeedProgress` used by calculator | Model Helper | Partial | Unit Tested | Partial Parity | Existing helper is now consumed by the calculator. Bit packing, loved flags, counters, and hungry level are tested, but full live common-data ownership remains unported. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.processFeedResult` | future C# pet flavour/feed result planner | Service / Template Flow | Not Started | Manual Only | Needs Verification | Java calls the calculator after resolving food group/loved state and then calls `getReward`. Static-data item-group lookup, reward group selection, and live feed behavior remain outside this helper. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator.getReward` | future C# reward selection helper | Utility / Reward Selection | Not Started | Manual Only | Needs Verification | Reward selection depends on Java `fullCounts`, `pointValues`, `DataManager.ITEM_DATA`, player level filtering, and `Rnd.get`; none are ported in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GetPointsMatchesJavaPrecalculatedTableSamples` | Unit | Java `PetFeedCalculator` documented point table and `getPoints` loop | Feed-point totals for representative feed points/counts, including max documented value. | Source-derived deterministic assertions. | Does not build `fullCounts` from static data. |
| `GetFeedPointsForItemLevelUsesJavaFiveLevelBuckets` | Unit | Java `updatePetFeedProgress` item-level bucket logic | Level 5/6/10/11/60 bucket boundaries. | Source-derived deterministic assertions. | Invalid high item levels still throw by array bounds; not separately asserted. |
| `UpdatePetFeedProgressAddsPointsAndIncrementsRegularCountLikeJava` | Unit | Java `updatePetFeedProgress` normal-feed branch | Adds feed points and increments regular count before threshold switch. | Source-derived deterministic assertion. | No live inventory or packet behavior. |
| `UpdatePetFeedProgressSwitchesHungryLevelAfterJavaThreshold` | Unit | Java `updatePetFeedProgress` forced-switch branch | Regular count above 50 percent switches from hungry to content and preserves old points. | Source-derived deterministic assertion. | Later content/semifull thresholds need broader coverage. |
| `UpdatePetFeedProgressDoesNotSwitchAtExactHalfThresholdLikeJava` | Unit | Java strict `>` threshold comparison | Exact half threshold adds points and does not switch. | Source-derived deterministic assertion. | No runtime comparison. |
| `UpdatePetFeedProgressLovedFoodSetsFullAndConsumesLovedLimitLikeJava` | Unit | Java loved-feed branch | Loved feed sets full, consumes loved count, and avoids regular count. | Source-derived deterministic assertion. | No reward selection. |
| `UpdatePetFeedProgressLovedFoodNoOpsWhenLovedLimitIsConsumedLikeJava` | Unit | Java loved-limit guard | Loved feed no-ops when remaining loved count is zero. | Source-derived deterministic assertion. | No live feedback packet. |

## Remaining Risks

- Java `PetFeedCalculator.getReward` is not ported.
- Java `PetFlavour.processFeedResult` is not ported.
- Static `fullCounts` and `pointValues` generation from `DataManager.PET_FEED_DATA` is not represented.
- Item-template level filtering and random reward selection remain unported.
- Live inventory mutation, feed scheduler/reuse-time update, DAO save calls, and packet dispatch remain disabled.
- Invalid high item-level array-bound behavior is source-compatible but not exhaustively tested.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 calculator helper and 7 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: reward selection, `PetFlavour` process flow, static data full-count generation, item-template lookup, random reward choice, live feed service mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Port a non-live reward-selection helper for `PetFeedCalculator.getReward` with injected full-counts, point thresholds, reward item levels, and deterministic random choice for loved rewards.
