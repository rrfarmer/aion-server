# Phase 6AQJ Completion - Delayed Nearby Refresh Scheduling Plan

Date: 2026-05-28
Unit of Work: UOW-1616
Status: Complete after focused validation

## Scope

This unit captured Java `WorldMapInstance.addObject(Npc)` delayed nearby-refresh scheduling as non-live C# metadata. Java adds `QuestNpc.onQuestStart` ids into the instance quest-id set, schedules one `updateNearbyQuests` task after 1500ms only when new ids were added, suppresses duplicate schedules while a task is pending, then clears the pending task before iterating players.

C# now records that decision without creating timers, iterating players, dispatching controller callbacks, or sending packets.

## Completed Work

- Added `WorldMapInstanceRuntimeState.RegisterQuestStartIdsAndPlanNearbyRefresh`.
- Added `WorldMapNearbyQuestRefreshSchedulePlan` and `WorldMapNearbyQuestRefreshScheduleStatus`.
- Added pending nearby-refresh metadata and `CompletePendingNearbyQuestRefresh`.
- Preserved existing `RegisterQuestStartIds` behavior.
- Added tests for initial scheduling, duplicate suppression, pending suppression with new ids, and re-scheduling after completion.
- Updated `docs/PHASE-6-PROGRESS.md` with discovery, Migration Parity Table, risks, metrics, and next-unit guidance.

## Validation

Focused world/nearby tests passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~WorldMapRuntimeStateTests|FullyQualifiedName~NearbyQuestRefreshInputAdapterServiceTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests"
```

Result: 31 passed, 0 failed.

Full game-server suite was not rerun in this unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| WorldMapInstance delayed nearby-refresh scheduling plan | `WorldMapInstanceRuntimeState.cs`, world runtime tests | Low | Yes | Compact non-live metadata mirror of Java pending-task rule. |
| Production ThreadPool nearby refresh dispatch | world services, connection dispatch | High | No | Deferred; would require live player iteration and packet sends. |
| ItemCharge storage-location audit | read-only Java/C# charge files | Low | No | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/generated artifacts | High | No | Blocked by Java tooling/runtime artifact strategy. |

No sub-agent was spawned because the runtime state mutation and pending flag need one owner.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `WorldMapInstanceRuntimeState_PlansDelayedNearbyRefreshLikeJavaWorldMapInstance` | Added | New quest ids schedule once with 1500ms metadata; duplicates do not schedule; new ids while pending do not create a second schedule. | Source-derived from Java `WorldMapInstance.addObject(Npc)`. |
| `WorldMapInstanceRuntimeState_CompletesPendingNearbyRefreshBeforeSchedulingAgain` | Added | Clearing pending metadata allows a later new quest id to schedule again. | Source-derived from Java scheduled callback clearing `updateNearbyQuestsTask`. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | `Aion.GameServer.World.WorldMapInstanceRuntimeState.RegisterQuestStartIdsAndPlanNearbyRefresh` | Runtime State / Scheduling Plan | Partial | Unit Tested | Partial Parity | C# records the Java delayed scheduling decision for newly added quest ids and pending-task suppression. It does not add visible objects or NPC maps. |
| `com.aionemu.gameserver.utils.ThreadPoolManager.schedule` | `WorldMapNearbyQuestRefreshSchedulePlan.Delay` | Scheduler Dependency | Partial | Unit Tested | Needs Verification | Java 1500ms delay is metadata only. No real timer, cancellation, thread execution, or Java runtime timing comparison was performed. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | future dispatch after `WorldMapNearbyQuestRefreshSchedulePlan` | Controller Callback Dependency | Partial | Unit Tested scheduling intent only | Needs Verification | C# clears pending metadata but does not iterate players or call the nearby-refresh adapter. |
| `com.aionemu.gameserver.questEngine.handlers.models.QuestNpc` | `QuestNpcStartTable` / `WorldMapInstanceRuntimeState.RegisterQuestStartIdsAndPlanNearbyRefresh` | Static Quest-NPC Registration Dependency | Partial | Existing Regression Tested + Unit Tested scheduling | Needs Verification | Java `QuestEngine.getQuestNpc` runtime lookup and NPC object/template integration remain partial. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | future dispatch after schedule plan | Packet Dependency | Partial | Existing Regression Tested | Needs Verification | This unit only plans delayed refresh scheduling; no packet serialization, send, or byte comparison occurred. |

## Remaining Risks

- C# records delayed-refresh scheduling intent only; it does not execute timers, iterate live players, or send packets.
- Java `WorldMapInstance.addObject` object maps, NPC maps, player maps, zones, and duplicate object behavior remain only partially represented.
- Threading behavior differs intentionally for the non-live plan: C# uses a lock-protected flag rather than Java's `Future` field and concurrent collections.
- Production `PlayerController.updateNearbyQuests` remains unwired.
- Packet bytes, socket ordering, reflection/dynamic quest lookups, date/time handling, and serialization remain unverified.
- `docs/commit-conventions.md` was requested by startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped rows.
- Total artifacts ported: 1 non-live delayed nearby-refresh scheduling plan plus 2 focused unit tests.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 4 grouped rows explicitly marked Needs Verification.
- Total blocked artifacts: live ThreadPool execution, player iteration, production controller wiring, packet-byte comparison, full WorldMapInstance object/NPC map parity, Java runtime timing comparison.
- Estimated overall migration completion: about 72%.

## Next Recommended Unit Of Work

Next best unit:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Non-live delayed nearby-refresh execution report | new small service/test using schedule plan, player list, `StaticData`, and `NearbyQuestRefreshInputAdapterService` | Record per-player refresh plans without timers or sends. |
| ItemCharge storage-location audit | read-only Java `Inventory`/`Equipment` lifecycle and C# selected-charge lookup | Safe support task after UOW-1611. |
| Java protection serializer implementation | Java serializer/generated artifacts | Use only if Java tooling/runtime artifact strategy is ready. |

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1616] Plan delayed nearby quest refresh scheduling
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6AQJ-Completion.md`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
