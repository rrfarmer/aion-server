# Phase 6 Session 2708 Handoff

## Completed UOW

[Phase 6] UOW-2708: Persist account warehouse same-storage slots.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM and same-storage CM_REPLACE_ITEM now persist restored account warehouse slot changes against account-owned inventory rows.
- Java source/runtime path: ItemMoveService.moveInSameStorage and switchItemsInStorages same-storage branch -> item.setEquipmentSlot -> InventoryDAO.store/getItemOwnerId.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GameServerConnection.HandleReplaceItemAsync, PlayerEnterWorldService.SaveInventoryItemSlotAsync, MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync.
- Client-visible/state/persistence effect: account warehouse same-storage item slots mutate in runtime state and persist with item_owner = accountId; no response packets are sent, matching Java.
- Why this is runtime progress: it changes live packet handling and database persistence through the existing inventory table.
```

## Commit

`[Phase 6][UOW-2708] Persist account warehouse slots`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2708-Completion.md`
- `docs/Phase-6-Session-2708-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveInventoryItemSlotAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync`
- `Aion.GameServer.Data.EmptyPlayerEnterWorldRepository`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSameStorageSwitchPersistsSlotsWithAccountOwnerLikeJava|FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_ShutdownSoonUnlocksBothItemsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems|FullyQualifiedName~HandleReplaceItemAsync_AccountWarehouseSaveFailureRollsBackListsAndOwners" --logger "console;verbosity=minimal"
```

Result:

- Passed: 6
- Failed: 0
- Skipped: 0
- Existing warnings only.

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

- `CM_MOVE_ITEM` same-storage account warehouse reorder now has partial runtime parity for restored account rows: runtime slot mutation, account-owner persistence, and no packets.
- `CM_REPLACE_ITEM` same-storage account warehouse switch now has partial runtime parity for restored account rows: both slots swap, both account-owned rows persist, and no packets are emitted.
- Full inventory/storage parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveInSameStorage` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage account warehouse move now mutates runtime slot and persists with account id. Other storage-family edge cases remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-storage account warehouse replace now swaps slots, persists both account-owned rows, and sends no packets. Cross-storage account warehouse behavior was covered in UOW-2707; full storage parity remains open. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `PlayerEnterWorldService.SaveInventoryItemSlotAsync` / `MySqlPlayerEnterWorldRepository.SaveInventoryItemSlotAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Slot persistence now uses account id for account warehouse and player id otherwise. Full Java dirty-item store semantics are not claimed. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Account warehouse same-storage paths operate on restored `Player.AccountWarehouseItems`. Regular warehouse restored-list parity remains unresolved. |

## Known Gaps / Watchouts

- Account warehouse merge-into-existing-stack may already be owner-aware from earlier UOWs; do not run a test-only UOW. Inspect Java/C# first and continue only if a live code mismatch is found.
- Account warehouse kinah split/move needs source review for owner/list/persistence behavior; continue only if the review finds a runtime gap.
- Account warehouse capacity/fullness is not fully wired.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: account warehouse storage capacity/fullness for live move/split destinations, if source review confirms the current C# capacity check does not model Java account warehouse limits.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live account warehouse move/split destinations should reject full storage using Java storage capacity semantics.
- Java source/runtime path: ItemMoveService.moveItem / splitItem -> Storage.isFull or equivalent storage-limit checks for ACCOUNT_WAREHOUSE.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync destination capacity checks, HandleSplitItemAsync destination capacity checks, account warehouse capacity helpers, and storage-full packet selection.
- Client-visible/state/persistence effect expected: full account warehouse destinations should send the Java storage-full system message and unlock/restore source client state without mutating or persisting rows.
- Why this is runtime progress: it changes live packet rejection behavior and protects runtime inventory/account warehouse state.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~AccountWarehouse|FullyQualifiedName~Full" --logger "console;verbosity=minimal"
```

Use that only after implementing a real code fix; do not count coverage alone as progress.

## Other Safe Runtime Candidates

- Inspect account warehouse kinah split/move and fix any owner/list/persistence mismatch found.
- Inspect account warehouse merge-into-existing-stack and proceed only if code, not just coverage, is missing.
- Start regular warehouse restored-list cleanup only after scoping the interaction with existing flattened-storage tests.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `f380a8afa [Phase 6][UOW-2707] Replace account warehouse items`
  - `321ab1a6f [Phase 6][UOW-2706] Split account warehouse items`
  - `6915ba305 [Phase 6][UOW-2705] Move account warehouse items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
