# Phase 6 Session 2736 Completion

## UOW

[Phase 6] UOW-2736: Persist legion warehouse same-slot moves with legion owner.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: same-storage `CM_MOVE_ITEM` slot moves inside legion warehouse now persist against the legion-owned inventory row.
- Java source/runtime path: `ItemMoveService.moveInSameStorage` marks the item slot dirty, and `InventoryDAO.getItemOwnerId` uses legion id for `StorageType.LEGION_WAREHOUSE`.
- C# runtime artifact wired: `PlayerEnterWorldService.SaveInventoryItemSlotAsync`, called by `GameServerConnection.HandleMoveItemAsync` same-storage branch.
- Client-visible/state/persistence effect: moving a legion warehouse item to another slot updates live item slot state and persists `slot` with `item_owner = player.LegionId`; same-storage moves remain packet-silent like Java.
- Why this is runtime progress: it fixes live database persistence from a live client packet handler using the existing inventory table shape.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveInSameStorage` updates the item equipment slot and marks the storage/item dirty without sending packets.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - `getItemOwnerId` returns account id for account warehouse, legion id for legion warehouse when available, and player id otherwise.

## C# Changes

- Updated `PlayerEnterWorldService.SaveInventoryItemSlotAsync` owner selection to mirror Java owner-id rules:
  - storage type `2` -> `player.AccountId`
  - storage type `3` with a legion -> `player.LegionId`
  - all other storage -> `player.ObjectId`
- Added a live handler test for same-storage legion warehouse slot moves proving slot state changes, persistence uses legion owner id, and no packets are sent.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava` | Unit / live handler persistence | `ItemMoveService.moveInSameStorage` + `InventoryDAO.getItemOwnerId` | Same-storage legion warehouse slot move updates live item slot, persists with legion id owner, and sends no packets. | Invokes live `HandleMoveItemAsync` and inspects repository slot persistence call plus runtime item state. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |
| `HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava` | Unit / adjacent live handler persistence | `InventoryDAO.getItemOwnerId` | Existing account warehouse owner-id behavior still persists with account id after adding legion owner handling. | Focused validation kept the adjacent owner branch green. | Does not cover regular warehouse in this UOW. |

## Validation Decision

```text
- Changed surface: live persistence helper used by same-storage `CM_MOVE_ITEM`.
- Specific behavior/contract: storage type `3` same-storage slot persistence uses Java's legion owner id while remaining packet-silent.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_LegionWarehouseSameStorageSlotPersistsWithLegionOwnerLikeJava|FullyQualifiedName~HandleMoveItemAsync_AccountWarehouseSameStorageSlotPersistsWithAccountOwnerLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this owner-id branch in the checkout.
- Broad-validation trigger: live persistence helper touched.
- Broad .NET decision: skipped; focused live handler tests compiled the affected project and directly proved the changed owner-id branches.
- Why this scope is sufficient: the changed helper is exercised through live move handling for the new legion branch and adjacent account branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveInSameStorage` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Same-storage slot mutation remains packet-silent and now persists legion owner id correctly through service helper. |
| `com.aionemu.gameserver.dao.InventoryDAO.getItemOwnerId` | `PlayerEnterWorldService.SaveInventoryItemSlotAsync` | Persistence owner-id helper | Partial | Unit Tested through live handler | Partial Parity | Account and legion owner branches are covered for slot persistence; other inventory persistence paths remain separate helpers. |

## Known Gaps

- C# still stores legion warehouse rows as flattened `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- Enter-world loading currently loads cube, regular warehouse, and account warehouse items, but does not load legion warehouse rows into runtime location `3` state.
- This UOW covers slot persistence owner id only; cross-storage move/switch helpers already use repository `GetStorageOwnerId` and were not changed.
- Exact Java database row output was not captured; source review and focused repository-call assertions are the evidence.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Load legion warehouse inventory rows into runtime C# state on enter-world, matching Java's ability to access `LEGION_WAREHOUSE` storage for a legion member.
2. Discover whether logout/periodic save includes location `3` rows consistently after legion warehouse rows are loaded into runtime state.
3. Revisit legion leave/kick/member-removal warehouse cleanup only after CM_LEGION or an equivalent membership-removal path becomes live.
