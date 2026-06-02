# Phase 6 Session 2103 Completion - FindGroup Disband Recruitment Cleanup Evidence

Date: 2026-06-02
Unit of Work: UOW-2103
Status: Completed

## Scope

- Added non-live C# evidence for Java group/alliance disband recruitment cleanup.
- Kept live `CM_FIND_GROUP` dispatch and normal live singleton wiring blocked.
- Updated readiness/design documentation to distinguish missing hooks from non-live opt-in planning evidence.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `disband(PlayerGroup group)` calls `FindGroupService.getInstance().removeRecruitment(group)` before removing the group.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `disband(PlayerAlliance alliance, boolean onBefore)` calls `FindGroupService.getInstance().removeRecruitment(alliance)` before alliance disband events.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `removeRecruitment(TemporaryPlayerTeam<?>)` removes the team-keyed recruitment and broadcasts `SM_FIND_GROUP` action 1 to same-race players.

## What Changed

- Added optional `FindGroupRecruitmentPlanService` dependencies to `PlayerGroupRuntime` and `PlayerAllianceRuntime`.
- Extended `PlayerGroupLeavePlan` with optional `FindGroupRecruitmentRemoval`.
- Extended `PlayerAllianceLeaveWorkflowPlan` with optional `FindGroupRecruitmentRemoval`.
- When a normal two-member group/alliance leave causes disband and a find-group service is supplied:
  - the runtime calls `RemoveRecruitment(teamId/allianceId, serverId, 0, 0, 0)`,
  - records the resulting non-live broadcast intent,
  - removes the find-group recruitment state before clearing the team/alliance runtime state.
- Updated `FindGroupLifecycleSingletonWiringReadinessService` to mark disband cleanup as `PartialNonLivePlanOnly` rather than missing-hook.
- Updated the CM_FIND_GROUP live-dispatch design note with the new evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - First run failed on overly specific assertion text for the broadcast-intent Java breadcrumb.
  - Final result: passed, 113 tests.
  - Note: existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW added C# non-live runtime plan evidence from reviewed Java disband call sites and existing `FindGroupService.removeRecruitment` behavior.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: this touched non-live group/alliance runtime planning and readiness/docs. It did not enable live handler wiring, shared connection dispatch, packet primitives, crypto, persistence, scheduling, world-state infrastructure, or live side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan`; `PlayerGroupLeavePlan.FindGroupRecruitmentRemoval` | Team Lifecycle | Partial | Unit Tested | Partial Parity | C# can now expose a non-live `FindGroupRecruitmentPlanService.RemoveRecruitment(teamId)` plan for disband cleanup when a find-group service is supplied. Normal runtime singleton wiring remains unproven. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow`; `PlayerAllianceLeaveWorkflowPlan.FindGroupRecruitmentRemoval` | Team Lifecycle | Partial | Unit Tested | Partial Parity | C# can now expose a non-live `FindGroupRecruitmentPlanService.RemoveRecruitment(allianceId)` plan for disband cleanup when a find-group service is supplied. Normal runtime singleton wiring remains unproven. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment(int, ...)` | Service Method | Partial | Unit Tested | Partial Parity | Existing removal planner is reused for group/alliance disband cleanup and records same-race world-broadcast intent. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Disband call sites moved from missing-hook to non-live plan-only evidence. One shared live singleton across all callers remains unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerGroupRuntimeTests.RemoveMemberWithLeavePlan_DisbandRemovesFindGroupTeamRecruitmentLikeJava` | Unit | Java `PlayerGroupService.disband` and `FindGroupService.removeRecruitment(group)` source review | Two-member group disband can remove team-keyed find-group recruitment and record broadcast intent without live dispatch | Focused C# unit test plus reviewed Java source | Opt-in injected service only; normal live singleton wiring unproven |
| `PlayerAllianceRuntimeTests.RemoveMemberWithLeaveWorkflow_DisbandRemovesFindGroupAllianceRecruitmentLikeJava` | Unit | Java `PlayerAllianceService.disband` and `FindGroupService.removeRecruitment(alliance)` source review | Two-member alliance disband can remove alliance-keyed find-group recruitment and record broadcast intent without live dispatch | Focused C# unit test plus reviewed Java source | Opt-in injected service only; normal live singleton wiring unproven |
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_RecordsObserverOnlyAndNonLivePlanGaps` | Unit | Java singleton lifecycle call-site source review | Readiness report marks disband cleanup as non-live plan-only, not live singleton ready | Focused C# unit test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` and live singleton wiring remain intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Normal C# runtime does not yet prove that connection, logout, group, and alliance lifecycle paths share one `FindGroupRecruitmentPlanService` instance.
- Disband cleanup evidence requires explicitly supplied `FindGroupRecruitmentPlanService`; DI/runtime wiring remains unproven.
- Existing logout and joined-team evidence is observer-only and does not send live packets.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and runtime service concurrency remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerGroupReconnectPlan.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2103-Completion.md`
- `docs/Phase-6-Session-2103-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add a non-live singleton DI/runtime wiring plan for sharing `FindGroupRecruitmentPlanService` across the CM_FIND_GROUP boundary, logout cleanup, joined-team cleanup, and disband cleanup callers.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under a future live singleton.
- Add a targeted Java/Maven fixture for one `FindGroupService` packet branch if a Java-executable target can be identified.
