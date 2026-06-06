# Phase 6 Session 2745 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed Unit of Work

[Phase 6][UOW-2745] Send live legion notice messages

Commit:
- Pending at handoff creation.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/Player.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmLegionInfo.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CmLegionTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldRepositoryDatabaseIntegrationTests.cs`
- `docs/Phase-6-Session-2745-Completion.md`
- `docs/Phase-6-Session-2745-Handoff.md`

## Java Artifacts Touched

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/sql/aion_gs.sql`

## Tests Run

Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

Result:
- Passed: 13
- Failed: 0
- Skipped: 0
- Existing warnings only.

Java/Maven:
- Not run. Java source was unchanged and no narrow Java test exists for this packet/DAO path.

DB integration:
- `LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema_WhenEnabled` compiled but did not execute against live MySQL because `AION_GAMESERVER_DB_INTEGRATION` was not set to `1`.

## Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcodes `0x07` and `0x08` are live; other subactions remain deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added `STR_MSG_NOSET_GUILD_NOTICE` and `STR_GUILD_NOTICE` equivalents. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Latest announcement load shape ported; MySQL execution and timezone remain unverified this session. |
| `com.aionemu.gameserver.model.team.legion.Legion.Announcement` | `Player.LegionAnnouncement` / `Player.LegionAnnouncementEpochSeconds` | Runtime State | Partial | Unit Tested / DB-gated Integration Test Compiled | Partial Parity | Snapshot state exists on player; shared legion aggregate and mutation/fanout remain missing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Announcement fields now use loaded player state; ranking/contribution/dominion still default. |

## Known Gaps

- `CM_LEGION` exOpcode `0x09` change announcement is not live yet.
- C# still does not have Java's shared `Legion` aggregate; announcement state is per loaded player snapshot.
- MySQL timestamp timezone behavior for announcement dates still needs live DB verification.
- No real client validation was performed for notice display.

## Next Runtime UOW Candidate

Candidate:
- Wire `CM_LEGION` exOpcode `0x09` to change/clear the active legion announcement.

Runtime Progress Gate:
- Deferred/live behavior advanced: a live client announcement edit would mutate runtime legion announcement state and persist to `legion_announcement_list`.
- Java source of truth: `CM_LEGION.runImpl` case `0x09`, `LegionService.changeAnnouncement`, `LegionDAO.saveAnnouncement`, `SM_SYSTEM_MESSAGE.STR_GUILD_WRITE_NOTICE_DONT_HAVE_RIGHT`, `STR_GUILD_WRITE_NOTICE_DONE`, and `STR_MSG_CLEAR_GUILD_NOTICE`.
- C# runtime artifact to wire/fix: `GameServerConnection.HandleLegionAsync`, `Player` announcement fields, repository save/delete for `legion_announcement_list`, `SmSystemMessage` helpers, and likely `SmLegionEdit.Announcement` or `SmLegionInfo` response behavior for the active player.
- Client-visible/state/persistence effect expected: authorized players can set or clear the announcement; player runtime state changes, the existing DB schema is updated, and success/clear/no-right packets are sent.
- Why this is not preview-only/test-only/documentation-only: it wires a deferred live packet path, mutates live player/legion runtime state, persists through the existing database shape, and sends real server packets.

Focused validation recipe:
- Specific behavior/contract: exOpcode `0x09` enforces Java EDIT permission mask `0x200`; unauthorized sends id `1300276`; non-empty messages truncate to 256 chars, update runtime state, persist, and send id `1300277` plus announcement edit packet if safe; empty messages clear runtime/persistence and send id `1390128`.
- C# command: `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore`
- Java/Maven: not expected unless a narrow Java fixture is added or discovered.
- Broad-validation trigger: live connection dispatch and repository persistence mutation.
- Broad .NET decision: start focused; do not run unfiltered project/solution validation unless focused evidence exposes wider repository or packet risk.

Safe alternative candidates:
- Fill more live `SmLegionInfo` fields from existing `legions` columns, such as contribution points and dominion fields, if repository hydration can be extended without inventing a shared aggregate.
- Continue with another `CM_LEGION` subaction only when it has Java source, active C# runtime state, and a client-visible packet/state/persistence effect.

## Context Needed Next

Start with:
- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- this handoff
- latest completion document
- current `git status --short`

Do not update `PHASE-6-PROGRESS.md` unless explicitly asked.
