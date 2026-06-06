# Phase 6 Session 2750 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2750] Wire live legion nickname edits

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Configuration/GameServerOptions.cs`
- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Model/Legion/LegionMemberSnapshot.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionUpdateNickname.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerOptionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2750-Completion.md`
- `docs/Phase-6-Session-2750-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_NICKNAME.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/configs/main/LegionConfig.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptionsTests" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 32
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
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x0F` is live alongside prior live legion info/notice/self-intro/permission subactions. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionNicknameChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Membership, BG rights, pattern validation, active/offline mutation, offline persistence, and update packet are live; online fanout is missing. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | `Player.LegionNickname` and `LegionMemberSnapshot` | Runtime State | Partial | Unit Tested | Partial Parity | Active snapshot and offline target snapshot are supported for this path. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository` nickname lookup/save methods | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Uses existing `legion_members.nickname`; no DB-gated run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_NICKNAME` | `SmLegionUpdateNickname` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `11` and payload covered. |
| `com.aionemu.gameserver.configs.main.LegionConfig` | `GameServerLegionOptions.NicknamePattern` | Config | Partial | Unit Tested | Partial Parity | Key/default/override are loaded. |

## Known Gaps

- No online legion-member fanout for nickname updates.
- No shared `LegionMember` aggregate, so online non-active target snapshots are not synchronized.
- Repository nickname lookup/save was not DB-gated in this UOW.
- No real client validation was performed.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x06` legion rank appointments.

Runtime Progress Gate:
- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x06` would stop being parser-only and mutate target legion-member rank from live code.
- Java source of truth: `CM_LEGION.readImpl` case `0x06`, `CM_LEGION.runImpl` case `0x06`, `LegionService.appointRank`, `LegionRestrictions.canAppointRank`, `LegionMember.setRank`, `LegionMemberDAO.storeLegionMember`, `SM_LEGION_UPDATE_MEMBER.writeImpl`, and `SM_SYSTEM_MESSAGE` rank-change helpers.
- C# runtime artifact to wire/fix: target member lookup can build on `LegionMemberSnapshot`, add rank-save repository support if needed, add/verify `SmLegionUpdateMember`, add system-message helper ids for rank errors/success, and route `GameServerConnection.HandleLegionAsync` case `0x06`.
- Client-visible/state/persistence effect expected: live rank edits should update active/offline target rank state, persist offline target rank through existing `legion_members.rank`, and send Java-shaped member update or Java error packets.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live client packet path, mutates legion-member rank state, may persist offline member state, and sends real server/system packets from live code.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x06` parses rank id plus member name, enforces Java BG/member/self checks, updates the target rank, persists offline targets, and writes `SM_LEGION_UPDATE_MEMBER` with object id, rank id, class id, level, world id, online flag, last-online, game-server id, message id, and text.
- C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptionsTests" --logger "console;verbosity=minimal" --no-restore`; broaden only if new repository or packet tests require it.
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch, runtime state mutation, persistence method addition, and server-packet output.

Risks to watch:
- Java `LegionRank.values()[rankId]` ordering must be mapped exactly, not inferred from display text.
- `SM_LEGION_UPDATE_MEMBER` needs class, level, world, online, and last-online fields; the current C# snapshot may need extension before the packet can be Java-shaped.
- Java rejects self-rank changes; preserve that guard before mutating state.
- Java broadcasts to all online legion members; active-connection-only behavior must be documented as Partial Parity if fanout remains unavailable.

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
