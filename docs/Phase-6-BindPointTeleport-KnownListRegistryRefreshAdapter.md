# Phase 6 Bind-Point Teleport Known-List Registry Refresh Adapter

Date: May 26, 2026
Unit of Work: UOW-1254
Scope: Add a disabled adapter from C# online-player registry snapshots to player known-list refresh metadata.
Source of truth: Java project.

## Summary

UOW-1254 adds `PlayerKnownListMembershipRegistryRefreshAdapterService`. It reads online players through `IGameClientConnectionRegistry.ForEachOnlinePlayer` only when explicitly enabled and passes that snapshot into `PlayerKnownListMembershipRefreshService`.

This remains an approximation. Java known-list population uses world/map-region object storage and region-neighbor scans, not a flat online connection registry.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipRegistryRefreshAdapterServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownListMembership" --nologo` passed 216 tests.
- No Java runtime comparison was executed.

## Migration Parity Table - UOW-1254

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.World` / `WorldMapInstance` player storage | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.ForEachOnlinePlayer`; `PlayerKnownListMembershipRegistryRefreshAdapterService` | Registry / Player Snapshot Adapter | Partial | Unit Tested | Intentional Difference | C# adapter reads online connection registry snapshots, not Java world/map-region object storage. Disabled approximation seam only. |
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | registry refresh adapter -> membership refresh service | Known-List Refresh Adapter | Partial | Unit Tested | Partial Parity | Refreshes one owner or all online snapshot players through current-distance approximation. No region scan or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | registry snapshot plus `WorldVisibility` refresh | Known-List Population | Partial | Unit Tested | Needs Verification | Online registry snapshot plus 95m same-world visibility. No Java `MapRegion` lifecycle or `canSee`. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | registry refresh adapter plus fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Can seed approximate membership metadata when enabled. Live fanout remains disabled. |

## Remaining Risks

- Adapter is disabled/unwired.
- Registry snapshots are not Java region known-list storage.
- Full Java known-list population design is still needed before live wiring.
- Movement, dispatch, and Java runtime capture remain missing.

## Next Recommended Unit of Work

Add a documentation/design audit for full Java-equivalent player known-list population requirements before wiring registry snapshot refresh or socket executor paths into live server flows.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled registry refresh adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete
