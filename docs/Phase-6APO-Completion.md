# Phase 6APO Completion - Protection Validator Fanout Payload Enforcement

Date: 2026-05-28
Unit of Work: UOW-1595
Status: Complete after focused validation.

## Scope

Continue nested-payload enforcement in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` by requiring fanout payload fields when a schema-v1 trace row supplies a `fanout` object.

This remains a validator-only tightening pass. It does not enable Java observer execution, Java JSON artifact writing, C# trace emission, production fanout wiring, live socket sends, or deterministic Java/C# runtime comparison.

## Completed Work

- Continued from `docs/Phase-6APN-Completion.md`.
- Kept implementation sequential because fanout, AI notify, and related tests share the same validator files as the previous nested-payload slices.
- Added fanout nested-payload validation for:
  - `packetName`
  - `includeSelf`
  - `recipientCount`
  - `knownListOrderIsParityKey`
- Kept `fanout: null` valid for rows without packet fanout evidence.
- Added a focused regression proving a fanout object missing `knownListOrderIsParityKey` invalidates schema-v1 artifacts.
- Preserved existing player snapshot, scheduler, task-cancellation, movement, and timestamp diagnostics behavior.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 14 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonPreflightReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonKeyProjectionReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"`.
- Result: passed 62 tests.
- Full game-server suite was not rerun in this unit; prior handoffs document the unrelated intermittent composition cleanup/seal test that passes in isolation.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | Fanout nested-payload validator enforcement | `PlayerController`, `PacketSendUtility`, `SM_PLAYER_STATE`, known-list fanout rows | validator service/test | Validation | Sequential | Medium | Selected; one shared validator and fixture file. |
| B | AI notify nested-payload validator enforcement | `PlayerController.notifyAIOnMove` and move callbacks | validator service/test | Validation | Later | Medium | Same files as A, so keep separate. |
| C | Emotion/action payload validator enforcement | `CM_EMOTION`, action packet branches | validator service/test | Validation | Later | Medium | Same files and broader branch surface; keep separate. |
| D | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | emitter design service/test | Metadata Integration | Possible later | Medium | Separate files, but less direct than validator contract closure. |
| E | Java serializer implementation | Java observer/serializer files | Java source | Live Artifact Generation | No | High | Still blocked by Java tooling/runtime artifact strategy. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Orchestrator | Enforce fanout nested payload fields in validator | Validation/Test/Docs | validator service/test, progress/handoff docs | Java source writes, AI/emotion/action validation, runtime readiness API changes, live hooks, item-use files | UOW-1594 movement enforcement | One tested validator tightening for fanout contract fields. |

No sub-agent was spawned for UOW-1595 because the unit touched one shared validator contract and its test only.

## Migration Parity Table - UOW-1595

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout field enforcement is orthogonal to attack payload semantics; attack stop-trigger artifacts still need Java runtime traces. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout field enforcement is orthogonal to cast behavior; Java skill/cast side effects are not runtime-compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout field enforcement is orthogonal to composite-stone behavior; action payload details remain unvalidated nested-payload work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout fields are enforced when supplied, but emotion payload details remain future validator/serializer work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout fields are enforced when supplied; dialog return-reason evidence remains schema/contract metadata only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout fields are enforced when supplied; movement math, anti-hack decisions, and float precision remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout fields are enforced when supplied; flying movement distance handling remains Java-runtime work. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout field enforcement is orthogonal to show-dialog behavior; payload remains contract-only. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | same as above | Packet Handler / Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout field enforcement is orthogonal to item-use scheduler behavior. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | validator and serializer contract services | Packet Handler / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Fanout validation keeps `fanout: null` valid for rows without packet fanout evidence; teleport fallback fanout still needs generated runtime traces. |
| `com.aionemu.gameserver.controllers.PlayerController` | validator and serializer contract services | Controller / Lifecycle Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Future protection start/stop traces now require fanout packet metadata when present. AI notification side effects remain unvalidated. |
| `com.aionemu.gameserver.utils.PacketSendUtility` | validator service | Utility / Fanout Dependency | Partial | Unit Tested metadata only | Needs Verification | Validator requires packet name, include-self flag, recipient count, and known-list order diagnostic. Java `KnownList.forEachPlayer` ordering and live socket sends are not runtime-verified. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE` | validator via fanout payload | Packet / Fanout Dependency | Partial | Unit Tested metadata only | Needs Verification | Validator requires fanout packet name but does not compare bytes for object id, visual state, see state, or blinking flag. |
| `com.aionemu.gameserver.controllers.CreatureController` | validator and serializer contract services | Controller / Task Map Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | No new task-map behavior; task cancellation remains field-presence-only and Java `ConcurrentHashMap` / `Future.cancel(false)` behavior is unverified. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | validator service | Service / Teleport Artifact Validator | Partial | Unit Tested metadata only | Needs Verification | Teleport fanout metadata fields are required only when a fanout object exists; fallback spawn side effects remain metadata-only. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` | validator service | Scheduler Utility / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | No new scheduler semantics; `ScheduledThreadPoolExecutor` behavior is not modeled. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | validator service | Packet Serialization | Partial | Unit Tested metadata only | Needs Verification | Validator covers JSON trace artifact shape, not byte-level packet serialization. |
| `com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver` | validator service | Observer Interface | Partial | Unit Tested metadata only | Needs Verification | Observer remains unwired; validator does not implement Java callbacks. |
| `com.aionemu.gameserver.network.aion.capture.NoOpServerPacketCaptureObserver` | validator service | Observer Implementation | Partial | Unit Tested metadata only | Needs Verification | Default Java observer remains no-op; Java serializer implementation remains blocked. |
| `com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState` | validator via player snapshot/fanout fields | Enum / Discovered Dependency | Partial | Unit Tested metadata only | Needs Verification | Fanout enforcement references visual-state packet fanout but does not verify `BLINKING` byte serialization. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `Validate_AcceptsRepresentativeTeleportSchemaV1ArtifactButKeepsRuntimeComparisonBlocked` | Unit / Metadata | Representative schema-v1 fixture and serializer contract | Existing fixture remains valid with `fanout: null` on rows without fanout evidence. | Focused and affected tests passed. | Fixture is not generated by Java runtime. |
| Added `Validate_RejectsMissingFanoutNestedPayloadFieldsWhenFanoutIsPresent` | Unit / Metadata | Java `PlayerController.startProtectionActiveTask`, `PlayerController.stopProtectionActiveTask`, `PacketSendUtility.broadcastToSightedPlayers`, and serializer contract fields | Missing `fanout.knownListOrderIsParityKey` produces `MissingNestedPayloadField` and blocks schema-v1 validity. | Focused and affected tests passed. | Field presence only; Java known-list membership/order, socket fanout, packet bytes, and recipient selection remain unverified. |

## Remaining Risks

- Java serializer implementation remains missing.
- Validator still does not enforce AI notify, emotion, action payload, caller origin, or branch-name semantics.
- Fanout field presence is enforced only when `fanout` is an object; semantic/type validation remains limited.
- Java known-list membership/order, `PacketSendUtility.broadcastPacket`, live socket writes, `SM_PLAYER_STATE` byte serialization, and recipient selection remain runtime-sensitive and unverified.
- Java movement precision, anti-hack decisions, `Future.cancel(false)`, `ScheduledThreadPoolExecutor`, `RunnableWrapper`, and `RunnableFuture` behavior remains runtime-sensitive and unverified.
- Timestamp fields are diagnostic-only; no date/time parity claim exists.
- Java 25 JDK/Maven blocker still prevents generated Java observer/runtime packet artifacts.

## Summary Metrics

- Total Java artifacts discovered: 20 grouped rows in this unit
- Total artifacts ported: 1 fanout nested-payload validator slice plus 1 focused unit test
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 20 grouped Java metadata/dependency rows
- Total blocked artifacts: Java protection schema-v1 serializer implementation, Java protection observer implementation, Java runtime packet/trace generation, Java 25 JDK, Java compiler, Maven, Maven wrapper, fanout runtime comparison, packet byte comparison, remaining nested-payload validator enforcement
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: enforce AI notify nested-payload fields in `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`.
- Why: player snapshot, scheduler, task-cancellation, movement, and fanout field presence are now guarded; AI notification payload fields are the next controller side-effect family after `PlayerController.stopProtectionActiveTask`.
- Scope:
  - require AI notify fields only when `aiNotify` is an object;
  - keep `aiNotify: null` valid for rows without AI notification evidence;
  - use schema fixture fields for notify method, queued/executed flags, target object id, and ordering diagnostics when present;
  - do not claim AI runtime parity without Java-generated runtime artifacts.

## Suggested Acceptance Criteria

- Validator rejects an AI notify object missing a required field.
- Representative fixture remains valid.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- No verified parity is claimed.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | AI notify nested-payload enforcement | validator service/test | Medium | Preferred next. |
| B | Emotion/action payload enforcement | validator service/test | Medium | Same files as A, so keep separate. |
| C | C# trace emitter serializer-contract surfacing | C# trace emitter design service/test | Medium | Separate report surface. |
| D | Java serializer implementation | Java observer/serializer files | High | Defer until Java tooling is available. |

## Do Not Parallelize

- Progress/handoff doc writes.
- Multiple validator nested-payload families in one unit.
- Production protection task-map/scheduler/movement/fanout wiring with serializer metadata work.
- Java observer/runtime implementation without Java 25 JDK and Maven.
- Item-use test edits while protection serializer work is active.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1595] Enforce protection fanout fields`.
- Files changed in UOW-1595:
  - `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6APO-Completion.md`
- Latest prior commits:
  - `16224a685 [Phase 6][UOW-1594] Enforce protection movement fields`
  - `57ea7ae55 [Phase 6][UOW-1593] Enforce protection task-cancel fields`
  - `556404856 [Phase 6][UOW-1592] Enforce protection scheduler fields`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
