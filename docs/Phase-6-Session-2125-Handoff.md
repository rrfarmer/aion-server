# Phase 6 Session 2125 Handoff - FindGroup Action 17 Missing Update Evidence

Date: 2026-06-02
Unit of Work: UOW-2125
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

Focused validation is the default. Do not run the broad .NET suite or full solution build without a documented broad-validation trigger.

Before expensive commands, record:

- Changed surface.
- Exact focused C#, Java/Maven, or hygiene command.
- Java/Maven command or the reason it is unavailable/not relevant.
- Broad-validation trigger, or `none`.
- Broad .NET suite/build decision.

## Current Phase Context

- Phase 6 remains in progress.
- Java remains the source of truth for behavior, packet layouts, side effects, guard order, persistence, concurrency, and runtime service semantics.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live `CmFindGroup` boundary plans from injected composition/adapter services, but it is not invoked by live packet processing.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, disabled-boundary action `2`/`6` posted-message-before-refresh order, disabled-boundary action `10` action-26-before-action-10 order, and disabled-boundary action `13` no-action-26 update evidence.
- Action `12` disabled boundary evidence covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, missing responder instance-group branches, and the inner branch status surfaced on the boundary intent plan.
- Action `17` disabled boundary evidence now surfaces instance-group mutation status and covers missing instance-group no-side-effect behavior.
- `PHASE-6-PROGRESS.md` should remain untouched in normal sessions.

## Latest Completed Work

- UOW-2100: non-live `CM_FIND_GROUP` dispatch adapter/result surface added.
- UOW-2101: `FindGroupRecruitmentPlanService` state stores aligned with Java concurrent map shape.
- UOW-2102: Java `FindGroupService.getInstance` lifecycle call-site readiness inventory added.
- UOW-2103: non-live group/alliance disband recruitment cleanup evidence added.
- UOW-2104: injected connection wiring evidence added for group/alliance joined-team cleanup.
- UOW-2105: production DI singleton graph evidence added for FindGroup joined-team/disband callers.
- UOW-2106: logout cleanup now uses the injected shared FindGroup service without requiring observer activation.
- UOW-2107: active docs now require focused validation decisions before expensive broad .NET commands.
- UOW-2108: `GameServerConnection` can consume injected non-live FindGroup composition/adapter services to create disabled boundary plans.
- UOW-2109: focused `onJoinedTeam` mutation-priority evidence added for leader solo-recruitment re-add over full-team removal.
- UOW-2110: opt-in FindGroup side-effect executor now records direct/broadcast execution order.
- UOW-2111: disabled action `12` accepted group invite connection-helper evidence added.
- UOW-2112: disabled action `12` accepted alliance invite connection-helper evidence added.
- UOW-2113: disabled action `12` declined whisper connection-helper evidence added.
- UOW-2114: disabled action `12` missing applicant and missing instance-group connection-helper evidence added.
- UOW-2115: show-list plans now have materialized snapshot evidence.
- UOW-2116: group joined-team recorder ordering evidence added.
- UOW-2117: alliance joined-team recorder ordering evidence added.
- UOW-2118: disabled client-action-to-logout singleton cleanup evidence added.
- UOW-2119: disabled client-action-to-group-disband singleton cleanup evidence added.
- UOW-2120: disabled client-action-to-alliance-disband singleton cleanup evidence added.
- UOW-2121: action `12` disabled boundary intent plan now exposes branch status for accepted group, accepted alliance, declined, missing applicant, and missing instance group.
- UOW-2122: disabled boundary execution order now records action `2`/`6` posted message before refreshed show-list direct packet.
- UOW-2123: disabled boundary execution order now records action `10` form-anywhere action `26` mask list before action `10` instance-group show list.
- UOW-2124: disabled boundary action `13` update evidence now records no action `26` mask-list packet.
- UOW-2125: disabled boundary action `17` now exposes instance-group mutation status and records missing-instance-group no-side-effect behavior.

## Current UOW Commit Message

- `[Phase 6][UOW-2125] Add find group action 17 missing update evidence`

## Validation In UOW-2125

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: 65 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `17` and `FindGroupService.updateInstanceGroup`; no focused Java test target was identified for this disabled C# boundary status/no-side-effect evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `17` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceGroupStatus`; `FindGroupConnectionBoundarySideEffectCompositionEvidenceService.ExecuteOptInAsync` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves `Updated` and `Missing` action `17` mutation statuses at the disabled boundary. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.updateInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.UpdateInstanceGroup`; disabled boundary execution plan | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers Java's missing-instance-group no-op branch: no direct packets, no broadcasts, and no executor order entries. Live socket behavior and Java runtime trace remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `17` boundary status/no-op behavior is now visible through the disabled boundary plan, but live socket behavior remains unverified.
- Java runtime/socket trace and packet-byte comparison were not produced in UOW-2125.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add disabled-boundary status evidence for action `9` missing remove-instance-group no-side-effect/update behavior if not already explicit.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.

## Files Changed In UOW-2125

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2125-Completion.md`
- `docs/Phase-6-Session-2125-Handoff.md`
