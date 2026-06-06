# Phase 6 Session 2698 Completion

## UOW

[Phase 6] UOW-2698: Persist replace storage switches atomically.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM cross-storage item switches now persist both swapped item locations through one storage-switch mutation.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> swap equipmentSlot values -> remove both items -> delete packets -> add both items, with the changed inventory state persisted as a coherent storage update.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync cross-storage branch, PlayerEnterWorldService, and PlayerEnterWorldRepository.
- Client-visible/state/persistence effect: a cube/warehouse item switch now rolls back both in-memory item locations and sends no packets if the two-row persistence fails; on success it keeps the existing Java delete/add packet order.
- Why this is runtime progress: it changes live CM_REPLACE_ITEM persistence and rollback behavior after a real inventory state mutation; it is not preview-only/test-only/documentation-only.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
  - Dispatches directly to `ItemMoveService.switchItemsInStorages`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage switches swap both item slots, remove both source rows from their old storages, send delete packets in old-storage order, then add both items to their new storages.

## C# Changes

- Added `SaveItemStorageSwitchMutationAsync` to `IPlayerEnterWorldRepository`, `EmptyPlayerEnterWorldRepository`, `MySqlPlayerEnterWorldRepository`, and `PlayerEnterWorldService`.
- Extracted the existing item-location SQL update into a shared private helper, then used a transaction for the two-row storage switch.
- Rewired `GameServerConnection.HandleReplaceItemAsync` cross-storage branch to call the single storage-switch mutation instead of two independent one-row move saves.
- Added a focused failure regression proving both in-memory items are rolled back and no UI packets are sent when persistence fails.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava` | Unit / live connection handler | `ItemMoveService.switchItemsInStorages` source review | A cube/warehouse switch mutates both items, calls one storage-switch persistence mutation, and preserves Java delete/delete/add/add packet order. | Socket-backed connection fixture invoking the live private handler, repository mutation counters, in-memory item assertions, and packet byte decoding. | Legion warehouse, shutdown-soon, and full Java restriction checks remain incomplete. |
| `HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems` | Unit / live connection handler | Java storage switch is a coherent state mutation before packet fanout | Failed two-row persistence rolls back both C# item locations/slots and suppresses client packet emission. | Socket-backed connection fixture with repository failure injection and no-packet assertion. | Does not exercise a real database transaction; MySQL transaction semantics are covered by implementation review only. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM cross-storage persistence/rollback plus repository/service persistence API.
- Specific behavior/contract: storage switches persist both item rows as one mutation before Java-order packet fanout; failed persistence rolls back both item states and sends no packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleReplaceItemAsync_CrossStorageSwitchDeletesThenAddsLikeJava|FullyQualifiedName~HandleReplaceItemAsync_StorageSwitchSaveFailureRollsBackBothItems" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live handler branch and one repository persistence method; the filtered command compiled the affected projects.
- Broad .NET decision: skipped; the filtered command built the affected projects and proved the scoped state, persistence-call, rollback, and packet fanout behavior.
- Why this scope is sufficient: the regression exercises the only live handler branch rewired to the two-row persistence mutation.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REPLACE_ITEM` | `Aion.GameServer.Network.Aion.ClientPackets.CmReplaceItem` / `GameServerConnection.HandleReplaceItemAsync` | Client packet / live handler | Partial | Unit Tested | Partial Parity | Cross-storage switch persistence and packet order are covered. Parser parity existed previously; full handler restrictions are not complete. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cross-storage cube/regular-warehouse switches now persist both swapped rows through one mutation and roll back both on save failure. Legion warehouse, shutdown-soon, and full restriction checks remain gaps. |
| `com.aionemu.gameserver.dao.InventoryDAO.store` | `MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync` | Repository / persistence | Partial | Unit Tested indirectly | Partial Parity | Two item-location updates are wrapped in a MySQL transaction. No live MySQL integration test was run for this UOW. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket` / `sendStorageUpdatePacket` | `SendItemDeletePacketAsync` / `SendStorageUpdatePacketAsync` usage in `HandleReplaceItemAsync` | Packet service / live server packet | Partial | Unit Tested indirectly | Partial Parity | Existing Java delete/delete/add/add order remains covered for cube and regular warehouse. Other storage families remain limited. |

## Known Gaps

- Complete `CM_REPLACE_ITEM` parity is not claimed.
- Java `ItemRestrictionService.isItemRestrictedFrom`, shutdown-soon behavior, and legion warehouse history/permission integration remain incomplete.
- The new MySQL two-row storage-switch mutation was compiled and reviewed, but not exercised against a live database fixture.
- Account and legion warehouse storage-size variants are not fully covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Inspect `CM_REPLACE_ITEM` restriction/shutdown branch against Java `ItemRestrictionService.isItemRestrictedFrom`, `isItemRestrictedTo`, and `GameServer.isShuttingDownSoon`; implement only a confirmed live mismatch such as missing source restriction unlock/system-message behavior.
2. Inspect account warehouse storage-size semantics in move/split/replace branches only if Java source review confirms a live C# packet mismatch.
3. Inspect legion warehouse replace/split history only if Java source review can isolate a narrow live storage branch without broad legion-service scaffolding.
