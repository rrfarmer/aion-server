# Phase 6 Session 2747 Completion

## Unit of Work

[Phase 6][UOW-2747] Hydrate legion info contribution fields

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_LEGION` exOpcode `0x08` now sends more complete Java-equivalent `SM_LEGION_INFO` data instead of defaulting contribution and dominion fields to zero.
- Java source of truth: `CM_LEGION.runImpl` case `0x08`, `SM_LEGION_INFO.writeImpl`, `LegionDAO.loadLegion`, and `Legion` getters for contribution and dominions.
- C# runtime artifact wired/fixed: `Player` legion runtime fields, `MySqlPlayerEnterWorldRepository.LoadPlayerAsync`, and `SmLegionInfo.FromPlayer`.
- Client-visible/state/persistence effect changed: player enter-world hydration restores `legions.contribution_points`, `occupied_legion_dominion`, `last_legion_dominion`, and `current_legion_dominion`, and the live legion info refresh packet writes those values.
- Why this is not preview-only/test-only/documentation-only: this UOW loads existing Java-schema database state into live C# player state used by a real server packet sent from a live client packet path.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_INFO.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added `Player.LegionContributionPoints`, `LegionOccupiedLegionDominion`, `LegionLastLegionDominion`, and `LegionCurrentLegionDominion`.
- Extended `MySqlPlayerEnterWorldRepository.LoadPlayerAsync` to select and map the matching `legions` columns.
- Updated `SmLegionInfo.FromPlayer` to write the loaded contribution and dominion values while keeping ranking position at `0` until the Abyss ranking cache path is ported.
- Extended packet and DB-gated repository tests around the live refresh/hydration contract.

## Validation Decision

- Changed surface: live packet population and database-backed runtime player state hydration.
- Specific behavior/contract: Java `LegionDAO.loadLegion` reads contribution/dominion columns and `SM_LEGION_INFO.writeImpl` writes those values after disband time.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLegionLevelForTradeListFilteringAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this DAO/server-packet path.
- Broad-validation trigger: repository hydration and live server packet population.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed hydration and packet contract.
- Why this scope is sufficient: the focused tests assert packet byte order from `SmLegionInfo.FromPlayer`, the live refresh branch sends those values, and the DB-gated repository fixture compiles against the Java schema columns.

## Validation Result

- Focused C# result: Passed, 18 total, 0 failed, 0 skipped.
- Existing nullable/analyzer warnings remain outside this UOW.
- DB-gated tests compiled but did not execute against live MySQL because `AION_GAMESERVER_DB_INTEGRATION=1` was not set.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmLegionInfo_FromPlayerWritesLoadedRuntimeFieldsLikeJava` | Unit | `SM_LEGION_INFO.writeImpl` | `FromPlayer` writes loaded contribution and dominion values in Java packet order. | Java source review plus packet byte assertions. | Ranking remains defaulted to zero. |
| `HandleInfrastructurePacketAsync_RefreshInfoSendsActivePlayerLegionInfoLikeJava` | Unit | `CM_LEGION.runImpl` case `0x08` and `SM_LEGION_INFO.writeImpl` | Live refresh-info path sends a packet containing loaded contribution and dominion values. | Java source review plus live handler packet assertion. | No real client validation. |
| `LoadPlayerAsync_HydratesLegionLevelForTradeListFilteringAgainstJavaSchema_WhenEnabled` | DB-gated Integration | `LegionDAO.loadLegion` | Repository maps `legions` level, disband time, contribution points, and dominion ids. | Java source review plus DB-gated test compilation. | Live MySQL execution not run this session. |
| `LoadPlayerAsync_DefaultsLegionFactsWhenNoLegionMemberAgainstJavaSchema_WhenEnabled` | DB-gated Integration | Java no-legion null/default behavior | No-legion players retain zero contribution and dominion fields. | Java source review plus DB-gated test compilation. | Live MySQL execution not run this session. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x08` now sends loaded contribution/dominion fields through live `SM_LEGION_INFO`; other subactions remain deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Packet field order for loaded legion info is covered; Abyss ranking position remains defaulted to zero. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerAsync` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Selected Java `legions` columns hydrate active player state; broader DAO methods are not ported. |
| `com.aionemu.gameserver.model.team.legion.Legion` | `Aion.GameServer.Model.GameObjects.Player` legion snapshot fields | Runtime State | Partial | Unit Tested | Partial Parity | C# still uses loaded player snapshot fields rather than Java's shared `Legion` aggregate. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported in this UOW: 4 partial runtime artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- `AbyssRankingCache.getRankingListPosition(legion)` remains defaulted to `0` in C# `SM_LEGION_INFO`.
- C# still lacks Java's shared `Legion` aggregate; values are hydrated into the active player snapshot.
- DB-gated hydration was not executed against live MySQL in this session.
- No online legion-member fanout or real client validation was performed.
