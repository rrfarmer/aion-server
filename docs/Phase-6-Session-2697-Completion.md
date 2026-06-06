# Phase 6 Session 2697 Completion

## UOW

[Phase 6] UOW-2697: Delete split warehouse source.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM merge-into-stack full-source deletes now use Java-equivalent source-storage delete fanout.
- Java source/runtime path: ItemSplitService.splitItem targetItem same item -> mergeStacks -> Storage.decreaseItemCount -> Storage.delete -> ItemPacketService.sendItemDeletePacket.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem same-item sourceItem.Count <= 0 branch.
- Client-visible/state/persistence effect: a warehouse-source full merge now sends SM_DELETE_WAREHOUSE_ITEM plus regular-warehouse SM_CUBE_UPDATE while preserving target-stack update, in-memory source removal, and merge persistence.
- Why this is runtime progress: it changes live CM_SPLIT_ITEM packets after a real merge mutation; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Same-item target merges call `mergeStacks`, which decreases the source stack.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `decreaseItemCount` deletes zero-count non-kinah source items and routes through storage-aware item delete packet behavior.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemDeletePacket` emits storage-specific delete packets and `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

## C# Changes

- Replaced the same-item full-source merge branch's hand-written delete packet and cube-size packet with `SendItemDeletePacketAsync`.
- Preserved the existing merge mutation, player item list removal, and delete-type selection.
- Added a focused warehouse-source full merge regression that decodes the emitted warehouse delete and source storage-size packets.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_FullWarehouseSourceMergeUsesJavaStorageSize` | Unit / live connection handler | `ItemSplitService.mergeStacks`, `Storage.decreaseItemCount`, and `ItemPacketService.sendItemDeletePacket` source review | A warehouse-source stack fully merged into a cube target mutates target/source state, persists the merge, sends target stack increase, sends `SM_DELETE_WAREHOUSE_ITEM`, and sends regular-warehouse `SM_CUBE_UPDATE`. | Socket-backed connection fixture invoking the live private handler, runtime-loaded stackable item template, in-memory item assertions, repository mutation counters, and packet byte decoding. | Complete split parity, account/legion storage variants, and Java destination/restriction branches remain incomplete. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM same-item merge full-source delete fanout plus focused handler regression.
- Specific behavior/contract: Java deletes a fully consumed source stack through source-storage-aware delete packets and source-storage SM_CUBE_UPDATE.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_FullWarehouseSourceMergeUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_FullSourceMergeDeletesSourceStackLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the branch that previously always sent default cube-size after deleting a non-cube source item.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Same-item full-source merge delete fanout now uses source-storage-aware delete/size packets. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemSplitService.mergeStacks` / `com.aionemu.gameserver.model.items.storage.Storage.decreaseItemCount` | `GameServerConnection.HandleSplitItemAsync` source decrease/delete branch | Service / storage mutation | Partial | Unit Tested indirectly | Partial Parity | Zero-count source deletion is covered for regular warehouse source into cube target. Other storage families and partial-source update branches still need review. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` | `SendItemDeletePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Warehouse delete plus warehouse size packet are asserted for this merge branch. Account/legion variants remain limited. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_WAREHOUSE_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmDeleteWarehouseItem` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Packet is decoded in the focused live handler regression for regular warehouse delete. Broader packet parity is not claimed. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse size packet is asserted for this delete branch. Account/legion variants remain limited. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Merge-into-stack partial source update for non-cube source storage still needs source review.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size variants are not fully covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_SPLIT_ITEM` merge-into-stack partial source update for non-cube source storage and implement storage-specific source update/size behavior if source review confirms a live packet mismatch.
2. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
3. Inspect account warehouse storage-size semantics in move/split branches only if Java source review confirms a live C# packet mismatch.
