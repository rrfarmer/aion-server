# Phase 6 Session 2091 Completion - Find Group Boundary Composition Evidence

Date: 2026-06-01
Unit of Work: UOW-2091
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` and `FindGroupService` action dispatch paths for the next boundary evidence slice.
- Added controlled C# evidence that a parsed `CmFindGroup` packet can flow through disabled boundary composition, explicit side-effect intent extraction, and the opt-in side-effect executor.
- Preserved live `CM_FIND_GROUP` deferral; `GameServerConnection` was not wired to the executor.
- Tightened orchestration documentation so focused tests remain the default and full solution builds are reserved for documented broad-validation triggers.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 15 reads `playerOrTeamId` and `instanceMaskId`.
  - `runImpl` dispatches Java actions to `FindGroupService`.
  - Actions 20 and 25 are parsed but have no `runImpl` branch.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroupMembersInfo` sends `new SM_FIND_GROUP(16, List.of(instanceGroup))` directly to the requesting player.

## What Changed

- Added `FindGroupConnectionBoundarySideEffectCompositionEvidenceService`.
  - Extracts explicit direct packet intents, world broadcast intents, and invite intents from a disabled `FindGroupConnectionClientActionCompositionPlan`.
  - Provides `ExecuteOptInAsync` for controlled evidence only.
  - Records `IsCmFindGroupBoundaryWired = false` and keeps `ShouldDispatchLiveSideEffects = false` for the boundary intent plan.
- Added focused tests that start from real parsed `CmFindGroup` payloads.
  - Action 15: parsed packet -> disabled composition plan -> extracted direct packet intent -> opt-in executor result through `IGameClientConnectionRegistry`.
  - Action 20: parsed-but-no-`runImpl` packet remains side-effect-free even when the controlled evidence path is invoked.
- Updated live-dispatch readiness and connection-boundary aggregate reports to include the new boundary composition evidence while keeping live readiness blocked.
- Updated `docs/orchestration-rules.md` to state that filtered `dotnet test` already provides the normal compile-and-test check for narrow units and full solution builds require a broad-validation trigger.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - Result: passed, 31 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# boundary evidence around reviewed Java source behavior. It did not change Java source, Java packet parsing, or a Java-executable test target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: narrow service/test/readiness documentation change; no live handler wiring, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or shared connection dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`; `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Client Packet / Boundary Evidence | Partial | Unit Tested | Partial Parity | Real parsed C# `CmFindGroup` payloads can feed disabled planner composition and explicit opt-in executor evidence for action 15. Live `GameServerConnection` dispatch remains intentionally deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroupMembersInfo` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupMembersInfo`; `FindGroupSideEffectDispatchExecutorService`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService` | Planner / Opt-In Side-Effect Evidence | Partial | Unit Tested | Partial Parity | Controlled evidence proves the Java-shaped action 16 direct packet intent from parsed action 15 can be executed through the registry when explicitly invoked. It does not prove live socket order or real-client parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_ComposesParsedActionFifteenPlanAndExecutorResult` | Unit | Java `CM_FIND_GROUP` action 15 and `FindGroupService.showInstanceGroupMembersInfo` source review | Parsed action 15 payload composes disabled planner output and opt-in executor result for the member-info direct packet | Focused C# unit test using real packet parsing and reviewed Java source | Does not wire `GameServerConnection`; no encrypted socket/runtime comparison |
| `FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.ExecuteOptInAsync_LeavesParsedButNoRunImplActionWithoutSideEffects` | Unit | Java `CM_FIND_GROUP.readImpl` parses action 20 but `runImpl` has no branch | Parsed action 20 remains side-effect-free in the controlled evidence path | Focused C# unit test using real packet parsing and reviewed Java source | Does not compare against a Java runtime trace |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, and concurrency remain unverified.
- Broad .NET validation was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/orchestration-rules.md`
- `docs/Phase-6-Session-2091-Completion.md`
- `docs/Phase-6-Session-2091-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: review whether another direct-packet `FindGroupService` branch needs controlled boundary evidence or is already sufficiently covered by the generic boundary composition/executor path.

Safe alternative candidates:

- Add narrow Java-side fixture/golden evidence for a `FindGroupService` packet branch if a matching Java test target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Add controlled world-broadcast composition evidence from a parsed `CmFindGroup` mutation branch, still without wiring `GameServerConnection`.
