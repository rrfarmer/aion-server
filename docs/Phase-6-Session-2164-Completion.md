# Phase 6 Session 2164 Completion - FindGroup Direct Show-List Boundary Trace Scaffold

Date: 2026-06-02
Unit of Work: UOW-2164
Status: Completed

## Scope

This unit added a non-live direct-packet live-boundary trace scaffold for the lowest-risk `CM_FIND_GROUP` direct packet actions: show recruitment list action `0` and show application list action `4`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `0` calls `FindGroupService.showRecruitments(player)`.
- `showRecruitments` filters the singleton recruitment map by `player.getRace()` and sends one `SM_FIND_GROUP` action `0` packet directly to the triggering player.
- Action `4` calls `FindGroupService.showApplications(player)`.
- `showApplications` filters the singleton application map by `application.getPlayer().getRace() == player.getRace()` and sends one `SM_FIND_GROUP` action `4` packet directly to the triggering player.
- These branches do not mutate FindGroup state, do not broadcast to world, and do not dispatch action `12` invite requests.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim runtime/socket parity.

## Changes

- Added `FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService`.
- Added `FindGroupDirectPacketShowListLiveBoundaryTraceScaffold` and ordered scaffold step records.
- The scaffold covers only direct packet actions `0` and `4`.
- The scaffold explicitly excludes mutating/direct actions `2`, `6`, `8`, `9`, `10`, `11`, `13`, `15`, and `17`.
- Required ordered steps:
  1. triggering client packet accepted,
  2. show-list plan composed,
  3. direct packet intent materialized,
  4. direct packet executor invoked from the boundary,
  5. registry send observed,
  6. boundary trace captured.
- The scaffold keeps:
  - `ShouldInvokeLiveSideEffects=false`,
  - `IsCmFindGroupBoundaryWired=false`,
  - `IsReadyForLiveShowListBoundary=false`.
- Updated `FindGroupDirectPacketBoundaryTraceReadinessService` to surface the show-list scaffold as non-live evidence and first low-risk direct trace candidate.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/scaffold service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests|FullyQualifiedName~FindGroupDirectPacketBoundaryTraceReadinessServiceTests|FullyQualifiedName~FindGroupDirectPacketLiveBoundaryTraceContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# scaffold used reviewed Java `CM_FIND_GROUP.runImpl` actions `0`/`4` plus `FindGroupService.showRecruitments/showApplications` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new scaffold plus adjacent direct-packet readiness and direct-packet live-boundary contract surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 9
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Action `0`/`4` show-list trace milestones are represented, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService`; `Aion.GameServer.Services.FindGroupDirectPacketBoundaryTraceReadinessService` | Direct Packet Readiness | Partial | Unit Tested | Partial Parity | Java show-list direct-send branches have a focused scaffold. No live registry send, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests.Create_KeepsShowListScaffoldBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService` source review | Scaffold remains blocked and does not invoke live side effects or wire the boundary. | Focused non-live readiness assertion. | Does not execute live direct sends. |
| `FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests.Create_ScopesOnlyLowRiskJavaShowListActions` | Unit | Java action switch and direct-send source review | Scaffold covers only actions `0`/`4` and excludes mutating direct-packet actions. | Focused action inventory assertion. | No runtime boundary comparison. |
| `FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests.Create_RequiresOrderedTraceMilestonesBeforeLiveReadiness` | Unit | Java show-list source review | Future live trace must include boundary acceptance, show-list plan, one direct intent, boundary executor, registry send, and one trace for each action. | Focused non-live trace scaffold assertion. | No live trace exists yet. |
| `FindGroupDirectPacketBoundaryTraceReadinessServiceTests.CreateReport_RecordsDisabledBoundaryActionZeroAndOptInTraceEvidence` | Unit | Java direct-send source review | Readiness report surfaces the show-list scaffold as non-live evidence. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Show-list scaffold is non-live; no live registry send, encrypted socket capture, or real-client runtime comparison has executed.
- Mutating direct-packet actions, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a trace schema/export DTO for `CM_FIND_GROUP` direct show-list boundary traces so future action `0`/`4` live trace captures have a stable comparison format.

Safe candidates:

- Add focused direct-packet live-boundary trace scaffolding for mutating direct-packet actions `2` and `6` without live dispatch.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketShowListLiveBoundaryTraceScaffoldServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketBoundaryTraceReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupDirectPacketBoundaryTraceReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2164-Completion.md`
- `docs/Phase-6-Session-2164-Handoff.md`
