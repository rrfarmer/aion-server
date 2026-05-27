# Phase 6ANQ Completion - Protection Stop Trigger Unspawned Callback Reader Fixture

Date: 2026-05-27
Unit of Work: UOW-1545
Status: Complete after validation.

## Scope

Add non-live reader fixture coverage for unspawned scheduled stop-callback skip-fanout behavior, while keeping generated-artifact runtime comparison blocked.

## Completed Work

- Extended `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Added an inline schema-v1 fixture for `PlayerController.stopProtectionActiveTask` where the scheduled callback fires after the player is no longer spawned.
- The fixture records Java's ordering:
  - `schedule_enter`;
  - `task_add`;
  - `callback_enter`;
  - `stop_call_enter`;
  - `task_cancel`;
  - `spawn_guard`;
  - `callback_return`.
- Added assertions that `task_cancel` occurs before the `player.isSpawned()` guard and that unspawned stop skips:
  - `visual_mutate`;
  - `state_broadcast`;
  - `ai_notify_enqueue`.
- Preserved scheduler metadata and `Future.cancel(false)` fields while keeping cancel return values as diagnostics only.
- Integrated read-only unspawned callback/caller-origin source audit from explorer `Hooke the 2nd`; it did not edit files.
- No Java instrumentation, trace serializer, generated artifacts, production scheduler execution, production packet handler hook, packet runtime integration, controller task-map owner, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Result: passed 15 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonDesignReportServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerSummaryReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 183 tests.

## Migration Parity Table - UOW-1545

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Guarded Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixtures now cover unspawned scheduled callback behavior where `cancelTask(TaskId.PROTECTION_ACTIVE)` occurs before `player.isSpawned()` and visual clear/fanout/AI notify are skipped. Start callers remain fixture-discovered but not fully covered. No Java runtime trace exists. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Task Map Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture asserts remove-before-`Future.cancel(false)` still occurs on the unspawned branch. Java `ConcurrentHashMap.compute`, stale callback task removal, ignored cancel return values, and race behavior remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Scheduler / Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Fixture preserves scheduler delay/unit/wrapper/callback metadata, but no Java scheduled executor artifact exists. Runtime callback timing and wrapper behavior remain unverified. |
| `java.util.concurrent.ScheduledFuture` / `Future` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Future / Threading Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixture records `Future.cancel(false)` on unspawned callback cancellation. Future identity serialization, callback already-started races, and cancel return semantics remain unresolved without runtime comparison. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Caller-Origin Dependency | Partial | Manual Source Audit | Needs Verification | Read-only audit found login-ready starts protection before `World.spawn`; this is a high-value future caller-origin fixture, but no C# fixture was added in this unit. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Caller-Origin Dependency | Partial | Manual Source Audit | Needs Verification | Read-only audit found same-map and channel-change start-protection callers. Caller ordering, spawn packet ordering, and delayed teleport behavior remain unverified. |
| `instance.drakenspire.BeritraPortalAI` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | AI Caller-Origin Dependency | Partial | Manual Source Audit | Needs Verification | Read-only audit found portal fade-out teleport callers that start protection around delayed spawn machinery. Dynamic handler parity and runtime caller ordering remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `UnspawnedScheduledCallbackArtifacts_CancelTaskAndSkipFanout` | Unit / schema semantics | `PlayerController.stopProtectionActiveTask` and `CreatureController.cancelTask` source review plus read-only audit | Scheduled callback fixture binds task cancellation before spawned guard, then asserts visual mutation, state broadcast, and AI notify are absent when `player.isSpawned()` is false. | Deterministic fixture assertions over source-reviewed branches. | Fixture is not generated by Java runtime; actual stale callback races, future identity, and live fanout absence are not compared. |
| Existing reader fixture tests | Unit / guarded reader smoke/regression | UOW-1541 through UOW-1544 reader shape and schema design | Existing schema binding, guarded scan, stop-path, no-stop, invalid-after-stop, scheduled callback, replacement race, timestamp diagnostics, and stable-name checks remain passing. | Regression coverage in focused and slice tests. | No generated Java artifacts exist locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Artifact reader fixtures are test-only and do not consume real Java output yet.
- No Java instrumentation, trace serializer, generated artifact, production packet handler stop hook, packet runtime integration, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, material-skill suppression integration, cooldown date comparison, action `instanceof` ordering comparison, or composition runtime comparison exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, stale callback behavior, task-map replacement, `ConcurrentHashMap.compute`, caller-origin ordering, and weak iteration remain unverified by runtime comparison.
- Inline fixtures prove reader branch representation only. They are not Java runtime evidence and do not justify Verified Parity.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: no production artifacts; 1 guarded test-side trace artifact reader expanded with unspawned scheduled callback skip-fanout fixture coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java trace instrumentation, Java trace serializer, generated Java trace artifacts, production packet handler stop hooks, production packet runtime dispatch, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/action-packet runtime comparison, scheduler timing/race comparison, caller-origin runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add non-live reader fixture coverage for start-protection caller-origin surfaces.
- Prioritize `CM_LEVEL_READY` because Java calls `startProtectionActiveTask()` before `World.spawn(activePlayer)`.
- Include caller origin, source file/line, start guard, visual set, cast/target cleanup, state fanout, and task schedule metadata.
- Keep generated-artifact runtime comparison blocked.
- Do not claim runtime verification until generated Java artifacts exist.

## Suggested Acceptance Criteria

- Existing reader test adds inline schema-v1 fixtures or helpers for `CM_LEVEL_READY` caller-origin metadata.
- Fixture records caller origin `cm_level_ready_before_world_spawn`, Java line breadcrumbs, and start-before-world-spawn ordering.
- Fixture records `startProtectionActiveTask` start guard, visual `BLINKING` set, cast/target cleanup, state fanout, and scheduled callback creation.
- Tests assert no runtime parity is claimed and guarded filesystem scan still reports Needs Verification when artifacts are absent.
- Existing protection stop-trigger focused and slice tests continue to pass.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add `CM_LEVEL_READY` caller-origin reader fixture coverage | existing reader test file only | Low | One writer only because it edits the reader test. |
| B | Read-only caller-source audit | Java packet/service/AI source only | Low | Can verify exact lines and caller ordering without writes. |
| C | Add teleport/portal caller-origin reader fixtures | existing reader test file only | Low-Med | Same file as A, so keep sequential unless split into a later unit. |
| D | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker; do not mix with reader fixture changes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add `CM_LEVEL_READY` caller-origin reader fixture coverage and docs | existing reader test file, progress/handoff docs | production packet handlers, packet runtime, scheduler implementation |
| Explorer | Read-only caller-source audit | read-only Java packet/service/AI/controller source inspection | all writes, shared docs, C# edits |

Shared docs remain orchestrator-owned. Do not assign multiple agents to the same test file.

## Do Not Parallelize

- Production packet handler wiring.
- Java instrumentation edits.
- Packet runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1545] Add protection stop trigger unspawned callback reader fixture`.
- Files changed in UOW-1545:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANQ-Completion.md`
- Latest prior commits:
  - `1a00195ec [Phase 6][UOW-1544] Add protection stop trigger scheduled callback reader fixtures`
  - `7eb149c44 [Phase 6][UOW-1543] Add protection stop trigger invalid-after-stop reader fixtures`
  - `fe396f319 [Phase 6][UOW-1542] Add protection stop trigger no-stop reader fixtures`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
