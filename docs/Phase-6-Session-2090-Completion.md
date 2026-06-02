# Phase 6 Session 2090 Completion - Find Group Action 15 Executor Evidence

Date: 2026-06-01
Unit of Work: UOW-2090
Status: Completed

## Scope

- Inspected Java `CM_FIND_GROUP` action 15 and `FindGroupService.showInstanceGroupMembersInfo`.
- Verified that the existing opt-in Find Group side-effect executor covers action 15 direct `SM_FIND_GROUP(16, List.of(instanceGroup))` packet intents.
- Preserved live `CM_FIND_GROUP` deferral; no packet boundary wiring changed.

## Java Source Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - Action 15 reads `playerOrTeamId` and `instanceMaskId`.
  - `runImpl` calls `FindGroupService.getInstance().showInstanceGroupMembersInfo(player, playerOrTeamId)`.
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
  - `showInstanceGroupMembersInfo` looks up the server-wide group by `playerObjectId`.
  - When present, Java sends `new SM_FIND_GROUP(16, List.of(instanceGroup))` directly to the requesting player.

## What Changed

- Added focused executor coverage to `FindGroupSideEffectDispatchExecutorServiceTests`.
  - Builds the action 15 member-info direct packet intent through `FindGroupRecruitmentPlanService`.
  - Executes that direct intent through the opt-in `FindGroupSideEffectDispatchExecutorService`.
  - Confirms the intended viewer is the registry recipient and the Java source breadcrumb is preserved.
- No product service code changed in this UOW.

## Validation

- Focused C#:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupSideEffectDispatchExecutorServiceTests|FullyQualifiedName~FindGroupRecruitmentPlanServiceTests|FullyQualifiedName~FindGroupSideEffectDispatchAuditServiceTests" --no-restore`
  - Result: passed, 37 tests.
  - Note: existing nullable/analyzer warnings were emitted from unrelated game-server and test files.
- Focused Java/Maven:
  - Not run.
  - Rationale: this UOW added focused C# executor evidence for reviewed Java action 15 source behavior. It did not change Java packet parsing, Java runtime behavior, or a Java-executable parity target.
- Broad .NET suite/build:
  - Intentionally skipped under the focused validation policy.
  - Rationale: test-only executor evidence; no live `CM_FIND_GROUP` wiring, packet primitive, persistence, crypto, scheduling, world-state mutation, or connection-dispatch branch changed.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.findgroup.FindGroupService.showInstanceGroupMembersInfo` | `Aion.GameServer.Services.FindGroupRecruitmentPlanService.ShowInstanceGroupMembersInfo`; `FindGroupSideEffectDispatchExecutorService` | Planner / Opt-In Side-Effect Executor | Partial | Unit Tested | Partial Parity | C# planner already emits the Java-shaped action 16 `SmFindGroup` member-info direct packet; executor test now proves the intent can flow through `IGameClientConnectionRegistry.SendPacketToPlayerAsync` when explicitly invoked. It is not wired into `CM_FIND_GROUP`. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupSideEffectDispatchExecutorServiceTests.ExecuteAsync_SendsActionFifteenMemberInfoIntentThroughConnectionRegistry` | Unit | Java `CM_FIND_GROUP` action 15 and `FindGroupService.showInstanceGroupMembersInfo` source review | Action 15 direct member-info packet intent can be sent by the opt-in executor to the requesting viewer | Focused C# unit test plus existing packet golden coverage | Does not execute encrypted real-client socket or live `CM_FIND_GROUP` boundary |

## Summary Metrics

- Total Java artifacts reviewed in this UOW: 2.
- Total artifacts ported or represented in this UOW: 2 C# surfaces.
- Total artifacts with verified parity: 0 broad artifacts.
- Total artifacts needing verification or partial parity: 1 table row.
- Total blocked artifacts: 0 new blocked artifacts; live `CM_FIND_GROUP` remains intentionally blocked.
- Estimated overall migration completion: unchanged, Phase 6 still in progress.

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection`.
- The side-effect executor is opt-in only and is not invoked by the packet boundary.
- Encrypted socket behavior, real-client behavior, packet order under live packet processing, visibility filtering beyond explicit predicates, and concurrency remain unverified.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupSideEffectDispatchExecutorServiceTests.cs`
- `docs/Phase-6-Session-2090-Completion.md`
- `docs/Phase-6-Session-2090-Handoff.md`

## Next Recommended Unit of Work

- Next sequential task: design a controlled boundary composition test that proves `CmFindGroup` can compose planner plus opt-in executor results without changing the live `GameServerConnection` switch.

Safe alternative candidates:

- Add Java-side fixture/golden evidence for another `FindGroupService` packet branch where a narrow Java target exists.
- Review concurrency/thread-safety implications for turning disabled `FindGroupRecruitmentPlanService` state into a live singleton later.
- Inspect whether another direct-packet branch should receive executor evidence or is already sufficiently covered by the generic executor/audit tests.
