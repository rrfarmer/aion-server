# Phase 6 Session 2654 Completion

## UOW

[Phase 6] UOW-2654: Execute live `CM_PET MOOD` subtype 3 gift request.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET MOOD subtype 3 now executes the live Java pet gift path instead of being parsed and ignored.
- Java source/runtime path: CM_PET.runImpl routes MOOD subtype 3 when gift cooldown is clear; PetMoodService.requestPresent checks full mood, gift cooldown, inventory capacity, clears mood statistics, sends SM_PET subtype 4, sends SM_PET subtype 3, and grants the pet condition reward through ItemService.addItem.
- C# runtime artifact wired: GameServerConnection.HandlePetMoodAsync now dispatches subtype 3 to HandlePetMoodGiftAsync, using PlayerOwnedPet mood/gift timing, PetTemplateTable condition reward data, InventoryAddService, SmPet.Mood subtype 3/subtype 4, and a new reward-only inventory persistence hook.
- Client-visible/state/persistence effect: live subtype 3 clears active pet mood statistics, starts gift cooldown, sends Java-shaped subtype 4 then subtype 3 mood packets, mutates live inventory with the condition reward, sends inventory add/update packets, and persists reward item rows through the existing inventory schema.
- Why this is runtime progress: this wires a parsed client packet action to live handler behavior, mutates active pet state, sends real server packets, mutates live inventory state, and persists reward inventory changes.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_PET.java`
  - `MOOD` routes subtype 3 only when the active pet exists and gift cooldown is clear.
- `game-server/src/com/aionemu/gameserver/services/toypet/PetMoodService.java`
  - `requestPresent` rejects mood below 9000, rejects gift cooldown, rejects full inventory, clears mood statistics, sends `new SM_PET(pet, 4, 0)`, sends `new SM_PET(pet, 3, 0)`, then grants `conditionReward`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/PetCommonData.java`
  - `getMoodPoints(false)` is the full-mood gate; `clearMoodStatistics` resets start/counter; gift cooldown is tracked on common data.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_PET.java`
  - MOOD subtype 4 writes mood/cooldown state; subtype 3 writes condition reward and starts gift cooldown.

## C# Changes

- Added live subtype 3 routing in `GameServerConnection.HandlePetMoodAsync`.
- Added `HandlePetMoodGiftAsync`:
  - rejects active gift cooldown,
  - rejects mood below 9000,
  - checks pet template condition reward and item template availability,
  - uses `InventoryAddService.CreateAddItemPlan` for reward capacity/merge/add behavior,
  - sends `SmSystemMessage.FullInventory()` when the reward cannot fit,
  - persists reward inventory mutations before applying live inventory state,
  - clears mood statistics, sends subtype 4 then subtype 3, starts gift cooldown, and updates active `PlayerOwnedPet` timing.
- Added `SaveInventoryRewardMutationAsync` to `IPlayerEnterWorldRepository`, `EmptyPlayerEnterWorldRepository`, `MySqlPlayerEnterWorldRepository`, and `PlayerEnterWorldService`.
- Added focused live connection tests for success, pre-full-mood rejection, and active gift cooldown rejection.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetMoodGiftClearsMoodStartsCooldownPersistsRewardAndSendsPackets` | Unit/live connection | `CM_PET.runImpl`, `PetMoodService.requestPresent`, `SM_PET.writeImpl`, `ItemService.addItem` source review | Live subtype 3 clears pet mood, starts gift cooldown, persists and applies reward inventory add, sends subtype 4 then subtype 3 then inventory add packet. | Runs actual connection packet dispatch, serializes emitted server packets, and records repository reward persistence. | Uses in-memory repository; no MySQL round trip. |
| `ProcessPacketAsync_CmPetMoodGiftBeforeFullMoodDoesNothing` | Unit/live connection | `PetMoodService.requestPresent` mood gate | Mood below 9000 sends no packets, starts no cooldown, and persists no reward. | Runs actual connection packet dispatch with active pet state. | Logging is not asserted. |
| `ProcessPacketAsync_CmPetMoodGiftDuringCooldownDoesNotSendPacket` | Unit/live connection | `CM_PET.runImpl` / `PetMoodService.requestPresent` gift cooldown gate | Active gift cooldown blocks subtype 3 state and packet side effects. | Runs actual connection packet dispatch with active cooldown state. | Java audit logging is not modeled. |

## Validation Decision

```text
- Changed surface: live CM_PET MOOD subtype 3 dispatch, active owned-pet mood/gift timing state, reward inventory mutation/persistence, and SM_PET mood subtype 3/subtype 4 packet emission.
- Specific behavior/contract: Java PetMoodService.requestPresent clears full pet mood, emits subtype 4 then subtype 3, starts gift cooldown, and grants conditionReward when present.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionBuyItemTests"
- Focused Java/Maven command: not run; no narrow Java unit fixture exists for CM_PET MOOD subtype 3 or PetMoodService.requestPresent in this checkout, and Java source was reviewed directly.
- Broad-validation trigger: live connection dispatch, active pet runtime state, live side-effect packet sends, and inventory persistence contract changed.
- Broad .NET decision: skipped after focused validation because the filtered live connection class built affected projects and directly exercised the scoped packet/state/inventory persistence behavior.
- Why this scope is sufficient: the edited runtime path is isolated to CM_PET MOOD subtype 3 and the new reward persistence hook; focused tests cover success, both live gates, serialized pet packets, inventory packet emission, live inventory mutation, and repository persistence recording.
```

Results:

- Initial focused run exposed a `Player.InventoryItems` read-only list assignment issue; fixed by applying reward mutations through a working list and assigning it back.
- Second focused run exposed the test fixture's incorrect ID factory assumption; corrected to assert the generated id consistently.
- Final focused run passed: 95/95 `GameServerConnectionBuyItemTests`.
- Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures remained.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_PET` MOOD subtype 3 branch | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetMoodAsync` / `HandlePetMoodGiftAsync` | Runtime packet path | Partial | Unit Tested | Partial Parity | Subtype 3 now executes against active pet state, including reward grant. Periodic mood task integration remains incomplete. |
| `com.aionemu.gameserver.services.toypet.PetMoodService.requestPresent` | `Aion.GameServer.Network.Aion.GameServerConnection.HandlePetMoodGiftAsync` | Runtime service behavior | Partial | Unit Tested | Partial Parity | Full-mood, gift cooldown, packet send, cooldown start, and reward grant branches are modeled. Java audit logging and exact full-inventory system-message id remain conservative gaps. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` MOOD subtype 3/subtype 4 | `Aion.GameServer.Network.Aion.ServerPackets.SmPet.Mood` | Server packet | Partial | Unit Tested | Partial Parity | Existing writer is now called by live dispatch; subtype 3 condition reward and subtype 4 reset state are asserted from serialized packets. |
| `com.aionemu.gameserver.services.item.ItemService.addItem` reward grant | `InventoryAddService.CreateAddItemPlan` / `PlayerEnterWorldService.SaveInventoryRewardMutationAsync` | Runtime inventory persistence | Partial | Unit Tested | Partial Parity | Reward add/merge persistence is wired for pet gifts through existing inventory rows. MySQL execution was not run. |

## Known Gaps

- Java periodic `TaskId.PET_UPDATE` mood update scheduling/cancellation remains incomplete in C#.
- Java gift cooldown audit logging is not modeled.
- C# uses the existing `SmSystemMessage.FullInventory()` packet for reward capacity rejection; exact Java `STR_WAREHOUSE_FULL_INVENTORY` message-id parity should be verified in a later runtime packet UOW if client-visible mismatch is found.
- Java checks inventory full before inspecting condition reward; C# only performs capacity planning when a nonzero reward template exists.
- Real client validation was not run.
- Real MySQL validation was not run.

## Next Recommended Runtime UOW

**UOW-2655 candidate: execute Java pet mood periodic update scheduling for active pets.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: active pets should schedule Java-style periodic mood update sends instead of only responding to immediate subtype 0/1/3 packets.
- Java source method or runtime path: PetMoodService.startCheckingMood schedules TaskId.PET_UPDATE and sends ongoing SM_PET mood updates while the pet remains active.
- C# runtime artifact to wire or fix: GameServerConnection pet mood subtype 0 path, active PlayerOwnedPet timing state, ThreadPoolManager scheduling, active-pet cancellation/dismiss paths, and SmPet.Mood subtype 0/subtype 4 packet sends.
- Client-visible/state effect expected: after live mood checking starts, the server should continue sending mood update packets on the Java interval and cancel them when the pet is no longer active.
- Why this is not preview-only/test-only/documentation-only: it wires live scheduler behavior, sends real server packets, and mutates/uses active pet runtime state.
```

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%
