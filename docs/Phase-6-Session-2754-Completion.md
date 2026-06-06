# Phase 6 Session 2754 Completion

## Unit of Work

[Phase 6][UOW-2754] Send kicked target legion leave packet

## Runtime Progress Gate

- Deferred/live behavior advanced: the live `CM_LEGION` exOpcode `0x04` kick path now sends Java's direct leave notification to the kicked online target instead of only resetting the target's legion state.
- Java source of truth: `LegionService.removeLegionMember` online-player branch, specifically `PacketSendUtility.sendPacket(player, new SM_LEGION_LEAVE_MEMBER(kickerName != null ? 1300246 : 1300241, 0, legion.getName()))`, plus `SM_LEGION_LEAVE_MEMBER.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionKickMemberAsync`, `IGameClientConnectionRegistry.SendPacketToPlayerAsync`, existing `SmLegionLeaveMember`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: when the kicked target is online and resolvable, C# sends a real `SmLegionLeaveMember` packet id `1300246` to that player's connection before clearing their live legion fields.
- Why this is not preview-only/test-only/documentation-only: this UOW sends a missing Java server packet from live kick code and changes client-visible behavior for online kicked players.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`

## C# Runtime Changes

- Added target-directed `SmLegionLeaveMember(1300246, 0, player.LegionName)` send when `HandleLegionKickMemberAsync` resolves the kicked member online.
- Preserved existing successful-kick behavior: delete membership, insert `KICK` history, reset resolved target state, and send the active-connection partial fanout packet id `1300247`.
- Extended the focused registry fake to capture direct `SendPacketToPlayerAsync` deliveries for byte-level packet assertions.

## Validation Decision

- Changed surface: live connection dispatch, target-directed packet send through connection registry, server-packet output, and live target state mutation.
- Specific behavior/contract: Java sends kicked online targets `SM_LEGION_LEAVE_MEMBER(1300246, 0, legionName)` while the successful kick path still deletes, records history, resets target state, and notifies remaining legion members separately.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live packet send through connection registry and existing live kick state mutation.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed target-directed send, existing kick state mutation, and packet payload.

## Validation Result

- Focused C# result: Passed, 65 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_KickResetsResolvedOnlineTargetAndSendsDirectDonePacketLikeJava` | Unit | `LegionService.removeLegionMember` online-player branch and `SM_LEGION_LEAVE_MEMBER.writeImpl` | Successful online kick resets target state, sends active partial fanout id `1300247`, and sends target-directed id `1300246` with `legionName`. | Java source review plus live handler, registry-send, and packet-byte assertions. | Java title/icon/conqueror cleanup and legion-wide broadcast remain incomplete. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Online kicked targets now receive direct leave packet id `1300246`; Java still performs title/icon/conqueror/bonus effects and legion-wide fanout. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` payload is now covered for direct kicked-target id `1300246` and prior kick/leave ids. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x04` remains live; this UOW closes one online-target packet branch. |

## Summary Metrics

- Total Java artifacts discovered: 2
- Total artifacts ported in this UOW: 3 partial runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java broadcasts `SM_LEGION_LEAVE_MEMBER(1300247, targetObjId, kickerName, targetName)` to all remaining online legion members; C# still sends that packet only to the active kicker connection.
- Java broadcasts `SM_LEGION_UPDATE_TITLE`, may send `SM_ICON_INFO`, performs Conqueror service cleanup, and removes legion bonuses; these adjacent runtime effects remain unported.
- No real client validation was performed.
