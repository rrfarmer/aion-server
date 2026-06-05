# Phase 6 Session 2638 Completion

## UOW

[Phase 6] UOW-2638: Execute active pet doping add/remove live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD actionType 2 dopeActions 0/1 previously returned silently in C# live code.
- Java source/runtime path: PetService.useDoping actions 0/1 call validateSetDopeItem, resolve PetFunctionType.DOPING id through DataManager.PET_DOPING_DATA, mutate PetDopingBag.setItem(itemId, slot), and send SM_PET(action, itemId, slot).
- C# runtime artifact wired: StaticData now loads Java pet_doping.xml rows into PetDopingTable; GameServerConnection.HandlePetDopingAsync validates dopeActions 0/1 against runtime pet template and pet-doping data, mutates active PlayerOwnedPet.DopingItemIds, and emits SmPet.DopingSpecialFunction.
- Client-visible/state/persistence effect: adding or removing a pet doping item on an active DOPING-capable pet mutates runtime doping slots and sends Java-shaped DOPING action 0/1 owner packets; invalid slot capability returns silently.
- Why this is runtime progress: this UOW loads Java XML/static data into runtime C# structures used by live code, executes a live client-handler branch, mutates active pet runtime state, and sends real server packets.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/dataholders/PetDopingData.java`
  - JAXB root `dopings`; indexes `<doping>` rows by id after unmarshal.
- `game-server/src/com/aionemu/gameserver/model/templates/pet/PetDopingEntry.java`
  - Required attributes: `id`, `usedrink`, `usefood`, `usescroll`.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `useDoping` actions 0/1 validate the target slot, set the item id in the doping bag, and send `SM_PET(action, itemId, slot)`.
  - `validateSetDopeItem` checks food slot, drink slot, and scroll capacity from `PetDopingData`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - DOPING SPECIAL_FUNCTION action 0 writes item id then slot; action 1 writes only slot.

## C# Changes

- Added `PetDopingTable` and `PetDopingEntrySummary`.
- Added `StaticData.PetDopings` and XML parsing for `<dopings><doping .../>` rows.
- Extended live `GameServerConnection.HandlePetDopingAsync`:
  - resolves pet templates from injected test data or runtime `StaticData.PetTemplates`,
  - resolves pet doping capability rows from injected test data or runtime `StaticData.PetDopings`,
  - validates dopeActions 0/1 like Java,
  - mutates active `PlayerOwnedPet.DopingItemIds`,
  - sends Java-shaped DOPING add/remove packets.
- Updated existing reflection-based `StaticData` test helpers with empty `PetDopingTable` instances so the new runtime table constructor shape remains valid.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `LoadFromCacheAsync_ParsesPetDopingEntries` | Unit/static-data | `PetDopingData` and `PetDopingEntry` JAXB mappings | Java-shaped `<dopings>` XML loads into `StaticData.PetDopings` with id, food/drink flags, and scroll capacity. | Source-derived XML parser assertions. | Does not run the full Java corpus. |
| `ProcessPacketAsync_CmPetDopingAddMutatesSlotAndSendsPacket` | Unit/live connection | `PetService.useDoping` action 0 | Real `CM_PET` parsing with active DOPING pet and matching doping data mutates a scroll slot and sends Java-shaped DOPING action 0 packet. | Live handler state mutation and serialized packet payload. | Does not validate item existence; Java validates only pet capability here. |
| `ProcessPacketAsync_CmPetDopingRemoveMutatesSlotAndSendsPacket` | Unit/live connection | `PetService.useDoping` action 1 | Remove request sets target slot to packet item id and sends Java-shaped DOPING action 1 packet. | Live handler state mutation and packet payload. | Persistence remains unwired. |
| `ProcessPacketAsync_CmPetDopingAddRejectedByDopingDataDoesNothing` | Unit/live connection | `validateSetDopeItem` scroll-capacity guard | Pet doping data denies an unsupported scroll slot without mutation or packets. | Live handler data-driven guard assertion. | Java audit logging is not implemented. |

## Validation Decision

```text
- Changed surface: runtime static-data loading, live CM_PET handler dispatch, active pet runtime doping slot state, and live SM_PET DOPING emission.
- Specific behavior/contract: pet_doping.xml rows should load into runtime StaticData and CM_PET FOOD actionType 2/dopeActions 0/1 should execute Java add/remove behavior for active DOPING-capable pets.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests|FullyQualifiedName~GamePacketTests|FullyQualifiedName~CmPetTests|FullyQualifiedName~StaticDataPetTemplateTests" --no-restore
- Focused Java/Maven command: not run; no narrow Java fixture was discovered for PetDopingData or PetService.useDoping actions 0/1.
- Broad-validation trigger: runtime static-data loading, live pet state, and packet dispatch changed.
- Broad .NET decision: skipped after focused coverage because the command built affected projects and directly covered the new static-data table, live connection dispatch, parser shape, packet serializer shape, and packet bytes.
- Why this scope is sufficient: the focused tests exercise the new runtime data table and the live client packet path that consumes it; unrelated static-data constructor helpers were updated for compatibility.
```

Result: passed, 392/392. The run emitted existing nullable/analyzer warnings in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.PetDopingData` | `Aion.GameServer.Dataholders.PetDopingTable` | Dataholder | Partial | Unit Tested | Partial Parity | Runtime lookup by id is ported and consumed by live code. Duplicate-id last-write behavior was not explicitly tested. |
| `com.aionemu.gameserver.model.templates.pet.PetDopingEntry` | `Aion.GameServer.Dataholders.PetDopingEntrySummary` | DTO | Partial | Unit Tested | Partial Parity | Required XML attributes id/usedrink/usefood/usescroll are parsed. JAXB exception details are not mirrored. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` FOOD actionType 2 | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetFoodAsync` | Client handler | Partial | Unit Tested | Partial Parity | DopeActions 0/1/2 now execute live. DopeAction 3 remains deferred. |
| `com.aionemu.gameserver.services.toypet.PetService#useDoping` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetDopingAsync` | Service behavior in handler | Partial | Unit Tested | Partial Parity | Add/remove validation, slot mutation, switch, and packet send are ported. Item use, scheduling, cooldown, skill effects, inventory decrement, save triggers, and audit logging remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` DOPING special function | `Aion.GameServer.Network.Aion.ServerPackets.SmPet` | Packet | Partial | Unit Tested | Partial Parity | Existing serializer writes DOPING actions 0/1/2 and live actionType 2 now sends them. |

## Summary Metrics

- Java artifacts discovered/touched: 5.
- C# artifacts changed/touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification or partial parity: 5.
- Blocked artifacts: 0.
- Estimated overall Phase 6 migration completion: unchanged, still partial.

## Known Gaps

- Doping use action 3 remains deferred because Java uses player spawned-state checks, delayed scheduling, item-use restrictions, item cooldown, skill effects, and inventory decrement.
- Doping bag dirty-state persistence is not wired from live mutation to `player_pets.dopings`.
- Java audit logging for unsupported doping configuration attempts is not implemented.
- Duplicate `PetDopingData` ids would use Java last-write map behavior; C# currently uses dictionary construction and was not tested for duplicates.
- Java invalid high-slot exception behavior was not validated for live connection handling.
- Full Java static-data corpus validation was not run.
- Real client validation was not run.
- Real MySQL validation was not run.
