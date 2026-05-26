# Phase 6ACM Completion Handoff

Date: May 26, 2026
Latest Unit of Work: UOW-1255
Status: Phase 6 continues; bind-point known-list fanout still has only non-live metadata and approximation seams. Full Java-equivalent region/player known-list population remains the next major blocker before live bind-point dispatch.

## Session Summary

UOW-1255 added a documentation/design audit for Java-equivalent player known-list population. It confirms that current C# player known-list metadata, registry snapshot refresh, and bind-point fanout executor surfaces are not sufficient for live Java parity because Java population is driven by world lifecycle methods, map regions, bidirectional known-list mutation, cached visibility state, and controller packet side effects.

Files changed:

- `docs/Phase-6-BindPointTeleport-PlayerKnownListPopulation-Design.md`
- `docs/Phase-6-BindPointTeleport-KnownListMembershipRefresh.md`
- `docs/Phase-6-BindPointTeleport-KnownListFanoutParity.md`
- `docs/Phase-6-BindPointTeleport-LiveAdapter-Readiness.md`
- `docs/Phase-6-BindPointTeleport-ScheduledKinah-LiveAdapter-FinalReadiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ACM-Completion.md`

## Validation

- No production code changed in this unit.
- No tests were added or run for this documentation-only audit.
- Java and C# source inspection was performed.
- No Java runtime known-list comparison was run.
- No live `GameServerConnection` dispatch was enabled.

## Migration Parity Table - UOW-1255

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.World.spawn` / `World.updatePosition` / `World.despawn` | `Aion.GameServer.World.World`; future central lifecycle adapter | World Lifecycle | Not Started | Manual Only | Needs Verification | Current C# object container does not mirror Java spawned state, map-instance add/remove, region add/remove, update-known-list order, despawn clear order, or zone revalidation coupling. |
| `com.aionemu.gameserver.world.WorldMapInstance` / `com.aionemu.gameserver.world.MapRegion` | `Aion.GameServer.World.WorldMapRuntimeState`; future region index | World Region Storage | Not Started | Manual Only | Needs Verification | Current runtime state tracks instance/player ids but not visible-object region buckets, neighbor scans, or instance-aware known-list discovery. |
| `com.aionemu.gameserver.world.knownlist.KnownList.update` / `findVisibleObjects` / `forgetObjectsOrUpdateVisibility` / `clear` | `PlayerKnownListMembershipService`; `PlayerKnownListMembershipRefreshService`; `PlayerKnownListMembershipRegistryRefreshAdapterService` | Known-List Lifecycle | Partial | Unit Tested | Partial Parity | C# has non-live player-player metadata and registry/distance approximation. Missing Java region scans, bidirectional world-object mutation, synchronized update ordering, clear packet side effects, hidden visibility retention, and live wiring. |
| `com.aionemu.gameserver.world.knownlist.KnownObject` | `Aion.GameServer.Services.PlayerKnownListMembershipEntry` | Known-Object / Visibility Cache | Partial | Unit Tested | Needs Verification | C# entry has `IsVisibleToOwner`, but no Java `owner.canSee` recomputation, pet visibility cascade, or all-object membership. |
| `com.aionemu.gameserver.controllers.PlayerController.see` / `notSee` / `VisibleObjectController.notKnow` | future known-list packet side-effect dispatcher | Controller / Packet Side Effects | Not Started | Manual Only | Needs Verification | Missing player spawn/delete side-effect dispatcher, ride/stance motion ordering, NPC/drop/quest side effects, and aggro/not-know cleanup behavior. |
| `com.aionemu.gameserver.utils.PacketSendUtility.broadcastPacket(Player,AionServerPacket,boolean)` / `BindPointTeleportService.teleport` action `3` | bind-point known-list fanout metadata stack plus disabled socket executor | Service / Fanout | Partial | Regression Tested | Needs Verification | Fanout execution can preserve source-first and precomputed membership order in tests, but live population and scheduled callback dispatch remain disabled. |

## Summary Metrics

- Total Java artifacts discovered: 6 grouped artifact rows in this unit
- Total artifacts ported: 0 code artifacts; 1 design audit document
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 6 grouped rows
- Total blocked artifacts: 1 region-backed world storage path, 1 central lifecycle path, 1 full bidirectional known-list path, 1 controller packet side-effect path, 1 live bind-point scheduled callback path, and 1 Java runtime capture path
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Remaining Risks

- Region-backed world object storage is not ported.
- Full bidirectional Java known-list mutation is not ported.
- Hidden/invisible-but-known retention is not ported.
- Controller packet side effects are not ported as known-list deltas.
- Current registry/distance fanout is not equivalent to Java known-list fanout.
- Java threading differs from current C# metadata locks; two-way mutation atomicity still needs design and tests.
- Reflection behavior did not change. Date/time behavior did not change. Serialization, threading, packet-order, dirty-state persistence, live known-list membership, and movement parity remain `Needs Verification`.

## Next Work Options

### Recommended Sequential Task

- Task: Add a small, test-first region/player snapshot model for Java known-list population prerequisites.
- Scope:
  - Represent world id, instance id, region id, owner player object id, and candidate player object ids.
  - Preserve owner exclusion and neighbor-candidate ordering metadata.
  - Keep the model disabled/unwired from live `GameServerConnection`.
  - Do not replace current distance broadcasts or execute socket fanout.
  - Add focused tests proving the model can describe same-region and neighbor-region candidates while excluding self.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Region/player snapshot model | new service/tests/docs | Medium | Best next executable prerequisite. Keep disabled/unwired. |
| B | Known-list packet side-effect design | docs only | Low/Medium | Useful before dispatching `see`/`notSee` packets. |
| C | Movement side-effect readiness audit | docs only | Low/Medium | Separate blocker after known-list population. |
| D | Registry refresh adapter edge tests | tests only | Low | Useful if avoiding new model work. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Inspect current C# world/map runtime state and Java region coordinates for a minimal player-region snapshot shape | read-only Java/C# inspection | all writes |
| Worker | Implement disabled region/player snapshot model and focused tests | new service/test/doc files only | `GameServerConnection`, live network dispatch, existing world mutation paths unless explicitly needed |
| Orchestrator | Integrate docs, parity tables, progress, and handoff | docs/progress/handoff | production behavior changes |

### Do Not Parallelize

- Live known-list population with socket execution.
- `GameServerConnection` dispatch with approximation-only membership.
- Region lifecycle replacement and packet side-effect dispatch in the same unit.
- Shared progress/handoff/parity docs; keep them orchestrator-owned.

## Context Needed By Next Session

- Java source of truth:
  - `game-server/src/com/aionemu/gameserver/world/World.java`
  - `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
  - `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownList.java`
  - `game-server/src/com/aionemu/gameserver/world/knownlist/KnownObject.java`
  - `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
  - `game-server/src/com/aionemu/gameserver/controllers/VisibleObjectController.java`
- C# surfaces:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRefreshService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerKnownListMembershipRegistryRefreshAdapterService.cs`
  - `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
  - `dotnetConversion/src/Aion.GameServer/World/World.cs`
- Latest completed commits:
  - `8c111cfe7 [Phase 6][UOW-1252] Add bind point teleport known-list socket executor boundary`
  - `67a912003 [Phase 6][UOW-1253] Add player known-list membership refresh approximation`
  - `b2292fc68 [Phase 6][UOW-1254] Add player known-list registry refresh adapter`
  - next commit should be `[Phase 6][UOW-1255] Audit player known-list population requirements`

Keep live bind-point behavior disabled until Java-equivalent known-list membership population, source-first fanout execution, live scheduled callback dispatch, final movement packet ordering, live scheduled task ownership, and Java packet/runtime validation have focused parity slices.
