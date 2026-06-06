# Phase 6 Session 2702 Completion

## UOW

[Phase 6] UOW-2702: Send split restriction denials.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cross-storage restriction rejection now sends Java-equivalent regular/account warehouse denial messages before unlocking the source item.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo -> denial system message, then ItemSplitService.splitItem restriction branch -> ItemPacketService.sendStorageUpdatePacket(sourceStorage, sourceItem).
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync cross-storage restriction branch.
- Client-visible/state/persistence effect: rejected split attempts for non-storable regular/account warehouse destinations now send the denial packet, restore the source slot with storage update and SM_CUBE_UPDATE, avoid item mutation, and avoid persistence.
- Why this is runtime progress: it changes packets emitted by live CM_SPLIT_ITEM rejection handling; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Cross-storage restriction rejects before kinah and split/merge branches, then sends a source storage update.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Regular warehouse rejection sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`; account warehouse rejection sends `STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendStorageUpdatePacket` sends a storage-family add/update packet followed by `SM_CUBE_UPDATE.cubeSize(storageType, player)`.

## C# Changes

- Reused `CreateRestrictedToStorageMessage` in `HandleSplitItemAsync`.
- Sent the Java denial message before the existing source storage update on split cross-storage restriction rejection.
- Updated focused regular and account destination restriction regressions to assert denial message plus source unlock/size packet order.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava` | Unit / live connection handler | `ItemRestrictionService.isItemRestrictedTo`, `ItemSplitService.splitItem`, and `ItemPacketService.sendStorageUpdatePacket` source review | A non-storable cube item rejected from regular warehouse split sends `STR_WAREHOUSE_CANT_DEPOSIT_ITEM`, unlocks the source item, sends cube size, and does not mutate or persist. | Socket-backed connection fixture invoking the live private handler, repository mutation counters, in-memory item assertions, and packet byte decoding. | `isItemRestrictedFrom` and legion warehouse restrictions remain deferred. |
| `HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize` | Unit / live connection handler | Same Java branch plus account warehouse denial | A warehouse source rejected from account warehouse split sends `STR_MSG_WAREHOUSE_CANT_ACCOUNT_DEPOSIT`, unlocks the source warehouse item, sends regular-warehouse size, and does not mutate or persist. | Socket-backed connection fixture invoking the live private handler, repository mutation counters, in-memory item assertions, and packet byte decoding. | Account warehouse successful mutation branches are not fully covered here. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM cross-storage restriction rejection packet fanout.
- Specific behavior/contract: Java sends the storage denial message as a side effect of isItemRestrictedTo, then sends a source storage update and storage-size packet before returning.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CrossStorageRestrictionUnlocksSourceLikeJava|FullyQualifiedName~HandleSplitItemAsync_WarehouseSourceRestrictionUnlockUsesJavaStorageSize" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped regular/account rejection behavior.
- Why this scope is sufficient: the regressions exercise the branch that previously unlocked the source without Java's denial packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Regular/account cross-storage restriction now sends Java denial plus source unlock/storage-size before mutation/persistence. Full split flow remains partial. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateRestrictedToStorageMessage` | Service helper / live rejection packet | Partial | Unit Tested indirectly | Partial Parity | Regular and account destination denials are asserted through live split handling. Legion warehouse restrictions remain deferred. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` usage in `HandleSplitItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Source unlock packet and source storage size are asserted for cube and regular warehouse sources. Other storage families remain limited. |

## Known Gaps

- Complete `CM_SPLIT_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, destination full checks, and legion warehouse history remain incomplete.
- Account warehouse successful split/move/replace mutation branches are not fully covered by live tests.
- Legion warehouse storage-size/history variants remain deferred.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` shutdown-soon rejection against Java and add a focused regression/documentation if needed; current code path appears wired after UOW-2701, so proceed only if source review finds a remaining live mismatch.
2. Inspect `CM_MOVE_ITEM` account warehouse rejection with soulbound/non-storable items if a separate live mismatch is confirmed beyond the shared helper.
3. Inspect legion warehouse replace/split/move history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
