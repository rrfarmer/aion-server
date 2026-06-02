# Phase 6 Session 2120 Completion - FindGroup Alliance Disband Cross-Caller Evidence

Date: 2026-06-02
Unit of Work: UOW-2120
Status: Completed

## Scope

- Added focused evidence that alliance disband cleanup removes FindGroup team recruitment created earlier through the disabled `CM_FIND_GROUP` client-action planner when both use the same injected `FindGroupRecruitmentPlanService`.
- Covered the Java `PlayerAllianceService.disband` cleanup shape where `FindGroupService.removeRecruitment(alliance)` runs before alliance disband events.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Java `runImpl` action `2` mutates singleton `FindGroupService` recruitment state.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `disband` calls `FindGroupService.getInstance().removeRecruitment(alliance)` before alliance disband events.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `removeRecruitment(TemporaryPlayerTeam<?>)` removes recruitment keyed by `team.getTeamId()` and plans race-filtered world broadcast in live Java.

## What Changed

- Added `PlayerAllianceRuntimeTests.RemoveMemberWithLeaveWorkflow_DisbandRemovesRecruitmentCreatedByDisabledClientActionPlannerLikeJavaSingleton`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record disabled client-action-to-alliance-disband singleton cleanup evidence.

## Validation

- Changed surface:
  - Test-only cross-caller singleton evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerAllianceRuntimeTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupConnectionClientActionCompositionPlanServiceTests" --no-restore`
  - Final result: passed, 75 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `CM_FIND_GROUP.runImpl`, `PlayerAllianceService.disband`, and `FindGroupService.removeRecruitment`; no focused Java test target was identified for this disabled C# singleton-cleanup evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped cross-caller behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` action `2` | `Aion.GameServer.Services.FindGroupClientActionPlanService.Plan`; `FindGroupRecruitmentPlanService` | Client Action Planner | Partial | Unit Tested | Partial Parity | Focused evidence covers disabled planner mutation of alliance recruitment singleton state consumed by alliance disband cleanup. Live `GameServerConnection.ProcessPacketAsync` dispatch remains deferred. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime.RemoveMemberWithLeaveWorkflow` | Lifecycle Runtime | Partial | Unit Tested | Partial Parity | Focused evidence covers disband cleanup against recruitment state created through the same injected FindGroup service. Live packet fanout, Java runtime trace, league event ordering, and full alliance event replay remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.removeRecruitment(TemporaryPlayerTeam<?>)` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.RemoveRecruitment` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers team-id keyed removal and race-filtered broadcast intent planning for alliance disband cleanup. Live dispatch and concurrent singleton callers remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlayerAllianceRuntimeTests.RemoveMemberWithLeaveWorkflow_DisbandRemovesRecruitmentCreatedByDisabledClientActionPlannerLikeJavaSingleton` | Unit | Java `CM_FIND_GROUP.runImpl`; `PlayerAllianceService.disband`; `FindGroupService.removeRecruitment` | Disabled client-action planner writes alliance recruitment state to the injected FindGroup service, then alliance disband removes that same singleton state | Focused C# unit test plus reviewed Java source | Does not prove live packet dispatch, Java runtime trace, league event ordering, full event replay, or concurrent singleton behavior |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 3.
- Total artifacts ported or represented in this UOW: 3 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 3 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Disabled client-action-to-alliance-disband singleton cleanup now has focused evidence, but live socket dispatch, Java runtime traces, real-client behavior, league event ordering, full alliance event replay, and concurrent singleton mutation ordering remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2120-Completion.md`
- `docs/Phase-6-Session-2120-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add focused live-readiness result evidence for action `12` failure/status reporting before any `ProcessPacketAsync` wiring, or continue narrowing live-boundary prerequisites with packet-byte evidence for action `12` declined `SM_MESSAGE`.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
- Add focused connection-registry ordering evidence for `FindGroupSideEffectDispatchExecutorService` under a disabled boundary plan.
