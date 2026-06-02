# Phase 6 Session 2119 Handoff - FindGroup Group Disband Cross-Caller Evidence

Date: 2026-06-02
Unit of Work: UOW-2119
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
- Action `12` disabled connection-helper evidence covers accepted group invite, accepted alliance invite, declined whisper, missing applicant, and missing responder instance-group branches.
- UOW-2115 added materialized snapshot evidence for recruitment/application/instance-group show plans.
- UOW-2116 added group joined-team recorder ordering evidence after runtime membership mutation.
- UOW-2117 added alliance joined-team recorder ordering evidence after runtime membership mutation.
- UOW-2118 added disabled client-action-to-logout singleton cleanup evidence.
- UOW-2119 added disabled client-action-to-group-disband singleton cleanup evidence.
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

## Current UOW Commit Message

- `[Phase 6][UOW-2119] Add find group group-disband singleton cleanup evidence`

## Validation In UOW-2119

- Focused C# passed:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerGroupRuntimeTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Final result: 95 tests passed.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven was not run:
  - No Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl`, `PlayerGroupService.disband`, and `FindGroupService.removeRecruitment`; no focused Java test target was identified for this disabled C# singleton-cleanup evidence.
- Broad .NET validation was skipped:
  - Broad-validation trigger: none.
  - The UOW did not enable live `CmFindGroup`, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `2` | `Aion.GameServer.Services.FindGroupClientActionPlanService.Plan`; `FindGroupRecruitmentPlanService` | Client Action Planner | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled planner mutation of team recruitment singleton state consumed by group disband cleanup. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Lifecycle Runtime | Partial | Unit Tested | Partial Parity | Focused evidence covers disband cleanup against recruitment state created through the same injected FindGroup service. Live packet fanout, Java runtime trace, and full group event replay remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers team-id keyed removal and race-filtered broadcast intent planning for group disband cleanup. Live dispatch and concurrent singleton callers remain unverified. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled client-action-to-group-disband singleton cleanup now has focused evidence, but live socket dispatch, Java runtime traces, real-client behavior, full group event replay, and concurrent singleton mutation ordering remain unverified.
- Broad .NET suite/build was not run in UOW-2119 because no broad-validation trigger applied.

## Next Recommended Unit of Work

- Next sequential task: add focused cross-caller evidence for alliance disband cleanup against FindGroup state created through the disabled client-action planner, matching Java `PlayerAllianceService.disband`.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.

## Files Changed In UOW-2119

- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerGroupRuntimeTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2119-Completion.md`
- `docs/Phase-6-Session-2119-Handoff.md`
