# Phase 6 Pet Row Projection

Date: May 27, 2026
Unit of Work: UOW-1316
Status: Non-live pet row projection helper added; live pet repository execution remains disabled.

## Scope

This unit ports the deterministic row-materialization portion of Java `PlayerPetsDAO.getPlayerPets` into a supplied-row C# projection helper.

It covers:

- basic `PetCommonData` fields from `player_pets`
- feed-progress hydrate gating when the template has `FOOD`
- mood/refeed timing field transfer
- null `despawn_time` fallback to current time
- doping CSV parse/projection when the template has `DOPING`

It does not execute SQL, access `DataManager`, construct a live pet object, or wire repository calls into game runtime.

## Migration Parity Table - UOW-1316

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO.getPlayerPets` | `Aion.GameServer.Data.PlayerPetRowProjection` | Repository Projection | Partial | Unit Tested | Partial Parity | Projects supplied row fields into a non-live projection with feed, timing, despawn fallback, and doping data. Does not execute SQL, catch/log exceptions, preserve Java partial-list return after mid-loop failure, or access templates through `DataManager`. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Data.PlayerPetLoadedProjection` / `PetCommonDataTiming` | DTO / Model Helper | Partial | Unit Tested | Partial Parity | Basic ids/name/decoration/expiration/birthday/despawn/timing are projected. Full live common data, volatile task state, expirable behavior, packet sends, and template-owned functions remain unported. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `Aion.GameServer.Services.ToyPet.PetFeedProgress` via `PlayerPetRowProjection` | Model / Persistence Projection | Partial | Unit Tested | Partial Parity | Feed status is hydrated only when caller says template has `FOOD`, matching Java's null guard after constructor template lookup. Live template lookup and DAO execution are not wired. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `Aion.GameServer.Data.PlayerPetDopingBagProjection` | DTO / Persistence Projection | Partial | Unit Tested | Partial Parity | Parses Java comma-separated slot order and exposes food/drink/scrolls plus persistence CSV. Live synchronized `setItem`, dirty flag, switch behavior, and runtime item mutation remain unported. |
| `com.aionemu.gameserver.services.toypet.PetHungryLevel` | `Aion.GameServer.Services.ToyPet.PetHungryLevelExtensions.FromId` | Enum / Persistence Field | Complete for helper | Existing Unit Tested | Partial Parity | Unknown DB hungry-level ids reject through C# `ArgumentOutOfRangeException`; Java would throw array-index exception from `values()[value]`, caught by DAO outer catch. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProjectHydratesBasicPetFieldsLikeJavaGetPlayerPets` | Unit | `PlayerPetsDAO.getPlayerPets` | Ids, name, decoration, birthday, despawn, and no function projections. | Source-derived row projection assertion. | No SQL execution or template lookup. |
| `ProjectUsesCurrentTimeForNullDespawnTimeLikeJava` | Unit | `PlayerPetsDAO.getPlayerPets` | Null `despawn_time` fallback to current timestamp. | Source-derived deterministic fallback assertion. | Uses supplied clock. |
| `ProjectHydratesFeedProgressWhenTemplateHasFoodFunction` | Unit | `PlayerPetsDAO.getPlayerPets` | Hungry level, feed data, and reuse time hydration when feed function exists. | Source-derived row projection assertion. | No live template lookup. |
| `ProjectSkipsFeedProgressWhenTemplateDoesNotHaveFoodFunction` | Unit | `PlayerPetsDAO.getPlayerPets` | Java null-guard behavior when pet has no feed function. | Source-derived row projection assertion. | No live template lookup. |
| `ProjectHydratesMoodTimingFieldsLikeJava` | Unit | `PlayerPetsDAO.getPlayerPets` | Mood started, counter, mood cooldown, and gift cooldown fields. | Source-derived row projection assertion. | No live common-data mutation. |
| `ProjectDopingBagParsesJavaCsvSlotOrder` | Unit | `PlayerPetsDAO.getPlayerPets` / `PetDopingBag` | Food/drink/scroll slot projection and persistence CSV. | Source-derived row projection assertion. | No live synchronized bag. |
| `ProjectDopingBagReturnsEmptyBagForNullCsvLikeJava` | Unit | `PlayerPetsDAO.getPlayerPets` / `PetDopingBag` | Null DB CSV leaves empty bag with zero food/drink. | Source-derived row projection assertion. | Does not distinguish dirty flag. |
| `ProjectSkipsDopingBagWhenTemplateDoesNotHaveDopingFunction` | Unit | `PlayerPetsDAO.getPlayerPets` | Java null-guard behavior when pet has no doping function. | Source-derived row projection assertion. | No live template lookup. |
| `ProjectDopingBagRejectsMoreThanJavaMaxSlots` | Unit | `PetDopingBag.setItem` | Rejects more than eight slots. | Source-derived guard assertion. | C# exception type differs from Java `IllegalArgumentException`. |
| `ProjectDopingBagRejectsMalformedCsvLikeJavaParseInt` | Unit | `Integer.parseInt` in `PlayerPetsDAO.getPlayerPets` | Malformed CSV throws during projection. | Source-derived guard assertion. | Full DAO outer catch/partial-list behavior not modeled. |
| `ProjectRejectsUnknownHungryLevelLikeJavaEnumIndex` | Unit | `PetHungryLevel.fromId` | Unknown hungry-level id rejects. | Source-derived guard assertion. | C# exception type differs from Java array-index exception. |

## Remaining Risks

- Live `PlayerPetsDAO.getPlayerPets` execution is not implemented.
- Java outer catch logs and returns pets accumulated before a row failure; this helper projects one row and throws.
- Template lookup is supplied via projection options instead of Java `DataManager.PET_DATA`.
- Live `PetDopingBag` synchronized mutation, dirty flag, and switch behavior remain unported.
- Timestamp binding/timezone behavior for database reads is still unverified.
- Java runtime DB comparison is unavailable.

## Next Recommended Unit of Work

Port the standalone `PetDopingBag` model helper, including dynamic slot expansion, dirty flag behavior, food/drink/scroll views, and scroll-only switch semantics, then connect the projection helper to it in a later unit.
