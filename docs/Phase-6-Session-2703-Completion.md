# Phase 6 Session 2703 Completion

## UOW

[Phase 6] UOW-2703: Reject full split destinations.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM split-to-empty-slot now rejects full cube and regular warehouse destinations before allocating an item id or mutating inventory.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null branch -> destStorage.isFull() -> PacketSendUtility.sendPacket(player, destStorage.getStorageIsFullMessage()).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem == null branch, plus SmSystemMessage factories for Java full-storage message ids.
- Client-visible/state/persistence effect: full cube destination sends STR_WAREHOUSE_FULL_INVENTORY (1390149), full regular warehouse destination sends STR_WAREHOUSE_DEPOSIT_FULL_BASKET (1300421), and split state/persistence packets are suppressed.
- Why this is runtime progress: it changes packets and mutation gating in the live CM_SPLIT_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - In the `targetItem == null` branch, Java checks `destStorage.isFull()` and returns after sending the destination storage full message.
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `CUBE` maps to `STR_WAREHOUSE_FULL_INVENTORY`.
  - Regular/account/legion warehouse families map to `STR_WAREHOUSE_DEPOSIT_FULL_BASKET`.
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`
  - Normal storage fullness is `getCubeItems().size() >= limit`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
  - `STR_WAREHOUSE_DEPOSIT_FULL_BASKET` uses message id `1300421`.
  - `STR_WAREHOUSE_FULL_INVENTORY` uses message id `1390149`.

## C# Changes

- Added Java full-storage system message factories:
  - `SmSystemMessage.WarehouseDepositFullBasket()`
  - `SmSystemMessage.WarehouseFullInventory()`
- Added `CreateStorageFullMessage` in `GameServerConnection` and wired it before `IDFactory.NextId()`, source count mutation, inventory insertion, and persistence in `HandleSplitItemAsync`.
- Used existing Java-derived capacity helpers for cube limit and regular warehouse limit, counting regular warehouse rows from the live flattened `player.InventoryItems` storage model used by current handlers.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_FullCubeDestinationSendsJavaStorageFullMessageWithoutMutation` | Unit / live connection handler | `ItemSplitService.splitItem`, `IStorage.getStorageIsFullMessage`, `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_FULL_INVENTORY` source review | Full cube split destination sends message `1390149`, does not decrement source count, does not add a split item, and emits no split fanout packets. | Socket-backed connection fixture invoking the live private handler with full cube rows and decoded system message assertion. | Special cube and account/legion storage fullness remain outside this test. |
| `HandleSplitItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation` | Unit / live connection handler | `ItemSplitService.splitItem`, `IStorage.getStorageIsFullMessage`, `SM_SYSTEM_MESSAGE.STR_WAREHOUSE_DEPOSIT_FULL_BASKET` source review | Full regular warehouse split destination sends message `1300421`, does not decrement source count, does not add a split item, and emits no split fanout packets. | Socket-backed connection fixture invoking the live private handler with full regular warehouse rows and decoded system message assertion. | Account and legion warehouse full handling remains partial/deferred. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM targetItem-null destination-full rejection.
- Specific behavior/contract: Java sends the destination storage family full message and returns before split mutation/persistence.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullCubeDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleSplitItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and two system-message factories.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped rejection plus adjacent successful split behavior.
- Why this scope is sufficient: the regressions exercise the branch that previously split into full destinations and the adjacent successful empty-slot branch that must still work.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full cube and regular warehouse split-to-empty-slot destinations now reject before mutation/persistence with Java message ids. Full split flow remains partial. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Cube and regular warehouse messages are wired. Account, legion, pet, and house storage full messages remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` | `InventoryCapacity` plus `GameServerConnection.GetRegularWarehouseFreeSlots` | Storage capacity helper | Partial | Unit Tested indirectly | Partial Parity | Uses existing Java-derived cube/warehouse limits; regular warehouse counts live flattened storage rows. Special cube and non-regular storage families remain deferred here. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Packet factory | Partial | Unit Tested indirectly | Partial Parity | Added factories for `1300421` and `1390149`; packet serialization is covered through decoded live handler packets. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Account warehouse destination fullness is still not wired because the current C# runtime model does not yet restore/count account warehouse rows for this handler.
- Legion warehouse, pet warehouse, house storage, special cube, and legion history behavior remain deferred.
- `CM_MOVE_ITEM` and `CM_REPLACE_ITEM` destination-full handling still need source review before any runtime UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` empty-slot destination-full handling against Java `ItemMoveService.moveItem` and wire the smallest confirmed live mismatch for cube/regular warehouse if present.
2. Inspect `CM_REPLACE_ITEM` destination/full-storage guards if Java source shows a live packet/state mismatch independent of broad legion scaffolding.
3. Inspect account warehouse runtime item restore/counting before attempting account warehouse full-destination parity.
