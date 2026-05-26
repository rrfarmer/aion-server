# Phase 6ACJ Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1252
Status: Phase 6 continues; bind-point scheduled Kinah now has SQL/send adapter seams, visible-distance fanout characterization, source-first known-list trace metadata, player known-list membership metadata, non-live send-policy metadata, a disabled source-first execution-plan composition, and a disabled-by-default opt-in socket executor boundary. Live known-list population, scheduled callback dispatch, movement, and `GameServerConnection` dispatch remain disabled.

## Session Summary

UOW-1252 added `BindPointTeleportKnownListFanoutSocketExecutorService`. It consumes `BindPointTeleportKnownListFanoutExecutionPlan`, records disabled/no-send behavior by default, and can call `IGameClientConnectionRegistry.SendPacketToPlayerAsync` only when explicitly enabled by a caller.

Java/C# read-only discovery also confirmed that true live known-list population is not available yet. C# can currently seed only an approximation from online player candidates plus `WorldVisibility`; Java parity requires region-scanned two-way known-list membership with cached visibility and `see/notSee/notKnow` side effects.

Files changed:

- `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutSocketExecutorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/BindPointTeleportKnownListFanoutSocketExecutorServiceTests.cs`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutExecutionPlan.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutSocketExecutor.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACJ-Completion.md`

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "BindPointTeleportKnownListFanoutSocketExecutorServiceTests" --nologo` passed 5 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "CharacterSelectionServerPackets_WriteJavaShapedPayloads|BindPointTeleport|CmBindPointTeleport|SmBindPointTeleport" --nologo` passed 204 tests.
- No Java runtime fanout capture was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1252

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` | `Aion.GameServer.Services.BindPointTeleportKnownListFanoutSocketExecutorService` | Network Utility / Disabled Socket Boundary | Partial | Unit Tested | Needs Verification | Opt-in executor preserves source-first ordering and known-list traversal order from the execution plan. It is disabled by default and not wired to dispatch. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket(Player,AionServerPacket)` | `BindPointTeleportKnownListFanoutSocketExecutorService.ExecuteAsync` | Packet Utility / Socket Send Boundary | Partial | Unit Tested | Needs Verification | Enabled path can call `SendPacketToPlayerAsync` per recipient. Missing connection, exception, and cancellation behavior are C# boundary metadata, not Java runtime comparison. |
| `com.aionemu.gameserver.utils.collections.CollectionUtil.forEach` | `BindPointTeleportKnownListFanoutSocketExecutorService` known-list recipient failure handling | Utility / Exception Policy | Partial | Unit Tested | Needs Verification | Known-list recipient exceptions continue traversal. Java logging text and real server exception path are not executed. |
| `com.aionemu.gameserver.world.knownlist.KnownList.forEachPlayer` | execution plan plus socket executor recipient loop | Known-List Traversal / Socket Boundary | Partial | Regression Tested | Needs Verification | Uses precomputed membership snapshot order. Live known-list population, `ConcurrentHashMap` runtime ordering, and two-way membership mutation remain missing. |
| `com.aionemu.gameserver.services.teleport.BindPointTeleportService.teleport` scheduled action `3` fanout | known-list fanout execution plan plus socket executor boundary | Service / Callback Fanout Boundary | Partial | Regression Tested | Needs Verification | Fanout socket boundary exists but is not connected to scheduled callback execution, cooldown storage, movement, or `GameServerConnection`. |

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: 1 disabled/opt-in socket executor service plus 5 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: 1 live world known-list population path, 1 live scheduled callback dispatch path, 1 live movement adapter, 1 `GameServerConnection` dispatch path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- The executor is opt-in and not used by live bind-point flows.
- Live known-list population remains missing; current C# registry/distance visibility is not Java known-list parity.
- Online state still comes from supplied send policy rather than direct Java-equivalent player connection state.
- Java logging, `ConcurrentHashMap` ordering, two-way known-list mutation, scheduled callback dispatch, movement, and Java runtime capture remain missing.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add `PlayerKnownListMembershipRefreshService` as a test-first approximation seam.
- Scope:
  - Accept an owner `Player`, supplied online player candidates, and `WorldVisibility`.
  - Exclude owner/source.
  - Upsert current-distance visible candidates into `PlayerKnownListMembershipService`.
  - Provide clear/remove helpers for logout.
  - Document explicitly that this is not Java region/known-list parity.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Player membership refresh approximation | new service/test pair | Medium | Best next fanout blocker; keep unwired. |
| B | Live Java known-list population design doc | docs only | Low/Medium | Can run in parallel if files are isolated. |
| C | Java packet/fanout observer design | docs only | Low/Medium | Useful before runtime capture tooling exists. |
| D | Movement side-effect readiness audit | docs only | Low/Medium | Separate blocker after fanout. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement refresh approximation service/tests | new `PlayerKnownListMembershipRefreshService.cs`; new matching test file | `GameServerConnection`, DI/global config, shared docs until final pass |
| Explorer | Live Java known-list population design details | read-only Java/C# inspection | all writes |

### Do Not Parallelize

- `GameServerConnection` dispatch with any known-list metadata work.
- Live known-list population and socket execution wiring in one unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
  - `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java`
  - `game-server/src/com/aionemu/gameserver/services/teleport/BindPointTeleportService.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/WorldVisibility.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/BindPointTeleportKnownListFanoutSocketExecutorService.cs`
- Latest completed commits:
  - `e7946efdd [Phase 6][UOW-1250] Add bind point teleport known-list send policy metadata`
  - `e2fe21701 [Phase 6][UOW-1251] Add bind point teleport known-list execution plan metadata`
  - next commit should be `[Phase 6][UOW-1252] Add bind point teleport known-list socket executor boundary`

Keep live bind-point behavior disabled until known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.

