# Phase 6 Session 2720 Handoff

## Completed UOW

[Phase 6] UOW-2720: Split cube items into legion warehouse.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: CM_SPLIT_ITEM cube -> LEGION_WAREHOUSE now executes Java's successful empty-slot split path after restriction checks instead of returning after denial handling.
- Java source/runtime path: CM_SPLIT_ITEM.runImpl -> ItemSplitService.splitItem -> ItemRestrictionService -> LegionService.addWHItemHistory -> InventoryDAO.getItemOwnerId -> sendStorageUpdatePacket.
- C# runtime artifact wired: GameServerConnection.HandleSplitItemAsync plus existing split persistence and storage packet fanout.
- Client-visible/state/persistence effect: allowed legion split reduces the cube source stack, creates a legion-owned location 3 item, records split persistence state, and sends inventory update plus legion warehouse add packets.
- Why this is runtime progress: it mutates live inventory state, persists a new runtime item, and sends real server packets from live code.
```

## Commit

`[Phase 6][UOW-2720] Split cube items into legion warehouse`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2720-Completion.md`
- `docs/Phase-6-Session-2720-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_SPLIT_ITEM.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemSplitService.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemRestrictionService.java`
- `game-server/src/com/aionemu/gameserver/dao/InventoryDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleSplitItemAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_SplitItemFromLegionWarehouseWithoutWithdrawalSendsNoRightLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava" --logger "console;verbosity=minimal"
```

Result:

- Passed: 3
- Failed: 0
- Skipped: 0
- Existing warnings only.

Validation note:

- The first focused attempt failed because the new allowed-branch fixture used `0x800`, which Java `LegionPermissionsMask` defines as `GUARDIAN_STONE`; corrected to `WH_DEPOSIT = 0x1000` and reran the same command successfully.

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

- `CM_SPLIT_ITEM` now has partial runtime parity for allowed cube -> legion warehouse empty-slot split, including live state mutation, packet fanout, and legion-owner persistence selection.
- Full legion warehouse split parity is not claimed. Dedicated shared legion storage, history, expansion counts, merge/source-legion branch tests, and exact Java bytes remain gaps.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_SPLIT_ITEM.runImpl` | `GameServerConnection.HandleSplitItemAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Successful cube -> legion empty-slot split is wired after restriction checks. |
| `ItemSplitService.splitItem` | `GameServerConnection.HandleSplitItemAsync` | Item split service | Partial | Unit Tested | Partial Parity | Source count decrease, new item creation, and packet fanout are represented for the scoped branch. |
| `InventoryDAO.getItemOwnerId` | `InventoryItem.OwnerId` / `MySqlPlayerEnterWorldRepository.InsertInventoryItemAsync` | Repository helper | Partial | Unit Tested indirectly | Partial Parity | Location 3 split rows use legion owner context through the new item state. |
| `LegionPermissionsMask` | `GameServerConnection` permission constants | Enum / permission mask | Partial | Unit Tested indirectly | Partial Parity | `WH_DEPOSIT = 0x1000` was verified during fixture correction; exhaustive enum parity was not in scope. |

## Known Gaps / Watchouts

- Modeled legion warehouse items still live in `Player.InventoryItems` with `Location = 3`; Java uses a `LegionStorageProxy` over shared `LegionWarehouse`.
- `LegionService.addWHItemHistory` is not implemented for Java move/split paths that call it.
- Successful legion split merge and source-legion withdrawal branches are reachable but not separately proven.
- Legion warehouse expansion count is not loaded into the C# player/legion model.
- Exact Java packet bytes were not captured.

## Next Recommended Runtime UOW

Recommended candidate: wire successful `CM_LEGION_WH_KINAH` deposit or withdrawal mutation.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: CM_LEGION_WH_KINAH with sufficient legion warehouse rights should mutate player/legion kinah instead of returning after permission checks.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LegionService/LegionWarehouse kinah mutation and history -> SM_LEGION_TABS / storage update fanout as applicable.
- C# runtime artifact likely involved: GameServerConnection.HandleLegionWarehouseKinahAsync, player inventory kinah item handling, persistence methods for kinah count/item owner, and any modeled legion warehouse kinah state.
- Client-visible/state/persistence effect expected: kinah count moves between player inventory and modeled legion warehouse storage, permission denials remain unchanged, and real response packets are sent from live code.
- Why this is runtime progress: it fills an explicitly deferred live packet body, mutates runtime currency state, persists through the existing item row shape if modeled as kinah item state, and sends real packets.
```

Suggested focused discovery/validation starting point:

```powershell
rg -n "CM_LEGION_WH_KINAH|LegionWarehouse|addHistory|Kinah" game-server/src dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Expected focused C# validation after implementation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
```

Java/Maven is not expected unless a narrow Java fixture is found during discovery. Broad-validation trigger: none if the UOW stays within this live kinah branch.

## Other Safe Runtime Candidates

- Implement live legion warehouse history persistence if the existing legion history schema can be wired directly.
- Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
- Port another small deferred live packet branch from `GameServerConnection` after checking that it mutates state or sends real packets.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `486884578 [Phase 6][UOW-2719] Replace cube and legion warehouse items`
  - `7eb5f728b [Phase 6][UOW-2718] Move cube items into legion warehouse`
  - `f2f78dd7a [Phase 6][UOW-2717] Send legion warehouse replace denial`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
