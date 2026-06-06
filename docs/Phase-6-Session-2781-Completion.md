# Phase 6 Session 2781 Completion

## Unit of Work

[Phase 6][UOW-2781] Populate legion member-list house fields

## Runtime Progress Gate

- Deferred/live behavior advanced: C# `SM_LEGION_MEMBERLIST` rows had fields for active house address and door state, but live roster construction and persisted roster loading always left them as zero.
- Java source of truth: `SM_LEGION_MEMBERLIST.writeLegionMember`, `HousingService.findActiveHouse`, `House.getAddress().getId`, `House.getDoorState().getId`, and `House.setPermissionsFromDB`.
- C# runtime artifact wired/fixed: `GameServerConnection.CreateLegionMemberListEntry`, `LegionMemberSnapshot`, `MySqlPlayerEnterWorldRepository.LoadLegionMembersAsync`, `LoadLegionMemberByNameAsync`, and `SmLegionMemberList` test coverage.
- Client-visible/state/persistence effect changed: live legion member-list packets sent after invite acceptance can now include active house address and door-state ids for online members with loaded houses and persisted roster members with house rows.
- Why this is not preview-only/test-only/documentation-only: it changes real `SM_LEGION_MEMBERLIST` packet payloads emitted by live legion code and restores runtime roster state from the existing `houses` table.

## Java Sources Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_LEGION_MEMBERLIST.java`
- `game-server/src/com/aionemu/gameserver/services/HousingService.java`
- `game-server/src/com/aionemu/gameserver/model/house/House.java`
- `game-server/src/com/aionemu/gameserver/model/house/HouseDoorState.java`

## C# Runtime Changes

- Added optional `HouseAddressId` and `HouseDoorStateId` fields to `LegionMemberSnapshot`.
- Extended legion member repository projections to select the Java-equivalent active house row from `houses`, using studio priority and oldest active-house ordering.
- Decoded persisted house settings through `PlayerHouse.GetDoorStateFromSettings`.
- Populated online roster rows from loaded `Player.Houses` using `PlayerActiveHouseResolverService.FindActiveHouse`.
- Updated packet/live handler tests to assert non-zero active-house fields in `SM_LEGION_MEMBERLIST`.

## Validation Decision

- Changed surface: live legion roster packet construction plus scoped legion member repository projection.
- Specific behavior/contract: Java writes each legion member's active house address and door-state id into `SM_LEGION_MEMBERLIST`.
- Focused C# command:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~CmLegionTests|FullyQualifiedName~PlayerEnterWorldRepositoryDatabaseIntegrationTests" --logger "console;verbosity=minimal" --no-restore
```

- Focused Java/Maven command: not run; Java source was unchanged and no narrow Java fixture exists for `SM_LEGION_MEMBERLIST`.
- Broad-validation trigger: scoped persistence projection changed, but only for legion member roster rows.
- Broad .NET decision: skipped. The focused command compiles the affected project, exercises live packet construction, and compiles the opt-in DB integration assertions; a broader suite would not execute the gated live DB branch without `AION_GAMESERVER_DB_INTEGRATION=1`.
- Why this scope is sufficient: `CmLegionTests` proves live packet payload behavior for online and persisted roster rows, while the repository integration fixture now documents the SQL contract for active-house projection when DB integration is enabled.

## Validation Result

- Focused C# result: Passed, 121 total, 0 failed, 0 skipped.
- `git diff --check`: passed with line-ending warnings only.
- Existing nullable/analyzer warnings remain outside this UOW.
- Opt-in DB branch in `PlayerEnterWorldRepositoryDatabaseIntegrationTests` was not executed because `AION_GAMESERVER_DB_INTEGRATION` was not set.

## Tests Added or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `SmLegionMemberList_WritesJavaLastChunkPayload` | Unit | `SM_LEGION_MEMBERLIST.writeLegionMember` | Packet writer preserves non-zero house address and door-state fields. | Packet byte reader assertions against reviewed Java field order. | No Java golden fixture. |
| `HandleQuestionResponseAsync_LegionInviteAcceptPersistsMemberMutatesStateAndBroadcastsLikeJava` | Unit | `LegionService.addLegionMember` and `SM_LEGION_MEMBERLIST.writeLegionMember` | Live invite-accept roster packet uses online `Player.Houses` and persisted snapshot house fields. | Live handler side-effect packet assertions. | Java `legion.addBonus()` remains missing. |
| `LoadLegionMembersAsync_LoadsPersistedRosterRowsAgainstJavaSchema_WhenEnabled` | Opt-in Integration | `HousingService.findActiveHouse` and `House.setPermissionsFromDB` | Repository projection restores active house address and door state from `houses`. | Compiled in focused run; DB execution requires opt-in environment. | Not executed in this session. |

## Conservative Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBERLIST` | `Aion.GameServer.Network.Aion.ServerPackets.SmLegionMemberList` | Server Packet | Partial | Unit Tested | Partial Parity | Active-house fields now populated; no Java golden packet fixture and broader roster ordering remains only source-reviewed. |
| `com.aionemu.gameserver.services.HousingService.findActiveHouse` | `Aion.GameServer.Services.PlayerActiveHouseResolverService.FindActiveHouse` plus repository projection | Service/Projection | Partial | Unit Tested and Opt-in Integration Fixture | Partial Parity | Online rows use loaded `Player.Houses`; persisted rows use SQL projection with studio/oldest-house priority. Java global housing cache is not fully modeled. |
| `com.aionemu.gameserver.model.house.House` | `Aion.GameServer.Model.GameObjects.PlayerHouse` | Model | Partial | Unit Tested | Partial Parity | Door state/settings decoding reused; full Java house object behavior is outside this UOW. |
| `com.aionemu.gameserver.model.team.legion.LegionMember` | `Aion.GameServer.Model.Legion.LegionMemberSnapshot` | Snapshot DTO | Partial | Unit Tested | Partial Parity | Snapshot now carries active-house fields; Java cached member list and full `Legion` aggregate remain partial. |

## Summary Metrics

- Total Java artifacts discovered: 4
- Total artifacts ported or advanced in this UOW: 4 runtime/packet/projection artifacts
- Total artifacts with verified parity: 0
- Total artifacts needing verification/partial parity: 4
- Total blocked/not-started artifacts: 3 legion bonus/aggregate/runtime gaps remain
- Estimated overall migration completion: unchanged, conservatively still Phase 6 in progress

## Known Gaps

- Java `legion.addBonus()` and `legion.removeBonus()` icon/XP bonus behavior are still not modeled.
- C# still does not model Java's full in-memory `Legion` aggregate and `memberIds`.
- The new SQL projection was compiled and documented by an opt-in integration fixture but was not executed against a live DB in this session.
- No Java golden or runtime comparison fixture was generated for `SM_LEGION_MEMBERLIST`.
