# Phase 6 Session 2136 Completion - FindGroup Shared Singleton Interleaving Fixtures

Date: 2026-06-02
Unit of Work: UOW-2136
Status: Completed

## Scope

This unit added focused, non-live evidence for deterministic shared `FindGroupRecruitmentPlanService` caller orders before live C# `CM_FIND_GROUP` dispatch.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `FindGroupService.onLogout(Player)`
- `FindGroupService.onJoinedTeam(Player)`
- `FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)`
- `FindGroupService.removeRecruitment(Player, byte, byte, byte, byte)`

Java remains the source of truth. This UOW does not prove live boundary or runtime concurrency parity.

## Changes

- Added `FindGroupSharedSingletonInterleavingTests`.
- Covered logout-before-joined-team ordering where `CM_FIND_GROUP`-created solo state is removed before joined-team cleanup runs, preventing re-created player or team FindGroup state.
- Covered joined-team-before-logout ordering where a solo recruitment is re-added as a team recruitment, logout removes only player-keyed state, and disband cleanup removes the team-keyed recruitment.
- Updated `FindGroupConcurrentMutationOrderingReadinessService` to record deterministic shared-singleton interleaving evidence while keeping live interleaving blocked.
- Updated `FindGroupLiveDispatchReadinessReportService` observer evidence.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md`.

## Validation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSharedSingletonInterleavingTests|FullyQualifiedName~FindGroupConcurrentMutationOrderingReadinessServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~PlayerGroupRuntimeTests" --no-restore
```

Result:

- Passed: 89
- Failed: 0
- Skipped: 0
- Existing unrelated nullable/analyzer warnings were emitted in `SmSystemMessage`, `GameServerConnection`, several existing tests, and `ProfessionFormulaServiceTests`.

Full .NET validation:

- Skipped intentionally. This was a focused test/readiness-report unit with no live dispatch, shared packet primitive, common runtime base, persistence, or solution-wide contract change.
- Filtered `dotnet test` compiled the affected project and adjacent FindGroup/group-runtime surface.

Java/Maven validation:

- Not run. No Java source changed, and this UOW used reviewed Java method order as the oracle for deterministic C# shared-service fixtures. No narrow Java test target was identified for this non-live C# fixture.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service | Partial | Unit Tested | Partial Parity | Focused tests now cover deterministic shared-service caller orders for `CM_FIND_GROUP`-created state, logout cleanup, joined-team cleanup, and disband cleanup. Live boundary/runtime interleaving is still unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onLogout` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnLogout` | Service Method | Complete | Unit Tested | Partial Parity | Java removes only player-object-id entries and sends no packets. C# fixture confirms logout-before-joined-team removes player-keyed state and joined-team does not resurrect it. Live server ordering remains unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Complete | Unit Tested | Partial Parity | Java removes application and solo recruitment, then re-adds leader team recruitment or removes full-team recruitment. C# fixture confirms joined-team-before-logout leaves team-keyed recruitment for disband cleanup. Live server ordering remains unverified. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime.RemoveMemberWithLeavePlan` | Runtime Service | Partial | Unit Tested | Partial Parity | Existing and adjacent focused tests cover disband cleanup removing team-keyed FindGroup recruitment from the shared service. This UOW uses direct shared-service disband cleanup evidence; live runtime/socket behavior remains unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupSharedSingletonInterleavingTests.LogoutBeforeJoinedTeamDoesNotRecreateClientActionState` | Unit | Java source review of `FindGroupService.onLogout` and `onJoinedTeam` | Logout removes player-keyed recruitment/application/instance-group state before joined-team cleanup; later joined-team cleanup sees missing state and does not re-create it. | Focused deterministic C# test based on Java method order. | Does not prove live packet-boundary or runtime thread interleaving. |
| `FindGroupSharedSingletonInterleavingTests.JoinedTeamBeforeLogoutLeavesTeamRecruitmentForDisbandCleanup` | Unit | Java source review of `FindGroupService.onJoinedTeam`, `onLogout`, and disband recruitment removal | Joined-team re-adds solo leader recruitment as team recruitment; logout removes only player-keyed state; disband cleanup removes team-keyed recruitment. | Focused deterministic C# test based on Java method order. | Does not prove live packet-boundary or runtime thread interleaving. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4
- Total artifacts ported in this UOW: 0
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 1 live `CM_FIND_GROUP` boundary
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Deterministic shared-service fixtures do not prove true concurrent live caller behavior.
- Direct-packet ordering, world-broadcast fanout, and action `12` invite dispatch still need live boundary evidence.
- Real encrypted socket or real-client behavior remains unverified.

## Next Recommended Unit of Work

Next sequential task:

- Add focused live-boundary readiness evidence for one simple direct-packet action without enabling broad live `CM_FIND_GROUP` dispatch.

Safe candidates:

- Add focused live-boundary readiness evidence for action `1` or `5` world-broadcast same-race/opposite-race fanout.
- Add focused action `12` live invite-dispatch failure/result readiness.
- Add runtime trace scaffolding for FindGroup caller interleavings if a narrow non-invasive hook exists.
- Add a targeted Java/Maven fixture only if a narrow executable Java FindGroup parity target is identified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConcurrentMutationOrderingReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConcurrentMutationOrderingReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSharedSingletonInterleavingTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2136-Completion.md`
- `docs/Phase-6-Session-2136-Handoff.md`
