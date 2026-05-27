# Phase 6AEW Completion Handoff

Date: May 27, 2026
Completed Unit of Work: UOW-1317
Latest Commit: included in the UOW-1317 unit commit
Status: Pet doping-bag helper complete as standalone helper; not yet wired into row projection or live pet runtime.

## What Changed

- Added `PetDopingBag`.
- Added focused tests for Java slot expansion, dirty flag, scroll views, switching, invalid slots, and zero defaults.
- Added `docs/Phase-6-BindPointTeleport-PetDopingBagHelper.md`.
- Updated `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`.
- Updated `docs/PHASE-6-PROGRESS.md`.

## Code Added

- `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetDopingBag.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PetDopingBagTests.cs`

## Validation Completed

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetDopingBag"` passed 11 tests.

No live pet doping runtime, repository execution, or row-projection wiring was enabled.

## Migration Parity Table - UOW-1317

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `Aion.GameServer.Services.ToyPet.PetDopingBag` | Model Helper | Complete for standalone helper | Unit Tested | Partial Parity | Ports food/drink accessors, dynamic slot growth, max-slot guard, dirty flag, scroll view, and scroll-only switch behavior. Not wired into live pet common data, row projection, packet builders, or persistence yet. |
| `java.util.Arrays.copyOf` / `copyOfRange` use in `PetDopingBag` | `Array.Resize`, range copy, and cloned item arrays | Collection Behavior | Refactored | Unit Tested | Intentional Difference | Java `getItems()` returns the backing array directly. C# returns a copy to avoid external mutation; this difference is documented and covered. `getScrollsUsed` remains copy-like as in Java. |
| Java `synchronized setItem` | C# `lock` in `PetDopingBag.SetItem` | Threading | Partial | Unit Tested for deterministic state only | Partial Parity | C# uses a private lock for mutation. No concurrent stress test or integration with live service threading has been run. |
| `com.aionemu.gameserver.services.toypet.PetService.useDoping` | future C# live pet doping runtime | Service | Not Started | Manual Only | Needs Verification | Doping bag helper exists, but live item lookup, cooldown, skill use, bag save trigger, and packet dispatch remain unported. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NewBagReturnsJavaZeroDefaultsAndIsNotDirty` | Unit | `PetDopingBag` accessors | Empty bag defaults. | Source-derived assertion. | No live runtime. |
| `SetFoodAndDrinkUseJavaSlotsAndSetDirty` | Unit | `setFoodItem` / `setDrinkItem` | Food/drink slot placement and dirty flag. | Source-derived assertion. | No persistence trigger. |
| `SetItemExpandsBackingArrayToTouchedSlotLikeJava` | Unit | `setItem` | Dynamic slot expansion and scroll view. | Source-derived assertion. | No concurrent mutation. |
| `SetItemExpandsButDoesNotMarkDirtyWhenZeroValueIsUnchangedLikeJava` | Unit | `setItem` | Expanding zero slot does not mark dirty. | Source-derived assertion. | No live runtime. |
| `SetItemLeavesDirtyStateTrueWhenAlreadyDirtyAndValueIsUnchangedLikeJava` | Unit | `setItem` | Dirty flag remains true after prior mutation. | Source-derived assertion. | No reset behavior exists in Java. |
| `SetItemRejectsSlotsOutsideJavaRange` | Unit | `setItem` | Rejects negative and max-or-higher slots. | Source-derived guard assertion. | C# exception type differs. |
| `SwitchItemsIgnoresFoodAndDrinkSlotsLikeJava` | Unit | `switchItems` | Slot 0/1 switches are ignored. | Source-derived assertion. | No live runtime. |
| `SwitchItemsSwapsScrollSlotsAndExpandsMissingSlotLikeJava` | Unit | `switchItems` | Scroll relocation and missing-slot expansion. | Source-derived assertion. | No persistence trigger. |
| `SwitchItemsThrowsWhenStorageIsUninitializedLikeJavaNullDereference` | Unit | `switchItems` | Java null-dereference-like behavior for uninitialized bag with scroll slots. | Source-derived assertion. | C# exception message/type is not a Java runtime comparison. |
| `GetItemsReturnsCopyToAvoidExternalMutation` | Unit | C# intentional difference | Returned item array cannot mutate internal state. | Intentional-difference assertion. | Java exposes backing array directly. |

## Remaining Risks

- Helper is not wired into `PlayerPetRowProjection`.
- Helper is not wired into live `PetCommonData` or `PetService.useDoping`.
- Dirty flag persistence trigger behavior remains unported.
- Concurrent Java/C# behavior has not been stress-tested.
- Exception types differ for invalid slots and null storage switch behavior.
- Java runtime vectors and live DB comparisons are unavailable.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 model helper and 11 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: projection wiring, live pet common data, live pet doping service, dirty-flag persistence trigger, item lookup/cooldown/skill use, Java runtime vectors, DB comparisons, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Connect `PlayerPetRowProjection` doping parsing to the standalone `PetDopingBag` helper while preserving the existing projection API or documenting any intentional API shift.

## Suggested Parallel Batch For Next Session

| Candidate | Scope | Files | Risk | Parallel Safe? | Notes |
|---|---|---|---|---|---|
| A | Row projection uses `PetDopingBag` | `PlayerPetRowProjection.cs`, projection tests, docs | Medium | Writer only | Recommended next implementation unit. |
| B | `PetFeedCalculator` deterministic audit | Java read-only/docs | Medium | Yes for audit | Larger calculator with static data/random reward dependencies; keep implementation separate. |
| C | Java pet vector generator retry | docs/tooling read-only | Low | Yes | Useful if Maven/tooling becomes available. |
| D | Pet DAO live execution readiness audit | Java/C# read-only/docs | Low | Yes | Useful before enabling DB execution. |

Recommended next batch: Candidate A as one writer, optionally paired with read-only B, C, or D.

## Context Files

- Java source:
  - `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
- C# source/tests:
  - `dotnetConversion/src/Aion.GameServer/Services/ToyPet/PetDopingBag.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PetDopingBagTests.cs`
  - `dotnetConversion/src/Aion.GameServer/Data/PlayerPetRowProjection.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerPetRowProjectionTests.cs`
- Docs:
  - `docs/csharp-port.md`
  - `docs/orchestration-rules.md`
  - `docs/parallelization-strategy.md`
  - `docs/parity-verification.md`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6-BindPointTeleport-PetDopingBagHelper.md`
  - `docs/Phase-6-BindPointTeleport-PetRowProjection.md`
  - `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
