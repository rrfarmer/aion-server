# Phase 6 Session 2141 Handoff - FindGroup Action 12 Alliance Invite Boundary Trace

Date: 2026-06-02
Unit of Work: UOW-2141
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite or full solution build unless a documented broad-validation trigger applies. Each completion/handoff should record the exact focused command and the reason broad validation was skipped or run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `GameServerConnection.CreateDisabledFindGroupBoundaryPlan` can compose non-live boundary plans but is not invoked live.
- Action `0` has disabled-boundary-plus-opt-in direct-packet execution trace evidence.
- Actions `1` and `5` have disabled-boundary-plus-opt-in world-broadcast fanout trace evidence.
- Action `12` accepted group and alliance replies now have disabled-boundary-plus-opt-in invite-request ordering trace evidence.
- Action `12` declined whisper, missing applicant, missing instance group, and missing invite player cases have focused disabled evidence.
- `FindGroupSideEffectDispatchExecutorService` records opt-in direct/broadcast execution order, but not live boundary execution.
- `FindGroupDirectPacketTriggerOrderingReadinessService` blocks live direct-packet ordering claims.
- `FindGroupWorldBroadcastFanoutReadinessService` blocks live world-broadcast fanout claims for actions `1` and `5`.
- `FindGroupConcurrentMutationOrderingReadinessService` records deterministic shared-singleton interleaving fixtures but still blocks live singleton interleaving claims.
- `PHASE-6-PROGRESS.md` remained untouched.

## UOW-2141 Summary

This UOW added focused readiness evidence for Java `CM_FIND_GROUP` action `12` accepted alliance invite:

- Java action `12` calls `FindGroupService.sendInstanceApplicationResult(responder, applicantId, reply)`.
- Java accept reply `1` resolves the applicant, checks the responder instance group, then calls `PlayerAllianceService.inviteToAlliance` when `minMembers > 6`.
- C# disabled boundary composes action `12` as an accepted alliance invite intent.
- The focused test records disabled action `12` acceptance before the opt-in alliance invite request evidence and keeps live dispatch disabled.
- Readiness reports now distinguish group/alliance trace evidence from still-missing live `ProcessPacketAsync` invite mutation and packet/question ordering proof.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult`
- `com.aionemu.gameserver.services.player.PlayerAllianceService.inviteToAlliance`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup`
- `Aion.GameServer.Network.Aion.GameServerConnection`
- `Aion.GameServer.Services.FindGroupRecruitmentPlanService`
- `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService`
- `Aion.GameServer.Services.FindGroupConnectionBoundaryReadinessAggregateService`
- `Aion.GameServer.Services.FindGroupLiveDispatchGoNoGoChecklistService`
- `Aion.GameServer.Services.PlayerAllianceInviteRequestService`
- `Aion.GameServer.Tests.GameServerConnectionFindGroupBoundaryTests`

## Validation In UOW-2141

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

Result:

- Passed: 53
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

Full .NET validation was skipped intentionally because this was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.

Java/Maven validation was not run because no Java source changed and no narrow executable Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `12` disabled boundary can be parsed/composed and now records accept-boundary ordering before opt-in group and alliance invite request evidence. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult` | Service Method | Partial | Unit Tested | Partial Parity | Accepted group invite, accepted alliance invite, declined whisper, missing applicant, and missing instance-group branches have focused C# evidence. Live invite mutation and packet/question ordering remain unverified. |
| `com.aionemu.gameserver.services.player.PlayerAllianceService.inviteToAlliance` | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` | Service Method | Partial | Unit Tested | Partial Parity | Opt-in action `12` alliance invite request evidence records the alliance question code after disabled boundary acceptance. This is not live `CM_FIND_GROUP` execution. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Known Gaps

- Live `CM_FIND_GROUP` execution is still disabled.
- No live `ProcessPacketAsync` trace proves action `12` invite request mutation or packet/question ordering relative to a triggering live `CM_FIND_GROUP` client packet.
- Direct packet and world-broadcast action evidence remains non-live until `ProcessPacketAsync` is wired.
- No encrypted socket or real-client comparison covers FindGroup.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `4` show applications, including disabled boundary acceptance before the direct `SmFindGroup` packet to the active player.

Safe candidates:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `8` register instance group.
- Add focused live-boundary readiness for missing-recipient direct-packet failures.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2141

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2141-Completion.md`
- `docs/Phase-6-Session-2141-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2141] Add find group action twelve alliance trace
```
