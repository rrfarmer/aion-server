# Phase 6 Session 2627 Completion

## UOW

[Phase 6] UOW-2627: Restore owned pets during enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET SPAWN now consumes Player.OwnedPets, but production enter-world did not restore owned pets from player_pets, so real persisted pets could not be spawned without manual/test setup.
- Java source/runtime path: Player construction creates PetList; PetList.loadPets calls PlayerPetsDAO.getPlayerPets; PlayerPetsDAO.getPlayerPets reads SELECT * FROM player_pets WHERE player_id = ? and materializes PetCommonData rows keyed by template id.
- C# runtime artifact wired: IPlayerEnterWorldRepository now exposes LoadPlayerPetsAsync, MySqlPlayerEnterWorldRepository reads player_pets rows through the existing Java-shaped projection, and PlayerEnterWorldService assigns Player.OwnedPets before the player enters the world.
- Client-visible/state/persistence effect: enter-world restores persisted owned-pet state into the live Player object, allowing a later CM_PET SPAWN for that template id to register a world pet, send SM_PET spawn, and feed CM_BUY_ITEM action 17 pet merchant sell.
- Why this is runtime progress: this UOW restores runtime state from the existing database shape and wires it into live enter-world player state consumed by a live client packet handler; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetList.java`
  - Constructor calls `loadPets(player)`.
  - `loadPets` calls `PlayerPetsDAO.getPlayerPets(player)` and stores rows by `pet.getTemplateId()`.
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `getPlayerPets` selects from `player_pets` by `player_id` and fills `PetCommonData` fields including id, template id, name, decoration, feed, doping, birthday, mood, gift cooldown, and despawn time.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - Constructor binds object id, template id, master object id, expire time, and initializes template-dependent optional state.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - SPAWN path consumes `player.getPetList().getPet(templateId)` through `PetSpawnService`.

## C# Changes

- Added `IPlayerEnterWorldRepository.LoadPlayerPetsAsync`.
- Added empty-repository default for pet restore.
- Implemented `MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` against `player_pets` using Java-shaped row projection.
- Wired `PlayerEnterWorldService.EnterWorldAsync` to assign `player.OwnedPets` before adding the player to the world.
- Extended `PlayerEnterWorldServiceTests.EnterWorld_MarksPlayerOnlineAndStoresInWorld` to prove restored owned-pet state is present on the live player.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `EnterWorld_MarksPlayerOnlineAndStoresInWorld` | Unit/live service | `PetList.loadPets` plus `PlayerPetsDAO.getPlayerPets` | Enter-world calls the repository pet loader and assigns restored pet state to `Player.OwnedPets`. | Runtime service assertion over player state after successful enter-world. | Uses in-memory repository, not live MySQL. |
| Existing `PlayerPetRowProjectionTests` | Unit/projection | `PlayerPetsDAO.getPlayerPets` row materialization | Java-shaped `player_pets` rows hydrate basic and partial common-data fields. | Focused projection tests over row fields and Java-like null/default behavior. | Full template-dependent feed/doping runtime integration remains partial. |
| Existing `ProcessPacketAsync_CmPetSpawnEnablesMerchantSellActionSeventeenLive` | Unit/live handler | `CM_PET` SPAWN plus `CM_BUY_ITEM` pet action 17 | Hydrated `Player.OwnedPets` shape remains compatible with live spawn and sell path. | Existing live packet chain proves the restored fields are the fields consumed downstream. | The test still injects the player rather than passing through full `CM_ENTER_WORLD` packet sequence. |

## Validation Decision

```text
- Changed surface: live enter-world player state restore plus repository database load surface.
- Specific behavior/contract: persisted player_pets rows should hydrate Player.OwnedPets during enter-world and remain consumable by live CM_PET spawn/action-17 pet sell.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PlayerPetsDAO.getPlayerPets or PetList.loadPets.
- Broad-validation trigger: live enter-world/player runtime state and persistence load boundary changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered enter-world hydration, row projection, CM_PET parsing, live spawn, and downstream action 17 sell.
- Why this scope is sufficient: the focused tests prove the edited live service state effect and the already-live packet path that consumes the restored state.
```

Result: passed, 151/151.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dao.PlayerPetsDAO#getPlayerPets` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` / `PlayerPetRowProjection` | Repository | Partial | Unit Tested | Partial Parity | Basic row fields are loaded into live owned-pet state; full template-dependent `PetCommonData` feed/doping/mood behavior is not live yet. |
| `com.aionemu.gameserver.model.gameobjects.player.PetList#loadPets` | `Aion.GameServer.Services.PlayerEnterWorldService.EnterWorldAsync` / `Player.OwnedPets` | Runtime player state | Partial | Unit Tested | Partial Parity | Enter-world restores owned pets; expirable registration and last-used-pet tracking remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData` | `PlayerOwnedPet` plus `PlayerPetLoadedProjection` | Runtime model/projection | Partial | Unit Tested | Partial Parity | Object id, template id, name, and decoration feed live spawn; birthday, expiration, feed, doping, mood, and scheduling fields remain partial/not represented on `PlayerOwnedPet`. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Enter-world still does not send Java `SM_PET(Collection<PetCommonData>)` / `LOAD_PETS` to the client.
- `PlayerOwnedPet` intentionally carries only the fields consumed by live spawn; richer `PetCommonData` state remains partial.
- Pet expirable registration from Java `ExpireTimerTask` is not wired.
- Java last-used-pet tracking is not represented.
- Live MySQL and real-client validation were not run.
