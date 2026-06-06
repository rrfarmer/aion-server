# Phase 6 Session 2710 Handoff

## Completed UOW

[Phase 6] UOW-2710: Merge account warehouse move stacks.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM automatic placement now merges stackable items into existing account warehouse stacks before full-storage rejection.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 branch -> targetStorage.getItemsByItemId -> ItemSplitService.mergeStacks -> Storage.increaseItemCount/decreaseItemCount -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync auto-merge branch, Player.AccountWarehouseItems lookup, PlayerEnterWorldService.SaveItemMergeMutationAsync, MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect: existing account warehouse stack count increases, source cube stack decreases/deletes, Java merge update/delete packets are sent, and persistence uses source/target OwnerId values.
- Why this is runtime progress: it changes live packet handling, runtime inventory/account warehouse state, packets, and persistence for a Java-supported move path previously skipped by C#.
```

## Commit

`[Phase 6][UOW-2710] Merge account warehouse move stacks`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2710-Completion.md`
- `docs/Phase-6-Session-2710-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageItems`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveItemMergeMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseAutoMergeFillsExistingStackBeforeFullCheckLikeJava|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 5
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

- `CM_MOVE_ITEM` cube-to-account warehouse automatic stack merge now has partial runtime parity for the fully consumed source path.
- Full move parity is not claimed. Partial merge remainder, account-source merge directions, regular warehouse restored lists, and other storage families remain incomplete.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse target stacks now participate in auto-merge before fullness checks. Remaining auto-merge directions and regular restored warehouse list cleanup remain partial. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` auto-merge branch | Service / stack mutation | Partial | Unit Tested | Partial Parity | Cross-storage merge packet/state behavior is covered for cube-to-account full consumption. Partial merge remainder and account-source merge directions need focused follow-up if code changes. |
| `com.aionemu.gameserver.dao.InventoryDAO.store/updateItems/getItemOwnerId` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Existing merge persistence uses each runtime item's OwnerId, so account warehouse target updates persist against account id. Broader dirty-item store parity remains incomplete. |
| `com.aionemu.gameserver.model.gameobjects.player.Player.getStorage` | `GameServerConnection.GetMoveStorageItems` | Runtime storage lookup | Partial | Unit Tested indirectly | Partial Parity | Auto-merge now reaches restored account warehouse rows. Regular warehouse restored-list parity remains unresolved because storage 1 still uses the flattened inventory list in this handler. |

## Known Gaps / Watchouts

- Regular warehouse restored-list parity remains inconsistent: Java `Player.getStorage(REGULAR_WAREHOUSE)` returns regular warehouse storage, while current move/split helpers still map storage `1` to `InventoryItems`.
- Account warehouse partial auto-merge with a remaining source stack is enabled by this branch but not specifically tested with account warehouse destination.
- Account warehouse source-to-cube auto-merge is enabled by this branch but not specifically tested.
- Account warehouse split merge-into-existing-stack should be inspected before any runtime fix.
- Account warehouse kinah split/move still needs source review for owner/list/persistence behavior.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: regular warehouse restored-list move parity for `CM_MOVE_ITEM`, starting with moving a restored regular warehouse row to cube.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: live CM_MOVE_ITEM should operate on restored Player.WarehouseItems for storage 1 instead of relying on flattened InventoryItems rows.
- Java source/runtime path: ItemMoveService.moveItem -> Player.getStorage(REGULAR_WAREHOUSE) -> Storage.remove/add -> ItemPacketService sendItemDeletePacket/sendStorageUpdatePacket -> InventoryDAO.store.
- C# runtime artifact likely involved: GameServerConnection.GetMoveStorageItems/SetMoveStorageItems/RemoveMoveStorageItem/AddMoveStorageItem, HandleMoveItemAsync, Player.WarehouseItems, and SaveItemCrossStorageMoveMutationAsync.
- Client-visible/state/persistence effect expected: moving a restored regular warehouse item to cube removes it from Player.WarehouseItems, adds it to Player.InventoryItems with player owner/location/slot, sends warehouse delete plus cube add packets, and persists the existing inventory row location/slot through the current schema.
- Why this is runtime progress: it changes live packet handling, runtime warehouse/inventory list mutation, packets, and persistence for restored database state.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_RegularWarehouseSourceMovesRestoredItemToCubeLikeJava|FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless the storage helper changes are broadened beyond move/split runtime inventory paths.

## Other Safe Runtime Candidates

- Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
- Inspect account warehouse kinah split/move and fix any owner/list/persistence mismatch found.
- Add focused account warehouse partial/source auto-merge coverage only alongside a real code fix; do not run it as a test-only UOW.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `aa483b09e [Phase 6][UOW-2709] Reject full account warehouse destinations`
  - `7f11e6495 [Phase 6][UOW-2708] Persist account warehouse slots`
  - `f380a8afa [Phase 6][UOW-2707] Replace account warehouse items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
