# Phase 6 Session 2719 Handoff

## Completed UOW

[Phase 6] UOW-2719: Replace cube and legion warehouse items.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_REPLACE_ITEM cube <-> LEGION_WAREHOUSE now executes Java's successful storage-switch mutation path after restriction checks instead of returning after denial handling.
- Java source/runtime path: CM_REPLACE_ITEM.runImpl -> ItemMoveService.switchItemsInStorages -> ItemRestrictionService -> Player.getStorage -> LegionStorageProxy -> InventoryDAO.getItemOwnerId -> sendItemDeletePacket/sendStorageUpdatePacket.
- C# runtime artifact wired: GameServerConnection.HandleReplaceItemAsync plus existing storage-switch persistence and packet fanout.
- Client-visible/state/persistence effect: allowed legion replacement swaps live item owner/location/slot state, records legion id for persistence owner selection, and sends delete/add storage packets for both items.
- Why this is runtime progress: it mutates live inventory state, persists through the existing inventory table shape, and sends real server packets from live code.
```

## Commit

`[Phase 6][UOW-2719] Replace cube and legion warehouse items`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2719-Completion.md`
- `docs/Phase-6-Session-2719-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_REPLACE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleReplaceItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava|FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0
- Existing warnings only.

Repository hygiene:

```powershell
git diff --check
```

Result:

- Passed with only Git CRLF working-copy warnings for touched files.

Java/Maven:

- Not run. Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.

Broad .NET:

- Not run. Broad-validation trigger: none.

## Conservative Parity Status

- `CM_REPLACE_ITEM` now has partial runtime parity for allowed cube <-> legion warehouse replacement, including live state mutation, packet fanout, and legion-owner persistence selection.
- Full legion warehouse replace parity is not claimed. Dedicated shared legion storage, expansion counts, and exact Java bytes remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_REPLACE_ITEM.runImpl` | `GameServerConnection.HandleReplaceItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Successful cube <-> legion replacement is wired after restriction checks. |
| `ItemMoveService.switchItemsInStorages` | `GameServerConnection.HandleReplaceItemAsync` | Item switch service | Partial | Unit Tested | Partial Parity | Slot/location/owner swap and delete/add packet fanout are represented for the scoped branch. |
| `InventoryDAO.getItemOwnerId` | `MySqlPlayerEnterWorldRepository.GetStorageOwnerId` | Repository helper | Partial | Unit Tested indirectly | Partial Parity | Location 3 replacement rows use legion owner context through the persistence contract. |

## Known Gaps / Watchouts

- Modeled legion warehouse items still live in `Player.InventoryItems` with `Location = 3`; Java uses a `LegionStorageProxy` over shared `LegionWarehouse`.
- Successful legion split remains deferred in `HandleSplitItemAsync`.
- `LegionService.addWHItemHistory` is not implemented for Java paths that use it.
- Legion warehouse expansion count is not loaded into the C# player/legion model.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire successful `CM_SPLIT_ITEM` cube -> `LEGION_WAREHOUSE` split.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_SPLIT_ITEM cube -> LEGION_WAREHOUSE with sufficient legion warehouse rights should create or merge the destination stack instead of returning after denial handling.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService.isItemRestrictedTo/isItemRestrictedFrom -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> ItemPacketService split/delete/add fanout.
- C# runtime artifact likely involved: GameServerConnection.HandleSplitItemAsync, PlayerEnterWorldService.SaveItemSplitMutationAsync/SaveItemMergeMutationAsync, MySqlPlayerEnterWorldRepository owner selection for new or merged location 3 rows.
- Client-visible/state/persistence effect expected: source stack count changes, destination legion item is created or merged under legion owner id, live packets update both storages, and persistence uses the existing inventory table shape.
- Why this is runtime progress: it mutates live item counts or creates live item state, persists the change, and sends real server packets from the live split handler.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or a narrow Java fixture changes. Broad-validation trigger: none if the UOW stays within this live split branch.

## Other Safe Runtime Candidates

- Wire successful `CM_SPLIT_ITEM` legion warehouse -> cube withdrawal after source-owner persistence is scoped.
- Implement live legion warehouse history persistence if the existing legion history schema can be wired directly.
- Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `7eb5f728b [Phase 6][UOW-2718] Move cube items into legion warehouse`
  - `f2f78dd7a [Phase 6][UOW-2717] Send legion warehouse replace denial`
  - `4180c7d5f [Phase 6][UOW-2716] Send legion warehouse split denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
