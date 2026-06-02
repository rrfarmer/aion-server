# Phase 6 Session 2152 Completion - FindGroup Action 11 Missing Recipient Boundary

Date: 2026-06-02
Unit of Work: UOW-2152
Status: Completed

## Scope

This unit added focused, non-live connection-boundary evidence for Java `CM_FIND_GROUP` action `11` when the requested recruiter/recipient is not online.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `11` reads `playerOrTeamId` and `instanceMaskId`.
- `CM_FIND_GROUP.runImpl` action `11` calls `FindGroupService.getInstance().sendInstanceApplication(player, playerOrTeamId)`.
- `FindGroupService.sendInstanceApplication` resolves the recruiter with `World.getInstance().getPlayer(playerOrTeamId)`.
- Java sends `new SM_FIND_GROUP(applicant)` only when the recruiter resolves to a non-null online player.
- Missing recruiter resolution produces no packet send and no side effects.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Added disabled-boundary trace coverage for action `11` missing recruiter resolution.
- The focused trace records disabled `CM_FIND_GROUP` action `11` boundary acceptance and then proves no direct packets, world broadcasts, executor sends, registry sends, or socket observer sends are produced.
- Surfaced `FindGroupInstanceApplicationPlanStatus.MissingRecipient` at the connection-boundary intent plan for this branch.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence to include action `11` missing-recipient no-send evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record action `11` resolved and missing-recipient disabled boundary evidence.

## Validation

Validation decision:

- Changed surface: focused tests, readiness-report text, and non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketTriggerOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupInstanceApplicationDirectDispatchPlanServiceTests" --no-restore
```

Result:

- Passed: 109
- Failed: 0
- Skipped: 0

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `11` plus `FindGroupService.sendInstanceApplication` and `World.getPlayer(playerOrTeamId)` behavior as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `11` disabled boundary can parse/compose both resolved-recipient and missing-recipient branches. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.sendInstanceApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.SendInstanceApplication` | Service Method | Partial | Unit Tested | Partial Parity | Resolved recruiter composes direct `SmFindGroup` applicant packet; missing recruiter records missing-recipient status and no side effects. |
| `com.aionemu.gameserver.world.World.getPlayer` | `Aion.GameServer.Network.Aion.GameServerConnection.ResolveOnlinePlayerByObjectId` | Boundary Resolver | Partial | Unit Tested | Partial Parity | Disabled boundary resolver can surface missing online player resolution for action `11`; live dispatch remains unwired. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Missing-recipient branch produces no intent; resolved branch can execute opt-in direct send. This does not prove live socket behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live trigger-order parity remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionElevenMissingRecipientRecordsNoSideEffects` | Unit | Java `CM_FIND_GROUP.runImpl` action `11`, `FindGroupService.sendInstanceApplication`, and `World.getPlayer(playerOrTeamId)` source review | Disabled boundary action `11` acceptance is recorded, missing recruiter status is surfaced, and no direct packet, world broadcast, registry send, executor send, or socket observer send occurs. | Focused connection-boundary and executor trace based on Java's null-recipient no-send branch. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client behavior. |
| `FindGroupLiveDispatchReadinessReportServiceTests.CreateReport_TracksObserverAndTransportEvidenceBeforeLiveEnablement` | Unit | Java source review plus C# focused boundary fixtures | Readiness report includes action `11` missing-recipient no-send observer evidence while continuing to block live dispatch claims. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `11` missing-recipient evidence is disabled-boundary evidence, not live socket evidence.
- Direct packet and world-broadcast evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add live connection-boundary world-broadcast fanout readiness for action `1` remove recruitment, proving disabled/non-live current state and preserving race-filtered same-race fanout expectations without enabling live dispatch.

Safe candidates:

- Add action `5` world-broadcast fanout live-boundary readiness.
- Add action `12` live invite dispatch failure/result handling evidence.
- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2152-Completion.md`
- `docs/Phase-6-Session-2152-Handoff.md`
