# Phase 6 Session 2777 Completion

## Unit of Work

[Phase 6][UOW-2777] Load persisted legion roster for invite member list

## Runtime Progress Gate

- Deferred/live behavior advanced: live legion invite acceptance sent `SM_LEGION_MEMBERLIST` from the online connection registry only, missing offline persisted members that Java includes from the legion member cache.
- Java source of truth: `LegionService.updateLegionMemberList`, `Legion.getMembers`, `LegionMemberDAO.loadLegionMembers`, `LegionMemberDAO.loadLegionMember`, `PlayerDAO.loadPlayerCommonData`, and `SM_LEGION_MEMBERLIST.writeImpl`.
- C# runtime artifact wired/fixed: `IPlayerEnterWorldRepository.LoadLegionMembersAsync`, `MySqlPlayerEnterWorldRepository.LoadLegionMembersAsync`, `GameServerConnection.SendLegionInviteMemberListAsync`, and the live invite-acceptance packet fanout.
- Client-visible/state/persistence effect changed: the accepted player now receives roster rows loaded from the persisted `legion_members` and `players` tables, including offline members, with online registry data overlaid for currently online members and the accepted player excluded.
- Why this is not preview-only/test-only/documentation-only: it reads runtime database state and sends real `SM_LEGION_MEMBERLIST` packets from live invite-acceptance code.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/services/LegionService.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/Legion.java`
- `game-server/src/com/aionemu/gameserver/model/team/legion/LegionMember.java`
- `game-server/src/com/aionemu/gameserver/dao/LegionMemberDAO.java`
- `game-server/src/com/aionemu/gameserver/dao/PlayerDAO.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`
- `game-server/sql/aion_gs.sql`

## C# Runtime Changes

- Added `LoadLegionMembersAsync` to the player-enter-world repository contract.
- Implemented MySQL roster loading from `legion_members` joined to `players`, matching Java's member row plus player common-data hydration needs.
- Updated invite acceptance member-list sending to use persisted roster rows as the base.
- Overlaid live online registry data for currently online members so packet rows keep current class, world, intro, nickname, and online state when available.
- Preserved accepted-player exclusion from Java `updateLegionMemberList(player, false, player.getObjectId())`.
- Retained a transitional fallback that includes online same-legion players missing from the persisted roster.

## Validation Decision

- Changed surface: live invite-acceptance packet fanout plus repository persistence read.
- Specific behavior/contract: Java builds `SM_LEGION_MEMBERLIST` from all legion members, removes the accepted player when an excluded id is passed, and writes offline member rows into the roster packet.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepository" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java unit fixture exists for this live invite roster path.
- Broad-validation trigger: none beyond the focused live handler and repository surface. The filtered command compiled the touched game-server project and asserted the packet behavior directly.
- Broad .NET decision: skipped because the focused command exercises the edited live handler path and repository contract without changing shared packet primitives or schema.
- Why this scope is sufficient: the UOW is isolated to one live roster send path and one repository read; the handler test asserts offline rows, exclusion, online overlay, and serialized packet shape.

## Validation Result

- Focused C# result: Passed, 126 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- The opt-in MySQL integration test compiles under the focused command and follows the existing `AION_GAMESERVER_DB_INTEGRATION=1` convention; it was not executed against a live database in this run.
- Existing nullable/analyzer warnings remain outside this UOW.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.updateLegionMemberList` and `SM_LEGION_MEMBERLIST.writeImpl` | Invite acceptance loads persisted roster rows, excludes the accepted player, includes offline members, and overlays online registry data before sending the roster packet. | Live handler packet assertions from Java source review. | Still sends one chunk and has zero house fields. |
| `LoadLegionMembersAsync_LoadsPersistedRosterRowsAgainstJavaSchema_WhenEnabled` | Integration, opt-in | `LegionMemberDAO` and `PlayerDAO.loadPlayerCommonData` | MySQL loader maps legion member rank/nickname/selfintro plus player name/class/world/online/last-online fields. | SQL-shape fixture matching Java schema, gated by `AION_GAMESERVER_DB_INTEGRATION=1`. | Not executed unless DB integration env var is enabled. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.LegionService.updateLegionMemberList` | `Aion.GameServer.Network.Aion.GameServerConnection.SendLegionInviteMemberListAsync` | Live Service Path | Partial | Unit Tested | Partial Parity | Uses persisted roster plus online overlays and accepted-player exclusion. Java 80-row chunking and house lookup remain missing. |
| `com.aionemu.gameserver.dao.LegionMemberDAO.loadLegionMembers/loadLegionMember` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadLegionMembersAsync` | Repository | Partial | Integration Test Added, Unit-compiled | Partial Parity | Joins `legion_members` and `players` for roster packet fields. The opt-in DB test was not run without the integration environment. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | `Aion.GameServer.Model.Legion.LegionMemberSnapshot` | DTO | Partial | Unit Tested through packet path | Partial Parity | Covers roster packet fields currently modeled by C#. Challenge score and full in-memory legion-member cache behavior remain outside this UOW. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionMemberList` | Server Packet | Partial | Unit Tested | Partial Parity | Persisted offline rows now reach live packet sends. Chunking and housing fields remain gaps. |

## Summary Metrics

- Total Java artifacts discovered: 7
- Total artifacts ported or advanced in this UOW: 4 runtime/repository/packet artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked/not-started artifacts: 3 roster/bonus/invite gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java splits member lists into chunks of 80; this C# invite path still sends a single roster packet.
- Member-list house address and door-state ids are still zero because C# does not query active houses for roster rows here.
- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`; this UOW uses the existing database shape as the runtime roster source.
- The opt-in MySQL integration test was not executed without `AION_GAMESERVER_DB_INTEGRATION=1`.
- C# still does not model Java `legion.addBonus()` side effects on invite acceptance.
- Invite eligibility still has broader Java gaps such as other-faction invite config and `DeniedStatus.GUILD` behavior.
