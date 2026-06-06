# Phase 6 Session 2723 Completion

## UOW

[Phase 6] UOW-2723: Persist and send live legion warehouse history.

## Runtime Progress Gate

```text
- Deferred/live behavior advanced: successful CM_LEGION_WH_KINAH action 0/1 now records KINAH_WITHDRAW/KINAH_DEPOSIT history, and CM_LEGION_HISTORY now sends represented history rows instead of returning because C# had no data source.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LegionService.addHistory -> LegionDAO.insertHistory; CM_LEGION_HISTORY.runImpl -> player.getLegion().getHistory(type) -> SM_LEGION_HISTORY.
- C# runtime artifact wired: GameServerConnection kinah handlers, GameServerConnection.HandleLegionHistoryAsync, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository legion history methods, LegionHistoryActions/LegionHistoryRow, existing SmLegionHistory packet.
- Client-visible/state/persistence effect: successful live legion warehouse kinah mutations write durable legion_history rows, and live opcode 55 can load warehouse history rows and send SM_LEGION_HISTORY to the client.
- Why this is runtime progress: it persists runtime legion state from live packet handlers and sends a real server packet from a live client packet path.
```

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
  - Successful action `0` records `KINAH_WITHDRAW`; successful action `1` records `KINAH_DEPOSIT`.
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
  - `addHistory` delegates to `LegionDAO.insertHistory`, then adds the returned entry to the in-memory legion history list.
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
  - Inserts `legion_id`, `date`, `history_type`, `name`, and `description`; loads rows ordered by `date DESC, id DESC`.
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`
  - Maps `KINAH_DEPOSIT` to id `17` and `KINAH_WITHDRAW` to id `18`, both with type `WAREHOUSE`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_HISTORY.java`
  - Returns silently with no legion, returns silently for `REWARD` history unless brigade general, otherwise sends `SM_LEGION_HISTORY`.

## C# Changes

- Added `LegionHistoryActions` and `LegionHistoryRow` under `Aion.GameServer.Model.Legion`.
- Added `InsertLegionHistoryAsync` and `LoadLegionHistoryAsync` to `IPlayerEnterWorldRepository`, the empty test repository, and the MySQL repository.
- MySQL history write uses the existing `legion_history` table and Java enum action names.
- MySQL history load preserves Java ordering and filters rows to the requested Java type ordinal.
- `HandleLegionWarehouseKinahDepositAsync` now writes `KINAH_DEPOSIT` after successful mutation packets.
- `HandleLegionWarehouseKinahWithdrawalAsync` now writes `KINAH_WITHDRAW` after successful mutation packets.
- `CM_LEGION_HISTORY` now dispatches to live `HandleLegionHistoryAsync`, including Java's no-legion and reward/brigade-general guards.
- Corrected the `CmLegionHistory` parser comment to use Java type ordinals `0=LEGION`, `1=REWARD`, `2=WAREHOUSE`.

## Tests Added / Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH`, `LegionService.addHistory`, `LegionDAO.insertHistory` source review | Successful live deposit still mutates/persists kinah and now records `KINAH_DEPOSIT` with player name and amount. | Socket-backed connection fixture, live `ProcessPacketAsync`, fake repository history capture. | Direct DB insert was not integration-tested; Java in-memory history trimming is not modeled. |
| `ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava` | Unit / live packet dispatch | `CM_LEGION_WH_KINAH`, `LegionService.addHistory`, `LegionDAO.insertHistory` source review | Successful live withdrawal still mutates/persists kinah and now records `KINAH_WITHDRAW` with player name and amount. | Socket-backed connection fixture, live `ProcessPacketAsync`, fake repository history capture. | Direct DB insert was not integration-tested; Java in-memory history trimming is not modeled. |
| `ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava` | Unit / live packet dispatch | `CM_LEGION_HISTORY`, `LegionDAO.loadHistory`, `SM_LEGION_HISTORY` source review | Live opcode 55 loads warehouse history rows and sends `SmLegionHistory` with the expected row fields and type ordinal. | Socket-backed connection fixture, repository load capture, decoded server packet payload. | Repository load uses fake rows; direct DB read ordering was not integration-tested. |
| `ProcessPacketAsync_LegionHistoryRewardRequiresBrigadeGeneralLikeJava` | Unit / live packet dispatch | `CM_LEGION_HISTORY.runImpl` source review | Non-brigade-general players get Java's silent return for `REWARD` history. | Socket-backed connection fixture verifies no load and no packet. | Brigade-general reward history send was not separately tested. |

## Validation Decision

```text
- Changed surface: live packet handlers plus game DB repository persistence/read methods.
- Specific behavior/contract: successful legion warehouse kinah deposit/withdrawal writes Java action-name history; CM_LEGION_HISTORY warehouse type loads represented rows and sends SM_LEGION_HISTORY; reward history has Java brigade-general guard.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistoryRewardRequiresBrigadeGeneralLikeJava|FullyQualifiedName~SmLegionHistoryTests" --logger "console;verbosity=minimal"
- Result: passed; 9 tests passed.
- Initial focused attempts: first failed because an accidental edit introduced non-existent dominion case labels; second failed because another fake repository needed the expanded interface; third failed because the fixture did not pass the fake repository into GameServerConnection and one expected name used "Test" instead of the player name "TicketUser". All were fixed before the final pass.
- Focused Java/Maven command: not run; Java source was reviewed unchanged, and no narrow Java fixture exists for this branch in this checkout.
- Broad-validation trigger: live handler and persistence wiring. Broad .NET was skipped after focused validation compiled the affected project and tests and directly exercised both live packet paths; no shared packet primitive or repository transaction helper changed.
- Why this scope is sufficient: the focused command proves the edited live handlers, repository contract consumption, history packet serialization adjacency, and Java guard behavior for the scoped UOW.
```

Repository hygiene:

```powershell
git diff --check
```

Result: passed with only Git CRLF working-copy warnings for touched files.

## Parity Status

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Kinah deposit/withdrawal now mutate state and write history; shared legion warehouse aggregate and DB integration remain gaps. |
| `LegionService.addHistory` | `GameServerConnection.AddLegionHistoryAsync` | Service side effect | Partial | Unit Tested indirectly | Partial Parity | C# persists rows through the repository but does not maintain Java's in-memory legion history cache or age-based trimming. |
| `LegionDAO.insertHistory` / `loadHistory` | `MySqlPlayerEnterWorldRepository.InsertLegionHistoryAsync` / `LoadLegionHistoryAsync` | Repository | Partial | Unit Tested via fake repository | Partial Parity | SQL shape and ordering match Java source; direct MySQL integration was not run. |
| `LegionHistoryAction` | `LegionHistoryActions` / `LegionHistoryRow` | Enum/model projection | Partial | Unit Tested indirectly | Partial Parity | Action ids/types needed by the live paths are represented; unused historical actions 7-10 stay absent like Java source comments. |
| `CM_LEGION_HISTORY.runImpl` | `GameServerConnection.HandleLegionHistoryAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | No-legion and reward/non-BG guards are represented; live warehouse history send is tested; brigade-general reward send and legion activity send are not separately tested. |
| `SM_LEGION_HISTORY.writeImpl` | `SmLegionHistory` | Server packet | Complete | Unit Tested | Partial Parity | Packet had existing focused tests and is now sent from live code; exact Java bytes were not newly captured in this UOW. |

## Known Gaps

- Direct MySQL integration for `legion_history` insert/load was not run.
- C# still does not model Java's shared in-memory `Legion` aggregate, `Legion.addHistory`, or old-entry deletion/trimming.
- Item deposit/withdraw history through Java `LegionService.addWHItemHistory` remains missing for live item move/split/replace paths.
- Exact Java packet bytes for live `CM_LEGION_HISTORY` responses were not captured.
- Brigade-general reward history and legion activity history sends were not separately covered.

## Summary Metrics

- Total Java artifacts discovered in this UOW: 6
- Total artifacts ported or extended in this UOW: 6
- Total artifacts with verified parity: 0
- Total artifacts needing verification or partial parity: 6
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 43%

## Next Runtime UOW Candidates

1. Wire live legion warehouse item history for item move/split/replace paths involving location `3`, matching Java `LegionService.addWHItemHistory` with `ITEM_DEPOSIT` / `ITEM_WITHDRAW`.
2. Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
3. Extend `CM_LEGION_HISTORY` coverage to brigade-general reward and legion activity sends only if paired with a live row-loading or client workflow change in the same UOW.
