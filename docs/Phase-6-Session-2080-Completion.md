# Phase 6 Session 2080 Completion - Find Group Joined-Team Invite Observer

Date: 2026-06-01
Unit of Work: UOW-2080
Status: Completed

## Scope

- Added observer-only find-group `onJoinedTeam` composition to C# group/alliance invite accept services.
- Preserved Java lifecycle placement for the represented slice: joined-team cleanup is recorded after team membership mutation and before entered-packet fanout planning.
- Kept live `CM_FIND_GROUP` dispatch and find-group packet side effects disabled.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupInvite.java`
  - Accepted group invite calls `PlayerGroupService.addPlayer(group, invited)` for existing groups or `PlayerGroupService.createGroup(inviter, invited, TeamType.GROUP, 0)` for new groups.
- `game-server/src/com/aionemu/gameserver/model/team/group/events/PlayerGroupEnteredEvent.java`
  - `handleEvent` calls `PlayerGroupService.addPlayerToGroup(team, player)` before group-info/member-info/system-message fanout.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `addPlayerToGroup` mutates membership and calls `FindGroupService.getInstance().onJoinedTeam(invited)`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceInvite.java`
  - Accepted alliance invite creates or extends an alliance, removing source groups before adding collected players.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceEnteredEvent.java`
  - `handleEvent` calls `PlayerAllianceService.addPlayerToAlliance(team, player)` before alliance-info/member-info/system-message fanout.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `addPlayerToAlliance` mutates membership and calls `FindGroupService.getInstance().onJoinedTeam(invited)`.

## What Changed

- Added `FindGroupJoinedTeamLifecycleRecorder`.
  - Builds a current team `FindGroupRecruitmentSubject` from C# group/alliance runtime members.
  - Calls `FindGroupRecruitmentPlanService.OnJoinedTeam(...)`.
  - Exposes observer-only disabled plans and dispatches nothing.
- Extended `PlayerGroupInviteRequestService` with optional joined-team recorder support.
  - New group accept records both initial entrants.
  - Existing group accept records the invited entrant only.
  - `GroupInviteResponseResult` now exposes `FindGroupJoinedTeamPlans`.
- Extended `PlayerAllianceInviteRequestService` with optional joined-team recorder support.
  - New alliance accept records the requester after alliance creation and records every added member.
  - Existing alliance accept records every added member.
  - `AllianceInviteResponseResult` now exposes `FindGroupJoinedTeamPlans`.
- Added focused tests for group and alliance invite accept paths.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Result: passed, 38 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: targeted search found no narrow Java test class for `PlayerGroupInvite`, `PlayerAllianceInvite`, entered events, or `onJoinedTeam` under `game-server/test`. Java evidence for this UOW is source review of the exact invite/event/service call chain above.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: change is optional observer-only invite lifecycle evidence plus focused tests. It does not enable live find-group dispatch, change packet primitives, persistence, crypto, scheduling, shared packet serialization, or broad runtime side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.events.PlayerGroupEnteredEvent` / `PlayerGroupService.addPlayerToGroup` find-group slice | `Aion.GameServer.Services.PlayerGroupInviteRequestService`; `FindGroupJoinedTeamLifecycleRecorder.RecordGroupJoin` | Group Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onJoinedTeam` plans after accepted group invite membership mutation. Live singleton dispatch remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceEnteredEvent` / `PlayerAllianceService.addPlayerToAlliance` find-group slice | `Aion.GameServer.Services.PlayerAllianceInviteRequestService`; `FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin` | Alliance Lifecycle / Service | Partial | Unit Tested | Partial Parity | C# now records disabled `FindGroupService.onJoinedTeam` plans after accepted alliance invite membership mutation, including group merge members. Live singleton dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam`; `FindGroupJoinedTeamPlan` | Find Group Service / Plan | Partial | Unit Tested | Partial Parity | Existing planner behavior is now connected to group/alliance invite accept paths as observer-only evidence. Instance-group min-member removal state is still not sourced from live auto-group runtime. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerGroupInviteRequestServiceTests.HandleResponse_AcceptCreatesGroupRecordsFindGroupJoinedTeamForBothEntrantsLikeJavaEvent` | Unit | Java group invite/create-group entered event source review | New group accept records disabled joined-team plans for both entrants and removes solo/application find-group state | Focused C# unit test | Does not execute Java runtime or live find-group sends |
| `PlayerGroupInviteRequestServiceTests.HandleResponse_AcceptAddsToExistingGroupRecordsFindGroupJoinedTeamOnlyForInvited` | Unit | Java existing-group invite entered event source review | Existing group accept records only the newly invited player | Focused C# unit test | Does not execute Java runtime or live find-group sends |
| `PlayerAllianceInviteRequestServiceTests.HandleResponse_AcceptCreatesAllianceRecordsFindGroupJoinedTeamForRequesterAndInvited` | Unit | Java alliance create/entered event source review | New alliance accept records disabled joined-team plans for requester and invited player | Focused C# unit test | Does not execute Java runtime or live find-group sends |
| `PlayerAllianceInviteRequestServiceTests.HandleResponse_AcceptMergesGroupsRecordsEveryAddedAllianceMemberAfterGroupRemoval` | Unit | Java alliance group-merge invite source review | Alliance accept that merges groups records requester plus every collected member after group removal | Focused C# unit test | Does not execute Java runtime, league branch, or live find-group sends |

## Summary Metrics

- Total Java artifacts discovered/reviewed in this UOW: 6.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- The joined-team hook is observer-only; it does not send direct packets or world broadcasts.
- Instance-group min-member removal input is not yet sourced from a live C# auto-group runtime.
- Encrypted socket behavior, real-client behavior, service concurrency, and live group/alliance invite dispatch from `CM_FIND_GROUP` remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupJoinedTeamLifecycleRecorder.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupInviteRequestService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInviteRequestService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupInviteRequestServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceInviteRequestServiceTests.cs`
- `docs/Phase-6-Session-2080-Completion.md`
- `docs/Phase-6-Session-2080-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: inspect whether the joined-team recorder can safely source instance-group min-member state from existing auto-group/instance-group C# surfaces, or whether a readiness report should keep it blocked.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the readiness report to define the final pre-live checklist.
