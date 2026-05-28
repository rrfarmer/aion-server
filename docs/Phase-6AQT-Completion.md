# Phase 6AQT Completion - Java MapRegion Lifecycle Analysis

Date: 2026-05-28
Unit of Work: UOW-1626
Status: Complete after read-only source review

## Scope

This unit performed the read-only Java `MapRegion` lifecycle analysis requested by AQS before any live nearby refresh dispatcher work. The goal was to document how Java positions obtain regions, how `getParent().getQuestIds()` is maintained, and which C# live-storage prerequisites are still missing.

No production C# or Java source changed.

## Completed Work

- Reviewed Java `MapRegion`.
- Reviewed Java `WorldPosition`.
- Reviewed Java `WorldMapInstance`.
- Reviewed Java `WorldMap`, `WorldMap2DInstance`, and `WorldMap3DInstance`.
- Reviewed Java `World`.
- Reviewed Java `VisibleObject.setPosition`, `Player.setPosition`, and `PlayerController.updateNearbyQuests`.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, lifecycle findings, Migration Parity Table, risks, metrics, and next-unit guidance.

## Java Lifecycle Findings

- `World.createPosition` resolves `WorldMap -> WorldMapInstance -> getRegion(x,y,z)` and stores the resulting `MapRegion` in `WorldPosition`.
- `World.spawn` marks the position spawned, adds the object to the parent `WorldMapInstance`, adds it to the current `MapRegion`, then updates known-list state.
- `World.updatePosition` derives the new region from `oldRegion.getParent().getRegion(newX,newY,newZ)`, revalidates zones when the region changes, moves the object between region maps, then updates known-list state.
- `World.despawn` marks the position unspawned, removes the object from both parent instance and map region, revalidates zones for creatures, then clears known-list state.
- `MapRegion` stores visible objects in a `ConcurrentHashMap`, tracks player count with synchronized counters, and asynchronously activates/deactivates itself and neighbor regions.
- `WorldMapInstance.addObject(Npc)` maintains a concurrent `questIds` set from `QuestNpc.getOnQuestStart`; newly added quest ids schedule one delayed nearby refresh task if no task is already pending.
- The delayed task clears `updateNearbyQuestsTask` before iterating players and invoking `player.getController().updateNearbyQuests()`.
- `PlayerController.updateNearbyQuests` reads `player.position.mapRegion.parent.questIds`, filters through `QuestService.checkStartConditions`, then unconditionally sends `SM_NEARBY_QUESTS`.

## C# Prerequisite Findings

- `WorldMapInstanceRuntimeState` models player ids, quest ids, registration, and pending nearby refresh metadata.
- It does not model live region ids, live `MapRegion` object buckets, neighbor arrays, region active/deactivation state, or per-position map-region references.
- `NearbyQuestMapRegionSnapshot` and `CreateReportFromMapRegions` model Java's controller dependency as explicit metadata only.
- `PlayerKnownListRegionSnapshotService` has a separate region snapshot model for known-list planning, but it is not live-backed and is not connected to nearby quest refresh.
- Live nearby dispatch should remain disabled until C# has one clear source of truth for region storage and position-region membership.

## Validation

No tests were run because this unit is read-only analysis and documentation only.

`git diff --check` should be run before commit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Java MapRegion lifecycle analysis | Java world/controller files, docs | Low | Yes | Required before live nearby dispatch planning. |
| ItemCharge multi-item packet/order audit | existing ItemCharge tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs if assigned | Low | No | Useful later; live region prerequisites are current blocker. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; region storage and live iteration prerequisites are missing. |

No sub-agent was spawned because the selected work is docs-only and the Java review scope was small enough for the orchestrator.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| None | N/A | Documentation-only dependency analysis. | Static source review of Java world/map-region lifecycle. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion` | `Aion.GameServer.Services.NearbyQuestMapRegionSnapshot`; `PlayerKnownListRegionSnapshotService` prerequisites | Region Storage / Boundary | Partial | Manual Only | Needs Verification | Java has live object maps, neighbor arrays, player-count activation/deactivation, zone revalidation, and parent instance references. C# has explicit snapshots only. |
| `com.aionemu.gameserver.world.WorldPosition` | `Aion.GameServer.World.WorldPosition` plus nearby snapshot metadata | Position Model | Partial | Manual Only | Needs Verification | Java carries mutable `MapRegion` reference and spawned flag; C# record stores world id/coords/instance id only. Null region, spawned state, equality, threading, and mutation semantics differ. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | World Instance Runtime | Partial | Existing Unit Tested + Manual Analysis | Partial Parity | C# models quest ids, player ids, registration, and pending nearby refresh metadata. Java also stores live objects/NPCs/players, regions, zones, `Future` tasks, concurrent maps/sets, and delayed task execution. |
| `com.aionemu.gameserver.world.World` | `WorldMapRuntimeStateTable` plus scattered service-level world models | World Registry / Spawn Boundary | Partial | Existing Unit Tested + Manual Analysis | Needs Verification | Java owns store/spawn/despawn/updatePosition and mutates map-region membership. C# lacks a unified live world/region mutation path for nearby refresh. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `NearbyQuestRefreshInputAdapterService.CreatePlanFromMapRegion`; delayed report metadata | Controller Callback | Partial | Unit Tested + Manual Analysis | Partial Parity | C# models per-player map-region parent quest-id lookup as metadata. Live controller invocation, `PacketSendUtility`, exact Java null behavior, and socket ordering remain disabled/unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `NearbyQuestRefreshPlan.WouldSendPacket`; `SmNearbyQuests` existing packet tests | Packet Dependency | Partial | Existing Regression Tested | Needs Verification | Packet intent and local serialization tests exist. No live send, encrypted frame comparison, Java runtime packet capture, or dispatcher ordering validation. |

## Remaining Risks

- No production code changed; this unit only documents the dependency map.
- Live C# `MapRegion` storage, region id calculation, neighbor arrays, region activation/deactivation, player-count synchronization, object membership, zone revalidation, and known-list integration remain unported or snapshot-only approximations.
- Java `WorldPosition` mutable region pointer and spawned-state semantics differ from C# `WorldPosition` record semantics.
- Java delayed refresh uses a real `Future` and `ConcurrentHashMap` player iteration; C# only has report metadata.
- Live `PacketSendUtility.sendPacket`, encrypted packet bytes, socket ordering, threading, dynamic handlers, and Java runtime behavior remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 6 grouped rows.
- Total artifacts ported: 0 code artifacts; 1 completed read-only lifecycle analysis with docs.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 4 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live MapRegion storage, region id/neighbor model, world spawn/despawn/updatePosition membership, live timer scheduling, live player iteration, live controller dispatch, packet send/encrypted frame comparison, Java runtime comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live nearby region-key/region-membership planning model | new nearby service/test files or reuse audit of known-list region snapshot service | Model Java-like region identity for nearby snapshots before live dispatch. |
| ItemCharge charge-all multi-item packet/order audit | existing ItemCharge charge-all tests | Independent safe implementation alternative. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: add a non-live nearby region-key/region-membership planning model for Java-like map-region identity and per-player snapshot assembly, or first audit whether `PlayerKnownListRegionSnapshotService` can be reused safely.
- Why: live nearby dispatch depends on consistent region identity and player-to-region membership.
- Files: likely new nearby service/test files; avoid editing live dispatch.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Known-list region snapshot reuse audit | read-only `PlayerKnownListRegionSnapshotService` and tests | Low | Helps avoid duplicate region-key concepts. |
| B | ItemCharge multi-item packet/order audit | ItemCharge tests/read-only Java | Medium | Independent from nearby files. |
| C | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Nearby region-key planning model or reuse audit | new nearby service/tests, docs | live dispatch, Java writes |
| Read-only Agent | ItemCharge multi-item ordering audit | read-only Java/C# ItemCharge files | all writes |

## Do Not Parallelize

- Live `GameServerConnection` / `PacketSendUtility` sends: still high risk and intentionally disabled.
- Java source files: read-only only.
- Shared region abstraction files if a new one is introduced: one owner only.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1626] Document Java MapRegion lifecycle
```

Files changed in this unit:

- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQT-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
