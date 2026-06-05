# Phase 6 Session 2639 Completion

## UOW

[Phase 6] UOW-2639: Persist live pet doping bag mutations.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD actionType 2 dopeActions 0/1/2 previously mutated active PlayerOwnedPet.DopingItemIds only in memory.
- Java source/runtime path: PlayerPetsDAO.saveDopingBag(int petObjectId, PetDopingBag bag) writes food, drink, then scroll slots into player_pets.dopings; PetDopingBag marks dirty only when slot contents change.
- C# runtime artifact wired: GameServerConnection.HandlePetDopingAsync now persists changed doping slot lists through PlayerEnterWorldService and MySqlPlayerEnterWorldRepository before mutating memory or sending success packets.
- Client-visible/state/persistence effect: successful live pet doping add/remove/switch now stores the changed slot CSV in player_pets.dopings, so the existing enter-world restore path can hydrate the updated bag later; persistence failure blocks mutation and packet send.
- Why this is runtime progress: this UOW persists live runtime pet state using the existing database shape and wires a live client-handler mutation path to repository execution.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `saveDopingBag` updates `player_pets.dopings` using `bag.getFoodItem() + "," + bag.getDrinkItem()` followed by `bag.getScrollsUsed()`.
  - `getPlayerPets` restores non-null `dopings` by splitting CSV and calling `PetDopingBag.setItem(Integer.parseInt(ids[i]), i)`.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `setItem` grows the backing slot array and marks dirty only when a slot value changes.
  - `switchItems` returns without mutation when either slot is food/drink.
- `game-server/src/com/aionemu/gameserver/controllers/PetController.java`
  - On despawn, dirty doping bags are persisted through `PlayerPetsDAO.saveDopingBag`.

## C# Changes

- Added `IPlayerEnterWorldRepository.SavePlayerPetDopingBagAsync`.
- Added `PlayerEnterWorldService.SavePlayerPetDopingBagAsync`.
- Added live MySQL repository execution:
  - writes `UPDATE player_pets SET dopings = ? WHERE id = ? AND player_id = ?`,
  - serializes item ids using Java slot order CSV,
  - returns false on update failure or exception.
- Extended `GameServerConnection.HandlePetDopingAsync`:
  - detects changed slot lists for add/remove/switch,
  - persists changed lists before mutating active owned-pet state,
  - suppresses mutation and success packet when persistence fails,
  - keeps Java no-op dirty semantics for unchanged valid actions such as food-slot switch.
- Extended test repositories to capture pet doping persistence calls.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetDopingSwitchMutatesSlotsAndSendsPacket` | Unit/live connection | `PetDopingBag.switchItems` plus `PlayerPetsDAO.saveDopingBag` | Changed switch persists Java slot CSV, mutates only the active pet, and sends DOPING action 2. | Captures repository call and live packet/state effects. | Does not run against a real client. |
| `ProcessPacketAsync_CmPetDopingSwitchWithFoodSlotSendsPacketWithoutMutatingSlots` | Unit/live connection | `PetDopingBag.switchItems` no-op for food/drink slots | Valid no-op switch sends the existing packet but does not persist unchanged slots. | Captures dirty/no-dirty distinction. | Java audit/logging is not involved. |
| `ProcessPacketAsync_CmPetDopingAddMutatesSlotAndSendsPacket` | Unit/live connection | `PetService.useDoping` action 0 and `PlayerPetsDAO.saveDopingBag` | Add persists `[food,drink,scroll]`, mutates memory, and sends DOPING add. | Live handler plus repository capture. | Inventory item existence remains out of scope for action 0. |
| `ProcessPacketAsync_CmPetDopingRemoveMutatesSlotAndSendsPacket` | Unit/live connection | `PetService.useDoping` action 1 and `PlayerPetsDAO.saveDopingBag` | Remove persists `[food,0]`, mutates memory, and sends DOPING remove. | Live handler plus repository capture. | Real DB execution is covered only by gated integration test. |
| `ProcessPacketAsync_CmPetDopingAddBlocksMutationAndPacketWhenPersistenceFails` | Unit/live connection | Java DAO failure is logged and does not prove client success | Repository failure blocks C# mutation and success packet, keeping memory/client aligned with persistence. | Negative live side-effect ordering coverage. | This is a conservative C# failure policy; Java despawn save timing differs. |
| `SavePlayerPetDopingBagAsync_WritesJavaCsvAgainstJavaSchema_WhenEnabled` | Gated DB integration | `PlayerPetsDAO.saveDopingBag` SQL/CSV shape | When DB integration is enabled, repository writes Java CSV into `player_pets.dopings`. | Compiled and available against Java schema. | Skipped unless `AION_GAMESERVER_DB_INTEGRATION=1`. |

## Validation Decision

```text
- Changed surface: live CM_PET doping handler, player-owned pet state mutation ordering, PlayerEnterWorldService, IPlayerEnterWorldRepository, MySqlPlayerEnterWorldRepository, and database-shape coverage.
- Specific behavior/contract: changed active pet doping slots should persist to player_pets.dopings in Java food/drink/scroll order before memory mutation and success packet emission.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~PetDopingBagTests|FullyQualifiedName~PlayerPetsRepositoryPlanTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --no-restore
- Focused Java/Maven command: not run; there is no game-server/src/test directory or narrow Java PlayerPetsDAO/PetDoping fixture in this checkout.
- Broad-validation trigger: live pet persistence and shared repository interface changed.
- Broad .NET decision: skipped after focused coverage because the filtered run built affected projects and covered live connection behavior, repository capture/failure ordering, row projection restore, bag dirty semantics, SQL plan shape, and the gated DB integration path.
- Why this scope is sufficient: the affected live path and persistence contract are directly exercised; unrelated repository consumers compiled through the interface change.
```

Result: passed, 120/120. Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `PlayerPetsDAO.saveDopingBag` | `MySqlPlayerEnterWorldRepository.SavePlayerPetDopingBagAsync` | Persistence | Partial | Unit + Gated Integration | Partial Parity | CSV order is ported. C# keeps existing player_id guard used by local pet repository methods. |
| `PetController.onDespawn` dirty save trigger | `GameServerConnection.HandlePetDopingAsync` immediate changed-slot save | Runtime persistence trigger | Intentional Difference | Unit Tested | Partial Parity | C# persists when the live mutation happens to close the current runtime restore gap; Java saves dirty bag on despawn. |
| `PetDopingBag` dirty semantics | `GameServerConnection` changed-list persistence guard | State behavior | Partial | Unit Tested | Partial Parity | Changed slot lists persist; valid unchanged switch does not persist. |
| `PlayerPetsDAO.getPlayerPets` doping restore | `PlayerPetRowProjection.ProjectDopingBag` and enter-world pet load | Persistence restore | Partial | Unit Tested | Partial Parity | Restore path existed before this UOW and now has a live save source. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 6.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Java saves dirty doping bags during pet despawn; C# now saves immediately after changed live slot mutations to ensure restored state, which is a documented timing difference.
- Doping use action 3 remains deferred because Java uses spawned-state checks, delayed scheduling, item-use restrictions, cooldowns, skill effects, inventory decrement, and packet effects.
- Java audit logging for unsupported doping configuration attempts is not implemented.
- Full Java static-data corpus validation was not run.
- Real client validation was not run.
- Real MySQL validation was not run in this session; a gated integration test was added for environments with `AION_GAMESERVER_DB_INTEGRATION=1`.
