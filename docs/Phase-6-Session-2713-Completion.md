# Phase 6 Session 2713 Completion

## UOW

[Phase 6] UOW-2713: Create missing Kinah rows for account warehouse moves.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM Kinah moves between cube and account warehouse now create a missing destination Kinah runtime row instead of returning.
- Java source/runtime path: ItemSplitService.splitItem -> moveKinah -> updateKinahCount -> Storage.increaseKinah -> Storage.add(ItemFactory.newItem(KINAH, 0)) -> increaseItemCount -> InventoryDAO.store.
- C# runtime artifact wired: GameServerConnection.HandleKinahMoveAsync, Player.InventoryItems, Player.AccountWarehouseItems, MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync.
- Client-visible/state/persistence effect: moving Kinah to an empty cube/account warehouse mutates both live storage lists, sends source decrease plus destination add/update packets, and persists the new Kinah row through the existing inventory table.
- Why this is runtime progress: this changes live packet handling, runtime inventory/account-warehouse state, server packets, object-id allocation/release, and database persistence.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Kinah split uses `moveKinah`, supports cube to account warehouse and account warehouse to cube, and uses `updateKinahCount`.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `increaseKinah` creates a zero-count Kinah item when the destination storage has no Kinah row before increasing count.
- `game-server/src/com/aionemu/gameserver/model/items/storage/IStorage.java`
  - `getKinahItem` may return null when a storage never had Kinah.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - New destination item add sends storage add plus cube-size packet; count increase sends update packet.

## C# Changes

- `HandleKinahMoveAsync` now creates a destination Kinah `InventoryItem` when cube/account warehouse target storage has no Kinah row.
- New destination Kinah rows use player owner for cube and account owner for account warehouse.
- Save-failure and overflow-guard paths remove newly-created destination rows and release the allocated object id.
- `SaveItemMergeMutationAsync` now inserts merge targets marked `PersistentState.New`; existing merge targets still update count normally.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleSplitItemAsync_CubeKinahMoveCreatesMissingAccountWarehouseKinahLikeJava` | Unit / live connection handler | `ItemSplitService.moveKinah`, `Storage.increaseKinah`, `ItemPacketService` source review | Cube Kinah split to an empty account warehouse creates account-owned Kinah, persists target as `New`, and emits source decrease plus destination add/update packets. | Socket-backed handler fixture, runtime list/owner/count assertions, packet assertions, repository capture. | Exact Java runtime packet bytes were not captured; packet shape is source-reviewed. |
| `HandleSplitItemAsync_AccountWarehouseKinahMoveCreatesMissingCubeKinahLikeJava` | Unit / live connection handler | Same as above | Account warehouse Kinah split to an empty cube creates player-owned Kinah, persists target as `New`, and emits source decrease plus destination add/update packets. | Socket-backed handler fixture, runtime list/owner/count assertions, packet assertions, repository capture. | Exact Java runtime packet bytes were not captured. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM Kinah move path plus targeted merge persistence for newly-created destination rows.
- Specific behavior/contract: Java Storage.increaseKinah creates missing destination Kinah rows before applying INC_KINAH_MERGE; C# must not no-op when destination Kinah is absent.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleSplitItemAsync_CubeKinahMoveCreatesMissingAccountWarehouseKinahLikeJava|FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseKinahMoveCreatesMissingCubeKinahLikeJava|FullyQualifiedName~HandleSplitItemAsync_CubeSourceSplitsItemToAccountWarehouseOwnerLikeJava|FullyQualifiedName~HandleSplitItemAsync_AccountWarehouseSourceSplitsRestoredItemToCubeLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to live Kinah split/move behavior and the existing merge persistence operation.
- Broad .NET decision: skipped; the filtered command built the affected project and validated both new Kinah-creation directions plus adjacent split behavior.
- Why this scope is sufficient: tests exercise live packet dispatch, runtime state mutation, owner assignment, new-row persistence intent, and packet emission for the Java Kinah path.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemSplitService.moveKinah` | `GameServerConnection.HandleKinahMoveAsync` | Service / live inventory mutation | Partial | Unit Tested | Partial Parity | Cube/account warehouse Kinah moves now handle missing destination Kinah rows. Legion warehouse Kinah remains deferred. |
| `com.aionemu.gameserver.model.items.storage.Storage.increaseKinah` | `GameServerConnection.HandleKinahMoveAsync` / `AddMoveStorageItem` | Storage mutation / packet fanout | Partial | Unit Tested indirectly | Partial Parity | Missing destination Kinah row creation and add/update packet sequence are modeled for cube/account warehouse only. |
| `com.aionemu.gameserver.services.item.ItemPacketService` | `SendStorageUpdatePacketAsync` / `SmInventoryAddItem` / `SmWarehouseAddItem` / update packets | Packet service | Partial | Unit Tested indirectly | Partial Parity | Tests assert packet classes and decoded fields for this Kinah path; exact Java runtime bytes were not captured. |
| `com.aionemu.gameserver.dao.InventoryDAO.store` | `MySqlPlayerEnterWorldRepository.SaveItemMergeMutationAsync` | Repository / persistence | Partial | Unit Tested via repository capture | Partial Parity | Merge targets marked `New` are inserted, enabling missing destination Kinah persistence. Broader dirty-item store parity remains incomplete. |

## Known Gaps

- Legion warehouse Kinah remains deferred; `CM_LEGION_WH_KINAH` is parsed but not dispatched.
- Legion warehouse item move/split/replace paths still return before Java permission, history, and storage behavior.
- Exact Java wire bytes for missing-destination Kinah add/update packets were not captured in this UOW.
- Regular/account warehouse storage object parity remains partial; C# still uses list helpers rather than full Java `Storage` objects.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire the live `CM_LEGION_WH_KINAH` no-legion/no-right denial path so the parsed packet sends Java's authority-denial system message instead of no-oping.
2. Scope the first legion warehouse item move/split runtime slice only after legion storage owner/history prerequisites are identified.
3. Inspect regular warehouse replace/split restored-list behavior and proceed only if a code mismatch remains, not just missing tests.
