# Phase 6 Session 2692 Completion

## UOW

[Phase 6] UOW-2692: Switch cross-storage replace items.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM now executes Java-style cross-storage item switches instead of returning early.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync.
- Client-visible/state/persistence effect: two item rows swap storage/slot through existing inventory persistence, then the client receives source delete, replacement delete, replacement add, and source add packets in Java order.
- Why this is runtime progress: it mutates live inventory item storage/slot state, persists both affected rows, and sends real server packets from the live CM_REPLACE_ITEM handler; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
  - Reads source storage/object id and replacement storage/object id, then delegates to `ItemMoveService.switchItemsInStorages`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `switchItemsInStorages` checks restrictions/trading/shutdown, swaps slots, removes both items, sends both delete packets, then adds replacement to source storage and source to replacement storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemDeletePacket` and `sendStorageUpdatePacket` send a storage-specific `SM_CUBE_UPDATE` after delete/add packets.

## C# Changes

- Removed the deferred cross-storage early return from `HandleReplaceItemAsync`.
- Added destination storability checks for both swapped items; restriction/trading failures now unlock both items like Java.
- Swapped source/replacement `Location` and `Slot`, persisted both updates through the existing cross-storage move mutation method, and rolled in-memory state back on persistence failure.
- Sent Java-ordered delete/delete/add/add packets for cross-storage replace, including storage-specific cube/warehouse size updates for the new replace path.
- Extended the empty test repository to record all cross-storage move persistence calls in order.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava` | Unit / live connection handler | `ItemMoveService.switchItemsInStorages` and `ItemPacketService` source review | Cross-storage replace swaps runtime item location/slot, requests two persistence updates, and sends source delete, replacement delete, replacement cube add, source warehouse add in Java order. | Socket-backed connection fixture invoking the live private handler, runtime-loaded item templates, repository mutation capture, in-memory item assertions, and packet byte decoding. | Legion warehouse, Java `isItemRestrictedFrom`, shutdown-system-message handling, and atomic two-row DB persistence remain incomplete. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM cross-storage handler plus focused test repository capture and packet assertions.
- Specific behavior/contract: Java switches cross-storage replace items by mutating both slots/storage locations and sending delete/delete/add/add in ItemMoveService.switchItemsInStorages order.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~CmReplaceItemTests" --logger "console;verbosity=minimal"
- First result: failed at compile because cross-storage locals reused same names as same-storage locals in the method scope.
- Focused C# command after correction: same command.
- Final result: passed; 1 test matched and passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The executable change is isolated to one live connection handler branch and uses existing packet/repository primitives.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, parser, and packet sequence behavior.
- Why this scope is sufficient: the regression exercises the branch that previously returned before any live cross-storage replace mutation or packet emission.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REPLACE_ITEM` | `Aion.GameServer.Network.Aion.ClientPackets.CmReplaceItem` / `GameServerConnection.HandleReplaceItemAsync` | Client packet / live handler | Partial | Unit Tested indirectly | Partial Parity | Packet read shape is represented and cross-storage live handler path now executes. No standalone parser test class exists. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage regular warehouse/cube switch now mutates state, persists both rows, and sends Java-ordered packets. Same-storage path existed. Legion warehouse, shutdown, and full restriction service behavior remain incomplete. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SmDeleteItem`, `SmDeleteWarehouseItem`, `SmInventoryAddItem`, `SmWarehouseAddItem`, `SmCubeUpdate` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Replace-item cross-storage delete/delete/add/add order is covered. Broader move/split packet-size helper parity remains under review. |

## Known Gaps

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- C# still does not model Java `ItemRestrictionService.isItemRestrictedFrom`.
- Shutdown-soon behavior and shutdown system message are not wired.
- Legion warehouse replace remains deferred.
- Two-row replace persistence reuses the existing single-row move update twice; it is not yet atomic at the database layer.
- Existing `CM_MOVE_ITEM` and `CM_SPLIT_ITEM` branches still have hand-written storage update packets that should be inspected against Java storage-specific cube-size behavior.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_MOVE_ITEM` cross-storage delete/add storage-size packets and implement the smallest confirmed Java mismatch in live `HandleMoveItemAsync`.
2. Inspect `CM_REPLACE_ITEM` persistence atomicity only if adding a two-row repository mutation can be kept tightly scoped to live replace behavior.
3. Inspect non-cube source cross-storage `CM_SPLIT_ITEM` restriction unlock packet behavior if a safe warehouse-source fixture can represent the live path.
