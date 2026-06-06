# Phase 6 Session 2694 Completion

## UOW

[Phase 6] UOW-2694: Delete merged warehouse source.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM stack auto-merge now uses Java-equivalent source-storage delete fanout when a warehouse source stack is fully consumed.
- Java source/runtime path: ItemMoveService.moveItem slot == -1 stack merge loop -> ItemSplitService.mergeStacks -> Storage.decreaseItemCount -> ItemPacketService.sendItemDeletePacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync auto-merge item.Count <= 0 branch.
- Client-visible/state/persistence effect: a warehouse-source full auto-merge now sends SM_DELETE_WAREHOUSE_ITEM followed by regular-warehouse SM_CUBE_UPDATE after the merge mutation instead of a cube-size packet.
- Why this is runtime progress: it changes packets emitted by live CM_MOVE_ITEM after a real stack merge state/persistence mutation; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Auto-placement stack moves call `ItemSplitService.mergeStacks`; when the source count reaches zero, the method returns after the merge.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `mergeStacks` decreases source count with `DEC_ITEM_SPLIT_MOVE` when source and destination storages differ.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseItemCount` deletes zero-count non-kinah items through `delete`, which calls `ItemPacketService.sendItemDeletePacket` using the source storage.

## C# Changes

- Replaced the auto-merge full-source branch's hand-written delete/cube-update fanout with the storage-aware delete helper.
- Added a focused warehouse-source full auto-merge regression.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_FullWarehouseSourceAutoMergeUsesJavaStorageSize` | Unit / live connection handler | `ItemMoveService.moveItem`, `ItemSplitService.mergeStacks`, `Storage.decreaseItemCount`, and `ItemPacketService.sendItemDeletePacket` source review | A warehouse-source stack fully merging into a cube target deletes the warehouse source and sends regular-warehouse `SM_CUBE_UPDATE`. | Socket-backed connection fixture invoking the live private handler, runtime-loaded stackable item template, repository merge capture, in-memory item assertions, and packet byte decoding. | Partial auto-merge source update branch and account/legion warehouse variants remain unverified. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM auto-merge full-source delete branch plus focused handler regression.
- Specific behavior/contract: Java deletes a fully consumed source stack through Storage.delete/ItemPacketService using the source storage type.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_FullWarehouseSourceAutoMergeUsesJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_StackableAutoSlotMergesDestinationStackLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the warehouse-source branch that previously sent cube storage size after a warehouse delete packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Full-source auto-merge now uses source-storage delete fanout for warehouse sources. Other move restrictions and storage families remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` | `GameServerConnection.HandleMoveItemAsync` auto-merge branch | Service / stack merge | Partial | Unit Tested indirectly | Partial Parity | Covered for a warehouse-source stack fully merging into cube target. Partial merge and split handler variants remain separately tracked. |
| `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` / `ItemPacketService.sendItemDeletePacket` | `SendItemDeletePacketAsync` usage in `HandleMoveItemAsync` | Storage / packet service | Partial | Unit Tested indirectly | Partial Parity | Zero-count source delete uses source storage for this branch. Full storage persistence lifecycle is not claimed. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Partial auto-merge source update branch still needs non-cube source review.
- Java `ItemRestrictionService.isItemRestrictedFrom`, trading unlock edge cases, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse merge/delete variants are not covered by this UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` partial auto-merge source update for non-cube source storage and implement storage-specific update/size packets if current C# differs from Java.
2. Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
3. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
