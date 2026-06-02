# Phase 6 Session 2102 Handoff - FindGroup Lifecycle Singleton Wiring Readiness

Date: 2026-06-02
Unit of Work: UOW-2102
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
- `FindGroupLifecycleSingletonWiringReadinessService` now inventories Java singleton call sites and keeps live singleton wiring blocked.
- The current C# evidence does not approve live dispatch. Live singleton lifecycle, cross-caller wiring, multi-step mutation ordering, packet order, and real-client/runtime behavior remain unverified.

## Latest Completed Work

- UOW-2099: conservative live-dispatch design note added.
- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2102] Add find group lifecycle singleton readiness`
- `f90bf84e2 [Phase 6][UOW-2101] Align find group state store concurrency`
- `d606b0568 [Phase 6][UOW-2100] Add find group non-live dispatch adapter`
- `d6a7bd5ab [Phase 6][UOW-2099] Document find group live dispatch design`
- `8f1415c77 [Phase 6][UOW-2098] Add find group recruitment mutation evidence`
- `17eb85462 [Phase 6][UOW-2097] Add find group instance application evidence`

## Java Call-Site Inventory From UOW-2102

Java uses one `FindGroupService.SingletonHolder` instance across:

- `CM_FIND_GROUP.runImpl`: all live find-group packet actions.
- `PlayerLeaveWorldService.leaveWorld`: `onLogout(player)` before `ResponseRequester.denyAll()`.
- `PlayerGroupService.addPlayerToGroup`: `onJoinedTeam(invited)` after group membership mutation.
- `PlayerAllianceService.addPlayerToAlliance`: `onJoinedTeam(invited)` after alliance membership mutation.
- `PlayerGroupService.disband`: `removeRecruitment(group)` before removing the group.
- `PlayerAllianceService.disband`: `removeRecruitment(alliance)` before alliance disband events.

Current C# state:

- `GameServerConnection` still defers `CmFindGroup`.
- `PlayerEnterWorldService.LeaveWorldAsync` can record disabled logout cleanup before question denial when an observer is supplied, but normal live singleton wiring is not proven.
- `PlayerGroupInviteRequestService` and `PlayerAllianceInviteRequestService` can record joined-team cleanup when a `FindGroupJoinedTeamLifecycleRecorder` is injected, but `GameServerConnection` currently constructs those services without a recorder.
- Group/alliance disband recruitment-removal hooks are missing from C# runtime planning.

## Validation In UOW-2102

- Focused C# tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Result: 88 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - The UOW added non-live readiness/reporting around reviewed Java source call sites. No Java source, parser behavior, packet wire format, or Java-executable behavior changed.
- Broad .NET validation was skipped:
  - Readiness/reporting/docs only; no live handler wiring, shared connection dispatch, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or live side effect changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupLifecycleSingletonWiringReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Java singleton call sites are inventoried and test-backed. C# still lacks one proven live singleton shared by `CM_FIND_GROUP`, logout cleanup, joined-team cleanup, and disband recruitment removal. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService` lifecycle call sites | `Aion.GameServer.Services.PlayerGroupInviteRequestService`; `Aion.GameServer.Services.PlayerGroupRuntime`; `FindGroupLifecycleSingletonWiringReadinessService` | Team Lifecycle | Partial | Unit Tested | Partial Parity | Joined-team observer evidence exists when recorder is injected. Java disband recruitment removal is identified but not yet represented in C# group disband planning. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` lifecycle call sites | `Aion.GameServer.Services.PlayerAllianceInviteRequestService`; `Aion.GameServer.Services.PlayerAllianceRuntime`; `FindGroupLifecycleSingletonWiringReadinessService` | Team Lifecycle | Partial | Unit Tested | Partial Parity | Joined-team observer evidence exists when recorder is injected. Java disband recruitment removal is identified but not yet represented in C# alliance disband planning. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` logout cleanup ordering | `Aion.GameServer.Services.PlayerEnterWorldService`; `FindGroupLifecycleSingletonWiringReadinessService` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | C# observer evidence preserves FindGroup cleanup before question denial when supplied, but normal live singleton wiring remains unproven. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in/non-live and are not invoked by the packet boundary.
- Normal C# runtime does not yet prove that connection, logout, group, and alliance lifecycle paths share one `FindGroupRecruitmentPlanService` instance.
- Group/alliance disband recruitment-removal hooks are identified but not represented in C# runtime planning yet.
- Existing logout and joined-team evidence is observer-only and does not send live packets.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and runtime service concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2102 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add non-live group/alliance disband recruitment-removal planning evidence for Java `PlayerGroupService.disband` and `PlayerAllianceService.disband`.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under a future live singleton.
- Add a targeted Java/Maven fixture for one `FindGroupService` packet branch if a Java-executable target can be identified.

## Files Changed In UOW-2102

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2102-Completion.md`
- `docs/Phase-6-Session-2102-Handoff.md`
