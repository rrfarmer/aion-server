# Phase 6 Session 2751 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2751] Wire live legion rank appointments

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionMemberSnapshot.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionRanks.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionUpdateMember.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2751-Completion.md`
- `docs/Phase-6-Session-2751-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionRank.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 56
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x06` is now live alongside prior live legion subactions. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionRankChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Rank checks, offline persistence, and update packet are live; online fanout is missing. |
| `com.aionemu.gameserver.model.team.legion.LegionRank` | `LegionRanks` | Enum/Utility | Partial | Unit Tested | Partial Parity | Rank id mapping follows Java enum order; invalid-id exception behavior differs defensively. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | `LegionMemberSnapshot` and `Player.LegionRank` | Runtime State | Partial | Unit Tested | Partial Parity | Snapshot supports packet fields for rank changes; shared aggregate missing. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository` member lookup/rank save methods | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing `legion_members.rank` is used; no DB-gated run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `SmLegionUpdateMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `113` and payload covered. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added rank error helpers. |

## Known Gaps

- No online legion-member fanout for rank updates.
- No shared `LegionMember` aggregate, so online non-active target snapshots are not synchronized.
- Invalid rank ids are ignored defensively rather than throwing like Java enum indexing.
- Repository rank lookup/save was not DB-gated in this UOW.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x04` legion member kick.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x04` would stop being parser-only and remove target legion membership from live code.
- Java source of truth: `CM_LEGION.readImpl` case `0x04`, `CM_LEGION.runImpl` case `0x04`, `LegionService.kickMember`, `LegionRestrictions.canKickPlayer`, `LegionService.removeLegionMember`, `LegionMemberDAO.deleteLegionMember`, `SM_LEGION_LEAVE_MEMBER.writeImpl`, `SM_LEGION_UPDATE_TITLE`, `SM_ICON_INFO`, and `SM_SYSTEM_MESSAGE` banish helpers.
- C# runtime artifact to wire/fix: target member lookup already exists, add/delete legion-member repository method, add `SmLegionLeaveMember` opcode `112`, add needed banish system-message helpers, and route `GameServerConnection.HandleLegionAsync` case `0x04`.
- Client-visible/state/persistence effect expected: valid kick should delete the target from `legion_members`, send Java-shaped leave/member removal packets, and reset live target legion state if an online target can be resolved safely.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates/persists legion membership state, and sends real server/system packets from live code.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x04` parses empty `D` plus member name, enforces Java membership/self/BG/rank/permission checks, deletes the target legion member row, and writes `SM_LEGION_LEAVE_MEMBER` as `playerObjId`, `0`, `0`, `msgId`, `name`, `name1`.
- C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore`; broaden only if new packet/repository tests require it.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch, membership state mutation, persistence delete method addition, and server-packet output.

Risks to watch:
- Java permission check uses `LegionPermissionsMask.KICK` and rank ordering; preserve the exact rank-id comparison.
- Java sends different packets to remaining members versus the kicked online member; active-connection-only behavior must be documented as Partial Parity if fanout/target resolution remains unavailable.
- Online target reset requires more than deleting the row; only mutate a live target if the C# connection registry can resolve that player safely.

Safe alternative candidates:
- Add live online legion fanout only if the C# connection registry can enumerate players by `LegionId` safely and the packet path already has runtime state.
- Continue adjacent `CM_LEGION` subactions only when they satisfy the Runtime Progress Gate with a Java source path and a client-visible state/packet effect.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
