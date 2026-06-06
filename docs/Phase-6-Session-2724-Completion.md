# Phase 6 Session 2724 Completion

## UOW

[Phase 6] UOW-2724: Persist live legion warehouse item history.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful live item movement and split paths involving legion warehouse now persist ITEM_DEPOSIT/ITEM_WITHDRAW history rows with Java's itemId:count description.
- Java source/runtime path: ItemMoveService.moveItem -> LegionService.addWHItemHistory; ItemSplitService.splitItem -> LegionService.addWHItemHistory; LegionService.addWHItemHistory -> addHistory -> LegionDAO.insertHistory.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GameServerConnection.HandleSplitItemAsync, existing AddLegionHistoryAsync/InsertLegionHistoryAsync, LegionHistoryActions.
- Client-visible/state/persistence effect: live item deposit/withdrawal history becomes durable in legion_history and is available to the already-live CM_LEGION_HISTORY warehouse packet path.
- Why this is runtime progress: it persists runtime legion warehouse state from existing live packet handlers after real inventory/warehouse mutations succeed.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage move involving `StorageType.LEGION_WAREHOUSE` calls `LegionService.addWHItemHistory(player, item.getItemId(), item.getItemCount(), sourceStorage, targetStorage)`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Cross-storage split-to-empty and split-merge branches call `addWHItemHistory` with the split amount.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `addWHItemHistory` writes `ITEM_WITHDRAW` when source storage is legion warehouse, or `ITEM_DEPOSIT` when destination storage is legion warehouse, with description `itemId:count`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `switchItemsInStorages` does not call `addWHItemHistory`; C# replace/switch history remains intentionally unwired.

## C# Changes

- Added `LegionHistoryActions.ItemDeposit` and `LegionHistoryActions.ItemWithdraw` constants and reused them in action metadata.
- Added `GameServerConnection.AddLegionWarehouseItemHistoryAsync` to map source/destination storage `3` to Java `ITEM_WITHDRAW`/`ITEM_DEPOSIT`.
- `HandleMoveItemAsync` now records one history row after a successful persisted move or first successful auto-merge involving legion warehouse.
- `HandleSplitItemAsync` now records history after successful persisted split-to-empty or split-merge mutations involving legion warehouse.
- Kept replace/switch item handling history-free because Java `ItemMoveService.switchItemsInStorages` has no `addWHItemHistory` call.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `ItemMoveService.moveItem`, `LegionService.addWHItemHistory` | Successful cube-to-legion move persists `ITEM_DEPOSIT` with `itemId:count`. | Socket-backed connection fixture, live `ProcessPacketAsync`, fake repository history capture. | Direct DB insert not integration-tested. |
| `ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava` | Unit / live packet dispatch | `ItemMoveService.moveItem`, `LegionService.addWHItemHistory` | Successful legion-to-cube move persists `ITEM_WITHDRAW` with `itemId:count`. | Socket-backed connection fixture, live `ProcessPacketAsync`, fake repository history capture. | Direct DB insert not integration-tested. |
| `ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `ItemSplitService.splitItem`, `LegionService.addWHItemHistory` | Successful cube-to-legion split persists `ITEM_DEPOSIT` with split amount. | Socket-backed connection fixture, live split mutation and repository capture. | Direct DB insert not integration-tested. |
| `ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava` | Unit / live packet dispatch | `ItemSplitService.splitItem`, `LegionService.addWHItemHistory` | Successful legion-to-cube split persists `ITEM_WITHDRAW` with split amount. | Socket-backed connection fixture, live split mutation and repository capture. | Direct DB insert not integration-tested. |
| `ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava` | Unit / live packet dispatch | `ItemMoveService.switchItemsInStorages` | Replace/switch involving legion warehouse still mutates owners/locations but writes no history, matching Java. | Socket-backed connection fixture and repository call count. | No Java byte capture. |
| `ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava` | Unit / live packet dispatch | `CM_LEGION_HISTORY`, `SM_LEGION_HISTORY` | Existing warehouse history read path still sends represented rows. | Socket-backed connection fixture, decoded packet payload. | Direct DB load not integration-tested. |

## Validation Decision

```text
- Changed surface: live inventory packet handlers and legion history action model constants.
- Specific behavior/contract: successful live item move/split into/from legion warehouse writes ITEM_DEPOSIT/ITEM_WITHDRAW rows with itemId:count, and replace/switch remains no-history per Java.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 6 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: live handler/persistence wiring. Broad .NET was skipped after focused validation compiled the affected project and directly exercised the edited move/split branches, replace boundary, and warehouse history packet adjacency.
- Why this scope is sufficient: the focused command proves durable history insert calls from live packet dispatch only after successful runtime item mutations in the scoped handlers.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `LegionService.addWHItemHistory` | `GameServerConnection.AddLegionWarehouseItemHistoryAsync` | Service side effect | Partial | Unit Tested indirectly | Partial Parity | Action selection and `itemId:count` description match Java; C# inserts after successful persistence to avoid rollback history rows. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Legion warehouse item move deposit/withdraw history is wired; direct DB integration and shared legion aggregate remain gaps. |
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Legion warehouse split deposit/withdraw history is wired for empty-slot and merge branches; direct DB integration remains a gap. |
| `ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Confirmed no item history write because Java switch path has no `addWHItemHistory` call. |
| `LegionHistoryAction` | `LegionHistoryActions` | Enum/model projection | Partial | Unit Tested indirectly | Partial Parity | Item and kinah warehouse actions used by live paths are represented. |
| `CM_LEGION_HISTORY.runImpl` | `GameServerConnection.HandleLegionHistoryAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Existing warehouse history send remains covered; new item rows flow through the same durable row shape. |

## Known Gaps

- Direct MySQL integration for item history rows was not run.
- C# still does not model Java's shared in-memory `Legion` history cache or age-based trimming.
- Move-history insert ordering intentionally differs from Java's pre-mutation call in C# rollback-sensitive paths; action names and descriptions match Java for successful mutations.
- Java exact packet bytes for item-history readback were not captured in this UOW.
- Legion warehouse expansion/runtime size loading remains partial.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 5
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 6
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Load and persist legion warehouse expansion count into the runtime model used by `SM_CUBE_UPDATE.LegionWarehouseSizeSnapshot`, matching Java `Legion.getWarehouseExpansions()`.
2. Wire live legion warehouse open/close in-use state if a concrete Java runtime path can be paired with a C# state mutation, not only readiness metadata.
3. Extend `CM_LEGION_HISTORY` to brigade-general reward or legion activity sends only if paired with a live row source and packet workflow change.
