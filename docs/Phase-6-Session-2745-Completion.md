# Phase 6 Session 2745 Completion

## Unit of Work

[Phase 6][UOW-2745] Send live legion notice messages

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x07` now sends Java-equivalent live legion notice/no-notice system messages.
- Java source of truth: `CM_LEGION.readImpl`, `CM_LEGION.runImpl` case `0x07`, `LegionDAO.loadAnnouncement`, `Legion.Announcement`, and `SM_SYSTEM_MESSAGE.STR_MSG_NOSET_GUILD_NOTICE` / `STR_GUILD_NOTICE`.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, `Player` legion announcement fields, `MySqlPlayerEnterWorldRepository.LoadPlayerAsync`, `SmSystemMessage`, and `SmLegionInfo.FromPlayer`.
- Client-visible/state/runtime-loading effect changed: loaded players now carry the latest `legion_announcement_list` row, `/gnotice`/notice refresh sends a real system message from live code, and `SM_LEGION_INFO` can include the loaded announcement data.
- Why this is not preview-only/test-only/documentation-only: this wires a deferred live client packet branch, loads Java schema data into runtime player state, and sends real server packets from live connection code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionDAO.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added `Player.LegionAnnouncement` and `Player.LegionAnnouncementEpochSeconds`.
- Hydrated the latest legion announcement from `legion_announcement_list` with Java-equivalent `ORDER BY date DESC LIMIT 1` scalar subqueries during `LoadPlayerAsync`.
- Added `SmSystemMessage.MsgNoSetGuildNotice()` and `SmSystemMessage.GuildNotice(...)` helpers with Java message ids and parameters.
- Wired `CM_LEGION` exOpcode `0x07` to send no-notice or notice messages for active legion members.
- Updated `SmLegionInfo.FromPlayer` so legion info refresh can include loaded announcement message/time.

## Validation Decision

- Changed surface: live connection dispatch, player runtime state, repository hydration, one server packet helper surface, and existing legion-info packet population.
- Specific behavior/contract: Java `CM_LEGION` exOpcode `0x07` consumes empty `D/H` fields, requires an active legion member, sends `STR_MSG_NOSET_GUILD_NOTICE` when no announcement is loaded, and sends `STR_GUILD_NOTICE(message, unixTime)` when an announcement exists. Java `LegionDAO.loadAnnouncement` reads the latest `legion_announcement_list` row by descending date.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java test exists for this packet/DAO path in the repo.
- Broad-validation trigger: live connection dispatch and repository hydration changed.
- Broad .NET decision: skipped after focused validation because the tests compile the game-server project, directly exercise the live connection branch and packet IDs/parameters, and compile the DB-gated schema hydration path. No shared packet primitive or broad persistence contract was changed.
- Why this scope is sufficient: the focused tests prove the changed live behavior and the targeted repository test compiles against the exact schema columns; residual risk is MySQL runtime timezone/DB execution, which is documented below.

## Validation Result

- Passed: 13
- Failed: 0
- Skipped: 0
- Existing nullable/analyzer warnings remain outside this UOW.
- The DB integration test body is gated behind `AION_GAMESERVER_DB_INTEGRATION=1`; it compiled in this run but did not execute against live MySQL.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `ReadFrom_ShowNoticeConsumesJavaEmptyFields` | Unit | `CM_LEGION.readImpl` cases `0x07`/`0x08` | C# parser consumes the Java empty `D/H` fields for exOpcode `0x07`. | Java source review plus parser assertion. | Does not execute socket parser. |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` helpers | No-notice id `1390127` and notice id `1400019` with message, Unix seconds, mode `2`. | Java source review plus parameter assertions. | Does not serialize full packet bytes. |
| `SmLegionInfo_FromPlayerWritesLoadedAnnouncementLikeJava` | Unit | `SM_LEGION_INFO.writeAnnouncements` | Loaded announcement appears in `SM_LEGION_INFO` with Unix time and stop marker. | Java source review plus byte-level reader assertions. | Other legion aggregate fields still default. |
| `HandleInfrastructurePacketAsync_ShowNoticeSendsNoNoticeMessageWhenAnnouncementMissingLikeJava` | Unit | `CM_LEGION.runImpl` case `0x07` | Live connection sends no-notice system message for a legion member without announcement state. | Java source review plus live packet observer. | Does not cover non-member branch separately from existing no-packet test. |
| `HandleInfrastructurePacketAsync_ShowNoticeSendsLoadedAnnouncementLikeJava` | Unit | `CM_LEGION.runImpl` case `0x07` | Live connection sends notice message and timestamp parameters from runtime player state. | Java source review plus live packet observer. | Timestamp source is injected runtime state. |
| `LoadPlayerAsync_HydratesLatestLegionAnnouncementAgainstJavaSchema_WhenEnabled` | DB-gated Integration | `LegionDAO.loadAnnouncement` and `legion_announcement_list` schema | Repository selects the newest announcement row for loaded player runtime state. | Java source review plus DB-gated test compiled. | Not executed against live MySQL in this session; timezone conversion remains Needs Verification. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `Aion.GameServer.Network.Aion.ClientPackets.CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcodes `0x07` and `0x08` are live; other mutation/service subactions remain deferred. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added notice/no-notice helpers with Java ids and normal string parameters. Broader helper surface is not claimed verified. |
| `com.aionemu.gameserver.dao.LegionDAO` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository` | Repository | Partial | DB-gated Integration Test Compiled | Partial Parity | Latest announcement load shape ported into player hydration. DB-gated test was not run against MySQL; timestamp timezone behavior remains Needs Verification. |
| `com.aionemu.gameserver.model.team.legion.Legion.Announcement` | `Player.LegionAnnouncement` / `Player.LegionAnnouncementEpochSeconds` | Runtime State | Partial | Unit Tested / DB-gated Integration Test Compiled | Partial Parity | Runtime state carries message and Unix seconds on player. C# still lacks shared `Legion` aggregate and announcement mutation/fanout. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionInfo` | Server Packet | Partial | Unit Tested | Partial Parity | Announcement fields now use loaded player state; ranking, contribution, and dominion fields still default. |

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported in this UOW: 5 partial runtime artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 5
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- C# still lacks Java's shared `Legion` aggregate; announcement state is currently attached to the loaded player snapshot.
- Announcement mutation (`CM_LEGION` exOpcode `0x09`) is not yet wired, so this UOW only displays existing loaded announcement state.
- DB-gated latest-announcement hydration was not executed against live MySQL in this session.
- `DateTime`/timezone interpretation for MySQL `timestamp` columns remains Needs Verification.
- Real client rendering was not validated.
