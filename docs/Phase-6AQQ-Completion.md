# Phase 6AQQ Completion - Nearby Quest Map-Region Adapter

Date: 2026-05-28
Unit of Work: UOW-1623
Status: Complete after focused validation

## Scope

This unit returned to nearby quest refresh prerequisites. Java `PlayerController.updateNearbyQuests` reads quest ids through `getOwner().getPosition().getMapRegion().getParent().getQuestIds()`, filters them through `QuestService.checkStartConditions`, then sends `SM_NEARBY_QUESTS`.

C# still keeps this non-live. This unit added an explicit map-region snapshot adapter so future delayed-refresh wiring can carry the Java controller-position/map-region-parent dependency without enabling production timers or packet sends.

## Completed Work

- Added `NearbyQuestMapRegionSnapshot`.
- Added `NearbyQuestRefreshInputAdapterService.CreatePlanFromMapRegion`.
- Extended `NearbyQuestRefreshInputAdapterResult` with player position, map-region position, and parent instance id metadata.
- Added `NearbyQuestRefreshInputAdapterStatus.MissingMapRegion`.
- Added tests for ready map-region parent quest ids and missing-map-region guard behavior.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused nearby quest tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestRefreshInputAdapterServiceTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestDelayedRefreshExecutionReportServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~GamePacketTests"
```

Result: 274 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Nearby controller-position/map-region metadata adapter | nearby adapter service/tests | Low | Yes | Smallest prerequisite for Java `player.position.mapRegion.parent.questIds` boundary. |
| Delayed refresh report map-region integration | delayed report service/tests | Medium | No | Builds on this unit's snapshot shape; best next nearby unit. |
| Charge-all multi-item packet/order audit | existing ItemCharge charge-all tests | Medium | No | Independent safe alternative, but latest handoff prioritized nearby. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; production timers and sends remain risky. |

No sub-agent was spawned because the work edits a shared adapter and paired tests; docs remain orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreatePlanFromMapRegion_UsesParentQuestIdsAndCapturesPositionMetadataWithoutLiveDispatch` | Added | Parent world instance quest ids flow through the adapter; player/map-region position and parent instance metadata are captured; packet intent is staged without live send. | Source-derived from Java `PlayerController.updateNearbyQuests`. |
| `CreatePlanFromMapRegion_GuardsMissingMapRegionBeforePlanning` | Added | Missing map-region snapshot returns explicit `MissingMapRegion`, preserves player position metadata, and produces no packet intent. | Documents the unported Java live map-region dependency conservatively. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshInputAdapterService.CreatePlanFromMapRegion` | Controller Adapter / Service | Partial | Unit Tested | Partial Parity | C# now models the Java `player.position.mapRegion.parent.questIds` boundary as explicit snapshot metadata. It still does not invoke a live controller, send packets, or resolve live `MapRegion` storage. |
| `com.aionemu.gameserver.world.MapRegion` | `Aion.GameServer.Services.NearbyQuestMapRegionSnapshot` | Region Boundary DTO | Partial | Unit Tested | Needs Verification | Snapshot carries position and parent world instance, but live Java region buckets, neighboring regions, zone/known-list behavior, threading, and object membership are not ported here. |
| `com.aionemu.gameserver.world.WorldMapInstance.getQuestIds` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.QuestIds` via `NearbyQuestMapRegionSnapshot.ParentWorldInstance` | World Runtime Dependency | Partial | Unit Tested | Partial Parity | Parent instance quest ids flow into the existing plan. Java concurrent set behavior and runtime ordering are not compared. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `NearbyQuestRefreshPlanService` / `NearbyQuestStartConditionService` | Service Dependency | Partial | Unit Tested through adapter | Needs Verification | Existing staged condition filters are reused. Unsupported XML/inventory/repeat timing dependencies remain explicit rejections. No Java runtime comparison. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `NearbyQuestRefreshPlan.WouldSendPacket`; `SmNearbyQuests` existing packet tests | Packet Dependency | Partial | Regression Tested + Unit Tested intent | Needs Verification | Map-region adapter produces packet intent only. No live send, encrypted frame comparison, or Java byte capture was run. |

## Remaining Risks

- This is metadata-only; no live `PlayerController.updateNearbyQuests`, `MapRegion` storage, timers, or `PacketSendUtility.sendPacket` is enabled.
- C# snapshot input approximates Java's live `player.position.mapRegion.parent` object graph.
- Java concurrent region/world collections, collection ordering, threading, zone/known-list side effects, reflection/dynamic handlers, date/time behavior, and encrypted packet bytes remain unverified.
- Existing nearby start-condition support is partial; unsupported XML inventory/repeat timing dependencies still reject candidates.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 map-region snapshot DTO plus 1 adapter overload and 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 3 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live MapRegion storage, live controller dispatch, production timer callback, packet send/encrypted frame comparison, full QuestService condition parity, Java runtime comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Delayed refresh map-region integration | `NearbyQuestDelayedRefreshExecutionReportService.cs`, report tests | Thread optional per-player `NearbyQuestMapRegionSnapshot` metadata into reports without sending packets. |
| ItemCharge charge-all multi-item packet/order audit | existing ItemCharge charge-all tests | Independent safe alternative. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Next Work Options

## Recommended Sequential Task

- Task: thread `NearbyQuestMapRegionSnapshot` into `NearbyQuestDelayedRefreshExecutionReportService` as optional per-player input metadata.
- Why: it is the next small step from explicit map-region metadata toward Java's delayed `WorldMapInstance.updateNearbyQuestsTask -> PlayerController.updateNearbyQuests` flow.
- Files: `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestDelayedRefreshExecutionReportService.cs`, `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestDelayedRefreshExecutionReportServiceTests.cs`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | ItemCharge charge-all multi-item packet/order audit | ItemCharge tests only if no nearby edits | Medium | Independent of nearby service files. |
| B | Read-only Java nearby region analysis | Java `MapRegion`, `WorldMapInstance`, `PlayerController` | Low | Analysis-only agent could report live region dependencies. |
| C | Nearby packet golden gap audit | `GamePacketTests` read-only unless assigned | Low | Avoid editing packet tests concurrently with report tests. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Delayed refresh map-region integration | delayed report service/tests, docs | live dispatch, packet sends, Java writes |
| Read-only Agent | Analyze Java `MapRegion` parent/quest-id lifecycle | read-only Java files | all writes |

## Do Not Parallelize

- `NearbyQuestRefreshInputAdapterService.cs`: shared adapter shape just changed; avoid concurrent edits with delayed report integration.
- `NearbyQuestDelayedRefreshExecutionReportService.cs`: next unit should have one owner.
- Live `GameServerConnection` / `PacketSendUtility` sends: still high risk and intentionally disabled.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1623] Add nearby map-region refresh adapter
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshInputAdapterService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRefreshInputAdapterServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQQ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
