# Phase 6 Session 2151 Completion - FindGroup Action 6 Posted Message Trace

Date: 2026-06-02
Unit of Work: UOW-2151
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for Java `CM_FIND_GROUP` action `6` direct-packet dispatch ordering.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `6` reads `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`.
- `CM_FIND_GROUP.runImpl` action `6` ignores `playerOrTeamId` and calls `FindGroupService.getInstance().addApplication(player, message, groupType, classId, level)`.
- `FindGroupService.addApplication` stores the application by active player object id.
- Java sends `SM_SYSTEM_MESSAGE.STR_PARTY_MATCH_SEEK_PARTY_POSTED()` directly to the triggering player, then calls `showApplications(player)`.
- `showApplications(player)` sends `new SM_FIND_GROUP(4, applications)` directly to the triggering player.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added disabled-boundary-plus-opt-in trace coverage for action `6`.
- The focused trace records disabled `CM_FIND_GROUP` action `6` boundary acceptance, then opt-in direct `SmSystemMessage`, then opt-in direct `SmFindGroup` action `4` show-list execution.
- Updated direct-packet boundary and trigger-ordering readiness reports to include actions `0`, `2`, `4`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, and `17`.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence and `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Validation decision:

- Changed surface: focused tests, readiness-report text/enums, and non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore
```

Result:

- Passed: 102
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `6` and `FindGroupService.addApplication` logic as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `6` disabled boundary can be parsed/composed and opt-in executed in ordered direct-packet traces. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.AddApplication` | Service Method | Partial | Unit Tested | Partial Parity | Adds active-player application, records posted system message, and composes refreshed action `4` show-list packet in Java order. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in action `6` registry direct sends record posted `SmSystemMessage` before refreshed `SmFindGroup` after disabled boundary acceptance. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionSixCanProduceOrderedOptInPostedMessageBeforeShowListTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `6`, `FindGroupService.addApplication`, `FindGroupService.showApplications`, and `PacketSendUtility.sendPacket` source review | Disabled boundary action `6` acceptance is recorded before opt-in posted message and refreshed show-list direct-packet execution; posted message precedes refreshed list. | Focused connection-boundary and executor trace based on Java branch order and direct send target. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java source review plus C# focused boundary fixtures | Readiness report separates Java action `0`/`2`/`4`/`6`/`8`/`9`/`10`/`11`/`13`/`15`/`17` direct-send review and C# disabled trace evidence from missing live proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketTriggerOrderingReadinessServiceTests.CreateReport_SeparatesJavaAndOptInExecutorEvidenceFromLiveBoundaryProof` | Unit | Java `AionClientPacket.run` and `CM_FIND_GROUP.runImpl` source review | Trigger-ordering readiness records disabled action `0`/`2`/`4`/`6`/`8`/`9`/`10`/`11`/`13`/`15`/`17` boundary trace evidence while keeping live boundary proof blocked. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `6` direct-packet ordering evidence is disabled-boundary plus opt-in registry evidence, not live socket evidence.
- Direct packet and world-broadcast evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary readiness for missing-recipient direct-packet failures, starting with action `11` missing recruiter resolution and documenting the no-send outcome against Java `World.getPlayer(playerOrTeamId)` behavior.

Safe candidates:

- Add live connection-boundary world-broadcast fanout readiness for actions `1` or `5`.
- Add action `12` live invite dispatch failure/result handling evidence.
- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
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
- `docs/Phase-6-Session-2151-Completion.md`
- `docs/Phase-6-Session-2151-Handoff.md`
