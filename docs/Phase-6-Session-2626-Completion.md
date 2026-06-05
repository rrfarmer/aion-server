# Phase 6 Session 2626 Completion

## UOW

[Phase 6] UOW-2626: Execute pet spawn live from `CM_PET`.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET parsed pet actions, but live connection dispatch did not execute SPAWN, so active pet merchant sell still required manual active-pet/world-pet setup.
- Java source/runtime path: CM_PET.runImpl SPAWN calls PetSpawnService.summonPet(player, templateId); PetSpawnService routes to VisibleObjectSpawner.spawnPet(Player, int), which requires player.getPetList().getPet(templateId), resolves DataManager.PET_DATA.getPetTemplate(templateId), creates a Pet at the player's position, spawns it in the world, and stores it on Player.
- C# runtime artifact wired: GameServerConnection now dispatches CmPet SPAWN, Player exposes Java-style owned pet lookup by template id, and a production WorldPet object is registered for the spawned pet using runtime PetTemplates.
- Client-visible/state/persistence effect: a live CM_PET SPAWN mutates Player.HasPetSummon/PetSummonObjectId/PetSummonNpcId, registers a world pet carrying merchant function facts, and sends a real SM_PET spawn packet. The spawned pet can immediately serve as the live CM_BUY_ITEM action 17 seller.
- Why this is runtime progress: this UOW wires a deferred client packet path, mutates live player/world state, sends a real server packet, and enables an existing live sell mutation path; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `runImpl` handles `SPAWN` by calling `PetSpawnService.summonPet(player, templateId)`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetSpawnService.java`
  - Guards same-template respawn, removes a different current pet, schedules pet update work, and calls `VisibleObjectSpawner.spawnPet`.
- `game-server/src/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java`
  - `spawnPet(Player, int)` resolves owned `PetCommonData`, loads the `PetTemplate`, creates the `Pet`, spawns it at the player's position, and stores it on the player.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetList.java`
  - Stores owned pets by template id and exposes `getPet(templateId)`.
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - Loads persisted `player_pets` rows into `PetCommonData`; production C# hydration remains a follow-up gap.

## C# Changes

- Added `PlayerOwnedPet` and `Player.OwnedPets` with `GetOwnedPet(templateId)` to model the Java `PetList.getPet(templateId)` spawn guard.
- Added production `WorldPet` implementing `IWorldPetObject`.
- Added live `CmPet` dispatch in `GameServerConnection`.
- Implemented `CM_PET` SPAWN handling:
  - requires an owned pet entry for the requested template id,
  - resolves the runtime pet template from `StaticData.PetTemplates`,
  - removes any previous active summoned object,
  - mutates active player pet fields,
  - registers a `WorldPet` at the player's current position,
  - sends `SM_PET` spawn.
- Extended focused tests to prove spawn packet/state effects and action 17 sell after spawn.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetSpawnMutatesActivePetStateAndSendsSpawnPacket` | Unit/live handler | `CM_PET.runImpl` SPAWN plus `VisibleObjectSpawner.spawnPet` | Live `CM_PET` SPAWN mutates player active-pet state, registers a production `WorldPet`, and sends `SM_PET` spawn with position/master/decoration fields. | Runtime state, world object, and serialized packet assertions. | Owned pet list is test-populated; DB hydration is not covered. |
| `ProcessPacketAsync_CmPetSpawnEnablesMerchantSellActionSeventeenLive` | Unit/live handler | `PetSpawnService.summonPet` plus `CM_BUY_ITEM` pet action 17 | A spawned merchant pet becomes the live seller for action 17 and drives item deletion, Kinah increase, persistence capture, and sell packets. | End-to-end packet sequence through spawn then buy-item sell using the spawned pet object id. | Uses in-memory repository and template fixtures, not live MySQL/client. |

## Validation Decision

```text
- Changed surface: live CM_PET packet dispatch, player pet runtime state, world pet registration, and the previously ported pet merchant sell path.
- Specific behavior/contract: CM_PET SPAWN for an owned pet should create active player/world pet state, emit SM_PET spawn, and allow CM_BUY_ITEM action 17 to use that pet as the merchant seller.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmPetTests|FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~StaticDataPetTemplateTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java test fixture was discovered for CM_PET spawn or pet merchant sell chaining.
- Broad-validation trigger: live packet dispatch and player/world state changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered CM_PET parsing, live connection dispatch, SM_PET spawn serialization, pet-template loading, and action 17 sell side effects.
- Why this scope is sufficient: the focused tests prove the new live handler effects and the immediate downstream runtime behavior this UOW unlocks.
```

Result: passed, 78/78.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET#runImpl` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetAsync` | Live packet handler path | Partial | Unit Tested | Partial Parity | SPAWN is live; DISMISS, SURRENDER, auto-sell, auto-loot, and adopted-pet flows remain incomplete. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner#spawnPet` | `GameServerConnection.HandlePetSpawnAsync` plus `WorldPet` | Runtime world/player state | Partial | Unit Tested | Partial Parity | Creates active player/world pet state and SM_PET spawn for owned pets; full Pet object, known-list, movement, and scheduler parity remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.PetList#getPet` | `Player.OwnedPets` / `Player.GetOwnedPet` | Runtime player state | Partial | Unit Tested | Partial Parity | Template-id lookup exists for spawn gating; persisted `player_pets` hydration is not wired yet. |

## Summary Metrics

- Java artifacts discovered/touched: 5.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- `Player.OwnedPets` is not yet hydrated from the existing database shape during production enter-world.
- Full Java `PetCommonData` fields such as mood, feed progress, doping, expire time, birthday, and reuse timers are not represented.
- Pet update scheduling, movement/known-list behavior, and full world visibility remain incomplete.
- `CM_PET` DISMISS, SURRENDER, auto-sell, auto-loot, and adopt-related paths remain deferred.
- Spawn-side Java auto-loot/auto-sell follow-up packets are not sent.
- Live MySQL and real-client validation were not run.
