# Phase 6 Session 2693 Completion

## UOW

[Phase 6] UOW-2693: Send move storage-size updates.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM normal cross-storage moves now use Java-equivalent storage-specific SM_CUBE_UPDATE packets after delete/add packets.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemPacketService.sendItemDeletePacket/sendStorageUpdatePacket -> SM_CUBE_UPDATE.cubeSize(storageType, player).
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync.
- Client-visible/state/persistence effect: regular warehouse destination moves now send a regular-warehouse SM_CUBE_UPDATE with warehouse count/ordinal after SM_WAREHOUSE_ADD_ITEM while preserving the existing item row location/slot persistence.
- Why this is runtime progress: it changes packets emitted by the live CM_MOVE_ITEM handler after real inventory state and persistence mutation; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Normal cross-storage move removes the item from the source storage, sends source delete, sets the target slot, and adds the item to the target storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemDeletePacket` and `sendStorageUpdatePacket` send `SM_CUBE_UPDATE.cubeSize(storageType, player)` for the storage touched by the packet.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
  - `cubeSize(REGULAR_WAREHOUSE, player)` writes regular warehouse ordinal/count/expansion fields, not cube count.

## C# Changes

- Routed the normal `HandleMoveItemAsync` cross-storage delete packet through the storage-aware delete helper.
- Routed the normal `HandleMoveItemAsync` cross-storage destination add packet through the storage-aware add helper.
- Added a focused live handler regression for cube-to-regular-warehouse move packet fanout.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize` | Unit / live connection handler | `ItemMoveService.moveItem`, `ItemPacketService`, and `SM_CUBE_UPDATE.cubeSize` source review | A normal cube-to-regular-warehouse move persists the item row, emits cube delete/cube-size, then emits warehouse add/regular-warehouse-size. | Socket-backed connection fixture invoking the live private handler, runtime-loaded storable item template, repository mutation capture, item state assertions, and packet byte decoding. | Auto-merge branches still contain hand-written delete/update fanout and need separate review for warehouse source cases. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM normal cross-storage packet fanout plus focused handler regression.
- Specific behavior/contract: Java sends storage-specific SM_CUBE_UPDATE after ItemPacketService delete/add packets, including regular warehouse ordinal/count after SM_WAREHOUSE_ADD_ITEM.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleMoveItemAsync_CrossStorageWarehouseCubeUpdatesUseJavaStorageSize|FullyQualifiedName~HandleMoveItemAsync_PartialAutoSlotMergeMovesRemainingStackLikeJava" --logger "console;verbosity=minimal"
- First result: failed because the new fixture item id 201 was not warehouse-storable in the loaded static data, so the handler correctly returned before mutation.
- Focused C# command after fixture correction: same command with storable item id 200.
- Final result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and reuses existing packet primitives/helpers.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the exact normal cross-storage path that previously sent cube-size after a warehouse add packet.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Normal cross-storage move now uses storage-aware delete/add packet fanout. Stack merge branches remain partially reviewed. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SmDeleteItem`, `SmDeleteWarehouseItem`, `SmInventoryAddItem`, `SmWarehouseAddItem`, `SmCubeUpdate` usage in `HandleMoveItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Normal move delete/add storage-size updates are covered. Other item handlers still have local packet fanout to inspect. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate` | Server packet | Partial | Unit Tested indirectly | Partial Parity | Regular warehouse ordinal/count is asserted for this move path. Account/legion storage behavior remains limited to existing modeled helpers. |

## Known Gaps

- Complete `CM_MOVE_ITEM` parity is not claimed.
- Auto-merge full-source delete branch still has hand-written source delete/cube-update fanout and should be inspected for warehouse source behavior.
- Java `ItemRestrictionService.isItemRestrictedFrom`, trading unlock edge cases, shutdown-soon behavior, and legion warehouse history remain incomplete.
- Account warehouse and legion warehouse storage-size behavior are not covered by this UOW.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` stack auto-merge full-source delete for warehouse source and implement storage-specific source-size packets if current C# differs from Java.
2. Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
3. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
