# Phase 6 Session 2709 Handoff

## Completed UOW

[Phase 6] UOW-2709: Reject full account warehouse destinations.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM and CM_SPLIT_ITEM now reject full account warehouse destinations using Java storage capacity semantics.
- Java source/runtime path: ItemMoveService.moveItem, ItemSplitService.splitItem, IStorage.getStorageIsFullMessage, StorageType.ACCOUNT_WAREHOUSE, ItemStorage.isFull.
- C# runtime artifact wired: GameServerConnection.CreateStorageFullMessage and InventoryCapacity account warehouse slot helpers.
- Client-visible/state/persistence effect: full account warehouse destinations send STR_WAREHOUSE_DEPOSIT_FULL_BASKET; move restores/unlocks the source item; move/split do not mutate runtime item lists or persist rows.
- Why this is runtime progress: it changes live client packet rejection behavior and protects live inventory/account warehouse state before mutation or persistence.
```

## Commit

`[Phase 6][UOW-2709] Reject full account warehouse destinations`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/InventoryCapacity.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2709-Completion.md`
- `docs/Phase-6-Session-2709-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/StorageType.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/ItemStorage.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageFullMessage`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Services.InventoryCapacity`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleSplitItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource|FullyQualifiedName~HandleSplitItemAsync_FullWarehouseDestinationSendsJavaStorageFullMessageWithoutMutation|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava" --logger "console;verbosity=minimal"
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

- `CM_MOVE_ITEM` full account warehouse destination now has partial runtime parity: Java warehouse-full message, source unlock fanout, no state mutation, and no persistence.
- `CM_SPLIT_ITEM` split-to-empty full account warehouse destination now has partial runtime parity: Java warehouse-full message, no new item allocation, no state mutation, and no persistence.
- Full storage parity is not claimed.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse full-destination rejection now matches Java message/unlock behavior before mutation. Account warehouse auto-merge remains excluded in C# and is a known next gap. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Account warehouse split-to-empty full destination now sends Java warehouse-full message and avoids item allocation/mutation. Other split branches remain partial. |
| `com.aionemu.gameserver.model.items.storage.IStorage.getStorageIsFullMessage` | `GameServerConnection.CreateStorageFullMessage` | Storage helper / packet selection | Partial | Unit Tested indirectly | Partial Parity | Cube, regular warehouse, and account warehouse messages are modeled. Legion, pet bag, house, broker, and mailbox messages remain deferred. |
| `com.aionemu.gameserver.model.items.storage.StorageType` | `InventoryCapacity` | Enum-derived capacity helper | Partial | Unit Tested indirectly | Partial Parity | Account warehouse base limit 16 is now modeled for live fullness checks. Row length and other storage families remain incomplete. |
| `com.aionemu.gameserver.model.items.storage.ItemStorage.isFull` | `InventoryCapacity.GetFreeAccountWarehouseSlots` | Storage capacity helper | Partial | Unit Tested indirectly | Partial Parity | Counts non-kinah account warehouse rows against the 16-slot limit. Full Java special-cube/template filtering is not relevant to account warehouse in this UOW. |

## Known Gaps / Watchouts

- `CM_MOVE_ITEM` account warehouse auto-merge is still excluded by the C# `slot == -1` branch; Java merges stackable items into any target storage, including account warehouse, before fullness checks.
- Account warehouse split merge-into-existing-stack may already be owner-aware; inspect source before choosing it.
- Account warehouse kinah split/move needs source review for owner/list/persistence behavior.
- Regular warehouse restored-list parity remains inconsistent with older flattened tests.
- Legion warehouse permissions/history, pet bags, and house storage remain deferred.

## Next Recommended Runtime UOW

Recommended candidate: account warehouse auto-merge for live `CM_MOVE_ITEM` when `slot == -1`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: moving a stackable cube item to account warehouse with automatic placement should merge into existing account warehouse stacks before checking fullness.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 branch -> targetStorage.getItemsByItemId -> ItemSplitService.mergeStacks -> Storage.increaseItemCount/decreaseItemCount -> InventoryDAO.store owner resolution.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync auto-merge branch, Player.AccountWarehouseItems lookup, PlayerEnterWorldService.SaveItemMergeMutationAsync, MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect expected: existing account warehouse stack count increases, source cube count decreases or deletes, runtime lists remain owner-correct, warehouse/inventory update packets are sent in Java merge order, and persistence uses account id for the account warehouse target row.
- Why this is runtime progress: it changes live packet handling, runtime inventory/account warehouse state, packets, and persistence for a Java-supported move path currently skipped by C#.
```

Suggested focused validation starting point if implemented:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseAutoMerge|FullyQualifiedName~HandleMoveItemAsync_CubeSourceMovesItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_FullAccountWarehouseDestinationSendsJavaStorageFullMessageAndUnlocksSource" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is added or Java source changes. Broad-validation trigger: none unless the merge persistence helper is broadened beyond the existing owner-aware path.

## Other Safe Runtime Candidates

- Inspect account warehouse split merge-into-existing-stack and proceed only if code, not just coverage, is missing.
- Inspect account warehouse kinah split/move and fix any owner/list/persistence mismatch found.
- Start regular warehouse restored-list cleanup only after scoping the interaction with existing flattened-storage tests.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `7f11e6495 [Phase 6][UOW-2708] Persist account warehouse slots`
  - `f380a8afa [Phase 6][UOW-2707] Replace account warehouse items`
  - `321ab1a6f [Phase 6][UOW-2706] Split account warehouse items`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
