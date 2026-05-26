# Phase 6 Bind-Point Teleport Known-List Two-Way Operation Planner

Date: May 26, 2026
Unit of Work: UOW-1258
Scope: Add a disabled planner for Java player-player known-list two-way add/remove ordering.
Source of truth: Java project.

## Summary

UOW-1258 adds `PlayerKnownListTwoWayOperationPlanService`, a pure metadata planner for Java `KnownList` two-way add/remove/clear ordering.

The planner does not mutate live membership and does not send controller packets. It records the ordered operations that later live work must preserve:

- candidate-side add before owner-side add;
- owner-side removal before candidate-side removal;
- `notSee` before `notKnow` when a removed entry was visible;
- owner-side clear uses `ObjectDeleteAnimation.NONE` while the other side uses the caller-supplied animation.

This remains a prerequisite slice, not Java region known-list parity.

## Java Source Findings

- `KnownList.findVisibleObjects()` calls `newObject.getKnownList().add(owner)` before owner-side `add(newObject)`.
- `KnownList.add` rejects awareness first, creates a `KnownObject`, uses `putIfAbsent`, and returns false on duplicate.
- `KnownObject.visible` defaults to false and only flips during `updateVisibility`.
- `KnownList.del` removes membership first, then sends `notSee` if visibility changed to false, then sends `notKnow`.
- `KnownList.clear(animation)` removes owner-side known objects with `ObjectDeleteAnimation.NONE`, then removes owner from each other known-list with the supplied animation.
- Java synchronizes `update` and `clear` per known-list, but the cross-list two-way relation is not globally atomic.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayOperationPlanService.cs`:

- `PlanAdd` models candidate-side add before owner-side add.
- `PlanAdd` rejects self, owner awareness failure, owner duplicate, candidate awareness failure, and candidate duplicate before planning both add steps.
- Add plans default to no `see` side-effect descriptors unless caller supplies visibility true.
- `PlanRemove` models owner-side remove before candidate-side remove.
- `PlanRemove` emits `notSee` before `notKnow` only for sides marked visible.
- `PlanClearPair` records clear-specific Java breadcrumbs and the same owner-first removal ordering.
- All results mark `MutatesLiveMembership=false`, `IsLive=false`, and `IsJavaRegionKnownListParity=false`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListTwoWayOperationPlanServiceTests" --nologo` passed 12 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 236 tests.
- No Java runtime comparison was executed.
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

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PlanAdd_SchedulesCandidateKnownListAddBeforeOwnerAdd` | Unit / Planner | `KnownList.findVisibleObjects` | Candidate add is planned before owner add. | Source-derived ordering. | No live mutation. |
| `PlanAdd_DefaultsNewKnownObjectVisibilityToFalseAndDoesNotPlanSeeSideEffects` | Unit / Planner | `KnownObject.visible`; `KnownList.updateVisibility` | New known-object visibility defaults false without `see` side-effect descriptors. | Source-derived default. | No `canSee` execution. |
| `PlanAdd_WhenVisibleTruePlansSeeSideEffectsAfterEachMembershipAdd` | Unit / Planner | `KnownList.updateVisibility`; `notifySee` | Visible add plans `see` descriptors after each membership add. | Source-derived ordering. | No packet/controller execution. |
| `PlanAdd_RejectsSelfAndStopsBeforeCandidateAdd` | Unit / Planner | `KnownList.isAwareOf` | Self add is rejected with no steps. | Source-derived guard. | Player-only planner. |
| `PlanAdd_StopsWhenJavaAddPreconditionsWouldFail` | Unit / Planner | `KnownList.add`; `putIfAbsent` | Duplicate and awareness failures produce no add steps. | Source-derived stop conditions. | Does not execute Java `putIfAbsent`. |
| `PlanRemove_SchedulesOwnerRemovalBeforeCandidateRemoval` | Unit / Planner | `forgetObjectsOrUpdateVisibility`; `del` | Owner-side removal is planned before candidate-side removal, with `notKnow` descriptors for invisible entries. | Source-derived ordering. | No live removal. |
| `PlanRemove_WhenVisiblePlansRemoveThenNotSeeThenNotKnowPerSide` | Unit / Planner | `KnownList.del` | Visible removal plans remove, `notSee`, then `notKnow` per side. | Source-derived side-effect order. | No packet/controller execution. |
| `PlanClearPair_UsesClearSpecificJavaSourceAndOwnerFirstOrdering` | Unit / Planner | `KnownList.clear` | Clear plan uses owner-first ordering and clear-specific Java breadcrumbs. | Source-derived ordering. | Animation values are documented, not executed. |
| `PlanRemove_ReturnsNothingToRemoveWhenNeitherSideKnowsTheOther` | Unit / Planner | `KnownList.del` absent entry behavior | No removal steps when neither side knows the other. | Source-derived no-op behavior. | No live map lookup. |

## Remaining Risks

- Planner is non-live and unwired.
- No live region object store exists.
- No live two-way membership mutation exists.
- No Java `owner.canSee`, range, hidden/search, or max visible-distance behavior executes.
- Controller `see`/`notSee`/`notKnow` side effects are descriptors only.
- Java exception-catching in notification hooks is not executed.
- Java per-known-list synchronization and C# future locking remain unimplemented for live mutation.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 two-way operation planner service plus 12 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live bidirectional known-list mutation engine, 1 range/can-see visibility engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled operation-plan-to-membership adapter that consumes `PlayerKnownListTwoWayOperationPlan` values and applies them to `PlayerKnownListMembershipService` only when explicitly requested. Keep it non-live, do not wire sockets or world lifecycle, and document that controller side effects remain descriptors.

## Update After UOW-1259

`PlayerKnownListTwoWayMembershipAdapterService` now provides that explicit opt-in adapter. It applies membership steps from planned add/remove/clear operations to non-live metadata and preserves `see`/`notSee`/`notKnow` steps as descriptors. It remains disabled by default and is not wired to world lifecycle or sockets.

## Update After UOW-1260

`PlayerKnownListVisibilityRangePlanService` now feeds the two-way operation planner from Java-shaped range and `canSee` metadata, including strict range boundary behavior and max visible-distance selection.
