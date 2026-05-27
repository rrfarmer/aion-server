# Phase 6 Pet Feed Reward Selection Helper

Date: May 27, 2026
Unit of Work: UOW-1320
Status: Non-live pet feed reward selection helper added; live feeding remains disabled.

## Scope

This unit ports the deterministic and injectable portions of Java `PetFeedCalculator.getReward`.

It covers:

- generated point-value tables from supplied Java full-count values
- null reward behavior when the pet is not full, rewards are empty, or `fullCount` is unknown
- normal-feed reward index selection from point thresholds
- Java rounding-fix clamp to the first and last reward entries
- loved-feed single-reward short-circuit before item-level filtering
- loved-feed highest allowed item-level filtering before caller-supplied random selection

It does not wire Java `DataManager.ITEM_DATA`, Java `Rnd.get`, XML static data loading, `PetFlavour.processFeedResult`, live inventory mutation, scheduling, persistence, or packet dispatch.

## Migration Parity Table - UOW-1320

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator.getReward` | `Aion.GameServer.Services.ToyPet.PetFeedCalculator.GetReward` | Utility / Reward Selection | Partial | Unit Tested | Partial Parity | Ports normal reward threshold indexing, Java rounding clamp, loved single-reward short-circuit, and loved item-level filtering. Java `DataManager.ITEM_DATA` item-level lookup and `Rnd.get` are injected/supplied by caller, not wired live. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator.pointValues` | `Aion.GameServer.Services.ToyPet.PetFeedCalculator.CreatePointValues` | Utility / Static Table Projection | Partial | Unit Tested | Partial Parity | Builds point-value rows from supplied full counts and Java item levels. Static initialization from `DataManager.PET_FEED_DATA.getPetFlavours()` and Java `TreeSet<Short>` edge cases are not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetFeedResult` | `Aion.GameServer.Services.ToyPet.PetFeedReward` | DTO / Reward Projection | Partial | Unit Tested | Needs Verification | C# carries item id plus supplied item level so reward selection can avoid live `DataManager.ITEM_DATA`. Java XML DTO only stores item id and resolves level during reward selection. |
| `com.aionemu.commons.utils.Rnd.get(List<T>)` | caller-supplied loved reward selector | Random Selection | Refactored | Unit Tested with deterministic selector | Intentional Difference | Java returns null for an empty list, first item for a singleton, or a random list entry. C# helper preserves empty/singleton behavior and injects multi-entry selection for deterministic tests; no Java RNG stream comparison was attempted. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.processFeedResult` | future C# pet flavour/feed result planner | Service / Template Flow | Not Started | Manual Only | Needs Verification | Feed group resolution, loved flag setup, calculator invocation, and live process flow remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreatePointValuesMatchesJavaDocumentedTableSamples` | Unit | Java `PetFeedCalculator` documented point table | Generated rows for level buckets and full counts match table samples. | Source-derived deterministic assertions. | Does not load full counts from XML/static data. |
| `GetRewardReturnsNullWhenNotFullNoRewardsOrUnknownFullCountLikeJava` | Unit | Java `getReward` guards | Not-full progress, empty rewards, and missing full count return null. | Source-derived deterministic assertions. | No live `PetFlavour` caller. |
| `GetRewardSelectsNormalRewardIndexFromPointThresholdsLikeJava` | Unit | Java normal reward loop and `Math.round(float)` | Full normal progress at an in-mask max threshold selects the last reward. | Source-derived deterministic assertion. | Uses supplied point table; Java runtime not executed. |
| `GetRewardClampsNormalRewardIndexToFirstLikeJavaRoundingFix` | Unit | Java reward-index clamp | Low progress clamps to the first reward. | Source-derived deterministic assertion. | Does not cover every intermediate row. |
| `GetRewardLovedFeedReturnsSingleRewardWithoutLevelFilteringLikeJava` | Unit | Java loved-feed singleton branch | Single loved reward returns before item-level filtering. | Source-derived deterministic assertion. | No live item lookup. |
| `GetRewardLovedFeedFiltersToHighestAllowedItemLevelBeforeRandomChoiceLikeJava` | Unit | Java loved-feed filtering before `Rnd.get` | Rewards above player level are skipped, only highest allowed level remains, and caller selector chooses among that list. | Source-derived deterministic assertion. | Java RNG stream is not compared. |

## Remaining Risks

- Static `fullCounts` generation from `DataManager.PET_FEED_DATA` is not wired.
- Java `PetFlavour.processFeedResult` is not ported.
- Food group lookup through `DataManager.ITEM_GROUPS_DATA` is not ported.
- Item-template level lookup through `DataManager.ITEM_DATA` is supplied, not live.
- Java `Rnd.get` randomness is injected and deterministic in tests; no runtime RNG comparison exists.
- `PetFeedProgress.TotalPoints` masks to 14 bits, so Java table entries above that mask need careful live interpretation.
- Live inventory mutation, refeed scheduling, DAO saves, and packet dispatch remain disabled.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 reward-selection helper extension, 1 reward DTO, and 6 focused test methods
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: static full-count loading, `PetFlavour.processFeedResult`, food group lookup, item-template lookup, Java RNG runtime comparison, live feed mutation, DAO writes, scheduler behavior, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Port a non-live `PetFlavour.processFeedResult` planner that resolves supplied reward groups, marks loved feed state, calls the calculator helpers, and returns reward/null without live inventory or static-data access.
