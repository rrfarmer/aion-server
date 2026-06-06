# Phase 6 Session 2752 Completion

## Unit of Work

[Phase 6][UOW-2752] Wire live legion member kicks

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x04` now removes a legion member from live connection code instead of remaining parser-only.
- Java source of truth: `CM_LEGION.readImpl` case `0x04`, `CM_LEGION.runImpl` case `0x04`, `LegionService.kickMember`, `LegionRestrictions.canKickPlayer`, `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, `SM_LEGION_LEAVE_MEMBER.writeImpl`, and `SM_SYSTEM_MESSAGE` banish helpers.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, `IPlayerEnterWorldRepository.DeleteLegionMemberAsync`, `LegionHistoryActions.Kick`, `SmLegionLeaveMember`, banish helpers in `SmSystemMessage`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: valid kicks delete the target from `legion_members`, add a `KICK` legion-history row, reset resolved online target legion state, and send Java-shaped `SM_LEGION_LEAVE_MEMBER` from live code; invalid membership, self, brigade-general target, rank, and permission paths send Java message-id errors.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client packet path, mutates live player legion state when the kicked target is online, persists membership removal through the existing DB shape, records runtime legion history, and sends a real server packet from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Runtime Changes

- Routed `CM_LEGION` exOpcode `0x04` to a live kick-member handler.
- Added Java-equivalent kick validation for missing/cross-legion target, self target, brigade-general target, same-or-higher rank, and missing `KICK` permission.
- Added `DeleteLegionMemberAsync` to the enter-world repository and MySQL implementation using the existing `legion_members.player_id` shape.
- Added `LegionHistoryActions.Kick` so successful kicks write Java action `KICK`.
- Added `SmLegionLeaveMember` with Java opcode `112` and Java packet payload order.
- Added banish system-message helpers for ids `1300243`, `1300244`, `1300248`, `1300249`, and `1390241`.
- Added live target reset for resolved online players, matching the `Player.resetLegionMember` side of Java removal.

## Validation Decision

- Changed surface: live connection dispatch, runtime legion state, DB membership deletion, legion-history persistence, server-packet output, and system-message output.
- Specific behavior/contract: Java `LegionService.kickMember` normalizes the target name, checks membership/self/BG/rank/permission, deletes the member row, records `KICK` history, resets online target legion state, and writes `SM_LEGION_LEAVE_MEMBER`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live connection dispatch, membership state mutation, persistence delete method addition, and server-packet output.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed live branch, rank/permission checks, repository delete/history calls, target reset, and packet payload.

## Validation Result

- Focused C# result: Passed, 60 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_KickBranchConsumesJavaEmptyIdAndCharacterName` | Unit | `CM_LEGION.readImpl` case `0x04` | Kick packet consumes the unused `D` and target character name. | Java source review plus parser assertion. | Parser-only evidence is supporting coverage, not the UOW's runtime proof. |
| `HandleInfrastructurePacketAsync_KickMissingMemberSendsNotMyGuildMemberLikeJava` | Unit | `LegionRestrictions.canKickPlayer` | Missing target sends id `1300248` with normalized name and does not delete. | Java source review plus live handler assertion. | DB-gated missing-member lookup was not run. |
| `HandleInfrastructurePacketAsync_KickRejectsSelfLikeJava` | Unit | `LegionRestrictions.canKickPlayer` | Self kick sends id `1300243` and does not delete. | Java source review plus live handler assertion. | None beyond no real client validation. |
| `HandleInfrastructurePacketAsync_KickRejectsBrigadeGeneralTargetLikeJava` | Unit | `LegionRestrictions.canKickPlayer` | BG target sends id `1300249` and does not delete. | Java source review plus live handler assertion. | None beyond no real client validation. |
| `HandleInfrastructurePacketAsync_KickRejectsEqualOrHigherRankLikeJava` | Unit | `LegionRestrictions.canKickPlayer` | Same-or-higher rank sends id `1390241` and does not delete. | Java source review plus rank-id assertion. | None beyond no real client validation. |
| `HandleInfrastructurePacketAsync_KickWithoutPermissionSendsNoRightLikeJava` | Unit | `LegionRestrictions.canKickPlayer` and `LegionPermissionsMask.KICK` | Missing kick permission sends id `1300244` and does not delete. | Java source review plus live handler assertion. | Rank permission masks are fake-backed, not DB-gated. |
| `HandleInfrastructurePacketAsync_KickDeletesMemberAddsHistoryAndSendsLeavePacketLikeJava` | Unit | `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, `SM_LEGION_LEAVE_MEMBER.writeImpl` | Successful kick deletes the target, inserts `KICK` history, and emits packet fields `playerObjId`, `0`, `0`, `msgId`, `name`, `name1`. | Java source review plus repository and packet-byte assertions. | Active connection receives the packet; Java broadcasts to remaining online legion members. |
| `HandleInfrastructurePacketAsync_KickResetsResolvedOnlineTargetLegionStateLikeJava` | Unit | `Player.resetLegionMember` during `removeLegionMember` | Resolved online target has legion id/name/rank/nickname/self-intro cleared after deletion. | Java source review plus live target state assertion. | Target-specific leave message id `1300246` is not sent yet. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` banish helpers | Includes banish error ids `1300243`, `1300244`, `1300248`, `1300249`, and `1390241`. | Java source review plus id/parameter assertions. | Broader system-message surface is not claimed verified. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x04` is now live; several legion subactions remain deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Core kick checks, delete, history, target reset, and active packet are live. Java legion-wide broadcast and target-specific packet are not complete. |
| `com.aionemu.gameserver.services.LegionRestrictions` | `GameServerConnection.HandleLegionKickMemberAsync` | Restriction Logic | Partial | Unit Tested | Partial Parity | Membership/self/BG/rank/permission checks follow the Java order used by `canKickPlayer`. |
| `com.aionemu.gameserver.model.team.legion.LegionPermissionsMask` | `LegionKickPermission` and rank permission fields | Permission Mask | Partial | Unit Tested | Partial Parity | `KICK` mask `0x10` is used; C# helper name is still warehouse-oriented from prior rank/warehouse paths. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository.DeleteLegionMemberAsync` | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing `legion_members.player_id` row delete is used; no DB-gated integration was run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` and payload are covered through live handler output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added banish helpers used by live handler. |

## Summary Metrics

- Total Java artifacts discovered: 9
- Total artifacts ported in this UOW: 7 partial runtime/persistence/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 7
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java broadcasts `SM_LEGION_LEAVE_MEMBER(1300247, targetObjId, kickerName, targetName)` to remaining online legion members, excluding the kicked target; C# currently sends to the active connection only.
- Java sends kicked online targets `SM_LEGION_LEAVE_MEMBER(1300246, 0, legionName)`; C# currently resets resolved target state but does not send the target-specific packet.
- Java also broadcasts `SM_LEGION_UPDATE_TITLE`, may send `SM_ICON_INFO`, performs Conqueror service cleanup, and removes legion bonuses; these adjacent runtime effects remain unported.
- Repository delete/history was not DB-gated in this UOW.
- No real client validation was performed.
