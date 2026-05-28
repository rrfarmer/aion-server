# Phase 6APG Completion - Protection Serializer Contract Execution Plan Integration

Date: 2026-05-28
Unit of Work: UOW-1587
Status: Complete after focused validation.

## Scope

Surface the UOW-1586 protection schema-v1 serializer field contract in the generated-artifact execution plan. This remains non-live metadata: no Java observer, Java serializer, C# runtime trace emitter, or production protection task-map wiring was enabled.

## Completed Work

- Re-read required migration docs, Phase 6 progress, and latest Phase 6 handoff.
- Confirmed `docs/commit-conventions.md` is still missing.
- Closed the old completed Explorer sub-agent from UOW-1584.
- Performed Parallel Work Discovery across generated-artifact plan integration, prerequisite dashboard integration, validator nested-payload enforcement, and Java serializer implementation.
- Updated `PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanService.Create` with an optional `PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractReport`.
- Added execution-plan report flags for:
  - `HasSerializerFieldContract`
  - `SerializerFieldContractRowCount`
  - `HasSerializerTimestampNonParityPolicy`
  - `HasSerializerNestedPayloadPlaceholders`
  - `NeedsJavaSerializerImplementation`
- Updated the Java trace serializer gate evidence/notes to expose contract rows, timestamp non-parity policy, nested payload placeholders, and remaining Java serializer blocker.
- Added a focused regression covering both default no-contract behavior and contract-present behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 8 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaObserverRunbookDesignReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 32 tests.
- Full game-server suite was not rerun in this unit; prior handoffs document the unrelated intermittent composition cleanup/seal test that passes in isolation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Generated-artifact plan serializer-contract integration | stop-trigger packet/controller/observer schema artifacts | generated-artifact execution plan service/test | Metadata Integration | Sequential | Low-Medium | Best next target; the plan already has the serializer gate and can accept contract metadata cleanly. |
| B | Prerequisite dashboard serializer-contract integration | same artifacts | prerequisite dashboard service/test | Metadata Integration | Later | Low-Medium | Separate report surface; avoid changing two readiness surfaces in one unit. |
| C | Validator nested payload enforcement | Java trace artifact validator | validator service/test | Validation | Later | Medium | Broader JSON validation behavior; should follow after contract surface is visible. |
| D | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling/runtime artifact strategy. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Integrate serializer field contract into generated-artifact execution plan | Metadata/Test/Docs | generated-artifact execution plan service/test, progress/handoff docs | Java source writes, validator enforcement, prerequisite dashboard, item-use files | UOW-1586 field contract | One tested execution-plan surface for contract blockers. |

No sub-agent was spawned for UOW-1587 because the unit touched a shared report contract and its test only.

## Migration Parity Table - UOW-1587

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanService` | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Execution plan now surfaces serializer field-contract blockers for future direct stop caller artifacts. No Java runtime trace exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Stop-call serializer evidence remains contract-only; Java skill/cast side effects are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Action payload details remain nested-payload work; unrelated composition cleanup/seal flake remains outside this unit. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Emotion payload details remain blocked until Java serializer implementation exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Dialog return-reason evidence remains schema/contract metadata only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Movement nested payload remains blocked; precision/rounding behavior still must be preserved by a future Java serializer. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Flying movement nested payload remains blocked. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Show-dialog stop evidence remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Execution Plan Metadata | Partial | Unit Tested metadata only | Needs Verification | Use-item stop evidence remains contract-only; scheduled item-use packet parity remains separate. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanService`; serializer contract service | Packet Handler / Teleport Artifact Plan | Partial | Unit Tested metadata only | Needs Verification | Execution plan surfaces scheduler/RunnableFuture serializer contract blockers. Java `FutureTask` execution and fallback behavior are not runtime-verified. |
| `com.aionemu.gameserver.controllers.PlayerController` | generated-artifact execution plan and serializer contract services | Controller / Lifecycle Artifact Plan | Partial | Unit Tested metadata only | Needs Verification | Plan surfaces player snapshot and timestamp policy blockers. `BLINKING`, `SM_PLAYER_STATE`, scheduler, and AI notification behavior remain runtime gaps. |
| `com.aionemu.gameserver.controllers.CreatureController` | generated-artifact execution plan and serializer contract services | Controller / Task Map Artifact Plan | Partial | Unit Tested metadata only | Needs Verification | Task cancellation nested payload remains blocked; `ConcurrentHashMap`/`Future.cancel(false)` threading remains unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | generated-artifact execution plan service | Service / Teleport Artifact Plan | Partial | Unit Tested metadata only | Needs Verification | Teleport scheduler/future artifact fields remain future serializer work. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | generated-artifact execution plan service; validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Execution plan covers JSON trace artifact serializer blockers, not byte-level packet serialization. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | generated-artifact execution plan service | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains unwired; execution plan metadata does not implement Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | generated-artifact execution plan service | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; Java serializer implementation remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | generated-artifact execution plan via fanout placeholder | Packet / Discovered Dependency | Partial | No direct new tests | Needs Verification | Fanout nested payload remains blocked until Java serializer exists; packet bytes are not compared. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | generated-artifact execution plan via player snapshot fields | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | `visualStateBefore/After` contract and `BLINKING` dependency are surfaced only as metadata. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `Create_SequencesAllRuntimeComparisonExecutionGates` | Unit / Metadata | Existing generated-artifact execution plan and trace schema | Default execution-plan callers remain no-contract and still mark Java serializer implementation as needed. | Focused tests passed. | Does not execute Java or serialize artifacts. |
| Added `Create_WithSerializerFieldContractSurfacesTimestampAndNestedPayloadBlockers` | Unit / Metadata | UOW-1586 serializer field contract, trace schema metadata | Execution plan surfaces contract row count, timestamp non-parity policy, nested-payload placeholders, and Java serializer implementation blocker. | Focused and affected tests passed. | Metadata only; no Java serializer implementation, runtime artifacts, packet bytes, or threading comparison. |

## Remaining Risks

- Java serializer implementation remains missing.
- Validator still does not enforce every nested payload field from the field contract.
- Java `ConcurrentHashMap`, `Future.cancel(false)`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.
- Prerequisite dashboard does not yet surface the serializer field contract directly.

## Summary Metrics

- Total Java artifacts discovered: 18 grouped rows in this unit
- Total artifacts ported: 1 execution-plan metadata integration plus 1 focused unit test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 18 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, scheduler/task-map runtime comparison, nested-payload validator enforcement, prerequisite dashboard serializer-contract surfacing
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: surface the serializer field contract in the prerequisite dashboard or summary export.
- Why: generated-artifact execution planning now exposes the contract, but the top-level dashboard still reports serializer readiness through generic artifact/tooling blockers.
- Scope:
  - choose one report surface, preferably prerequisite dashboard first;
  - pass contract metadata through without enabling Java/C# live hooks;
  - keep validator nested-payload enforcement deferred;
  - avoid production `PlayerController`, scheduler, task-map, and item-use files.

## Suggested Acceptance Criteria

- One dashboard/export report accepts the serializer field contract and exposes timestamp policy, nested payload placeholders, and Java serializer blocker flags.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Prerequisite dashboard serializer-contract integration | prerequisite dashboard service/test | Low-Medium | Preferred next. |
| B | Summary export serializer-contract integration | summary export service/test | Low-Medium | Do after dashboard or as a separate isolated unit. |
| C | Validator nested-payload enforcement design | validator service/test | Medium | Defer until report surfaces are complete. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Production protection task-map/scheduler wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while hook serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1587] Surface serializer contract in artifact plan`.
- Files changed in UOW-1587:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerGeneratedArtifactExecutionPlanServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APG-Completion.md`
- Latest prior commits:
  - `ba946d028 [Phase 6][UOW-1586] Add protection serializer field contract`
  - `629f4455e [Phase 6][UOW-1585] Export protection hook readiness`
  - `a972ebde7 [Phase 6][UOW-1584] Surface protection hook readiness`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
