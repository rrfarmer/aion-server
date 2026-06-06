# Phase 6 Session 2721 Completion

## UOW

[Phase 6] UOW-2721: Deposit kinah into legion warehouse.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: live CM_LEGION_WH_KINAH action 1 now mutates cube and modeled legion warehouse kinah after Java permission checks instead of stopping after the permission branch.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> Player.getInventory().tryDecreaseKinah -> Player.getStorage(LEGION_WAREHOUSE).increaseKinah -> LegionStorageProxy -> Storage.increaseKinah -> LegionService.addHistory(KINAH_DEPOSIT).
- C# runtime artifact wired: GameServerConnection.HandleLegionWarehouseKinahAsync / HandleLegionWarehouseKinahDepositAsync, modeled location 3 kinah InventoryItem rows, existing SaveItemMergeMutationAsync persistence, SmInventoryUpdateItem/SmWarehouseAddItem/SmWarehouseUpdateItem packet fanout.
- Client-visible/state/persistence effect: a permitted deposit reduces live cube kinah, creates or increases a legion-owned location 3 kinah row, persists both row counts through the existing inventory table shape, and sends real inventory/warehouse update packets.
- Why this is runtime progress: it mutates live currency state, persists runtime inventory rows, and sends real server packets from a live client packet handler.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
  - Action `1` requires `WH_DEPOSIT`, decreases cube kinah, increases legion warehouse kinah, and records `KINAH_DEPOSIT` history.
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
  - `tryDecreaseKinah` checks enough kinah before `decreaseKinah`; `increaseKinah` creates a zero-count kinah item before increasing count and sending an update.
- `game-server/src/com/aionemu/gameserver/model/items/storage/LegionStorageProxy.java`
  - Proxies legion warehouse kinah mutation through the acting player so packets are sent to the actor.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
  - Forces mutation through `LegionStorageProxy` and manages the shared legion warehouse storage.
- `game-server/src/com/aionemu/gameserver/services/item/ItemPacketService.java`
  - Default kinah deposit masks are `DEC_KINAH_BUY` for cube decrease and `INC_KINAH_COLLECT` for legion increase.

## C# Changes

- `HandleLegionWarehouseKinahAsync` now dispatches action `1` into a live deposit mutation after Java-equivalent permission checks.
- Added `HandleLegionWarehouseKinahDepositAsync` to:
  - find the cube kinah row,
  - create a modeled location `3` kinah row with legion owner id when missing,
  - apply overflow-checked source/target count mutation,
  - persist with `SaveItemMergeMutationAsync`,
  - rollback in-memory state on persistence failure,
  - send cube decrease and legion warehouse add/update packets.
- Added a local clone helper so the zero-count warehouse-add packet remains stable when captured tests serialize packets after runtime state has advanced.
- Successful kinah withdrawal and `LegionService.addHistory` remain deferred.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH`, `Storage`, `LegionStorageProxy`, `ItemPacketService` source review | Live opcode 76 action 1 with `WH_DEPOSIT` reduces cube kinah, creates legion-owned location 3 kinah, records merge persistence state, and emits `DEC_KINAH_BUY`, zero-count warehouse add, cube-size, and `INC_KINAH_COLLECT` packets. | Socket-backed connection fixture, live `ProcessPacketAsync`, decoded packet assertions, fake repository mutation capture. | Shared legion warehouse aggregate, history row persistence, withdrawal, and exact Java packet bytes were not captured. |

## Validation Decision

```text
- Changed surface: live CM_LEGION_WH_KINAH successful deposit branch.
- Specific behavior/contract: allowed action 1 deposit mutates cube/legion kinah rows, records source/target persistence state, and sends Java-shaped kinah update packets.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahVolunteerWithdrawalSendsNoRightLikeJava" --logger "console;verbosity=minimal"
- Result: passed; 2 tests passed.
- Initial focused attempts: first failed because IDFactory([9001]) means 9001 is already used and the next generated id is 1; second failed because captured packet serialization saw the live target count after the zero-count add step. Fixed the fixture expectation and used a zero-count snapshot for the add packet.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: none. The changed behavior is isolated to one live packet action and is directly exercised by live packet dispatch plus adjacent denial coverage.
- Broad .NET decision: skipped; the filtered command built affected projects and validated the edited branch.
- Why this scope is sufficient: tests prove live mutation, rollback-sensitive persistence call shape, permission guard adjacency, and packet update masks for the scoped deposit path.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Successful action 1 deposit is wired; action 0 withdrawal and history remain missing. |
| `Storage.increaseKinah` / `tryDecreaseKinah` | `GameServerConnection.HandleLegionWarehouseKinahDepositAsync` | Storage mutation | Partial | Unit Tested | Partial Parity | Positive deposit count mutation and zero-count add packet are represented for modeled rows; non-positive history behavior is not modeled. |
| `LegionStorageProxy` / `LegionWarehouse` | `Player.InventoryItems` location 3 model | Storage proxy / aggregate | Partial | Unit Tested indirectly | Partial Parity | C# still models legion warehouse kinah as player-scoped location 3 rows, not shared storage. |
| `ItemPacketService.ItemUpdateType` | `SmInventoryUpdateItem` / `SmWarehouseUpdateItem` | Packet update masks | Partial | Unit Tested | Partial Parity | Scoped deposit uses `DEC_KINAH_BUY` and `INC_KINAH_COLLECT`; exact Java bytes were not captured. |

## Known Gaps

- Successful legion warehouse kinah withdrawal remains deferred.
- `LegionService.addHistory(KINAH_DEPOSIT)` is not implemented.
- C# still models legion warehouse items and kinah inside `Player.InventoryItems` with `Location = 3` rather than a shared legion warehouse aggregate.
- Direct DB integration for the new location 3 kinah insert/update was not run.
- Exact Java packet bytes for the deposit sequence were not captured.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or extended in this UOW: 4
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire successful `CM_LEGION_WH_KINAH` action 0 withdrawal using the modeled location 3 kinah row as the source and cube kinah as the destination.
2. Implement live legion warehouse history persistence for kinah deposit/withdrawal if the existing schema can be wired directly.
3. Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
