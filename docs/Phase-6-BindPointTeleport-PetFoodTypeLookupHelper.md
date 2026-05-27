# Phase 6 Pet Food Type Lookup Helper

Date: May 27, 2026
Unit of Work: UOW-1322
Status: Non-live pet food-type lookup helper added; live static-data integration remains disabled.

## Scope

This unit ports the supplied-data portion of Java `PetFlavour.getFoodType` and `ItemGroupsData.isFood`.

It covers:

- reward-group ordered food-type scanning
- `EXCLUDES` and `STINKY` rejection before requested food-type matching
- direct food-type membership checks
- Java `MISCELLANEOUS` behavior across `ARMOR`, `BALAUR_SCALES`, `BONES`, `FLUIDS`, `SOULS`, and `THORNS`
- null result when no configured reward group matches

It does not load XML item groups, wire `DataManager.ITEM_GROUPS_DATA`, validate item races/templates, connect live inventory items, or dispatch feed packets.

## Migration Parity Table - UOW-1322

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.pet.PetFlavour.getFoodType` | `Aion.GameServer.Services.ToyPet.PetFoodTypeLookup.GetFoodType` | Template Flow / Lookup | Partial | Unit Tested | Partial Parity | Ports ordered reward-group scan and null result with supplied item-group data. Java `DataManager.ITEM_GROUPS_DATA`, XML-loaded groups, item race/template validation, and live item lookup remain unported. |
| `com.aionemu.gameserver.dataholders.ItemGroupsData.isFood` | `Aion.GameServer.Services.ToyPet.PetFoodItemGroups.IsFood` | Static Data Lookup Helper | Partial | Unit Tested | Partial Parity | Ports exclusion/stinky rejection, direct type membership, and miscellaneous junk-group matching. Java petFood map initialization, missing-map null behavior, XML/JAXB load ordering, and warning logs for unhandled types are not wired. |
| `com.aionemu.gameserver.model.templates.pet.FoodType` | `Aion.GameServer.Services.ToyPet.PetFoodType` | Enum | Complete for listed values | Unit Tested through lookup | Needs Verification | Existing enum surface is consumed by lookup helper. XML string serialization/name mapping is still not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetRewards` | `Aion.GameServer.Services.ToyPet.PetFeedRewardGroup` | DTO / Reward Group Projection | Partial | Unit Tested | Needs Verification | Lookup uses supplied reward group order and type. Java JAXB lazy lists and XML static-data ownership remain unported. |
| `com.aionemu.gameserver.model.templates.itemgroups.ItemRaceEntry` | supplied item-id sets in `PetFoodItemGroups` | Static Data DTO / Item Group Entry | Not Started | Manual Only | Needs Verification | C# helper uses item ids only. Java race filtering, `afterUnmarshal` item-template validation, chance/count fields, and XML serialization are not ported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `IsFoodRejectsExcludedAndStinkyItemsBeforeSpecificTypeLikeJava` | Unit | `ItemGroupsData.isFood` | Excluded and stinky item ids reject even when present in requested type. | Source-derived deterministic assertion. | No live static data map. |
| `IsFoodMatchesSpecificFoodTypeLikeJava` | Unit | `ItemGroupsData.isFood` non-misc branch | Direct type membership succeeds only for the requested type. | Source-derived deterministic assertion. | No XML group loading. |
| `IsFoodMiscellaneousMatchesJavaJunkFoodGroupsOnly` | Unit | `ItemGroupsData.isFood` miscellaneous branch | Miscellaneous matches armor, balaur scales, bones, fluids, souls, and thorns, but not unrelated food. | Source-derived deterministic assertion. | Does not verify every enum group. |
| `GetFoodTypeReturnsFirstRewardGroupWhoseItemGroupMatchesLikeJava` | Unit | `PetFlavour.getFoodType` | Reward-group order controls returned food type. | Source-derived deterministic assertion. | Uses supplied groups, not `DataManager`. |
| `GetFoodTypeReturnsNullWhenNoRewardGroupMatchesLikeJava` | Unit | `PetFlavour.getFoodType` null path | No matching reward group returns null. | Source-derived deterministic assertion. | No live item lookup. |

## Remaining Risks

- XML/static-data loading for pet food groups is not implemented.
- Java `DataManager.ITEM_GROUPS_DATA` and `petFood` map initialization are not wired.
- `ItemRaceEntry.afterUnmarshal` item-template and race validation are not ported.
- Missing Java `petFood` map entries would likely throw `NullPointerException`; C# helper currently treats missing supplied groups as empty by design and needs integration review.
- XML name mapping for `FoodType` / `PetFoodType` remains unverified.
- Live `PetService` feed flow, inventory mutation, scheduler/reuse-time update, DAO saves, and packet dispatch remain disabled.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 lookup helper, 1 supplied item-group container, and 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: XML pet food loading, `DataManager.ITEM_GROUPS_DATA`, `ItemRaceEntry` validation, live item lookup, live feed mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit and port a non-live pet feed static-data projection for `pet_feed.xml` flavour full counts and reward groups, keeping XML parsing supplied/offline and avoiding live `DataManager` integration.
