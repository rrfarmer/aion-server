# Phase 6 Pet Doping Bag Helper

Date: May 27, 2026
Unit of Work: UOW-1317
Status: Standalone pet doping-bag helper ported; not yet wired into row projection or live pet runtime.

## Scope

This unit ports Java `PetDopingBag` behavior into a standalone C# helper:

- food/drink zero defaults
- dynamic slot expansion up to eight slots
- dirty flag changes only when the stored slot value changes
- scroll-slot views
- scroll-only `switchItems`
- invalid slot rejection

The helper is not yet used by `PlayerPetRowProjection` or live pet doping service paths.

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

## Next Recommended Unit of Work

Connect `PlayerPetRowProjection` doping parsing to the standalone `PetDopingBag` helper while preserving the existing projection API or documenting any intentional API shift.
