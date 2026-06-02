# Phase 6 Session 2121 Handoff - FindGroup Action 12 Boundary Status Evidence

Date: 2026-06-02
Unit of Work: UOW-2121
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
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct-packet and world-broadcast execution order, including direct-before-broadcast order, for future live-boundary audits.
- Action `12` disabled boundary evidence now covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, missing responder instance-group branches, and the inner branch status surfaced on the boundary intent plan.
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

## Current UOW Commit Message

- `[Phase 6][UOW-2121] Add find group action 12 boundary status evidence`

## Validation In UOW-2121

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Final result: 71 tests passed.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl` action `12` and `FindGroupService.sendInstanceApplicationResult`; no focused Java test target was identified for this disabled C# boundary status evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `12` | `Aion.GameServer.Services.FindGroupConnectionBoundarySideEffectIntentPlan.InstanceApplicationStatus`; `FindGroupConnectionBoundaryDispatchAdapterService` | Client Action Boundary | Partial | Unit Tested | Partial Parity | Focused evidence preserves action `12` branch status for accepted group invite, accepted alliance invite, declined, missing applicant, and missing instance group. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult`; boundary intent-plan status surface | Service Method | Partial | Unit Tested | Partial Parity | Existing planner behavior is now visible at the connection boundary. Live socket behavior, Java runtime trace, and packet-byte comparison remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Action `12` branch status is visible through the disabled boundary plan, but live boundary execution, real socket behavior, Java runtime traces, and packet-byte parity remain unverified.
- Broad .NET suite/build was not run in UOW-2121 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add packet-byte evidence for action `12` declined `SM_MESSAGE`, or add focused connection-registry ordering evidence for `FindGroupSideEffectDispatchExecutorService` under a disabled boundary plan.

Safe alternative candidates:

- Add focused Java/Maven parity fixture coverage for one executable FindGroup branch if a suitable Java test target can be identified.
- Add live-readiness failure-result tests for missing world recipients or skipped invite recipients before any `ProcessPacketAsync` wiring.
- Review multi-step mutation ordering under concurrent singleton callers before live dispatch.

## Files Changed In UOW-2121

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryDispatchAdapterServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2121-Completion.md`
- `docs/Phase-6-Session-2121-Handoff.md`
