# Phase 6 Bind-Point Teleport Known-List Two-Way Membership Adapter

Date: May 26, 2026
Unit of Work: UOW-1259
Scope: Add an explicit opt-in adapter from two-way operation plans to non-live membership metadata.
Source of truth: Java project.

## Summary

UOW-1259 adds `PlayerKnownListTwoWayMembershipAdapterService`. It consumes a `PlayerKnownListTwoWayOperationPlan` and, only when explicitly requested, applies membership add/remove steps to `PlayerKnownListMembershipService`.

The adapter is disabled by default. It preserves Java controller side-effect steps as descriptors and does not execute packets, world lifecycle, range checks, `canSee`, scheduler callbacks, movement, or socket fanout.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListTwoWayMembershipAdapterService.cs`:

- `ExecuteMembershipMutation=false` returns `Disabled` and mutates nothing.
- Rejected/non-planned operation plans return `SkippedRejectedPlan`.
- Add plans apply candidate-side membership before owner-side membership, following Java `findVisibleObjects`.
- Remove and clear plans apply membership steps in the order produced by the planner.
- `see`, `notSee`, and `notKnow` steps are preserved in `PreservedSideEffectSteps` and never executed.
- Applied membership entries use `PlayerKnownListMembershipUpdateReason.TwoWayOperationPlan`.
- Results remain `IsLive=false` and `IsJavaRegionKnownListParity=false`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListTwoWayMembershipAdapterServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 240 tests.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1259

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListTwoWayMembershipAdapterService` | Membership Adapter | Partial | Unit Tested | Partial Parity | Enabled adapter applies candidate-side membership before owner-side membership. It consumes a prebuilt plan and does not perform live region scan, range, `canSee`, or Java `putIfAbsent`. |
| `com.aionemu.gameserver.world.knownlist.KnownList.add` | `PlayerKnownListMembershipService.UpsertKnownPlayers` via two-way adapter | Membership Mutation | Partial | Unit Tested | Needs Verification | Metadata mutation only; disabled by default. C# upsert still differs from Java `putIfAbsent` and actual visibility update. |
| `com.aionemu.gameserver.world.knownlist.KnownList.del` | `PlayerKnownListTwoWayMembershipAdapterService` remove-step application | Membership Removal | Partial | Unit Tested | Needs Verification | Removes metadata in planned order but preserves `notSee`/`notKnow` as descriptors only. No controller hooks or exception-catching path executes. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | `PlayerKnownListTwoWayMembershipAdapterService` clear-step application | Clear Adapter | Partial | Unit Tested | Needs Verification | Applies planned membership removals but does not iterate a live known-list or apply Java animation semantics. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` with `TwoWayOperationPlan` reason | Known-Object / Membership Metadata | Partial | Unit Tested | Needs Verification | Visible flags are copied from plan side-effect descriptors. No Java `owner.canSee` implementation. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | two-way membership adapter plus existing fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Membership metadata can be seeded from operation plans when explicitly enabled. Live action `3` callback, socket fanout, cooldown, movement, and dispatch remain disabled. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Apply_DisabledDoesNotMutateMembership` | Unit / Adapter Guard | C# live-safety gate | Default adapter path mutates no membership. | C# safety guard. | Java has no disabled equivalent. |
| `Apply_SkipsRejectedPlansWithoutMutation` | Unit / Adapter Guard | `KnownList.add` rejected paths | Rejected plans are not applied. | Source-derived stop condition. | No Java runtime comparison. |
| `Apply_AddPlanMutatesCandidateFirstThenOwnerMembership` | Unit / Adapter | `KnownList.findVisibleObjects`; `KnownList.add` | Enabled add plan applies candidate membership before owner membership and marks entries with `TwoWayOperationPlan`. | Source-derived ordering. | No live region scan or `putIfAbsent`. |
| `Apply_RemovePlanMutatesOwnerFirstThenCandidateMembershipAndPreservesSideEffectDescriptors` | Unit / Adapter | `KnownList.del` | Enabled remove plan removes owner then candidate membership and preserves side-effect descriptors without executing them. | Source-derived ordering. | No packet/controller execution. |

## Remaining Risks

- Adapter is disabled by default and unwired.
- It mutates metadata only and does not represent live Java object ownership.
- Java `putIfAbsent`, `owner.canSee`, range, hidden/search behavior, and max visible-distance logic remain missing.
- Controller packet side effects are preserved as descriptors only.
- Java synchronization/locking and cross-list non-atomicity are not implemented as live behavior.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 1 two-way membership adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live region object store, 1 range/can-see visibility engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, 1 live movement path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled visibility/range operation planner for player-player known-list population. It should model Java's max visible-distance range rule and caller-supplied `canSee` results as metadata, then feed the two-way operation planner without executing live world visibility.

## Update After UOW-1260

`PlayerKnownListVisibilityRangePlanService` now models Java range and caller-supplied `canSee` metadata before producing two-way operation plans. It uses strict Java range comparison and max visible-distance metadata, but remains non-live and does not execute subclass visibility logic.

## Update After UOW-1261

`PlayerKnownListPopulationPlanService` now composes range plans and the membership adapter for supplied region snapshot candidates. The adapter remains opt-in and non-live inside that composition.

## Update After UOW-1262

`PlayerKnownListPlayerSideEffectPlanService` now provides concrete descriptor payloads for the `see` and `notSee` steps preserved by this adapter. The adapter still does not execute them; the next safe work is a composition layer that attaches these packet descriptors to operation-plan side-effect steps.
