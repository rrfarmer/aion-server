# Phase 6APF Completion - Protection Schema-v1 Serializer Field Contract

Date: 2026-05-27
Unit of Work: UOW-1586
Status: Complete after focused validation.

## Scope

Add a non-live schema-v1 serializer field contract for future protection stop-trigger Java trace artifacts. The contract clarifies what a Java serializer must write before generated artifacts can be accepted for runtime comparison, without implementing Java instrumentation or C# live emitters.

## Completed Work

- Continued from `docs/Phase-6APE-Completion.md`.
- Reviewed the existing broad trace schema report and schema-v1 validator.
- Added `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService`.
- Added field scopes for top-level fields, runtime facts, trace rows, player snapshots, and nested payload placeholders.
- Marked timestamps as diagnostic-only and explicitly non-parity evidence.
- Kept nested payloads such as movement, task cancellation, fanout, AI notify, and scheduler blocked until a Java serializer implementation exists.
- Added focused tests for the field contract, timestamp policy, and blocked nested payloads.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerTraceArtifactSchemaReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests"`.
- First result: one new assertion failed because the scheduler nested-payload note did not explicitly include `RunnableFuture`.
- Updated the serializer contract note to name `RunnableFuture`.
- Reran the same focused schema/validator/contract test set.
- Result: passed 17 tests.
- Ran adjacent readiness/generator tests in parallel with the first rerun.
- First result: failed to build due to a compiler server file lock on `Aion.GameServer.dll`; this was a test-run concurrency issue, not a product failure.
- Reran the adjacent readiness/generator set sequentially:
  `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaObserverRunbookDesignReportServiceTests"`.
- Result: passed 28 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Schema-v1 serializer field contract | protection trace schema and validator artifacts | new serializer field contract service/test | Metadata / Planning | Local-only implementation | Medium | Defines the next missing Java serializer prerequisite without live hooks. |
| B | Validator expansion for nested payloads | schema-v1 artifact validator | validator service/test | Validation | Later | Medium | Should follow after contract fields settle; broader JSON validation could be noisy. |
| C | Readiness dashboard integration of serializer contract | readiness/export services | readiness/export service/test | Integration | Later | Low-Medium | Useful next step after the contract exists. |
| D | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling and needs careful runtime side-effect isolation. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Add non-live serializer field contract and docs | Metadata/Test/Documentation | new serializer field contract service/test, progress/handoff docs | Java source writes, production live hooks, validator nested-payload enforcement, item-use files | existing trace schema and validator | Tested field contract and conservative docs. |

No sub-agent was spawned for UOW-1586. The implementation depended on adjacent local schema/validator files and used a single disjoint write set.

## Migration Parity Table - UOW-1586

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService` | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Contract defines artifact fields that future direct stop caller traces must serialize. No Java artifact writer exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Field contract covers stop-call/return-reason evidence, but Java skill/cast side effects remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Contract leaves action payload details as future nested payload work; composition behavior is not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Emotion payload details remain blocked until Java serializer implementation exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Dialog return-reason fields are represented by the broader trace schema, not live artifacts. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Movement nested payload remains blocked; precision/rounding behavior must be preserved by future Java serializer. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Flying movement nested payload remains blocked; no Java runtime evidence exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Dialog/show-dialog stop evidence remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Serializer Contract | Partial | Unit Tested metadata only | Needs Verification | Use-item stop evidence remains contract-only; scheduled item-use parity remains separate. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService`; validator/schema services | Packet Handler / Teleport Artifact Contract | Partial | Unit Tested metadata only | Needs Verification | Contract includes scheduler placeholder and `RunnableFuture` note. Java `FutureTask` execution and fallback behavior are not runtime-verified. |
| `com.aionemu.gameserver.controllers.PlayerController` | `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService`; trace schema service | Controller / Lifecycle Artifact Contract | Partial | Unit Tested metadata only | Needs Verification | Contract requires player snapshot fields and timestamp non-parity policy. `BLINKING`, `SM_PLAYER_STATE`, scheduler, and AI notification behavior remain runtime gaps. |
| `com.aionemu.gameserver.controllers.CreatureController` | `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService`; trace schema service | Controller / Task Map Artifact Contract | Partial | Unit Tested metadata only | Needs Verification | Task cancellation nested payload is explicitly blocked until Java serializer exists; `ConcurrentHashMap`/`Future.cancel(false)` threading remains unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService` | Service / Teleport Artifact Contract | Partial | Unit Tested metadata only | Needs Verification | Scheduler/future nested payload is a placeholder; no Java runtime artifact exists. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService`; validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Field contract is for JSON trace artifacts, not byte-level packet serialization. Serialized packet bytes are not compared. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | same as above | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains a source dependency; field contract does not wire Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | same as above | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; serializer contract is still blocked on Java implementation. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | serializer field contract via fanout placeholder | Packet / Discovered Dependency | Partial | No direct new tests | Needs Verification | Fanout nested payload is blocked until Java serializer exists; packet bytes are not compared. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | serializer field contract via player snapshot fields | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields require `visualStateBefore/After` string arrays preserving `BLINKING`; Java enum serialization remains future work. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Added `Create_MapsTopLevelRuntimeTraceAndPlayerSnapshotContracts` | Unit / Metadata | Existing schema-v1 validator and trace schema metadata | Top-level, runtime facts, trace row, and player snapshot field contracts exist and preserve schema-v1 ordering. | Focused tests passed. | Does not execute Java or serialize artifacts. |
| Added `Create_DocumentsTimestampPolicyAsDiagnosticOnly` | Unit / Metadata | Trace schema caveat that timestamps are diagnostics only | `wallTimeEpochMillis`, `monotonicNanos`, and `timestampIsParityKey` are diagnostic-only, with `timestampIsParityKey` required false. | Focused tests passed. | Date/time handling is not parity evidence. |
| Added `Create_KeepsNestedPayloadsBlockedUntilJavaSerializerExists` | Unit / Metadata | Trace schema task-map/fanout/scheduler fields | Movement/task cancellation/fanout/AI/scheduler payloads remain blocked until Java serializer exists; `Future.cancel(false)` and `RunnableFuture` risks are explicit. | Focused and adjacent tests passed after sequential rerun. | No Java serializer implementation, runtime artifacts, packet bytes, or threading comparison. |

## Remaining Risks

- Serializer contract is metadata only; Java artifact writing remains unimplemented.
- Validator currently accepts representative schema-v1 shape but does not enforce every nested payload field from the new contract.
- Java `ConcurrentHashMap`, `Future.cancel(false)`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are intentionally diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.
- The unrelated item-use composition cleanup/seal flake remains documented from UOW-1582 through UOW-1584.

## Summary Metrics

- Total Java artifacts discovered: 18 grouped rows in this unit
- Total artifacts ported: 1 serializer field contract service plus 3 focused unit tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 18 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, scheduler/task-map runtime comparison, nested-payload validator enforcement
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: integrate the serializer field contract into readiness/export metadata.
- Why: the field contract exists, but readiness/export reports still speak generally about a missing serializer. The next unit can surface `RequiresJavaSerializerImplementation`, timestamp non-parity policy, and blocked nested payload placeholders without enabling Java/C# live hooks.
- Scope:
  - feed the contract into either generated-artifact execution plan or prerequisite/dashboard summary metadata;
  - keep Java observer execution and C# live emitter disabled;
  - avoid expanding validator enforcement until the contract surface is visible;
  - do not touch production `PlayerController`, scheduler, or task-map wiring.

## Suggested Acceptance Criteria

- One readiness/export report can accept the serializer field contract and expose its blocker flags.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Generated-artifact execution plan serializer-contract integration | generated artifact execution plan service/test | Low-Medium | Good next target because that plan already has a serializer gate. |
| B | Prerequisite dashboard serializer-contract integration | prerequisite dashboard service/test | Low-Medium | Also reasonable; avoid doing both in one unit. |
| C | Validator nested-payload enforcement design | validator service/test | Medium | Defer until metadata surface is visible. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Production protection task-map/scheduler wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while hook serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1586] Add protection serializer field contract`.
- Files changed in UOW-1586:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APF-Completion.md`
- Latest prior commits:
  - `629f4455e [Phase 6][UOW-1585] Export protection hook readiness`
  - `a972ebde7 [Phase 6][UOW-1584] Surface protection hook readiness`
  - `39710a0c8 [Phase 6][UOW-1583] Cover decompose persistence failure`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
