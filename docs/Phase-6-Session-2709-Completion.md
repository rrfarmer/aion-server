# Phase 6 Session 2709 Completion

## UOW

[Phase 6] UOW-2709: Reject full account warehouse destinations.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM and CM_SPLIT_ITEM now reject full account warehouse destinations using Java storage capacity semantics.
- Java source/runtime path: ItemMoveService.moveItem, ItemSplitService.splitItem, IStorage.getStorageIsFullMessage, StorageType.ACCOUNT_WAREHOUSE, and ItemStorage.isFull.
- C# runtime artifact wired: GameServerConnection.CreateStorageFullMessage and InventoryCapacity account warehouse slot helpers.
- Client-visible/state/persistence effect: full account warehouse destinations send STR_WAREHOUSE_DEPOSIT_FULL_BASKET; move restores/unlocks the source item; move/split do not mutate runtime item lists or persist rows.
- Why this is runtime progress: this changes live client packet rejection behavior and protects live inventory/account warehouse state before mutation or persistence.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` checks `targetStorage.isFull()` after merge attempts and before remove/add mutation.
  - On full destination it sends `targetStorage.getStorageIsFullMessage()` and `sendItemUnlockPacket`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `splitItem` checks `destStorage.isFull()` before creating the split item and sends only `destStorage.getStorageIsFullMessage()`.
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `getStorageIsFullMessage` returns `STR_WAREHOUSE_DEPOSIT_FULL_BASKET` for regular, account, and legion warehouse.
- `game-server/src/com/aionemu/gameserver/model/items/storage/StorageType.java`
  - `ACCOUNT_WAREHOUSE(2, 16, 8)` defines the account warehouse limit.
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`
  - `isFull` checks normal item slots against the storage limit.

## C# Changes

- Added account warehouse capacity helpers to `InventoryCapacity` with the Java base limit of 16 slots.
- `CreateStorageFullMessage` now checks storage `2` and returns `SmSystemMessage.WarehouseDepositFullBasket()` when account warehouse is full.
- Focused handler tests now fill `Player.AccountWarehouseItems` directly, matching the restored account warehouse runtime list.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource` | Unit / live connection handler | `ItemMoveService.moveItem`, `IStorage.getStorageIsFullMessage`, `StorageType.ACCOUNT_WAREHOUSE` source review | Full account warehouse move destination sends Java warehouse-full message, restores the cube source item with ALL_SLOT packet fanout, avoids persistence, and leaves runtime lists unchanged. | Socket-backed handler fixture, decoded packets, runtime list assertions, repository no-call assertions. | Does not cover legion/pet/house full messages. |
| `HandleSplitItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation` | Unit / live connection handler | `ItemSplitService.splitItem`, `IStorage.getStorageIsFullMessage`, `StorageType.ACCOUNT_WAREHOUSE` source review | Full account warehouse split-to-empty-slot destination sends Java warehouse-full message, does not allocate/add the split item, and avoids persistence. | Socket-backed handler fixture, decoded packet, runtime list assertions, repository no-call assertions. | Split merge-into-existing-stack is not a full-storage path and remains separately scoped. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM/CM_SPLIT_ITEM destination full checks and account warehouse capacity helper.
- Specific behavior/contract: Java account warehouse limit is 16 normal slots; full account warehouse destinations emit STR_WAREHOUSE_DEPOSIT_FULL_BASKET before mutation/persistence, with move additionally unlocking the source item.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleSplitItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleSplitItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 6 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to account warehouse storage-full selection and directly related live handlers.
- Broad .NET decision: skipped; the filtered command built the affected project and proved new account warehouse rejection plus adjacent regular warehouse/account warehouse success paths.
- Why this scope is sufficient: the tests exercise both live packet handlers that call CreateStorageFullMessage for account warehouse destinations and assert the Java-derived packet/state/persistence contract.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse full-destination rejection now matches Java message/unlock behavior before mutation. Account warehouse auto-merge remains excluded in C# and is a known next gap. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse split-to-empty full destination now sends Java warehouse-full message and avoids item allocation/mutation. Other split branches remain partial. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / packet selection | Partial | Unit Tested indirectly | Partial Parity | Cube, regular warehouse, and account warehouse messages are modeled. Legion, pet bag, house, broker, and mailbox messages remain deferred. |
| `com.aionemu.gameserver.model.items.storage.StorageType` | `InventoryCapacity` | Enum-derived capacity helper | Partial | Unit Tested indirectly | Partial Parity | Account warehouse base limit 16 is now modeled for live fullness checks. Row length and other storage families remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` | `InventoryCapacity.GetFreeAccountWarehouseSlots` | Storage capacity helper | Partial | Unit Tested indirectly | Partial Parity | Counts non-kinah account warehouse rows against the 16-slot limit. Full Java special-cube/template filtering is not relevant to account warehouse in this UOW. |

## Known Gaps

- `CM_MOVE_ITEM` account warehouse auto-merge is still excluded by C# when `slot == -1`; Java merges stackable items into account warehouse stacks before fullness checks.
- Account warehouse merge-into-existing-stack for `CM_SPLIT_ITEM` still needs source review before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire account warehouse auto-merge for `CM_MOVE_ITEM` when `slot == -1`, using Java `ItemMoveService.moveItem` / `ItemSplitService.mergeStacks`.
2. Inspect account warehouse split merge-into-existing-stack and proceed only if a live owner/list/persistence mismatch is found.
3. Inspect account warehouse kinah split/move and fix any live owner/list/persistence mismatch found.
