# Phase 6 Session 2137 Completion - FindGroup Action 0 Direct Boundary Trace Readiness

Date: 2026-06-02
Unit of Work: UOW-2137
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for the simplest direct-packet branch: Java `CM_FIND_GROUP` action `0`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.runImpl` action `0` calls `FindGroupService.getInstance().showRecruitments(player)`.
- `FindGroupService.showRecruitments(player)` filters same-race recruitment entries and sends `SM_FIND_GROUP` action `0` directly to the triggering player.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added `FindGroupDirectPacketBoundaryTraceReadinessService`.
- Added focused tests for the readiness report.
- Added a connection-boundary test proving disabled `CM_FIND_GROUP` action `0` acceptance can be recorded before opt-in registry execution of the direct `SmFindGroup` packet to the active player.
- Updated `FindGroupDirectPacketTriggerOrderingReadinessService` to include the new disabled action `0` trace evidence.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests" --no-restore
```

Result:

- Passed: 26
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in `SmSystemMessage`, `GameServerConnection`, several existing tests, and `ProfessionFormulaServiceTests`.

Full .NET validation:

- Skipped intentionally. This was a focused readiness-report and connection-boundary test unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.
- Filtered `dotnet test` compiled the affected project and directly adjacent FindGroup boundary/executor surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `0` and `FindGroupService.showRecruitments` logic as the oracle. No narrow Java test target was identified for this non-live C# readiness-report and boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `0` disabled boundary can be parsed/composed and opt-in executed in an ordered trace. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showRecruitments` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowRecruitments` | Service Method | Partial | Unit Tested | Partial Parity | Same-race direct `SmFindGroup` action `0` intent evidence exists and can be opt-in executed through `IGameClientConnectionRegistry`. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in direct registry send order is recorded for action `0`; this does not prove live `GameServerConnection.SendPacketAsync` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionZeroCanProduceOrderedOptInDirectPacketTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `0` and `FindGroupService.showRecruitments` source review | Disabled boundary action `0` acceptance is recorded before opt-in registry execution of direct `SmFindGroup` to the active player. | Focused connection-boundary and executor test based on Java branch order. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java source review plus C# focused boundary fixture | Readiness report separates Java action `0` direct-send evidence and C# disabled-boundary trace evidence from missing live proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `0` has disabled-boundary-plus-opt-in execution evidence, not live socket evidence.
- World-broadcast fanout for actions `1` and `5` still needs live boundary evidence.
- Action `12` invite dispatch still needs live boundary evidence.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout without enabling broad live `CM_FIND_GROUP` dispatch.

Safe candidates:

- Add focused action `12` live invite-dispatch failure/result readiness.
- Add another simple direct-action disabled-boundary-plus-opt-in ordered trace, such as action `4` show applications or action `8` register instance group.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketTriggerOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketTriggerOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2137-Completion.md`
- `docs/Phase-6-Session-2137-Handoff.md`
