# Phase 6 Session 2637 Completion

## UOW

[Phase 6] UOW-2637: Execute active pet doping slot switch live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD actionType 2 previously returned silently in C# live code.
- Java source/runtime path: CM_PET.runImpl FOOD actionType 2 calls PetService.useDoping(pet, dopingAction, dopingItemId, dopingSlot1, dopingSlot2); PetService.useDoping action 2 calls PetDopingBag.switchItems(slot, slot2) and sends SM_PET(action, slot2, slot).
- C# runtime artifact wired: GameServerConnection.HandlePetFoodAsync now routes actionType 2 to live doping handling for dopeAction 2; active PlayerOwnedPet.DopingItemIds is switched through the Java-derived PetDopingBag helper.
- Client-visible/state/persistence effect: switching two scroll slots on a DOPING-capable active pet mutates runtime pet doping slots and sends Java-shaped SM_PET SPECIAL_FUNCTION DOPING action 2 to the owner.
- Why this is runtime progress: this UOW executes a previously deferred live client-handler branch, mutates active pet runtime state, and sends a real server packet from live code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - FOOD actionType 2 reads `dopingAction`, `dopingItemId`, `dopingSlot1`, and `dopingSlot2`, then routes to `PetService.useDoping`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `useDoping` returns when the common-data doping bag is null.
  - Dope action 2 switches slots through `PetDopingBag.switchItems(slot, slot2)` and sends `new SM_PET(action, slot2, slot)`.
  - Dope actions 0/1 require `PetDopingData` validation; dope action 3 enters item-use, cooldown, scheduling, skill-effect, and inventory-decrement behavior.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingBag.java`
  - `switchItems` returns without mutation when either slot is below 2; otherwise it swaps scroll slots through `setItem`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - DOPING SPECIAL_FUNCTION action 2 writes source slot then target slot.

## C# Changes

- Extended `GameServerConnection.HandlePetFoodAsync`:
  - routes FOOD actionType 2 to `HandlePetDopingAsync`,
  - validates active pet and DOPING function using runtime-loaded `PetTemplateTable`,
  - executes dopeAction 2 slot switching against active `PlayerOwnedPet.DopingItemIds`,
  - sends `SmPet.DopingSpecialFunction` with Java action-2 field order.
- Reused existing `Aion.GameServer.Services.ToyPet.PetDopingBag` and existing `SmPetDopingSpecialFunctionSnapshot`.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetDopingSwitchMutatesSlotsAndSendsPacket` | Unit/live connection | `CM_PET.runImpl FOOD actionType 2 -> PetService.useDoping` dopeAction 2 | Real `CM_PET` parsing with active DOPING-capable pet swaps scroll slots only on the active pet and sends Java-shaped DOPING action 2 packet. | Asserts live handler state mutation and serialized packet payload. | Does not cover add/remove/use branches. |
| `ProcessPacketAsync_CmPetDopingSwitchWithFoodSlotSendsPacketWithoutMutatingSlots` | Unit/live connection | `PetDopingBag.switchItems` slot guard | Slot values below 2 do not mutate doping slots, but Java still sends the action 2 packet. | Direct live handler state and packet assertions. | Does not cover invalid high-slot exception behavior. |
| `ProcessPacketAsync_CmPetDopingSwitchWithoutDopingFunctionDoesNothing` | Unit/live connection | `PetService.useDoping` null doping-bag guard from non-DOPING template | Non-DOPING active pet returns silently and does not mutate/send. | Direct live handler assertion. | Java audit logging is not involved in this branch. |

## Validation Decision

```text
- Changed surface: live CM_PET handler dispatch, active pet runtime doping slot state, and live SM_PET DOPING emission.
- Specific behavior/contract: CM_PET FOOD actionType 2/dopeAction 2 should execute Java pet doping slot switching for active DOPING-capable pets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetService.useDoping action 2 or SM_PET DOPING action 2 activation.
- Broad-validation trigger: live pet state and packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered live connection dispatch, parser shape, packet serializer shape, and packet bytes.
- Why this scope is sufficient: the focused tests exercise the real client packet parser/connection path and inspect the exact owner-visible packet/state contract for the scoped doping switch behavior.
```

Result: passed, 387/387. The run emitted existing nullable/analyzer warnings in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 2 | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | Live actionType 2 now executes dopeAction 2 slot switching. Dope actions 0/1/3 remain deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#useDoping` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetDopingAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | Null-bag/DOPING-function guard and action 2 switch/send are ported. Add/remove validation, item use, scheduling, cooldown, skill effects, inventory decrement, and save triggers remain missing. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingBag#switchItems` | `Aion.GameServer.Services.ToyPet.PetDopingBag.SwitchItems` used by live handler | Runtime helper | Partial | Unit Tested | Partial Parity | Existing helper is now used by live code for scroll-slot switching. Threading parity remains limited to deterministic tests. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` DOPING special function | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes DOPING action 2 shape and live actionType 2 now sends it. |

## Summary Metrics

- Java artifacts discovered/touched: 4.
- C# artifacts changed/touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 4.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Doping add/remove actions 0/1 remain deferred because Java requires runtime `PetDopingData` food/drink/scroll-capacity validation.
- Doping use action 3 remains deferred because Java uses player spawned-state checks, delayed scheduling, item-use restrictions, item cooldown, skill effects, and inventory decrement.
- Doping bag dirty-state persistence is not wired from live mutation to `player_pets.dopings`.
- Java invalid high-slot exception behavior was not validated for live connection handling.
- Java audit logging for unsupported branches is not implemented.
- Delayed pet `checkFeeding` and auto-loot free-for-all denial remain deferred.
- Real client validation was not run.
- Real MySQL validation was not run.
