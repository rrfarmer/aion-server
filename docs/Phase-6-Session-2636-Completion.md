# Phase 6 Session 2636 Completion

## UOW

[Phase 6] UOW-2636: Execute active pet auto-sell activation live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD actionType 4 previously returned silently in C# live code.
- Java source/runtime path: CM_PET.runImpl FOOD actionType 4 calls PetService.activateAutoSell(pet, activateSpecialFunction != 0); PetService.activateAutoSell validates MERCHANT support only on enable, sets PetCommonData.isSelling, and sends SM_PET(PetSpecialFunction.AUTOSELL, activate).
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync now routes actionType 4 to live auto-sell activation; PlayerOwnedPet now stores IsSelling runtime state.
- Client-visible/state/persistence effect: enabling auto-sell on a MERCHANT-capable active pet mutates runtime selling state and sends Java-shaped SM_PET AUTOSELL true; enabling on a non-merchant pet returns silently; disabling clears IsSelling and sends SM_PET AUTOSELL false.
- Why this is runtime progress: this UOW mutates live active-pet state and sends a real owner packet from the live client handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - FOOD actionType 4 routes to `PetService.getInstance().activateAutoSell(pet, activateSpecialFunction != 0)`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `activateAutoSell` returns silently on enable when the pet template lacks `PetFunctionType.MERCHANT`.
  - Enable and disable set `PetCommonData.isSelling` and send `SM_PET(PetSpecialFunction.AUTOSELL, activate)`.
  - Disable skips the MERCHANT-template guard.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - SPECIAL_FUNCTION subtype 4 activation writes `0` then active flag `1` or `0`.

## C# Changes

- Added `PlayerOwnedPet.IsSelling` as live active-pet runtime state.
- Extended `GameServerConnection.HandlePetFoodAsync`:
  - routes actionType 4 to `HandlePetAutoSellActivationAsync`,
  - validates active pet and MERCHANT function for enable using runtime-loaded `PetTemplateTable`,
  - mutates active owned pet `IsSelling`,
  - sends `SmPet.SpecialFunction` AUTOSELL activation/deactivation.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetAutoSellEnableMutatesPetAndSendsPacket` | Unit/live connection | `CM_PET.runImpl FOOD actionType 4 -> PetService.activateAutoSell(true)` | Real `CM_PET` parsing with active MERCHANT-capable pet sets `IsSelling` and sends Java-shaped AUTOSELL true packet. | Asserts live handler state mutation and serialized packet payload. | Java audit logging is not implemented. |
| `ProcessPacketAsync_CmPetAutoSellDisableMutatesPetAndSendsPacket` | Unit/live connection | `PetService.activateAutoSell(false)` | Disable skips MERCHANT-template guard, clears `IsSelling`, and sends Java-shaped AUTOSELL false packet. | Direct live handler state and packet assertions. | No persistence; Java state is runtime-only here. |
| `ProcessPacketAsync_CmPetAutoSellEnableWithoutMerchantFunctionDoesNothing` | Unit/live connection | `PetService.activateAutoSell(true)` missing MERCHANT guard | Enabling auto-sell on a non-merchant pet returns silently and does not mutate/send. | Direct live handler assertion. | Java audit logging is not implemented. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch, active pet runtime state, and live SM_PET emission.
- Specific behavior/contract: CM_PET FOOD actionType 4 should execute Java auto-sell activation/deactivation ordering for active pets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.activateAutoSell or SM_PET AUTOSELL activation.
- Broad-validation trigger: live pet state and packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, packet serializer shape, and packet bytes.
- Why this scope is sufficient: the focused tests exercise the real client packet parser/connection path and inspect the exact owner-visible packet/state contract for the scoped auto-sell behavior.
```

Result: passed, 384/384. The run emitted existing nullable/analyzer warnings in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 4 | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live auto-sell activation/deactivation now executes for active pets. Doping remains deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#activateAutoSell` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetAutoSellActivationAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | MERCHANT function guard, state mutation, and AUTOSELL packet are ported. Java audit logging is not live yet. |
| `com.aionemu.gameserver.model.gameobjects.player.PetCommonData#isSelling` | `Aion.GameServer.Model.GameObjects.PlayerOwnedPet.IsSelling` | Runtime model | Partial | Unit Tested | Partial Parity | Runtime-only selling flag exists and is mutated by live auto-sell activation. Persistence is not expected for this Java field. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` AUTOSELL special function | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes AUTOSELL activation shape and live actionType 4 now sends it. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Java audit logging for enabling auto-sell on non-merchant pets is not implemented.
- FOOD actionType 2 doping remains deferred/partial.
- Java team free-for-all denial branch for auto-loot remains deferred.
- Delayed pet `checkFeeding` remains deferred; static feed-data runtime wiring was not safe enough for this UOW.
- Real client validation was not run.
- Real MySQL validation was not relevant to this runtime-only state/packet UOW and was not run.
