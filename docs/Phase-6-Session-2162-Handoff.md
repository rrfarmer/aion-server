# Phase 6 Session 2162 Handoff - FindGroup Action 12 Invite Boundary Trace Contract

Date: 2026-06-02
Unit of Work: UOW-2162
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

Each completion/handoff must record the changed surface, focused C# command or hygiene command, Java/Maven command or skip rationale, broad-validation trigger or `none`, broad .NET skip/run decision, and why the selected scope was sufficient.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- `FindGroupDirectPacketLiveBoundaryTraceContractService` defines the direct-packet ordered trace contract.
- `FindGroupWorldBroadcastLiveBoundaryTraceContractService` defines the world-broadcast ordered trace contract.
- `FindGroupActionTwelveInviteLiveBoundaryTraceContractService` now defines the action `12` invite ordered trace contract.

## UOW-2162 Summary

This UOW added an action `12` invite trace contract for future live boundary evidence:

- Covered action: `12`.
- Accept reply: `instanceApplicationReply == 1`.
- Decline rule: `instanceApplicationReply != 1`.
- Required ordered trace milestones:
  1. triggering client packet accepted,
  2. applicant resolved,
  3. reply branch evaluated,
  4. responder instance group evaluated,
  5. invite kind selected,
  6. invite executor invoked from the boundary,
  7. live invite request mutation observed,
  8. declined whisper observed,
  9. one boundary trace captured.

The contract keeps `ShouldInvokeLiveSideEffects=false`, `ShouldMutateInviteRequests=false`, `IsCmFindGroupBoundaryWired=false`, and `IsReadyForLiveActionTwelveInviteBoundary=false`.

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Touched

- `Aion.GameServer.Services.FindGroupActionTwelveInviteLiveBoundaryTraceContractService`
- `Aion.GameServer.Services.FindGroupActionTwelveInviteLiveBoundaryTraceContract`
- `Aion.GameServer.Services.FindGroupLiveDispatchGoNoGoChecklistService`
- `Aion.GameServer.Services.FindGroupLiveDispatchDryRunPlanService`
- `Aion.GameServer.Tests.FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests`
- `Aion.GameServer.Tests.FindGroupLiveDispatchGoNoGoChecklistServiceTests`
- `Aion.GameServer.Tests.FindGroupLiveDispatchDryRunPlanServiceTests`

## Validation In UOW-2162

Validation decision:

- Changed surface: focused production readiness/contract service, focused tests, readiness-report text, and non-live design documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests|FullyQualifiedName~FindGroupLiveDispatchGoNoGoChecklistServiceTests|FullyQualifiedName~FindGroupLiveDispatchDryRunPlanServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryDispatchAdapterServiceTests" --no-restore
```

- Focused Java/Maven command: not run. No Java source changed, and this non-live C# trace contract used reviewed Java `CM_FIND_GROUP.runImpl` action `12` plus `FindGroupService.sendInstanceApplicationResult` behavior as the oracle. No narrow executable Java fixture was identified for this readiness artifact.
- Broad-validation trigger: none.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Action `12` trace contract is non-live; no live invite request mutation, question-window ordering, declined whisper send, encrypted socket capture, or real-client runtime comparison has executed.
- Direct-packet live ordering, world-broadcast live fanout, shared singleton live interleavings, and runtime/socket comparison remain non-live or missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a focused runtime/socket comparison preflight contract for `CM_FIND_GROUP` that enumerates required Java/C# trace fields and capture points without executing live dispatch.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add direct-packet live boundary trace implementation scaffolding while keeping `ProcessPacketAsync` disabled.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed In UOW-2162

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupActionTwelveInviteLiveBoundaryTraceContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupActionTwelveInviteLiveBoundaryTraceContractServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchGoNoGoChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchGoNoGoChecklistServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchDryRunPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchDryRunPlanServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2162-Completion.md`
- `docs/Phase-6-Session-2162-Handoff.md`

## Commit

Recommended commit message:

```text
[Phase 6][UOW-2162] Add find group action 12 invite trace contract
```
