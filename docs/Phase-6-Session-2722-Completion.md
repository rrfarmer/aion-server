# Phase 6 Session 2722 Completion

## UOW

[Phase 6] UOW-2722: Withdraw kinah from legion warehouse.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_LEGION_WH_KINAH action 0 now mutates modeled legion warehouse kinah and cube kinah after Java permission checks instead of returning after the permission branch.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> activePlayer.getStorage(LEGION_WAREHOUSE).tryDecreaseKinah -> activePlayer.getInventory().increaseKinah -> LegionStorageProxy -> ItemPacketService update fanout -> LegionService.addHistory(KINAH_WITHDRAW).
- C# runtime artifact wired: GameServerConnection.HandleLegionWarehouseKinahAsync / HandleLegionWarehouseKinahWithdrawalAsync, modeled location 3 kinah InventoryItem rows, cube kinah row/create path, existing SaveItemMergeMutationAsync persistence, SmWarehouseUpdateItem/SmInventoryUpdateItem packet fanout.
- Client-visible/state/persistence effect: a permitted withdrawal reduces live legion-owned location 3 kinah, increases or creates cube kinah, persists both row counts through the existing inventory table shape, and sends real warehouse/inventory update packets.
- Why this is runtime progress: it wires a deferred live client packet branch, mutates currency state, persists runtime inventory rows, and sends real server packets from live code.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
  - Action `0` requires `WH_WITHDRAWAL`, decreases legion warehouse kinah, increases cube kinah, and records `KINAH_WITHDRAW` history.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `tryDecreaseKinah` checks enough kinah before decreasing; `increaseKinah` creates a zero-count kinah item before increasing count and sending an update.
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
  - Proxies legion warehouse mutation through the acting player so packets are sent to that player.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - Forces mutation through `LegionStorageProxy` and manages shared legion warehouse storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - Default kinah withdrawal masks are `DEC_KINAH_BUY` for source decrease and `INC_KINAH_COLLECT` for destination increase.

## C# Changes

- `HandleLegionWarehouseKinahAsync` now dispatches action `0` into a live withdrawal mutation after Java-equivalent permission checks.
- Added `HandleLegionWarehouseKinahWithdrawalAsync` to:
  - reject non-positive, missing-template, missing-source, and insufficient-source withdrawal attempts,
  - find the modeled legion warehouse kinah row at location `3`,
  - create cube kinah with a generated object id when missing,
  - apply source/target count mutation,
  - persist with `SaveItemMergeMutationAsync`,
  - rollback in-memory state and release newly allocated ids on persistence failure,
  - send legion warehouse decrease and cube inventory increase packets.
- `LegionService.addHistory(KINAH_WITHDRAW)` remains deferred.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH`, `Storage`, `LegionStorageProxy`, `ItemPacketService` source review | Live opcode 76 action 0 with `WH_WITHDRAWAL` reduces legion-owned location 3 kinah, increases cube kinah, records merge persistence state, and emits `DEC_KINAH_BUY` plus `INC_KINAH_COLLECT` packets. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions, fake repository mutation capture. | Shared legion warehouse aggregate, history row persistence, exact Java packet bytes, and direct DB integration were not captured. |

## Validation Decision

```text
- Changed surface: live CM_LEGION_WH_KINAH successful withdrawal branch.
- Specific behavior/contract: allowed action 0 withdrawal mutates legion/cube kinah rows, records source/target persistence state, and sends Java-shaped kinah update packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 3 tests passed.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The changed behavior is isolated to one live packet action and is directly exercised by live packet dispatch plus adjacent deposit and denial coverage.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited branch.
- Why this scope is sufficient: tests prove live mutation, persistence call shape, permission guard adjacency, and packet update masks for the scoped withdrawal path.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Successful action 0 withdrawal and action 1 deposit are wired; Java history remains missing. |
| `Storage.tryDecreaseKinah` / `Storage.increaseKinah` | `GameServerConnection.HandleLegionWarehouseKinahWithdrawalAsync` | Storage mutation | Partial | Unit Tested | Partial Parity | Positive withdrawal mutation and cube create path are represented for modeled rows; Java history side effect is not modeled. |
| `LegionStorageProxy` / `LegionWarehouse` | `Player.InventoryItems` location 3 model | Storage proxy / aggregate | Partial | Unit Tested indirectly | Partial Parity | C# still models legion warehouse kinah as player-scoped location 3 rows, not shared storage. |
| `ItemPacketService.ItemUpdateType` | `SmWarehouseUpdateItem` / `SmInventoryUpdateItem` | Packet update masks | Partial | Unit Tested | Partial Parity | Scoped withdrawal uses `DEC_KINAH_BUY` and `INC_KINAH_COLLECT`; exact Java bytes were not captured. |

## Known Gaps

- `LegionService.addHistory(KINAH_DEPOSIT/KINAH_WITHDRAW)` is not implemented.
- C# still models legion warehouse items and kinah inside `Player.InventoryItems` with `Location = 3` rather than a shared legion warehouse aggregate.
- `CM_LEGION_HISTORY` still has no runtime data source even though `SmLegionHistory` is ported.
- Direct DB integration for the location 3 kinah update was not run.
- Exact Java packet bytes for the withdrawal sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Implement live legion warehouse history persistence for `KINAH_DEPOSIT` / `KINAH_WITHDRAW` and use it from the completed `CM_LEGION_WH_KINAH` deposit/withdrawal paths if the existing `legion_history` schema can be wired directly.
2. Wire `CM_LEGION_HISTORY` to a runtime legion history data source and send `SmLegionHistory` for represented history rows.
3. Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
