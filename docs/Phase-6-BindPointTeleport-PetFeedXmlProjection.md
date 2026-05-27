# Phase 6 Pet Feed XML Projection

Date: May 27, 2026
Unit of Work: UOW-1323
Status: Non-live pet feed XML projection added; live `DataManager` integration remains disabled.

## Scope

This unit adds an offline projection for Java `pet_feed.xml` / `PetFeedData`.

It covers:

- parsing flavour id, `full_count`, `loved_limit`, and cooldown
- Java class default for missing `full_count` as `1`
- reward group food type, loved flag default, and result item ids
- duplicate flavour id replacement like Java `Map.put`
- sorted positive full-count projection like Java `TreeSet<Short>`
- checked-in `pet_feed.xml` shape counts: 33 flavours, 64 food nodes, 199 result nodes, and full counts `1,10,25,40,50,100,200`

It does not integrate with global static-data loading, XML schema validation, `DataManager.PET_FEED_DATA`, item-template level lookup, item-group lookup, live feed mutation, persistence, or packet dispatch.

## Migration Parity Table - UOW-1323

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.PetFeedData` | `Aion.GameServer.Services.ToyPet.PetFeedXmlProjection` | Static Data Projection | Partial | Unit Tested | Partial Parity | Parses offline XML into an id-keyed map and replaces duplicate ids like Java `Map.put`. Java JAXB unmarshal lifecycle, global `DataManager.PET_FEED_DATA`, schema validation, and static-data bootstrap are not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetFlavour` | `Aion.GameServer.Services.ToyPet.PetFeedFlavourProjection` | DTO / Static Data Projection | Partial | Unit Tested | Needs Verification | Carries id, full count, loved limit, cooldown, and reward groups. Java `getFoodType`, process methods, JAXB annotations, lazy food list, and live template ownership remain outside this DTO. |
| `com.aionemu.gameserver.model.templates.pet.PetRewards` | `Aion.GameServer.Services.ToyPet.PetFeedRewardGroup` via XML projection | DTO / Static Data Projection | Partial | Unit Tested | Needs Verification | XML group/loved/result item ids are projected. Java JAXB lazy list behavior and live item-level lookup remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetFeedResult` | `Aion.GameServer.Services.ToyPet.PetFeedReward` via XML projection | DTO / Static Data Projection | Partial | Unit Tested | Needs Verification | XML result item ids are projected with item level `0` because Java resolves item template levels later during reward selection. |
| `com.aionemu.gameserver.model.templates.pet.FoodType` | `Aion.GameServer.Services.ToyPet.PetFoodType` XML mapping in `PetFeedXmlProjection` | Enum / XML Mapping | Partial | Unit Tested | Needs Verification | Parser maps Java XML enum strings used by pet feed data, including existing helper-only `EXCLUDES`/`STINKY` support. Full serializer/round-trip behavior is not wired. |
| `com.aionemu.gameserver.services.toypet.PetFeedCalculator` static full-count discovery | `PetFeedXmlProjection.GetSortedFullCounts` | Static Data Helper | Partial | Unit Tested | Partial Parity | Mirrors sorted positive full-count discovery from supplied flavours. Java casts to `short` and uses `TreeSet`; overflow behavior is not tested because checked-in XML values are small positive ints. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ParseProjectsJavaPetFeedXmlDefaultsAndRewardGroups` | Unit | `PetFlavour`, `PetRewards`, `PetFeedResult` JAXB fields | Flavour defaults, cooldown, reward group order, loved default/true flag, and result item ids. | Source-derived deterministic assertion. | No schema validation. |
| `ParseUsesLaterDuplicateFlavourLikeJavaMapPut` | Unit | `PetFeedData.afterUnmarshal` | Duplicate flavour id keeps later entry like Java `Map.put`. | Source-derived deterministic assertion. | No Java runtime unmarshal comparison. |
| `GetSortedFullCountsMatchesJavaTreeSetBehavior` | Unit | `PetFeedCalculator` static initialization | Positive full counts are distinct and sorted; zero is ignored. | Source-derived deterministic assertion. | Does not test short overflow. |
| `ParseCheckedInPetFeedXmlMatchesCurrentJavaStaticDataShape` | Regression | Checked-in Java `game-server/data/static_data/pets/pet_feed.xml` | Current XML has 33 flavours, full counts `1,10,25,40,50,100,200`, 64 food nodes, and 199 result nodes. | Repository XML regression assertion. | Does not run Java JAXB loader. |

## Remaining Risks

- Java JAXB/schema validation is not executed.
- Global `DataManager.PET_FEED_DATA` integration is not wired.
- Item-template level lookup through `DataManager.ITEM_DATA` remains unresolved.
- Food item-group lookup through `DataManager.ITEM_GROUPS_DATA` remains offline/supplied.
- Java `TreeSet<Short>` overflow behavior is not tested because XML values are in normal range.
- XML serialization/round-trip behavior is not implemented.
- Live feed service mutation, scheduler/reuse-time update, DAO saves, and packet dispatch remain disabled.
- Java runtime comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 XML projection helper, 1 flavour projection DTO, and 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: Java JAXB validation, `DataManager.PET_FEED_DATA`, item-template lookup, item-group lookup, live feed mutation, DAO writes, scheduler behavior, Java runtime comparison, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Compose the offline pet feed pipeline by combining `PetFeedXmlProjection`, `PetFeedCalculator.CreatePointValues`, `PetFoodTypeLookup`, and `PetFeedPlanner` into a non-live feed evaluation helper, still avoiding live inventory, `DataManager`, scheduler, DAO writes, and packet dispatch.
