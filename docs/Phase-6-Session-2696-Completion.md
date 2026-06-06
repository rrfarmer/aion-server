# Phase 6 Session 2696 Completion

## UOW

[Phase 6] UOW-2696: Split empty-slot storage updates.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM split-to-empty-slot cross-storage moves now use Java-equivalent storage-specific SM_CUBE_UPDATE packets after source decrease and destination add.
- Java source/runtime path: ItemSplitService.splitItem targetItem == null branch -> sourceStorage.decreaseItemCount -> SM_CUBE_UPDATE.cubeSize(sourceStorage) -> destStorage.add(newItem) -> ItemPacketService.sendStorageUpdatePacket(destinationStorage).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync targetItem == null branch.
- Client-visible/state/persistence effect: a warehouse-source split into an empty cube slot now sends regular-warehouse size after the source decrease, then cube add plus cube size after the destination add, while preserving split state and persistence.
- Why this is runtime progress: it changes live CM_SPLIT_ITEM packets after a real split mutation and existing persistence call; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - In the target-null branch, Java decreases the source stack, sends `SM_CUBE_UPDATE.cubeSize(sourceStorage)`, then adds the new item to destination storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - Destination `sendStorageUpdatePacket` sends storage-specific add packet and `SM_CUBE_UPDATE.cubeSize(destinationStorage)`.

## C# Changes

- Replaced split-to-empty-slot source `SmCubeUpdate.CubeSize(player)` with source-storage `CreateStorageSizePacket`.
- Replaced hand-written destination add packet plus cube-size with the shared storage-aware add helper.
- Added a focused warehouse-source to cube empty-slot split regression.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize` | Unit / live connection handler | `ItemSplitService.splitItem` target-null branch and `ItemPacketService.sendStorageUpdatePacket` source review | A warehouse-source stack split into an empty cube slot mutates source/new item state and emits warehouse update, warehouse size, cube add, cube size in Java order. | Socket-backed connection fixture invoking the live private handler, runtime-loaded stackable item template, IDFactory-backed new item id, in-memory item assertions, repository mutation counters, and packet byte decoding. | Destination storage full handling, account/legion storage variants, and Java `ItemRestrictionService.isItemRestrictedFrom` remain incomplete. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM split-to-empty-slot cross-storage packet fanout plus focused handler regression.
- Specific behavior/contract: Java emits source-storage size after source decrease and destination-storage size after destination add.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageEmptySlotUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the cross-storage empty-slot split branch that previously sent cube storage size for a warehouse source and hand-wrote destination fanout.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Split-to-empty-slot cross-storage packet fanout now uses source/destination storage-specific size packets. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Destination add fanout is covered for cube destination from warehouse source. Other split branches still need review. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse and cube size packets are asserted for this split branch. Account/legion variants remain limited. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Merge-into-stack full-source delete branch still has hand-written source delete/cube-size fanout and should be inspected for non-cube source behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size variants are not fully covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_SPLIT_ITEM` merge-into-stack full-source delete packet sequence for source warehouse/cube combinations and implement storage-specific source delete fanout if current C# differs from Java.
2. Inspect `CM_SPLIT_ITEM` merge-into-stack partial source update for non-cube source storage and implement storage-specific source update/size behavior if needed.
3. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
