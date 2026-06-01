# Phase 6 Session 2083 Completion - Find Group Action 12 Invite Executor Evidence

Date: 2026-06-01
Unit of Work: UOW-2083
Status: Completed

## Scope

- Added disabled connection-adjacent evidence for `CM_FIND_GROUP` action 12 accepted instance-application invite dispatch.
- Preserved deferred live `CM_FIND_GROUP` handling in `GameServerConnection`.
- Updated readiness reporting to record action 12 executor evidence while keeping live dispatch blocked.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 12 reads `playerOrTeamId` and `instanceApplicationReply`, then calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Accepted instance application resolves applicant via `World.getPlayer(applicantId)`.
  - `minMembers <= 6` delegates to `PlayerGroupService.inviteToGroup(responder, applicant)`.
  - `minMembers > 6` delegates to `PlayerAllianceService.inviteToAlliance(responder, applicant)`.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `inviteToGroup` registers the party question request and sends inviter/question packets.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `inviteToAlliance` handles selected group-member redirection to group leader and registers the alliance question request.

## What Changed

- Added `FindGroupInstanceApplicationInviteDispatchPlanService`.
  - Consumes `FindGroupInstanceInviteIntent` from the existing disabled action 12 planner.
  - Resolves inviter/applicant players through a supplied resolver.
  - Routes group intents to `PlayerGroupInviteRequestService.SendInvite`.
  - Routes alliance intents to `PlayerAllianceInviteRequestService.SendInvite`.
  - Returns disabled results with `DispatchLiveSideEffects = false`; no packets are sent from this service.
- Added focused tests proving:
  - group invite intents register party question requests without live packet dispatch;
  - alliance invite intents register alliance question requests without live packet dispatch;
  - missing applicant and missing intent paths skip safely.
- Updated `FindGroupLiveDispatchReadinessReportService`.
  - Records action 12 disabled executor evidence.
  - Keeps the `CM_FIND_GROUP` boundary blocked because the executor is not invoked from live connection dispatch.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Result: passed, 64 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - `mvn -pl game-server -am test "-Dmaven.test.skip=false" "-DskipTests=false" "-Dtest=CM_FIND_GROUP_ReadPayloadGoldenTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
  - Result: passed, 12 tests.
  - Evidence scope: Java parser golden covers action 12 payload fields; action 12 service delegation evidence is Java source review.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: added a disabled executor and readiness evidence only. No live connection dispatch, packet primitive, persistence, crypto, scheduling, world-state, or live send path was enabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action 12 | `Aion.GameServer.Services.FindGroupClientActionPlanService`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Client Action / Service Plan | Partial | Unit Tested; Java Golden Tested | Partial Parity | C# parses/plans action 12 and can compose disabled group/alliance invite request-service results. Live `GameServerConnection` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` accepted invite branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; `FindGroupInstanceApplicationInviteDispatchPlanService` | Find Group Service / Invite Dispatch | Partial | Unit Tested | Partial Parity | C# preserves group-vs-alliance invite selection by `minMembers` and now composes the matching invite request service result. Direct packet sends remain disabled. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.inviteToGroup` find-group action 12 path | `Aion.GameServer.Services.PlayerGroupInviteRequestService.SendInvite` via disabled executor | Group Invite / Request Service | Partial | Unit Tested | Partial Parity | Disabled executor registers the party question request and exposes packet plans; it does not send packets or enable live action 12 dispatch. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` find-group action 12 path | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` via disabled executor | Alliance Invite / Request Service | Partial | Unit Tested | Partial Parity | Disabled executor registers the alliance question request and preserves existing redirection behavior through the request service; it does not send packets or enable live action 12 dispatch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_GroupInviteIntentRegistersPartyQuestionWithoutLiveDispatch` | Unit | Java `FindGroupService.sendInstanceApplicationResult` and `PlayerGroupService.inviteToGroup` source review | Group invite intent routes to group invite request registration with live dispatch disabled | Focused C# unit test | Does not send packets or execute Java runtime |
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_AllianceInviteIntentRegistersAllianceQuestionWithoutLiveDispatch` | Unit | Java `FindGroupService.sendInstanceApplicationResult` and `PlayerAllianceService.inviteToAlliance` source review | Alliance invite intent routes to alliance invite request registration with live dispatch disabled | Focused C# unit test | Does not send packets or execute Java runtime |
| `FindGroupInstanceApplicationInviteDispatchPlanServiceTests.CreateDisabledPlan_MissingApplicantSkipsWithoutQuestionMutation` | Unit | Java `World.getPlayer(applicantId)` resolution branch source review | Missing applicant skips request mutation and remains non-live | Focused C# unit test | Does not compare Java runtime return/log behavior |
| `CM_FIND_GROUP_ReadPayloadGoldenTest` | Java Golden | Java test suite | Action 12 payload parses `playerOrTeamId` and `instanceApplicationReply` | Focused Java/Maven test | Parser-only; not service delegation runtime |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 4.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The action 12 executor is not invoked from the live connection boundary.
- Direct packet sends from group/alliance invite request results are still not executed by this path.
- World broadcasts, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupInstanceApplicationInviteDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationInviteDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-Session-2083-Completion.md`
- `docs/Phase-6-Session-2083-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: define a no-live-send executor contract for direct `FindGroupDirectPacketIntent` and `FindGroupWorldBroadcastIntent` dispatch so direct sends/broadcasts can be audited separately before any live `CM_FIND_GROUP` boundary wiring.

Safe alternative candidates:

- Inspect `GameServerConnection` `CmFindGroup` deferred branch and design the final disabled boundary aggregation report.
- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
