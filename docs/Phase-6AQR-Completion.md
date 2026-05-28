# Phase 6AQR Completion - Nearby Delayed Refresh Map-Region Reports

Date: 2026-05-28
Unit of Work: UOW-1624
Status: Complete after focused validation

## Scope

This unit threaded the map-region snapshot adapter from UOW-1623 into the non-live delayed nearby refresh report path. Java `WorldMapInstance.updateNearbyQuestsTask` clears its pending task and invokes each player's `PlayerController.updateNearbyQuests`; that controller resolves quest ids from the player's current `position.mapRegion.parent`.

C# now has a report overload that takes per-player map-region snapshot input while preserving the existing explicit-player/world-instance path. No live timer, controller dispatch, or packet send was enabled.

## Completed Work

- Added `NearbyQuestDelayedRefreshPlayerInput`.
- Added `NearbyQuestDelayedRefreshExecutionReportService.CreateReportFromMapRegions`.
- Added tests for per-player map-region report composition and missing-map-region no-send behavior.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused nearby quest tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestDelayedRefreshExecutionReportServiceTests|FullyQualifiedName~NearbyQuestRefreshInputAdapterServiceTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~GamePacketTests"
```

Result: 276 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Delayed refresh map-region report integration | delayed report service/tests | Low | Yes | Direct follow-up to UOW-1623 snapshot shape. |
| Java map-region lifecycle analysis | read-only Java world files | Low | No | Safe supporting work before live dispatch. |
| ItemCharge multi-item packet/order audit | existing ItemCharge tests | Medium | No | Independent safe alternative. |
| Live nearby refresh dispatch | world/connection services | High | No | Deferred; production timers and sends remain risky. |

No sub-agent was spawned because the implementation touches the shared delayed report service and its paired tests.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateReportFromMapRegions_ComposesPerPlayerMapRegionResultsWithoutSending` | Added | Delayed report clears pending refresh and composes per-player map-region adapter results with position and parent instance metadata. | Source-derived from Java `WorldMapInstance.updateNearbyQuestsTask` and `PlayerController.updateNearbyQuests`. |
| `CreateReportFromMapRegions_RecordsMissingMapRegionPerPlayerWithoutPacketIntent` | Added | Missing map-region for a player records `MissingMapRegion`, keeps no-send packet intent, and summary has zero packet intent. | Documents the unported Java live map-region dependency conservatively. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstance.updateNearbyQuestsTask` | `Aion.GameServer.Services.NearbyQuestDelayedRefreshExecutionReportService.CreateReportFromMapRegions` | Scheduler Callback / Report | Partial | Unit Tested | Partial Parity | C# clears pending refresh and records per-player map-region refresh results without live timers or packet sends. Java `Future` scheduling, concurrent player iteration, and runtime ordering remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `CreateReportFromMapRegions` via `NearbyQuestRefreshInputAdapterService.CreatePlanFromMapRegion` | Controller Callback Dependency | Partial | Unit Tested | Partial Parity | Report path now uses per-player map-region snapshots instead of only the scheduled instance's quest ids. It still does not invoke live controllers or `PacketSendUtility`. |
| `com.aionemu.gameserver.world.MapRegion` | `NearbyQuestDelayedRefreshPlayerInput.MapRegion` / `NearbyQuestMapRegionSnapshot` | Region Boundary DTO | Partial | Unit Tested | Needs Verification | Missing per-player map-region is surfaced as `MissingMapRegion` with no packet intent. Live region storage, neighbors, object membership, zone side effects, and threading remain unported. |
| `com.aionemu.gameserver.world.WorldMapInstance.getQuestIds` | `WorldMapInstanceRuntimeState.QuestIds` through snapshot parent | World Runtime Dependency | Partial | Unit Tested | Partial Parity | Parent instance quest ids feed marker projection per player. Java concurrent set behavior and collection ordering are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `NearbyQuestDelayedRefreshPacketIntentSummary` / `NearbyQuestRefreshPlan.WouldSendPacket` | Packet Dependency | Partial | Existing Regression Tested + Unit Tested intent | Needs Verification | The new report path still records packet intent only. No live send, encrypted frame, socket ordering, or Java byte comparison occurred. |

## Remaining Risks

- This remains non-live report metadata; no production scheduler, live player iteration, controller invocation, or packet send is enabled.
- C# map-region snapshots approximate Java's live `WorldPosition`/`MapRegion` object graph.
- Java concurrent player collection iteration, map-region storage, zone/known-list side effects, collection ordering, threading, serialization/encrypted packet bytes, and dynamic handler behavior remain unverified.
- Full `QuestService.checkStartConditions` parity remains partial; unsupported XML/inventory/repeat timing dependencies still reject candidates.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 delayed-refresh player input DTO plus 1 report overload and 2 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live timer scheduling, live player iteration, controller dispatch, live MapRegion storage, packet send/encrypted frame comparison, Java runtime comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Map-region report packet-intent summary regression | delayed report tests | Cover mixed ready/missing-map-region/empty-map-region players through summary aggregation. |
| Java MapRegion lifecycle analysis | Java `MapRegion`, `WorldMapInstance`, `WorldPosition` | Read-only dependency map before live dispatcher planning. |
| ItemCharge charge-all multi-item packet/order audit | existing ItemCharge charge-all tests | Independent safe alternative. |

## Next Work Options

## Recommended Sequential Task

- Task: add a focused map-region report packet-intent summary regression for mixed ready, missing-map-region, and empty-map-region players.
- Why: it validates the report aggregation surface before considering live dispatcher boundaries.
- Files: `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestDelayedRefreshExecutionReportServiceTests.cs`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Java MapRegion lifecycle analysis | read-only Java world files | Low | Safe as a read-only agent task. |
| B | ItemCharge multi-item packet/order audit | ItemCharge tests | Medium | Independent of nearby report files. |
| C | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Do not edit packet tests concurrently with report tests. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Add mixed map-region packet-intent summary regression | delayed report tests, docs | live dispatch, packet sends, Java writes |
| Read-only Agent | Analyze Java `MapRegion` parent/quest-id lifecycle | read-only Java files | all writes |

## Do Not Parallelize

- `NearbyQuestDelayedRefreshExecutionReportService.cs`: just changed; keep one owner for any follow-up edits.
- `NearbyQuestRefreshInputAdapterService.cs`: shared adapter boundary for map-region snapshots.
- Live `GameServerConnection` / `PacketSendUtility` sends: still high risk and intentionally disabled.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1624] Thread nearby map-region reports
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestDelayedRefreshExecutionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestDelayedRefreshExecutionReportServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQR-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
