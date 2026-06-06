# Phase 6 Session 2704 Completion

## UOW

[Phase 6] UOW-2704: Reject full move destinations.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM cross-storage moves now reject full cube and regular warehouse destinations after Java-style auto-merge attempts and before moving/persisting the remaining source item.
- Java source/runtime path: ItemMoveService.moveItem -> optional stack auto-merge -> targetStorage.isFull() -> send targetStorage.getStorageIsFullMessage() -> sendItemUnlockPacket(player, item).
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync reuses CreateStorageFullMessage and source SendStorageUpdatePacketAsync ALL_SLOT unlock.
- Client-visible/state/persistence effect: full cube destination sends STR_WAREHOUSE_FULL_INVENTORY (1390149) and unlocks the warehouse source; full regular warehouse destination sends STR_WAREHOUSE_DEPOSIT_FULL_BASKET (1300421) and unlocks the cube source; move mutation/persistence and destination delete/add packets are suppressed.
- Why this is runtime progress: it changes packet fanout and mutation gating in the live CM_MOVE_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage moves first reject restrictions/trading/shutdown.
  - For `slot == -1`, Java attempts stack merges into destination stacks.
  - After merge attempts, Java checks `targetStorage.isFull()`, sends the destination storage full message, sends `sendItemUnlockPacket(player, item)`, and returns.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` uses `sendStorageUpdatePacket` with `ItemAddType.ALL_SLOT`.
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `CUBE` maps to `STR_WAREHOUSE_FULL_INVENTORY`.
  - Regular/account/legion warehouse families map to `STR_WAREHOUSE_DEPOSIT_FULL_BASKET`.

## C# Changes

- Added a destination-full guard to `HandleMoveItemAsync` after the existing auto-merge loop and before item location/slot mutation.
- Reused the UOW-2703 Java message helper:
  - cube full -> `SmSystemMessage.WarehouseFullInventory()`
  - regular warehouse full -> `SmSystemMessage.WarehouseDepositFullBasket()`
- On rejection, sends the Java-equivalent source unlock with `SmInventoryAddItem.AllSlot` and source storage size.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_FullCubeDestinationSendsJavaStorageFullMessageAndUnlocksSource` | Unit / live connection handler | `ItemMoveService.moveItem`, `IStorage.getStorageIsFullMessage`, and `ItemPacketService.sendItemUnlockPacket` source review | Warehouse-to-cube move into a full cube sends message `1390149`, unlocks the warehouse source with `ALL_SLOT`, sends regular warehouse size, and avoids move persistence. | Socket-backed connection fixture invoking live handler, decoded system message, warehouse add packet, cube update, and repository counters. | Account/legion/pet/house storage full branches remain deferred. |
| `HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource` | Unit / live connection handler | Same Java move full-destination branch | Cube-to-regular-warehouse move into a full warehouse sends message `1300421`, unlocks the cube source with `ALL_SLOT`, sends cube size, and avoids move persistence. | Socket-backed connection fixture invoking live handler, decoded system message, inventory add packet, cube update, and repository counters. | Partial auto-merge followed by full rejection is covered by source ordering review but not a separate regression. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM cross-storage destination-full rejection.
- Specific behavior/contract: Java checks destination fullness after auto-merge attempts, sends the storage-family full message, unlocks the source item with ALL_SLOT, and returns before move mutation/persistence.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullCubeDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet/capacity helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped rejection plus adjacent successful move behavior.
- Why this scope is sufficient: the regressions exercise the branch that previously moved into full destinations and the adjacent successful cross-storage move that must still work.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full cube and regular warehouse destinations now reject after auto-merge attempts with Java message ids and source unlock. Full move flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Full-destination move rejection now restores the source item with `ALL_SLOT` and storage-size packet for cube/regular warehouse sources. Other storage families remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Cube and regular warehouse full messages are used by live move and split handlers. Account, legion, pet, and house storage full messages remain incomplete. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge followed by full-destination rejection is Java-reviewed but not separately asserted.
- Account warehouse destination fullness is still not wired because the current C# runtime model does not yet restore/count account warehouse rows for this handler.
- Legion warehouse history/permissions and non-regular storage families remain deferred.
- `CM_REPLACE_ITEM` restriction, shutdown, and switch ordering are partially covered, but full Java source review is still needed for remaining edge cases.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_REPLACE_ITEM` / `ItemMoveService.switchItemsInStorages` trading and shutdown rejection against current C# and wire the smallest confirmed live mismatch.
2. Inspect `CM_MOVE_ITEM` partial auto-merge followed by full-destination rejection if a focused live mismatch is suspected beyond the source-reviewed ordering.
3. Inspect account warehouse runtime item restore/counting before attempting account warehouse full-destination parity.
