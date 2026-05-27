# Phase 6ANV Completion - Protection Stop Trigger Beritra Animation Completion Fixture

Date: 2026-05-27
Unit of Work: UOW-1550
Status: Complete after validation.

## Scope

Add non-live reader fixture coverage for Beritra animation-completion delayed spawn (`CM_TELEPORT_ANIMATION_DONE` running `TeleportService.SpawnTask`), while keeping generated-artifact runtime comparison blocked.

## Completed Work

- Extended `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Added an inline schema-v1 fixture for `CM_TELEPORT_ANIMATION_DONE.runImpl` removing the stored `TaskId.TELEPORT` future and synchronously running `TeleportService.SpawnTask.run`.
- Captured the normal Beritra same-map delayed completion path:
  - `SpawnTask.run` starts while the player is unspawned and already protected from Beritra line 38;
  - player and pet positions are set;
  - same-map `spawnOnSameMap` packet surface is represented;
  - player and pet spawn;
  - the later `startProtectionActiveTask` attempt at `TeleportService.java:213` reaches the already-active guard and skips.
- Corrected the fixture semantics so `CM_TELEPORT_ANIMATION_DONE` is not represented as directly starting protection. The protection start attempt belongs to `TeleportService.spawnOnSameMap` after world spawn.
- The fixture records Java's ordering:
  - `animation_done_enter`;
  - `teleport_task_remove`;
  - `spawn_task_run`;
  - `spawn_task_abort_actions`;
  - `world_position_set`;
  - `pet_position_set`;
  - `same_map_spawn_packets`;
  - `world_spawn_completed`;
  - `pet_spawn_completed`;
  - `protection_start_skip`;
  - `post_spawn_cleanup`.
- Added assertions that `TaskId.TELEPORT` is removed without cancellation, `SpawnTask.run` executes while unspawned/protected, position set keeps the player unspawned before same-map spawn, player spawn precedes the skipped protection start, and no second visual mutation, state fanout, or 60000 ms protection schedule is emitted.
- Integrated read-only delayed-spawn source audit from explorer `Carson the 2nd`; it did not edit files.
- No Java instrumentation, trace serializer, generated artifacts, production packet handler hook, packet runtime integration, live `FutureTask` execution, live `CM_TELEPORT_ANIMATION_DONE` execution, live world spawn, packet serialization comparison, socket fanout, known-list mutation, pet spawn execution, zone/effect update execution, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Result: passed 20 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonDesignReportServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerSummaryReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 188 tests.

## Migration Parity Table - UOW-1550

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet Handler / Deferred Spawn Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixture now covers `getAndRemoveTask(TaskId.TELEPORT)` at line 36 and synchronous pending `RunnableFuture` execution at lines 37-41. Missing/finished/non-runnable task no-op behavior and exception fallback remain unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Service / SpawnTask Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records normal delayed `SpawnTask.run` after animation completion: unspawned guard, abort actions, player/pet position set, same-map packet sequence, player/pet spawn, already-active protection start skip, and post-spawn cleanup. Full reload branch, dead/missing-instance fallback, live packets, leave-map hooks, and real effect/zone updates remain unexecuted. |
| `com.aionemu.gameserver.world.World` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | World / Position and Spawn Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records unspawned position set before same-map spawn and spawned state after `World.spawn(player)`. It does not execute region insert/remove, known-list update, pet spawn, zone revalidation, or `onBeforeSpawn`/`onAfterSpawn`. |
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Already-Active Start Guard Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture records the second `startProtectionActiveTask` attempt after same-map spawn as an already-active guard skip with no second visual mutation, fanout, or schedule refresh. Live task state, near-timeout timer behavior, and concurrent state races remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Task Map Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records `TaskId.TELEPORT` removal without cancellation and distinguishes it from `cancelTask`. Java `ConcurrentHashMap`, non-runnable task no-op, finished future no-op, and task replacement races remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet Utility Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records the same-map packet sequence as metadata but does not serialize or send `SM_CHANNEL_INFO`, `SM_PLAYER_INFO`, `SM_STATS_INFO`, or `SM_MOTION`. Socket order and byte parity remain unverified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CHANNEL_INFO` / `SM_PLAYER_INFO` / `SM_STATS_INFO` / `SM_MOTION` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Packet Dependencies | Partial | Unit Tested Metadata | Needs Verification | Same-map spawn packets are represented as a grouped packet surface only. No packet byte comparison, field serialization, or client-visible ordering proof exists in this unit. |
| `ai.instance.drakenspire.BeritraPortalAI` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | AI Caller-Origin Dependency | Partial | Regression Tested Metadata | Needs Verification | This unit depends on UOW-1549's Beritra pre-animation protection start. It confirms the later same-map start attempt should skip rather than refresh that protection timer, but does not execute the AI or random destination selection. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `BeritraAnimationDoneArtifacts_RecordSameMapSpawnAndProtectionStartSkip` | Unit / schema semantics | `CM_TELEPORT_ANIMATION_DONE.runImpl`, `TeleportService.SpawnTask.run`, `TeleportService.spawnOnSameMap`, `World`, `CreatureController`, and `PlayerController.startProtectionActiveTask` source review plus read-only audit | Caller-origin fixture binds animation-completion task removal, synchronous spawn-task run, same-map packet/spawn ordering, and already-active protection start skip without a second scheduler/fanout. | Deterministic fixture assertions over source-reviewed branches. | Fixture is not generated by Java runtime; no live future execution, packet serialization, known-list update, pet spawn, effect icon update, zone update, or exception fallback comparison exists. |
| Existing reader fixture tests | Unit / guarded reader smoke/regression | UOW-1541 through UOW-1549 reader shape and schema design | Existing schema binding, guarded scan, stop-path, no-stop, invalid-after-stop, scheduled callback, replacement race, unspawned callback, `CM_LEVEL_READY`, same-map teleport, change-channel teleport, Beritra portal pre-animation start, timestamp diagnostics, and stable-name checks remain passing. | Regression coverage in focused and slice tests. | No generated Java artifacts exist locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Artifact reader fixtures are test-only and do not consume real Java output yet.
- No Java instrumentation, trace serializer, generated artifact, production packet handler stop hook, packet runtime integration, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, live `FutureTask`, live `CM_TELEPORT_ANIMATION_DONE`, same-map packet serialization, world spawn, pet spawn, zone/effect update execution, inbound-damage guard, aggro suppression, target/skill rejection, material-skill suppression integration, cooldown date comparison, action `instanceof` ordering comparison, or composition runtime comparison exists.
- Java `Future.cancel(false)`, `FutureTask`, `RunnableFuture`, `ScheduledFuture`, stale callback behavior, task-map removal/replacement, `ConcurrentHashMap.compute`, caller-origin ordering, packet send ordering, world position/despawn/spawn side effects, known-list fanout, destination randomness, scheduler timing, and weak iteration remain unverified by runtime comparison.
- Inline fixtures prove reader branch representation only. They are not Java runtime evidence and do not justify Verified Parity.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: no production artifacts; 1 guarded test-side trace artifact reader expanded with Beritra animation-completion delayed-spawn fixture coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java trace instrumentation, Java trace serializer, generated Java trace artifacts, production packet handler stop hooks, production packet runtime dispatch, production controller owner wiring, production lifecycle hook, live future/task-map execution, live delayed teleport execution, live scheduler callback execution, socket fanout, known-list mutations, AI move notification, Java/C# future/future-task/concurrent-map/action-packet runtime comparison, scheduler timing/race comparison, caller-origin runtime comparison, world-position/despawn/spawn runtime comparison, delayed teleport runtime comparison, same-map packet runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add non-live reader fixture coverage for delayed teleport missing-instance/dead-player fallback, or pivot to generated Java trace artifact scaffolding when Java/Maven tooling becomes available.
- Focus on Java ordering in `TeleportService.SpawnTask.run`:
  - delayed teleport branch;
  - `player.isDead()` or missing instance condition;
  - send `SM_PLAYER_INFO`;
  - `World.spawn(player)`;
  - return before `World.setPosition`, pet position set, same-map/full-reload branch, or protection start attempt.
- Keep generated-artifact runtime comparison blocked.
- Do not claim runtime verification until generated Java artifacts exist.

## Suggested Acceptance Criteria

- Existing reader test adds inline schema-v1 fixture or helper for the delayed teleport fallback branch.
- Fixture records fallback packet/spawn ordering and explicitly asserts no destination position set, no pet position set, no same-map packet sequence, and no additional `startProtectionActiveTask` call.
- Tests avoid asserting unstable known-list recipient counts unless generated Java artifacts later prove them.
- Guarded filesystem scan remains unchanged and still reports Needs Verification when artifacts are absent.
- Existing protection stop-trigger focused and slice tests continue to pass.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add delayed-teleport fallback fixture | existing reader test file only | Medium | One writer only because it edits the reader test and follows the delayed-spawn fixture. |
| B | Start generated Java trace artifact scaffolding | new Java instrumentation/design files if tooling is available | High | Only start if Java/Maven tooling is available and scope is explicit. |
| C | Read-only fallback source audit | Java teleport/client packet/world/controller source only | Low | Can verify exact lines and fallback return semantics without writes. |
| D | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker; do not mix with reader fixture changes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add delayed-teleport fallback reader fixture and docs | existing reader test file, progress/handoff docs | production packet handlers, packet runtime, scheduler implementation |
| Explorer | Read-only fallback source audit | read-only Java client packet/service/world/controller source inspection | all writes, shared docs, C# edits |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same test file.

## Do Not Parallelize

- Production packet handler wiring.
- Java instrumentation edits without an explicit generated-artifact design unit.
- Packet runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1550] Add protection stop trigger Beritra animation completion fixture`.
- Files changed in UOW-1550:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANV-Completion.md`
- Latest prior commits:
  - `b546c6ce5 [Phase 6][UOW-1549] Add protection stop trigger Beritra portal caller fixture`
  - `06edef69b [Phase 6][UOW-1548] Add protection stop trigger change-channel caller fixture`
  - `02759364e [Phase 6][UOW-1547] Add protection stop trigger teleport same-map caller fixture`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
