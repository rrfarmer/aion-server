# Phase 6 Session 2634 Completion

## UOW

[Phase 6] UOW-2634: Start active pet feeding live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: no-delay CM_PET FOOD with a valid active-pet inventory item previously stayed silent in C# live code.
- Java source/runtime path: CM_PET.runImpl FOOD falls through to PetService.removeObject(objectId, count, player); PetService.removeObject validates inventory/count, clears cancelFeed, sends SM_PET subtype 1, sends SM_EMOTION START_FEEDING, then schedules later checkFeeding work.
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync now validates the requested inventory item/count, clears PlayerOwnedPet.CancelFeed, sends SmPet.Food subtype 1, and sends SmEmotion StartFeeding from live code.
- Client-visible/state/persistence effect: feeding an active pet with a valid item now mutates active owned-pet state and sends Java-shaped feed-start owner packets. Inventory consumption remains delayed/deferred to match Java's later checkFeeding boundary.
- Why this is runtime progress: this UOW mutates live pet state and sends real server packets from the live client handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - FOOD checks active pet, skips action types 2/3/4 into separate services, handles cancel and not-hungry branches, then calls `PetService.removeObject` for regular feeding.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `removeObject` returns when inventory item is missing, pet is missing, or requested count exceeds item count.
  - Valid starts clear `cancelFeed`, send `new SM_PET(1, item.getObjectId(), count, pet)`, send `new SM_EMOTION(... START_FEEDING ...)`, and schedule `checkFeeding` 2500 ms later.
  - `checkFeeding` performs item acceptance, inventory decrement, progress/reward updates, refeed persistence, and end-feeding packets later.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - FOOD subtype 1 writes feed progress, zero refeed field, item object id, and count.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_EMOTION.java`
  - START_FEEDING uses the simple movement-state/speed shape.

## C# Changes

- Extended live `GameServerConnection.HandlePetFoodAsync`:
  - keeps action types 2/3/4 deferred for future doping/auto-loot/auto-sell UOWs,
  - keeps cancel-feeding and positive-refeed branches first,
  - validates the requested inventory object id and requested count like Java `removeObject`,
  - clears `CancelFeed` on the active owned pet,
  - sends `SmPet.Food` subtype 1 and `SmEmotion` START_FEEDING,
  - leaves scheduled food acceptance, item decrement, feed progress, reward, refeed persistence, and end-feeding packets for later runtime work.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetFoodWithoutRefeedDelayStartsFeeding` | Unit/live connection | `CM_PET.runImpl FOOD -> PetService.removeObject` | Real `CM_PET` parsing with active pet and valid item clears `CancelFeed`, leaves inventory count untouched for the later scheduled check, and sends Java-shaped SM_PET subtype 1 plus SM_EMOTION START_FEEDING. | Asserts live handler state mutation and serialized packet payloads. | Does not execute delayed `checkFeeding`. |
| `ProcessPacketAsync_CmPetFoodWithoutInventoryItemDoesNothing` | Unit/live connection | `PetService.removeObject` missing item guard | Missing inventory item returns silently and preserves existing `CancelFeed`. | Direct live handler assertion. | No Java runtime fixture comparison. |
| `ProcessPacketAsync_CmPetFoodCountAboveInventoryCountDoesNothing` | Unit/live connection | `PetService.removeObject` count guard | Requested count above item count returns silently, preserving pet and inventory state. | Direct live handler assertion. | No Java runtime fixture comparison. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch, active pet state mutation, and live SM_PET/SM_EMOTION packet emission.
- Specific behavior/contract: regular no-delay CM_PET FOOD should validate inventory/count, clear cancelFeed, send Java-shaped feed-start packets, and not decrement inventory until the later checkFeeding phase.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.removeObject or CM_PET FOOD feed-start.
- Broad-validation trigger: live pet state and packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, packet serializer shape, and packet bytes.
- Why this scope is sufficient: the focused tests exercise the real client packet parser/connection path and inspect the exact owner-visible packet/state contract for the scoped feed-start behavior.
```

Result: passed, 378/378. The run emitted existing nullable/analyzer warnings in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD regular feed-start | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live regular feed-start now validates item/count, clears cancel feed, and sends subtype 1 plus START_FEEDING. Doping, auto-loot, auto-sell, and delayed feeding completion remain partial/deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#removeObject` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | First synchronous Java effects are ported. Java's scheduled `checkFeeding` inventory decrement, food acceptance, rewards, refeed persistence, and end-feeding packets are not live yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype 1 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes subtype 1 shape and live feed-start now sends it. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` START_FEEDING | `Aion.GameServer.Network.Aion.ServerPackets.SmEmotion` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes simple START_FEEDING shape and live feed-start now sends it. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Delayed pet `checkFeeding` is not live: food-type validation, item unlock, inventory decrement, feed progress mutation, reward grant, refeed time persistence, and end-feeding packets remain deferred.
- FOOD doping, auto-loot, and auto-sell branches remain deferred/partial.
- C# `RefeedDelaySeconds` still does not clear expired refeed time the way Java `PetCommonData.getRefeedDelay` clears negative delays.
- Real client validation was not run.
- Real MySQL validation was not needed for this non-persistent feed-start slice.
