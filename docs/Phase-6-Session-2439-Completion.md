# Phase 6 Session 2439 Completion

## Unit of Work

- UOW-2439: Scaffold alliance offline-timeout runtime hook.

## Status

- Completed and validated with focused tests.
- C# alliance runtime now exposes a Java-derived single-step timeout hook that removes the next expired offline alliance member through the existing `LEAVE_TIMEOUT` leave workflow.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
- `game-server/src/com/aionemu/gameserver/model/team/alliance/events/PlayerAllianceLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/model/team/common/events/PlayerLeavedEvent.java`
- `game-server/src/com/aionemu/gameserver/configs/main/GroupConfig.java`
- `game-server/src/com/aionemu/gameserver/model/team/TeamType.java`

## C# Artifacts Changed

- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceInfoPlan.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerAllianceRuntime.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerAllianceRuntimeTests.cs`

## Implementation Notes

- Added `PlayerAllianceRuntime.RemoveNextExpiredOfflineMemberWithLeaveWorkflow(...)` as a narrow equivalent for the Java `OfflinePlayerAllianceChecker` event source.
- Preserved Java timeout delay selection: auto alliances use 60 seconds; ordinary alliances use the configured alliance remove time.
- Reused the existing leave workflow mutation path for `PlayerAllianceLeaveReason.LeaveTimeout`, including member removal, timeout fanout, disband planning, and base leave side effects.
- Added `PlayerAllianceTeamTypeExtensions.IsAutoTeam/IsOffence` and `PlayerAllianceOfflineTimeoutPlan` so callers can observe Java side effects like offence invader removal without wiring the full scheduler in this UOW.
- For in-league timeout disband, runtime defers remaining alliance cleanup so a future dispatcher can preserve Java `disband(team, true)` ordering around league removal before alliance disband packets.

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `RemoveNextExpiredOfflineMemberWithLeaveWorkflow_DispatchesTimeoutWithoutLeagueBroadcastLikeJava` | Regression | Java source review of `OfflinePlayerAllianceChecker` and `PlayerAllianceLeavedEvent` | Expired offline member is removed with `LEAVE_TIMEOUT`; timeout message/member/alliance-info fanout precedes disband packets; ordinary league broadcast is skipped; base leave follows alliance leave | Focused C# runtime workflow assertions | Does not wire a live scheduler or socket dispatcher. |
| `RemoveNextExpiredOfflineMemberWithLeaveWorkflow_UsesSixtySecondAutoAllianceDelayLikeJava` | Regression | Java source review of `OfflinePlayerAllianceChecker` and `TeamType.isAutoTeam` | Auto-alliance timeout uses 60 seconds instead of configured alliance remove time and does not disband auto alliance singletons | Focused C# runtime boundary assertions | Does not cover offence VortexService integration beyond the returned side-effect flag. |

## Validation Decision

- Changed surface: alliance runtime, alliance plan records, focused runtime tests.
- Specific behavior/contract: Java `OfflinePlayerAllianceChecker` timeout eligibility and `LEAVE_TIMEOUT` workflow ordering.
- Focused C# command: `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionPlayerStatusInfoTests|FullyQualifiedName~PlayerAllianceRuntimeTests" --no-restore`
- Result: passed, 101 tests.
- Hygiene command: `git diff --check`
- Result: passed; Git emitted existing line-ending normalization warnings for touched files.
- Focused Java/Maven command: not run; Java source was reviewed and no Java source or fixtures changed.
- Broad-validation trigger: none. The change is scoped to a runtime hook and tests; no scheduler thread, packet primitive, serializer, persistence, or release-readiness trigger applied.
- Broad .NET decision: skipped; the filtered command compiled the affected test project and validated the edited alliance runtime surface plus adjacent command coverage.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.OfflinePlayerAllianceChecker` | `Aion.GameServer.Services.PlayerAllianceRuntime.RemoveNextExpiredOfflineMemberWithLeaveWorkflow` | Scheduler/Event Source | Partial | Regression Tested | Partial Parity | Runtime event-source hook exists for one expired member at a time, including delay selection and timeout workflow. Live scheduled execution and packet dispatch remain open. |
| `com.aionemu.gameserver.model.team.alliance.events.PlayerAllianceLeavedEvent` | `Aion.GameServer.Services.PlayerAllianceLeaveWorkflowPlanner`; `Aion.GameServer.Services.PlayerAllianceRuntime` | Event/Workflow | Partial | Regression Tested | Partial Parity | `LEAVE_TIMEOUT` removes the offline member, skips ordinary league broadcast, plans disband, and invokes base leave after alliance leave. |
| `com.aionemu.gameserver.model.team.TeamType` | `Aion.GameServer.Services.PlayerAllianceTeamTypeExtensions` | Enum Helper | Partial | Unit Tested | Partial Parity | `isAutoTeam` and `isOffence` behavior needed by timeout checker is represented; broader Java TeamType helper surface remains partial. |
| `com.aionemu.gameserver.configs.main.GroupConfig` | `RemoveNextExpiredOfflineMemberWithLeaveWorkflow(..., allianceRemoveTimeSeconds)` | Config Boundary | Partial | Regression Tested | Partial Parity | Config value is passed into the runtime hook; no global C# scheduler config binding was introduced. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 5
- Total artifacts ported or adjusted in this UOW: 3 C# artifacts
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4
- Total blocked artifacts: 0
- Estimated overall Phase 6 migration completion: 41%

## Remaining Risks

- No live C# scheduled service invokes the timeout hook yet.
- Timeout packet dispatch through active connections remains unwired.
- Offence VortexService removal is represented as a plan flag, not executed by a C# runtime integration.
- In-league timeout disband still needs an end-to-end dispatcher test proving league-left packets are sent before alliance disband packets.
