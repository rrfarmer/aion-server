# Phase 6 Session 2247 Completion - Deterministic Java Capture Timestamp Override

## Scope

Added a deterministic fixture-only `serverEpochSeconds` override for future `CM_FIND_GROUP` action `2`/`6` Java mutation-post trace artifacts. The override is opt-in through a system property and preserves default Java hook behavior by continuing to use `GroupRecruitment.getLastUpdate()` and `GroupApplication.getLastUpdate()` when the property is absent.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`

## Changes

Java:

- Added `FindGroupMutationPostTraceCaptureHooks.SERVER_EPOCH_SECONDS_PROPERTY` with value `aion.findGroupMutationPost.serverEpochSeconds`.
- Added `FindGroupMutationPostTraceCaptureHooks.serverEpochSeconds(int javaLastUpdate)`.
- Default behavior: blank or missing property returns the Java `lastUpdate` value.
- Override behavior: an integer property value replaces the captured row timestamp for action `2` and action `6` rows.
- Invalid property values throw `NumberFormatException` rather than silently producing misleading artifacts.
- Added focused Java tests for default preservation, deterministic override, and invalid override rejection.

C#:

- Updated `FindGroupMutationPostJavaArtifactCaptureRunbookService.FocusedMavenCommand()` to include `-Daion.findGroupMutationPost.serverEpochSeconds=1700000000`.
- Updated `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.JavaCaptureCommand(...)` to include the same deterministic timestamp property before the artifact-root property.
- Updated focused C# tests for exact command metadata.

Documentation:

- Updated `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` to record the deterministic timestamp override.
- `docs/PHASE-6-PROGRESS.md` was intentionally not touched.

## Validation Decision

Changed surface:

- Java production hook scaffold used only when the capture flag is enabled.
- Java fixture tests.
- C# non-live command metadata and unit tests.
- Design/session documentation.

Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, failed 0, errors 0, skipped 1. Existing Java `Unsafe` warnings remain.

Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: passed 22, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Broad-validation trigger: none. Full `.NET` build/suite and broad Maven validation were skipped because the change is limited to a guarded capture hook/test path and adjacent non-live command metadata. The targeted Maven command compiled the affected Java module, and the filtered C# command built the affected C# project/dependencies.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks` | `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService` | Java Capture Hook / C# Runbook Metadata | Partial | Unit Tested / Java Targeted Tested | Partial Parity | Adds opt-in deterministic timestamp override for capture rows only. Default behavior still uses Java `lastUpdate`. No runtime Java/C# comparison executed. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Service Mutation Capture Command Metadata | Partial | Unit Tested / Java Targeted Tested | Partial Parity | Capture command now includes deterministic `serverEpochSeconds` property so future generated artifacts avoid timestamp churn. Live C# boundary rows, registry observation, value projection, materialization, emission, and runtime comparison remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `productionHooksAssembleMutationPostRowsInMemoryWithoutWritingArtifacts` | Java Unit | Java `FindGroupService.addRecruitment/addApplication` hook placement and `GroupRecruitment`/`GroupApplication` last-update behavior | Missing timestamp override preserves Java `lastUpdate` in captured rows. | Targeted Java test. | Does not compare against C# runtime rows. |
| `productionHooksCanUseDeterministicServerEpochSecondsOverrideForCaptureRows` | Java Unit | Capture fixture property contract | Integer override forces both action `2` and action `6` rows to use the deterministic timestamp. | Targeted Java test. | Fixture-only; no runtime comparison. |
| `deterministicServerEpochSecondsOverrideRejectsInvalidValues` | Java Unit | Capture fixture property contract | Invalid timestamp override values fail fast with `NumberFormatException`. | Targeted Java test. | No artifact output comparison. |
| `Create_NamesFocusedMavenCommandButMarksItDesignOnly` | C# Unit | C# runbook command metadata | Java capture runbook command includes deterministic timestamp property. | Non-live command metadata. | Does not execute Maven. |
| `Create_RuntimeMissingRunbookNamesConcreteJavaCaptureCommandAndArtifactRoot` | C# Unit | C# live-capture preflight metadata | Preflight command includes deterministic timestamp property and artifact-root property. | Non-live command metadata. | Does not execute capture. |

## Summary Metrics

- Java artifacts reviewed: 4
- C# artifacts reviewed/updated: 3
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: live Java artifact generation from explicit artifact root, live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW makes future evidence more deterministic but does not add runtime parity evidence.

## Next Recommended UOW

Add a narrow command-oriented artifact-root validation report that checks whether generated Java action `2`/`6` files exist at a supplied artifact root and names the exact C# validator command to run after capture.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a C# non-live report that records the deterministic timestamp property across all value-reader capture command providers.
