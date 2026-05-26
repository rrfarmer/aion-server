# Phase 6 Bind-Point Teleport Known-List Membership Refresh

Date: May 26, 2026
Unit of Work: UOW-1253
Scope: Add a test-first C# approximation seam for player known-list membership refresh.
Source of truth: Java project.

## Summary

UOW-1253 adds `PlayerKnownListMembershipRefreshService`. It refreshes `PlayerKnownListMembershipService` from supplied online `Player` candidates using `WorldVisibility`.

This is intentionally not full Java known-list parity. It is a safe prerequisite seam for future live population work.

## Java Source Findings

- Java `KnownList.update()` performs `forgetObjectsOrUpdateVisibility()` then `findVisibleObjects()`.
- `findVisibleObjects()` scans neighboring `MapRegion` objects, not a flat online-player list.
- Java creates two-way known-list relations by adding owner to the new object before adding the new object to owner.
- Java can retain known-but-not-visible objects by updating cached `KnownObject.visible`.
- Java cleanup sends `notSee` and `notKnow` side effects through controllers.

## C# Implementation

- `RefreshOwnerFromOnlinePlayers` filters supplied online players through `WorldVisibility`.
- Owner/source is excluded.
- Stale entries absent from the current visible candidate set are removed.
- `RefreshAllFromOnlinePlayers` creates a bidirectional distance approximation over supplied players.
- `ClearOwnerForLogout` and `RemoveDepartingPlayerFromKnownLists` provide metadata cleanup helpers.
- The service remains unwired from live enter/move/logout.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipRefreshServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownListMembership" --nologo` passed 212 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1253

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListMembershipRefreshService` | Known-List Refresh / Approximation | Partial | Unit Tested | Partial Parity | Current-distance approximation only. No region scan, synchronization parity, or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `RefreshOwnerFromOnlinePlayers` | Known-List Population | Partial | Unit Tested | Partial Parity | Supplied online candidates plus `WorldVisibility`; no Java `MapRegion` neighbor scan. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | stale removal in `RefreshOwnerFromOnlinePlayers` | Known-List Cleanup | Partial | Unit Tested | Needs Verification | Removes stale distance entries but does not distinguish visibility-only changes. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | `ClearOwnerForLogout`; `RemoveDepartingPlayerFromKnownLists` | Known-List Cleanup / Logout Metadata | Partial | Unit Tested | Needs Verification | Metadata cleanup only; no `notSee`/`notKnow` packets or live world scan. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | membership refresh plus known-list fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Approximate refresh can seed metadata, but live bind-point fanout still remains disabled. |

## Remaining Risks

- This is not Java region known-list parity.
- Hidden/invisible-but-known behavior is not modeled.
- Service is not registered or wired into live connection/world flows.
- Java controller side effects and concurrency behavior remain missing.

## Next Recommended Unit of Work

Add a documentation/design audit for full Java-equivalent player known-list population requirements, or add a disabled adapter that converts `IGameClientConnectionRegistry.ForEachOnlinePlayer` snapshots into refresh inputs without wiring live dispatch.

## Update After UOW-1254

`PlayerKnownListMembershipRegistryRefreshAdapterService` now adapts `IGameClientConnectionRegistry.ForEachOnlinePlayer` snapshots into `PlayerKnownListMembershipRefreshService`. It remains disabled by default and unwired from `GameClientSocketServer` and `GameServerConnection`.

The adapter preserves the approximation flags: registry snapshots plus `WorldVisibility` are not Java `MapRegion` known-list parity.

## Update After UOW-1255

`docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md` now records the full Java player known-list population requirements before live wiring. The audit confirms that registry snapshots and `WorldVisibility` are only an approximation and cannot replace Java `World.spawn`, `World.updatePosition`, `World.despawn`, `MapRegion` neighbor scans, bidirectional `KnownList` mutation, cached `KnownObject.visible`, or controller `see`/`notSee`/`notKnow` packet side effects.

The next safe executable slice is a disabled region/player snapshot model, not live bind-point fanout dispatch.

## Update After UOW-1256

`PlayerKnownListRegionSnapshotService` now exists as a disabled, pure prerequisite model for owner-region plus neighbor-region player candidate selection. `PlayerKnownListMembershipRefreshService` is not yet wired to consume it, but the next safe slice is a non-live adapter from region snapshot candidate ids into membership metadata.

## Update After UOW-1257

`PlayerKnownListRegionMembershipAdapterService` now provides that non-live bridge from region snapshots into membership metadata. It is intentionally separate from `PlayerKnownListMembershipRefreshService` and remains disabled/unwired from registry, world movement, sockets, and live bind-point callbacks.

## Update After UOW-1258

`PlayerKnownListTwoWayOperationPlanService` now defines non-live add/remove/clear operation ordering that a future membership adapter can consume. Existing refresh services remain approximations and are not upgraded to Java region parity by this planner.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 membership refresh approximation service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow packet side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
