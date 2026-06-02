# Phase 6 Session 2100 Completion - CM_FIND_GROUP Non-Live Dispatch Adapter

Date: 2026-06-02
Unit of Work: UOW-2100
Status: Completed

## Scope

- Added a non-live `CM_FIND_GROUP` dispatch adapter/result surface.
- Kept live `GameServerConnection` dispatch deferred.
- Updated readiness/design documentation to reflect the new adapter evidence without marking live dispatch ready.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` remains the source of truth for action routing.
  - Actions `20` and `25` remain parsed-only because Java has no `runImpl` branch.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Reviewed through the existing boundary evidence: direct packet sends, world broadcasts, action 12 invite branches, and no-packet update branches.

## What Changed

- Added `FindGroupConnectionBoundaryDispatchAdapterService`.
  - Composes disabled side-effect intent results from an existing parsed boundary composition plan.
  - Returns explicit statuses for:
    - composed direct packet/world-broadcast/invite side-effect intents,
    - Java branches with no packet side effects,
    - parsed actions with no Java `runImpl` branch,
    - missing active-player boundary,
    - missing action 12 invite runtime dependencies.
  - Does not invoke `GameServerConnection`, send packets, broadcast to world, or mark live dispatch ready.
- Added focused adapter tests covering:
  - action 0 direct packet intent,
  - action 1 world-broadcast intent,
  - action 3 no-side-effect update,
  - action 20 parsed-only no-op,
  - missing active-player boundary,
  - action 12 invite missing runtime,
  - action 12 invite runtime supplied.
- Updated readiness reports and design notes to mention the adapter evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: passed, 108 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# non-live adapter evidence around already-reviewed Java source behavior. It did not change Java source, Java parser behavior, packet wire format, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: non-live adapter/readiness/docs only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupConnectionBoundaryDispatchAdapterService` | Client Packet / Boundary Adapter | Partial | Unit Tested | Partial Parity | Non-live adapter can classify parsed boundary composition into direct packet intents, world-broadcast intents, optional action 12 invite plans, Java no-side-effect branches, parsed-only no-op branches, and missing-runtime statuses. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService`; dispatch/audit/executor services; `FindGroupConnectionBoundaryDispatchAdapterService` | Service / Boundary Adapter | Partial | Unit Tested | Partial Parity | Adapter reuses controlled planner/evidence output for FindGroupService-equivalent side effects but does not execute live sends. Singleton state lifetime, Java `ConcurrentHashMap` equivalence, lifecycle wiring, and runtime ordering remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionZeroComposesDirectPacketIntentWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP` action 0 and `FindGroupService.showRecruitments` source review | Adapter classifies parsed action 0 as direct packet intent without live dispatch | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no runtime send |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionOneComposesWorldBroadcastIntentWithoutLiveDispatch` | Unit | Java action 1 and `removeRecruitment` broadcast branch source review | Adapter exposes world-broadcast intent without live dispatch | Focused C# unit test using real packet parsing and reviewed Java source | Does not prove live registry broadcast order |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionThreeRecordsNoSideEffects` | Unit | Java action 3 update branch source review | Adapter classifies no-packet update branch as no side effects | Focused C# unit test using real packet parsing and reviewed Java source | Does not prove live singleton concurrency |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwentyPreservesParsedButNoRunImplNoOp` | Unit | Java `readImpl` parses action 20 but `runImpl` has no branch | Adapter preserves parsed-only no-op status | Focused C# unit test using real packet parsing and reviewed Java source | No live dispatch intentionally |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_MissingActivePlayerRecordsBlockedBoundaryWithoutLiveDispatch` | Unit | Java `runImpl` reads active player from connection | Adapter records missing active-player boundary | Focused C# unit test using real packet parsing and reviewed Java source | Does not test live connection logging |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwelveInviteWithoutRuntimeRecordsBlockedMissingRuntime` | Unit | Java action 12 invite branch source review | Adapter blocks invite planning when resolver/runtimes are absent | Focused C# unit test using real packet parsing and reviewed Java source | Does not dispatch live invite packets |
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwelveInviteWithRuntimeComposesDisabledInvitePlan` | Unit | Java action 12 group invite branch source review | Adapter composes disabled group invite plan when runtime dependencies are supplied | Focused C# unit test using real packet parsing and reviewed Java source | No real-client question-window comparison |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in/non-live and are not invoked by the packet boundary.
- `FindGroupRecruitmentPlanService` live singleton lifetime and concurrency are not verified.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryDispatchAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryDispatchAdapterServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2100-Completion.md`
- `docs/Phase-6-Session-2100-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.

Safe alternative candidates:

- Add an adapter-consumer test slice that proves how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review lifecycle singleton wiring requirements for logout and joined-team cleanup before any live dispatch work.
