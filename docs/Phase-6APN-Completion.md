# Phase 6APN Completion - Protection Validator Movement Payload Enforcement

Date: 2026-05-28
Unit of Work: UOW-1594
Status: Complete after focused validation.

## Scope

Continue nested-payload enforcement in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` by requiring movement payload fields when a schema-v1 trace row supplies a `movement` object.

This remains a validator-only tightening pass. It does not enable Java observer execution, Java JSON artifact writing, C# trace emission, production movement/controller wiring, or deterministic Java/C# runtime comparison.

## Completed Work

- Continued from `docs/Phase-6APM-Completion.md`.
- Kept implementation sequential because movement, fanout, AI notify, and related tests share the same validator files as the previous nested-payload slices.
- Added movement nested-payload validation for:
  - `oldX`
  - `oldY`
  - `oldZ`
  - `packetX`
  - `packetY`
  - `packetZ`
  - `zDelta`
  - `heading`
  - `movementType`
  - `antiHackAccepted`
  - `teleportationModeAbsoluteMove`
  - `stopThresholdExceeded`
- Kept `movement: null` valid for rows without movement evidence.
- Added a focused regression proving a movement object missing `stopThresholdExceeded` invalidates schema-v1 artifacts.
- Preserved existing player snapshot, scheduler, task-cancellation, and timestamp diagnostics behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 13 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonPreflightReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonKeyProjectionReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 61 tests.
- Full game-server suite was not rerun in this unit; prior handoffs document the unrelated intermittent composition cleanup/seal test that passes in isolation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Movement nested-payload validator enforcement | `CM_MOVE`, `CM_MOVE_IN_AIR`, `PlayerMoveController`, `AntiHackService` | validator service/test | Validation | Sequential | Medium | Selected; one shared validator and fixture file. |
| B | Fanout nested-payload validator enforcement | `PacketSendUtility`, `SM_PLAYER_STATE`, protection stop fanout rows | validator service/test | Validation | Later | Medium | Same files as A, so keep separate. |
| C | AI notify nested-payload validator enforcement | `PlayerController.notifyAIOnMove` and controller move callbacks | validator service/test | Validation | Later | Medium | Same validator files; keep separate. |
| D | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | emitter design service/test | Metadata Integration | Possible later | Medium | Separate files, but less direct than validator contract closure. |
| E | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling/runtime artifact strategy. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Enforce movement nested payload fields in validator | Validation/Test/Docs | validator service/test, progress/handoff docs | Java source writes, fanout/AI validation, runtime readiness API changes, live hooks, item-use files | UOW-1593 task-cancellation enforcement | One tested validator tightening for movement contract fields. |

No sub-agent was spawned for UOW-1594 because the unit touched one shared validator contract and its test only.

## Migration Parity Table - UOW-1594

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field enforcement is orthogonal to attack payload semantics; attack stop-trigger artifacts still need Java runtime traces. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field enforcement is orthogonal to cast behavior; Java skill/cast side effects are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field enforcement is orthogonal to composite-stone behavior; action payload details remain unvalidated nested-payload work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement fields are enforced when supplied, but emotion payload details remain future validator/serializer work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement fields are enforced when supplied; dialog return-reason evidence remains schema/contract metadata only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Validator now requires movement position, z-delta, heading, movement type, anti-hack decision, teleportation-mode, and threshold fields when a movement object exists. Java float precision, asymmetric `z + 0.5f` comparison, and world-position updates are not runtime-verified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field presence is enforced when supplied; flying movement distance handling and controller side effects remain Java-runtime gaps. |
| `com.aionemu.gameserver.controllers.movement.PlayerMoveController` | same as above | Controller / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | Movement controller target/vector/jumping updates are represented only as required trace fields; live C# movement behavior was not compared. |
| `com.aionemu.gameserver.services.antihack.AntiHackService` | same as above | Service / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | Validator requires `antiHackAccepted` when movement payload exists, but Java anti-hack decision logic is not ported or executed. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field enforcement is orthogonal to show-dialog behavior; payload remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement field enforcement is orthogonal to item-use scheduler behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | validator and serializer contract services | Packet Handler / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Movement validation keeps `movement: null` valid for teleport animation rows; scheduler/RunnableFuture semantics remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController` | validator and serializer contract services | Controller / Lifecycle Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Future protection-stop traces now require movement payload fields when present; fanout/AI notification side effects remain unvalidated. |
| `com.aionemu.gameserver.controllers.CreatureController` | validator and serializer contract services | Controller / Task Map Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | No new task-map behavior; task cancellation remains field-presence-only and Java `ConcurrentHashMap` / `Future.cancel(false)` behavior is unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | validator service | Service / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Teleport movement metadata fields are required only when a movement object exists; teleport scheduler/future fields remain metadata-only. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | validator service | Scheduler Utility / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | No new scheduler semantics; `ScheduledThreadPoolExecutor` behavior is not modeled. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Validator covers JSON trace artifact shape, not byte-level packet serialization. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | validator service | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains unwired; validator does not implement Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | validator service | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; Java serializer implementation remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | validator via player/fanout placeholders | Packet / Discovered Dependency | Partial | No direct new tests | Needs Verification | Movement validation does not cover fanout; packet bytes are not compared. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | validator via player snapshot fields | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | No new enum behavior; visual state arrays remain metadata-only and Java-runtime unverified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `Validate_AcceptsRepresentativeTeleportSchemaV1ArtifactButKeepsRuntimeComparisonBlocked` | Unit / Metadata | Representative schema-v1 fixture and serializer contract | Existing fixture remains valid with `movement: null` on rows without movement evidence. | Focused and affected tests passed. | Fixture is not generated by Java runtime. |
| Added `Validate_RejectsMissingMovementNestedPayloadFieldsWhenMovementIsPresent` | Unit / Metadata | Java `CM_MOVE`, `CM_MOVE_IN_AIR`, `PlayerMoveController`, and serializer contract fields | Missing `movement.stopThresholdExceeded` produces `MissingNestedPayloadField` and blocks schema-v1 validity. | Focused and affected tests passed. | Field presence only; Java movement math, anti-hack decisions, float precision/rounding, and controller side effects remain unverified. |

## Remaining Risks

- Java serializer implementation remains missing.
- Validator still does not enforce fanout, AI notify, emotion, action payload, caller origin, or branch-name semantics.
- Movement field presence is enforced only when `movement` is an object; semantic/type validation remains limited.
- Java float precision/rounding, `z + 0.5f` threshold behavior, anti-hack decisions, `PlayerMoveController` state changes, and air-movement distance handling remain runtime-sensitive and unverified.
- Java `ConcurrentHashMap`, `Future.cancel(false)`, `ScheduledThreadPoolExecutor`, `RunnableWrapper`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.

## Summary Metrics

- Total Java artifacts discovered: 21 grouped rows in this unit
- Total artifacts ported: 1 movement nested-payload validator slice plus 1 focused unit test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 21 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, movement runtime comparison, anti-hack runtime comparison, remaining nested-payload validator enforcement
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: enforce fanout nested-payload fields in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`.
- Why: player snapshot, scheduler, task-cancellation, and movement field presence are now guarded; fanout payload fields are the next high-value controller/packet side-effect family for `SM_PLAYER_STATE` and sighted-player broadcasts.
- Scope:
  - require fanout fields only when `fanout` is an object;
  - keep `fanout: null` valid for rows without fanout evidence;
  - use schema fixture fields such as packet name, include-self flag, recipient count, and known-list ordering diagnostics when present;
  - do not claim byte-level packet or recipient-order parity without Java-generated runtime artifacts.

## Suggested Acceptance Criteria

- Validator rejects a fanout object missing a required field.
- Representative fixture remains valid.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Fanout nested-payload enforcement | validator service/test | Medium | Preferred next. |
| B | AI notify nested-payload enforcement | validator service/test | Medium | Same files as A, so keep separate. |
| C | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | Medium | Separate report surface. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Multiple validator nested-payload families in one unit.
- Production protection task-map/scheduler/movement wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while protection serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1594] Enforce protection movement fields`.
- Files changed in UOW-1594:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APN-Completion.md`
- Latest prior commits:
  - `57ea7ae55 [Phase 6][UOW-1593] Enforce protection task-cancel fields`
  - `556404856 [Phase 6][UOW-1592] Enforce protection scheduler fields`
  - `43365684d [Phase 6][UOW-1591] Enforce protection player snapshot fields`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
