# Phase 6 Session 2756 Completion

## Unit of Work

[Phase 6][UOW-2756] Send legion title clear on removal

## Runtime Progress Gate

- Deferred/live behavior advanced: live legion kick and self-leave now emit Java's post-removal `SM_LEGION_UPDATE_TITLE` title-clear packet instead of leaving that client-visible update absent.
- Java source of truth: `LegionService.removeLegionMember` online-player branch with `PacketSendUtility.broadcastPacket(player, new SM_LEGION_UPDATE_TITLE(player.getObjectId(), 0, "", legionMember.getRank()), true)`, `SM_LEGION_UPDATE_TITLE.writeImpl`, and server opcode `114`.
- C# runtime artifact wired/fixed: new `SmLegionUpdateTitle`, `GameServerConnection.HandleLegionKickMemberAsync`, `GameServerConnection.HandleLegionLeaveAsync`, `BroadcastLegionUpdateTitleAsync`, `IGameClientConnectionRegistry.BroadcastToVisiblePlayersAsync`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: resolved online kicked targets and active self-leavers now broadcast a real title-clear packet with fields `playerObjectId`, `0`, `""`, and removed member rank id before their live legion fields are reset.
- Why this is not preview-only/test-only/documentation-only: this UOW ports and sends a missing Java server packet from live legion removal code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_TITLE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Runtime Changes

- Added `SmLegionUpdateTitle` with opcode `114` and Java payload order.
- Added `BroadcastLegionUpdateTitleAsync` to send title-clear updates through visible-player fanout with `includeSourcePlayer: true`.
- Kick now sends title-clear update for resolved online kicked targets before `ResetLegionMember`.
- Self-leave now preserves the active member rank, sends leave-done id `1300241`, sends title-clear update, then resets legion state.

## Validation Decision

- Changed surface: live server-packet output, visible-player broadcast through connection registry, and existing live membership state mutation.
- Specific behavior/contract: `SM_LEGION_UPDATE_TITLE` opcode `114` writes `D playerObjectId`, `D legionId`, `S legionName`, `C rankId`; live removal sends the title-clear variant with legion id `0`, empty name, removed player object id, and removed rank id.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live server-packet output and existing live membership state mutation.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise packet payload, visible broadcast usage, kick removal, and self-leave removal.

## Validation Result

- Focused C# result: Passed, 65 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_KickResetsResolvedOnlineTargetAndSendsDirectDonePacketLikeJava` | Unit | `LegionService.removeLegionMember` online-player branch and `SM_LEGION_UPDATE_TITLE.writeImpl` | Online kick sends title-clear visible broadcast with source target object id, `includeSourcePlayer: true`, legion id `0`, empty legion name, and target rank id. | Java source review plus live handler and packet-byte assertions. | Exact Java known-list recipient set is approximated through C# visible-player registry. |
| `HandleInfrastructurePacketAsync_LeaveDeletesMemberAddsKickHistoryResetsPlayerAndSendsDonePacketLikeJava` | Unit | `LegionService.removeLegionMember` online-player branch and `SM_LEGION_UPDATE_TITLE.writeImpl` | Self-leave sends title-clear visible broadcast with source active player object id, `includeSourcePlayer: true`, legion id `0`, empty legion name, and preserved rank id. | Java source review plus live handler and packet-byte assertions. | Exact Java known-list recipient set is approximated through C# visible-player registry. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync`, `HandleLegionLeaveAsync`, and `BroadcastLegionUpdateTitleAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Title-clear packet is now live for kick/self-leave removals; icon/conqueror/bonus cleanup remains incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_TITLE` | `SmLegionUpdateTitle` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `114` and title-clear payload are covered; non-clear legion-title updates are not claimed fully verified by this UOW. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Kick and leave paths now send title-clear visible broadcast packets. |

## Summary Metrics

- Total Java artifacts discovered: 3
- Total artifacts ported in this UOW: 3 partial runtime/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 3
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java may send `SM_ICON_INFO(1, false)` when the legion has a bonus; C# lacks reliable legion-bonus state in this path.
- Java performs `ConquerorAndProtectorService.onLeaveLegion(player)` and `legion.removeBonus()`; these effects remain unported.
- Exact Java visible-recipient known-list semantics depend on runtime world state; tests verify C# calls the visible-player broadcast hook and packet payload.
- Repository delete/history remains fake-tested, not DB-gated.
- No real client validation was performed.
