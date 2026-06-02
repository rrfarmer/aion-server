# Phase 6 Session 2138 Completion - FindGroup Action 1 World Broadcast Fanout Trace

Date: 2026-06-02
Unit of Work: UOW-2138
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for Java `CM_FIND_GROUP` action `1` world-broadcast fanout.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`

Java behavior:

- `CM_FIND_GROUP.runImpl` action `1` calls `FindGroupService.getInstance().removeRecruitment(player, serverId, unk1, unk2, unk3)`.
- `FindGroupService.removeRecruitment` removes the player/current-team recruitment and calls `PacketSendUtility.broadcastToWorld(..., p -> p.getRace() == recruitment.getRace())` only when a recruitment was removed.
- Java broadcast fanout includes players accepted by the race predicate and excludes players rejected by it.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added a connection-boundary test proving disabled `CM_FIND_GROUP` action `1` acceptance can be recorded before opt-in world-broadcast execution.
- The focused action `1` trace proves same-race recipients are included and opposite-race recipients are excluded through the opt-in registry executor.
- Updated `FindGroupWorldBroadcastFanoutReadinessService` to record disabled action `1` boundary fanout evidence while keeping live boundary fanout blocked.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

Result:

- Passed: 47
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in `SmSystemMessage`, `GameServerConnection`, several existing tests, and `ProfessionFormulaServiceTests`.

Full .NET validation:

- Skipped intentionally. This was a focused readiness-report and connection-boundary test unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.
- Filtered `dotnet test` compiled the affected project and directly adjacent FindGroup boundary/executor surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `1`, `FindGroupService.removeRecruitment`, and `PacketSendUtility.broadcastToWorld` logic as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `1` disabled boundary can be parsed/composed and opt-in executed in an ordered world-broadcast trace. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service Method | Partial | Unit Tested | Partial Parity | Removed recruitment creates a race-filtered world-broadcast intent and removes state. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in action `1` registry broadcast includes same-race recipients and excludes opposite-race recipients. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionOneCanProduceOrderedOptInWorldBroadcastFanoutTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `1`, `FindGroupService.removeRecruitment`, and `PacketSendUtility.broadcastToWorld` source review | Disabled boundary action `1` acceptance is recorded before opt-in world-broadcast execution; same-race recipients are included and opposite-race recipients are excluded. | Focused connection-boundary and executor test based on Java branch order and race predicate. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_SeparatesJavaRaceFilterAndCSharpExecutorEvidenceFromLiveProof` | Unit | Java source review plus C# focused boundary fixture | Readiness report separates Java race-filter evidence and C# disabled action `1` fanout trace evidence from missing live proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `1` has disabled-boundary-plus-opt-in fanout evidence, not live socket evidence.
- Action `5` still needs equivalent disabled-boundary fanout trace evidence.
- Action `12` invite dispatch still needs live boundary evidence.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused disabled-boundary-plus-opt-in fanout trace evidence for action `5` application removal, including same-race recipients and opposite-race exclusion.

Safe candidates:

- Add focused action `12` live invite-dispatch failure/result readiness.
- Add another simple direct-action disabled-boundary-plus-opt-in ordered trace, such as action `4` show applications or action `8` register instance group.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2138-Completion.md`
- `docs/Phase-6-Session-2138-Handoff.md`
