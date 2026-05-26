# Phase 6ACR Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1260
Status: Phase 6 continues; C# now has non-live region snapshot, region membership, two-way operation, membership adapter, and visibility/range planning for player known-list population. Live world known-list population and bind-point dispatch remain disabled.

## Session Summary

UOW-1260 added `PlayerKnownListVisibilityRangePlanService`, which models Java `KnownList.isInRange` using max visible distance, same world/instance checks, strict squared-distance comparison, and caller-supplied `canSee` results before producing a two-way operation plan.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListVisibilityRangePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListVisibilityRangePlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListVisibilityRangePlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListTwoWayMembershipAdapter.md`
- `docs/Phase-6-BindPointTeleport-KnownListTwoWayOperationPlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACR-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListVisibilityRangePlanServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 245 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1260

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.isInRange` | `Aion.GameServer.Services.PlayerKnownListVisibilityRangePlanService` | Range Planner | Partial | Unit Tested | Partial Parity | Models max visible-distance rule and delegates to add/remove operation plans. Does not execute live Java objects or region scans. |
| `com.aionemu.gameserver.utils.PositionUtil.isInRange` | `PlayerKnownListVisibilityRangePlanService.Plan` | Utility / Range Check | Partial | Unit Tested | Partial Parity | Models same-world/instance and strict squared-distance comparison. Bound-radius overloads are not modeled because known-list uses center-to-center default. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.getVisibleDistance` | `PlayerKnownListVisibilityRangeObject.VisibleDistance` | Visibility Distance Metadata | Partial | Unit Tested | Needs Verification | Default 95m is available through `WorldVisibility.DefaultVisibleDistance`; subclass overrides are caller-supplied metadata, not live Java object behavior. |
| `com.aionemu.gameserver.model.gameobjects.VisibleObject.canSee` | `PlayerKnownListVisibilityRangeObject.CanSeeOther` | Visibility Predicate Metadata | Partial | Unit Tested | Needs Verification | Caller supplies `canSee` result. Hidden/search/subclass behavior is not ported. |
| `com.aionemu.gameserver.world.knownlist.KnownList.updateVisibility` | visibility flags passed into `PlayerKnownListTwoWayOperationPlanService` | Visibility Side-Effect Planning | Partial | Unit Tested | Needs Verification | Produces see/not-see descriptors through operation plans. Does not execute controller packet side effects. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | visibility/range planner plus known-list metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Range-aware metadata can feed future known-list membership. Live action `3`, sockets, movement, scheduler, and dispatch remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 visibility/range planner service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live subclass `canSee` engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, 1 live movement path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Planner is non-live and unwired.
- No live region object store exists.
- No Java subclass `canSee`, hide/search, stealth, or awareness behavior executes.
- Bound-radius `centerToCenter=false` behavior is not modeled because Java known-list uses the default center-to-center call.
- Controller side effects remain descriptors only.
- Existing `WorldVisibility` remains a broader approximation and should not be treated as this Java range planner.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled composition service for non-live known-list population planning.
- Scope:
  - Consume `PlayerKnownListRegionSnapshot` candidates.
  - Build `PlayerKnownListVisibilityRangePlan` values for supplied candidate facts.
  - Produce/apply two-way membership adapter results only when explicitly enabled.
  - Keep side effects as descriptors.
  - Do not wire world lifecycle, sockets, scheduler, movement, or `GameServerConnection`.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Known-list population composition service | new service/tests/docs | Medium | Best next executable step. Keep non-live. |
| B | Known-list packet side-effect design | docs only | Low/Medium | Useful before dispatching `see`/`notSee` packets. |
| C | Region object store design audit | docs only | Low/Medium | Useful before live world lifecycle work. |
| D | Visibility range edge tests | tests only | Low | Add z-distance and awareness rejection cases. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect composition inputs needed to join region snapshot candidates with range/visibility facts | read-only Java/C# inspection | all writes |
| Worker | Implement disabled population composition service and focused tests | new service/test/doc files only | `GameServerConnection`, socket executor live wiring, world mutation paths |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes outside the planner |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Region object storage and controller packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/model/gameobjects/VisibleObject.java`
  - `game-server/src/com/aionemu/gameserver/utils/PositionUtil.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionSnapshotService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListVisibilityRangePlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayMembershipAdapterService.cs`
- Latest completed commits:
  - `6e435df58 [Phase 6][UOW-1258] Add player known-list two-way operation planner`
  - `908835d1e [Phase 6][UOW-1259] Add player known-list two-way membership adapter`
  - next commit should be `[Phase 6][UOW-1260] Add player known-list visibility range planner`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
