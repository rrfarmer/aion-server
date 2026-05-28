# Phase 6APR Completion - Protection Validator Caller And Branch Metadata Enforcement

Date: 2026-05-28
Unit of Work: UOW-1598
Status: Complete after focused validation

## Scope

This unit closed another validator-side schema gap for player protection stop-trigger traces. The C# validator now requires trace-level `actionBranchName`, and when `callerOrigin` is present as an object it requires the nested caller-origin payload fields needed to reason about level-ready, teleport, Beritra portal, and teleport-animation caller ordering.

This is still a validator/reporting slice only. It does not implement the Java serializer, wire the live C# emitter, compare packet bytes, or prove runtime parity.

## Completed Work

- Added required trace-level `actionBranchName` validation to `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`.
- Added conditional nested `callerOrigin` payload enforcement for:
  - `callerName`
  - `callerClass`
  - `callerMethod`
  - `callerSourceFile`
  - `callerLine`
  - `startProtectionLine`
  - `startsProtectionBeforeWorldSpawn`
  - `worldSpawnLine`
  - `spawnedBeforeStart`
  - `ordering`
- Added focused validator regression coverage for missing `actionBranchName`.
- Added focused validator regression coverage for missing nested `callerOrigin.ordering`.
- Updated the directory-report representative artifact fixture so it remains shape-valid under the stricter validator.
- Updated `docs/PHASE-6-PROGRESS.md` with the session record, parity table, risks, metrics, and next recommended unit.

## Parallel Work Discovery

| Candidate | Files / Area | Risk | Selected | Notes |
| --- | --- | --- | --- | --- |
| Caller-origin and branch-name validator enforcement | `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService`, validator tests, representative report fixture | Medium | Yes | Adds closed schema checks without assuming generated Java trace content. |
| C# trace emitter / serializer contract surfacing | Readiness or design reporting services and tests | Medium | No | Good next unit after the stricter validator is committed. |
| Java serializer implementation | Java-side instrumentation, fixture generation, runtime artifacts | High | No | Still blocked by Java runtime/build environment and larger integration risk. |

Selected batch: orchestrator-only. The validator, representative fixture, progress doc, and handoff doc share the same behavioral contract and were kept in one sequential unit.

## Validation

Focused validator/serializer contract tests initially failed to compile because the new raw-string replacement in the test did not satisfy C# raw-string indentation rules. The test was corrected to use an ordinary escaped search string and a tab-aligned raw-string replacement.

Focused rerun passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"
```

Result: 19 passed, 0 failed.

The first affected-suite rerun then failed because `PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests.Create_ValidArtifactReportsShapeValidButNotRuntimeReady` used a representative artifact fixture that did not yet include `actionBranchName`. The fixture was updated to include the same branch-name metadata the validator now requires.

Affected rerun passed:

```powershell
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonContractServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonPreflightReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonKeyProjectionReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportServiceTests|FullyQualifiedName~PlayerProtectionActiveTaskStopTriggerJavaTraceSerializerFieldContractServiceTests"
```

Result: 67 passed, 0 failed.

Full suite was not rerun for this unit.

## Tests Added Or Updated

| Test | Change | Validates | Java Comparison |
| --- | --- | --- | --- |
| `Validate_RequiresTraceActionBranchName` | Added | Validator rejects traces missing required `actionBranchName`. | No generated Java artifact yet; schema contract only. |
| `Validate_RejectsMissingCallerOriginNestedPayloadFieldsWhenCallerOriginIsPresent` | Added | Validator rejects incomplete nested `callerOrigin` payloads, including missing `ordering`. | No generated Java artifact yet; schema contract only. |
| `Create_ValidArtifactReportsShapeValidButNotRuntimeReady` | Updated fixture | Directory report representative artifact remains shape-valid under the stricter validator. | No runtime Java comparison; readiness remains false. |

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` schema contract for `callerOrigin` | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Java is source of truth. Validator now requires caller-origin fields when present, but no generated Java trace confirms exact caller values or ordering. |
| `com.aionemu.gameserver.services.teleport.TeleportService` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` schema contract for `callerOrigin` | Service / Validator Contract | Partial | Unit Tested | Needs Verification | Teleport caller breadcrumb metadata is required structurally, but runtime behavior and exact Java source line mapping are unverified. |
| `ai.instance.beritra.BeritraPortalAI` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` schema contract for `callerOrigin` | AI / Validator Contract | Partial | Unit Tested | Needs Verification | Beritra portal caller metadata can now be rejected if incomplete. Generated Java evidence is still missing. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_TELEPORT_ANIMATION_DONE` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` schema contract for `callerOrigin` | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Teleport animation caller ordering fields are required when caller origin exists. Runtime comparison is not implemented. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_ATTACK` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | `actionBranchName` is required for action-trigger traces, but branch-name values are not allow-listed until generated Java evidence exists. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_CASTSPELL` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Spell action branch metadata is structurally required only. Cast validation, cooldowns, and Java runtime side effects remain outside this unit. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_COMPOSITE_STONES` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Composite-stone action branch metadata is structurally required only. Packet handling parity is unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_EMOTION` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Emotion action branch metadata is structurally required only. Forced-emotion side effects and broadcast behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Dialog action branch metadata is structurally required only. Java dialog behavior is not runtime compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Movement branch metadata is required through the shared trace schema. Anti-hack, Future scheduling, and `MovementNotifyTask` behavior remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_MOVE_IN_AIR` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | In-air movement branch metadata is required structurally. Timing, glide/fly behavior, and Java movement semantics remain unverified. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_SHOW_DIALOG` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Show-dialog branch metadata is structurally required only. Runtime Java dialog effects are not compared. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` trace schema contract | Packet / Validator Contract | Partial | Unit Tested | Needs Verification | Use-item branch metadata is structurally required only. Item-use behavior, cooldowns, and inventory state parity remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` controller trace schema contract | Controller / Validator Contract | Partial | Unit Tested | Needs Verification | Controller protection state breadcrumbs are structurally validated elsewhere; this unit only adds branch/caller metadata requirements. |
| `com.aionemu.gameserver.controllers.CreatureController` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` controller trace schema contract | Controller / Validator Contract | Partial | Unit Tested | Needs Verification | Creature/controller breadcrumbs remain schema-only until Java traces are generated. Threading and lifecycle behavior are not verified. |
| `com.aionemu.gameserver.network.aion.AionServerPacket` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` packet-observer trace schema contract | Packet Base / Validator Contract | Partial | Unit Tested | Needs Verification | Packet-name and observer metadata can be represented in artifacts, but packet bytes and serialization ordering are not compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.ServerPacketCaptureObserver` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` observer trace schema contract | Observer Interface / Validator Contract | Partial | Unit Tested | Needs Verification | Observer payload structure remains contract-only. Java observer implementation and runtime attachment are not available. |
| `com.aionemu.gameserver.network.aion.serverpackets.NoOpServerPacketCaptureObserver` | `Aion.GameServer.Services.PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService` observer trace schema contract | Observer Implementation / Validator Contract | Partial | Unit Tested | Needs Verification | No-op observer behavior is not runtime compared. C# validator only checks artifact shape. |

## Remaining Risks

- Java serializer and generated Java artifacts remain missing, so parity is not objectively verified.
- `actionBranchName` is required structurally but not allow-listed; this avoids overfitting before Java output exists.
- `callerOrigin` validation currently checks required field presence when the payload is present; deeper type/semantic validation is still future work.
- Caller ordering for level-ready, teleport, Beritra portal, and teleport-animation done flows is not runtime compared.
- Movement, anti-hack, `Future` scheduling, `MovementNotifyTask`, emotion/action side effects, and packet-byte output are still unverified.
- Timestamp fields remain diagnostic only and should not be treated as deterministic parity proof.
- Java 25/JDK/Maven availability is still a blocker for runtime Java trace generation.
- `docs/commit-conventions.md` was requested by the startup flow but is absent in this repository.

## Summary Metrics

- Total Java artifacts discovered: 18 grouped artifact rows for the protection stop-trigger contract.
- Total artifacts ported: 1 validator contract slice, plus 2 focused unit tests and 1 affected representative fixture update.
- Total artifacts with verified parity: 0.
- Total artifacts needing verification: 18.
- Total blocked artifacts: Java serializer/observer/runtime traces and Java build/runtime environment remain blocked.
- Estimated overall migration completion: about 72%, unchanged by this schema-only unit.

## Next Recommended Unit Of Work

Surface the now-closed protection stop-trigger validator schema contract in `PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportService` or the C# trace-emitter design report.

Recommended scope:

- Make readiness/report output explicitly mention that validator shape coverage now includes nested payload requirements plus caller/branch metadata.
- Keep `ReadyForRuntimeComparison` false until generated Java trace artifacts exist.
- Do not wire a live C# emitter or Java observer in the same unit.
- Do not claim Verified Parity.

Acceptance criteria:

- Focused readiness/report tests updated and passing.
- `docs/PHASE-6-PROGRESS.md` updated with a new session entry, Migration Parity Table, risks, metrics, and next unit.
- A new completion handoff is generated.
- A commit is created for the completed unit.

Safe parallel candidates for the next session:

| Candidate | Files / Area | Notes |
| --- | --- | --- |
| Readiness-report surfacing | `PlayerProtectionActiveTaskStopTriggerRuntimeComparisonReadinessReportService` and tests | Best next unit; narrow, low-risk reporting work. |
| C# trace-emitter design-report surfacing | Existing design/report service and tests if present | Useful if readiness already covers enough detail. |
| Java serializer implementation | Java instrumentation and generated fixture path | Larger and blocked/high-risk; defer unless environment is ready. |

Do not parallelize edits to shared docs, the shared validator, or the same readiness test file.

## Continuation Context

Current unit commit message:

```text
[Phase 6][UOW-1598] Enforce protection caller branch fields
```

Files changed in this unit:

- `dotnetConversion/src/Aion.GameServer/Services/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactValidatorServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerProtectionActiveTaskStopTriggerJavaTraceArtifactDirectoryReportServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6APR-Completion.md`

Latest prior commits:

- `0f70910b9 [Phase 6][UOW-1597] Enforce protection emotion action fields`
- `26b8e9d6c [Phase 6][UOW-1596] Enforce protection AI notify fields`
- `e52a8884a [Phase 6][UOW-1595] Enforce protection fanout fields`
- `16224a685 [Phase 6][UOW-1594] Enforce protection movement fields`
- `57ea7ae55 [Phase 6][UOW-1593] Enforce protection task-cancel fields`

Required startup reading for the next continuation:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parallelization-strategy.md`
- `docs/parity-verification.md`
- `docs/PHASE-6-PROGRESS.md`
- Latest completion handoff, currently this file

Note: `docs/commit-conventions.md` is still missing.
