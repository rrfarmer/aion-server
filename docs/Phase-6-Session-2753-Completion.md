# Phase 6 Session 2753 Completion

## Unit of Work

[Phase 6][UOW-2753] Wire live legion leave

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x02` now lets a live player leave their legion instead of remaining parser-only.
- Java source of truth: `CM_LEGION.readImpl` case `0x02`, `CM_LEGION.runImpl` case `0x02`, `LegionService.leaveLegion`, `LegionRestrictions.canLeave`, `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, `LegionWarehouse.getCurrentUser/unsetInUse`, and `SM_LEGION_LEAVE_MEMBER.writeImpl`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, `GameServerConnection.HandleLegionLeaveAsync`, `LegionWarehouseRuntime`, `IPlayerEnterWorldRepository.DeleteLegionMemberAsync`, `SmLegionLeaveMember`, leave helpers in `SmSystemMessage`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: valid leaves delete the active player from `legion_members`, add Java's `KICK` legion-history action, reset the active player's legion fields, and send Java-shaped leave-done packet id `1300241`; invalid brigade-general and current warehouse-user paths send Java message-id errors.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client packet path, mutates live player legion state, persists membership removal through the existing DB shape, consults runtime legion warehouse lock state, records runtime history, and sends real server/system packets from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## C# Runtime Changes

- Routed `CM_LEGION` exOpcode `0x02` to a live leave-legion handler.
- Added Java leave restriction checks for brigade generals and players currently using the legion warehouse.
- Reused `DeleteLegionMemberAsync` for active player membership removal.
- Reused Java action `KICK` for leave history because Java `deleteLegionMemberFromDB` records `LegionHistoryAction.KICK` for both kick and leave removal.
- Reset active player legion fields after successful deletion.
- Sent `SmLegionLeaveMember(1300241, 0, legionName)` to the active player after successful leave.
- Added leave system-message helpers for ids `1300237` and `1300238`.

## Validation Decision

- Changed surface: live connection dispatch, runtime player state, runtime legion warehouse lock state, DB membership deletion, legion-history persistence, server-packet output, and system-message output.
- Specific behavior/contract: Java `LegionService.leaveLegion` rejects brigade generals, rejects current warehouse users, deletes the active member row, records `KICK` history through `deleteLegionMemberFromDB`, resets the active player's legion state, and writes `SM_LEGION_LEAVE_MEMBER` id `1300241` to the leaving player.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live connection dispatch, membership state mutation, persistence delete reuse, runtime warehouse lock state, and server-packet output.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed live branch, Java restriction checks, repository delete/history calls, active player reset, and packet payload.

## Validation Result

- Focused C# result: Passed, 65 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_LeaveBranchConsumesJavaEmptyFields` | Unit | `CM_LEGION.readImpl` case `0x02` | Leave packet consumes Java's empty `D` and `H` fields. | Java source review plus parser assertion. | Parser evidence supports the live UOW but is not standalone runtime proof. |
| `HandleInfrastructurePacketAsync_LeaveRejectsBrigadeGeneralLikeJava` | Unit | `LegionRestrictions.canLeave` | BG leave sends id `1300238`, does not delete, and leaves player state intact. | Java source review plus live handler assertion. | No real client validation. |
| `HandleInfrastructurePacketAsync_LeaveRejectsCurrentWarehouseUserLikeJava` | Unit | `LegionRestrictions.canLeave` and `LegionWarehouse.getCurrentUser` | Current warehouse user sends id `1300237`, does not delete, and keeps the runtime warehouse lock. | Java source review plus live runtime-state assertion. | Does not exercise the dialog close path that releases the lock. |
| `HandleInfrastructurePacketAsync_LeaveDeletesMemberAddsKickHistoryResetsPlayerAndSendsDonePacketLikeJava` | Unit | `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, `deleteLegionMemberFromDB`, `SM_LEGION_LEAVE_MEMBER.writeImpl` | Successful leave deletes active membership, records Java `KICK` history, resets player legion fields, and emits leave-done packet id `1300241`. | Java source review plus repository, state, and packet-byte assertions. | Java broadcast to remaining online legion members is not yet implemented. |
| `HandleInfrastructurePacketAsync_LeaveDeleteFailureDoesNotResetOrSendLikeJavaAbort` | Unit | `LegionMemberDAO.deleteLegionMember` failure aborts removal side effects | Delete failure avoids history, reset, and packet send. | Java source review plus fake repository assertion. | MySQL delete failure was not DB-gated. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` leave helpers | Includes leave restriction ids `1300237` and `1300238`. | Java source review plus id assertions. | Broader system-message surface is not claimed verified. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x02` is now live; several legion subactions remain deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionLeaveAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Core leave restrictions, delete, history, active reset, and active packet are live. Java legion-wide broadcast and title/icon cleanup are not complete. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse` | `LegionWarehouseRuntime` | Runtime State | Partial | Unit Tested | Partial Parity | Current-user guard is used for leave; Java aggregate also owns item/kinah state outside this UOW. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `LegionHistoryActions` | Enum/Utility | Partial | Unit Tested through repository fake | Partial Parity | Java records `KICK` for member removal even on self-leave; C# preserves that behavior. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository.DeleteLegionMemberAsync` | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing delete method is reused; no DB-gated integration was run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Existing opcode `112` packet now covers active self-leave id `1300241`. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added leave restriction helpers used by live handler. |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported in this UOW: 7 partial runtime/persistence/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 7
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java broadcasts `SM_LEGION_LEAVE_MEMBER(1300240, playerObjId, playerName, legionName)` to remaining online legion members, excluding the leaving player; C# currently sends only the active player's `1300241` leave-done packet.
- Java also broadcasts `SM_LEGION_UPDATE_TITLE`, may send `SM_ICON_INFO`, performs Conqueror service cleanup, removes legion bonuses, and unsets warehouse use inside the full legion aggregate; these adjacent runtime effects remain partial or unported.
- Repository delete/history was not DB-gated in this UOW.
- No real client validation was performed.
