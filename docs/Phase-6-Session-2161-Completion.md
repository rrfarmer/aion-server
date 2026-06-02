# Phase 6 Session 2161 Completion - FindGroup World Broadcast Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2161
Status: Completed

## Scope

This unit added a non-live world-broadcast live-boundary trace contract for future `CM_FIND_GROUP` wiring. It defines what a future ordered boundary trace must prove before world-broadcast fanout for actions `1` and `5` can be considered ready.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `1` calls `FindGroupService.removeRecruitment(player, serverId, unk1, unk2, unk3)` and broadcasts only when a recruitment is removed.
- Action `5` calls `FindGroupService.removeApplication(player)` and broadcasts only when an application is removed.
- Java filters action `1` broadcasts by `recruitment.getRace()`.
- Java filters action `5` broadcasts by `application.getPlayer().getRace()`.
- Missing recruitment/application branches produce no world broadcast.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim runtime/socket parity.

## Changes

- Added `FindGroupWorldBroadcastLiveBoundaryTraceContractService`.
- Added `FindGroupWorldBroadcastLiveBoundaryTraceContract` and ordered trace step records.
- The contract requires future live traces to record:
  1. triggering client packet accepted,
  2. shared singleton removal evaluated,
  3. world-broadcast intent materialized only for removed branches,
  4. Java race filter applied,
  5. world-broadcast executor invoked from the boundary,
  6. registry broadcast observed,
  7. one ordered boundary trace captured.
- The contract keeps:
  - `ShouldInvokeLiveSideEffects=false`,
  - `IsCmFindGroupBoundaryWired=false`,
  - `IsReadyForLiveWorldBroadcastBoundary=false`.
- Updated `FindGroupWorldBroadcastFanoutReadinessService` to surface the contract as non-live evidence while keeping live fanout blockers.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupWorldBroadcastFanoutReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` and `FindGroupService.removeRecruitment/removeApplication` broadcast behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new trace contract plus adjacent world-broadcast, dry-run, and go/no-go readiness surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 13
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContractService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | World-broadcast action inventory and trace milestones are represented for actions `1` and `5`, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupWorldBroadcastLiveBoundaryTraceContractService`; `Aion.GameServer.Services.FindGroupWorldBroadcastFanoutReadinessService` | World Broadcast Readiness | Partial | Unit Tested | Partial Parity | Java `PacketSendUtility.broadcastToWorld` race-filter branches have a future trace contract. No live registry broadcasts, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests.Create_KeepsContractBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Contract remains blocked and does not invoke live side effects or wire the boundary. | Focused non-live readiness assertion. | Does not execute live broadcasts. |
| `FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests.Create_CoversOnlyJavaWorldBroadcastActions` | Unit | Java action switch and broadcast call-site source review | World-broadcast action inventory is exact for actions `1`/`5` and excludes other `CM_FIND_GROUP` actions. | Focused non-live action inventory assertion. | No runtime broadcast comparison. |
| `FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests.Create_RequiresOrderedTraceMilestonesBeforeLiveReadiness` | Unit | Java removal and broadcast race-filter source review | Future live trace must include ordered boundary acceptance, removal outcome, broadcast intent materialization, race filter, boundary-invoked executor, registry broadcast observation, and captured trace. | Focused non-live trace contract assertion. | No live trace exists yet. |
| `FindGroupWorldBroadcastFanoutReadinessServiceTests.CreateReport_SeparatesJavaRaceFilterAndCSharpExecutorEvidenceFromLiveProof` | Unit | Java broadcast source review | Readiness report surfaces the new world-broadcast live-boundary trace contract as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- World-broadcast trace contract is non-live; no registry broadcasts, encrypted socket capture, or real-client runtime comparison has executed.
- Direct packet ordering, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add action `12` invite live-boundary trace contract/scaffolding without invoking request mutation or live dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add runtime/socket comparison preflight contract for `CM_FIND_GROUP` after action `12` trace prerequisites are explicit.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupWorldBroadcastFanoutReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupWorldBroadcastFanoutReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2161-Completion.md`
- `docs/Phase-6-Session-2161-Handoff.md`
