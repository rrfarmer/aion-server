# Phase 6 Session 2628 Completion

## UOW

[Phase 6] UOW-2628: Send restored pet list during enter-world.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: enter-world restored Player.OwnedPets, but the client was not notified with Java's pet-list login packet.
- Java source/runtime path: PlayerEnterWorldService.onLogin calls PetService.getInstance().onPlayerLogin(player); PetService.onPlayerLogin sends new SM_PET(player.getPetList().getPets()) when the collection is not empty; SM_PET.writeImpl LOAD_PETS writes count and writePetData rows.
- C# runtime artifact wired: GameServerConnection enter-world packet sequence now maps PlayerOwnedPet state through SmPetDataSnapshot and sends SmPet.LoadPets after SM_STATS_INFO and before mailbox/macro/recipe login packets.
- Client-visible effect: a player with restored persisted pets receives a real SM_PET LOAD_PETS packet during live CM_ENTER_WORLD processing, including object/template/name/master/birthday/decoration and Java-ordered packet-writable static pet functions.
- Why this is runtime progress: this UOW sends a real server packet from live enter-world code using restored runtime state and Java-loaded pet templates; it is not preview-only, test-only, or documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/player/PlayerEnterWorldService.java`
  - `onLogin` calls `PetService.getInstance().onPlayerLogin(player)` before mail, macro, and recipe login packets.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `onPlayerLogin` sends `new SM_PET(player.getPetList().getPets())` when the pet collection is not empty.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - `LOAD_PETS` writes the list header and `writePetData` for each `PetCommonData`.
  - `writePetData` writes packet-writable functions in Java order: warehouse, loot, doping, food, then pads missing function slots with `NONE`, then writes appearance.

## C# Changes

- Extended `PlayerOwnedPet` with packet fields needed by Java `SM_PET.writePetData`: master object id, birthday, expiration, feed progress, refeed time, and doping item ids.
- Preserved richer pet fields in `MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync`, using runtime pet templates to decide whether feed and doping state should be projected.
- Added live enter-world `SmPet.LoadPets` send in `GameServerConnection` immediately after `SmStatsInfo`, matching the Java login ordering before mailbox/macro/recipe packets.
- Added owned-pet-to-`SmPetDataSnapshot` mapping that uses Java-loaded `PetTemplateTable` function data to emit packet-writable warehouse/loot/doping/food function snapshots.
- Extended `EmptyPlayerEnterWorldRepository` with opt-in loaded-player, loaded-pets, and mark-online results for connection-level enter-world tests without changing its default empty behavior.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmEnterWorldSendsRestoredPetListAfterStats` | Unit/live connection | `PlayerEnterWorldService.onLogin -> PetService.onPlayerLogin -> SM_PET(Collection<PetCommonData>)` | Real `CM_ENTER_WORLD` parsing and connection handling sends `SM_PET LOAD_PETS` after `SM_STATS_INFO` for restored owned pets. | Serializes the emitted `SmPet` and asserts action, count, pet data, warehouse function, `NONE` padding, appearance, birthday, object ids, and decoration. | Uses in-memory repository/static template fixture, not live MySQL or a real client. |
| Existing `SmPet` packet tests | Unit/packet | `SM_PET.writeImpl` / `writePetData` | Existing Java-shaped packet writer remains compatible with load-pets and function ordering. | Included in focused validation through `GamePacketTests`. | No new Java golden fixture was discovered. |
| Existing enter-world and pet tests | Unit/service/handler | `PlayerPetsDAO.getPlayerPets`, `PetList.loadPets`, `CM_PET` SPAWN | Restored pet model remains compatible with service restore, live spawn, and pet merchant sell. | Included in focused validation through enter-world, row projection, and connection tests. | Full pet common-data runtime remains partial. |

## Validation Decision

```text
- Changed surface: live enter-world packet sequence, restored pet runtime model, MySQL pet projection, and connection test fixture.
- Specific behavior/contract: restored owned pets should produce Java-shaped SM_PET LOAD_PETS during live CM_ENTER_WORLD handling, after stats and before mailbox/macro/recipe login packets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerPetRowProjectionTests|FullyQualifiedName~GamePacketTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.onPlayerLogin or SM_PET load-pets.
- Broad-validation trigger: live enter-world packet sequence changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered connection-level CM_ENTER_WORLD handling, packet serialization, enter-world restore, and pet row projection.
- Why this scope is sufficient: the focused test exercises the real packet parser and connection send path and verifies the emitted packet bytes for the new client-visible behavior.
```

Result: passed, 410/410.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService#onPlayerLogin` | `GameServerConnection.SendOwnedPetListAsync` | Runtime packet send | Partial | Unit Tested | Partial Parity | Sends `SM_PET LOAD_PETS` for restored pets during live enter-world. Remaining pet login side effects, expirable behavior, and last-used pet state are not complete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET#writePetData` | `SmPet.WritePetData` plus `PlayerOwnedPet` mapping | Packet/runtime mapping | Partial | Unit Tested | Partial Parity | Emits known Java packet fields and writable static functions. Full `PetCommonData` mood and scheduling behavior remains partial. |
| `com.aionemu.gameserver.dao.PlayerPetsDAO#getPlayerPets` | `MySqlPlayerEnterWorldRepository.LoadPlayerPetsAsync` / `PlayerPetRowProjection` | Repository | Partial | Unit Tested | Partial Parity | Load now preserves fields needed for login packet snapshots when templates identify food/doping functions. Live MySQL validation was not run. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Pet expirable registration from Java `ExpireTimerTask` is not wired.
- Java last-used-pet tracking is not represented.
- `CM_PET` DISMISS, SURRENDER, auto-sell, auto-loot, adopt, and food/doping interaction flows remain deferred.
- Full live `PetCommonData` mood, gift, feed, and refeed scheduling behavior remains partial.
- Live MySQL and real-client validation were not run.
