# Phase 6 Session 2640 Completion

## UOW

[Phase 6] UOW-2640: Execute single-count pet feeding check live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD regular feed-start now executes the accepted single-count PetService.checkFeeding branch instead of stopping after subtype 1/start-emotion packets.
- Java source/runtime path: PetService.removeObject schedules PetService.checkFeeding; checkFeeding resolves the active FOOD function flavour, validates the item food type through PetFeedData/ItemGroupsData, decrements inventory, mutates PetFeedProgress, and sends SM_PET subtype 2/subtype 5 plus END_FEEDING emotion.
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync schedules/executes a live single-count feed check using runtime PetTemplateTable, Java XML-backed PetFeedDataTable, ItemTemplateTable, inventory mutation, PlayerEnterWorldService, and MySqlPlayerEnterWorldRepository.
- Client-visible/state/persistence effect: accepted single-count pet food decrements the live inventory stack, advances active PlayerOwnedPet feed progress/hungry state, persists player_pets hungry_level/feed_progress/reuse_time plus inventory count/delete mutation, sends inventory update/delete, sends SM_PET subtype 2 and subtype 5, and sends END_FEEDING emotion.
- Why this is runtime progress: this mutates live inventory and pet state, persists runtime state using the existing database shape, loads Java XML/static item-group data into runtime-used C# structures, and sends real server packets from the live client handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `removeObject` validates the item/count, clears cancel-feed, sends `SM_PET(1, itemObjectId, count, pet)`, sends `SM_EMOTION START_FEEDING`, then schedules `checkFeeding`.
  - `checkFeeding` resolves the active FOOD pet function id, uses `DataManager.PET_FEED_DATA` and `DataManager.ITEM_GROUPS_DATA`, consumes one inventory item, mutates feed progress, and sends subtype 2 followed by subtype 5 plus END_FEEDING when no requested count remains.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetFlavour.java`
  - `getFoodType` scans configured food reward groups in order.
- `game-server/src/com/aionemu/gameserver/dataholders/ItemGroupsData.java`
  - pet-food item groups and `EXCLUDES`/`STINKY` filtering are the source of truth for item-to-food-type lookup.

## C# Changes

- Added `PetFeedDataTable` as a runtime wrapper around existing ToyPet feed evaluation context.
- Extended `StaticData` to load `pet_feed.xml` flavours and `item_groups.xml` pet food groups into `StaticData.PetFeedData`.
- Added `PlayerOwnedPet.HungryLevel` and restored it from the `player_pets.hungry_level` projection during live enter-world pet load.
- Added `IPlayerEnterWorldRepository.SavePlayerPetFeedConsumeMutationAsync` plus service/MySQL implementations that transactionally persist inventory decrement/delete and `player_pets` feed fields.
- Extended `GameServerConnection.HandlePetFoodAsync`:
  - feed-start still sends Java-shaped subtype 1 and START_FEEDING first,
  - with a scheduler, accepted single-count feed checks run after Java's 2500 ms delay,
  - without a scheduler, tests execute the check immediately,
  - successful persistence gates live inventory/pet mutation and subtype 2/subtype 5/end-emotion packets.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetFoodSingleCountConsumesFoodPersistsFeedStatusAndSendsProgressEndPackets` | Unit/live connection | `PetService.checkFeeding` accepted one-item branch | Live handler decrements inventory, persists feed mutation, updates pet progress/hungry state, and sends inventory/subtype 2/subtype 5/end-emotion packets. | Direct packet/state/repository capture from `ProcessPacketAsync`. | Does not cover chained count, rejected food, reward/refeed, or a real client. |
| `ProcessPacketAsync_CmPetFoodPersistenceFailureLeavesFoodAndFeedStateUnchangedAfterStart` | Unit/live connection | Java consumes only after checkFeeding; C# gates mutation on persistence | Persistence failure leaves inventory/feed state unchanged after the already-sent start packets. | Negative side-effect ordering coverage. | Conservative C# failure ordering; Java DAO timing differs. |
| `DataManager_LoadsRealJavaStaticDataManifestCounts` | Static data loader | Java XML corpus and C# manifest loader | Real static data still loads with pet feed/item group parsing enabled. | Manifest-backed loader test passes. | Does not assert every pet feed row against Java objects. |

## Validation Decision

```text
- Changed surface: live CM_PET FOOD feed check, player-owned pet feed state, inventory mutation packets, StaticData runtime loading, PlayerEnterWorldService, and IPlayerEnterWorldRepository/MySQL repository.
- Specific behavior/contract: accepted single-count food consumes one item, advances pet feed packet data, persists feed/inventory mutation, and sends Java-shaped subtype 2/subtype 5/end-emotion packets only after persistence succeeds.
- Focused C# command 1: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused C# command 2: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts --no-restore
- Focused Java/Maven command: not run; no narrow Java PetService.checkFeeding fixture or game-server/src/test tree was found in this checkout.
- Broad-validation trigger: live inventory mutation, pet feed persistence, and shared repository interface changed.
- Broad .NET decision: skipped after focused coverage because the filtered connection run built affected projects and covered live packet/state/persistence ordering, while the real static data loader test covered manifest-backed XML load.
```

Results:

- `GameServerConnectionBuyItemTests`: passed, 77/77.
- `StaticDataLoadingTests.DataManager_LoadsRealJavaStaticDataManifestCounts`: passed, 1/1.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PetService.removeObject` feed-start scheduling | `GameServerConnection.HandlePetFoodAsync` + `ThreadPoolManager.Schedule` | Runtime handler/scheduler | Partial | Unit Tested | Partial Parity | Scheduler delay is wired when present; tests execute immediate no-scheduler path. |
| `PetService.checkFeeding` accepted single-count branch | `ExecutePetFeedingCheckAsync` | Runtime mutation/packets | Partial | Unit Tested | Partial Parity | Consumed-stop branch is live. Rejected, continue, reward/refeed branches remain open. |
| `PetFeedData` + `ItemGroupsData.isFood` | `StaticData.PetFeedData` + `PetFoodItemGroups` | Runtime static data | Partial | Static Loader Tested | Partial Parity | XML load is runtime-used. Row-by-row Java comparison remains open. |
| `PlayerPetsDAO.saveFeedStatus` | `SavePlayerPetFeedConsumeMutationAsync` | Persistence | Partial | Unit Tested | Partial Parity | Persists feed fields immediately with inventory mutation. Java saves feed status at pet despawn for persisted pet state. |
| `PetFeedProgress` | `PetFeedProgress` + `PlayerOwnedPet.HungryLevel` | State/packet data | Partial | Unit Tested | Partial Parity | Existing feed calculator is reused; live pet record now tracks hungry level. |

## Known Gaps

- Multi-count feed chaining (`ConsumedContinue`) remains deferred.
- Rejected/non-eatable food branch remains deferred, including item unlock and system message.
- Reward/full/refeed branch remains deferred, including reward item creation, refeed scheduling, and feed reset.
- Java random loved reward selection is not live-wired; the current single-count accepted branch avoids reward execution.
- Real MySQL execution was not run locally; repository method compiles and is covered through unit capture, but no gated DB integration was added in this UOW.
- Real client validation was not run.
