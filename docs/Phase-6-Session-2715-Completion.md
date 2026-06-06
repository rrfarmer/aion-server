# Phase 6 Session 2715 Completion

## UOW

[Phase 6] UOW-2715: Send legion warehouse move withdrawal denial.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_MOVE_ITEM source LEGION_WAREHOUSE now reaches Java's item-restriction denial branch instead of silently returning before permission checks.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemRestrictionService.isItemRestrictedFrom(LEGION_WAREHOUSE) -> SM_SYSTEM_MESSAGE.STR_GUILD_WAREHOUSE_NO_RIGHT -> ItemPacketService.sendItemUnlockPacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, CreateLegionWarehouseMoveRestrictionMessage, Player legion permission masks, SmSystemMessage.GuildWarehouseNoRight, SendStorageUpdatePacketAsync.
- Client-visible/state/persistence effect: a legion member lacking WH_WITHDRAWAL receives the authority-denial system message and a source-storage unlock packet; no item state mutates.
- Why this is runtime progress: this sends a real Java-equivalent server packet sequence from a live client packet handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
  - Reads item object id, source storage, destination storage, and slot; delegates to `ItemMoveService.moveItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
  - Cross-storage move calls `ItemRestrictionService.isItemRestrictedTo`, then `isItemRestrictedFrom`, and sends item unlock on restriction.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - `isItemRestrictedFrom(LEGION_WAREHOUSE)` sends `STR_GUILD_WAREHOUSE_NO_RIGHT` when legion warehouse is disabled, player is not a legion member, or member lacks `WH_WITHDRAWAL`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
  - `getStorage(LEGION_WAREHOUSE)` returns null when `getLegion()` is null, so no-legion move packets return before restriction messages.

## C# Changes

- `HandleMoveItemAsync` now handles legion warehouse restriction denial before the remaining successful move path returns deferred.
- Added `CreateLegionWarehouseMoveRestrictionMessage` for the Java `ItemRestrictionService` source/destination checks that can be evaluated from current C# runtime facts.
- Successful legion warehouse item movement, storage mutation, persistence, and history remain deferred.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava` | Unit / live packet dispatch | `CM_MOVE_ITEM`, `ItemMoveService.moveItem`, `ItemRestrictionService.isItemRestrictedFrom` source review | Live move-item dispatch sends `STR_GUILD_WAREHOUSE_NO_RIGHT`, unlocks the source legion warehouse item with `ALL_SLOT`, and does not mutate the item. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded system message/add/update packet assertions. | Exact Java runtime bytes were not captured; success movement remains deferred. |

## Validation Decision

```text
- Changed surface: live CM_MOVE_ITEM legion warehouse restriction branch plus focused test coverage.
- Specific behavior/contract: Java denial packet and source unlock for source LEGION_WAREHOUSE when WH_WITHDRAWAL is missing.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The change is isolated to one live denial branch and reuses the permission helper validated in UOW-2714.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited live path plus adjacent shared permission behavior.
- Why this scope is sufficient: tests exercise live packet dispatch, Java-derived denial ordering, source unlock packet fanout, and no state mutation for the scoped branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_ITEM` | `GameServerConnection.HandleMoveItemAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | Source legion warehouse missing-withdrawal denial and unlock are wired. Successful legion moves remain deferred. |
| `com.aionemu.gameserver.services.item.ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Service / live item movement | Partial | Unit Tested | Partial Parity | Restriction branch for source `LEGION_WAREHOUSE` is modeled; history and mutation are not. |
| `com.aionemu.gameserver.services.item.ItemRestrictionService` | `CreateLegionWarehouseMoveRestrictionMessage` | Restriction helper | Partial | Unit Tested indirectly | Partial Parity | Legion source withdrawal denial and some destination checks are represented for move-item gating only. |

## Known Gaps

- Successful legion warehouse item move still returns after restriction checks; no storage mutation, persistence, or `LegionService.addWHItemHistory` exists yet.
- `CM_SPLIT_ITEM` and `CM_REPLACE_ITEM` legion warehouse restriction branches still return before Java-equivalent denial/unlock behavior.
- Java `LegionConfig.LEGION_WAREHOUSE` is not yet modeled in C# runtime config for this handler.
- Exact Java wire bytes for the denial/unlock packet sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 3
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 42%

## Next Runtime UOW Candidates

1. Wire `CM_SPLIT_ITEM` source/destination legion warehouse restriction denial and unlock before the current deferred return.
2. Wire `CM_REPLACE_ITEM` legion warehouse restriction denial and both-item unlock before the current deferred return.
3. Scope successful legion warehouse item movement only after live legion storage, owner mapping, persistence, and history dependencies are identified.
