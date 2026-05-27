# Phase 6AFB Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1322
Latest Commit: included in the UOW-1322 unit commit
Status: Pet food-type lookup is ported as a non-live supplied-data helper; live static-data integration remains disabled.

## What Changed

- Added `PetFoodItemGroups`.
- Added `PetFoodTypeLookup`.
- Ported supplied-data behavior from Java `PetFlavour.getFoodType` and `ItemGroupsData.isFood`.
- Added focused tests for exclude/stinky precedence, direct food-type matching, miscellaneous junk-group matching, ordered reward-group scan, and null result.
- Added `docs/Phase-6-BindPointTeleport-PetFoodTypeLookupHelper.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFoodTypeLookup.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetFoodTypeLookupTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFoodTypeLookup|PetFeedPlanner|PetFeedCalculator|PetFeedProgress|PetHungryLevel"` passed 37 tests.

No XML item-group loading, `DataManager.ITEM_GROUPS_DATA`, live item-template lookup, race validation, live inventory mutation, feed scheduler, DAO write, or socket dispatch was enabled.

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

# Next Work Options

## Recommended Sequential Task

- Task: Audit and port a non-live pet feed static-data projection for `pet_feed.xml` flavour full counts and reward groups.
- Why: The calculator, planner, reward selection, and food-type lookup now accept supplied data; the next prerequisite is a safe offline projection of Java pet feed XML shape.
- Files: likely new helper/test files under `Services/ToyPet` or `Data`; avoid live `DataManager`, global static loaders, DI, repositories, and packet dispatch.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live pet feed XML projection | New helper/test files | Medium | Recommended next writer; keep offline/supplied. |
| B | Pet DAO live execution readiness audit | Java/C# read-only | Low | Independent from static-data projection. |
| C | Java pet vector generator retry | Tooling/docs read-only | Low | Only useful if runtime capture tooling works. |
| D | `PetService.feed` dependency map | Java read-only | Medium | Analysis only; live mutation should not start yet. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Port non-live pet feed XML projection | New helper/test files | Shared docs until orchestrator update, live `DataManager`, DI, repositories |
| Agent B | Audit `PetService.feed` dependencies | Read-only Java/C# inspection | All writes |

## Do Not Parallelize

- `PetFoodTypeLookup.cs` and `PetFeedPlanner.cs`: fresh helpers; avoid concurrent edits unless the next writer owns them.
- Shared progress/handoff docs: orchestrator-owned only.
- Live feed runtime/static-data loader: still blocked by static-data projection, inventory, scheduler, persistence, and packet surfaces.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFlavour.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetRewards.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFeedResult.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/FoodType.java`
  - `game-server/src/com/aionemu/gameserver/dataholders/ItemGroupsData.java`
  - `game-server/data/static_data/pets/pet_feed.xml`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFoodTypeLookup.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedPlanner.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedCalculator.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFoodTypeLookupTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedPlannerTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFoodTypeLookupHelper.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
