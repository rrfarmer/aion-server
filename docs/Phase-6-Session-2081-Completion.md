# Phase 6 Session 2081 Completion - Find Group Instance-Group Join Removal

Date: 2026-06-01
Unit of Work: UOW-2081
Status: Completed

## Scope

- Tightened C# `FindGroupRecruitmentPlanService.OnJoinedTeam` behavior for the Java server-wide instance-group removal branch.
- Preserved disabled planner semantics: no live find-group direct sends or world broadcasts were enabled.
- Kept validation focused on the planner and adjacent invite observer tests.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onJoinedTeam(player)` reads `instanceGroups.get(player.getObjectId())`.
  - If the registered server-wide group exists and `getMembers().size() >= getMinMembers()`, it removes `instanceGroups.remove(player.getObjectId())`.
- `game-server/src/com/aionemu/gameserver/model/gameobjects/findGroup/ServerWideGroup.java`
  - `getMembers()` returns the recruiter's current team members when the recruiter is in a team; otherwise it returns the stored single-member list.
  - This makes current-team size the Java threshold proxy after the recruiter joins/forms a team.

## What Changed

- Updated `FindGroupRecruitmentPlanService.OnJoinedTeam`.
  - Looks up the stored instance-group registration by `player.ObjectId`.
  - Uses supplied `FindGroupInstanceGroupJoinState` when present, otherwise uses `currentTeam.Size` as the Java `ServerWideGroup.getMembers()` proxy.
  - Removes the stored instance-group entry when the effective member count reaches `MinMembers`.
- Extended `FindGroupInstanceGroupRemovalPlan` with optional `RemovedInstanceGroup` evidence.
- Added a focused test proving:
  - below-threshold current-team size leaves the stored registration intact;
  - threshold-reaching current-team size removes the stored registration;
  - the removal is still a disabled plan with no live dispatch.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Result: passed, 39 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no narrow Java test class exists for `FindGroupService.onJoinedTeam` or `ServerWideGroup` under `game-server/test`. Java evidence is source review of the exact methods listed above.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: change is a localized disabled planner mutation plus focused tests. It does not enable live dispatch, change packet primitives, persistence, crypto, scheduling, connection dispatch, or broad runtime side effects.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` instance-group branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam`; `FindGroupInstanceGroupRemovalPlan` | Find Group Service / Plan | Partial | Unit Tested | Partial Parity | C# now removes a stored instance-group registration when the effective current-team member count reaches `MinMembers`, matching Java's `ServerWideGroup.getMembers()` proxy for the represented planner slice. |
| `com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup.getMembers` threshold behavior | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` current-team-size proxy | Model Behavior / Planner Input | Partial | Unit Tested | Partial Parity | C# uses `currentTeam.Size` when no explicit join-state override is supplied. Full live auto-group membership/runtime behavior remains outside this disabled planner slice. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupRecruitmentPlanServiceTests.OnJoinedTeam_RemovesRegisteredInstanceGroupWhenCurrentTeamReachesMinMembersLikeJavaProxy` | Unit | Java `FindGroupService.onJoinedTeam` and `ServerWideGroup.getMembers` source review | Stored instance-group entry remains below threshold and is removed once current team size reaches `minMembers` | Focused C# unit test | Does not execute Java runtime, live auto-group service, or packet dispatch |

## Summary Metrics

- Total Java artifacts discovered/reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred.
- Joined-team handling remains disabled planner/observer evidence, not a live singleton service path.
- Full live auto-group registration, registration windows, periodic instance scheduling, and `SM_AUTO_GROUP` workflows remain outside this UOW.
- Encrypted socket behavior, real-client behavior, service concurrency, and live group/alliance invite dispatch from `CM_FIND_GROUP` remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupRecruitmentPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupRecruitmentPlanServiceTests.cs`
- `docs/Phase-6-Session-2081-Completion.md`
- `docs/Phase-6-Session-2081-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: update `FindGroupLiveDispatchReadinessReportService` to recognize the joined-team observer and instance-group threshold evidence while keeping live dispatch blocked for direct-send/world-broadcast/group-alliance invite runtime gaps.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for `AutoGroupData` if a lightweight Java test can be introduced safely.
- Review one remaining `FindGroupService` action branch for runtime-fact gaps before live dispatch is considered.
- Audit `GameServerConnection` `CmFindGroup` deferred branch against the readiness report to define the final pre-live checklist.
