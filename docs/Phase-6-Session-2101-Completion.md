# Phase 6 Session 2101 Completion - FindGroup State Store Concurrency

Date: 2026-06-02
Unit of Work: UOW-2101
Status: Completed

## Scope

- Reviewed Java `FindGroupService` state-store declarations and matching C# state-store behavior.
- Aligned the C# recruitment, application, and instance-group maps with Java `ConcurrentHashMap` shape.
- Kept live `CM_FIND_GROUP` dispatch and live singleton wiring blocked pending separate lifecycle/order evidence.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Java declares `recruitments`, `applications`, and `instanceGroups` as `ConcurrentHashMap`.
  - Reviewed add/update lookup, remove, show-list enumeration, logout cleanup, and joined-team cleanup map operations.

## What Changed

- Updated `FindGroupRecruitmentPlanService` state stores from `Dictionary` to `ConcurrentDictionary`.
- Updated map removals to `TryRemove` for recruitment, application, instance-group, logout cleanup, and joined-team cleanup paths.
- Added a focused concurrency smoke test covering parallel add/register operations, show-list enumeration, and parallel logout cleanup.
- Updated readiness services and tests to record Java-shaped concurrent state-store evidence without marking live dispatch ready.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the remaining blocker is live singleton lifecycle, caller wiring, multi-step mutation ordering, and runtime observation rather than the basic C# map type.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 105 tests.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW aligned C# map type and removal operations to reviewed Java `ConcurrentHashMap` source.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: this changed a non-live scoped FindGroup state-store service and readiness/docs. It did not wire live `GameServerConnection`, shared connection dispatch, packet primitives, crypto, persistence, scheduling, or world-state infrastructure. Focused planner/state-store/adapter/readiness tests cover the affected surface.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` `ConcurrentHashMap` state stores | `Aion.GameServer.Services.FindGroupRecruitmentPlanService` `ConcurrentDictionary` state stores | Service State | Partial | Unit Tested | Partial Parity | C# now mirrors Java concurrent map shape for recruitment, application, and instance-group stores. Live singleton lifecycle, cross-caller wiring, multi-step ordering, enumeration snapshot semantics, and runtime dispatch remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `CmFindGroup` planner/evidence/adapter services | Client Packet / Boundary Adapter | Partial | Unit Tested | Partial Parity | This UOW only closed the basic map-type concurrency gap. Live packet boundary remains deferred. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.ConcurrentMutations_UseJavaConcurrentHashMapStyleStateStores` | Unit | Java `FindGroupService` `ConcurrentHashMap` state declarations and map operations | Parallel add/register, show-list enumeration, and logout cleanup do not rely on unsafe `Dictionary` mutation | Focused C# unit test plus reviewed Java source | Does not prove live singleton wiring, atomic multi-step ordering, or real client packet order |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_RecordsLifecycleObserverEvidenceWithoutMarkingLiveDispatchReady` | Unit | Java `FindGroupService` lifecycle state-store behavior | Readiness report records concurrent state-store evidence while keeping live dispatch blocked | Focused C# unit test | Report evidence only |
| `FindGroupConnectionBoundaryReadinessAggregateServiceTests.CreateReport_AggregatesDisabledPlannerExecutorAuditAndLifecycleEvidence` | Unit | Java `FindGroupService.onLogout/onJoinedTeam` and map declarations | Aggregate readiness includes concurrent state-store evidence with live singleton wiring still partial | Focused C# unit test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1.
- Total artifacts ported or represented in this UOW: 1 service plus readiness documentation.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- `FindGroupRecruitmentPlanService` is not yet proven as the live singleton used consistently across connection, logout, and invite callers.
- `ConcurrentDictionary` closes the basic concurrent map-type gap, but it does not by itself prove Java-equivalent multi-step ordering or live packet visibility.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and runtime service concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2101-Completion.md`
- `docs/Phase-6-Session-2101-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review lifecycle singleton wiring requirements for logout and joined-team cleanup before any live dispatch work.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Add a targeted Java/Maven fixture for one `FindGroupService` packet branch if a Java-executable target can be identified.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under a future live singleton.
