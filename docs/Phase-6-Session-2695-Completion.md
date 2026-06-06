# Phase 6 Session 2695 Completion

## UOW

[Phase 6] UOW-2695: Unlock split warehouse source.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction failures now use Java-equivalent source-storage update packets for non-cube source storage.
- Java source/runtime path: ItemSplitService.splitItem cross-storage restriction branch -> ItemPacketService.sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync.
- Client-visible/state/persistence effect: a warehouse-source split rejected by destination restrictions now sends SM_WAREHOUSE_ADD_ITEM followed by regular-warehouse SM_CUBE_UPDATE and leaves item state/persistence unchanged.
- Why this is runtime progress: it changes live packet emission from CM_SPLIT_ITEM on a restriction branch and prevents mutation; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Checks cross-storage restrictions before split-to-empty-slot and merge-into-stack branches.
  - On restriction failure, sends a source storage update packet and returns.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendStorageUpdatePacket` sends the source storage add/update packet and `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

## C# Changes

- Replaced the split restriction branch's hand-written source add plus cube-size packet with the storage-aware add helper.
- Added a focused warehouse-source/account-warehouse restriction regression.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize` | Unit / live connection handler | `ItemSplitService.splitItem` restriction branch and `ItemPacketService.sendStorageUpdatePacket` source review | A warehouse-source stack rejected from account warehouse stays unchanged, skips persistence, and sends warehouse add plus regular-warehouse size. | Socket-backed connection fixture invoking the live private handler, runtime-loaded restricted item template, in-memory state assertions, repository mutation counters, and packet byte decoding. | Full `ItemRestrictionService.isItemRestrictedFrom`, legion warehouse, and shutdown handling remain incomplete. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM cross-storage restriction packet fanout plus focused handler regression.
- Specific behavior/contract: Java restores the source storage view using source storage type when restrictions reject a cross-storage split.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize|FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the non-cube source restriction branch that previously sent cube storage size after a warehouse add packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage restriction unlock now uses source storage fanout for cube and regular warehouse source. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Restriction unlock source add plus storage-size packet is covered for warehouse source. Other split branches still have local packet fanout to inspect. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `ItemTemplateSummary.IsStorableInWarehouse` / `InventoryItem.IsStorableInAccountWarehouse` checks in `HandleSplitItemAsync` | Restriction logic | Partial | Unit Tested indirectly | Partial Parity | Destination restriction branches are modeled for warehouse/account warehouse. `isItemRestrictedFrom` remains unmodeled. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Split-to-empty-slot cross-storage packet fanout still has hand-written source/destination cube-size packets and should be inspected for non-cube source/destination behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse/legion warehouse storage-size variants are not fully covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_SPLIT_ITEM` split-to-empty-slot cross-storage packet fanout and implement source/destination storage-specific `SM_CUBE_UPDATE` packets if current C# differs from Java.
2. Inspect `CM_SPLIT_ITEM` merge-into-stack full-source delete packet sequence for source warehouse/cube combinations.
3. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
