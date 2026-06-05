# Phase 6 Session 2635 Completion

## UOW

[Phase 6] UOW-2635: Execute active pet auto-loot activation live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD actionType 3 previously returned silently in C# live code.
- Java source/runtime path: CM_PET.runImpl FOOD actionType 3 calls PetService.activateLoot(pet, activateSpecialFunction != 0); PetService.activateLoot validates LOOT support on enable, sends the enable system message, mutates pet common-data looting state, and sends SM_PET(PetSpecialFunction.AUTOLOOT, activate).
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync now routes actionType 3 to live auto-loot activation; PlayerOwnedPet now stores IsLooting runtime state.
- Client-visible/state/persistence effect: enabling auto-loot on a LOOT-capable active pet sends STR_MSG_LOOTING_PET_MESSAGE01 and Java-shaped SM_PET AUTOLOOT activation; enabling on a non-loot pet returns silently; disabling clears IsLooting and sends SM_PET AUTOLOOT false.
- Why this is runtime progress: this UOW mutates live active-pet state and sends real owner packets from the live client handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - FOOD actionType 3 routes to `PetService.getInstance().activateLoot(pet, activateSpecialFunction != 0)`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `activateLoot` returns silently on enable when the pet template lacks `PetFunctionType.LOOT`.
  - On enable, Java checks team free-for-all loot rules, sends `STR_MSG_LOOTING_PET_MESSAGE01`, sets `PetCommonData.isLooting`, and sends `SM_PET(PetSpecialFunction.AUTOLOOT, activate)`.
  - On disable, Java skips the LOOT/free-for-all guards, sets `isLooting` false, and sends `SM_PET(PetSpecialFunction.AUTOLOOT, false)`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - SPECIAL_FUNCTION subtype 3 activation writes `0` then active flag `1` or `0`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - Stores `isLooting` runtime state.

## C# Changes

- Added `PlayerOwnedPet.IsLooting` as live active-pet runtime state.
- Extended `GameServerConnection.HandlePetFoodAsync`:
  - routes actionType 3 to `HandlePetAutoLootActivationAsync`,
  - keeps actionType 2 doping and actionType 4 auto-sell deferred,
  - validates active pet and LOOT function for enable using runtime-loaded `PetTemplateTable`,
  - sends `SmSystemMessage(1400876)` for successful enable,
  - mutates active owned pet `IsLooting`,
  - sends `SmPet.SpecialFunction` AUTOLOOT activation/deactivation.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetAutoLootEnableMutatesPetAndSendsPackets` | Unit/live connection | `CM_PET.runImpl FOOD actionType 3 -> PetService.activateLoot(true)` | Real `CM_PET` parsing with active LOOT-capable pet sets `IsLooting`, sends message `1400876`, and sends Java-shaped AUTOLOOT true packet. | Asserts live handler state mutation and serialized packet payload. | Does not validate team free-for-all denial. |
| `ProcessPacketAsync_CmPetAutoLootDisableMutatesPetAndSendsPacket` | Unit/live connection | `PetService.activateLoot(false)` | Disable skips LOOT-template guard, clears `IsLooting`, and sends Java-shaped AUTOLOOT false packet. | Direct live handler state and packet assertions. | No persistence; Java state is runtime-only here. |
| `ProcessPacketAsync_CmPetAutoLootEnableWithoutLootFunctionDoesNothing` | Unit/live connection | `PetService.activateLoot(true)` missing LOOT guard | Enabling auto-loot on a non-loot pet returns silently and does not mutate/send. | Direct live handler assertion. | Java audit logging is not implemented. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch, active pet runtime state, and live SM_PET/SM_SYSTEM_MESSAGE emission.
- Specific behavior/contract: CM_PET FOOD actionType 3 should execute Java auto-loot activation/deactivation ordering for active pets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.activateLoot or SM_PET AUTOLOOT activation.
- Broad-validation trigger: live pet state and packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, packet serializer shape, and packet bytes.
- Why this scope is sufficient: the focused tests exercise the real client packet parser/connection path and inspect the exact owner-visible packet/state contract for the scoped auto-loot behavior.
```

Result: passed, 381/381. The run emitted existing nullable/analyzer warnings in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 3 | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live auto-loot activation/deactivation now executes for active pets. Doping and auto-sell action types remain deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#activateLoot` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetAutoLootActivationAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | LOOT function guard, enable message, state mutation, and AUTOLOOT packet are ported. Java audit logging and team free-for-all denial are not live yet. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData#isLooting` | `Aion.GameServer.Model.GameObjects.PlayerOwnedPet.IsLooting` | Runtime model | Partial | Unit Tested | Partial Parity | Runtime-only looting flag exists and is mutated by live auto-loot activation. Persistence is not expected for this Java field. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` AUTOLOOT special function | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes AUTOLOOT activation shape and live actionType 3 now sends it. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Java team free-for-all denial branch (`STR_MSG_LOOTING_PET_MESSAGE03`) is not live because this slice did not inspect/wire active team loot-rule state into pet activation.
- Java audit logging for enabling auto-loot on non-loot pets is not implemented.
- FOOD actionType 2 doping and actionType 4 auto-sell remain deferred/partial.
- Delayed pet `checkFeeding` remains deferred; static feed-data runtime wiring was not safe enough for this UOW.
- Real client validation was not run.
- Real MySQL validation was not relevant to this runtime-only state/packet UOW and was not run.
