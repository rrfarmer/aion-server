# Phase 6 Session 2753 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2753] Wire live legion leave

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2753-Completion.md`
- `docs/Phase-6-Session-2753-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionHistoryAction.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionWarehouse.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 65
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x02` is now live alongside prior live legion subactions. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionLeaveAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Leave restrictions, delete, history, active reset, and active packet are live; online fanout is incomplete. |
| `com.aionemu.gameserver.model.team.legion.LegionWarehouse` | `LegionWarehouseRuntime` | Runtime State | Partial | Unit Tested | Partial Parity | Current-user guard is live for leave; full Java aggregate semantics remain broader. |
| `com.aionemu.gameserver.model.team.legion.LegionHistoryAction` | `LegionHistoryActions` | Enum/Utility | Partial | Unit Tested through fake calls | Partial Parity | Removal history uses Java `KICK` action for self-leave too. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository.DeleteLegionMemberAsync` | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing `legion_members.player_id` delete is reused; no DB-gated run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` and leave-done payload covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added leave restriction helpers. |

## Known Gaps

- No online legion-member fanout for self-leave notification id `1300240`.
- No target-specific kick notification id `1300246` for kicked online targets from UOW-2752.
- No `SM_LEGION_UPDATE_TITLE`, `SM_ICON_INFO`, Conqueror cleanup, or legion bonus removal.
- Repository delete/history was not DB-gated in this UOW.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Send the kicked online target's direct leave notification packet from live `CM_LEGION` exOpcode `0x04`.

Runtime Progress Gate:
- Deferred/live behavior advanced: the existing live kick path would send Java's target-specific `SM_LEGION_LEAVE_MEMBER(1300246, 0, legionName)` to the kicked online player instead of only resetting state.
- Java source of truth: `LegionService.removeLegionMember` branch `if (player != null)` with `PacketSendUtility.sendPacket(player, new SM_LEGION_LEAVE_MEMBER(kickerName != null ? 1300246 : 1300241, 0, legion.getName()))`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionKickMemberAsync`, `IGameClientConnectionRegistry.SendPacketToPlayerAsync`, existing `SmLegionLeaveMember`, and focused `CmLegionTests` registry fake.
- Client-visible/state/persistence effect expected: when a kicked target is online and resolvable by object id, C# should send a real `SmLegionLeaveMember` packet id `1300246` to that player's connection while preserving the current delete/history/reset behavior.
- Why this is not preview-only/test-only/documentation-only: it sends a missing Java server packet from live kick code and changes client-visible behavior for online kicked players.

Focused validation recipe:
- Specific behavior/contract: live kick of an online target deletes membership, resets target legion state, and sends target-directed `SM_LEGION_LEAVE_MEMBER` fields `0`, `0`, `0`, `1300246`, `legionName`, `""`.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live packet send through connection registry and existing live kick state mutation; start focused and document any residual fanout gap.

Risks to watch:
- Capture the legion name before `ResetLegionMember` clears target state.
- Do not send target packet for offline/unresolved targets.
- Preserve the current active-connection `1300247` partial fanout behavior until a broader legion-wide broadcast UOW is selected.

Safe alternative runtime candidates:
- Add real legion-wide fanout for self-leave id `1300240` and kick id `1300247` if `IGameClientConnectionRegistry` can safely enumerate online players by `LegionId`.
- Add `SM_LEGION_UPDATE_TITLE` for leave/kick if its Java payload is ported first in the same runtime UOW.
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
