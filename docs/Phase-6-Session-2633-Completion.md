# Phase 6 Session 2633 Completion

## UOW

[Phase 6] UOW-2633: Send active pet not-hungry response live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD with a nonzero food item object id and a future refeed delay previously stayed silent in C# live code.
- Java source/runtime path: CM_PET.runImpl FOOD checks active pet, then when pet.getCommonData().getRefeedDelay() > 0 sends new SM_PET(8, objectId, count, player.getPet()).
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync now checks PlayerOwnedPet.RefeedDelaySeconds and sends SmPet.Food subtype 8 from live code.
- Client-visible/state/persistence effect: feeding an active pet during refeed delay sends Java-shaped SM_PET FOOD subtype 8 with feed progress, refeed delay, item object id, and count.
- Why this is runtime progress: this UOW sends a real server packet from the live client handler based on active pet runtime state.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - After cancel-feeding and before regular food consumption, Java checks `pet.getCommonData().getRefeedDelay() > 0` and sends `new SM_PET(8, objectId, count, player.getPet())`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `getRefeedDelay` returns remaining milliseconds and clears expired negative values to zero.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - FOOD subtype 8 writes feed progress, refeed delay seconds, item object id, and count.

## C# Changes

- Extended live `GameServerConnection.HandlePetFoodAsync`:
  - leaves action types 2/3/4 reserved for later doping/auto-loot/auto-sell runtime UOWs,
  - keeps the existing cancel-feeding branch for `ObjectId == 0`,
  - for active pets with `RefeedDelaySeconds(DateTimeOffset.Now) > 0`, sends `SmPet.Food` subtype 8,
  - leaves regular no-delay food consumption deferred and silent for a later runtime UOW.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetFoodWithRefeedDelaySendsNotHungryPacket` | Unit/live connection | `CM_PET.runImpl FOOD -> getRefeedDelay() > 0 -> SM_PET(8, objectId, count, pet)` | Real `CM_PET` parsing sends Java-shaped FOOD subtype 8 with active pet feed progress, positive refeed delay, requested item object id, and count. | Asserts serialized `SM_PET` subtype 8 payload from live handler. | Uses a large future refeed time and asserts a lower-bound delay to avoid clock flake; does not compare against a Java runtime fixture. |
| `ProcessPacketAsync_CmPetFoodWithoutRefeedDelayLeavesRegularFeedDeferred` | Unit/live connection | Java falls through to `PetService.removeObject` when refeed delay is zero | C# no-delay regular feeding remains intentionally deferred in this UOW and does not send the not-hungry response. | Direct live handler assertion prevents subtype 8 from firing without a Java refeed delay. | Regular feeding still needs a runtime UOW for inventory/feed mutation and packets. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch and live SM_PET FOOD subtype 8 packet emission.
- Specific behavior/contract: CM_PET FOOD with an active pet and positive refeed delay should send Java-shaped SM_PET subtype 8 containing feed progress, refeed delay, item object id, and count.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for CM_PET food not-hungry or SM_PET FOOD subtype 8.
- Broad-validation trigger: live pet packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, existing packet serializer shape, and subtype 8 packet bytes.
- Why this scope is sufficient: the focused tests exercise the real packet parser/connection path and inspect exact packet payload for the scoped not-hungry behavior.
```

Result: passed, 376/376.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD not-hungry | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live positive-refeed branch sends Java-shaped subtype 8. Regular feeding, doping, auto-loot, and auto-sell remain partial/deferred. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData#getRefeedDelay` | `Aion.GameServer.Model.GameObjects.PlayerOwnedPet.RefeedDelaySeconds` | Runtime model | Partial | Unit Tested | Partial Parity | C# computes remaining delay from restored `RefeedTimeMillis`; unlike Java, it does not clear expired negative refeed time in this helper. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype 8 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes subtype 8 shape and live not-hungry response now sends it. |

## Summary Metrics

- Java artifacts discovered/touched: 3.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 3.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Regular pet feeding remains partial: food item lookup, inventory decrement, feed progress mutation, loved-food/reward handling, scheduling, and persistence are not live.
- C# `RefeedDelaySeconds` does not clear expired refeed time the way Java `PetCommonData.getRefeedDelay` clears negative delays.
- FOOD doping, auto-loot, and auto-sell branches remain deferred/partial.
- Real client validation was not run.
