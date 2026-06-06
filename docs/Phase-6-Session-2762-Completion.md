# Phase 6 Session 2762 Completion

## Unit of Work

[Phase 6][UOW-2762] Send legion dominion ranking packet

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION_DOMINION_REQUEST_RANKING` is no longer deferred for valid Stonespear ids `1..6`; it now sends a real ranking server packet.
- Java source of truth: `CM_LEGION_DOMINION_REQUEST_RANKING.runImpl`, `SM_LEGION_DOMINION_RANK.writeImpl`, `LegionDominionLocation.getLegionRanking(false)`, `LegionDominionDAO.loadParticipants`, and `LegionDominionParticipantInfo`.
- C# runtime artifact wired/fixed: `GameServerConnection` ranking branch, `SmLegionDominionRank`, and `IPlayerEnterWorldRepository.LoadLegionDominionParticipantsAsync`.
- Client-visible/state/persistence effect changed: requesting a valid dominion id sends `SM_LEGION_DOMINION_RANK` with location id, requesting legion rank, top participant count, and point/time/date/name rows loaded from the existing database shape.
- Why this is not preview-only/test-only/documentation-only: it wires a previously deferred client packet path and sends a real server packet from live connection code using runtime persisted data.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION_DOMINION_REQUEST_RANKING.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_DOMINION_RANK.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionLocation.java`
- `game-server/src/com/aionemu/gameserver/model/legionDominion/LegionDominionParticipantInfo.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDominionDAO.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added `SmLegionDominionRank` opcode `302` with Java field order: `locationId(D)`, `rank(C)`, `count(H)`, then participant `points(D)`, `survivedTime(D)`, `date(Q)`, `legionName(S)`.
- Implemented Java ranking order: points descending, participated timestamp ascending.
- Preserved Java's top-25 replacement rule: if the requesting legion is ranked outside the top 25, replace the last top row with the requesting legion.
- Added `LegionDominionParticipantRow` and a MySQL repository load from `legion_dominion_participants`, joined to `legions.name` with Java's `"NOT AVAILABLE"` fallback.
- Wired `GameServerConnection` to handle valid `CmLegionDominionRequestRanking` ids and send the new packet; invalid ids remain no-op like Java.

## Validation Decision

- Changed surface: live packet dispatch, server packet serialization, and a repository read over an existing Java schema.
- Specific behavior/contract: valid dominion ranking requests load persisted participant rows, compute Java rank/top rows, and send `SM_LEGION_DOMINION_RANK`; invalid ids do not load or send.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionDominion|FullyQualifiedName~SmLegionDominionRank|FullyQualifiedName~CmLegionTests" --logger "console;verbosity=minimal" --no-restore
```

- Guarded repository command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~LoadLegionDominionParticipantsAsync_LoadsJavaRowsWithLegionNameFallback" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java test fixture exists for `SM_LEGION_DOMINION_RANK`.
- Broad-validation trigger: live client packet dispatch and server packet send were enabled.
- Broad .NET decision: skipped after focused validation because the filtered commands compiled the affected project and directly exercised the edited live handler, packet, and repository contract.
- Why this scope is sufficient: no shared packet primitive, crypto, scheduler, schema, or broad world-state primitive changed.

## Validation Result

- Focused packet/handler C# result: Passed, 67 total, 0 failed, 0 skipped.
- Guarded repository C# result: Passed, 1 total, 0 failed, 0 skipped. With `AION_GAMESERVER_DB_INTEGRATION` unset, the test confirms the guarded path compiles and returns immediately.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `TryCreatePacket_RegistersJavaLegionDominionRankingOpcodeAsInGameOnly` | Unit | Java packet opcode table and C# factory registration | Client opcode `29` is registered only for in-game state. | Factory assertion. | Does not prove live send by itself. |
| `ReadFrom_LegionDominionRankingConsumesJavaStonespearId` | Unit | `CM_LEGION_DOMINION_REQUEST_RANKING.readImpl` | Reads one `D` as the Stonespear id. | Java source review plus parser assertion. | None for the read shape. |
| `SmLegionDominionRankTests.OpcodeIs302` | Unit | `ServerPacketsOpcodes.addPacketOpcode(302, SM_LEGION_DOMINION_RANK.class)` | Server opcode value. | Java source review plus assertion. | None for opcode. |
| `SmLegionDominionRankTests.WritesJavaPayloadSortedByPointsThenDate` | Unit | `SM_LEGION_DOMINION_RANK.writeImpl`, `LegionDominionLocation.getLegionRanking(false)` | Field order and ranking sort. | Packet serialization assertion. | Date is asserted as epoch seconds from supplied rows, not Java runtime output. |
| `SmLegionDominionRankTests.ReplacesLastTopRowWithRequesterWhenRequesterRankIsOutsideTopTwentyFive` | Unit | `SM_LEGION_DOMINION_RANK` constructor | Java top-25 replacement behavior. | Packet serialization assertion. | None for the scoped replacement branch. |
| `HandleInfrastructurePacketAsync_DominionRankingRejectsInvalidIdsLikeJava` | Unit | `CM_LEGION_DOMINION_REQUEST_RANKING.runImpl` id guard | Invalid ids do not hit storage or send packets. | Live handler assertion. | Only ids `0` and `7` sampled. |
| `HandleInfrastructurePacketAsync_DominionRankingLoadsRowsAndSendsRankLikeJava` | Unit | `CM_LEGION_DOMINION_REQUEST_RANKING.runImpl`, `SM_LEGION_DOMINION_RANK` | Live handler loads participants and sends sorted rank packet. | Live handler assertion. | Uses test repository rows, not a real client. |
| `LoadLegionDominionParticipantsAsync_LoadsJavaRowsWithLegionNameFallback_WhenEnabled` | Guarded Integration | `LegionDominionDAO.loadParticipants`, `LegionDominionParticipantInfo.getLegionName` | Database load filters by dominion id and applies legion-name fallback. | Guarded DB test added. | Not executed against MySQL unless `AION_GAMESERVER_DB_INTEGRATION=1`; timestamp exactness remains a timezone verification risk. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION_DOMINION_REQUEST_RANKING` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegionDominionRequestRanking` / `GameServerConnection` | Client Packet / Live Handler | Complete for request/read/dispatch | Unit Tested | Partial Parity | Valid id guard and live send are ported. Java service-backed in-memory location model is approximated through persisted repository rows for ranking until the full dominion service is ported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_DOMINION_RANK` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionDominionRank` | Server Packet | Complete for current fields | Unit Tested | Partial Parity | Field order, rank, sort, and top-25 replacement are tested. No Java golden packet fixture or real-client verification yet. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionLocation.getLegionRanking` | `SmLegionDominionRank` ranking projection | Runtime Projection | Partial | Unit Tested | Partial Parity | Ports `removeNonEligibleLegions=false` order only. Territory winner eligibility threshold path remains unported. |
| `com.aionemu.gameserver.model.legionDominion.LegionDominionParticipantInfo` | `LegionDominionParticipantRow` / `LegionDominionRankEntry` | DTO / Packet Row | Partial | Unit Tested | Partial Parity | Includes legion id/name, points, time, and epoch seconds. Java in-memory legion-name lookup is represented by a repository join plus `"NOT AVAILABLE"` fallback. |
| `com.aionemu.gameserver.dao.LegionDominionDAO.loadParticipants` | `MySqlPlayerEnterWorldRepository.LoadLegionDominionParticipantsAsync` | Repository | Partial | Guarded Integration | Needs Verification | Filters by `legion_dominion_id` and maps points/time/date/name. Exact JDBC/MySQL timestamp timezone parity remains unverified. |

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported or advanced in this UOW: 5 runtime/repository/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 2 (full dominion service map/weekly scoring, exact DB timestamp timezone parity)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Full `LegionDominionService` remains unported: in-memory locations, weekly calculation, reward mail, occupation updates, rift opening, and ranking broadcasts after instance finish are still missing.
- `SM_LEGION_DOMINION_LOC_INFO` is not ported.
- Exact MySQL/JDBC timestamp timezone equivalence for `participated_date` is not runtime-compared.
- No Java golden packet fixture was generated for `SM_LEGION_DOMINION_RANK`.
- No real-client validation was performed for the ranking window.
