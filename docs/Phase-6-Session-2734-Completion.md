# Phase 6 Session 2734 Completion

## UOW

[Phase 6] UOW-2734: Honor disabled legion warehouse config in live item movement.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: disabling `gameserver.legion.warehouse` now blocks live item movement to/from legion warehouse storage type `3`, not only the open-dialog path.
- Java source/runtime path: ItemRestrictionService.isItemRestrictedTo, ItemRestrictionService.isItemRestrictedFrom, LegionConfig.LEGION_WAREHOUSE, ItemMoveService.moveItem, ItemSplitService.splitItem, and CM_REPLACE_ITEM's shared restriction path.
- C# runtime artifact wired: GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage now consumes GameServerOptions.Legion.WarehouseEnabled and is used by live CM_MOVE_ITEM, CM_SPLIT_ITEM, and CM_REPLACE_ITEM handling.
- Client-visible/state/persistence effect: live move/split into disabled legion warehouse sends system-message id `1400355`; live move from disabled legion warehouse sends system-message id `1300322`; blocked operations unlock the source storage view and leave item state, persistence calls, and legion history unchanged.
- Why this is runtime progress: it sends real server packets from live inventory packet handlers and prevents incorrect live item mutation when Java's legion warehouse config disables the feature.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - `isItemRestrictedTo(LEGION_WAREHOUSE)` sends `STR_MSG_WAREHOUSE_CANT_LEGION_DEPOSIT` when the item is not legion-storable or `LegionConfig.LEGION_WAREHOUSE` is false.
  - `isItemRestrictedFrom(LEGION_WAREHOUSE)` sends `STR_GUILD_WAREHOUSE_NO_RIGHT` when the config is false, the player is not a legion member, or the player lacks withdrawal rights.
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
  - `LEGION_WAREHOUSE` backs `gameserver.legion.warehouse`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - `moveItem` runs restriction checks before live move/merge mutation.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - `splitItem` runs restriction checks before creating the split item.

## C# Changes

- Converted `GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage` from static to instance-scoped so it can read `_options.Legion.WarehouseEnabled`.
- Matched Java `isItemRestrictedTo` for destination storage type `3`: disabled legion warehouse returns `SmSystemMessage.WarehouseCantLegionDeposit()` (`1400355`).
- Matched Java `isItemRestrictedFrom` for source storage type `3`: disabled legion warehouse returns `SmSystemMessage.GuildWarehouseNoRight()` (`1300322`) before mutation even when the player otherwise has withdrawal rights.
- Added live packet-dispatch tests for disabled-config move and split paths.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceMoveToDisabledLegionWarehouseSendsJavaDepositRestriction` | Unit / live client packet dispatch | `ItemRestrictionService.isItemRestrictedTo` + `ItemMoveService.moveItem` | `CM_MOVE_ITEM` from cube to disabled legion warehouse sends `1400355`, unlocks the cube item, and avoids item/persistence/history mutation. | Dispatches opcode `156` through `ProcessPacketAsync` and inspects packets, runtime item state, and repository calls. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |
| `ProcessPacketAsync_LegionWarehouseSourceMoveWhenDisabledSendsNoRightLikeJava` | Unit / live client packet dispatch | `ItemRestrictionService.isItemRestrictedFrom` + `ItemMoveService.moveItem` | `CM_MOVE_ITEM` from disabled legion warehouse sends `1300322` and avoids item/persistence/history mutation. | Dispatches opcode `156` through `ProcessPacketAsync` with withdrawal rights present, proving config disables the live path. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |
| `ProcessPacketAsync_CubeSourceSplitToDisabledLegionWarehouseSendsJavaDepositRestriction` | Unit / live client packet dispatch | `ItemRestrictionService.isItemRestrictedTo` + `ItemSplitService.splitItem` | `CM_SPLIT_ITEM` from cube to disabled legion warehouse sends `1400355`, unlocks the cube item, and avoids split/persistence/history mutation. | Dispatches opcode `157` through `ProcessPacketAsync` and verifies no new split object is created. | Uses C# flattened location-3 model, not a full Java `LegionWarehouse`. |

## Validation Decision

```text
- Changed surface: live inventory restriction helper shared by CM_MOVE_ITEM, CM_SPLIT_ITEM, and CM_REPLACE_ITEM.
- Specific behavior/contract: disabled legion warehouse config blocks storage type `3` live movement using Java message ids 1400355 and 1300322 before mutation.
- Focused C# command 1: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToDisabledLegionWarehouseSendsJavaDepositRestriction|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMoveWhenDisabledSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused C# command 2: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitToFullLegionWarehouseSendsJavaFullMessageWithoutMutation|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMoveToFullLegionWarehouseSendsJavaFullMessageAndUnlocksSource|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~GamePacketTests" --logger "console;verbosity=minimal"
- Result: passed; 295 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this config branch in the checkout.
- Broad-validation trigger: live inventory mutation path. Broad .NET was skipped because focused live packet dispatch compiled the affected project and proved both new config branches plus adjacent existing legion warehouse movement behavior.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched C# files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedTo` | `GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage` | Live movement restriction helper | Partial | Unit Tested through live handlers | Partial Parity | Destination storage type `3` now includes Java disabled-config handling and sends `1400355`; other Java storage types remain outside this helper. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService.isItemRestrictedFrom` | `GameServerConnection.CreateLegionWarehouseMoveRestrictionMessage` | Live movement restriction helper | Partial | Unit Tested through live handlers | Partial Parity | Source storage type `3` now includes Java disabled-config handling and sends `1300322`. |
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Move to/from disabled legion warehouse is blocked before mutation. Full Java item movement parity remains broader than this UOW. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler branch | Partial | Unit Tested | Partial Parity | Split into disabled legion warehouse is blocked before mutation. Full Java split parity remains broader than this UOW. |

## Known Gaps

- C# still models legion warehouse contents as player `InventoryItems` location `3`, not a full Java `LegionWarehouse`.
- `CM_REPLACE_ITEM` uses the same changed restriction helper, but this UOW's new disabled-config assertions cover move/split directly; existing replace no-right coverage remains adjacent evidence.
- Exact Java golden bytes for `1400355` and `1300322` were not captured in this UOW.
- Full edited-class validation still has unrelated known failures from the previous session; use focused recipes until those areas are addressed.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Discover and wire the next live Java `ItemRestrictionService` storage branch that C# still omits, only if it affects an existing live C# packet path.
2. Persist or restore Java-equivalent legion warehouse item state from live save/logout/load paths if discovery finds a concrete runtime gap in the current C# owner/location persistence.
3. Revisit legion leave/kick/member-removal warehouse cleanup only after a live CM_LEGION membership-removal path exists.
