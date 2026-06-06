# Phase 6 Session 2723 Handoff

## Completed UOW

[Phase 6] UOW-2723: Persist and send live legion warehouse history.

## Runtime Progress Gate Result

```text
- Deferred/live behavior advanced: successful CM_LEGION_WH_KINAH action 0/1 records KINAH_WITHDRAW/KINAH_DEPOSIT, and CM_LEGION_HISTORY now sends represented history rows from live code.
- Java source/runtime path: CM_LEGION_WH_KINAH.runImpl -> LegionService.addHistory -> LegionDAO.insertHistory; CM_LEGION_HISTORY.runImpl -> legion.getHistory(type) -> SM_LEGION_HISTORY.
- C# runtime artifact wired: GameServerConnection kinah/history handlers, IPlayerEnterWorldRepository/MySqlPlayerEnterWorldRepository history methods, LegionHistoryActions/LegionHistoryRow, SmLegionHistory packet projection.
- Client-visible/state/persistence effect: successful warehouse kinah mutation writes durable legion_history rows, and opcode 55 can send a real SM_LEGION_HISTORY response.
- Why this is runtime progress: live handlers now persist runtime legion state and send a real server packet.
```

## Commit

`[Phase 6][UOW-2723] Persist legion warehouse history`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ClientPackets/CmLegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2723-Completion.md`
- `docs/Phase-6-Session-2723-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_WH_KINAH.java`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_HISTORY.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_HISTORY.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`

## C# Artifacts Touched

- `Aion.GameServer.Model.Legion.LegionHistoryActions`
- `Aion.GameServer.Model.Legion.LegionHistoryRow`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository`
- `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository`
- `Aion.GameServer.Network.Aion.ClientPackets.CmLegionHistory`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahDepositAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionWarehouseKinahWithdrawalAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleLegionHistoryAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmLegionHistory`

## Validation

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahDepositMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionWarehouseKinahWithdrawalMutatesLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistoryRewardRequiresBrigadeGeneralLikeJava|FullyQualifiedName~SmLegionHistoryTests" --logger "console;verbosity=minimal"
```

Result:

- Passed: 9
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

- Not run. Broad-validation trigger was live handler and persistence wiring, but the focused command built the affected project and directly exercised the edited live branches, repository contract use, and packet projection. No shared packet primitive or common repository helper changed.

## Conservative Parity Status

- `CM_LEGION_WH_KINAH` now has partial runtime parity for kinah deposit/withdrawal including Java history side effects.
- `CM_LEGION_HISTORY` now has partial runtime parity for represented history rows, including Java's no-legion and reward/non-brigade-general silent guards.
- Full legion history parity is not claimed because C# still lacks Java's shared `Legion` aggregate/history cache, direct DB integration evidence, exact Java packet bytes, and old-entry trimming.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_LEGION_WH_KINAH.runImpl` | `GameServerConnection.HandleLegionWarehouseKinahAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Kinah deposit/withdrawal now mutate state and write history; shared legion warehouse aggregate remains missing. |
| `LegionService.addHistory` | `GameServerConnection.AddLegionHistoryAsync` | Service side effect | Partial | Unit Tested indirectly | Partial Parity | Persists rows but does not maintain Java in-memory cache/trimming. |
| `LegionDAO.insertHistory` / `loadHistory` | `MySqlPlayerEnterWorldRepository.InsertLegionHistoryAsync` / `LoadLegionHistoryAsync` | Repository | Partial | Unit Tested via fake repository | Partial Parity | SQL shape and ordering follow Java source; direct MySQL integration not run. |
| `LegionHistoryAction` | `LegionHistoryActions` / `LegionHistoryRow` | Enum/model projection | Partial | Unit Tested indirectly | Partial Parity | Action ids and type ordinals used by live paths are represented. |
| `CM_LEGION_HISTORY.runImpl` | `GameServerConnection.HandleLegionHistoryAsync` | Live packet handler | Partial | Unit Tested | Partial Parity | Warehouse send and reward/non-BG guard are tested; reward/BG and activity sends remain untested. |
| `SM_LEGION_HISTORY.writeImpl` | `SmLegionHistory` | Server packet | Complete | Unit Tested | Partial Parity | Existing packet tests plus live send coverage; exact Java bytes not newly captured. |

## Known Gaps / Watchouts

- Direct DB integration for `legion_history` was not run.
- Java `Legion.addHistory` in-memory cache and age-based deletion/trimming are not modeled.
- Live item move/split/replace paths involving legion warehouse do not yet write `ITEM_DEPOSIT` / `ITEM_WITHDRAW` history.
- Exact Java bytes for live history packets were not captured.
- `CM_LEGION_HISTORY` reward/BG and activity sends need focused coverage when those paths matter.

## Next Recommended Runtime UOW

Recommended candidate: wire live legion warehouse item history for item move/split/replace paths involving location `3`.

Runtime progress gate for that candidate:

```text
- Deferred/live behavior to advance: successful item deposit/withdrawal into/from legion warehouse should persist ITEM_DEPOSIT/ITEM_WITHDRAW history instead of only mutating item rows and packets.
- Java source/runtime path: LegionService.addWHItemHistory(Player, itemId, count, sourceStorage, destStorage) -> LegionService.addHistory -> LegionDAO.insertHistory.
- C# runtime artifact likely involved: GameServerConnection move/split/replace handlers for location 3, existing InsertLegionHistoryAsync, item source/destination storage resolution.
- Client-visible/state/persistence effect expected: successful live item movement involving legion warehouse writes durable legion_history rows with `itemId:count` descriptions, making them visible through the now-live CM_LEGION_HISTORY warehouse path.
- Why this is runtime progress: it persists runtime legion state from existing live inventory handlers and feeds a real client-visible history packet path.
```

Suggested discovery:

```powershell
rg -n "addWHItemHistory|ITEM_DEPOSIT|ITEM_WITHDRAW|HandleMoveItemAsync|HandleSplitItemAsync|HandleReplaceItemAsync|Location = 3|LegionWarehouse" game-server/src/com/aionemu/gameserver dotnetConversion/src/Aion.GameServer dotnetConversion/tests/Aion.GameServer.Tests
```

Suggested focused validation:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~ProcessPacketAsync_CubeSourceMovesItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeSourceSplitsItemToLegionWarehouseOwnerLikeJava|FullyQualifiedName~ProcessPacketAsync_CubeAndLegionWarehouseReplaceSwitchesOwnersLikeJava|FullyQualifiedName~ProcessPacketAsync_LegionHistorySendsWarehouseRowsLikeJava" --logger "console;verbosity=minimal"
```

Narrow or adjust the filter to the exact edited move/split/replace tests after discovery. Java/Maven is not expected unless a narrow Java fixture is found. Broad-validation trigger: live handler/persistence wiring, but start with focused inventory/history dispatch tests.

## Other Safe Runtime Candidates

- Load legion warehouse expansion data into the runtime model used by `SM_CUBE_UPDATE`.
- Extend `CM_LEGION_HISTORY` coverage to brigade-general reward and legion activity sends if a live data source row shape is needed by a client workflow.

## Context Needed By Next Session

- Startup must reread `docs/csharp-port.md`, `docs/orchestration-rules.md`, `docs/parity-verification.md`, this handoff, the matching completion doc, and `git status --short`.
- Latest completed commits before this UOW:
  - `bd4c0b1df [Phase 6][UOW-2722] Withdraw kinah from legion warehouse`
  - `73e42ae32 [Phase 6][UOW-2721] Deposit kinah into legion warehouse`
  - `67e44a33e [Phase 6][UOW-2720] Split cube items into legion warehouse`
- Ignore preview, readiness, metadata, and test-only recommendations unless explicitly requested or immediately tied to same-session runtime behavior.
