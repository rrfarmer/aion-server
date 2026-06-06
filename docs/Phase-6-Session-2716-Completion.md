# Phase 6 Session 2716 Completion

## UOW

[Phase 6] UOW-2716: Send legion warehouse split withdrawal denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM source LEGION_WAREHOUSE now reaches Java's cross-storage restriction denial branch instead of silently returning before source item checks.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService.isItemRestrictedTo / isItemRestrictedFrom -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> ItemPacketService.sendStorageUpdatePacket.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync, CreateLegionWarehouseMoveRestrictionMessage, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message and a source legion warehouse storage update; no split, merge, inventory, or persistence state mutates.
- Why this is runtime progress: this sends Java-equivalent server packets from a live client packet handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
  - Reads source object id, amount, source storage, destination object id, destination storage, and slot; delegates to `ItemSplitService.splitItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Checks trading, resolves source/destination storages and source item, then applies cross-storage restrictions before Kinah and split/merge mutation.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Source `LEGION_WAREHOUSE` without `WH_WITHDRAWAL` sends `STR_GUILD_WAREHOUSE_NO_RIGHT`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - Split restriction failure sends `sendStorageUpdatePacket(player, sourceStorage.getStorageType(), sourceItem)`, which uses `ITEM_COLLECT`.

## C# Changes

- `HandleSplitItemAsync` no longer returns before resolving source/template for legion warehouse paths.
- The handler now runs the Java-shaped cross-storage legion restriction branch before Kinah/general split mutation.
- Denied legion splits send the Java authority message plus a source storage update with `ItemCollect`; successful legion warehouse split remains deferred.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava` | Unit / live packet dispatch | `CM_SPLIT_ITEM`, `ItemSplitService.splitItem`, `ItemRestrictionService`, `ItemPacketService` source review | Live split dispatch sends `STR_GUILD_WAREHOUSE_NO_RIGHT`, sends source legion warehouse update with `ITEM_COLLECT`, and leaves item count/location unchanged. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions. | Exact Java runtime bytes were not captured; success split remains deferred. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM legion warehouse restriction branch plus focused test coverage.
- Specific behavior/contract: Java denial packet and source storage update for source LEGION_WAREHOUSE when WH_WITHDRAWAL is missing.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live split denial branch and reuses the UOW-2715 legion restriction helper.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited live split path plus adjacent move behavior.
- Why this scope is sufficient: tests exercise live packet dispatch, Java-derived denial ordering, source update packet type, and no state mutation for the scoped branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SPLIT_ITEM` | `GameServerConnection.HandleSplitItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse missing-withdrawal denial and source update are wired. Successful legion splits remain deferred. |
| `com.aionemu.gameserver.services.item.ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Service / live item split | Partial | Unit Tested | Partial Parity | Cross-storage legion restriction branch is represented; history and mutation are not. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket` | `SendStorageUpdatePacketAsync` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Denial branch uses source storage add/update fanout with `ITEM_COLLECT`; exact Java bytes were not captured. |

## Known Gaps

- Successful legion warehouse split still returns after restriction checks; no storage mutation, persistence, or `LegionService.addWHItemHistory` exists yet.
- `CM_REPLACE_ITEM` still returns before legion warehouse restriction denial and both-item unlock.
- Destination-legion-warehouse denial is implemented through the shared helper but only source-withdrawal split was tested in this UOW.
- Java `LegionConfig.LEGION_WAREHOUSE` is not yet modeled in C# runtime config for this handler.
- Exact Java packet bytes for the denial/update sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire `CM_REPLACE_ITEM` legion warehouse restriction denial and both-item unlock before the current deferred return.
2. Add focused destination-legion-warehouse denial coverage if paired with another runtime branch consuming the shared restriction helper.
3. Scope successful legion warehouse item movement/split only after live legion storage, owner mapping, persistence, and history dependencies are identified.
