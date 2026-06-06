# Phase 6 Session 2717 Completion

## UOW

[Phase 6] UOW-2717: Send legion warehouse replace withdrawal denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_REPLACE_ITEM involving LEGION_WAREHOUSE now reaches Java's restriction denial branch instead of silently returning before client unlock packets.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> ItemRestrictionService.isItemRestrictedFrom / isItemRestrictedTo -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> ItemPacketService.sendItemUnlockPacket(sourceItem) and sendItemUnlockPacket(replaceItem).
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync, CreateLegionWarehouseMoveRestrictionMessage, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message plus source and replacement item unlock packets; no item location, slot, count, or persistence state mutates.
- Why this is runtime progress: this sends Java-equivalent server packets from a live client packet handler and advances a previously deferred packet path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
  - Reads source storage/object id and replacement storage/object id; delegates to `ItemMoveService.switchItemsInStorages`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Replace flow resolves both storages and items, checks source/replacement `isItemRestrictedFrom` then cross-storage `isItemRestrictedTo`, and on restriction unlocks both items before returning.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Source `LEGION_WAREHOUSE` without `WH_WITHDRAWAL` sends `STR_GUILD_WAREHOUSE_NO_RIGHT`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - `sendItemUnlockPacket` maps item location to a storage update with `ALL_SLOT`, followed by cube-size update.

## C# Changes

- `HandleReplaceItemAsync` now evaluates Java-shaped legion warehouse restriction order before the existing successful-legion-replace deferred return.
- Denied legion warehouse replacement sends the restriction system message and unlocks both participating items with `ALL_SLOT`.
- The branch leaves both items unchanged; successful legion warehouse replacement remains deferred until live legion storage, persistence, and history support exist.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava` | Unit / live packet dispatch | `CM_REPLACE_ITEM`, `ItemMoveService.switchItemsInStorages`, `ItemRestrictionService`, `ItemPacketService` source review | Live replace dispatch sends `STR_GUILD_WAREHOUSE_NO_RIGHT`, unlocks source legion warehouse item and replacement cube item with `ALL_SLOT`, and leaves both item states unchanged. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions. | Exact Java runtime bytes were not captured; successful legion warehouse replacement remains deferred. |

## Validation Decision

```text
- Changed surface: live CM_REPLACE_ITEM legion warehouse restriction branch plus focused test coverage.
- Specific behavior/contract: Java denial packet plus source and replacement item unlock packets for source LEGION_WAREHOUSE when WH_WITHDRAWAL is missing.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live replace denial branch and reuses the legion restriction helper validated by UOW-2715/UOW-2716.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited live replace path plus adjacent split behavior.
- Why this scope is sufficient: tests exercise live packet dispatch, Java-derived denial ordering, both unlock packets, and no state mutation for the scoped branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_REPLACE_ITEM` | `GameServerConnection.HandleReplaceItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse missing-withdrawal denial and both-item unlock are wired. Successful legion replacements remain deferred. |
| `com.aionemu.gameserver.services.item.ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Service / live item replace | Partial | Unit Tested | Partial Parity | Cross-storage legion restriction branch is represented; history and mutation are not. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `SendStorageUpdatePacketAsync` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Restriction failure unlocks both items with `ALL_SLOT`; exact Java bytes were not captured. |

## Known Gaps

- Successful legion warehouse replace still returns after restriction checks; no storage mutation, persistence, or `LegionService.addWHItemHistory` exists yet.
- Successful legion warehouse move/split also remains deferred beyond their denial branches.
- Destination-legion-warehouse denial is represented through the shared helper but only source-withdrawal replace was tested in this UOW.
- Java `LegionConfig.LEGION_WAREHOUSE` is not yet modeled in C# runtime config for this handler.
- Exact Java packet bytes for the denial/unlock sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Add focused destination-legion-warehouse denial coverage for `CM_REPLACE_ITEM`, `CM_MOVE_ITEM`, or `CM_SPLIT_ITEM` only when paired with live branch behavior.
2. Scope successful legion warehouse item movement/split/replace after live legion storage, owner mapping, persistence, packet, and history dependencies are identified.
3. Add Java/C# golden capture for denial/update packets if a narrow Java runtime capture becomes available and directly unblocks live behavior.
