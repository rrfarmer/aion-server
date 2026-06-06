# Phase 6 Session 2751 Completion

## Unit of Work

[Phase 6][UOW-2751] Wire live legion rank appointments

## Runtime Progress Gate

- Deferred/live behavior advanced: `CM_LEGION` exOpcode `0x06` now changes target legion-member rank from live connection code instead of remaining parser-only.
- Java source of truth: `CM_LEGION.readImpl` case `0x06`, `CM_LEGION.runImpl` case `0x06`, `LegionService.appointRank`, `LegionRestrictions.canAppointRank`, `LegionRank.values()[rankId]`, `LegionMember.setRank`, `LegionMemberDAO.storeLegionMember`, `SM_LEGION_UPDATE_MEMBER.writeImpl`, and `SM_SYSTEM_MESSAGE` rank-change helpers.
- C# runtime artifact wired/fixed: `GameServerConnection.HandleLegionAsync`, `LegionRanks.FromRankId`, expanded `LegionMemberSnapshot`, `IPlayerEnterWorldRepository.SaveLegionMemberRankAsync`, `SmLegionUpdateMember`, `SmSystemMessage` rank helpers, and focused `CmLegionTests`.
- Client-visible/state/persistence effect changed: valid rank edits persist offline target rank through `legion_members.rank` and send Java-shaped `SM_LEGION_UPDATE_MEMBER`; invalid rights, missing target, cross-legion target, and self-target paths send Java message-id errors.
- Why this is not preview-only/test-only/documentation-only: this UOW wires a deferred live client packet path, mutates/persists legion-member rank state, expands runtime target-member loading for packet fields, and sends a real server packet from live code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEGION.java`
- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionRank.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_UPDATE_MEMBER.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java`
- `game-server/src/com/aionemu/gameserver/network/aion/ServerPacketsOpcodes.java`

## C# Runtime Changes

- Routed `CM_LEGION` exOpcode `0x06` to a live rank-change handler.
- Added Java ordinal-to-rank mapping for `LegionRank.values()[rankId]`.
- Expanded `LegionMemberSnapshot` with class, EXP, world, and last-online fields needed by `SM_LEGION_UPDATE_MEMBER`.
- Changed member lookup to load by normalized player name first, then apply Java membership checks in handlers.
- Added offline rank persistence through `legion_members.rank`.
- Added `SmLegionUpdateMember` with Java opcode `113` and the Java packet payload shape.
- Added rank-change system message helpers for ids `1300262`, `1300263`, `1300264`, and `1300265`.

## Validation Decision

- Changed surface: live connection dispatch, runtime rank state, offline DB persistence, target-member loading, server-packet output, and rank enum mapping.
- Specific behavior/contract: Java `LegionService.appointRank` checks brigade-general rights, missing/cross-legion/self targets, maps rank id through `LegionRank.values()`, stores offline members, and broadcasts `SM_LEGION_UPDATE_MEMBER`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~GameServerOptionsTests|FullyQualifiedName~LegionRanksTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this packet/service path.
- Broad-validation trigger: live connection dispatch, runtime state mutation, persistence method addition, and server-packet output.
- Broad .NET decision: skipped after focused validation because the selected tests compile the game-server project and directly exercise the changed live branch, rank checks, persistence call, packet payload, and rank mapping.

## Validation Result

- Focused C# result: Passed, 56 total, 0 failed, 0 skipped.
- `git diff --check`: passed; line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Java/Maven was not run for the reason above.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleInfrastructurePacketAsync_ChangeRankWithoutBrigadeGeneralSendsNoRightLikeJava` | Unit | `LegionRestrictions.canAppointRank` | Non-BG path sends id `1300262` and does not load or persist target rank. | Java source review plus live handler assertion. | No real client validation. |
| `HandleInfrastructurePacketAsync_ChangeRankMissingMemberSendsNoUserLikeJava` | Unit | `LegionRestrictions.canAppointRank` | Missing target sends id `1300264`. | Java source review plus live handler assertion. | Does not prove DB lookup returns null for every deleted-player state. |
| `HandleInfrastructurePacketAsync_ChangeRankOtherLegionMemberSendsNotMyGuildMemberLikeJava` | Unit | `LegionRestrictions.canAppointRank` | Cross-legion target sends id `1300265`. | Java source review plus live handler assertion. | Depends on fake lookup; DB-gated integration was not run. |
| `HandleInfrastructurePacketAsync_ChangeRankRejectsSelfLikeJava` | Unit | `LegionRestrictions.canAppointRank` | Self-rank change sends id `1300263` and does not persist. | Java source review plus live handler assertion. | No broader BG transfer path covered. |
| `HandleInfrastructurePacketAsync_ChangeRankPersistsOfflineMemberAndSendsUpdateLikeJava` | Unit | `LegionService.appointRank` and `SM_LEGION_UPDATE_MEMBER.writeImpl` | Offline target rank persists and packet writes object/rank/class/level/world/offline/last-online/server/message/text. | Java source review plus repository call and packet-byte assertions. | DB-gated integration was not run. |
| `HandleInfrastructurePacketAsync_ChangeRankOnlineMemberSendsUpdateWithoutPersistenceLikeJava` | Unit | `LegionService.appointRank` | Online target sends update without offline persistence. | Java source review plus live handler assertion. | Target online player snapshot is not mutated because no shared `LegionMember` aggregate exists. |

## Tests Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmSystemMessage_LegionNoticeHelpersUseJavaIdsAndParameters` | Unit | `SM_SYSTEM_MESSAGE` rank helpers | Includes rank error ids `1300262`, `1300263`, `1300264`, and `1300265`. | Java source review plus id/parameter assertions. | Broader system-message surface is not claimed verified. |
| `LegionRanksTests` | Unit | `LegionRank` enum | Covers Java ordinal-to-name and name-to-ordinal mapping for rank ids. | Java source review plus direct mapping assertions. | Invalid C# reverse lookup returns null instead of throwing like Java indexing. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEGION` | `CmLegion` and `GameServerConnection.HandleLegionAsync` | Client Packet / Runtime Dispatch | Partial | Unit Tested | Partial Parity | ExOpcode `0x06` is now live; several legion subactions remain deferred. |
| `com.aionemu.gameserver.services.LegionService` | `GameServerConnection.HandleLegionRankChangeAsync` | Service Logic | Partial | Unit Tested | Partial Parity | Core rank appointment checks, offline persistence, and active send are live. Java broadcasts to all online legion members; C# sends to active connection only. |
| `com.aionemu.gameserver.model.team.legion.LegionRank` | `LegionRanks` | Enum/Utility | Partial | Unit Tested | Partial Parity | Forward rank ids and reverse rank-id mapping are covered for live rank changes; invalid Java exception behavior is defensively ignored in C#. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | `LegionMemberSnapshot` and `Player.LegionRank` | Runtime State | Partial | Unit Tested | Partial Parity | Snapshot now carries packet fields needed for rank changes; no shared online aggregate. |
| `com.aionemu.gameserver.dao.LegionMemberDAO` | `MySqlPlayerEnterWorldRepository.LoadLegionMemberByNameAsync` and `SaveLegionMemberRankAsync` | Persistence | Partial | Unit Tested through fake calls | Partial Parity | Existing `legion_members.rank` is used; no DB-gated integration was run. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER` | `SmLegionUpdateMember` | Server Packet | Partial | Unit Tested | Partial Parity | Opcode `113` and payload are covered through live handler output. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE` | `SmSystemMessage` | Server Packet Helper | Partial | Unit Tested | Partial Parity | Added rank error helpers used by live handler. |

## Summary Metrics

- Total Java artifacts discovered: 8
- Total artifacts ported in this UOW: 7 partial runtime/persistence/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 7
- Total blocked artifacts: 0
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java broadcasts rank updates to all online legion members; C# currently sends only to the active connection.
- C# lacks Java's shared `LegionMember` aggregate, so online non-active target snapshots are not mutated.
- Invalid rank ids are ignored defensively in C# instead of throwing from `LegionRank.values()[rankId]`; this is documented as partial parity.
- Repository rank lookup/save was not DB-gated in this UOW.
- No real client validation was performed.
