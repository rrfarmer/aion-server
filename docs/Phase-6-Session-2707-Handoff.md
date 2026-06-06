# Phase 6 Session 2707 Handoff

## Completed UOW

[Phase 6] UOW-2707: Replace account warehouse items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM now switches cube rows with restored Player.AccountWarehouseItems.
- Java source/runtime path: ItemMoveService.switchItemsInStorages -> Player.getStorage(ACCOUNT_WAREHOUSE) -> Storage.remove/add -> ItemPacketService delete/add packets -> InventoryDAO.store/getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync, account-aware storage helpers, PlayerEnterWorldService.SaveItemStorageSwitchMutationAsync, MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync.
- Client-visible/state/persistence effect: Java-order delete/add packets are emitted; runtime rows move between InventoryItems and AccountWarehouseItems; OwnerId and persisted item_owner change playerId <-> accountId with location/slot for both rows.
- Why this is runtime progress: it changes live packet handling, runtime state mutation, and persistence through the existing inventory table.
```

## Commit

`[Phase 6][UOW-2707] Replace account warehouse items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2707-Completion.md`
- `docs/Phase-6-Session-2707-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageItems`
- `Aion.GameServer.Network.Aion.GameServerConnection.RemoveMoveStorageItem`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddMoveStorageItem`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveItemStorageSwitchMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSwitchMovesRestoredRowsAndOwnersLikeJava|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSaveFailureRollsBackListsAndOwners|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems" --logger "console;verbosity=minimal"
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

- `CM_REPLACE_ITEM` cube/account-warehouse cross-storage switch now has partial runtime parity for restored account rows, packet order, runtime list/owner mutation, and owner-aware persistence.
- Full replace parity is not claimed. Same-storage account warehouse slot reorder and non-modeled storage families remain open.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube/account warehouse cross-storage switch now resolves restored account rows, mutates runtime lists/owners/slots, emits Java delete/add order, and persists both rows with owner-aware location updates. Same-storage account warehouse reorder and full storage-family parity remain incomplete. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Switch persistence now uses account id for account warehouse old/new locations and player id otherwise. Full dirty-item store parity and other storage families remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Replace handling now uses account warehouse runtime storage list. Regular warehouse restored-list parity remains unresolved. |
| `com.aionemu.gameserver.model.items.storage.Storage.add/remove` | `RemoveMoveStorageItem` / `AddMoveStorageItem` / `SendStorageUpdatePacketAsync` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Cross-storage replace now removes both old rows and adds each item to the opposite runtime list before packet fanout. Full Java storage state and handler side effects are not fully covered. |

## Known Gaps / Watchouts

- Same-storage account warehouse slot reorder still uses player-owner slot persistence and remains unwired.
- Account warehouse merge-into-existing-stack has owner-aware persistence support but still needs explicit packet/state tests.
- Account warehouse kinah split/move needs focused coverage.
- Account warehouse capacity/fullness remains unwired.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: same-storage account warehouse slot reorder for `CM_MOVE_ITEM` and `CM_REPLACE_ITEM`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live same-storage account warehouse item reorder/switch should persist slot changes for rows owned by account id instead of player id.
- Java source/runtime path: ItemMoveService.moveInSameStorage and switchItemsInStorages same-storage branch -> item.setEquipmentSlot -> InventoryDAO.store owner resolution for ACCOUNT_WAREHOUSE.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync same-storage path, HandleReplaceItemAsync same-storage path, PlayerEnterWorldService/MySqlPlayerEnterWorldRepository SaveInventoryItemSlotAsync owner-aware variant.
- Client-visible/state/persistence effect expected: account warehouse slot changes mutate runtime item slots and persist `inventory.slot` using `item_owner = accountId`; no response packets are sent, matching Java/client UI behavior.
- Why this is runtime progress: it changes live client packet handling and database persistence for restored account warehouse state.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSameStorageSwitchPersistsSlotsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if implemented: same-storage account warehouse move/replace mutates slots, persists using account owner capture, sends no packets, and adjacent cross-storage replace still emits Java delete/add ordering.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared persistence helpers are broadened beyond slot-owner selection.

## Other Safe Runtime Candidates

- Add runtime coverage and any missing list fixes for account warehouse merge-into-existing-stack in `CM_SPLIT_ITEM`.
- Add account warehouse kinah split/move coverage and fix any owner/list gaps found.
- Start regular warehouse restored-list cleanup only after scoping the interaction with existing flattened-storage tests.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `321ab1a6f [Phase 6][UOW-2706] Split account warehouse items`
  - `6915ba305 [Phase 6][UOW-2705] Move account warehouse items`
  - `c1c6bb3dc [Phase 6][UOW-2704] Reject full move destinations`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
