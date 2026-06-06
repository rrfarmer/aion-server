# Phase 6 Session 2705 Handoff

## Completed UOW

[Phase 6] UOW-2705: Move account warehouse items with owner parity.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM now supports explicit-slot cube/account-warehouse moves using restored Player.AccountWarehouseItems.
- Java source/runtime path: Player.getStorage(StorageType.ACCOUNT_WAREHOUSE) -> ItemMoveService.moveItem remove/add -> Storage.add item.setItemLocation -> InventoryDAO.store/updateItems getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, InventoryItem.OwnerId, PlayerEnterWorldService.SaveItemCrossStorageMoveMutationAsync, MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect: source delete and destination add packets are sent; items move between InventoryItems and AccountWarehouseItems; inventory.item_owner changes playerId <-> accountId with item_location and slot.
- Why this is runtime progress: live packet handling, runtime storage state, and database persistence changed. It is not preview-only/test-only/documentation-only.
```

## Commit

`[Phase 6][UOW-2705] Move account warehouse items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/InventoryItem.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2705-Completion.md`
- `docs/Phase-6-Session-2705-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageItems`
- `Aion.GameServer.Network.Aion.GameServerConnection.MoveItemBetweenRuntimeStorages`
- `Aion.GameServer.Model.GameObjects.InventoryItem.OwnerId`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveItemCrossStorageMoveMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
- Failed: 0
- Skipped: 0

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- Explicit-slot `CM_MOVE_ITEM` cube/account-warehouse boundary moves now have partial runtime parity for state membership, packet fanout, and row owner/location/slot persistence.
- Full `CM_MOVE_ITEM`, account warehouse stack merging, account warehouse split/replace, and all storage-family parity are not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Explicit-slot cube/account-warehouse cross moves now mutate the correct runtime lists, send delete/add packets, and persist owner/location/slot. Auto-merge, split, replace, fullness, legion, pet, and house storage remain partial. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | `ACCOUNT_WAREHOUSE` lookup now uses `Player.AccountWarehouseItems` for live move handling. Regular warehouse remains represented inconsistently in older handler tests and needs future cleanup. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Cross-storage move persistence now updates `item_owner` to account id for account warehouse and player id otherwise. Full dirty-item store parity and switch/split/merge owner transitions remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.Storage.add` | `MoveItemBetweenRuntimeStorages` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Account warehouse boundary moves update runtime storage membership before delete/add packet fanout. Quest item-get side effects and all storage families are not fully covered. |

## Known Gaps / Watchouts

- Account warehouse stack auto-merge is intentionally not wired yet because merge persistence still assumes player-owned rows.
- `CM_SPLIT_ITEM` and `CM_REPLACE_ITEM` still need restored account warehouse source/target lookup and owner-aware persistence.
- Same-storage account warehouse slot reorder needs owner-aware slot persistence.
- Account warehouse destination fullness is not wired.
- Regular warehouse restored-list parity remains risky: enter-world loads `WarehouseItems`, but older live inventory handler tests model storage id `1` in `InventoryItems`.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: wire `CM_SPLIT_ITEM` for restored account warehouse rows, but only if the same UOW also updates split/merge persistence to use Java owner semantics.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM should split or merge items when source or destination is the restored account warehouse list, instead of ignoring account warehouse rows.
- Java source/runtime path: ItemSplitService.splitItem and moveKinah/mergeStacks branches using Player.getStorage(ACCOUNT_WAREHOUSE), Storage.add/decreaseItemCount, and InventoryDAO.store owner resolution.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync, account-aware storage lookup/list mutation helpers, PlayerEnterWorldService/MySqlPlayerEnterWorldRepository SaveItemSplitMutationAsync and SaveItemMergeMutationAsync owner handling.
- Client-visible/state/persistence effect expected: split/merge packets update cube/account warehouse UI, runtime lists mutate, source/target counts persist, new split rows use account id when created in account warehouse, and moved stack counts update under the correct old owner.
- Why this is runtime progress: it would execute a live client packet path, mutate inventory/account warehouse state, send packets, and persist through the existing inventory table.
```

Suggested focused validation starting point if the fix is implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if implemented: restored account warehouse source/destination items are found by live split handling, split rows and merge count updates persist under Java owner rules, and adjacent existing cube split/merge behavior still works.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or general dirty-item persistence are changed.

## Other Safe Runtime Candidates

- Wire `CM_REPLACE_ITEM` account warehouse row lookup and owner-aware two-item switch persistence.
- Wire same-storage account warehouse slot reorder with owner-aware `SaveInventoryItemSlotAsync`.
- Inspect and fix regular warehouse restored-list handling for move/split/replace, but keep scope small because existing tests use flattened storage id `1` rows.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `c1c6bb3dc [Phase 6][UOW-2704] Reject full move destinations`
  - `7a3ab5a13 [Phase 6][UOW-2703] Reject full split destinations`
  - `6505d4840 [Phase 6][UOW-2702] Send split restriction denials`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
