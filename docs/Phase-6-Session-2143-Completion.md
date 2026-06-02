# Phase 6 Session 2143 Completion - FindGroup Action 8 Direct Packet Trace

Date: 2026-06-02
Unit of Work: UOW-2143
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for Java `CM_FIND_GROUP` action `8` direct-packet dispatch ordering.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `8` reads `instanceMaskId`, an ignored byte, `message`, and `minMembers`.
- `CM_FIND_GROUP.runImpl` action `8` calls `FindGroupService.getInstance().registerInstanceGroup(player, instanceMaskId, message, minMembers)`.
- `FindGroupService.registerInstanceGroup` creates a `ServerWideGroup`, stores it by `player.getObjectId()`, and calls `PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(14, List.of(instanceGroup)))`.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added a disabled-boundary-plus-opt-in direct-packet trace for action `8`.
- The focused trace records disabled `CM_FIND_GROUP` action `8` boundary acceptance before opt-in registry execution of the direct `SmFindGroup` action `14` packet to the active player.
- The action `8` test also verifies the non-live service state stores the expected recruiter, instance mask, minimum member count, and message.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to record Java action `8` direct-send review and C# disabled action `8` composition evidence.
- Updated `FindGroupDirectPacketTriggerOrderingReadinessService` and `FindGroupLiveDispatchReadinessReportService` to describe direct-packet boundary trace evidence for actions `0`, `4`, and `8`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests" --no-restore
```

Result:

- Passed: 53
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Full .NET validation:

- Skipped intentionally. This was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.
- Filtered `dotnet test` compiled the affected project and directly adjacent FindGroup boundary/direct-packet/readiness surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `8` and `FindGroupService.registerInstanceGroup` logic as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `8` disabled boundary can be parsed/composed and opt-in executed in an ordered direct-packet trace. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.registerInstanceGroup` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RegisterInstanceGroup` | Service Method | Partial | Unit Tested | Partial Parity | Register-instance-group stores the instance group and creates a direct `SmFindGroup` action `14` intent to the active player. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in action `8` registry direct send records the `SmFindGroup` packet after disabled boundary acceptance. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionEightCanProduceOrderedOptInDirectPacketTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `8`, `FindGroupService.registerInstanceGroup`, and `PacketSendUtility.sendPacket` source review | Disabled boundary action `8` acceptance is recorded before opt-in direct-packet execution to the active player, and the non-live service state contains the registered instance group. | Focused connection-boundary and executor trace based on Java branch order, direct send target, and stored state. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java source review plus C# focused boundary fixtures | Readiness report separates Java action `0`/`4`/`8` direct-send review and C# disabled trace evidence from missing live proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `8` direct-packet ordering evidence is disabled-boundary plus opt-in registry evidence, not live socket evidence.
- Direct packet and world-broadcast evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `11` send instance application, including disabled boundary acceptance before the direct `SmFindGroup` applicant packet to the resolved recruiter.

Safe candidates:

- Add focused live-boundary readiness for missing-recipient direct-packet failures.
- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `15` instance-group member-info when a registered target exists.
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
- `docs/Phase-6-Session-2143-Completion.md`
- `docs/Phase-6-Session-2143-Handoff.md`
