# Phase 6 Session 2087 Completion - Find Group Action 11 Direct Dispatch Evidence

Date: 2026-06-01
Unit of Work: UOW-2087
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` action 11 and `FindGroupService.sendInstanceApplication`.
- Added a connection-adjacent disabled executor for the action 11 direct `SM_FIND_GROUP(applicant)` packet intent.
- Updated readiness and boundary aggregate reporting so action 11 direct packet dispatch evidence is explicit alongside action 12 invite evidence.
- Preserved live `CM_FIND_GROUP` deferral; no packet was sent through the connection registry.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 11 reads `playerOrTeamId` and `instanceMaskId`.
  - `runImpl` calls `FindGroupService.getInstance().sendInstanceApplication(player, playerOrTeamId)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplication` resolves the target with `World.getInstance().getPlayer(playerOrTeamId)`.
  - If the target player exists, Java sends `new SM_FIND_GROUP(applicant)` directly to that player.
  - If the target player is missing, Java performs no send.

## What Changed

- Added `FindGroupInstanceApplicationDirectDispatchPlanService`.
  - Consumes a `FindGroupInstanceApplicationPlan`.
  - Resolves planned direct packet recipients through a supplied player resolver.
  - Produces direct packet dispatch audit evidence through `FindGroupSideEffectDispatchAuditService`.
  - Always returns `DispatchLiveSideEffects = false`.
- Added focused tests proving:
  - action 11 direct `SmFindGroup` packet intent is planned without live dispatch;
  - missing recipient skips without live dispatch;
  - missing direct packet intent skips without live dispatch;
  - missing plan skips without live dispatch.
- Updated readiness reporting:
  - `FindGroupLiveDispatchReadinessReportService` now lists action 11 disabled executor evidence and still keeps live dispatch blocked.
  - `FindGroupConnectionBoundaryReadinessAggregateService` now includes action 11 direct packet executor evidence and updated live-dispatch requirements.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Result: passed, 66 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# disabled executor evidence around reviewed Java action 11 source behavior. It did not change Java packet parsing, Java runtime behavior, or a Java-executable parity target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: disabled executor/reporting only; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupInstanceApplicationDirectDispatchPlanService` | Disabled Executor / Side-Effect Boundary | Partial | Unit Tested | Partial Parity | C# can consume the action 11 direct packet intent, resolve intended recipients, and audit the planned `SmFindGroup` packet without live dispatch. It does not send through the connection registry. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action 11 readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`; `FindGroupConnectionBoundaryReadinessAggregateService` | Readiness Report | Partial | Unit Tested | Partial Parity | Readiness reports now separate action 11 direct packet executor evidence from action 12 invite executor evidence while preserving live-dispatch blockers. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_ActionElevenDirectPacketIntentIsPlannedWithoutLiveDispatch` | Unit | Java `FindGroupService.sendInstanceApplication` source review and existing `SmFindGroup` action 11 golden | Action 11 direct packet intent resolves to an audit record without live dispatch | Focused C# unit test | Does not execute real socket send |
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_MissingRecipientSkipsWithoutLiveDispatch` | Unit | Java `World.getPlayer(...) != null` guard | Missing recipient prevents dispatch evidence and remains non-live | Focused C# unit test | Does not compare live Java runtime |
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_MissingDirectPacketIntentSkipsWithoutLiveDispatch` | Unit | C# defensive disabled executor behavior | Missing direct packet intent is skipped safely | Focused C# unit test | Java has no corresponding disabled helper |
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_MissingPlanSkipsWithoutLiveDispatch` | Unit | C# defensive disabled executor behavior | Missing application plan is skipped safely | Focused C# unit test | Java has no corresponding disabled helper |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 4 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The action 11 direct dispatch service is disabled and does not call the connection registry.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- Future live dispatch still needs an opt-in executor with connection-registry tests before wiring to the packet boundary.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupInstanceApplicationDirectDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationDirectDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `docs/Phase-6-Session-2087-Completion.md`
- `docs/Phase-6-Session-2087-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: design opt-in connection-registry direct-send/world-broadcast executor tests for Find Group side effects without wiring them into `CM_FIND_GROUP`.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Inspect declined action 12 `SM_MESSAGE` direct packet dispatch evidence and decide whether it should share the action 11 direct-dispatch executor or stay under the generic side-effect audit.
