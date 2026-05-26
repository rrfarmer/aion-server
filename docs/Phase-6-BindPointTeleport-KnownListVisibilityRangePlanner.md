# Phase 6 Bind-Point Teleport Known-List Visibility Range Planner

Date: May 26, 2026
Unit of Work: UOW-1260
Scope: Add a disabled visibility/range planner for player-player known-list population metadata.
Source of truth: Java project.

## Summary

UOW-1260 adds `PlayerKnownListVisibilityRangePlanService`, a pure metadata planner that models Java `KnownList.isInRange` and caller-supplied `canSee` results before producing a two-way known-list operation plan.

This planner deliberately does not use the existing `WorldVisibility` approximation for parity claims. Java uses same world and instance checks, the maximum visible distance of both objects, and a strict squared-distance comparison.

## Java Source Findings

- `KnownList.isInRange` uses `Math.max(getVisibleDistance(), newObject.getKnownList().getVisibleDistance())`.
- `VisibleObject.getVisibleDistance()` defaults to `95`.
- `PositionUtil.isInRange(VisibleObject, VisibleObject, range)` rejects different world ids and different instance ids.
- `PositionUtil.isInRange(float...)` returns `dx * dx + dy * dy + dz * dz < range * range`, a strict less-than comparison.
- `KnownList.updateVisibility` uses `owner.canSee(object)` separately from range membership.
- `VisibleObject.canSee` defaults to `object != null`, but subclasses can override behavior.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListVisibilityRangePlanService.cs`:

- `PlayerKnownListVisibilityRangeObject` carries object id, world, instance, coordinates, visible distance, awareness, caller-supplied `canSee`, and existing known state.
- `PlayerKnownListVisibilityRangePlanService.Plan`:
  - uses the maximum of owner and candidate visible distances;
  - checks same world and same instance;
  - uses Java strict `< range * range` squared comparison;
  - uses caller-supplied `canSee` values to set operation-plan visibility descriptors;
  - returns an add operation plan when in range;
  - returns a remove operation plan when out of range;
  - remains non-live and marks `IsJavaRegionKnownListParity=false`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListVisibilityRangePlanServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 245 tests.
- No Java runtime comparison was executed.
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

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Plan_UsesStrictJavaRangeComparisonAtBoundary` | Unit / Range Planner | `PositionUtil.isInRange(float...)` | Distance exactly equal to range is out of range. | Source-derived boundary rule. | No Java runtime comparison. |
| `Plan_UsesMaximumVisibleDistanceAcrossBothKnownLists` | Unit / Range Planner | `KnownList.isInRange` | Candidate visible distance can extend detection range through max rule. | Source-derived max-distance rule. | No subclass live object lookup. |
| `Plan_DifferentInstanceIsOutOfRangeEvenWhenCoordinatesOverlap` | Unit / Range Planner | `PositionUtil.isInRange(VisibleObject, VisibleObject)` | Different instance rejects range even at identical coordinates. | Source-derived instance check. | No live world object state. |
| `Plan_InRangeUsesCallerSuppliedCanSeeResultsForSeeDescriptors` | Unit / Range Planner | `KnownList.updateVisibility`; `VisibleObject.canSee` | Caller-supplied `canSee` controls see descriptors after add. | Metadata equivalent for visibility input. | Does not implement hide/search visibility. |
| `Plan_OutOfRangeExistingVisibleMembershipPlansRemoveSideEffects` | Unit / Range Planner | `forgetObjectsOrUpdateVisibility`; `KnownList.del` | Existing visible membership out of range plans remove, `notSee`, then `notKnow` per side. | Source-derived removal sequence. | No controller packet execution. |

## Remaining Risks

- Planner is non-live and unwired.
- No live region object store exists.
- No Java subclass `canSee`, hide/search, stealth, or awareness behavior executes.
- Bound-radius `centerToCenter=false` behavior is not modeled because Java known-list uses the default center-to-center call.
- Controller side effects remain descriptors only.
- Existing `WorldVisibility` remains a broader approximation and should not be treated as this Java range planner.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 visibility/range planner service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live region object store, 1 live subclass `canSee` engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, 1 live movement path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled composition service that consumes region snapshot candidates, visibility/range plans, two-way operation plans, and the membership adapter into one non-live known-list population plan. Keep it explicitly disabled and do not wire live server flows.

## Update After UOW-1261

`PlayerKnownListPopulationPlanService` now provides that disabled composition layer. It connects region snapshot candidates, visibility/range plans, two-way operation plans, and the membership adapter, while preserving the default no-mutation behavior and keeping side effects descriptor-only.

## Update After UOW-1262

`PlayerKnownListPlayerSideEffectPlanService` now expands those side-effect descriptors into Java packet-intent metadata for player see/notSee transitions. Range planning remains non-live and still depends on caller-supplied `canSee` and known-state facts.
