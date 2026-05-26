# Phase 6ACL Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1254
Status: Phase 6 continues; bind-point known-list fanout now has metadata for membership, refresh approximation, registry snapshot refresh adapter, send policy, disabled execution planning, and an opt-in socket boundary. Full Java-equivalent region known-list population and live bind-point dispatch remain disabled.

## Session Summary

UOW-1254 added `PlayerKnownListMembershipRegistryRefreshAdapterService`, a disabled adapter that reads online player snapshots from `IGameClientConnectionRegistry.ForEachOnlinePlayer` only when explicitly enabled, then feeds those players into `PlayerKnownListMembershipRefreshService`.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRegistryRefreshAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListMembershipRegistryRefreshAdapterServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-KnownListRegistryRefreshAdapter.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACL-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipRegistryRefreshAdapterServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownListMembership" --nologo` passed 216 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1254

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.World` / `WorldMapInstance` player storage | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.ForEachOnlinePlayer`; `PlayerKnownListMembershipRegistryRefreshAdapterService` | Registry / Player Snapshot Adapter | Partial | Unit Tested | Intentional Difference | C# adapter reads online connection registry snapshots, not Java world/map-region object storage. This is a disabled approximation seam, not live parity. |
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `PlayerKnownListMembershipRegistryRefreshAdapterService` -> `PlayerKnownListMembershipRefreshService` | Known-List Refresh Adapter | Partial | Unit Tested | Partial Parity | Adapter can refresh one owner or all supplied online players through current-distance approximation. It does not perform region-neighbor scan or controller side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | registry snapshot plus `WorldVisibility` refresh | Known-List Population | Partial | Unit Tested | Needs Verification | Uses online registry snapshot and 95m same-world visibility. No Java `MapRegion` lifecycle, `canSee`, or object visible-distance negotiation. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | registry refresh adapter plus known-list fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Registry snapshots can now seed approximate membership metadata when explicitly enabled. Live scheduled callback dispatch and Java-equivalent known-list population remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled registry refresh adapter service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Adapter remains disabled/unwired.
- Registry snapshots are not Java region known-list storage.
- Hidden/invisible-but-known state, controller packets, region lifecycle, and concurrency remain missing.
- Live scheduled callback dispatch, movement, `GameServerConnection`, and Java runtime capture remain disabled.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a documentation/design audit for full Java-equivalent player known-list population requirements.
- Scope:
  - Document Java `World.spawn`, `World.updatePosition`, `MapRegion`, `KnownList.update`, `findVisibleObjects`, `forgetObjectsOrUpdateVisibility`, `clear`.
  - Map required C# surfaces and missing hooks.
  - Do not wire registry refresh, socket executor, or `GameServerConnection`.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Full Java known-list population design audit | docs only | Low/Medium | Best next blocker before live wiring. |
| B | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| C | Movement side-effect readiness audit | docs only | Low/Medium | Separate blocker after fanout. |
| D | Registry refresh adapter hardening | tests only | Low/Medium | Add edge cases if design audit does not start. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Full Java known-list population design details | read-only Java/C# inspection | all writes |
| Orchestrator | Population design doc and parity/handoff updates | new doc plus shared docs | production code, tests |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRefreshService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRegistryRefreshAdapterService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - `dotnetConversion/src/Aion.GameServer/World/World.cs`
- Latest completed commits:
  - `8c111cfe7 [Phase 6][UOW-1252] Add bind point teleport known-list socket executor boundary`
  - `67a912003 [Phase 6][UOW-1253] Add player known-list membership refresh approximation`
  - next commit should be `[Phase 6][UOW-1254] Add player known-list registry refresh adapter`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.

