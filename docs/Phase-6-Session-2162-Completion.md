# Phase 6 Session 2162 Completion - FindGroup Action 12 Invite Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2162
Status: Completed

## Scope

This unit added a non-live action `12` invite live-boundary trace contract for future `CM_FIND_GROUP` wiring. It defines what a future ordered boundary trace must prove before action `12` accepted invite and declined whisper side effects can be considered live-ready.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- Action `12` parses `playerOrTeamId` as the applicant id and `instanceApplicationReply`.
- `runImpl` calls `FindGroupService.sendInstanceApplicationResult(player, playerOrTeamId, instanceApplicationReply)`.
- Java resolves the applicant through `World.getInstance().getPlayer(applicantId)`.
- Missing applicant produces no side effects.
- Reply value `1` is the accept branch. It looks up `instanceGroups.get(responder.getObjectId())`; missing instance group produces no side effects.
- Accepted branch invokes `PlayerGroupService.inviteToGroup(responder, applicant)` when `minMembers <= 6`; otherwise it invokes `PlayerAllianceService.inviteToAlliance(responder, applicant)`.
- Any non-`1` reply sends the applicant an `SM_MESSAGE` whisper using `ChatUtil.l10n(1400217)` and `ChatType.WHISPER`.

This UOW does not enable live `CM_FIND_GROUP` dispatch, does not mutate live invite requests from the boundary, and does not claim runtime/socket parity.

## Changes

- Added `FindGroupActionTwelveInviteLiveBoundaryTraceContractService`.
- Added `FindGroupActionTwelveInviteLiveBoundaryTraceContract` and ordered trace step records.
- The contract requires future live traces to record:
  1. triggering client packet accepted,
  2. applicant resolved,
  3. reply branch evaluated,
  4. responder instance group evaluated,
  5. invite kind selected,
  6. invite executor invoked from the boundary,
  7. live invite request mutation observed,
  8. declined whisper observed,
  9. one ordered boundary trace captured.
- The contract keeps:
  - `ShouldInvokeLiveSideEffects=false`,
  - `ShouldMutateInviteRequests=false`,
  - `IsCmFindGroupBoundaryWired=false`,
  - `IsReadyForLiveActionTwelveInviteBoundary=false`.
- Updated the live dispatch go/no-go checklist and dry-run plan to reference the action `12` contract as non-live evidence and future live-trace work.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` action `12` plus `FindGroupService.sendInstanceApplicationResult` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none. No live connection dispatch, live side effects, packet primitive, common runtime base, persistence, shared infrastructure, or broad behavior surface changed.
- Broad .NET decision: skipped intentionally.
- Why this scope is sufficient: the filtered tests cover the new trace contract plus adjacent go/no-go, dry-run, and action `12` disabled boundary surfaces, and the filtered command built the affected project/dependencies.

Result:

- Passed: 17
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Services.FindGroupActionTwelveInviteLiveBoundaryTraceContractService` | Client Packet Boundary Readiness | Blocked | Unit Tested | Partial Parity | Action `12` branch inventory and trace milestones are represented, but live `ProcessPacketAsync` execution remains disabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupActionTwelveInviteLiveBoundaryTraceContractService`; `Aion.GameServer.Services.FindGroupInstanceApplicationInviteDispatchPlanService` | Invite Dispatch Readiness | Partial | Unit Tested | Partial Parity | Java applicant resolution, reply branching, group/alliance invite selection, declined whisper, and missing branches have a future trace contract. No live invite request mutation, socket comparison, or runtime trace has executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests.Create_KeepsContractBlockedAndNonLive` | Unit | Java `CM_FIND_GROUP.runImpl`; `FindGroupService.sendInstanceApplicationResult` source review | Contract remains blocked and does not invoke live side effects, mutate invite requests, or wire the boundary. | Focused non-live readiness assertion. | Does not execute live action `12`. |
| `FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests.Create_RequiresOrderedTraceMilestonesBeforeLiveReadiness` | Unit | Java action `12` parse/run and applicant resolution source review | Future live trace must include ordered boundary acceptance, applicant resolution, reply branch evaluation, and missing-applicant evidence. | Focused non-live trace contract assertion. | No live trace exists yet. |
| `FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests.Create_SeparatesAcceptedInviteMutationFromDeclinedWhisperTrace` | Unit | Java accepted invite and declined whisper source review | Future live trace must separately prove group/alliance invite mutation and declined whisper dispatch. | Focused non-live trace contract assertion. | No live request mutation or whisper dispatch from `ProcessPacketAsync`. |
| `FindGroupLiveDispatchGoNoGoChecklistServiceTests.CreateChecklist_SeparatesEvidenceAvailableGatesFromReadyGates` | Unit | Java action `12` source review | Go/no-go checklist surfaces the new action `12` contract as non-live evidence and still blocks live dispatch. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |
| `FindGroupLiveDispatchDryRunPlanServiceTests.CreatePlan_MapsRequiredGatesToExecutorsAndResultSurfaces` | Unit | Java action `12` source review | Dry-run plan names the action `12` contract as the required future result surface. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Action `12` trace contract is non-live; no live invite request mutation, question-window ordering, declined whisper send, encrypted socket capture, or real-client runtime comparison has executed.
- Direct packet ordering, world-broadcast fanout, shared singleton interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a focused runtime/socket comparison preflight contract for `CM_FIND_GROUP` that enumerates required Java/C# trace fields and capture points without executing live dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add direct-packet live boundary trace implementation scaffolding while keeping `ProcessPacketAsync` disabled.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupActionTwelveInviteLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchDryRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchDryRunPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2162-Completion.md`
- `docs/Phase-6-Session-2162-Handoff.md`
