# Phase 6 Session 2718 Handoff

## Completed UOW

[Phase 6] UOW-2718: Move cube items into legion warehouse.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_MOVE_ITEM cube -> LEGION_WAREHOUSE now executes Java's successful cross-storage mutation path after restriction checks instead of returning after denial handling.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemRestrictionService -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> sendItemDeletePacket/sendStorageUpdatePacket.
- C# runtime artifact wired: GameServerConnection.HandleMoveItemAsync, GetMoveStorageOwnerId, CreateStorageSizePacket, PlayerEnterWorldService, MySqlPlayerEnterWorldRepository.
- Client-visible/state/persistence effect: allowed legion deposit mutates live item owner/location/slot, records legion id for persistence, and sends source delete/cube update plus legion warehouse add/cube update.
- Why this is runtime progress: it mutates live inventory state, persists through the existing inventory table shape, and sends real server packets from live code.
```

## Commit

`[Phase 6][UOW-2718] Move cube items into legion warehouse`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2718-Completion.md`
- `docs/Phase-6-Session-2718-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_MOVE_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemMoveService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/Player.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleMoveItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.GetMoveStorageOwnerId`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateStorageSizePacket`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveItemCrossStorageMoveMutationAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService.SaveItemStorageSwitchMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemCrossStorageMoveMutationAsync`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.SaveItemStorageSwitchMutationAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`
- `Aion.GameServer.Tests.PlayerEnterWorldServiceTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_ReplaceItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 4
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

- `CM_MOVE_ITEM` now has partial runtime parity for allowed cube -> legion warehouse item deposits, including live state mutation, packet fanout, and legion-owner persistence selection.
- Full legion warehouse move parity is not claimed. Dedicated legion storage aggregate, history, expansion counts, successful withdrawal, and exact Java bytes remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_MOVE_ITEM.runImpl` | `GameServerConnection.HandleMoveItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Successful cube -> legion deposit is wired; other successful legion move branches remain. |
| `ItemMoveService.moveItem` | `GameServerConnection.HandleMoveItemAsync` | Item move service | Partial | Unit Tested | Partial Parity | Move mutation and packets are represented for deposit; history and dedicated storage proxy are not. |
| `InventoryDAO.getItemOwnerId` | `MySqlPlayerEnterWorldRepository.GetStorageOwnerId` | Repository helper | Partial | Unit Tested indirectly | Partial Parity | Account and legion owner selection are represented; direct DB integration for legion rows remains unrun. |
| `SM_CUBE_UPDATE.cubeSize` | `SmCubeUpdate.LegionWarehouseSizeSnapshot` / `CreateStorageSizePacket` | Packet fanout | Partial | Unit Tested indirectly | Partial Parity | Legion item count is modeled; expansion count remains zero until legion warehouse expansions are loaded. |

## Known Gaps / Watchouts

- `LegionService.addWHItemHistory` is not implemented.
- Modeled legion warehouse items still live in `Player.InventoryItems` with `Location = 3`; Java uses a `LegionStorageProxy` over shared `LegionWarehouse`.
- Successful legion withdrawal, split success, and replace success remain deferred.
- Legion warehouse expansion count is not loaded into the C# player/legion model.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire successful `CM_MOVE_ITEM` source `LEGION_WAREHOUSE` withdrawal to cube using the newly added legion owner persistence path.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_MOVE_ITEM LEGION_WAREHOUSE -> CUBE with WH_WITHDRAWAL should mutate the item back to player-owned cube storage instead of only supporting denial.
- Java source/runtime path: CM_MOVE_ITEM.runImpl -> ItemMoveService.moveItem -> ItemRestrictionService.isItemRestrictedFrom -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> ItemPacketService delete/add fanout.
- C# runtime artifact likely involved: GameServerConnection.HandleMoveItemAsync, GetMoveStorageOwnerId, PlayerEnterWorldService.SaveItemCrossStorageMoveMutationAsync, MySqlPlayerEnterWorldRepository.GetStorageOwnerId.
- Client-visible/state/persistence effect expected: item owner changes from legion id to player id, location changes 3 -> 0, source warehouse delete and cube add packets are sent, and persistence updates the existing inventory row using old owner legion id.
- Why this is runtime progress: it mutates live item state, persists the movement using the database row shape, and sends real server packets from the live move handler.
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_MoveItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless Java source or a narrow Java fixture changes. Broad-validation trigger: none if the UOW stays within this live move branch.

## Other Safe Runtime Candidates

- Wire successful legion warehouse replacement after adding focused storage-switch owner tests.
- Wire successful legion split only after owner/history/count behavior is scoped carefully.
- Implement live legion warehouse history persistence if the existing legion history schema can be wired directly.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `f2f78dd7a [Phase 6][UOW-2717] Send legion warehouse replace denial`
  - `4180c7d5f [Phase 6][UOW-2716] Send legion warehouse split denial`
  - `e6aa8a8e3 [Phase 6][UOW-2715] Send legion warehouse move denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
