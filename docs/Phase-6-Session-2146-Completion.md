# Phase 6 Session 2146 Completion - FindGroup Action 10 Direct Packet Trace

Date: 2026-06-02
Unit of Work: UOW-2146
Status: Completed

## Scope

This unit added focused, non-live connection-boundary readiness evidence for Java `CM_FIND_GROUP` action `10` direct-packet dispatch ordering.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.runImpl` action `10` calls `FindGroupService.getInstance().showInstanceGroups(player, false)`.
- `FindGroupService.showInstanceGroups(player, false)` filters server-wide instance groups to the player's race.
- When `GroupConfig.FORM_INSTANCE_GROUP_ANYWHERE` is enabled and this is not an update, Java sends `new SM_FIND_GROUP(instanceMaskIds)` before the action `10` show-list packet.
- Java always sends `new SM_FIND_GROUP(10, instanceGroups)` after the optional mask-list packet.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added a disabled-boundary-plus-opt-in direct-packet trace for normal action `10` show-list dispatch.
- Added a disabled-boundary-plus-opt-in trace for form-anywhere action `10`, proving optional action `26` mask-list intent is recorded and opt-in executed before the action `10` show-list intent.
- Extended the local connection-boundary test fixture to pass optional `GameServerOptions` and `AutoGroupTable` into the non-live composition service.
- Updated direct-packet boundary and trigger-ordering readiness reports to include actions `0`, `4`, `8`, `10`, `11`, and `15`.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence and `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Validation decision:

- Changed surface: test/readiness-report plus non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore
```

Result:

- Passed: 79
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `10` and `FindGroupService.showInstanceGroups` logic as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `10` disabled boundary can be parsed/composed and opt-in executed in ordered direct-packet traces. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroups` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupsForClient` | Service Method | Partial | Unit Tested | Partial Parity | Action `10` filters same-race instance groups and can compose optional action `26` mask-list before action `10` show-list when form-anywhere is enabled. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in action `10` registry direct sends record mask-list before show-list when applicable and otherwise show-list only. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTenCanProduceOrderedOptInDirectPacketTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `10`, `FindGroupService.showInstanceGroups`, and `PacketSendUtility.sendPacket` source review | Disabled boundary action `10` acceptance is recorded before opt-in action `10` show-list direct-packet execution to the active viewer. | Focused connection-boundary and executor trace based on Java branch order and direct send target. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionTenFormAnywhereCanProduceOrderedMaskThenShowTrace` | Unit | Java `FindGroupService.showInstanceGroups(player, false)` source review | Disabled boundary action `10` acceptance is recorded before opt-in action `26` mask-list direct send, which is recorded before the action `10` show-list direct send. | Focused connection-boundary and executor trace based on Java optional mask-list-before-show-list order. | Does not prove live `ProcessPacketAsync`, encrypted socket, real-client ordering, target-NPC runtime resolution, or Java `DataManager.AUTO_GROUP` parity beyond the injected table fixture. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java source review plus C# focused boundary fixtures | Readiness report separates Java action `0`/`4`/`8`/`10`/`11`/`15` direct-send review and C# disabled trace evidence from missing live proof. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |
| `FindGroupDirectPacketTriggerOrderingReadinessServiceTests.CreateReport_SeparatesJavaAndOptInExecutorEvidenceFromLiveBoundaryProof` | Unit | Java `AionClientPacket.run` and `CM_FIND_GROUP.runImpl` source review | Trigger-ordering readiness records disabled action `0`/`4`/`8`/`10`/`11`/`15` boundary trace evidence while keeping live boundary proof blocked. | Focused readiness-report test. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `10` direct-packet ordering evidence is disabled-boundary plus opt-in registry evidence, not live socket evidence.
- Form-anywhere mask-list evidence uses injected C# `AutoGroupTable` data and does not prove live `DataManager.AUTO_GROUP` parity.
- Direct packet and world-broadcast evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused direct-action disabled-boundary-plus-opt-in ordered trace evidence for action `13` instance-group update show list, proving update requests emit the action `10` show-list direct packet and do not emit action `26`.

Safe candidates:

- Add focused live-boundary readiness for missing-recipient direct-packet failures.
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
- `docs/Phase-6-Session-2146-Completion.md`
- `docs/Phase-6-Session-2146-Handoff.md`
