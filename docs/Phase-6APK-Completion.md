# Phase 6APK Completion - Protection Validator Player Snapshot Payload Enforcement

Date: 2026-05-28
Unit of Work: UOW-1591
Status: Complete after focused validation.

## Scope

Begin nested-payload enforcement in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` by requiring the schema-v1 player snapshot fields that the serializer contract calls out for protection stop-trigger traces.

This remains a validator-only tightening pass. It does not enable Java observer execution, Java JSON artifact writing, C# trace emission, production protection task-map wiring, or deterministic Java/C# runtime comparison.

## Completed Work

- Continued from `docs/Phase-6APJ-Completion.md`.
- Kept the unit sequential because the selected change touched the shared Java trace artifact validator and its focused tests.
- Added `MissingNestedPayloadField` to `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidationIssueCode`.
- Added player snapshot nested-payload validation for:
  - `objectId`
  - `spawned`
  - `flying`
  - `dead`
  - `protectionActiveBefore`
  - `protectionActiveAfter`
  - `visualStateBefore`
  - `visualStateAfter`
- Preserved timestamp behavior: timestamps remain diagnostic-only and `timestampIsParityKey=true` is still rejected.
- Added a focused regression proving missing `player.visualStateAfter` invalidates schema-v1 artifacts.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 10 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonPreflightReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonKeyProjectionReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 58 tests.
- Full game-server suite was not rerun in this unit; prior handoffs document the unrelated intermittent composition cleanup/seal test that passes in isolation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Player snapshot nested-payload validator enforcement | `PlayerController`, `CreatureVisualState`, packet trace rows | validator service/test | Validation | Sequential | Medium | Selected smallest contract-backed nested payload family. |
| B | Scheduler nested-payload validator enforcement | `CM_TELEPORT_ANIMATION_DONE`, `TeleportService`, `CreatureController` task map | validator service/test | Validation | Later | Medium | Good next family; keep separate from player snapshot enforcement. |
| C | Task cancellation nested-payload validator enforcement | `CreatureController` task map | validator service/test | Validation | Later | Medium | Threading-sensitive `Future.cancel(false)` evidence should be isolated. |
| D | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling/runtime artifact strategy. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Enforce player snapshot nested payload fields in validator | Validation/Test/Docs | validator service/test, progress/handoff docs | Java source writes, runtime readiness API changes, live hooks, item-use files | UOW-1590 readiness surfacing | One tested validator tightening for player snapshot contract fields. |

No sub-agent was spawned for UOW-1591 because the unit touched one shared validator contract and its test only.

## Migration Parity Table - UOW-1591

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Validator now rejects missing player snapshot fields for future attack stop-trigger artifacts. No Java runtime trace exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot contract is enforced for future cast traces; Java skill/cast side effects are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced, but action payload details remain unvalidated nested-payload work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced, but emotion payload details remain blocked until future validator/serializer work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; dialog return-reason evidence remains schema/contract metadata only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced, but movement precision/rounding nested payload remains unvalidated. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; flying movement payload and precision remain future work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; show-dialog payload remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; scheduled item-use packet parity remains separate. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | validator and serializer contract services | Packet Handler / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; scheduler/RunnableFuture nested payload remains next-work candidate. Java `FutureTask` behavior is not runtime-verified. |
| `com.aionemu.gameserver.controllers.PlayerController` | validator and serializer contract services | Controller / Lifecycle Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Validator now requires protection-active before/after and visual-state arrays. `BLINKING`, `SM_PLAYER_STATE`, scheduler, and AI notification behavior remain runtime gaps. |
| `com.aionemu.gameserver.controllers.CreatureController` | validator and serializer contract services | Controller / Task Map Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced, but task cancellation nested payload and `Future.cancel(false)` threading remain unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | validator service | Service / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Player snapshot fields are enforced; teleport scheduler/future artifact fields remain future validator work. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Validator covers JSON trace artifact shape, not byte-level packet serialization. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | validator service | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains unwired; validator does not implement Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | validator service | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; Java serializer implementation remains blocked. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | validator via player/fanout placeholders | Packet / Discovered Dependency | Partial | No direct new tests | Needs Verification | Player visual-state fields are enforced, but fanout nested payload remains blocked until Java serializer exists. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | validator via `visualStateBefore` / `visualStateAfter` | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | Validator now requires visual-state arrays, but enum serialization and `BLINKING` preservation are not Java-runtime verified. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `Validate_AcceptsRepresentativeTeleportSchemaV1ArtifactButKeepsRuntimeComparisonBlocked` | Unit / Metadata | Representative schema-v1 fixture and serializer contract | Existing fixture remains valid when player snapshot nested fields are present. | Focused tests passed. | Fixture is not generated by Java runtime. |
| Added `Validate_RejectsMissingPlayerSnapshotNestedPayloadFields` | Unit / Metadata | Serializer field contract player snapshot fields | Missing `player.visualStateAfter` produces `MissingNestedPayloadField` and blocks schema-v1 validity. | Focused and affected tests passed. | Only player snapshot family is enforced; scheduler/task-map/fanout/action payloads remain future work. |

## Remaining Risks

- Java serializer implementation remains missing.
- Validator still does not enforce scheduler, task cancellation, movement, fanout, AI notify, emotion, action payload, caller origin, or branch-name semantics.
- Field presence is enforced for player snapshots, but field type/semantic validation remains limited.
- Java `ConcurrentHashMap`, `Future.cancel(false)`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.

## Summary Metrics

- Total Java artifacts discovered: 18 grouped rows in this unit
- Total artifacts ported: 1 validator nested-payload enforcement slice plus 1 focused unit test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 18 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, scheduler/task-map runtime comparison, remaining nested-payload validator enforcement
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: enforce scheduler nested-payload fields in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`.
- Why: player snapshot fields are now guarded, and teleport/scheduler payloads are the clearest next contract-backed nested payload family.
- Scope:
  - require scheduler fields only when `scheduler` is an object;
  - keep `scheduler: null` valid for non-scheduler trace rows;
  - include `oldFutureCancelArgument` / `oldFutureCancelResult` as nullable-but-present contract fields;
  - do not attempt to model Java `RunnableFuture` execution semantics beyond field presence in this unit.

## Suggested Acceptance Criteria

- Validator rejects a scheduler object missing a required scheduler field.
- Representative fixture remains valid.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Scheduler nested-payload enforcement | validator service/test | Medium | Preferred next. |
| B | Task cancellation nested-payload enforcement | validator service/test | Medium | Keep separate due threading notes. |
| C | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | Medium | Separate report surface. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Production protection task-map/scheduler wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while hook serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1591] Enforce protection player snapshot fields`.
- Files changed in UOW-1591:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APK-Completion.md`
- Latest prior commits:
  - `436f1f54a [Phase 6][UOW-1590] Surface serializer contract in readiness`
  - `1b5e272ee [Phase 6][UOW-1589] Export serializer contract readiness`
  - `b50bc26c1 [Phase 6][UOW-1588] Surface serializer contract in dashboard`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
