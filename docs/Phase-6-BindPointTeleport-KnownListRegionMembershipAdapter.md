# Phase 6 Bind-Point Teleport Known-List Region Membership Adapter

Date: May 26, 2026
Unit of Work: UOW-1257
Scope: Adapt disabled region snapshot candidates into non-live player known-list membership metadata.
Source of truth: Java project.

## Summary

UOW-1257 adds `PlayerKnownListRegionMembershipAdapterService`. It consumes a supplied `PlayerKnownListRegionSnapshot` and upserts its candidate player ids into `PlayerKnownListMembershipService`.

This remains non-live metadata. It does not scan live map regions, compute Java range/can-see state, perform two-way world-object known-list mutation, send controller packets, or wire bind-point action `3` fanout into scheduled callbacks.

The adapter preserves existing membership by default because Java can keep objects known while toggling visibility. A caller can opt into removing missing snapshot candidates, but that is still a C# approximation and not verified Java parity.

## C# Implementation

Added `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListRegionMembershipAdapterService.cs`:

- `PlayerKnownListRegionMembershipAdapterRequest` carries a region snapshot, stale-removal preference, and candidate visible-state preference.
- `PlayerKnownListRegionMembershipAdapterResult` records membership snapshot, candidate/upsert/remove counts, flags, Java source, and live/parity status.
- `ApplySnapshot` upserts region candidate ids with `PlayerKnownListMembershipUpdateReason.RegionSnapshotRefresh`.
- Existing membership is preserved by default.
- Optional stale removal can delete known players absent from the supplied region snapshot.
- Candidate visible state is supplied by caller because this slice does not port Java `owner.canSee`.

Updated `PlayerKnownListMembershipUpdateReason` with `RegionSnapshotRefresh`.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListRegionMembershipAdapterServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownList" --nologo` passed 224 tests.
- No Java runtime comparison was executed.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1257

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `Aion.GameServer.Services.PlayerKnownListRegionMembershipAdapterService` | Known-List Population Adapter | Partial | Unit Tested | Partial Parity | Region snapshot candidate ids can now seed membership metadata. Missing Java range, can-see, already-known checks, live region scan, two-way add order, and controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.add` | `PlayerKnownListMembershipService.UpsertKnownPlayers` via region adapter | Known-List Membership Mutation | Partial | Unit Tested | Needs Verification | Owner-side metadata upsert only. Does not mutate the candidate object's known-list first like Java. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | optional stale removal in `PlayerKnownListRegionMembershipAdapterService` | Known-List Cleanup / Visibility | Partial | Unit Tested | Needs Verification | Adapter preserves existing membership by default because Java can retain invisible known objects. Optional stale removal is not a Java-verified range/can-see implementation. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `PlayerKnownListMembershipEntry` with `RegionSnapshotRefresh` reason | Known-Object / Visibility Metadata | Partial | Unit Tested | Needs Verification | Caller supplies visible state because Java `owner.canSee` is not ported. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` action `3` fanout | region snapshot adapter plus existing known-list fanout metadata | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Better non-live membership seeding exists, but no scheduled callback, socket execution, movement, or live dispatch wiring was added. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ApplySnapshot_UpsertsRegionCandidatesAsNonLiveMembershipMetadata` | Unit / Adapter | `KnownList.findVisibleObjects` | Region candidates become non-live membership entries with `RegionSnapshotRefresh`. | Source-derived metadata bridge. | No live region scan or Java runtime comparison. |
| `ApplySnapshot_CanPreserveExistingMembershipByDefaultBecauseJavaCanKeepInvisibleKnownObjects` | Unit / Adapter | `KnownObject.visible` separate from membership | Existing membership is retained by default, including invisible entries. | Source-derived conservative policy. | Does not recompute Java visibility. |
| `ApplySnapshot_CanRemoveMissingSnapshotCandidatesWhenRequested` | Unit / Adapter | `forgetObjectsOrUpdateVisibility` | Optional stale removal deletes existing entries absent from snapshot. | C# approximation only. | No range/can-see parity. |
| `ApplySnapshot_CanRecordInvisibleCandidateStateWhenCallerDoesNotHaveCanSeeParity` | Unit / Adapter | `KnownList.updateVisibility`; `KnownObject.visible` | Caller can mark candidate visible state false. | Metadata support for visible-vs-known separation. | No Java `owner.canSee` implementation. |

## Remaining Risks

- Adapter is non-live and unwired.
- No live region object store exists.
- No two-way Java known-list mutation exists.
- Range, max visible-distance negotiation, `canSee`, hidden/search behavior, and already-known checks remain missing.
- Controller `see`/`notSee`/`notKnow` side effects remain missing.
- Optional stale removal is an approximation and must not be treated as verified Java parity.
- Threading differs from Java synchronized update over `ConcurrentHashMap`.
- Serialization, date/time, precision/rounding, and reflection behavior did not change.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 region membership adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 live region object store, 1 bidirectional known-list mutation engine, 1 range/can-see visibility engine, 1 controller packet side-effect dispatcher, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add a disabled two-way membership operation planner for player-player known-list add/remove semantics. It should plan Java's `newObject.getKnownList().add(owner)` before owner-side add, remain non-live, and document that actual live object ownership and locking are still missing.

## Update After UOW-1258

`PlayerKnownListTwoWayOperationPlanService` now provides that disabled planner. It records candidate-first add order, owner-first remove/clear order, visibility-driven `see`/`notSee` descriptors, and `notKnow` descriptors without mutating live membership or executing controller packets.

## Update After UOW-1259

`PlayerKnownListTwoWayMembershipAdapterService` now consumes those operation plans and can apply membership metadata only when explicitly enabled. Region snapshot membership remains separate from live world population and still does not execute Java visibility or packet side effects.
