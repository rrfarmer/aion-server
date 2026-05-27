# Phase 6 Pet Feed Evaluation Helper

Date: May 27, 2026
Unit of Work: UOW-1324
Status: Non-live pet feed evaluation helper added; live `PetService.feed` execution remains disabled.

## Scope

This unit composes the already ported offline feed pieces into a single supplied-data evaluator for one Java-shaped pet feed attempt.

It covers:

- flavour lookup by id from projected `pet_feed.xml` data
- ordered food-type resolution through supplied item groups
- Java `PetService.checkFeeding` loved-limit gate before mutation
- supplied fed-item level lookup before calculator mutation
- point-table construction from projected full counts
- delegation to `PetFeedPlanner.ProcessFeedResult`
- loved multi-reward item-level resolution before selector-based reward choice
- explicit failures when required item-template levels are not supplied

It does not consume live inventory, decrement items, send `SM_PET`/`SM_EMOTION`/system messages, schedule refed cooldowns, save DAO state, call live `DataManager`, or run Java runtime comparisons.

## Migration Parity Table - UOW-1324

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` feed-type/loved-limit/reward branch | `Aion.GameServer.Services.ToyPet.PetFeedEvaluation.Evaluate` | Service Flow Helper | Partial | Unit Tested | Partial Parity | Composes flavour lookup, food-type lookup, loved-limit rejection, fed-item level lookup, progress mutation, and reward return for supplied data. Live inventory decrement, item unlock, packet order, scheduler recursion, reward item creation, cooldown timing, DAO writes, and socket dispatch are not ported. |
| `com.aionemu.gameserver.dataholders.PetFeedData.getFlavourById` | `Aion.GameServer.Services.ToyPet.PetFeedEvaluationContext.Flavours` | Static Data Lookup | Partial | Unit Tested | Needs Verification | Uses supplied projected flavour dictionary and throws on missing ids instead of silently assuming parity. Java returns null from the map and the caller would fail later if dereferenced. Global `DataManager.PET_FEED_DATA` is not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.getFoodType` | `Aion.GameServer.Services.ToyPet.PetFoodTypeLookup.GetFoodType` via evaluator | Template Flow / Lookup | Partial | Unit Tested | Partial Parity | Evaluator calls the existing ordered supplied item-group lookup before any mutation. XML item-group loading, race validation, live item lookup, and `DataManager.ITEM_GROUPS_DATA` remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.isLovedFood` | `Aion.GameServer.Services.ToyPet.PetFeedPlanner.IsLovedFood` via evaluator | Template Flow / Predicate | Partial | Unit Tested | Partial Parity | Evaluator mirrors Java service behavior by rejecting loved food with no remaining loved limit before calling process-feed mutation. Live packet/system-message branch is not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.processFeedResult` | `Aion.GameServer.Services.ToyPet.PetFeedPlanner.ProcessFeedResult` via evaluator | Template Flow / Planner | Partial | Unit Tested | Partial Parity | Evaluator delegates mutation/reward planning to the existing helper after resolving supplied item level. Live `PetFlavour` ownership, JAXB lazy list behavior, inventory mutation, and packet feedback remain unported. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator` static full-count/point-table use | `Aion.GameServer.Services.ToyPet.PetFeedEvaluationContext.FullCounts` / `PointValues` | Static Table Composition | Partial | Unit Tested | Partial Parity | Builds point tables from supplied projected flavours, including loved-food full count `1` when present in data. Java static initialization through `DataManager.PET_FEED_DATA` and short overflow behavior remain unverified. |
| `com.aionemu.gameserver.dataholders.DataManager.ITEM_DATA.getItemTemplate(...).getLevel()` | supplied `PetFeedEvaluationContext.ItemLevels` | Static Data Boundary | Refactored | Unit Tested | Needs Verification | Fed item levels and loved multi-reward levels are supplied explicitly. Missing levels throw `KeyNotFoundException`; Java live behavior depends on item-template availability and would fail differently if data is absent. |
| `com.aionemu.commons.utils.Rnd.get(List<T>)` | caller-supplied loved reward selector through evaluator | Random Selection Boundary | Refactored | Unit Tested with deterministic selector | Intentional Difference | Random multi-reward choice remains injected for deterministic tests. No Java RNG stream comparison was attempted. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EvaluateReturnsNoMatchAndDoesNotMutateWhenItemIsNotFoodLikeJavaServiceGate` | Unit | `PetService.checkFeeding` non-eatable branch | No matching food type returns null result and leaves progress unchanged. | Source-derived deterministic assertion. | Does not assert packet/system-message branch. |
| `EvaluateLovedFoodWithNoRemainingLimitReturnsNoMatchAndDoesNotMutateLikeJavaServiceGate` | Unit | `PetService.checkFeeding` loved-limit precheck | Loved food with no remaining limit is rejected before process-feed mutation. | Source-derived deterministic assertion. | Does not assert unlock/end-feeding packets. |
| `EvaluateNormalFoodUsesSuppliedItemLevelAndReturnsNullUntilFullLikeJava` | Unit | `PetService.checkFeeding` plus `PetFlavour.processFeedResult` | Normal food uses supplied item level, mutates regular count/points, and returns no reward before full. | Source-derived deterministic assertion. | No inventory decrement. |
| `EvaluateNormalFoodReturnsRewardAfterFullTransitionLikeJava` | Unit | `PetFlavour.processFeedResult` / `PetFeedCalculator.getReward` | A semifull normal feed crossing the full threshold returns the expected reward item. | Source-derived deterministic assertion. | No item creation or cooldown reset. |
| `EvaluateLovedFoodResolvesRewardItemLevelsBeforeSelectorLikeJava` | Unit | `PetFeedCalculator.getReward` loved multi-reward branch | Loved reward levels are supplied/resolved before highest-allowed-level filtering and selector invocation. | Source-derived deterministic assertion. | Java RNG is injected, not compared. |
| `EvaluateThrowsWhenFedItemLevelIsMissingInsteadOfAssumingParity` | Unit | `DataManager.ITEM_DATA` live boundary | Missing fed item-template level is explicit. | Boundary assertion. | Java exception type/runtime path not compared. |
| `EvaluateThrowsWhenLovedRewardItemLevelIsMissingInsteadOfAssumingParity` | Unit | `DataManager.ITEM_DATA` live boundary | Missing loved reward item-template level is explicit. | Boundary assertion. | Java exception type/runtime path not compared. |
| `EvaluationContextBuildsFullCountTablesFromProjectedFlavours` | Unit | `PetFeedCalculator` static initialization | Context composes sorted full counts and point-table width from supplied flavours. | Source-derived deterministic assertion. | No live static bootstrap. |

## Remaining Risks

- Live `PetService.checkFeeding` inventory decrement and item unlock behavior remain unported.
- `SM_PET`, `SM_EMOTION`, and system-message dispatch ordering is not composed or validated.
- Refeed scheduling, cooldown timestamp updates, and `PlayerPetsDAO.setTime` remain disabled.
- `ItemService.addItem` reward creation and failure behavior are not modeled.
- Global `DataManager.PET_FEED_DATA`, `ITEM_GROUPS_DATA`, and `ITEM_DATA` are not wired.
- Java JAXB/schema validation and XML item-group loading remain unavailable.
- Missing item-template levels are explicit C# exceptions; Java runtime exception parity is not verified.
- Loved multi-reward RNG is injected; no Java RNG stream comparison exists.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 evaluation helper, 1 evaluation context, 1 result DTO, and 8 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: live `DataManager` surfaces, inventory mutation, item unlock, reward item creation, scheduler/refeed timing, DAO writes, packet dispatch, Java RNG runtime comparison, Java JAXB/schema validation, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit and design the live `PetService.checkFeeding` operation plan around the evaluator: inventory decrement, non-eatable unlock/end-feed packets, reward packet order, cooldown/refeed DAO write, progress reset, and scheduler recursion. Keep it non-live until packet dispatch, repository execution, and item mutation boundaries are ready.
