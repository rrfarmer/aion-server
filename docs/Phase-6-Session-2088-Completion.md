# Phase 6 Session 2088 Completion - Find Group Declined Application Direct Dispatch Evidence

Date: 2026-06-01
Unit of Work: UOW-2088
Status: Completed

## Scope

- Inspected Java `FindGroupService.sendInstanceApplicationResult` declined branch.
- Extended the disabled instance-application direct dispatch evidence to cover declined action 12 `SM_MESSAGE` whisper intents as well as action 11 `SM_FIND_GROUP(applicant)` intents.
- Updated readiness and boundary aggregate reporting to describe both direct packet paths.
- Preserved live `CM_FIND_GROUP` deferral; no packet was sent through the connection registry.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `sendInstanceApplicationResult` resolves the applicant with `World.getInstance().getPlayer(applicantId)`.
  - When `instanceApplicationReply != 1`, Java sends `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` directly to the applicant.
  - If the applicant is missing, Java performs no send.

## What Changed

- Updated `FindGroupInstanceApplicationDirectDispatchPlanService`.
  - Clarified that it covers direct packet intents from both `sendInstanceApplication` and declined `sendInstanceApplicationResult`.
  - Still resolves intended recipients through a supplied resolver and returns audit-only evidence.
  - Still always returns `DispatchLiveSideEffects = false`.
- Added focused test coverage for declined action 12 direct whisper intent.
- Updated readiness reporting:
  - `FindGroupLiveDispatchReadinessReportService` now states action 11 and declined action 12 direct packet dispatch share disabled executor evidence.
  - `FindGroupConnectionBoundaryReadinessAggregateService` now names the broader instance-application direct packet executor evidence.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Result: passed, 67 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added C# disabled executor evidence around reviewed Java declined action 12 source behavior. It did not change Java packet parsing, Java runtime behavior, or a Java-executable parity target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: disabled executor/reporting only; no live dispatch, connection registry send, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch behavior changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` declined branch | `Aion.GameServer.Services.FindGroupInstanceApplicationDirectDispatchPlanService` | Disabled Executor / Side-Effect Boundary | Partial | Unit Tested | Partial Parity | C# can consume the declined action 12 direct `SmMessage` intent, resolve the intended applicant recipient, and audit the planned whisper without live dispatch. It does not send through the connection registry. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` direct instance-application readiness | `Aion.GameServer.Services.FindGroupLiveDispatchReadinessReportService`; `FindGroupConnectionBoundaryReadinessAggregateService` | Readiness Report | Partial | Unit Tested | Partial Parity | Readiness reports now describe action 11 and declined action 12 direct packet executor evidence while preserving live-dispatch blockers. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupInstanceApplicationDirectDispatchPlanServiceTests.CreateDisabledPlan_DeclinedActionTwelveWhisperIntentIsPlannedWithoutLiveDispatch` | Unit | Java `FindGroupService.sendInstanceApplicationResult` declined branch source review | Declined action 12 direct `SmMessage` whisper intent resolves to an audit record without live dispatch | Focused C# unit test | Does not execute real socket send or Java runtime |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 1.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The direct dispatch service is disabled and does not call the connection registry.
- Direct packet sends, world race-filter fanout, encrypted socket behavior, real-client behavior, visibility filtering, and concurrency remain unverified.
- Future live dispatch still needs an opt-in executor with connection-registry tests before wiring to the packet boundary.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupInstanceApplicationDirectDispatchPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupInstanceApplicationDirectDispatchPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `docs/Phase-6-Session-2088-Completion.md`
- `docs/Phase-6-Session-2088-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: design opt-in connection-registry direct-send/world-broadcast executor tests for Find Group side effects without wiring them into `CM_FIND_GROUP`.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Inspect whether `showInstanceGroupMembersInfo` action 15 should receive a direct-dispatch disabled executor slice or remain covered by the generic side-effect audit.
