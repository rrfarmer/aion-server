# Phase 6 Session 2755 Completion

## Unit of Work

[Phase 6][UOW-2755] Fan out legion leave member packets

## Runtime Progress Gate

- Deferred/live behavior advanced: live legion kick and self-leave now send Java's `broadcastToLegion` leave-member notifications to other online same-legion players instead of only the active connection.
- Java source of truth: `LegionService.removeLegionMember` `PacketSendUtility.broadcastToLegion` branches for kick id `1300247` and self-leave id `1300240`, plus `SM_LEGION_LEAVE_MEMBER.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionKickMemberAsync`, `GameServerConnection.HandleLegionLeaveAsync`, new `BroadcastLegionLeaveMemberAsync`, `IGameClientConnectionRegistry.ForEachOnlinePlayer`, `IGameClientConnectionRegistry.SendPacketToPlayerAsync`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: online same-legion players other than the removed member now receive real `SmLegionLeaveMember` fanout packets for both kick and leave.
- Why this is not preview-only/test-only/documentation-only: this UOW sends missing Java server packets from live legion removal code and changes client-visible behavior for other online legion members.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`

## C# Runtime Changes

- Added `BroadcastLegionLeaveMemberAsync` to enumerate online players, filter by `LegionId`, exclude the removed member, and send Java-shaped leave-member packets.
- Kick now sends `SmLegionLeaveMember(1300247, targetObjId, kickerName, targetName)` to the active kicker and other online same-legion players, excluding the kicked target.
- Self-leave now sends `SmLegionLeaveMember(1300240, playerObjId, playerName, legionName)` to online same-legion players, excluding the leaving player.
- Preserved direct kicked-target packet id `1300246` and active leave-done packet id `1300241`.

## Validation Decision

- Changed surface: live connection dispatch, same-legion recipient filtering, connection-registry fanout, server-packet output, and existing live membership state mutation.
- Specific behavior/contract: Java `broadcastToLegion` sends kick id `1300247` or leave id `1300240` to remaining online legion members while excluding the removed member object id.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live fanout through connection registry and existing live membership state mutation.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise same-legion inclusion, cross-legion exclusion, removed-member exclusion, active packet send, and packet payload.

## Validation Result

- Focused C# result: Passed, 65 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_KickResetsResolvedOnlineTargetAndSendsDirectDonePacketLikeJava` | Unit | `LegionService.removeLegionMember` kick broadcast and online-player branches | Kick sends active id `1300247`, same-legion bystander id `1300247`, target direct id `1300246`, and excludes cross-legion players. | Java source review plus live handler, registry fanout, and packet-byte assertions. | Java title/icon/conqueror cleanup remains incomplete. |
| `HandleInfrastructurePacketAsync_LeaveDeletesMemberAddsKickHistoryResetsPlayerAndSendsDonePacketLikeJava` | Unit | `LegionService.removeLegionMember` self-leave broadcast branch | Leave sends same-legion bystander id `1300240`, excludes the leaving player and cross-legion players, and still sends active leave-done id `1300241`. | Java source review plus live handler, registry fanout, state, and packet-byte assertions. | Java title/icon/conqueror cleanup remains incomplete. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync`, `HandleLegionLeaveAsync`, and `BroadcastLegionLeaveMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | `broadcastToLegion` leave-member packet fanout is now live for kick and self-leave. Java title/icon/conqueror/bonus effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` payload covered for kick fanout, self-leave fanout, direct kicked target, and active leave done. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcodes `0x04` and `0x02` now perform broader Java packet fanout. |

## Summary Metrics

- Total Java artifacts discovered: 2
- Total artifacts ported in this UOW: 3 partial runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java also broadcasts `SM_LEGION_UPDATE_TITLE`, may send `SM_ICON_INFO`, performs Conqueror service cleanup, and removes legion bonuses; these adjacent runtime effects remain unported.
- Fanout uses C# connection-registry online snapshots, not Java's full `Legion` aggregate.
- Repository delete/history remains fake-tested, not DB-gated.
- No real client validation was performed.
