# Phase 6APM Completion - Protection Validator Task-Cancellation Payload Enforcement

Date: 2026-05-28
Unit of Work: UOW-1593
Status: Complete after focused validation.

## Scope

Continue nested-payload enforcement in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` by requiring task-cancellation payload fields when a schema-v1 trace row supplies a `taskCancellation` object.

This remains a validator-only tightening pass. It does not enable Java observer execution, Java JSON artifact writing, C# trace emission, production protection task-map wiring, or deterministic Java/C# runtime comparison.

## Completed Work

- Continued from `docs/Phase-6APL-Completion.md`.
- Kept implementation sequential because task-cancellation validation and related tests share the same validator files as the previous nested-payload slices.
- Added task-cancellation nested-payload validation for:
  - `taskIdName`
  - `taskIdOrdinal`
  - `taskPresentBeforeCancel`
  - `taskRemovedBeforeCancel`
  - `futureCancelArgument`
  - `futureCancelResult`
  - `scheduledDelayMillis`
  - `stopOrigin`
- Kept `taskCancellation: null` valid for rows without task-map evidence.
- Added a focused regression proving a task-cancellation object missing `futureCancelResult` invalidates schema-v1 artifacts.
- Preserved existing player snapshot, scheduler, and timestamp diagnostics behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 12 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonPreflightReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonKeyProjectionReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 60 tests.
- Full game-server suite was not rerun in this unit; prior handoffs document the unrelated intermittent composition cleanup/seal test that passes in isolation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Task-cancellation nested-payload validator enforcement | `CreatureController.cancelTask*`, protection stop-trigger rows | validator service/test | Validation | Sequential | Medium | Selected; one shared validator and fixture file. |
| B | Movement nested-payload validator enforcement | `CM_MOVE`, `CM_MOVE_IN_AIR`, anti-hack/movement rows | validator service/test | Validation | Later | Medium | Same validator files; keep separate. |
| C | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | emitter design service/test | Metadata Integration | Possible later | Medium | Separate files, but less direct than validator contract closure. |
| D | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling/runtime artifact strategy. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Enforce task-cancellation nested payload fields in validator | Validation/Test/Docs | validator service/test, progress/handoff docs | Java source writes, movement validation, runtime readiness API changes, live hooks, item-use files | UOW-1592 scheduler enforcement | One tested validator tightening for task-cancellation contract fields. |

No sub-agent was spawned for UOW-1593 because the unit touched one shared validator contract and its test only.

## Migration Parity Table - UOW-1593

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied, but attack stop-trigger artifacts still need Java runtime traces. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; Java skill/cast side effects are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; action payload details remain unvalidated nested-payload work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; emotion payload details remain future validator/serializer work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; dialog return-reason evidence remains schema/contract metadata only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; movement precision/rounding nested payload remains unvalidated. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; flying movement payload and precision remain future work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; show-dialog payload remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation field presence is enforced when supplied; scheduled item-use packet parity remains separate. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | validator and serializer contract services | Packet Handler / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Task-cancellation validation is mostly orthogonal to teleport animation done; scheduler/RunnableFuture semantics remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController` | validator and serializer contract services | Controller / Lifecycle Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Future stopProtectionActiveTask traces now require task-cancellation fields when present; live scheduler callback execution remains disabled. |
| `com.aionemu.gameserver.controllers.CreatureController` | validator and serializer contract services | Controller / Task Map Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Validator now requires task id, remove-before-cancel, `Future.cancel(false)`, scheduled delay, and origin fields when a task-cancellation payload exists. Java `ConcurrentHashMap` and Future behavior are not runtime-verified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | validator service | Service / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | No new teleport side-effect parity; teleport scheduler/future fields remain metadata-only. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | validator service | Scheduler Utility / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | No new scheduler semantics; `ScheduledThreadPoolExecutor` behavior is not modeled. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Validator covers JSON trace artifact shape, not byte-level packet serialization. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | validator service | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains unwired; validator does not implement Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | validator service | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; Java serializer implementation remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | validator via player/fanout placeholders | Packet / Discovered Dependency | Partial | No direct new tests | Needs Verification | Task-cancellation validation does not cover fanout; packet bytes are not compared. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | validator via player snapshot fields | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | No new enum behavior; visual state arrays remain metadata-only and Java-runtime unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `Validate_AcceptsRepresentativeTeleportSchemaV1ArtifactButKeepsRuntimeComparisonBlocked` | Unit / Metadata | Representative schema-v1 fixture and serializer contract | Existing fixture remains valid with `taskCancellation: null` on rows without task-map evidence. | Focused tests passed. | Fixture is not generated by Java runtime. |
| Added `Validate_RejectsMissingTaskCancellationNestedPayloadFieldsWhenTaskCancellationIsPresent` | Unit / Metadata | Java `CreatureController.cancelTask`, `Future.cancel(false)`, and serializer contract | Missing `taskCancellation.futureCancelResult` produces `MissingNestedPayloadField` and blocks schema-v1 validity. | Focused and affected tests passed. | Field presence only; Java `ConcurrentHashMap.remove`, `Future.cancel(false)`, and race behavior remain unverified. |

## Remaining Risks

- Java serializer implementation remains missing.
- Validator still does not enforce movement, fanout, AI notify, emotion, action payload, caller origin, or branch-name semantics.
- Task-cancellation field presence is enforced only when `taskCancellation` is an object; semantic/type validation remains limited.
- Java `ConcurrentHashMap`, `Future.cancel(false)`, `ScheduledThreadPoolExecutor`, `RunnableWrapper`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.

## Summary Metrics

- Total Java artifacts discovered: 19 grouped rows in this unit
- Total artifacts ported: 1 task-cancellation nested-payload validator slice plus 1 focused unit test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 19 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, task-map runtime comparison, remaining nested-payload validator enforcement
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: enforce movement nested-payload fields in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`.
- Why: player snapshot, scheduler, and task-cancellation field presence are now guarded; movement payload fields are the next high-value packet-origin payload family for `CM_MOVE` / `CM_MOVE_IN_AIR`.
- Scope:
  - require movement fields only when `movement` is an object;
  - keep `movement: null` valid for non-movement rows;
  - include x/y/z/current vs packet position fields and anti-hack decision fields if present in the schema fixtures;
  - do not claim precision/rounding parity without Java-generated runtime artifacts.

## Suggested Acceptance Criteria

- Validator rejects a movement object missing a required field.
- Representative fixture remains valid.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Movement nested-payload enforcement | validator service/test | Medium | Preferred next. |
| B | Fanout nested-payload enforcement | validator service/test | Medium | Same files as A, so keep separate. |
| C | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | Medium | Separate report surface. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Multiple validator nested-payload families in one unit.
- Production protection task-map/scheduler wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while hook serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1593] Enforce protection task-cancel fields`.
- Files changed in UOW-1593:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APM-Completion.md`
- Latest prior commits:
  - `556404856 [Phase 6][UOW-1592] Enforce protection scheduler fields`
  - `43365684d [Phase 6][UOW-1591] Enforce protection player snapshot fields`
  - `436f1f54a [Phase 6][UOW-1590] Surface serializer contract in readiness`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
