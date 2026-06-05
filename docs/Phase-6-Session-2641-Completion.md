# Phase 6 Session 2641 Completion

## UOW

[Phase 6] UOW-2641: Execute rejected pet food branch live.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_PET FOOD checkFeeding now handles rejected/non-eatable single-count food instead of returning silently after feed-start.
- Java source/runtime path: PetService.checkFeeding branch where foodType is null after PetFlavour.getFoodType/loved-limit validation; Java calls ItemPacketService.sendItemUnlockPacket, sends SM_PET subtype 5, sends SM_EMOTION END_FEEDING, and sends SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR.
- C# runtime artifact wired: GameServerConnection.ExecutePetFeedingCheckAsync now executes PetFeedServiceOperationPlanStatus.RejectedFood using SmInventoryAddItem/SmWarehouseAddItem ALL_SLOT unlock packets, SmCubeUpdate, SmPet subtype 5, SmEmotion END_FEEDING, and SmSystemMessage id 1400618.
- Client-visible/state/persistence effect: invalid single-count food after feed-start restores/unlocks the item in the client UI, ends feeding, sends the Java face-message, and leaves inventory/feed state unchanged.
- Why this is runtime progress: this executes a live handler branch and sends real client-visible packets/messages from runtime code; it is not preview/test/docs-only work.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/toypet/PetService.java`
  - `checkFeeding` sets `foodType = null` for non-eatable food or exhausted loved-food limit.
  - The rejection branch calls `ItemPacketService.sendItemUnlockPacket(player, item)`, sends `SM_PET(5, 0, 0, pet)`, sends END_FEEDING, and sends `STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR(pet.getName(), item.getItemTemplate().getL10n())`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` resolves storage type and calls `sendStorageUpdatePacket(..., ItemAddType.ALL_SLOT)`.
  - Cube storage sends `SM_INVENTORY_ADD_ITEM` then `SM_CUBE_UPDATE`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR` uses message id `1400618` and two ordered string parameters.

## C# Changes

- Added `SmSystemMessage.ToyPetFeedFoodNotLoveFlavor` for Java message id `1400618`.
- Added `GameServerConnection.SendItemUnlockPacketAsync` to send Java `ALL_SLOT` storage updates and trailing cube-size packets for unlock/restore behavior.
- Extended `ExecutePetFeedingCheckAsync` for `PetFeedServiceOperationPlanStatus.RejectedFood`:
  - sends item unlock/storage update,
  - sends `SM_PET` subtype 5 using current feed progress,
  - sends END_FEEDING emotion,
  - sends pet/item face-message,
  - avoids inventory mutation, feed mutation, and feed persistence.

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CmPetFoodRejectedFoodUnlocksItemAndSendsFaceMessageWithoutMutation` | Unit/live connection | `PetService.checkFeeding` rejected-food branch and `ItemPacketService.sendItemUnlockPacket` | Live invalid food sends start packets, ALL_SLOT inventory unlock, cube update, subtype 5, END_FEEDING, and message 1400618 while leaving inventory/feed state unchanged. | Direct packet/state/repository capture from `ProcessPacketAsync`. | Covers cube-storage single-count branch only; non-cube unlock packet path is wired but not covered here. |

## Validation Decision

```text
- Changed surface: live CM_PET FOOD rejected checkFeeding branch, item unlock packet fanout, and system-message helper.
- Specific behavior/contract: invalid single-count food after feed-start should send Java unlock/end/message packets without consuming inventory, mutating feed progress, or persisting feed state.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter FullyQualifiedName~GameServerConnectionBuyItemTests --no-restore
- Focused Java/Maven command: not run; no narrow Java PetService.checkFeeding fixture or game-server/src/test tree was found in this checkout.
- Broad-validation trigger: live handler packet fanout changed.
- Broad .NET decision: skipped after focused coverage because the filtered connection run built affected projects and directly exercised the changed live branch, packet ordering, state non-mutation, and message id/parameters.
```

Result: passed, 78/78. Existing nullable/analyzer warnings were emitted in unrelated surfaces; no failures.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected branch | `Aion.GameServer.Network.Aion.GameServerConnection.ExecutePetFeedingCheckAsync` | Runtime handler | Partial | Unit Tested | Partial Parity | Rejected single-count branch is live. Multi-count, reward/refeed, and repeated scheduling remain deferred. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Network.Aion.GameServerConnection.SendItemUnlockPacketAsync` | Packet fanout | Partial | Unit Tested | Partial Parity | Cube unlock is covered; non-cube warehouse unlock is wired through existing packet type but not covered by this test. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage.ToyPetFeedFoodNotLoveFlavor` | Packet helper | Complete | Unit Tested | Partial Parity | Message id and parameter order are tested through live handler; standalone golden packet coverage was not added. |

## Known Gaps

- Multi-count feed chaining (`ConsumedContinue`) remains deferred.
- Full/reward/refeed branch remains deferred, including reward item creation, subtype 6/7, refeed scheduling/persistence, and feed reset.
- Java random loved reward selection is not live-wired.
- Non-cube rejected-food unlock packet path is wired but not covered by this UOW's focused live test.
- Real client validation was not run.
