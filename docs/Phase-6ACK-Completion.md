# Phase 6ACK Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1253
Status: Phase 6 continues; bind-point known-list fanout now has metadata for membership, send policy, disabled execution planning, opt-in socket boundary, and a test-first current-distance membership refresh approximation. Full Java-equivalent region known-list population and live bind-point dispatch remain disabled.

## Session Summary

UOW-1253 added `PlayerKnownListMembershipRefreshService`, an approximation seam that can seed `PlayerKnownListMembershipService` from supplied online `Player` candidates using `WorldVisibility`. It explicitly marks itself as not Java region known-list parity.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRefreshService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerKnownListMembershipRefreshServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipMetadata.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACK-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerKnownListMembershipRefreshServiceTests" --nologo` passed 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport|PlayerKnownListMembership" --nologo` passed 212 tests.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1253

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.knownlist.KnownList.update` | `Aion.GameServer.Services.PlayerKnownListMembershipRefreshService` | Known-List Refresh / Approximation | Partial | Unit Tested | Partial Parity | C# refreshes from supplied online players and current `WorldVisibility`, removes stale out-of-range entries, and excludes owner. It does not perform Java region-neighbor scans, synchronized update, two-way atomic-ish add/remove, or controller packet side effects. |
| `com.aionemu.gameserver.world.knownlist.KnownList.findVisibleObjects` | `PlayerKnownListMembershipRefreshService.RefreshOwnerFromOnlinePlayers` | Known-List Population | Partial | Unit Tested | Partial Parity | Uses supplied candidates plus 95m same-world visibility only. No Java `MapRegion` scan, object visible distance negotiation, or `canSee` state. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forgetObjectsOrUpdateVisibility` | `RefreshOwnerFromOnlinePlayers` stale removal | Known-List Cleanup | Partial | Unit Tested | Needs Verification | Removes entries absent from current distance-visible candidate set. Does not distinguish out-of-range removal from in-range invisible visibility changes; Java hidden known objects can remain known with `visible=false`. |
| `com.aionemu.gameserver.world.knownlist.KnownList.clear` | `ClearOwnerForLogout`; `RemoveDepartingPlayerFromKnownLists` | Known-List Cleanup / Logout Metadata | Partial | Unit Tested | Needs Verification | Clears owner metadata and removes departing player from supplied owners. Does not send Java `notSee`/`notKnow`, does not walk all world objects automatically, and remains unwired from logout. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | membership refresh plus known-list fanout metadata stack | Service / Fanout Prerequisite | Partial | Regression Tested | Needs Verification | Fanout can now be seeded from an approximate membership refresh in tests. Live bind-point dispatch still uses no Java-equivalent known-list population. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 membership refresh approximation service plus 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: 1 Java-equivalent region known-list population path, 1 live enter/move/logout wiring path, 1 controller see/notSee/notKnow packet side-effect path, 1 live scheduled callback dispatch path, 1 live movement adapter, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- This service is an approximation and is explicitly not Java region known-list parity.
- It is not registered or wired into enter/move/logout.
- Hidden/invisible-but-known Java behavior is not modeled; C# distance refresh can remove entries Java might retain as invisible.
- Java controller packet side effects, ordering, synchronization, and region lifecycle remain missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a disabled adapter that converts `IGameClientConnectionRegistry.ForEachOnlinePlayer` snapshots into `PlayerKnownListMembershipRefreshService` inputs.
- Scope:
  - Keep adapter disabled/unwired.
  - Do not modify `GameClientSocketServer` or `GameServerConnection`.
  - Test that supplied registry online-player snapshots can refresh owner/all-player membership metadata.
  - Preserve explicit approximation flags.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Registry snapshot refresh adapter | new service/test pair | Medium | Best next bridge; keep unwired. |
| B | Full Java known-list population design audit | docs only | Low/Medium | Separate from approximation code. |
| C | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Separate blocker after fanout. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Registry snapshot refresh adapter/tests | new adapter service/test files | `GameServerConnection`, `GameClientSocketServer`, DI/global config, shared docs until final pass |
| Explorer | Full Java known-list population design details | read-only Java/C# inspection | all writes |

### Do Not Parallelize

- Live connection dispatch with known-list refresh work.
- Region known-list population and socket fanout execution in one unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRefreshService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/WorldVisibility.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
- Latest completed commits:
  - `8c111cfe7 [Phase 6][UOW-1252] Add bind point teleport known-list socket executor boundary`
  - next commit should be `[Phase 6][UOW-1253] Add player known-list membership refresh approximation`

Keep live bind-point behavior disabled until known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.

