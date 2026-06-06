# Phase 6 Session 2765 Completion

## Unit of Work

[Phase 6][UOW-2765] Broadcast legion rank changes

## Runtime Progress Gate

- Deferred/live behavior advanced: live `CM_LEGION` exOpcode `0x06` rank changes now update online target state and broadcast the Java member update packet to online same-legion members instead of only sending one packet to the requester.
- Java source of truth: `CM_LEGION.readImpl`/`runImpl` exOpcode `0x06`, `LegionService.appointRank`, `LegionRestrictions.canAppointRank`, `LegionMember.setRank`, `LegionMemberDAO.storeLegionMember`, and `PacketSendUtility.broadcastToLegion(legion, new SM_LEGION_UPDATE_MEMBER(...))`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionRankChangeAsync`, `ResolveLegionMemberByNameAsync`, live `Player.LegionRank`, existing offline `SaveLegionMemberRankAsync`, and same-legion online fanout using `SmLegionUpdateMember`.
- Client-visible/state/persistence effect changed: online target rank changes mutate the target `Player.LegionRank`; offline target rank changes still persist through the existing database shape; online same-legion players receive the real rank-change `SM_LEGION_UPDATE_MEMBER` packet.
- Why this is not preview-only/test-only/documentation-only: it mutates live player legion state and sends real server packets from a live client handler.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionRank.java`

## C# Runtime Changes

- `ResolveLegionMemberByNameAsync` now checks the online connection registry before falling back to the repository.
- Online rank-change targets now have their live `Player.LegionRank` mutated before packet serialization.
- Offline targets retain Java behavior: the rank is saved through `SaveLegionMemberRankAsync`.
- Rank-change packets now broadcast to the active requester and online same-legion registry members using `SmLegionUpdateMember`.
- Outsider online players are filtered out of the rank-change fanout.

## Validation Decision

- Changed surface: live client handler, online player state, offline rank persistence, and same-legion packet fanout.
- Specific behavior/contract: Java `LegionService.appointRank` sets the target rank, stores offline targets, and broadcasts `SM_LEGION_UPDATE_MEMBER(legionMember, msgId, name)` to the legion.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~SmLegionUpdateMember" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for `LegionService.appointRank`.
- Broad-validation trigger: live state mutation and connection fanout were enabled.
- Broad .NET decision: skipped after focused validation because the filtered command compiled the affected project and directly exercised the edited live handler, offline persistence call, online target state mutation, and packet serialization contract. No shared packet primitive, crypto, scheduler, schema, or common persistence abstraction changed.
- Why this scope is sufficient: the change is isolated to the existing `CM_LEGION 0x06` handler and existing `SM_LEGION_UPDATE_MEMBER` packet serializer.

## Validation Result

- Focused C# result: Passed, 72 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_ChangeRankPersistsOfflineMemberAndSendsUpdateLikeJava` | Unit | `LegionService.appointRank` | Offline target rank persists and rank-change packet is sent to the requester plus same-legion online bystander, excluding outsiders. | Live handler, repository-call, and packet payload assertions. | Uses test registry, not a real client. |
| `HandleInfrastructurePacketAsync_ChangeRankOnlineMemberMutatesStateAndBroadcastsWithoutPersistenceLikeJava` | Unit | `LegionService.appointRank` | Online target rank mutates in memory, avoids repository persistence, and broadcasts the update to same-legion online players. | Live handler, state mutation, and packet fanout assertions. | Does not generate Java golden packet bytes. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` exOpcode `0x06` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` / `GameServerConnection.HandleLegionRankChangeAsync` | Client Packet / Live Handler | Partial | Unit Tested | Partial Parity | Read shape, restrictions, online mutation, offline persistence, and fanout are covered. No real-client verification. |
| `com.aionemu.gameserver.services.LegionService.appointRank` | `GameServerConnection.HandleLegionRankChangeAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Ports set-rank, offline save, message id selection, and legion broadcast. Full Java `Legion` object membership model is approximated through loaded snapshots and online registry players. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `SmLegionUpdateMember` | Server Packet | Partial | Unit Tested | Partial Parity | Existing serializer reused and asserted from live rank-change handler. No Java golden fixture. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.storeLegionMember` | `IPlayerEnterWorldRepository.SaveLegionMemberRankAsync` | Repository Boundary | Partial | Unit Tested via fake repository | Partial Parity | Scoped to rank column persistence; Java DAO stores nickname/selfintro/challenge score too. |
| `com.aionemu.gameserver.model.team.legion.LegionRank` | `Aion.GameServer.Model.Legion.LegionRanks` | Model / Constants | Partial | Unit Tested via handler packets | Partial Parity | Rank id/name mapping consumed by live rank-change packets. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported or advanced in this UOW: 5 runtime/repository/packet/model artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 1 (no Java golden packet fixture for rank-change update packet)
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- No Java golden packet fixture was generated for the rank-change `SM_LEGION_UPDATE_MEMBER`.
- Full Java `Legion` in-memory membership is still approximated by loaded snapshots and online registry players.
- No real-client validation was performed for the rank-change broadcast.
