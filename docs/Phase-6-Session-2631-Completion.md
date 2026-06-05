# Phase 6 Session 2631 Completion

## UOW

[Phase 6] UOW-2631: Execute active pet rename live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET RENAME previously parsed but did not mutate active pet state, persist the name, or send the Java rename packet.
- Java source/runtime path: CM_PET.runImpl RENAME validates NameRestrictionService, then PetService.renamePet normalizes with Util.convertName, mutates PetCommonData.name, calls PlayerPetsDAO.updatePetName, and broadcasts SM_PET RENAME.
- C# runtime artifact wired: GameServerConnection now handles PetAction.Rename, GameServerOptions loads gameserver.name.pet_pattern, PlayerEnterWorldService/MySqlPlayerEnterWorldRepository update player_pets.name, and SmSystemMessage exposes the Java invalid pet-name message.
- Client-visible/state/persistence effect: valid active-pet rename updates Player.OwnedPets and the WorldPet object, persists player_pets.name, and broadcasts SM_PET RENAME with include-source visibility; invalid names send system message 1400643 without mutation.
- Why this is runtime progress: this UOW wires a live client packet branch, mutates live player/world pet state, persists existing database state, and emits real server packets from live code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `RENAME` reads object id and name, validates `NameRestrictionService.isValidPetName` and `isForbidden`, then calls `PetService.renamePet(player, petName)`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `renamePet` applies `Util.convertName`, mutates active `PetCommonData.name`, calls `PlayerPetsDAO.updatePetName`, and broadcasts `new SM_PET(pet.getObjectId(), pet.getName())` including the source player.
- `game-server/src/com/aionemu/gameserver/dao/PlayerPetsDAO.java`
  - `updatePetName` executes `UPDATE player_pets SET name = ? WHERE id = ?`.
- `game-server/src/com/aionemu/gameserver/configs/main/NameConfig.java`
  - `gameserver.name.pet_pattern` defaults to `[a-zA-Z]{2,16}`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `RENAME` writes pet object id and pet name.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_MSG_PET_NOT_AVALIABE_NAME` uses message id `1400643`.

## C# Changes

- Added `GameServerNameOptions.PetPattern` and `CreatePetNameRegex`, loaded from `gameserver.name.pet_pattern`.
- Added `IPlayerEnterWorldRepository.UpdatePlayerPetNameAsync`.
- Added test/empty repository capture for pet-name updates.
- Implemented MySQL `player_pets.name` update in `MySqlPlayerEnterWorldRepository`, scoped by pet id and player id.
- Added `PlayerEnterWorldService.UpdatePlayerPetNameAsync`.
- Added `SmSystemMessage.PetNotAvailableName`.
- Wired `GameServerConnection.HandlePetRenameAsync` for live `PetAction.Rename`.
- Rename now validates pet names, normalizes using Java `Util.convertName` behavior, persists the active pet name, mutates owned/world pet state, and broadcasts `SM_PET RENAME`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetRenameUpdatesActivePetNamePersistsAndBroadcastsRenamePacket` | Unit/live connection | `CM_PET.runImpl -> PetService.renamePet -> PlayerPetsDAO.updatePetName -> SM_PET RENAME` | Real `CM_PET` parsing persists normalized active-pet name, mutates `Player.OwnedPets`, updates the `WorldPet`, and broadcasts Java-shaped rename packet including source player. | Asserts repository update call, Java-style name normalization, owned/world pet mutation, and serialized `SM_PET RENAME` bytes. | Does not exercise real MySQL or actual known-list recipient filtering. |
| `ProcessPacketAsync_CmPetRenameInvalidNameSendsSystemMessageWithoutMutation` | Unit/live connection | `NameRestrictionService.isValidPetName` and `STR_MSG_PET_NOT_AVALIABE_NAME` | Invalid pet name is rejected before persistence/state mutation and sends Java message id `1400643`. | Asserts no repository call, no state mutation, no visible broadcast, and system-message id. | Does not test forbidden-sequence and forbidden-word variants separately. |
| `ProcessPacketAsync_CmPetRenameWithoutActivePetDoesNothing` | Unit/live connection | `PetService.renamePet` returns when `player.getPet()` is null | Valid rename with no active pet does not persist, mutate, or send packets. | Direct live handler no-op assertion. | Does not cover mismatched active object with missing owned-pet projection beyond no-op behavior. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch, player/world pet state, GameServerOptions name config, PlayerEnterWorldService persistence wrapper, MySQL player_pets update, system-message id, and test doubles.
- Specific behavior/contract: CM_PET RENAME should validate Java pet-name rules, normalize with Util.convertName semantics, update active pet state, persist player_pets.name, and broadcast Java-shaped SM_PET RENAME.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.renamePet or SM_PET rename.
- Broad-validation trigger: live pet state plus persistence boundary changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, packet shape, service interface consumers, and repository implementer compile coverage.
- Why this scope is sufficient: the focused tests exercise the real packet parser/connection path and inspect validation, persistence call, state mutation, world update, and packet bytes for the scoped rename behavior.
```

Result: passed, 434/434.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` RENAME | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetRenameAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live rename validates, normalizes, mutates state, persists, and broadcasts rename. Other CM_PET branches remain partial/deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#renamePet` | `GameServerConnection.HandlePetRenameAsync` / `PlayerEnterWorldService.UpdatePlayerPetNameAsync` | Service/runtime lifecycle | Partial | Unit Tested | Partial Parity | Core active-pet rename path is live. C# waits for repository success before mutation; Java DAO logs failures and continues in memory. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO#updatePetName` | `MySqlPlayerEnterWorldRepository.UpdatePlayerPetNameAsync` | Repository | Partial | Unit Tested | Partial Parity | C# updates by pet id and player id for safety; Java updates by pet id only. Live MySQL validation was not run. |
| `com.aionemu.gameserver.configs.main.NameConfig` pet pattern | `GameServerNameOptions.PetPattern` | Configuration | Partial | Unit Tested | Partial Parity | Default/key are loaded for live rename validation. Runtime config-file override integration for this new property was not separately tested. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` RENAME | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes Java rename shape and live rename now broadcasts it. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE#STR_MSG_PET_NOT_AVALIABE_NAME` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.PetNotAvailableName` | Packet/message | Partial | Unit Tested | Partial Parity | Live invalid-name branch sends Java message id `1400643`; no standalone packet golden was added. |

## Summary Metrics

- Java artifacts discovered/touched: 6.
- C# artifacts changed/touched: 7.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 6.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Real MySQL and real-client rename validation were not run.
- C# requires repository update success before mutating memory; Java mutates memory even if `PlayerPetsDAO.updatePetName` logs a persistence failure.
- Full known-list recipient filtering and true client visibility around pet rename remain partial.
- `CM_PET` adopt, food/doping, auto-sell, auto-loot, mood, and expiration flows remain deferred/partial.
- Java `PetController.onDelete` persistence side effects for feed status, doping bag, mood data, despawn time, and task cancellation remain incomplete.
