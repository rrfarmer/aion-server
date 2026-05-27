# Phase 6 Pet Row Projection Doping Bag Wiring

Date: May 27, 2026
Unit of Work: UOW-1318
Status: Non-live pet row projection now hydrates the standalone `PetDopingBag`; live pet repository execution remains disabled.

## Scope

This unit connects Java `PlayerPetsDAO.getPlayerPets` doping CSV materialization to the C# `PetDopingBag` helper added in UOW-1317.

It covers:

- replacing the projection-only doping DTO with `Aion.GameServer.Services.ToyPet.PetDopingBag`
- parsing Java comma-separated `dopings` slots through `PetDopingBag.SetItem`
- preserving Java load-time dirty behavior for non-zero DB slot values
- preserving Java zero-slot expansion without dirtying the bag

It does not execute SQL, access `DataManager`, construct a live `PetCommonData`, trigger save-on-dirty behavior, or wire live `PetService.useDoping`.

## Migration Parity Table - UOW-1318

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO.getPlayerPets` | `Aion.GameServer.Data.PlayerPetRowProjection.ProjectDopingBag` | Repository Projection | Partial | Unit Tested | Partial Parity | Doping CSV now hydrates the standalone `PetDopingBag` by calling `SetItem(parsedId, slot)` like Java. SQL execution, Java outer catch/partial-list return, logging, live `DataManager` template lookup, and runtime DB comparison remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `Aion.GameServer.Services.ToyPet.PetDopingBag` via `PlayerPetLoadedProjection.DopingBag` | Model Helper / Projection Target | Complete for helper, Partial for integration | Unit Tested | Partial Parity | Row projection now exposes the live helper instead of the prior projection DTO. Non-zero DB values mark the bag dirty like Java `setItem`; all-zero CSV expands storage without dirtying. Live common-data ownership, persistence triggers, packet builders, and `PetService.useDoping` remain unported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Data.PlayerPetLoadedProjection` | DTO / Model Projection | Partial | Unit Tested | Partial Parity | The loaded projection now carries `PetDopingBag?` for future common-data wiring. Full Java constructor template setup, synchronized runtime mutation ownership, volatile task fields, expirable callbacks, packet sends, and DAO list materialization remain unported. |
| `com.aionemu.gameserver.services.toypet.PetService.useDoping` | future C# live pet doping runtime | Service | Not Started | Manual Only | Needs Verification | Newly hydrated bag state is still non-live. Item lookup, slot validation, skill/cooldown use, inventory mutation, dirty save trigger, and `SM_PET.SPECIAL_FUNCTION` dispatch remain blocked. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProjectDopingBagParsesJavaCsvSlotOrder` | Unit | `PlayerPetsDAO.getPlayerPets` and `PetDopingBag.setItem` | CSV food/drink/scroll slots hydrate through `PetDopingBag`, preserve slot order, and mark dirty for non-zero loaded values. | Source-derived row projection assertion. | No live DAO execution or save trigger. |
| `ProjectDopingBagReturnsEmptyBagForNullCsvLikeJava` | Unit | `PlayerPetsDAO.getPlayerPets` null `dopings` guard | Null DB CSV returns an empty non-dirty bag. | Source-derived row projection assertion. | No live template lookup or common-data ownership. |
| `ProjectDopingBagZeroCsvExpandsWithoutDirtyLikeJavaSetItem` | Unit | `PetDopingBag.setItem(0, slot)` | All-zero CSV expands storage and scroll view without marking dirty. | Source-derived helper assertion through row projection. | No Java runtime DB comparison. |
| `ProjectSkipsDopingBagWhenTemplateDoesNotHaveDopingFunction` | Unit | `PlayerPetsDAO.getPlayerPets` null doping-function guard | Projection still omits doping state when the caller says the template lacks `DOPING`. | Source-derived row projection assertion. | Template lookup is supplied, not live `DataManager`. |
| `ProjectDopingBagRejectsMoreThanJavaMaxSlots` | Unit | `PetDopingBag.setItem` max-slot guard | More than eight CSV slots are rejected. | Source-derived guard assertion. | C# exception type differs from Java `IllegalArgumentException`. |
| `ProjectDopingBagRejectsMalformedCsvLikeJavaParseInt` | Unit | `Integer.parseInt` in `PlayerPetsDAO.getPlayerPets` | Malformed CSV throws during projection. | Source-derived guard assertion. | Full Java DAO outer catch/partial-list behavior is not modeled. |

## Remaining Risks

- Live `PlayerPetsDAO.getPlayerPets` execution is not implemented.
- Java outer catch logs and returns pets accumulated before a row failure; this helper projects one row and throws.
- Template lookup is supplied by `PlayerPetProjectionOptions` instead of Java `DataManager.PET_DATA`.
- The `PlayerPetLoadedProjection.DopingBag` API changed intentionally from a projection DTO to `PetDopingBag`; this is local to the non-live projection surface so far.
- Dirty flag behavior during DB load is now represented, but no live save trigger or dirty reset behavior exists.
- Java `getItems()` exposes the backing array while C# `GetItems()` returns a copy by intentional helper design.
- Concurrent Java/C# behavior has not been stress-tested.
- Timestamp binding/timezone behavior for database reads is still unverified.
- Java runtime DB comparison is unavailable.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 projection integration update and 6 focused projection/helper tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: live pet repository execution, Java template lookup, full common-data model, DAO outer-catch partial-list behavior, dirty-flag persistence trigger, live pet doping service, Java runtime DB comparison, and socket dispatch
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Audit or port the next deterministic pet prerequisite, likely `PetFeedCalculator` source-derived reward/threshold behavior, before enabling live feed or doping runtime mutation.
