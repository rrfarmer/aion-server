# Phase 6 Session 2725 Completion

## UOW

[Phase 6] UOW-2725: Use legion level for live legion warehouse size packets.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live storage-3 SM_CUBE_UPDATE packets now report Java-equivalent legion warehouse expansion count instead of always writing zero.
- Java source/runtime path: SM_CUBE_UPDATE.cubeSize(StorageType.LEGION_WAREHOUSE, Player) -> player.getLegion().getWarehouseExpansions(); Legion.getWarehouseExpansions() -> getLegionLevel() - 1.
- C# runtime artifact wired: Player.LegionWarehouseExpansions and GameServerConnection.CreateStorageSizePacket storage type 3.
- Client-visible/state/persistence effect: item move/split packet fanout involving legion warehouse writes the expansion byte derived from loaded legion level, matching Java's live packet contract.
- Why this is runtime progress: it changes live server packet payloads emitted from existing inventory handlers and uses runtime legion state loaded from the existing legions table join.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_CUBE_UPDATE.java`
  - `cubeSize(StorageType.LEGION_WAREHOUSE, player)` writes item count and `player.getLegion().getWarehouseExpansions()` as `npcExpands`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
  - `getWarehouseExpansions()` returns `getLegionLevel() - 1`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - Warehouse slot limit uses `DEFAULT_ROWS + warehouseExpansions`, confirming the expansion value is level-derived.

## C# Changes

- Added `Player.LegionWarehouseExpansions`, derived from `Math.Max(0, LegionLevel - 1)`.
- Updated `GameServerConnection.CreateStorageSizePacket` for storage type `3` to pass `player.LegionWarehouseExpansions` into `SmCubeUpdate.LegionWarehouseSizeSnapshot`.
- Updated live move/split packet tests to set `LegionLevel` and assert the storage-3 `SM_CUBE_UPDATE` expansion byte.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `SM_CUBE_UPDATE.cubeSize`, `Legion.getWarehouseExpansions` source review | Successful cube-to-legion move sends storage-3 update with `npcExpands = LegionLevel - 1`. | Socket-backed connection fixture, decoded `SmCubeUpdate` payload. | No Java golden bytes captured in this UOW. |
| `ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava` | Unit / live packet dispatch | `SM_CUBE_UPDATE.cubeSize`, `Legion.getWarehouseExpansions` source review | Successful legion-to-cube move sends storage-3 update with `npcExpands = LegionLevel - 1`. | Socket-backed connection fixture, decoded `SmCubeUpdate` payload. | No Java golden bytes captured in this UOW. |
| `ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava` | Unit / live packet dispatch | `SM_CUBE_UPDATE.cubeSize`, `Legion.getWarehouseExpansions` source review | Successful cube-to-legion split sends storage-3 update with `npcExpands = LegionLevel - 1`. | Socket-backed connection fixture, decoded `SmCubeUpdate` payload. | No Java golden bytes captured in this UOW. |
| `ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava` | Unit / live packet dispatch | `SM_CUBE_UPDATE.cubeSize`, `Legion.getWarehouseExpansions` source review | Successful legion-to-cube split sends storage-3 update with `npcExpands = LegionLevel - 1`. | Socket-backed connection fixture, decoded `SmCubeUpdate` payload. | No Java golden bytes captured in this UOW. |

## Validation Decision

```text
- Changed surface: live packet payload state selection for storage-3 SM_CUBE_UPDATE.
- Specific behavior/contract: storage type 3 cube-size fanout writes Java's legion warehouse expansion count, derived from legion level.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceMovesItemToCubeHistoryLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseSourceSplitsItemToCubeHistoryLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 4 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this packet branch in this checkout.
- Broad-validation trigger: live packet payload behavior changed. Broad .NET was skipped because focused validation compiled the affected project and decoded the exact edited packet branch from live move/split dispatch.
- Why this scope is sufficient: the focused command proves the storage-3 `SM_CUBE_UPDATE` payload byte from live handlers for both deposit and withdrawal directions.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `SM_CUBE_UPDATE.cubeSize` | `GameServerConnection.CreateStorageSizePacket` / `SmCubeUpdate.LegionWarehouseSizeSnapshot` | Server packet fanout | Partial | Unit Tested | Partial Parity | Storage-3 count and expansion byte now follow Java for live move/split paths; exact Java packet bytes were not newly captured. |
| `Legion.getWarehouseExpansions` | `Player.LegionWarehouseExpansions` | Model projection | Partial | Unit Tested indirectly | Partial Parity | C# derives from loaded `LegionLevel`; clamps below zero to preserve safe no-legion/default behavior. |
| `LegionWarehouse.updateLimit` | No full C# equivalent | Runtime storage model | Not Started | No Tests | Unknown | This UOW only wires packet expansion value, not dynamic slot-limit enforcement for legion warehouse. |

## Known Gaps

- No direct Java golden packet capture was added.
- C# still lacks Java's full `Legion` aggregate and `LegionWarehouse` slot-limit model.
- Legion warehouse open path is still not fully live; current coverage is from existing move/split packet fanout.
- Direct database integration for loading legion level was not run in this UOW; existing repository load joins `legions.level`.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3
- Total artifacts ported or extended in this UOW: 2
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 3
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire live `OPEN_LEGION_WAREHOUSE` handling to send Java-equivalent `SM_LEGION_EDIT(0x04)`, `SM_WAREHOUSE_INFO` chunks for storage `3`, and `SM_DIALOG_WINDOW` legion warehouse page if the current C# dispatch boundary can safely own the side effect.
2. Model live legion warehouse in-use state (`setInUse` / `unsetInUse`) only if paired with open/close/logout runtime paths in the same UOW.
3. Continue storage-3 runtime parity around warehouse capacity/slot-limit enforcement if Java source shows a live move/split blocker that C# currently skips.
