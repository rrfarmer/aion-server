# Phase 6 Session 2100 Handoff - CM_FIND_GROUP Non-Live Dispatch Adapter

Date: 2026-06-02
Unit of Work: UOW-2100
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
- `FindGroupConnectionBoundaryDispatchAdapterService` now provides a non-live adapter/result surface for direct packet intents, world-broadcast intents, optional action 12 invite plans, no-side-effect Java branches, parsed-only no-op branches, missing active player, and missing invite runtime dependencies.
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` has been updated to show the non-live adapter exists.
- The design note and adapter explicitly do not approve or enable live dispatch.

## Latest Completed Work

- UOW-2097: parsed action 11/12 instance-application direct/invite boundary evidence added.
- UOW-2098: parsed action 2/3/6/7 recruitment/application mutation boundary evidence added.
- UOW-2099: conservative live-dispatch design note added.
- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.

## Recent Commits

- Current UOW commit message: `[Phase 6][UOW-2100] Add find group non-live dispatch adapter`
- `d6a7bd5ab [Phase 6][UOW-2099] Document find group live dispatch design`
- `8f1415c77 [Phase 6][UOW-2098] Add find group recruitment mutation evidence`
- `17eb85462 [Phase 6][UOW-2097] Add find group instance application evidence`
- `b9f5696b5 [Phase 6][UOW-2096] Add find group instance mutation evidence`

## Validation In UOW-2100

- Focused C# boundary/adapter tests passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore`
  - Result: 108 tests passed.
  - Existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - C# non-live adapter evidence was added around already-reviewed Java source behavior. No Java source, Java parser behavior, packet wire format, or Java-executable target changed.
- Broad .NET validation was skipped:
  - Non-live adapter/readiness/docs only; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`; `FindGroupConnectionBoundaryDispatchAdapterService` | Client Packet / Boundary Adapter | Partial | Unit Tested | Partial Parity | Non-live adapter can classify parsed boundary composition into direct packet intents, world-broadcast intents, optional action 12 invite plans, Java no-side-effect branches, parsed-only no-op branches, and missing-runtime statuses. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService`; dispatch/audit/executor services; `FindGroupConnectionBoundaryDispatchAdapterService` | Service / Boundary Adapter | Partial | Unit Tested | Partial Parity | Adapter reuses controlled planner/evidence output for FindGroupService-equivalent side effects but does not execute live sends. Singleton state lifetime, Java `ConcurrentHashMap` equivalence, lifecycle wiring, and runtime ordering remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor and invite dispatcher are opt-in/non-live and are not invoked by the packet boundary.
- `FindGroupRecruitmentPlanService` live singleton lifetime and concurrency are not verified.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and concurrency remain unverified.
- Broad .NET suite/build was not run in UOW-2100 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.

Safe alternative candidates:

- Add an adapter-consumer test slice that proves how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review lifecycle singleton wiring requirements for logout and joined-team cleanup before any live dispatch work.

## Files Changed In UOW-2100

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryDispatchAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryDispatchAdapterServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2100-Completion.md`
- `docs/Phase-6-Session-2100-Handoff.md`
