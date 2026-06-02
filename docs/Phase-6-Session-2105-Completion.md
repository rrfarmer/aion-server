# Phase 6 Session 2105 Completion - FindGroup Production Singleton Graph Evidence

Date: 2026-06-02
Unit of Work: UOW-2105
Status: Completed

## Scope

- Added production DI singleton graph registration for the shared FindGroup service surface.
- Kept live `CM_FIND_GROUP` dispatch blocked.
- Kept logout cleanup observer-gated and not claimed as normal live singleton behavior.
- Continued focused validation only; no broad .NET suite/build was run because no broad trigger applied.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - Private constructor plus singleton usage through `FindGroupService.getInstance()`.
  - Shared `ConcurrentHashMap` state for recruitments, applications, and instance groups.
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `runImpl` dispatches all represented actions through `FindGroupService.getInstance()`.
- `game-server/src/com/aionemu/gameserver/model/team/group/PlayerGroupService.java`
  - `disband(...)` calls `FindGroupService.getInstance().removeRecruitment(group)`.
- `game-server/src/com/aionemu/gameserver/model/team/alliance/PlayerAllianceService.java`
  - `disband(...)` calls `FindGroupService.getInstance().removeRecruitment(alliance)`.

## What Changed

- Added `FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph`.
- Registered the graph in production `Program.cs` before game-client socket construction:
  - `FindGroupRecruitmentPlanService`
  - `FindGroupClientActionPlanService`
  - `FindGroupJoinedTeamLifecycleRecorder`
  - `PlayerGroupRuntime`
  - `PlayerAllianceRuntime`
  - `PlayerGroupInviteRequestService`
  - `PlayerAllianceInviteRequestService`
  - `FindGroupInstanceApplicationInviteDispatchPlanService`
  - `FindGroupConnectionBoundaryDispatchAdapterService`
  - `FindGroupConnectionClientActionCompositionPlanService`
- Added focused tests proving:
  - `GameClientSocketServer` receives the DI singleton group/alliance runtimes and invite services.
  - DI-resolved group invite service moves solo FindGroup recruitment to team recruitment through the shared service.
  - DI-resolved group/alliance runtimes remove team/alliance recruitment from the shared service during disband planning.
- Updated readiness/design text to mark joined-team/disband evidence as production singleton graph evidence while keeping live readiness blocked.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupServiceCollectionExtensionsTests|FullyQualifiedName~FindGroupLifecycleSingletonWiringReadinessServiceTests|FullyQualifiedName~FindGroupConnectionBoundaryReadinessAggregateServiceTests" --no-restore`
  - First run failed because the socket-server provider test used synchronous provider disposal for an `IAsyncDisposable` singleton.
  - Final result: passed, 10 tests.
  - Existing nullable/xUnit warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: no Java source changed, and this UOW added C# DI graph evidence for previously reviewed Java singleton call sites.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: the UOW registered a narrow service graph and did not enable live `CM_FIND_GROUP`, live packet sends, packet primitives, crypto, persistence, scheduling, or broad world-state behavior. Filtered tests built the affected projects and proved the scoped graph.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService` singleton lifecycle | `Aion.GameServer.Services.FindGroupServiceCollectionExtensions.AddFindGroupSingletonGraph`; `FindGroupRecruitmentPlanService` | Service Lifecycle | Partial | Unit Tested | Partial Parity | Production DI now creates one shared FindGroup service graph for joined-team and disband callers. Live `CM_FIND_GROUP` and logout cleanup still are not proven against the graph. |
| `com.aionemu.gameserver.model.team.group.PlayerGroupService.disband` | `Aion.GameServer.Services.PlayerGroupRuntime` registered through `AddFindGroupSingletonGraph` | Team Lifecycle | Partial | Unit Tested | Partial Parity | DI-resolved group runtime removes team-keyed recruitment from the shared FindGroup service during disband planning. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.model.team.alliance.PlayerAllianceService.disband` | `Aion.GameServer.Services.PlayerAllianceRuntime` registered through `AddFindGroupSingletonGraph` | Team Lifecycle | Partial | Unit Tested | Partial Parity | DI-resolved alliance runtime removes alliance-keyed recruitment from the shared FindGroup service during disband planning. Live packet fanout remains disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` | `FindGroupConnectionClientActionCompositionPlanService`; `FindGroupConnectionBoundaryDispatchAdapterService` registered through `AddFindGroupSingletonGraph` | Client Packet Boundary | Partial | Unit Tested for DI registration only | Needs Verification | Non-live planner/adapter services are registered in the graph, but `GameServerConnection` still defers `CmFindGroup` and does not invoke them. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupServiceCollectionExtensionsTests.AddFindGroupSingletonGraph_RegistersSharedSocketRuntimeServices` | Unit | Java `FindGroupService.getInstance` singleton lifetime source review | Socket-server production construction receives DI singleton runtimes and invite services | Focused C# DI graph test | Uses reflection to inspect constructor-assigned private fields; no live socket packet flow |
| `FindGroupServiceCollectionExtensionsTests.AddFindGroupSingletonGraph_GroupInviteUsesSharedFindGroupService` | Unit | Java group invite accept -> `FindGroupService.onJoinedTeam` source review | DI-resolved group invite service and group runtime share FindGroup state | Focused C# behavior test | No live `CM_FIND_GROUP`; no Java runtime packet trace |
| `FindGroupServiceCollectionExtensionsTests.AddFindGroupSingletonGraph_GroupRuntimeDisbandUsesSharedFindGroupService` | Unit | Java `PlayerGroupService.disband` source review | DI-resolved group runtime removes team recruitment from the shared FindGroup service | Focused C# behavior test | Live packet fanout remains disabled |
| `FindGroupServiceCollectionExtensionsTests.AddFindGroupSingletonGraph_AllianceRuntimeDisbandUsesSharedFindGroupService` | Unit | Java `PlayerAllianceService.disband` source review | DI-resolved alliance runtime removes alliance recruitment from the shared FindGroup service | Focused C# behavior test | Live packet fanout remains disabled |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 4.
- Total artifacts ported or represented in this UOW: 5 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 4 table rows.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` and logout singleton cleanup remain blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- Logout cleanup remains observer-only; production DI registration alone does not make it normal live singleton behavior.
- The production graph does not prove socket-level packet ordering, real-client behavior, Java runtime output, race-filtered world fanout, or concurrency.
- `FindGroupConnectionBoundaryDispatchAdapterService` is registered but not consumed by `GameServerConnection`.
- Broad .NET suite/build was not run in UOW-2105 because no broad-validation trigger applied.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Program.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupServiceCollectionExtensions.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupLifecycleSingletonWiringReadinessService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupConnectionBoundaryReadinessAggregateService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupServiceCollectionExtensionsTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupLifecycleSingletonWiringReadinessServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2105-Completion.md`
- `docs/Phase-6-Session-2105-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: add logout cleanup singleton wiring evidence so `PlayerEnterWorldService.LeaveWorldAsync` can use the shared `FindGroupRecruitmentPlanService` without requiring an observer-only test hook, while keeping packet sends disabled.

Safe alternative candidates:

- Add an adapter-consumer test slice proving how `GameServerConnection` could call the non-live `FindGroupConnectionBoundaryDispatchAdapterService` without enabling live sends.
- Review multi-step mutation ordering for `FindGroupRecruitmentPlanService` under future live singleton use.
- Add focused Java/Maven parity fixture for one FindGroup branch if an executable Java test target can be identified.
