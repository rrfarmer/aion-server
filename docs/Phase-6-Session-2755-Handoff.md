# Phase 6 Session 2755 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2755] Fan out legion leave member packets

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `docs/Phase-6-Session-2755-Completion.md`
- `docs/Phase-6-Session-2755-Handoff.md`

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
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionKickMemberAsync`, `HandleLegionLeaveAsync`, and `BroadcastLegionLeaveMemberAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Java leave-member broadcast packet fanout is live for kick/self-leave; title/icon/conqueror/bonus effects remain incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_LEAVE_MEMBER` | `SmLegionLeaveMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `112` fanout payloads covered. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | Kick and leave paths now send remaining-member fanout packets. |

## Known Gaps

- No `SM_LEGION_UPDATE_TITLE` on kick/leave yet.
- No `SM_ICON_INFO`, Conqueror cleanup, or legion bonus removal.
- Fanout uses C# online registry snapshots instead of Java's full `Legion` aggregate.
- Repository delete/history remains fake-tested, not DB-gated.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Port and send `SM_LEGION_UPDATE_TITLE` for live kick/self-leave removal.

Runtime Progress Gate:
- Deferred/live behavior advanced: Java's post-removal title update packet would be emitted from live kick/leave code instead of remaining absent.
- Java source of truth: `LegionService.removeLegionMember` online-player branch with `PacketSendUtility.broadcastPacket(player, new SM_LEGION_UPDATE_TITLE(player.getObjectId(), 0, "", legionMember.getRank()), true)`, `SM_LEGION_UPDATE_TITLE.writeImpl`, and server opcode `114`.
- C# runtime artifact to wire/fix: add `SmLegionUpdateTitle`, send it from `HandleLegionKickMemberAsync` and `HandleLegionLeaveAsync` when the removed player is online/active, and extend focused `CmLegionTests`.
- Client-visible/state/persistence effect expected: nearby/visible clients or currently available registry recipients receive Java-shaped title-clear packet fields `playerObjectId`, `0`, `""`, `rankId` after legion removal.
- Why this is not preview-only/test-only/documentation-only: it ports and sends a missing Java server packet from live legion removal code.

Focused validation recipe:
- Specific behavior/contract: `SmLegionUpdateTitle` opcode `114` writes `D playerObjectId`, `D legionId`, `S legionName`, `C rankId`; live kick/leave removal sends the title-clear variant with legion id `0`, empty name, and removed member rank id.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live server-packet output and existing live membership state mutation; start focused and document residual visibility semantics.

Risks to watch:
- Java uses `broadcastPacket(player, ..., true)` for visible players plus self; C# may need to use the current connection plus registry/visibility approximation and document partial parity if exact known-list fanout is unavailable.
- Preserve removed member rank before reset clears active/target state.
- Keep this packet separate from `SM_ICON_INFO` and Conqueror cleanup unless those are ported in the same live UOW.

Safe alternative runtime candidates:
- Add `SM_ICON_INFO(1, false)` removal if the packet exists or can be ported and a reliable legion-bonus state is available.
- Add Conqueror/Protector leave-legion cleanup only if the runtime service exists or can be ported directly from Java.
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
