# Phase 6ANP Completion - Protection Stop Trigger Scheduled Callback Reader Fixtures

Date: 2026-05-27
Unit of Work: UOW-1544
Status: Complete after validation.

## Scope

Add non-live reader fixture coverage for scheduled protection stop callback and replacement/cancel race branches, while keeping generated-artifact runtime comparison blocked.

## Completed Work

- Extended `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Added an inline schema-v1 scheduled callback fixture for `PlayerController.startProtectionActiveTask` scheduling `this::stopProtectionActiveTask` with a 60000 ms delay.
- Added scheduler payload fields for delay, time unit, runnable-wrapper application, callback method, old-future presence, old-future cancel argument/result, and new-future storage.
- Added a replacement race fixture for `CreatureController.addTask` where an old protection future is cancelled with `false` before the new future is stored.
- Added assertions for Java ordering:
  - `schedule_enter`;
  - `task_add`;
  - `callback_enter`;
  - `stop_call_enter`;
  - `task_cancel`;
  - `visual_mutate`;
  - `state_broadcast`;
  - `ai_notify_enqueue`;
  - `callback_return`.
- Updated the generic artifact semantic assertion to allow scheduler-origin artifacts to start with `schedule_enter` instead of packet-origin `packet_enter`.
- Integrated read-only scheduler/controller source audit from explorer `Mendel the 2nd`; it did not edit files.
- No Java instrumentation, trace serializer, generated artifacts, production scheduler execution, production packet handler hook, packet runtime integration, controller task-map owner, socket fanout, known-list mutation, AI move notification, or protection bridge execution was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests`.
- Result: passed 14 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonDesignReportServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerSummaryReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 182 tests.

## Migration Parity Table - UOW-1544

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Guarded Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixtures now cover scheduling `stopProtectionActiveTask` with 60000 ms delay, callback entry, spawned stop visual clear, state fanout, and AI move notify ordering. Java start callers (`CM_LEVEL_READY`, teleport services, Beritra portal AI) were discovered but not ported or fixture-covered. No Java runtime trace exists. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Controller / Task Map Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixtures now cover `addTask` replacement cancellation before storing the new future and `cancelTask` remove-before-`Future.cancel(false)` ordering. Java `ConcurrentHashMap.compute`, weak iteration, cancellation races, and ignored cancel return values remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Scheduler / Artifact Reader Shape | Partial | Unit Tested Metadata | Needs Verification | Reader fixtures now record millisecond default scheduling, runnable wrapper application, and callback method metadata. Java `ScheduledThreadPoolExecutor` runtime timing, wrapper exception behavior, and callback race behavior remain unverified. |
| `java.util.concurrent.ScheduledFuture` / `Future` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Future / Threading Dependency | Partial | Unit Tested Metadata | Needs Verification | Fixtures record old-future and current-future `cancel(false)` arguments/results, but Java ignores return values and no runtime comparison confirms whether callbacks had already started. Future identity serialization and race behavior remain unresolved. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Tests.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests` | Fanout Utility Dependency | Partial | Unit Tested Metadata | Needs Verification | Read-only audit discovered self-first fanout and known-list fanout ordering, but fixtures only preserve `SM_PLAYER_STATE` fanout metadata. Socket order and recipient filtering remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `ScheduledCallbackArtifacts_RecordDelayAndStopCallbackOrdering` | Unit / schema semantics | `PlayerController.startProtectionActiveTask`, `ThreadPoolManager.schedule`, `CreatureController.addTask`, and `PlayerController.stopProtectionActiveTask` source review | Scheduled callback fixture binds delay/unit/wrapper/callback metadata and asserts task add before callback entry, stop, cancel, fanout, AI notify, and callback return. | Deterministic fixture assertions over source-reviewed branches. | Fixture is not generated by Java runtime; actual scheduling, callback timing, socket fanout, and AI task enqueue are not compared. |
| `ReplacementRaceArtifacts_RecordOldFutureCancellationBeforeNewTaskStorage` | Unit / schema semantics | `CreatureController.addTask` and `cancelTask` source review | Replacement fixture records old-future cancellation with `cancel(false)` before new future storage and current-future remove-before-cancel ordering during stop. | Deterministic fixture assertions over source-reviewed branches. | Java cancel return is ignored; runtime race behavior and future identity remain unverified. |
| Existing reader fixture tests | Unit / guarded reader smoke/regression | UOW-1541 through UOW-1543 reader shape and schema design | Existing schema binding, guarded scan, stop-path fields, no-stop branches, invalid-after-stop branches, timestamp diagnostics, and stable-name checks remain passing. | Regression coverage in focused and slice tests. | No generated Java artifacts exist locally. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- Artifact reader fixtures are test-only and do not consume real Java output yet.
- No Java instrumentation, trace serializer, generated artifact, production packet handler stop hook, packet runtime integration, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, AI move notification, inbound-damage guard, aggro suppression, target/skill rejection, material-skill suppression integration, cooldown date comparison, action `instanceof` ordering comparison, or composition runtime comparison exists.
- Java `Future.cancel(false)`, `ScheduledFuture`, callback timing, action-packet stop-trigger runtime ordering, task-map replacement, `ConcurrentHashMap.compute`, and weak iteration remain unverified by runtime comparison.
- Inline fixtures prove reader branch representation only. They are not Java runtime evidence and do not justify Verified Parity.

## Summary Metrics

- Total Java artifacts discovered: 5 grouped artifact rows in this unit
- Total artifacts ported: no production artifacts; 1 guarded test-side trace artifact reader expanded with scheduled callback/replacement race fixture coverage
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java trace instrumentation, Java trace serializer, generated Java trace artifacts, production packet handler stop hooks, production packet runtime dispatch, production controller owner wiring, production lifecycle hook, live scheduler callback execution, live delayed stop side effects, socket fanout, known-list mutations, AI move notification, Java/C# future/concurrent-map/action-packet runtime comparison, scheduler timing/race comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add non-live reader fixture coverage for start-protection caller surfaces or unspawned stop-callback skip-fanout behavior.
- Candidate caller surfaces discovered by read-only audit:
  - `CM_LEVEL_READY` login ready start call;
  - `TeleportService` same-map spawn start call;
  - `TeleportService` channel-change start call;
  - `BeritraPortalAI` portal start call.
- Alternative narrow fixture: unspawned scheduled stop callback should cancel task but skip visual clear, state fanout, and AI notify due to `player.isSpawned()` guard.
- Keep generated-artifact runtime comparison blocked.
- Do not claim runtime verification until generated Java artifacts exist.

## Suggested Acceptance Criteria

- Existing reader test adds inline schema-v1 fixtures or helpers for caller-origin metadata or unspawned callback skip-fanout behavior.
- If caller-origin fixtures are chosen, include source caller name, Java line, start guard, visual set, cast/target cleanup, state fanout, and task schedule metadata.
- If unspawned callback fixture is chosen, assert `task_cancel` occurs but `visual_mutate`, `state_broadcast`, and `ai_notify_enqueue` are absent.
- Guarded filesystem scan remains unchanged and still reports Needs Verification when artifacts are absent.
- Existing protection stop-trigger focused and slice tests continue to pass.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Add caller-origin reader fixture coverage | existing reader test file only | Low | One writer only because it edits the reader test. |
| B | Add unspawned callback skip-fanout reader fixture coverage | existing reader test file only | Low | Mutually exclusive with A for parallel writes; can be next sequential task. |
| C | Read-only caller-source audit | Java packet/service/AI source only | Low | Can verify exact lines and guard behavior without writes. |
| D | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate current full-suite blocker; do not mix with reader fixture changes. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add one reader fixture coverage slice and docs | existing reader test file, progress/handoff docs | production packet handlers, packet runtime, scheduler implementation |
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

- Current unit should be committed with message `[Phase 6][UOW-1544] Add protection stop trigger scheduled callback reader fixtures`.
- Files changed in UOW-1544:
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6ANP-Completion.md`
- Latest prior commits:
  - `7eb149c44 [Phase 6][UOW-1543] Add protection stop trigger invalid-after-stop reader fixtures`
  - `fe396f319 [Phase 6][UOW-1542] Add protection stop trigger no-stop reader fixtures`
  - `e386c3af2 [Phase 6][UOW-1541] Add protection stop trigger artifact reader shape`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
