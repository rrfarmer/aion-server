# Phase 6 Pet Repository Command Plan

Date: May 27, 2026
Unit of Work: UOW-1315
Status: Non-executing Java-shaped pet repository SQL plans added; live DB execution remains disabled.

## Scope

This unit maps Java `PlayerPetsDAO` SQL into deterministic C# command plans:

- `saveFeedStatus`
- `saveDopingBag`
- `setTime`
- `insertPlayerPet`
- `removePlayerPet`
- `getPlayerPets`
- `updatePetName`
- `savePetMoodData`
- `getUsedIDs`

No live MySQL execution was added. The command plans preserve SQL text and positional parameter order for future repository implementation and integration tests.

## Migration Parity Table - UOW-1315

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO` | `Aion.GameServer.Data.PlayerPetsRepositoryPlan` | Repository Plan / SQL Contract | Partial | Unit Tested | Partial Parity | Java SQL text and parameter ordering are captured for feed status, doping bag CSV, reuse time, insert, delete, load, rename, mood save, and used-id load. No live DB execution, row materialization, exception logging, null fallback, scrollable result-set behavior, or transaction behavior is ported. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `Aion.GameServer.Data.PlayerPetInsertRow` / `PlayerPetMoodData` | DTO / Persistence Input | Partial | Unit Tested | Needs Verification | DTOs carry persistence inputs only. Full common data, template-driven feed/doping presence, birthday/despawn defaults, and runtime mutation remain unported. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag` | `PlayerPetsRepositoryPlan.SaveDopingBag` item-id list | Model / Persistence Projection | Partial | Unit Tested | Needs Verification | C# joins supplied ids with commas in Java slot order. Live `PetDopingBag` slot mutation, dirty flag, food/drink/scroll naming, and malformed CSV load behavior remain unported. |
| `com.aionemu.gameserver.services.toypet.PetHungryLevel` | `Aion.GameServer.Services.ToyPet.PetHungryLevel` plus repository plan `hungry_level` parameter | Enum / Persistence Field | Partial | Existing Unit Tested | Partial Parity | Repository plan preserves integer field. Live mapping from DB `hungry_level` to enum is not wired. |
| `com.aionemu.gameserver.services.toypet.PetFeedProgress` | `Aion.GameServer.Services.ToyPet.PetFeedProgress` plus repository plan `feed_progress` parameter | Model / Persistence Field | Partial | Existing Unit Tested | Partial Parity | Repository plan preserves packed feed-progress field. Live loading/saving into common data is not wired. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `SaveFeedStatusUsesJavaSqlAndParameterOrder` | Unit | `PlayerPetsDAO.saveFeedStatus` | SQL text and hungry/feed/reuse/id parameter order. | Source-derived SQL assertion. | No DB execution. |
| `SaveDopingBagSerializesJavaCommaSeparatedSlotOrder` | Unit | `PlayerPetsDAO.saveDopingBag` | Comma-separated doping slot string and id parameter. | Source-derived SQL/projection assertion. | Does not load malformed CSV or live bag state. |
| `SetTimeUsesJavaReuseTimeSql` | Unit | `PlayerPetsDAO.setTime` | Reuse-time update SQL and parameter order. | Source-derived SQL assertion. | No DB execution. |
| `InsertPlayerPetUsesJavaColumnOrder` | Unit | `PlayerPetsDAO.insertPlayerPet` | Insert SQL and seven Java column parameters. | Source-derived SQL assertion. | Does not verify MySQL timestamp binding. |
| `RemovePlayerPetUsesJavaDeleteSql` | Unit | `PlayerPetsDAO.removePlayerPet` | Delete SQL and id parameter. | Source-derived SQL assertion. | No DB execution. |
| `LoadPlayerPetsUsesJavaSelectAllByPlayerSql` | Unit | `PlayerPetsDAO.getPlayerPets` | Select-all SQL and player-id parameter. | Source-derived SQL assertion. | No row materialization. |
| `UpdatePetNameUsesJavaNameSqlAndParameterOrder` | Unit | `PlayerPetsDAO.updatePetName` | Rename SQL and name/id parameter order. | Source-derived SQL assertion. | No DB execution. |
| `SavePetMoodDataUsesJavaColumnOrder` | Unit | `PlayerPetsDAO.savePetMoodData` | Mood/counter/cooldown/despawn/id update order. | Source-derived SQL assertion. | Does not verify timestamp binding or return false on exception. |
| `LoadUsedIdsUsesJavaPetIdSqlWithoutParameters` | Unit | `PlayerPetsDAO.getUsedIDs` | Used-id select SQL and no parameters. | Source-derived SQL assertion. | Does not model Java scrollable result set or null-on-SQL-exception behavior. |

## Remaining Risks

- Live pet repository execution is not implemented.
- `getPlayerPets` row materialization into live common data is not implemented.
- Java exception handling logs and returns null/false/list depending on method; C# command plans do not model those runtime fallbacks.
- MySQL timestamp binding and timezone behavior remain unverified.
- Doping CSV parse/load behavior remains unported.
- Transaction/autocommit behavior has not been runtime-tested.
- Java runtime DB comparison is unavailable.

## Next Recommended Unit of Work

Audit `PlayerPetsDAO.getPlayerPets` row materialization and create a non-live row projection helper that can hydrate `PetCommonDataTiming`, `PetFeedProgress`, and future doping-bag DTOs without executing database reads yet.
