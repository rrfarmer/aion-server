# Phase 6ACS Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1261
Status: Phase 6 continues; C# now has a non-live composition layer over region snapshots, visibility/range plans, two-way operation plans, and optional membership metadata application. Live world known-list population and bind-point dispatch remain disabled.

## Session Summary

UOW-1261 added `PlayerKnownListPopulationPlanService`, which composes region snapshot candidates with supplied candidate facts, builds visibility/range plans, routes operation plans through the two-way membership adapter, and optionally applies membership metadata when explicitly enabled.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListPopulationPlanServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListPopulationComposition.md`
- `docs/Phase-6-BindPointTeleport-KnownListVisibilityRangePlanner.md`
- `docs/Phase-6-BindPointTeleport-KnownListTwoWayMembershipAdapter.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACS-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPlanServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 249 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1261

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListPopulationPlanService` | Known-List Population Composition | Partial | Unit Tested | Partial Parity | Composes region candidates, range plans, operation plans, and optional metadata mutation. Does not execute Java synchronized update, live clear/update-before-find ordering, or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListPopulationPlanService.Plan` | Candidate Population Composition | Partial | Unit Tested | Partial Parity | Iterates supplied region snapshot candidates and applies Java-shaped range/two-way add metadata. Does not scan live `MapRegion` objects or skip live already-known objects except through supplied facts. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | out-of-range plans from `PlayerKnownListVisibilityRangePlanService` within composition | Cleanup / Visibility Composition | Partial | Unit Tested | Needs Verification | Existing known state can produce remove plans and metadata removals. Does not update live visibility or execute `notSee`/`notKnow`. |
| `com.aionemu.gameserver.world.knownlist.KnownList.add` / `del` | `PlayerKnownListTwoWayOperationPlanService`; `PlayerKnownListTwoWayMembershipAdapterService` through composition | Membership Operation Composition | Partial | Unit Tested | Needs Verification | Add/remove ordering is composed and can apply metadata only by explicit opt-in. Java `putIfAbsent`, exception behavior, locking, and controller hooks remain missing. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | candidate facts and membership entries in population plan | Known-Object / Visibility Metadata | Partial | Unit Tested | Needs Verification | Uses caller-supplied known/visible state; no Java `KnownObject` live object or `owner.canSee` recomputation. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | non-live known-list population plan plus existing fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Population composition can seed metadata for future fanout. Live scheduled action `3`, sockets, movement, cooldown, and dispatch remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 population composition service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live synchronized known-list update engine, 1 live subclass `canSee` engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, 1 live movement path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Composition service is non-live and unwired.
- No live region object store exists.
- Java synchronized update order is not executed.
- Java `putIfAbsent`, `owner.canSee`, hidden/search/subclass visibility, and controller side effects remain missing.
- Metadata mutation is explicit opt-in and not a live world known-list.
- Existing `WorldVisibility` remains an approximation and is not upgraded by this composition service.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled packet side-effect plan for player-player known-list `see` and `notSee` transitions.
- Scope:
  - Model Java `PlayerController.see(Player)` packet ordering: `SM_PLAYER_INFO`, `SM_MOTION`, optional ride `SM_EMOTION`, optional `SM_PLAYER_STANCE`.
  - Model Java `PlayerController.notSee(Player)` delete behavior when viewer remains spawned.
  - Keep descriptors non-sending.
  - Do not wire sockets, `GameServerConnection`, movement, scheduler, or live known-list callbacks.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player see/notSee packet side-effect planner | new service/tests/docs | Medium | Best next executable step. Descriptor-only. |
| B | Region object store design audit | docs only | Low/Medium | Useful before live world lifecycle work. |
| C | Composition service edge tests | tests only | Low | Add duplicate facts and one-sided stale known-state cases. |
| D | Known-list clear animation descriptor planner | new service/tests/docs | Medium | Related but should not overlap with player packet planner. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect Java `PlayerController.see(Player)` and `notSee(Player)` packet ordering and required C# packet names | read-only Java/C# inspection | all writes |
| Worker | Implement descriptor-only player known-list packet side-effect planner and focused tests | new service/test/doc files only | `GameServerConnection`, socket executor live wiring, world mutation paths |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes outside the planner |

### Do Not Parallelize

- Live packet sends with known-list mutation.
- `GameServerConnection` dispatch with descriptor-only side effects.
- Region object storage and controller packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`
  - packet classes under `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets`
- Latest completed commits:
  - `908835d1e [Phase 6][UOW-1259] Add player known-list two-way membership adapter`
  - `6c60c7c3d [Phase 6][UOW-1260] Add player known-list visibility range planner`
  - next commit should be `[Phase 6][UOW-1261] Add player known-list population composition`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
