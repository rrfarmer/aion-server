# Phase 6ARG Completion - MapRegion Lifecycle Intent

Date: 2026-05-28
Unit of Work: UOW-1639
Status: Complete after focused unit tests

## Scope

This unit modeled Java `MapRegion` player-count lifecycle decisions without live object storage. It captures activation, delayed deactivation scheduling, deactivation blocking, and active-state transition intent as metadata.

This remains a DTO/model layer. It does not mutate `MapRegion.objects`, run a scheduler, notify AI, revalidate zones, or attach live `MapRegion` objects to positions.

## Completed Work

- Added `WorldMapRegionLifecyclePlanService`.
- Added `WorldMapRegionLifecycleContext`.
- Added `WorldMapRegionLifecycleRegionState`.
- Added `WorldMapRegionLifecyclePlan`.
- Added lifecycle action and blocked-reason enums.
- Modeled Java first-player activation of self plus neighbours.
- Modeled Java last-player removal scheduling with 60-second delay.
- Modeled Java duplicate deactivation suppression through `deactivationPending`.
- Modeled scheduled deactivation guards for self players, neighbour players, instance maps, and Transidium Annex.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, parity table, tests, risks, metrics, and next-unit guidance.

## Java Parity Notes

- Java `MapRegion.add` activates only when a newly added object is a `Player` and incremented `playerCount` becomes `1`.
- Java `MapRegion.remove` schedules deactivation only when a removed object was a `Player` and decremented `playerCount` becomes `0`.
- Java `scheduleDeactivation` suppresses duplicate pending schedules and uses a 60-second delay.
- Java scheduled deactivation clears `deactivationPending` before checking whether self still has zero players.
- Java `tryDeactivate` skips instance maps and `WorldMapType.TRANSIDIUM_ANNEX`.
- Java deactivation also skips inactive regions and regions whose neighbours-including-self have players.
- C# models these as plans only; Java scheduler and AI notification side effects remain disabled.

## Validation

Ran:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRegionLifecyclePlanServiceTests|FullyQualifiedName~WorldMapRegionCreationSnapshotServiceTests|FullyQualifiedName~WorldMapRegionZoneFilterServiceTests|FullyQualifiedName~WorldMapRegionLayoutServiceTests|FullyQualifiedName~WorldRegionKeyProjectionServiceTests|FullyQualifiedName~WorldRegionIdServiceTests"
```

Result: passed 47 tests.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| `MapRegion` lifecycle intent model | new lifecycle service/tests | Medium | Yes | Models threshold and delayed-state decisions in one isolated helper. |
| Charge-all DB rollback integration planning | repository integration tests | Medium | No | Independent safe alternative. |
| Nearby packet golden gap audit | packet tests/docs | Low | No | Useful later. |
| Zone-handler source audit | Java zone handler files | Low | No | Useful before live callbacks. |

No sub-agent was spawned for UOW-1639 because the selected implementation and tests touched one small helper surface and docs remained orchestrator-owned.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `CreateAddPlan_FirstPlayerActivatesInactiveSelfAndNeighbours` | Added | First new player activates inactive self/neighbours and skips already active neighbours. | Static source review of Java `MapRegion.add` and `activate`; no Java runtime comparison. |
| `CreateAddPlan_DuplicateOrNonPlayerDoesNotChangeLifecycle` | Added | Duplicate adds and non-player adds do not affect lifecycle state. | Static source review of Java `objects.put == null` and `instanceof Player` guard. |
| `CreateRemovePlan_LastPlayerSchedulesOneJavaDelayedDeactivation` | Added | Last player removal schedules one 60-second deactivation. | Static source review of Java `MapRegion.remove` and `scheduleDeactivation`. |
| `CreateRemovePlan_PendingDeactivationSuppressesDuplicateSchedule` | Added | Pending deactivation suppresses duplicate schedule. | Static source review of Java `deactivationPending` guard. |
| `CreateRemovePlan_NonLastPlayerOnlyDecrementsCount` | Added | Removing one of multiple players decrements count without scheduling. | Static source review of Java `decrementPlayerCount` threshold. |
| `CreateScheduledDeactivationPlan_DeactivatesActiveSelfAndNeighboursWhenNoPlayersRemain` | Added | Active self/neighbours deactivate when no players remain. | Static source review of Java scheduled task and `tryDeactivate`. |
| `CreateScheduledDeactivationPlan_BlocksInstanceAndTransidiumAnnexMaps` | Added | Instance maps and Transidium Annex block deactivation. | Static source review of Java `tryDeactivate` guards and `WorldMapType.TRANSIDIUM_ANNEX`. |
| `CreateScheduledDeactivationPlan_BlocksWhenAnyNeighbourStillHasPlayers` | Added | Any neighbour with players blocks deactivation. | Static source review of Java `anyNeighbourHasPlayers`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.MapRegion.add` | `WorldMapRegionLifecyclePlanService.CreateAddPlan` | Region Lifecycle / Utility | Partial | Unit Tested | Partial Parity | C# models duplicate-object no-op, non-player add, player-count increment, and first-player activation of inactive self/neighbours. It does not mutate a live `ConcurrentHashMap`, store objects, or notify AI. |
| `com.aionemu.gameserver.world.MapRegion.remove` | `WorldMapRegionLifecyclePlanService.CreateRemovePlan` | Region Lifecycle / Utility | Partial | Unit Tested | Partial Parity | C# models missing-object no-op, non-player removal, decrement without crossing zero, last-player schedule, and duplicate pending suppression. It does not mutate live objects or schedule a real task. |
| `com.aionemu.gameserver.world.MapRegion.activate` | `WorldMapRegionLifecyclePlan.ActivatedRegionIds` | Activation Intent | Partial | Unit Tested | Partial Parity | C# reports which self/neighbour region ids would transition inactive-to-active. Java asynchronous `ThreadPoolManager.execute` and `AIEventType.ACTIVATE` notifications remain unported. |
| `com.aionemu.gameserver.world.MapRegion.scheduleDeactivation` | `WorldMapRegionLifecyclePlanService.CreateScheduledDeactivationPlan` | Delayed Deactivation Intent | Partial | Unit Tested | Partial Parity | C# models 60-second delay metadata, pending clear, self-player guard, neighbour-player guard, instance-map guard, Transidium Annex guard, and inactive-region skip. It does not execute a scheduler or synchronize live state. |
| `com.aionemu.gameserver.world.MapRegion.tryDeactivate` | `WorldMapRegionLifecyclePlan.DeactivatedRegionIds`; `BlockedReason` | Deactivation Intent | Partial | Unit Tested | Partial Parity | C# reports active region ids that would deactivate and reasons deactivation is blocked. Java `AIEventType.DEACTIVATE` notification and creature iteration remain unported. |
| `com.aionemu.gameserver.world.WorldMapType.TRANSIDIUM_ANNEX` | `WorldMapRegionLifecyclePlanService.TransidiumAnnexWorldId` | Enum Constant / Guard | Partial | Unit Tested | Needs Verification | Constant `400030000` was verified by Java source review and covered by tests for deactivation blocking. Broader `WorldMapType` enum remains unported at this boundary. |
| `com.aionemu.gameserver.world.MapRegion` | `WorldMapRegionLifecycleContext`; `WorldMapRegionLifecycleRegionState`; `WorldMapRegionLifecyclePlan` | Region Runtime Boundary DTO | Partial | Unit Tested | Needs Verification | DTOs model player count, active state, deactivation pending flag, and neighbour states. Missing live object maps, `ZoneInstance[]`, parent instance references, synchronized methods, volatile/threading behavior, AI notifications, zone revalidation, and death/item-use zone callbacks. |

## Remaining Risks

- Lifecycle model is non-live and does not mutate `MapRegion.objects`, `playerCount`, `regionActive`, or `deactivationPending`.
- Java synchronization/volatile behavior and scheduled task timing are represented as metadata only.
- AI activation/deactivation notifications and creature iteration are not executed.
- Zone revalidation, death callbacks, item-use zone checks, and live `ZoneInstance[]` behavior remain unported.
- Parent instance references and full `WorldMapType` enum behavior remain incomplete.
- Charge-all DB rollback remains a future gap: fake-repository tests prove runtime no-mutation/no-packet behavior, not MySQL transaction rollback.
- `docs/commit-conventions.md` is still missing.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped rows.
- Total artifacts ported: 1 non-live lifecycle intent helper plus 8 focused tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 2 grouped rows explicitly marked Needs Verification; remaining rows are Partial Parity with known gaps.
- Total blocked artifacts: live C# MapRegion storage, object map mutation, scheduler execution, synchronization/volatile runtime parity, AI notifications, zone revalidation, full `WorldMapType`, charge-all MySQL rollback regression.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Region runtime snapshot/readiness model | snapshot/lifecycle helper and tests | Compose constructor prerequisites plus active/player/deactivation state before live storage. |
| Charge-all DB rollback integration regression | gated DB integration tests | Prove actual MySQL rollback/no-DB-mutation on partial charge-all save failure. |
| Nearby packet golden gap audit | packet tests/docs | Keep read-only unless adding isolated packet tests. |

## Next Work Options

## Recommended Sequential Task

- Task: compose `WorldMapRegionCreationSnapshot` with `WorldMapRegionLifecyclePlanService` into a non-live region runtime snapshot/readiness model.
- Why: constructor prerequisites and lifecycle plans now exist separately; the next step is one DTO boundary for future live `MapRegion` storage.
- Files: likely a new runtime snapshot helper/tests plus docs.
- Java source to read: `MapRegion` constructor, `getObjects`, `getNeighbours`, `isActive`, `getZoneCount`.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
| --- | --- | --- | --- | --- |
| A | Charge-all DB rollback integration test planning | repository integration tests/fixtures | Medium | Independent from region lifecycle files. |
| B | Nearby packet golden gap audit | packet tests/docs read-only unless assigned | Low | Avoid live send work. |
| C | Zone handler source audit | read-only Java zone handler files | Low | Useful before live zone callbacks. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
| --- | --- | --- | --- |
| Orchestrator | Region runtime snapshot/readiness model | new helper/tests, docs | Java writes, live nearby dispatch |
| Read-only Agent | Charge-all DB rollback test planning | read-only repository integration tests/fixtures | all writes |

## Do Not Parallelize

- Java source files: read-only only.
- Live nearby dispatch and live `MapRegion` storage: still high risk and intentionally disabled.
- Shared lifecycle/snapshot helper changes: one owner only if implementation begins.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1639] Model MapRegion lifecycle intents
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/WorldMapRegionLifecyclePlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRegionLifecyclePlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6ARG-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
