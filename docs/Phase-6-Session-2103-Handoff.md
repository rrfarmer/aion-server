# Phase 6 Session 2103 Handoff - FindGroup Disband Recruitment Cleanup Evidence

Date: 2026-06-02
Unit of Work: UOW-2103
Status: Completed

## Startup Context Rule

Future Phase 6 sessions should not read `PHASE-6-PROGRESS.md` during normal startup.

Read these instead:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- Latest `docs/Phase-6-Session-*-Completion.md`
- Latest `docs/Phase-6-Session-*-Handoff.md`

`docs/PHASE-6-PROGRESS.md` is a historical archive. Open it only for targeted archaeology when the latest completion/handoff docs do not contain enough context.

## Test Selection Rule

Focused validation is the default.

Do not run the broad .NET suite or full solution build as a routine heartbeat, end-of-unit habit, or substitute for choosing the right parity evidence. Filtered `dotnet test` commands already build the affected project and dependencies, so a full solution build needs its own documented broad-validation trigger.

Use the narrowest command that proves the scoped change.

Avoid broad .NET commands unless a broad-validation trigger is documented first.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Controlled parsed-boundary evidence exists for Java `runImpl` actions `0,1,2,3,4,5,6,7,8,9,10,11,12,13,15,17`.
- Parsed actions `20` and `25` remain no-op because Java parses them but has no `runImpl` branch.
- `FindGroupConnectionBoundaryDispatchAdapterService` provides a non-live adapter/result surface for direct packet intents, world-broadcast intents, optional action 12 invite plans, no-side-effect Java branches, parsed-only no-op branches, missing active player, and missing invite runtime dependencies.
- `FindGroupRecruitmentPlanService` uses `ConcurrentDictionary` for recruitment, application, and instance-group state stores to mirror Java `FindGroupService` `ConcurrentHashMap` declarations.
- `FindGroupLifecycleSingletonWiringReadinessService` inventories Java singleton call sites and keeps live singleton wiring blocked.
- `PlayerGroupRuntime` and `PlayerAllianceRuntime` can now expose non-live find-group recruitment-removal plans for Java disband paths when supplied with a `FindGroupRecruitmentPlanService`.
- The current C# evidence does not approve live dispatch. Live singleton lifecycle, cross-caller wiring, multi-step mutation ordering, packet order, and real-client/runtime behavior remain unverified.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2103] Add find group disband cleanup evidence`
- `2101f6caa [Phase 6][UOW-2102] Add find group lifecycle singleton readiness`
- `f90bf84e2 [Phase 6][UOW-2101] Align find group state store concurrency`
- `d606b0568 [Phase 6][UOW-2100] Add find group non-live dispatch adapter`
- `d6a7bd5ab [Phase 6][UOW-2099] Document find group live dispatch design`
- `8f1415c77 [Phase 6][UOW-2098] Add find group recruitment mutation evidence`

## Validation In UOW-2103

- Focused C# tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - First run failed on overly specific assertion text for the broadcast-intent Java breadcrumb.
  - Final result: 113 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. The UOW added C# non-live runtime plan evidence from reviewed Java disband call sites and existing `FindGroupService.removeRecruitment` behavior.
- Broad .NET validation was skipped:
  - Non-live group/alliance runtime planning and readiness/docs only; no live handler wiring, shared connection dispatch, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or live side effects changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan`; `PlayerGroupLeavePlan.FindGroupRecruitmentRemoval` | Team Lifecycle | Partial | Unit Tested | Partial Parity | C# can expose a non-live `FindGroupRecruitmentPlanService.RemoveRecruitment(teamId)` plan for disband cleanup when a find-group service is supplied. Normal runtime singleton wiring remains unproven. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow`; `PlayerAllianceLeaveWorkflowPlan.FindGroupRecruitmentRemoval` | Team Lifecycle | Partial | Unit Tested | Partial Parity | C# can expose a non-live `FindGroupRecruitmentPlanService.RemoveRecruitment(allianceId)` plan for disband cleanup when a find-group service is supplied. Normal runtime singleton wiring remains unproven. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment(int, ...)` | Service Method | Partial | Unit Tested | Partial Parity | Existing removal planner is reused for group/alliance disband cleanup and records same-race world-broadcast intent. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupLifecycleSingletonWiringReadinessService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Disband call sites are now non-live plan-only evidence. One shared live singleton across all callers remains unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in/non-live and are not invoked by the packet boundary.
- Normal C# runtime does not yet prove that connection, logout, group, and alliance lifecycle paths share one `FindGroupRecruitmentPlanService` instance.
- Disband cleanup evidence requires explicitly supplied `FindGroupRecruitmentPlanService`; DI/runtime wiring remains unproven.
- Existing logout and joined-team evidence is observer-only and does not send live packets.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and runtime service concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2103 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add a non-live singleton DI/runtime wiring plan for sharing `FindGroupRecruitmentPlanService` across the CM_FIND_GROUP boundary, logout cleanup, joined-team cleanup, and disband cleanup callers.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under a future live singleton.
- Add a targeted Java/Maven fixture for one `FindGroupService` packet branch if a Java-executable target can be identified.

## Files Changed In UOW-2103

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
