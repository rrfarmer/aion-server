# Phase 6 Session 2156 Completion - FindGroup Shared Singleton Trace Projection

Date: 2026-06-02
Unit of Work: UOW-2156
Status: Completed

## Scope

This unit added focused, non-live trace projection scaffolding for FindGroup shared-singleton caller interleavings.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Java behavior:

- `FindGroupService` stores recruitments, applications, and instance groups in independent `ConcurrentHashMap` instances.
- `onLogout(Player)` removes player-keyed recruitment, application, and instance-group entries and sends no packets.
- `onJoinedTeam(Player)` removes a qualifying instance-group registration, removes the player's application, removes the player's solo recruitment with `unknown3=16`, then either re-adds leader recruitment under the team key or removes a full team's recruitment.
- `removeRecruitment(TemporaryPlayerTeam<?>)` removes team-keyed recruitment before group/alliance disband cleanup clears the remaining team state.

This UOW does not enable live `CM_FIND_GROUP` dispatch and does not claim live runtime comparison.

## Changes

- Added `FindGroupSharedSingletonInterleavingTraceService` to project deterministic non-live trace rows for shared singleton caller order.
- Extended `FindGroupSharedSingletonInterleavingTests` to assert trace rows for:
  - logout before joined-team cleanup,
  - joined-team cleanup before logout before disband cleanup.
- Updated `FindGroupConcurrentMutationOrderingReadinessService` with a separate trace-projection evidence row.
- Updated `FindGroupLiveDispatchReadinessReportService` and `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to distinguish deterministic non-live trace projections from still-missing live singleton interleaving proof.

## Validation

Validation decision:

- Changed surface: focused production trace projection service, focused tests, readiness-report text, and non-live design documentation.
- Broad-validation trigger: none. No live dispatch, shared packet primitive, common runtime base, persistence, solution-wide contract, or shared infrastructure was changed.
- Broad .NET decision: skipped intentionally.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSharedSingletonInterleavingTests|FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests" --no-restore
```

Result:

- Passed: 48
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in existing files/tests.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java `FindGroupService.onLogout`, `FindGroupService.onJoinedTeam`, and `FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` behavior as the oracle. No narrow Java test target was identified for this non-live C# trace projection fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service | Partial | Unit Tested | Partial Parity | Shared state stores and deterministic lifecycle caller orders have focused evidence. Live concurrent caller interleavings remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Partial | Unit Tested | Partial Parity | Player-keyed cleanup is included in deterministic trace rows. Live logout interleaving with `CM_FIND_GROUP` remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Partial | Unit Tested | Partial Parity | Java method-order outcomes and deterministic trace rows are covered. Live joined-team interleaving remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service Method | Partial | Unit Tested | Partial Parity | Team-keyed disband cleanup is included in deterministic trace rows. Live disband interleaving remains unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `Aion.GameServer.Network.Aion.GameServerConnection.ProcessPacketAsync` | Connection Boundary | Blocked | Unit Tested | Partial Parity | Java `CM_FIND_GROUP` can participate in the singleton interleavings; C# live boundary remains deferred, so trace projection is non-live only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupSharedSingletonInterleavingTests.LogoutBeforeJoinedTeamDoesNotRecreateClientActionState` | Unit | Java `FindGroupService.onLogout` and `onJoinedTeam` source review | Logout removes player-keyed state before joined-team cleanup observes missing player-keyed state; trace rows preserve caller order and outcomes. | Focused deterministic non-live trace projection. | Does not prove live `CM_FIND_GROUP`, encrypted socket, or concurrent runtime behavior. |
| `FindGroupSharedSingletonInterleavingTests.JoinedTeamBeforeLogoutLeavesTeamRecruitmentForDisbandCleanup` | Unit | Java `FindGroupService.onJoinedTeam`, `onLogout`, and `removeRecruitment(team)` source review | Joined-team cleanup re-keys leader recruitment to team id, logout does not remove team-keyed recruitment, and disband cleanup removes it; trace rows preserve caller order and outcomes. | Focused deterministic non-live trace projection. | Does not prove live `CM_FIND_GROUP`, encrypted socket, or concurrent runtime behavior. |
| `FindGroupConcurrentMutationOrderingReadinessServiceTests.CreateReport_SeparatesConcurrentMapShapeFromMultiStepLiveEvidence` | Unit | Java source review plus C# focused trace projection fixtures | Readiness report separates deterministic trace projection evidence from still-missing live singleton interleaving proof. | Focused readiness-report assertion. | Report evidence only; live dispatch remains blocked. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 5
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Trace projection evidence is deterministic and non-live; it is not a runtime/socket comparison.
- Live singleton caller interleavings across `CM_FIND_GROUP`, logout cleanup, joined-team cleanup, and disband cleanup remain unverified.
- Real encrypted socket or real-client FindGroup behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused go/no-go checklist coverage for the remaining live-dispatch blockers before any `ProcessPacketAsync` wiring attempt, making direct packet ordering, world-broadcast fanout, action `12` invite dispatch, shared singleton interleavings, and runtime/socket comparison gates explicit.

Safe candidates:

- Add live boundary or runtime trace evidence for shared singleton caller interleavings.
- Add a narrow non-live `CM_FIND_GROUP` live-wiring dry-run plan that enumerates required executors without invoking them.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupSharedSingletonInterleavingTraceService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConcurrentMutationOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSharedSingletonInterleavingTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConcurrentMutationOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2156-Completion.md`
- `docs/Phase-6-Session-2156-Handoff.md`
