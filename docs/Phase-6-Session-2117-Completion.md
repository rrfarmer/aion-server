# Phase 6 Session 2117 Completion - FindGroup Alliance Join Ordering Evidence

Date: 2026-06-02
Unit of Work: UOW-2117
Status: Completed

## Scope

- Added focused evidence that `FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin` reads alliance runtime state after membership mutation.
- Covered the Java full-team removal branch that depends on `team.isFull()` after the invited player has joined an alliance.
- Kept `GameServerConnection.ProcessPacketAsync` live `CmFindGroup` dispatch deferred.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `addPlayerToAlliance` calls `alliance.addMember(member)` before `FindGroupService.getInstance().onJoinedTeam(invited)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `onJoinedTeam` removes the full team's recruitment when no solo recruitment was removed and `team.isFull()` is true.

## What Changed

- Added `FindGroupJoinedTeamLifecycleRecorderTests.RecordAllianceJoin_UsesRuntimeMembersAfterAllianceMutationLikeJavaAddPlayerOrdering`.
- Updated `Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record alliance joined-team recorder ordering evidence.

## Validation

- Changed surface:
  - Test-only lifecycle recorder evidence plus design documentation.
- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupJoinedTeamLifecycleRecorderTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests" --no-restore`
  - Final result: passed, 4 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed. This UOW reviewed Java `PlayerAllianceService.addPlayerToAlliance` and `FindGroupService.onJoinedTeam`; no focused Java test target was identified for this disabled C# recorder evidence.
- Broad .NET suite/build:
  - Intentionally skipped.
  - Broad-validation trigger: none.
  - Rationale: this UOW did not enable live `CmFindGroup` dispatch, live packet sends from the connection boundary, packet primitives, crypto, persistence schema, scheduling, or broad world-state behavior. Filtered tests built the affected project and covered the scoped lifecycle recorder behavior.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.addPlayerToAlliance` | `Aion.GameServer.Services.FindGroupJoinedTeamLifecycleRecorder.RecordAllianceJoin`; `PlayerAllianceRuntime` | Lifecycle Ordering | Partial | Unit Tested | Partial Parity | Focused evidence covers recorder observation after alliance runtime membership mutation by exercising the full-team removal branch with a 24-member Java-shaped alliance. Live Java runtime comparison and socket fanout ordering remain unverified. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.onJoinedTeam` full-team branch | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.OnJoinedTeam` | Service Method | Partial | Unit Tested | Partial Parity | Focused evidence now covers full-team recruitment removal through both group and alliance recorders after runtime membership is already full. Concurrent caller behavior and live singleton dispatch remain unverified. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupJoinedTeamLifecycleRecorderTests.RecordAllianceJoin_UsesRuntimeMembersAfterAllianceMutationLikeJavaAddPlayerOrdering` | Unit | Java `PlayerAllianceService.addPlayerToAlliance`; `FindGroupService.onJoinedTeam` | Recorder sees the invited player in the already-mutated alliance runtime and removes the full team recruitment like Java `team.isFull()` branch | Focused C# unit test plus reviewed Java source | Does not prove live packet fanout ordering, concurrent singleton behavior, or Java runtime trace |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 2 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Group and alliance joined-team recorder ordering now have focused evidence, but Java runtime traces, real-client behavior, socket-level order, singleton mutation ordering under concurrent callers, and live dispatch remain unverified.
- Broad .NET suite/build was not run because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupJoinedTeamLifecycleRecorderTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2117-Completion.md`
- `docs/Phase-6-Session-2117-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add focused evidence for singleton lifecycle mutation ordering under another Java call site, starting with a targeted `onLogout` or disband cleanup scenario that exercises cross-caller state on the shared `FindGroupRecruitmentPlanService`.

Safe alternative candidates:

- Add a focused Java/Maven parity fixture for one executable FindGroup branch if a suitable Java test target can be identified.
- Add a non-live execution-result surface for action `12` live-readiness failure reporting before any `ProcessPacketAsync` wiring.
- Add packet-byte evidence for action `12` declined `SM_MESSAGE` if a Java golden target can be created.
