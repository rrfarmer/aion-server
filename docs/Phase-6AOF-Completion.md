# Phase 6AOF Completion - Protection Stop Trigger C# Runtime Trace Row Contract

Date: 2026-05-27
Unit of Work: UOW-1560
Status: Complete after validation.

## Scope

Extend the non-live protection stop-trigger comparison contract with row-level C# runtime trace metadata that mirrors the Java schema-v1 trace row shape closely enough for future comparison preflight.

## Completed Work

- Updated `PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService`.
- Added `PlayerProtectionActiveTaskStopTriggerCSharpRuntimeTraceRow`.
- Added `PlayerProtectionActiveTaskStopTriggerCSharpRuntimeTracePlayerSnapshot`.
- Added C# runtime trace validation issue records and issue codes.
- Added `CreateCSharpRuntimeTraceReport` to derive scenarios from rows and validate synthetic C# trace rows.
- C# trace validation now rejects:
  - missing trace rows;
  - out-of-order event sequences;
  - timestamps marked as parity keys.
- Comparison contract C# runtime trace readiness now remains blocked when trace rows are missing or invalid.
- Updated readiness test helper to create row-level synthetic C# trace reports.
- No live packet hooks, production trace emission, Java instrumentation, generated Java artifacts, or runtime comparison executor was wired.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests"`.
- Result: passed 21 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactReaderTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|PlayerProtectionActiveTaskStopTriggerRuntimeComparisonDesignReportServiceTests|PlayerProtectionActiveTaskControllerTaskMapWiringIntentReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerSummaryReportServiceTests|PlayerProtectionActiveTaskFirstActionStopTriggerAuditServiceTests|PlayerProtectionActiveTaskLifecycleClosureReportServiceTests|PlayerProtectionActiveTaskDelayedStopCallbackPreviewServiceTests|PlayerProtectionActiveTaskSchedulerCallbackPlanServiceTests|PlayerProtectionActiveTaskReadinessAggregateServiceTests|PlayerProtectionActiveTaskControllerTaskMapOwnerPrototypeServiceTests|PlayerProtectionActiveTaskTaskMapSimulationServiceTests|PlayerProtectionActiveTaskTaskOperationPlanServiceTests|PlayerProtectionActiveTaskTaskMapAdapterServiceTests|PlayerProtectionActiveTaskAdapterServiceTests|PlayerProtectionActiveTaskExecutionBridgeServiceTests|PlayerProtectionActiveTaskExecutionSummaryServiceTests|PlayerProtectionActiveTaskReportServiceTests|PlayerProtectionActiveTaskPlanServiceTests|PlayerProtectionActiveTaskFanoutServiceTests|PlayerProtectionActiveTaskSightedRecipientTraceServiceTests|PlayerProtectionActiveTaskLiveReadinessServiceTests|PlayerStateTests"`.
- Result: passed 218 tests.

## Migration Parity Table - UOW-1560

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Packet Handler / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | C# trace rows can now represent packet name, phase, return reason, stop-called and expected-stop flags for synthetic teleport animation traces. No live packet handler executes and no Java artifact comparison exists. |
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Controller / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | Player snapshot fields can represent protection-active and visual-state before/after flags. Live `startProtectionActiveTask`, `stopProtectionActiveTask`, BLINKING mutation, scheduler callback, fanout, and AI notify remain unverified. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Controller / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | Row schema can carry event ordering needed for future task-map traces, and validation rejects out-of-order synthetic rows. Java `ConcurrentHashMap`, task removal/replacement, weak iteration, future cancellation, and race behavior remain unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Service / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | Row schema can represent delayed teleport phases/reasons synthetically. Spawn task execution, fallback packet ordering, position/pet/world-spawn behavior, and exception handling remain unverified. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Utility / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | Contract still does not execute socket sends, online gates, known-list filtering, fanout ordering, or packet bytes. Runtime comparison remains blocked. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Packet Serialization Hook Dependency | Partial | Manual Source Audit | Needs Verification | Row schema carries packet names only; no packet byte comparison, length/opcode validation, encoding validation, clear-frame observer output, or encrypted transport behavior exists in this unit. |
| `java.util.concurrent.Future` / `java.util.concurrent.RunnableFuture` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService` | Interface / C# Runtime Trace Contract | Partial | Unit Tested Metadata | Needs Verification | Row schema can carry event ordering and return reasons for future task traces. Java future state transitions, `isDone`, `run`, `get`, `cancel(false)`, exception propagation, and C# task abstraction differences remain unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `CreateCSharpRuntimeTraceReport_WithValidRowsDerivesScenariosAndKeepsRuntimeBlocked` | Unit / trace contract | UOW-1555/UOW-1556 schema-v1 trace shape and UOW-1559 comparison contract | Synthetic C# trace rows carry scenario, phase, reason, stop flags, timestamp flag, and player snapshot; scenarios are derived deterministically. | Deterministic synthetic report assertions. | No live C# trace output and no Java comparison. |
| `CreateCSharpRuntimeTraceReport_RejectsOutOfOrderEventSeq` | Unit / trace contract | Java artifact validator event ordering rule | C# runtime trace rows require strictly increasing `eventSeq`. | Deterministic synthetic report assertions. | No runtime event stream exists. |
| `CreateCSharpRuntimeTraceReport_RejectsTimestampParityKeys` | Unit / trace contract | Java artifact validator timestamp non-parity rule | C# runtime trace rows reject timestamp parity keys. | Deterministic synthetic report assertions. | No runtime clock/timestamp behavior is compared. |
| `Create_WithInvalidCSharpRuntimeTraceRowsKeepsCSharpTraceBlocked` | Unit / comparison contract | UOW-1558 comparison contract and C# row validation | Invalid C# trace rows keep C# runtime trace readiness blocked. | Deterministic synthetic report assertions. | No live C# hooks or comparator exists. |
| Updated `Create_WithSyntheticCSharpRuntimeTraceStillBlocksComparisonExecution` and `Create_WithSyntheticCSharpTraceWithoutLiveHooksStillBlocksCSharpTraceReadiness` | Unit / comparison contract | UOW-1558 comparison contract | Existing comparison contract tests now use row-level C# trace reports. | Deterministic synthetic report assertions. | No live C# trace output exists. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Full game-server test suite is still known to have two unrelated stable failures in `GameServerConnectionInventoryExpansionUseItemTests`: `ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` and `HandleUseItemAsync_ExpExtractMergesRestrictedRewardWithCleanupSealFlag`.
- C# trace rows are synthetic only; no live packet/controller trace emitter exists.
- Shape-valid JSON and synthetic C# trace rows still do not prove Java/C# behavior parity.
- No Java instrumentation, trace serializer, generated artifact, production packet handler stop hook, packet runtime integration, live C# trace emitter, controller task-map owner, delete/logout hook, scheduler callback execution, socket fanout, known-list mutation, live `FutureTask`, live `CM_TELEPORT_ANIMATION_DONE`, live logger capture, live fallback packet serialization, live fallback world spawn, inbound-damage guard, aggro suppression, target/skill rejection, material-skill suppression integration, cooldown date comparison, action `instanceof` ordering comparison, or composition runtime comparison exists.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: no production artifacts; 1 guarded C# runtime trace row contract plus 4 new/updated focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: Java runtime artifact generation, Java trace instrumentation, Java trace serializer, generated Java trace artifacts, production packet handler stop hooks, production packet runtime dispatch, production controller owner wiring, production lifecycle hook, live C# trace emission, comparison execution, live task-map/future execution, live exception fallback execution, live logger capture, socket fanout, known-list mutations, Java/C# future/future-task/concurrent-map/action-packet runtime comparison, scheduler timing/race comparison, caller-origin runtime comparison, delayed teleport no-op/fallback/exception branch runtime comparison, unrelated full-suite cleanup-seal failures
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: add a guarded comparison preflight report that consumes shape-valid Java artifact directory reports and valid C# runtime trace row reports.
- The preflight should check scenario/row-count alignment only.
- Keep actual runtime comparison blocked until generated Java artifacts and live C# trace output exist.

## Suggested Acceptance Criteria

- Preflight report distinguishes missing/invalid Java artifacts, missing/invalid C# trace rows, scenario mismatch, row-count mismatch, and preflight-ready-but-comparison-not-run.
- Tests use synthetic Java directory reports and synthetic C# trace reports only.
- Existing comparison contract and readiness tests continue to pass.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Comparison preflight report | new preflight service/test files | Medium | Safe if it does not edit existing readiness files in the same unit. |
| B | Java/tooling feasibility rerun | read-only tooling/source inspection | Low | Useful only if Java 25/Maven may now be available. |
| C | Inventory cleanup-seal failure triage | inventory item-use tests/services | Medium | Separate full-suite blocker; do not mix with protection comparison files. |
| D | Another Phase 6 runtime prerequisite | separate feature files from `## Next Steps` | Medium | Use if protection metadata work should pause. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add guarded comparison preflight report and docs | new preflight service/test files, progress/handoff docs | Java source writes, production packet runtime |
| Explorer | Read-only Java/tooling feasibility check | read-only Java/tooling inspection | all writes, shared docs, C# edits |

Shared docs must remain orchestrator-owned.

## Do Not Parallelize

- Java generator implementation without Java 25/Maven tooling.
- Production packet handler wiring without generated Java evidence.
- Packet runtime changes.
- Shared scheduler implementation.
- Production protection bridge/adapter execution paths.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1560] Add protection stop trigger C# trace row contract`.
- Files changed in UOW-1560:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AOF-Completion.md`
- Latest prior commits:
  - `0f3fb0a20 [Phase 6][UOW-1559] Integrate protection stop trigger comparison readiness`
  - `127f478cf [Phase 6][UOW-1558] Add protection stop trigger comparison contract`
  - `e8732128e [Phase 6][UOW-1557] Integrate protection stop trigger artifact readiness`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
