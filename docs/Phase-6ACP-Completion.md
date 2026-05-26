# Phase 6ACP Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1258
Status: Phase 6 continues; C# now has disabled region snapshot, region membership adapter, and two-way known-list operation planner metadata. Full Java live region storage, mutation, visibility, and bind-point dispatch remain disabled.

## Session Summary

UOW-1258 added `PlayerKnownListTwoWayOperationPlanService`, a non-live planner for Java player-player known-list add/remove/clear ordering. The planner records candidate-first add order, owner-first remove/clear order, visible-entry `notSee` before `notKnow`, and clear-specific Java source breadcrumbs.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListTwoWayOperationPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListTwoWayOperationPlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListRegionMembershipAdapter.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACP-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListTwoWayOperationPlanServiceTests" --nologo` passed 12 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 236 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1258

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListTwoWayOperationPlanService.PlanAdd` | Known-List Add Planner | Partial | Unit Tested | Partial Parity | Plans candidate-side add before owner-side add and models duplicate/awareness stop conditions. Does not execute live region scan, range check, `canSee`, `putIfAbsent`, or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.add` | `PlayerKnownListTwoWayOperationPlanService.PlanAdd` | Membership Add Planner | Partial | Unit Tested | Needs Verification | Models Java add preconditions and visibility-triggered `see` descriptors. Does not mutate `PlayerKnownListMembershipService` or preserve Java `ConcurrentHashMap` runtime behavior. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListTwoWayOperationPlanService.PlanRemove` | Membership Remove Planner | Partial | Unit Tested | Needs Verification | Models remove, optional `notSee`, and `notKnow` order. Does not execute packets, controller hooks, exception catching, or live membership removal. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | `PlayerKnownListTwoWayOperationPlanService.PlanClearPair` | Clear Planner | Partial | Unit Tested | Needs Verification | Models owner-side then candidate-side removal and clear-specific Java animation breadcrumbs. Does not iterate a live known-list or apply animations. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | visibility flags in `PlayerKnownListTwoWayOperationState` and side-effect steps | Known-Object / Visibility Metadata | Partial | Unit Tested | Needs Verification | Defaults visibility false unless caller supplies true, matching Java `KnownObject.visible` default. No Java `owner.canSee` implementation. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | two-way planner plus existing non-live known-list fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Planner clarifies future membership ordering before fanout. No scheduled callback, socket execution, movement, or live dispatch wiring was added. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 two-way operation planner service plus 12 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live bidirectional known-list mutation engine, 1 range/can-see visibility engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Planner is non-live and unwired.
- No live region object store exists.
- No live two-way membership mutation exists.
- No Java `owner.canSee`, range, hidden/search, or max visible-distance behavior executes.
- Controller `see`/`notSee`/`notKnow` side effects are descriptors only.
- Java exception-catching in notification hooks is not executed.
- Java per-known-list synchronization and C# future locking remain unimplemented for live mutation.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled operation-plan-to-membership adapter for `PlayerKnownListTwoWayOperationPlan`.
- Scope:
  - Consume add/remove/clear plans.
  - Apply membership changes to `PlayerKnownListMembershipService` only when explicitly requested.
  - Respect plan status and execute no-op for rejected plans.
  - Preserve side-effect descriptors as metadata only.
  - Do not wire `GameServerConnection`, sockets, world lifecycle, scheduler, or movement.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Operation-plan-to-membership adapter | new service/tests/docs | Medium | Best next executable step. Keep non-live and explicit opt-in. |
| B | Known-list packet side-effect design | docs only | Low/Medium | Useful before dispatching `see`/`notSee` packets. |
| C | Region object store design audit | docs only | Low/Medium | Useful before live world lifecycle work. |
| D | Two-way planner edge tests | tests only | Low | Add partial one-sided remove and clear visibility/animation descriptor cases. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect how to safely apply planned two-way operations to current membership metadata without implying Java atomicity | read-only Java/C# inspection | all writes |
| Worker | Implement disabled operation-plan-to-membership adapter and focused tests | new service/test/doc files only | `GameServerConnection`, socket executor live wiring, world mutation paths |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes outside the adapter |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Region object storage and controller packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionMembershipAdapterService.cs`
- Latest completed commits:
  - `02e62487c [Phase 6][UOW-1256] Add player known-list region snapshot model`
  - `13caa7245 [Phase 6][UOW-1257] Add player known-list region membership adapter`
  - next commit should be `[Phase 6][UOW-1258] Add player known-list two-way operation planner`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
