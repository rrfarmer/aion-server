# Phase 6 Session 2720 Completion

## UOW

[Phase 6] UOW-2720: Split cube items into legion warehouse.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_SPLIT_ITEM cube -> LEGION_WAREHOUSE now proceeds past the previous successful-legion deferred return when Java restriction checks allow it.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> ItemPacketService split/add fanout.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync, existing SaveItemSplitMutationAsync persistence, GetMoveStorageOwnerId location 3 owner mapping, SmInventoryUpdateItem/SmWarehouseAddItem/SmCubeUpdate fanout.
- Client-visible/state/persistence effect: an allowed cube stack split to legion warehouse reduces the source stack, creates a new location 3 item owned by the legion id, persists the source/new item mutation through the existing inventory table shape, and sends source update plus legion warehouse add packets.
- Why this is runtime progress: it mutates live inventory state, persists a new runtime item, and sends real server packets from the live split handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
  - Reads source object id, split amount, source storage, destination object id, destination storage, and slot, then calls `ItemSplitService.splitItem`.
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
  - Restricts cross-storage movement before kinah/split handling, calls `LegionService.addWHItemHistory` for cross-storage non-kinah split/merge, creates a new item for empty destination, decreases source count, sends `SM_CUBE_UPDATE`, and adds the new item to destination storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
  - Legion warehouse destination requires the item to be legion-storable and the actor to have deposit or withdrawal rights.
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
  - Inserts/updates location 3 rows under the player's legion id when available.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
  - `WH_DEPOSIT` is `0x1000`; `WH_WITHDRAWAL` is `0x4`.

## C# Changes

- `HandleSplitItemAsync` now returns from the legion warehouse branch only when Java-equivalent restriction checks deny the operation.
- Allowed legion warehouse splits fall through to the existing Java-shaped split/merge logic.
- Empty-slot cube -> legion split now creates a new item with `OwnerId = player.LegionId`, `Location = 3`, and cross-storage slot `0`, then persists through `SaveItemSplitMutationAsync`.
- The dedicated Java `LegionStorageProxy` / `LegionWarehouse` aggregate and `LegionService.addWHItemHistory` are still not modeled.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `CM_SPLIT_ITEM`, `ItemSplitService.splitItem`, `ItemRestrictionService`, `LegionPermissionsMask`, `InventoryDAO.getItemOwnerId` source review | Live opcode 157 dispatch with `WH_DEPOSIT` permission reduces the cube stack, creates a legion-owned location 3 split item, records the split persistence contract, and emits source update plus legion warehouse add packets. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions, fake repository mutation capture. | Dedicated shared legion warehouse storage, history persistence/fanout, and exact Java packet bytes were not captured. |

## Validation Decision

```text
- Changed surface: live CM_SPLIT_ITEM successful legion warehouse split branch.
- Specific behavior/contract: allowed cube -> LEGION_WAREHOUSE split mutates source/new item counts and owners, records split persistence state, and sends Java-shaped source update plus legion warehouse add/cube-size packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Initial focused attempt: failed because the new allowed-branch fixture used `0x800`, which Java `LegionPermissionsMask` identifies as `GUARDIAN_STONE`, not `WH_DEPOSIT`; corrected fixture to `0x1000` and reran successfully.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The changed behavior is isolated to a previously deferred successful split branch and is directly exercised through live packet dispatch.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited split branch plus adjacent denial/replace coverage.
- Why this scope is sufficient: tests prove the live mutation, packet sequence, and split persistence contract for the scoped branch.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_SPLIT_ITEM.runImpl` | `GameServerConnection.HandleSplitItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Allowed cube -> legion warehouse empty-slot split is wired; other split branches are not fully verified. |
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Item split service | Partial | Unit Tested | Partial Parity | Restriction and split mutation are represented for the scoped branch; history and shared legion storage remain missing. |
| `InventoryDAO.getItemOwnerId` | `InventoryItem.OwnerId` / `MySqlPlayerEnterWorldRepository.InsertInventoryItemAsync` | Repository helper | Partial | Unit Tested indirectly | Partial Parity | The live split creates the new location 3 item with legion owner id; direct DB integration was not run. |
| `LegionPermissionsMask` | `GameServerConnection` legion permission constants | Enum / permission mask | Partial | Unit Tested indirectly | Partial Parity | Test fixture and handler use Java `WH_DEPOSIT = 0x1000`; other permission masks are not exhaustively verified. |

## Known Gaps

- `LegionService.addWHItemHistory` remains unmodeled for Java move/split paths that call it.
- C# still models legion warehouse rows inside `Player.InventoryItems` rather than a shared `LegionStorageProxy` / `LegionWarehouse` aggregate.
- Successful legion split merge and source-legion withdrawal branches are now reachable through the same handler path but are not separately proven by focused tests.
- Legion warehouse expansion counts are not loaded, so size packet expansion fields remain zero.
- Exact Java packet bytes for the successful split sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire successful `CM_LEGION_WH_KINAH` deposit or withdrawal mutation using Java `CM_LEGION_WH_KINAH.runImpl` / legion warehouse kinah storage as source of truth.
2. Implement live legion warehouse history persistence for move/split if the existing legion history schema can be wired directly.
3. Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
