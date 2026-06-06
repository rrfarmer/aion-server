# Phase 6 Session 2706 Handoff

## Completed UOW

[Phase 6] UOW-2706: Split account warehouse items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM now handles non-kinah empty-slot splits between cube and restored Player.AccountWarehouseItems.
- Java source/runtime path: ItemSplitService.splitItem -> Player.getStorage(ACCOUNT_WAREHOUSE) -> Storage.decreaseItemCount -> SM_CUBE_UPDATE.cubeSize -> Storage.add -> InventoryDAO.store owner resolution.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync, HandleKinahMoveAsync destination lookup, account-aware storage helpers, MySqlPlayerEnterWorldRepository SaveItemSplitMutationAsync/SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect: source decrease packets and destination add packets are emitted; runtime rows are added to InventoryItems or AccountWarehouseItems; source counts and new split rows persist under playerId/accountId item_owner as Java does.
- Why this is runtime progress: it changes a live client packet path, mutates runtime inventory/account warehouse state, sends server packets, and persists through the existing database shape.
```

## Commit

`[Phase 6][UOW-2706] Split account warehouse items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2706-Completion.md`
- `docs/Phase-6-Session-2706-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleKinahMoveAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageItems`
- `Aion.GameServer.Network.Aion.GameServerConnection.AddMoveStorageItem`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemSplitMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
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

- Empty-slot non-kinah `CM_SPLIT_ITEM` cube/account-warehouse boundary splits now have partial runtime parity for runtime state, packet fanout, and owner-aware persistence.
- Full split parity is not claimed. Account stack merge tests, account kinah split tests, account same-storage slot updates, and capacity behavior remain open.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Non-kinah empty-slot splits now work across cube/account warehouse with correct runtime lists, owner ids, source updates, destination adds, and persistence captures. Merge path now uses owner-aware persistence but account merge target coverage remains incomplete. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleSplitItemAsync` / `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Service / persistence | Partial | Unit Tested indirectly | Partial Parity | Count/delete persistence now uses each item's owner id, allowing future account warehouse stack merges to save correctly. Account merge packet/state tests remain needed. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemSplitMutationAsync` / `SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Split source counts and new rows use item owner ids. Full dirty-item store parity, switch owner transitions, and same-storage account slot updates remain incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Split handling now uses account warehouse runtime storage list. Regular warehouse restored-list parity remains unresolved. |

## Known Gaps / Watchouts

- Account warehouse merge-into-existing-stack needs explicit packet/state tests.
- Account warehouse kinah split/move lookup was improved, but no focused kinah account warehouse test was added.
- Same-storage account warehouse slot reorder needs owner-aware slot persistence.
- `CM_REPLACE_ITEM` still does not use restored account warehouse rows or owner-aware switch persistence.
- Account warehouse destination fullness/capacity remains unwired.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.

## Next Recommended Runtime UOW

Recommended candidate: finish `CM_SPLIT_ITEM` account warehouse merge-into-existing-stack behavior, including the source-depleted delete branch.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_SPLIT_ITEM should merge account warehouse source/destination stacks and delete depleted sources from the correct runtime storage list.
- Java source/runtime path: ItemSplitService.mergeStacks -> destStorage.increaseItemCount -> sourceStorage.decreaseItemCount/delete -> ItemPacketService item update/delete packets -> InventoryDAO.store owner-based count/delete updates.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync merge branch, RemoveMoveStorageItem, MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect expected: account warehouse target stack receives INC_ITEM_COLLECT/INC_ITEM_MERGE packet, source stack receives decrease or delete packet from account/cube storage, runtime source list is updated, and count/delete persistence uses source/target owner ids.
- Why this is runtime progress: it would execute a live client packet path, mutate inventory/account warehouse stack state, send packets, and persist/delete rows using the existing inventory table.
```

Suggested focused validation starting point if the fix is implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceMergesIntoCubeStackLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceMergesIntoAccountWarehouseStackLikeJava|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
```

Behavior this should prove if implemented: account warehouse merge targets/source rows are found from restored storage lists, update/delete packets match Java storage families, runtime lists remove depleted sources correctly, and owner-aware merge persistence records source/target items.

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless shared packet primitives or general dirty-item persistence are changed.

## Other Safe Runtime Candidates

- Wire `CM_REPLACE_ITEM` account warehouse rows with owner-aware two-item switch persistence.
- Wire same-storage account warehouse slot reorder with owner-aware `SaveInventoryItemSlotAsync`.
- Add account warehouse kinah split/move coverage now that destination lookup is account-aware.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `6915ba305 [Phase 6][UOW-2705] Move account warehouse items`
  - `c1c6bb3dc [Phase 6][UOW-2704] Reject full move destinations`
  - `7a3ab5a13 [Phase 6][UOW-2703] Reject full split destinations`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
