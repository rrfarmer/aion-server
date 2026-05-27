# Phase 6ANU Completion - Protection Stop Trigger Beritra Portal Caller Fixture

Date: 2026-05-27
Unit of Work: UOW-1549
Status: Complete after validation.

## Scope

Add non-live reader fixture coverage for the `BeritraPortalAI` fade-out teleport start-protection caller-origin path, while keeping generated-artifact runtime comparison blocked.

## Completed Work

- Extended `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Added an inline schema-v1 fixture for `BeritraPortalAI.handleUseItemFinish(Player)`.
- Captured the Java ordering where the AI queues a `TeleportAnimation.FADE_OUT` teleport, `TeleportService.sendLoc` despawns the player and stores `TaskId.TELEPORT`, then Beritra immediately calls `player.getController().startProtectionActiveTask()`.
- Captured the delayed `SM_PLAY_MOVIE(false, 0, 0, 915, true)` callback scheduled after 1000 ms.
- The fixture records Java's ordering:
  - `caller_enter`;
  - `teleport_to_enter`;
  - `teleport_sendloc_despawn`;
  - `teleport_animation_packet`;
  - `teleport_task_store`;
  - `start_guard`;
  - `start_visual_set`;
  - `start_cast_target_cleanup`;
  - `start_state_fanout`;
  - `start_task_schedule`;
  - `movie_schedule`.
- Added assertions that the Beritra path uses caller origin `beritra_portal_fade_out_before_animation_done`, records `startProtectionLine=38`, records later same-map spawn line `211`, marks `startsProtectionBeforeWorldSpawn=true`, marks `spawnedBeforeStart=false`, and records ordering `fade_out_teleport_task_before_start_protection_before_animation_done`.
- Added assertions that the caller starts while spawned, `sendLoc` leaves the player unspawned before protection starts, `BLINKING` is set, `SM_PLAYER_STATE` start fanout includes self, the 60000 ms protection stop callback is scheduled, and the 1000 ms movie callback is represented.
- Kept the three random destination coordinate branches explicit as a risk; the fixture uses one representative branch because all three share the same control-flow shape.
- Kept animation-completion spawn out of the fixture because it depends on client `CM_TELEPORT_ANIMATION_DONE`; the later same-map `spawnOnSameMap` call can attempt `startProtectionActiveTask()` again at `TeleportService.java:213`, but that branch should be skipped while protection remains active.
- Integrated read-only Beritra portal source audit from explorer `Locke the 2nd`; it did not edit files.
- No Java instrumentation, trace serializer, generated artifacts, production scheduler execution, production packet handler hook, packet runtime integration, controller task-map owner, socket fanout, known-list mutation, AI move notification, `TaskId.TELEPORT` runtime execution, `CM_TELEPORT_ANIMATION_DONE` execution, `SpawnTask.run` execution, same-map respawn execution, direct movie packet comparison, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Result: passed 19 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonDesignReportServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerSummaryReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 187 tests.

## Migration Parity Table - UOW-1549

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `ai.instance.drakenspire.BeritraPortalAI` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | AI / Caller-Origin Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixture now covers fade-out teleport queuing before immediate `startProtectionActiveTask()` at line 38 and 1000 ms movie scheduling at line 39. `Rnd.get(1, 3)` destination randomness is represented by one deterministic branch; other coordinate branches share control flow but are not separately fixture-tested. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Service / Delayed Teleport Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records `sendLoc` despawn at line 185, `SM_TELEPORT_LOC` at line 192, and `TaskId.TELEPORT` future storage at line 194 before protection starts. `SpawnTask.run`, same-map spawn, full reload branch, leave-map hooks, and pet position update remain unexecuted. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet Handler / Deferred Spawn Dependency | Partial | Manual Source Audit | Needs Verification | Source audit confirmed animation done removes/runs the stored teleport task, but this unit intentionally excludes animation completion and world spawn from the fixture. Runtime task execution and exception fallback remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Start Protection Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture records start guard, BLINKING set, cast/target cleanup, start-state fanout, and scheduling a 60000 ms stop callback while player is despawned/fading out. Later `spawnOnSameMap` may call start again at line 213 and skip due to already-active protection; not fixture-tested here. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Task Map Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture records `TaskId.TELEPORT` future storage and `TaskId.PROTECTION_ACTIVE` scheduling metadata, but does not execute Java `FutureTask`, task replacement, stale callback removal, or `ConcurrentHashMap.compute` race behavior. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Scheduler / Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture preserves 60000 ms protection scheduling and 1000 ms movie scheduling metadata. Java executor timing, wrapper behavior, callback order, and movie packet send remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet/Fanout Utility Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records `SM_TELEPORT_LOC`, `SM_PLAYER_STATE`, and delayed `SM_PLAY_MOVIE` surfaces, but recipient count remains diagnostic after despawn. Socket order and recipient filtering remain unverified. |
| `com.aionemu.gameserver.world.World` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | World / Despawn and Later Spawn Dependency | Partial | Manual Source Audit | Needs Verification | Fixture records despawned state after `sendLoc`; later same-map `World.spawn(player)` at line 211 is represented only as caller-origin metadata. Region removal, known-list clearing, spawn, pet spawn, and `onAfterSpawn` remain unexecuted. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records the delayed movie callback target and 1000 ms delay, but does not serialize, send, or byte-compare `SM_PLAY_MOVIE(false, 0, 0, 915, true)`. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BeritraPortalCallerOriginArtifacts_RecordFadeOutTeleportBeforeStartProtection` | Unit / schema semantics | `BeritraPortalAI.handleUseItemFinish`, `TeleportService.sendLoc`, `CM_TELEPORT_ANIMATION_DONE`, and `PlayerController.startProtectionActiveTask` source review plus read-only audit | Caller-origin fixture binds fade-out teleport queued before start-protection metadata, server-side unspawned start state, BLINKING set, cast/target cleanup, start fanout, protection scheduler metadata, delayed movie scheduler metadata, and absence of animation-completion spawn from this artifact. | Deterministic fixture assertions over source-reviewed branches. | Fixture is not generated by Java runtime; destination randomness, real `SM_TELEPORT_LOC`/`SM_PLAY_MOVIE` serialization, `TaskId.TELEPORT` execution, same-map respawn, later skipped start, recipient count, and scheduler execution are not compared. |
| Existing reader fixture tests | Unit / guarded reader smoke/regression | UOW-1541 through UOW-1548 reader shape and schema design | Existing schema binding, guarded scan, stop-path, no-stop, invalid-after-stop, scheduled callback, replacement race, unspawned callback, `CM_LEVEL_READY`, same-map teleport, change-channel teleport, timestamp diagnostics, and stable-name checks remain passing. | Regression coverage in focused and slice tests. | No generated Java artifacts exist locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Artifact reader fixtures are test-only and do not consume real Java output yet.
- No Java instrumentation, trace serializer, generated artifact, production packet handler stop hook, packet runtime integration, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, world-spawn execution, world-position/despawn execution, `TaskId.TELEPORT` runtime execution, `CM_TELEPORT_ANIMATION_DONE` execution, `SM_TELEPORT_LOC`/`SM_PLAY_MOVIE` comparison, inbound-damage guard, aggro suppression, target/skill rejection, material-skill suppression integration, cooldown date comparison, action `instanceof` ordering comparison, or composition runtime comparison exists.
- Java `Future.cancel(false)`, `FutureTask`, `ScheduledFuture`, stale callback behavior, task-map replacement, `ConcurrentHashMap.compute`, caller-origin ordering, packet send ordering, world position/despawn/spawn side effects, known-list fanout, destination randomness, scheduler timing, and weak iteration remain unverified by runtime comparison.
- Inline fixtures prove reader branch representation only. They are not Java runtime evidence and do not justify Verified Parity.

## Summary Metrics

- Total Java artifacts discovered: 9 grouped artifact rows in this unit
- Total artifacts ported: no production artifacts; 1 guarded test-side trace artifact reader expanded with `BeritraPortalAI` fade-out caller-origin fixture coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 9 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java trace instrumentation, Java trace serializer, generated Java trace artifacts, production packet handler stop hooks, production packet runtime dispatch, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, socket fanout, known-list mutations, AI move notification, Java/C# future/future-task/concurrent-map/action-packet runtime comparison, scheduler timing/race comparison, caller-origin runtime comparison, world-position/despawn/spawn runtime comparison, delayed teleport runtime comparison, movie-packet runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add non-live reader fixture coverage for Beritra animation-completion delayed spawn (`CM_TELEPORT_ANIMATION_DONE` running `TeleportService.SpawnTask`), or move to generated Java trace artifact scaffolding if tooling becomes available.
- Focus on Java ordering after the client animation completes:
  - remove `TaskId.TELEPORT`;
  - run `SpawnTask`;
  - call `World.setPosition`;
  - same-map `spawnOnSameMap`;
  - later `startProtectionActiveTask` at `TeleportService.java:213` should skip while protection is already active from Beritra line 38.
- Keep generated-artifact runtime comparison blocked.
- Do not claim runtime verification until generated Java artifacts exist.

## Suggested Acceptance Criteria

- Existing reader test adds inline schema-v1 fixture or helper for Beritra animation completion and delayed spawn.
- Fixture records `CM_TELEPORT_ANIMATION_DONE`, `TaskId.TELEPORT` removal, `SpawnTask.run`, same-map spawn, and already-active protection start skip.
- Tests avoid asserting unstable known-list recipient counts unless generated Java artifacts later prove them.
- Guarded filesystem scan remains unchanged and still reports Needs Verification when artifacts are absent.
- Existing protection stop-trigger focused and slice tests continue to pass.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add Beritra animation-completion delayed-spawn fixture | existing reader test file only | Medium | One writer only because it edits the reader test and follows the fixture just added. |
| B | Start generated Java trace artifact scaffolding | new Java instrumentation/design files if tooling is available | High | Only start if Java/Maven tooling is available and scope is explicit. |
| C | Read-only delayed-spawn source audit | Java teleport/client packet/world/controller source only | Low | Can verify exact lines and skipped-start semantics without writes. |
| D | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker; do not mix with reader fixture changes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add Beritra delayed-spawn reader fixture and docs | existing reader test file, progress/handoff docs | production packet handlers, packet runtime, scheduler implementation |
| Explorer | Read-only delayed-spawn source audit | read-only Java client packet/service/world/controller source inspection | all writes, shared docs, C# edits |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same test file.

## Do Not Parallelize

- Production packet handler wiring.
- Java instrumentation edits without an explicit generated-artifact design unit.
- Packet runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1549] Add protection stop trigger Beritra portal caller fixture`.
- Files changed in UOW-1549:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANU-Completion.md`
- Latest prior commits:
  - `06edef69b [Phase 6][UOW-1548] Add protection stop trigger change-channel caller fixture`
  - `02759364e [Phase 6][UOW-1547] Add protection stop trigger teleport same-map caller fixture`
  - `e5aee7540 [Phase 6][UOW-1546] Add protection stop trigger level-ready caller fixture`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
