# Phase 6 Session 2116 Completion - FindGroup Group Join Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2116
Status: Completed

## Scope

- Added focused evidence that `FindGroupJoinedTeamLifecycleRecorder.RecordGroupJoin` reads group runtime state after membership mutation.
- Covered the Java full-team removal branch that depends on `team.isFull()` after the invited player has joined.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `addPlayerToGroup` calls `group.addMember(new PlayerGroupMember(invited))` before `FindGroupService.getInstance().onJoinedTeam(invited)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onJoinedTeam` removes the full team's recruitment when no solo recruitment was removed and `team.isFull()` is true.

## What Changed

- Added `FindGroupJoinedTeamLifecycleRecorderTests.RecordGroupJoin_UsesRuntimeMembersAfterGroupMutationLikeJavaAddPlayerOrdering`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record group joined-team recorder ordering evidence.

## Validation

- Changed surface:
  - Test-only lifecycle recorder evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupJoinedTeamLifecycleRecorderTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests" --no-restore`
  - Final result: passed, 9 tests.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `PlayerGroupService.addPlayerToGroup` and `FindGroupService.onJoinedTeam`; no focused Java test target was identified for this disabled C# recorder evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped lifecycle recorder behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.addPlayerToGroup` | `Aion.GameServer.Services.FindGroupJoinedTeamLifecycleRecorder.RecordGroupJoin`; `PlayerGroupRuntime` | Lifecycle Ordering | Partial | Unit Tested | Partial Parity | Focused evidence covers recorder observation after group runtime membership mutation by exercising the full-team removal branch. Live Java runtime comparison and socket fanout ordering remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` full-team branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence covers full-team recruitment removal through the recorder after runtime membership is already full. Concurrent caller behavior and live singleton dispatch remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupJoinedTeamLifecycleRecorderTests.RecordGroupJoin_UsesRuntimeMembersAfterGroupMutationLikeJavaAddPlayerOrdering` | Unit | Java `PlayerGroupService.addPlayerToGroup`; `FindGroupService.onJoinedTeam` | Recorder sees the invited player in the already-mutated group runtime and removes the full team recruitment like Java `team.isFull()` branch | Focused C# unit test plus reviewed Java source | Does not prove live packet fanout ordering or Java runtime trace |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Group joined-team recorder ordering has focused evidence, but alliance joined-team recorder ordering still needs the same explicit after-mutation evidence.
- Java runtime traces, real-client behavior, socket-level order, singleton mutation ordering under concurrent callers, and live dispatch remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupJoinedTeamLifecycleRecorderTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2116-Completion.md`
- `docs/Phase-6-Session-2116-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add focused evidence that `FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin` reads alliance runtime state after membership mutation, matching Java `PlayerAllianceService.addPlayerToAlliance` ordering.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
