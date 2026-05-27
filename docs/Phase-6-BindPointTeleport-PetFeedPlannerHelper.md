# Phase 6 Pet Feed Planner Helper

Date: May 27, 2026
Unit of Work: UOW-1321
Status: Non-live pet flavour feed-result planner added; live feeding remains disabled.

## Scope

This unit ports the supplied-data portion of Java `PetFlavour.processFeedResult` and `PetFlavour.isLovedFood`.

It covers:

- Java `FoodType` values as a C# enum surface
- supplied reward-group selection by food type
- no-op/null result when the food group is missing
- normal-feed progress mutation followed by null result until `FULL`
- normal-feed reward return after the calculator reaches `FULL`
- loved-feed state marking, loved count consumption, and reward return
- loved-limit consumed no-op behavior through the existing calculator helper
- `isLovedFood` matching by food type only, mirroring the Java method's ignored `itemId`

It does not wire `DataManager.ITEM_GROUPS_DATA`, XML static data loading, live item-template lookup, Java RNG runtime comparison, inventory mutation, scheduling, persistence, or packet dispatch.

## Migration Parity Table - UOW-1321

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.processFeedResult` | `Aion.GameServer.Services.ToyPet.PetFeedPlanner.ProcessFeedResult` | Template Flow / Planner | Partial | Unit Tested | Partial Parity | Ports supplied reward-group lookup, loved flag setup, calculator invocation, not-full null return, and reward return. Java `getFoodType`, XML/static data, item-group lookup, live inventory mutation, scheduler, DAO writes, and packet dispatch remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.isLovedFood` | `Aion.GameServer.Services.ToyPet.PetFeedPlanner.IsLovedFood` | Template Flow / Predicate | Complete for supplied groups | Unit Tested | Partial Parity | Mirrors Java's reward-group lookup and ignores `itemId` behavior. Live food-type resolution through `DataManager.ITEM_GROUPS_DATA` remains unported. |
| `com.aionemu.gameserver.model.templates.pet.FoodType` | `Aion.GameServer.Services.ToyPet.PetFoodType` | Enum | Complete for listed values | Unit Tested through planner | Needs Verification | All Java enum values are represented in C# PascalCase. XML string serialization/name mapping is not wired and needs verification before static-data integration. |
| `com.aionemu.gameserver.model.templates.pet.PetRewards` | `Aion.GameServer.Services.ToyPet.PetFeedRewardGroup` | DTO / Reward Group Projection | Partial | Unit Tested | Needs Verification | C# carries supplied food type, loved flag, and reward DTOs. JAXB defaults, XML serialization, lazy list construction, and static-data ownership remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetFeedResult` | `Aion.GameServer.Services.ToyPet.PetFeedReward` | DTO / Reward Projection | Partial | Existing Unit Tested | Needs Verification | Planner consumes the existing supplied item id/item level DTO. Java XML DTO only stores item id; level lookup remains supplied outside live `DataManager.ITEM_DATA`. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator` | `Aion.GameServer.Services.ToyPet.PetFeedCalculator` via planner | Utility / Calculator | Partial | Unit Tested | Partial Parity | Planner delegates progress mutation and reward selection to existing helpers. Static full-count table generation and Java runtime comparison remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessFeedResultReturnsNullAndDoesNotMutateWhenFoodGroupMissingLikeJava` | Unit | `PetFlavour.processFeedResult` missing group branch | Missing matching reward group returns null and leaves progress unchanged. | Source-derived deterministic assertion. | No live food-type lookup. |
| `ProcessFeedResultNormalFoodUpdatesProgressAndReturnsNullUntilFullLikeJava` | Unit | `PetFlavour.processFeedResult` normal group branch | Normal feed uses flavour `fullCount`, mutates progress, and returns null while not full. | Source-derived deterministic assertion. | No inventory mutation or packet feedback. |
| `ProcessFeedResultNormalFoodReturnsRewardAfterCalculatorReachesFullLikeJava` | Unit | `PetFlavour.processFeedResult` and calculator full transition | Semifull progress crossing Java threshold becomes full and returns reward. | Source-derived deterministic assertion. | Uses supplied point table/reward group. |
| `ProcessFeedResultLovedFoodMarksLovedFeedsAndReturnsRewardLikeJava` | Unit | `PetFlavour.processFeedResult` loved group branch | Loved group marks loved-feed state, consumes loved count, sets full, and returns selector-chosen reward. | Source-derived deterministic assertion. | Java `Rnd.get` remains injected. |
| `ProcessFeedResultLovedFoodReturnsNullWhenLovedLimitAlreadyConsumedLikeJava` | Unit | Java loved-limit guard through `PetFeedCalculator.updatePetFeedProgress` | Loved flag is set, but consumed loved limit prevents full transition and reward. | Source-derived deterministic assertion. | No live feedback packet. |
| `IsLovedFoodMatchesFirstRewardGroupTypeLikeJava` | Unit | `PetFlavour.isLovedFood` | Matching groups report loved flag; missing groups return false. | Source-derived deterministic assertion. | Java method's ignored `itemId` remains represented by omission. |

## Remaining Risks

- Java `PetFlavour.getFoodType` is not ported.
- Food group lookup through `DataManager.ITEM_GROUPS_DATA` is not wired.
- Static XML/JAXB defaults for `PetFlavour`, `PetRewards`, and `PetFeedResult` are not wired.
- Item-template level lookup through `DataManager.ITEM_DATA` remains supplied to `PetFeedReward`.
- Java `Rnd.get` randomness is injected and deterministic in tests; no runtime RNG comparison exists.
- Live inventory mutation, feed scheduler/reuse-time update, DAO saves, and packet dispatch remain disabled.
- XML serialization/name mapping for `FoodType` needs verification before static-data integration.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 planner helper, 1 enum surface, 1 reward-group DTO, and 6 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: `PetFlavour.getFoodType`, item-group static lookup, XML/JAXB static data, item-template lookup, Java RNG runtime comparison, live feed mutation, DAO writes, scheduler behavior, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit and port a non-live `PetFlavour.getFoodType` / item-group lookup adapter using supplied item-group predicates before attempting XML/static-data or live feed service wiring.
