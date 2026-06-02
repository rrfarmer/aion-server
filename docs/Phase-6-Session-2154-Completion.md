# Phase 6 Session 2154 Completion - FindGroup Action 5 Missing Application Fanout Boundary

Date: 2026-06-02
Unit of Work: UOW-2154
Status: Completed

## Scope

This unit added focused, non-live connection-boundary evidence for Java `CM_FIND_GROUP` action `5` remove-application behavior, including Java's missing-application no-broadcast branch.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `CM_FIND_GROUP.readImpl` action `5` reads `playerOrTeamId`.
- `CM_FIND_GROUP.runImpl` action `5` ignores the parsed `playerOrTeamId` and calls `FindGroupService.getInstance().removeApplication(player)`.
- `FindGroupService.removeApplication` removes by active player object id.
- Java broadcasts `new SM_FIND_GROUP(player.getObjectId())` only when the application existed.
- Java filters the broadcast with `p -> p.getRace() == application.getPlayer().getRace()`.
- Missing application removal sends no packet.

This UOW does not enable live `CM_FIND_GROUP` dispatch.

## Changes

- Surfaced `FindGroupApplicationPlanStatus` at the disabled connection-boundary intent plan.
- Tightened the existing action `5` removed-branch disabled-boundary test to assert `Removed` status.
- Added action `5` missing-application disabled-boundary evidence proving no direct packets, world broadcasts, executor sends, registry sends, or socket observer sends are produced.
- Updated `FindGroupWorldBroadcastFanoutReadinessService` to record action `5` removed-branch fanout evidence plus missing-branch no-send status evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to distinguish action `1`/`5` removed/missing branch evidence from still-missing live fanout proof.

## Validation

Validation decision:

- Changed surface: focused boundary intent status, focused tests, readiness-report text, and non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionFindGroupBoundaryTests|FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupConnectionBoundarySideEffectCompositionEvidenceServiceTests|FullyQualifiedName~FindGroupClientActionPlanServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~SmFindGroupTests" --no-restore
```

Result:

- Passed: 116
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `CM_FIND_GROUP` action `5`, `FindGroupService.removeApplication`, and `PacketSendUtility.broadcastToWorld` behavior as the oracle. No narrow Java test target was identified for this non-live C# boundary-trace fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.ClientPackets.CmFindGroup` | Client Packet | Partial | Unit Tested | Partial Parity | Action `5` disabled boundary can parse/compose removed and missing application branches. Live `ProcessPacketAsync` dispatch remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeApplication` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveApplication` | Service Method | Partial | Unit Tested | Partial Parity | Removed application composes race-filtered world broadcast; missing application records missing status and no side effects. Live socket ordering remains unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastToWorld` | `Aion.GameServer.Services.FindGroupSideEffectDispatchExecutorService.ExecuteAsync` | Dispatch Utility | Partial | Unit Tested | Partial Parity | Opt-in executor records same-race action `5` broadcast and opposite-race exclusion; missing branch has no broadcast intent. This does not prove live `GameServerConnection` behavior. |
| `com.aionemu.gameserver.network.aion.AionClientPacket.run` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java synchronously invokes `runImpl`; C# still defers `CmFindGroup` in `ProcessPacketAsync`, so live world-broadcast fanout remains blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionFiveCanProduceOrderedOptInWorldBroadcastFanoutTrace` | Unit | Java `CM_FIND_GROUP.runImpl` action `5`, `FindGroupService.removeApplication`, and `PacketSendUtility.broadcastToWorld` source review | Disabled boundary action `5` removed status is surfaced; boundary acceptance is recorded before opt-in race-filtered world-broadcast execution; same-race recipients are included and opposite-race recipients are excluded. | Focused connection-boundary and executor trace based on Java branch order and race predicate. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client ordering. |
| `GameServerConnectionFindGroupBoundaryTests.CreateDisabledFindGroupBoundaryPlan_ActionFiveMissingApplicationRecordsNoSideEffects` | Unit | Java `FindGroupService.removeApplication` null removal branch source review | Disabled boundary action `5` missing status is surfaced and no direct packet, world broadcast, registry send, executor send, or socket observer send occurs. | Focused no-side-effect boundary trace based on Java's null removal branch. | Does not prove live `ProcessPacketAsync`, encrypted socket, or real-client behavior. |
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_SeparatesJavaRaceFilterAndCSharpExecutorEvidenceFromLiveProof` | Unit | Java source review plus C# focused boundary fixtures | Readiness report records action `5` removed-branch fanout evidence and missing-branch no-send evidence while keeping live fanout blocked. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `5` fanout evidence is disabled-boundary plus opt-in executor evidence, not live socket evidence.
- Direct packet and world-broadcast action evidence remains non-live until `ProcessPacketAsync` is wired.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add action `12` live invite dispatch failure/result readiness evidence, keeping live dispatch disabled while proving disabled boundary handling for invite-request failure surfaces and no request mutation where Java would not send.

Safe candidates:

- Add focused runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundarySideEffectCompositionEvidenceService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionFindGroupBoundaryTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2154-Completion.md`
- `docs/Phase-6-Session-2154-Handoff.md`
