# Phase 6AFC Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1323
Latest Commit: included in the UOW-1323 unit commit
Status: Pet feed XML projection is ported as an offline helper; live `DataManager` integration remains disabled.

## What Changed

- Added `PetFeedXmlProjection`.
- Added `PetFeedFlavourProjection`.
- Parsed Java-shaped `pet_feed.xml` into an id-keyed projection map.
- Preserved Java class default `full_count = 1`, loved default `false`, duplicate id replacement, and sorted positive full-count discovery.
- Added regression coverage for the checked-in `game-server/data/static_data/pets/pet_feed.xml` shape.
- Added `docs/Phase-6-BindPointTeleport-PetFeedXmlProjection.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedXmlProjection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedXmlProjectionTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedXmlProjection|PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel"` passed 41 tests.

No Java JAXB/schema validation, global static-data loader integration, live item-template lookup, item-group lookup, live inventory mutation, feed scheduler, DAO write, or socket dispatch was enabled.

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

# Next Work Options

## Recommended Sequential Task

- Task: Compose an offline pet feed evaluation helper.
- Why: XML projection, full-count tables, food-type lookup, feed planner, and reward selection now exist separately; the next safe step is a non-live composition helper that takes supplied item groups/item levels and evaluates one feed attempt without live mutation.
- Files: likely new helper/test files under `Services/ToyPet`; avoid live `DataManager`, global static loaders, DI, repositories, and packet dispatch.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Offline feed evaluation composition | New helper/test files | Medium | Recommended next writer; supplied item groups/item levels only. |
| B | Pet feed XML Java runtime/JAXB comparison design | docs/read-only | Low | Useful before claiming stronger parity. |
| C | Pet DAO live execution readiness audit | Java/C# read-only | Low | Independent from feed composition. |
| D | `PetService.feed` dependency map | Java read-only | Medium | Analysis only; live mutation should not start yet. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Compose offline feed evaluation helper | New `Services/ToyPet` helper, new tests | Shared docs until orchestrator update, live `DataManager`, DI, repositories |
| Agent B | Audit `PetService.feed` dependencies | Read-only Java/C# inspection | All writes |

## Do Not Parallelize

- `PetFeedXmlProjection.cs`, `PetFoodTypeLookup.cs`, and `PetFeedPlanner.cs`: fresh helpers; avoid concurrent edits unless the next writer owns them.
- Shared progress/handoff docs: orchestrator-owned only.
- Live feed runtime/static-data loader: still blocked by static-data integration, inventory, scheduler, persistence, and packet surfaces.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/dataholders/PetFeedData.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFlavour.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetRewards.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFeedResult.java`
  - `game-server/data/static_data/pets/pet_feed.xml`
  - `game-server/data/static_data/pets/pet_feed.xsd`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedXmlProjection.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFoodTypeLookup.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPlanner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedCalculator.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedXmlProjectionTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedXmlProjection.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
