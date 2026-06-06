# Phase 6 Session 2752 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2752] Wire live legion member kicks

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionHistory.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionLeaveMember.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2752-Completion.md`
- `docs/Phase-6-Session-2752-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/services/LegionRestrictions.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionPermissionsMask.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 60
- Failed: 0
- Skipped: 0
- Existing warnings only.

Other validation:
- `git diff --check` passed with line-ending warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet/service path.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x04` is now live alongside prior live legion subactions. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Kick checks, delete, history, resolved target reset, and leave packet are live; online fanout is incomplete. |
| `com.aionemu.gameserver.services.LegionRestrictions` | `GameServerConnection.HandleLegionKickMemberAsync` | Restriction Logic | Partial | Unit Tested | Partial Parity | Membership/self/BG/rank/permission checks follow the Java `canKickPlayer` path. |
| `com.aionemu.gameserver.model.team.legion.LegionPermissionsMask` | `LegionKickPermission` and rank permission fields | Permission Mask | Partial | Unit Tested | Partial Parity | `KICK` mask `0x10` is wired through existing rank permission fields. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository.DeleteLegionMemberAsync` | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing `legion_members.player_id` is deleted; no DB-gated run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` and payload covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added banish error helpers. |

## Known Gaps

- No online legion-member fanout for kick notifications; C# sends only to the active connection.
- No target-specific `SM_LEGION_LEAVE_MEMBER(1300246, 0, legionName)` packet for the kicked online target yet.
- No `SM_LEGION_UPDATE_TITLE`, `SM_ICON_INFO`, Conqueror cleanup, or legion bonus removal.
- Repository delete/history was not DB-gated in this UOW.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x02` live legion leave.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x02` would stop being parser-only and allow a player to leave their legion from live connection code.
- Java source of truth: `CM_LEGION.runImpl` case `0x02`, `LegionService.leaveLegion`, `LegionRestrictions.canLeave`, `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, and `SM_LEGION_LEAVE_MEMBER.writeImpl`.
- C# runtime artifact to wire/fix: route `GameServerConnection.HandleLegionAsync` case `0x02`, reuse `DeleteLegionMemberAsync`, reuse `SmLegionLeaveMember`, add missing leave system-message helpers, and reset the active player's legion state.
- Client-visible/state/persistence effect expected: valid leave should delete the active player from `legion_members`, add leave history if Java does, reset active player legion fields, and send Java-shaped leave/removal packets from live code.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates live player legion state, persists membership deletion, and sends real server packets.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x02` enforces Java leave restrictions, deletes the active membership row, resets the active player, and writes `SM_LEGION_LEAVE_MEMBER` in Java field order.
- C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`; broaden only if new repository/packet helpers affect other focused tests.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch, membership state mutation, persistence delete reuse, and server-packet output.

Risks to watch:
- Java BG leave and legion-warehouse-in-use restrictions must be checked before deletion.
- Java leave may use different messages for active player versus remaining members; preserve payload/message ids conservatively and document any fanout gap.
- Reuse the UOW-2752 delete and reset helpers, but do not hide leave-specific Java differences behind the kick path.

Safe alternative runtime candidates:
- Add real legion-wide fanout for rank/kick packets if `IGameClientConnectionRegistry` can safely target online players by `LegionId`.
- Wire the kicked-target packet `SM_LEGION_LEAVE_MEMBER(1300246, 0, legionName)` if a reliable player-object-id send path is available.
- Continue adjacent `CM_LEGION` subactions only when they satisfy the Runtime Progress Gate with a Java source path and client-visible state/packet effect.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
