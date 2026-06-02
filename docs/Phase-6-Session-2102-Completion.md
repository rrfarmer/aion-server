# Phase 6 Session 2102 Completion - FindGroup Lifecycle Singleton Wiring Readiness

Date: 2026-06-02
Unit of Work: UOW-2102
Status: Completed

## Scope

- Reviewed Java `FindGroupService.getInstance` lifecycle call sites before any live `CM_FIND_GROUP` dispatch work.
- Added a non-live readiness report that inventories singleton wiring requirements and current C# gaps.
- Kept live singleton wiring and live `CM_FIND_GROUP` dispatch blocked.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Uses `SingletonHolder.instance`.
  - `onJoinedTeam`, `onLogout`, and `removeRecruitment(TemporaryPlayerTeam<?>)` share the same maps used by `CM_FIND_GROUP.runImpl`.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Dispatches live packet actions through `FindGroupService.getInstance()`.
- `game-server/src/com/aionemu/gameserver/services/player/PlayerLeaveWorldService.java`
  - Calls `FindGroupService.getInstance().onLogout(player)` before `player.getResponseRequester().denyAll()`.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - Calls `FindGroupService.getInstance().onJoinedTeam(invited)` after group membership mutation.
  - Calls `FindGroupService.getInstance().removeRecruitment(group)` during disband before removing the group.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - Calls `FindGroupService.getInstance().onJoinedTeam(invited)` after alliance membership mutation.
  - Calls `FindGroupService.getInstance().removeRecruitment(alliance)` during disband before alliance disband events.

## What Changed

- Added `FindGroupLifecycleSingletonWiringReadinessService`.
  - Enumerates Java singleton call sites for `CM_FIND_GROUP`, logout cleanup, group/alliance joined-team cleanup, and group/alliance disband recruitment removal.
  - Marks the current C# state as blocked because lifecycle callers are observer-only or missing-hook.
  - Records that all listed call sites must share one `FindGroupRecruitmentPlanService` singleton before live dispatch can claim Java lifetime parity.
- Added focused readiness tests for the call-site inventory and blocker statuses.
- Updated `FindGroupConnectionBoundaryReadinessAggregateService` to expose lifecycle singleton readiness as a first-class gate.
- Updated `FindGroupLiveDispatchReadinessReportService` and tests to reference the lifecycle singleton readiness evidence.
- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` with the Java singleton call-site inventory and current C# gaps.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests|FullyQualifiedName~FindGroupLiveDispatchReadinessReportServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~PlayerGroupInviteRequestServiceTests|FullyQualifiedName~PlayerAllianceInviteRequestServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests" --no-restore`
  - Result: passed, 88 tests.
  - Note: existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this was a non-live C# readiness/reporting unit based on reviewed Java source call sites. No Java source, parser behavior, packet wire format, or Java-executable behavior changed.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: readiness/reporting/docs only; no live handler wiring, shared connection dispatch, packet primitive, serialization helper, crypto, scheduling, world-state infrastructure, persistence, or live side effect was changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupLifecycleSingletonWiringReadinessService`; `Aion.GameServer.Services.FindGroupRecruitmentPlanService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Java singleton call sites are inventoried and test-backed. C# still lacks one proven live singleton shared by `CM_FIND_GROUP`, logout cleanup, joined-team cleanup, and disband recruitment removal. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService` lifecycle call sites | `Aion.GameServer.Services.PlayerGroupInviteRequestService`; `Aion.GameServer.Services.PlayerGroupRuntime`; `FindGroupLifecycleSingletonWiringReadinessService` | Team Lifecycle | Partial | Unit Tested | Partial Parity | Joined-team observer evidence exists when recorder is injected. Java disband recruitment removal is identified but not yet represented in C# group disband planning. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService` lifecycle call sites | `Aion.GameServer.Services.PlayerAllianceInviteRequestService`; `Aion.GameServer.Services.PlayerAllianceRuntime`; `FindGroupLifecycleSingletonWiringReadinessService` | Team Lifecycle | Partial | Unit Tested | Partial Parity | Joined-team observer evidence exists when recorder is injected. Java disband recruitment removal is identified but not yet represented in C# alliance disband planning. |
| `com.aionemu.gameserver.services.player.PlayerLeaveWorldService` logout cleanup ordering | `Aion.GameServer.Services.PlayerEnterWorldService`; `FindGroupLifecycleSingletonWiringReadinessService` | Logout Lifecycle | Partial | Unit Tested | Partial Parity | C# observer evidence preserves FindGroup cleanup before question denial when supplied, but normal live singleton wiring remains unproven. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_KeepsLiveSingletonWiringBlocked` | Unit | Java `FindGroupService.SingletonHolder` source review | Report remains blocked and names the one-shared-singleton requirement | Focused C# test plus reviewed Java source | Report evidence only; no live wiring |
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_EnumeratesJavaSingletonLifecycleCallSites` | Unit | Java `CM_FIND_GROUP`, leave-world, group, and alliance call sites | Report lists all reviewed singleton lifecycle call sites | Focused C# test plus reviewed Java source | Does not execute Java |
| `FindGroupLifecycleSingletonWiringReadinessServiceTests.CreateReport_RecordsObserverOnlyAndMissingHookGaps` | Unit | Java `onLogout`, `onJoinedTeam`, and disband `removeRecruitment` call sites | Report distinguishes observer-only hooks from missing disband hooks | Focused C# test plus reviewed Java source | Does not add missing hooks |
| `FindGroupConnectionBoundaryReadinessAggregateServiceTests.CreateReport_AggregatesDisabledPlannerExecutorAuditAndLifecycleEvidence` | Unit | Java singleton lifecycle source review | Aggregate readiness carries lifecycle singleton readiness evidence | Focused C# test | Report evidence only |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 5.
- Total artifacts ported or represented in this UOW: 1 new readiness service plus readiness/design docs.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` and live singleton wiring remain intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Normal C# runtime does not yet prove that connection, logout, group, and alliance lifecycle paths share one `FindGroupRecruitmentPlanService` instance.
- Group/alliance disband recruitment-removal hooks are identified but not represented in C# runtime planning yet.
- Existing logout and joined-team evidence is observer-only and does not send live packets.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, lifecycle singleton wiring, and runtime service concurrency remain unverified.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLiveDispatchReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupConnectionBoundaryReadinessAggregateServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLiveDispatchReadinessReportServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2102-Completion.md`
- `docs/Phase-6-Session-2102-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add non-live group/alliance disband recruitment-removal planning evidence for Java `PlayerGroupService.disband` and `PlayerAllianceService.disband`.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live adapter without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under a future live singleton.
- Add a targeted Java/Maven fixture for one `FindGroupService` packet branch if a Java-executable target can be identified.
