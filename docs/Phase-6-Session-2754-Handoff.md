# Phase 6 Session 2754 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2754] Send kicked target legion leave packet

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2754-Completion.md`
- `docs/Phase-6-Session-2754-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_LEAVE_MEMBER.java`

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
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Direct kicked-target packet id `1300246` is live; Java title/icon/conqueror/bonus effects and legion-wide fanout remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` direct target payload covered. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Kick path remains live with improved online target packet behavior. |

## Known Gaps

- No legion-wide broadcast for kick id `1300247` or self-leave id `1300240`; current C# still sends only to the active connection.
- No `SM_LEGION_UPDATE_TITLE`, `SM_ICON_INFO`, Conqueror cleanup, or legion bonus removal.
- Repository delete/history remains fake-tested, not DB-gated.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Add legion-wide live fanout for kick and/or self-leave `SM_LEGION_LEAVE_MEMBER` notifications to online legion members.

Runtime Progress Gate:
- Deferred/live behavior advanced: Java's `PacketSendUtility.broadcastToLegion` leave-member notifications would be sent to online same-legion players instead of only the active connection.
- Java source of truth: `LegionService.removeLegionMember` `broadcastToLegion` branches for kick id `1300247` and self-leave id `1300240`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionKickMemberAsync`, `GameServerConnection.HandleLegionLeaveAsync`, `IGameClientConnectionRegistry.ForEachOnlinePlayer`, `IGameClientConnectionRegistry.SendPacketToPlayerAsync`, and focused `CmLegionTests`.
- Client-visible/state/persistence effect expected: online same-legion players other than the removed member should receive Java-shaped `SmLegionLeaveMember` broadcast packets after kick/leave.
- Why this is not preview-only/test-only/documentation-only: it sends missing Java server packets from live legion removal code and changes client-visible behavior for other online legion members.

Focused validation recipe:
- Specific behavior/contract: live kick broadcasts id `1300247` to online same-legion members excluding the kicked target; live leave broadcasts id `1300240` to online same-legion members excluding the leaving player.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live fanout through connection registry and existing live membership state mutation; start focused and document residual title/icon gaps.

Risks to watch:
- Preserve Java exclusion of the removed member object id.
- Avoid sending legion fanout to cross-legion online players.
- Decide whether the active kicker/leaver should receive the fanout packet by matching Java `broadcastToLegion` semantics after the removed member is deleted.
- Keep `1300246` direct kicked-target packet separate from `1300247` remaining-member fanout.

Safe alternative runtime candidates:
- Add `SM_LEGION_UPDATE_TITLE` for kick/leave if its Java payload is ported first in the same runtime UOW.
- Add `SM_ICON_INFO` removal if the packet exists or is ported and a reliable legion-bonus state is available.
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
