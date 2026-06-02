# Phase 6 Session 2141 Completion - FindGroup Action 12 Alliance Invite Boundary Trace

Date: 2026-06-02
Unit of Work: UOW-2141
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for Java `CM_FIND_GROUP` action `12` accepted alliance-invite dispatch ordering.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `12` reads `playerOrTeamId` and `instanceApplicationReply`.
- `CM_FIND_GROUP.runImpl` action `12` calls `FindGroupService.getInstance().sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- `FindGroupService.sendInstanceApplicationResult` resolves the applicant through `World.getInstance().getPlayer(applicantId)`.
- For accept reply `1`, Java checks the responder instance group and calls `PlayerAllianceService.inviteToAlliance(responder, applicant)` when `minMembers > 6`.
- For decline replies, Java sends `new SM_MESSAGE(responder, ChatUtil.l10n(1400217), ChatType.WHISPER)` to the applicant.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added an ordered disabled-boundary trace to the existing action `12` alliance-accept boundary test.
- The trace records disabled `CM_FIND_GROUP` action `12` boundary acceptance before the opt-in alliance invite request evidence.
- Updated `FindGroupLiveDispatchGoNoGoChecklistService` to name action `12` boundary-acceptance-before-group/alliance-invite traces while keeping the action `12` live invite gate as evidence-available, not ready.
- Updated `FindGroupConnectionBoundaryReadinessAggregateService` to include both group and alliance disabled boundary traces in the action `12` invite executor evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupInstanceApplicationInviteDispatchPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

Result:

- Passed: 53
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in `SmSystemMessage`, `GameServerConnection`, existing tests, and `ProfessionFormulaServiceTests`.

Full .NET validation:

- Skipped intentionally. This was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.
- Filtered `dotnet test` compiled the affected project and directly adjacent FindGroup boundary/invite/readiness surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `12` and `FindGroupService.sendInstanceApplicationResult` logic as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `12` disabled boundary can be parsed/composed and now records accept-boundary ordering before opt-in group and alliance invite request evidence. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplicationResult` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplicationResult` | Service Method | Partial | Unit Tested | Partial Parity | Accepted group invite, accepted alliance invite, declined whisper, missing applicant, and missing instance-group branches have focused C# evidence. Live invite mutation and packet/question ordering remain unverified. |
| `com.aionemu.gameserver.services.player.PlayerAllianceService.inviteToAlliance` | `Aion.GameServer.Services.PlayerAllianceInviteRequestService.SendInvite` | Service Method | Partial | Unit Tested | Partial Parity | Opt-in action `12` alliance invite request evidence records the alliance question code after disabled boundary acceptance. This is not live `CM_FIND_GROUP` execution. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTwelveAcceptAllianceUsesConnectionResolverAndRuntimesWithoutLiveDispatch` | Unit | Java `CM_FIND_GROUP.runImpl` action `12`, `FindGroupService.sendInstanceApplicationResult`, and `PlayerAllianceService.inviteToAlliance` source review | Disabled boundary action `12` acceptance is recorded before opt-in alliance invite request evidence, and no live socket sends occur. | Focused connection-boundary trace based on Java branch order and accepted alliance invite branch. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_SeparatesEvidenceAvailableGatesFromReadyGates` | Unit | Java source review plus C# focused boundary fixtures | Go/no-go checklist separates disabled action `12` group/alliance invite trace evidence from missing live invite request mutation and packet/question ordering proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `12` accepted group/alliance invite ordering evidence is disabled-boundary plus opt-in request evidence, not live socket evidence.
- Direct packet and world-broadcast evidence remains non-live until `ProcessPacketAsync` is wired.
- Action `12` live invite mutation and packet/question ordering still need live boundary evidence before dispatch can be enabled.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `4` show applications, including disabled boundary acceptance before the direct `SmFindGroup` packet to the active player.

Safe candidates:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `8` register instance group.
- Add focused live-boundary readiness for missing-recipient direct-packet failures.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2141-Completion.md`
- `docs/Phase-6-Session-2141-Handoff.md`
