# Phase 6AEZ Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1320
Latest Commit: included in the UOW-1320 unit commit
Status: Pet feed reward selection is ported as a non-live injectable helper; live feeding remains disabled.

## What Changed

- Extended `PetFeedCalculator` with `CreatePointValues` and `GetReward`.
- Added `PetFeedReward` as a supplied reward/item-level DTO.
- Ported Java reward guard behavior, point-threshold reward indexing, rounding clamp, loved single-reward short-circuit, and loved highest-allowed-level filtering.
- Kept Java `Rnd.get` and `DataManager.ITEM_DATA` as injected/supplied boundaries.
- Added `docs/Phase-6-BindPointTeleport-PetFeedRewardSelectionHelper.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Changed

- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedCalculator.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedCalculatorTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedCalculator"` passed 20 test cases.

No XML/static data loading, live item-template lookup, Java RNG runtime comparison, live inventory mutation, feed scheduler, DAO write, or socket dispatch was enabled.

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

# Next Work Options

## Recommended Sequential Task

- Task: Port a non-live `PetFlavour.processFeedResult` planner.
- Why: Calculator progress and reward helpers now exist, but Java's feed template flow still needs supplied reward-group resolution before live feeding can be planned.
- Files: likely new helper/test files under `Services/ToyPet`; avoid live inventory, static data singletons, repositories, and packet dispatch.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live `PetFlavour.processFeedResult` planner | New helper/test files | Medium | Recommended next writer; use supplied food type/reward groups. |
| B | Pet feed static XML full-count audit | XML/Java/C# read-only | Low | Useful before live static data integration. |
| C | Pet DAO live execution readiness audit | Java/C# read-only | Low | Keep separate from feed planner. |
| D | Java pet vector generator retry | Tooling/docs read-only | Low | Only useful if runtime capture tooling works. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Agent A | Port non-live feed result planner | New `Services/ToyPet` helper, new tests | Shared docs until orchestrator update, live runtime, DI, repositories |
| Agent B | Audit static feed XML full counts | Read-only XML/Java/C# inspection | All writes |

## Do Not Parallelize

- `PetFeedCalculator.cs`: reward and progress helpers are fresh; avoid concurrent edits unless the next writer owns this file.
- `PetFeedProgress.cs`: avoid touching unless planner work proves a missing Java behavior.
- Shared progress/handoff docs: orchestrator-owned only.
- Live feed runtime: still blocked by inventory, static data, scheduler, persistence, and packet surfaces.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetFeedCalculator.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFlavour.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetRewards.java`
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFeedResult.java`
  - `commons/src/com/aionemu/commons/utils/Rnd.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedCalculator.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetFeedProgress.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedCalculatorTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetFeedProgressTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetFeedRewardSelectionHelper.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
