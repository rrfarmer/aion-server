# Phase 6 Session 2155 Completion - FindGroup Action 12 Missing Runtime No-Mutation Evidence

Date: 2026-06-02
Unit of Work: UOW-2155
Status: Completed

## Scope

This unit added focused, non-live readiness evidence for Java `CM_FIND_GROUP` action `12` accepted invite handling when the disabled C# boundary lacks the runtime services required to compose the invite request.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `12` reads `playerOrTeamId` and `instanceApplicationReply`.
- `CM_FIND_GROUP.runImpl` action `12` calls `FindGroupService.getInstance().sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- Java resolves the applicant with `World.getInstance().getPlayer(applicantId)`.
- Accepted replies only continue when the applicant exists and the responder has a registered instance group.
- Accepted group invites call `PlayerGroupService.inviteToGroup(responder, applicant)` when `minMembers <= 6`.
- Accepted alliance invites call `PlayerAllianceService.inviteToAlliance(responder, applicant)` when `minMembers > 6`.
- Declined replies send `SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` to the applicant when the applicant exists.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Tightened the action `12` missing-invite-runtime adapter test to prove no invite request mutation occurs when the disabled adapter blocks for missing resolver/runtime dependencies.
- Updated `FindGroupLiveDispatchReadinessReportService` to record action `12` missing-runtime no-mutation evidence.
- Updated `FindGroupConnectionBoundaryReadinessAggregateService` to include the same missing-runtime no-mutation evidence in the top-level boundary readiness report.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to distinguish missing invite runtime from accepted invite, declined whisper, missing applicant, and missing instance group branches.

## Validation

Validation decision:

- Changed surface: focused tests, readiness-report text, aggregate-readiness text, and non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore
```

Result:

- Passed: 84
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `12`, `FindGroupService.sendInstanceApplicationResult`, `PlayerGroupService.inviteToGroup`, and `PlayerAllianceService.inviteToAlliance` behavior as the oracle. No narrow Java test target was identified for this non-live C# adapter/readiness fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `12` disabled boundary can parse accepted and declined replies and surface accepted invite intent/missing-runtime blockers. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult` | Service Method | Partial | Unit Tested | Partial Parity | Accepted, declined, missing applicant, and missing instance group branches are planned without live dispatch; invite runtime composition is still disabled-boundary evidence only. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.inviteToGroup` | `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService.CreateDisabledPlan` | Service Boundary | Partial | Unit Tested | Partial Parity | Disabled group invite request can be composed when runtimes exist; missing-runtime adapter blocks before mutating applicant request state. Live request dispatch remains blocked. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.inviteToAlliance` | `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService.CreateDisabledPlan` | Service Boundary | Partial | Unit Tested | Partial Parity | Disabled alliance invite request can be composed when runtimes exist; missing-runtime adapter blocks before mutating applicant request state. Live request dispatch remains blocked. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live action `12` invite dispatch remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupConnectionBoundaryDispatchAdapterServiceTests.CreateDisabledPlan_ActionTwelveInviteWithoutRuntimeRecordsBlockedMissingRuntime` | Unit | Java `FindGroupService.sendInstanceApplicationResult`, `PlayerGroupService.inviteToGroup`, and `PlayerAllianceService.inviteToAlliance` source review | Accepted action `12` invite intent is surfaced, missing disabled-runtime dependencies block adapter composition, and applicant invite request state is not mutated. | Focused disabled-adapter failure-result assertion. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client invite behavior. |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_ActionTwelveKeepsInviteSideEffectGateExplicit` | Unit | Java source review plus C# disabled fixtures | Readiness report records action `12` invite dispatch evidence and missing-runtime no-mutation blocker. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupConnectionBoundaryReadinessAggregateServiceTests.CreateReport_AggregatesDisabledPlannerExecutorAuditAndLifecycleEvidence` | Unit | Java source review plus C# disabled fixtures | Aggregate boundary readiness report carries action `12` missing-runtime no-mutation evidence. | Focused aggregate-readiness assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `12` invite evidence is disabled-boundary evidence, not live invite request dispatch.
- Direct packet and world-broadcast action evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists, starting with shared singleton ordering across `CM_FIND_GROUP`, logout cleanup, joined-team cleanup, and group/alliance disband cleanup without enabling live `CM_FIND_GROUP` dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add focused go/no-go checklist coverage for the remaining live-dispatch blockers before any `ProcessPacketAsync` wiring attempt.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryDispatchAdapterServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2155-Completion.md`
- `docs/Phase-6-Session-2155-Handoff.md`
