# Phase 6 Bind-Point Teleport Known-List Population Composition

Date: May 26, 2026
Unit of Work: UOW-1261
Scope: Compose region snapshot candidates, visibility/range planning, two-way operation planning, and optional membership metadata application.
Source of truth: Java project.

## Summary

UOW-1261 adds `PlayerKnownListPopulationPlanService`, a disabled composition service for non-live known-list population planning. It joins existing prerequisite pieces:

- `PlayerKnownListRegionSnapshot`
- `PlayerKnownListVisibilityRangePlanService`
- `PlayerKnownListTwoWayOperationPlanService`
- `PlayerKnownListTwoWayMembershipAdapterService`

The default path is descriptor-only and mutates no membership. When explicitly enabled, it applies membership metadata through the existing adapter, but still does not execute live world lifecycle, controller packet side effects, scheduler callbacks, movement, or socket fanout.

## Java Source Findings

- Java `KnownList.update()` runs `forgetObjectsOrUpdateVisibility()` before `findVisibleObjects()`.
- `findVisibleObjects()` scans region candidates, checks awareness, skips already-known objects, checks range, then performs candidate-side add before owner-side add.
- `forgetObjectsOrUpdateVisibility()` removes out-of-range pairs from both known lists and updates visibility for in-range pairs.
- C# composition still uses supplied metadata rather than live Java object collections.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListPopulationPlanService.cs`:

- `PlayerKnownListPopulationCandidateFact` supplies candidate coordinates, visible distance, awareness, `canSee`, and existing known state.
- `PlayerKnownListPopulationPlanRequest` carries a region snapshot, owner range object, candidate facts, and explicit membership mutation flag.
- `PlayerKnownListPopulationPlanService.Plan`:
  - iterates region snapshot candidates in snapshot order;
  - records missing candidate facts without producing range or membership plans;
  - builds visibility/range plans for supplied facts;
  - passes operation plans through the two-way membership adapter;
  - mutates metadata only when `ExecuteMembershipMutation=true`;
  - preserves controller side effects as descriptors.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListPopulationPlanServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 249 tests.
- No Java runtime comparison was executed.
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

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_DisabledComposesRegionCandidatesThroughRangePlansWithoutMutatingMembership` | Unit / Composition | `KnownList.findVisibleObjects`; `isInRange` | Region candidates flow through range plans and disabled adapter without mutating membership. | Source-derived composition path. | No live region scan. |
| `Plan_EnabledAppliesInRangeMembershipAndPreservesOutOfRangeNoOp` | Unit / Composition | `KnownList.add`; `KnownList.del` | Explicitly enabled plan applies in-range two-way metadata and skips rejected/out-of-range no-op. | Source-derived operation plan behavior. | Metadata only; no Java runtime comparison. |
| `Plan_TracksMissingCandidateFactsWithoutCreatingRangePlan` | Unit / Composition Guard | C# safety boundary | Missing candidate facts are recorded without range or membership plans. | C# guard. | Java has live objects instead of supplied facts. |
| `Plan_UsesCandidateFactsForVisibilityAndExistingKnownState` | Unit / Composition | `forgetObjectsOrUpdateVisibility`; `KnownList.del` | Existing visible membership out of range produces removal metadata and preserved side-effect descriptors. | Source-derived cleanup order. | No controller packet execution. |

## Remaining Risks

- Composition service is non-live and unwired.
- No live region object store exists.
- Java synchronized update order is not executed.
- Java `putIfAbsent`, `owner.canSee`, hidden/search/subclass visibility, and controller side effects remain missing.
- Metadata mutation is explicit opt-in and not a live world known-list.
- Existing `WorldVisibility` remains an approximation and is not upgraded by this composition service.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 population composition service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live synchronized known-list update engine, 1 live subclass `canSee` engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, 1 live movement path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled packet side-effect plan for player-player known-list `see` and `notSee` transitions. It should model Java `PlayerController.see(Player)` packet ordering (`SM_PLAYER_INFO`, `SM_MOTION`, ride emotion, stance) and `notSee` delete behavior as descriptors only.

## Update After UOW-1262

`PlayerKnownListPlayerSideEffectPlanService` now supplies those descriptor-only player side-effect plans. The next safe step is to attach those packet descriptors to existing two-way operation-plan `see`/`notSee` steps without executing live packet sends or world lifecycle callbacks.

## Update After UOW-1263

`PlayerKnownListOperationSideEffectAttachmentService` now attaches player packet side-effect descriptors to two-way operation-plan `see`/`notSee` steps. Population composition still does not carry those attachments in its result; that is the next safe non-live integration step.
