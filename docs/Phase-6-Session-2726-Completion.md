# Phase 6 Session 2726 Completion

## UOW

[Phase 6] UOW-2726: Open live legion warehouse dialog.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: CM_DIALOG_SELECT action OPEN_LEGION_WAREHOUSE now sends the live legion warehouse packet sequence from C# instead of falling through without runtime packets.
- Java source/runtime path: DialogService.onDialogSelect OPEN_LEGION_WAREHOUSE -> LegionService.openLegionWarehouse -> SM_LEGION_EDIT(0x04), SM_WAREHOUSE_INFO chunks for StorageType.LEGION_WAREHOUSE, SM_DIALOG_WINDOW(DialogPage.LEGION_WAREHOUSE).
- C# runtime artifact wired: CmDialogSelect.OpenLegionWarehouse, GameServerConnection.HandleDialogSelectAsync/HandleOpenLegionWarehouseDialogAsync, SmWarehouseInfo.CreateLegionWarehouseOpenPackets, SmDialogWindow.LegionWarehousePageId.
- Client-visible/state/persistence effect: a client selecting a valid legion warehouse NPC action receives real warehouse kinah, storage-3 item list, and dialog page packets.
- Why this is runtime progress: it wires a deferred live client packet path and sends real server packets from live dialog dispatch.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/DialogService.java`
  - `OPEN_LEGION_WAREHOUSE` delegates to `LegionService.openLegionWarehouse`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `openLegionWarehouse` sends warehouse kinah, storage-3 `SM_WAREHOUSE_INFO` chunks, a final empty warehouse-info packet, and `SM_DIALOG_WINDOW` page `DialogPage.LEGION_WAREHOUSE`.
  - `canOpenWarehouse` guards membership, enabled NPC action, disbanding, permissions, and in-use lock.
- `game-server/src/com/aionemu/gameserver/model/DialogPage.java`
  - `LEGION_WAREHOUSE` page id is `25`.
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_WAREHOUSE_INFO.java`
  - Packet writes warehouse type, first flag, expand level, item count, and item blobs.

## C# Changes

- Added `CmDialogSelect.OpenLegionWarehouse = 53`.
- Added `SmDialogWindow.LegionWarehousePageId = 25`.
- Added `SmWarehouseInfo.CreateLegionWarehouseOpenPackets` for storage `3`, Java 10-item chunking, level-derived expansion, and final empty packet.
- Added `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` and wired it into `HandleDialogSelectAsync`.
- The live branch validates target/action, legion membership/rank, and deposit-or-withdrawal permission, then sends `SmLegionEdit.WarehouseKinah`, the storage-3 warehouse info packets, and the legion warehouse dialog page.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleDialogSelectAsync_OpenLegionWarehouseSendsJavaPacketSequence` | Unit / live dialog dispatch | `DialogService.onDialogSelect`, `LegionService.openLegionWarehouse`, `DialogPage.LEGION_WAREHOUSE` source review | Valid live action `53` sends `SmLegionEdit` kinah, storage-3 `SmWarehouseInfo` item/final packets with expansion level, and `SmDialogWindow` page `25`. | Socket-backed connection fixture, live `HandleDialogSelectAsync`, decoded server packet payloads. | Denial branches, in-use locking, disbanding guard, and exact Java golden bytes remain untested. |

## Validation Decision

```text
- Changed surface: live dialog packet dispatch and warehouse-info packet composition.
- Specific behavior/contract: OPEN_LEGION_WAREHOUSE sends Java's successful packet sequence from live C# dialog handling.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionStorageExpansionDialogTests|FullyQualifiedName~SmLegionEditTests" --logger "console;verbosity=minimal"
- Result: passed; 24 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this dialog branch in this checkout.
- Broad-validation trigger: live dialog packet dispatch. Broad .NET was skipped because the focused command compiled the affected project and decoded the edited packet sequence from live dialog handling.
- Why this scope is sufficient: the focused command exercises the new live branch, the new packet helper, existing legion edit packet bytes, and adjacent dialog behavior without running unrelated game-server tests.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `DialogService.onDialogSelect` | `GameServerConnection.HandleDialogSelectAsync` | Live client packet handler | Partial | Unit Tested | Partial Parity | `OPEN_LEGION_WAREHOUSE` successful path now sends live packets; other dialog actions remain mixed live/non-live. |
| `LegionService.openLegionWarehouse` | `GameServerConnection.HandleOpenLegionWarehouseDialogAsync` | Service side effect | Partial | Unit Tested | Partial Parity | Sends kinah, storage-3 warehouse info, and dialog page; in-use state, disbanding, enabled config, and all denial messages remain gaps. |
| `SM_WAREHOUSE_INFO` | `SmWarehouseInfo.CreateLegionWarehouseOpenPackets` | Server packet helper | Partial | Unit Tested | Partial Parity | Storage-3 open chunks and final packet are covered; exact Java packet golden bytes were not captured. |
| `DialogPage.LEGION_WAREHOUSE` | `SmDialogWindow.LegionWarehousePageId` | Constant / packet field | Complete | Unit Tested indirectly | Partial Parity | Page id `25` is used by live open path; broader DialogPage enum is not modeled. |

## Known Gaps

- Java `LegionWarehouse.setInUse` / `getCurrentUser` lock state is not modeled.
- Denial branches for no legion, disabled config/unsupported NPC action, disbanding legion, no permission, and in-use warehouse are not fully represented.
- `LegionWhUpdate` details beyond the kinah packet and warehouse info sequence were not separately modeled.
- Exact Java golden bytes for `SM_WAREHOUSE_INFO` storage `3` were not captured.
- C# still stores legion warehouse items in `Player.InventoryItems` rather than a full Java `LegionWarehouse` aggregate.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Add live legion warehouse open denial branches for no legion, missing permissions, and unsupported NPC action, matching Java `canOpenWarehouse` messages where C# packet helpers exist.
2. Model live legion warehouse in-use state for open/close/logout paths, including `CLOSE_LEGION_WAREHOUSE`/dialog close release, if a small runtime state holder can be added safely.
3. Add legion warehouse capacity checks to live move/split paths using Java `LegionWarehouse.updateLimit` if discovery identifies a current C# over-capacity gap.
