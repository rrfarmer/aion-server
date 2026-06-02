# Phase 6 Session 2247 Handoff - Deterministic Java Capture Timestamp Override

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2247-Completion.md`
- `docs/Phase-6-Session-2247-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2247 added an opt-in deterministic timestamp override for Java mutation-post capture rows:

- Property: `aion.findGroupMutationPost.serverEpochSeconds`
- Default deterministic command value: `1700000000`
- Default Java behavior with the property absent or blank: use Java `lastUpdate`.
- Invalid property values: fail fast with `NumberFormatException`.

Changed Java artifacts:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

Changed C# artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The change does not write repository artifacts by default, does not enable live C# dispatch, and does not execute Java/C# runtime comparison.

## Validation From Last Session

Focused Java/Maven validation passed:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Result: build success. `FindGroupMutationPostTraceCaptureTest` ran 32 tests, failed 0, errors 0, skipped 1. Existing Java `Unsafe` warnings remain.

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureImplementationReadinessServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: passed 22, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Broad validation was skipped because no broad-validation trigger applied.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

For Java capture fixture changes, use:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Daion.findGroupMutationPost.serverEpochSeconds=1700000000" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

For C# command-metadata or non-live report changes, use filtered `dotnet test` for the edited test class and directly adjacent classes. A passing filtered `dotnet test` is the compile signal for the affected C# project and dependencies.

Use full `.NET` build/suite only when a broad-validation trigger from `docs/orchestration-rules.md` is explicitly documented first.

## Next Recommended UOW

Add a narrow command-oriented artifact-root validation report that checks whether generated Java action `2`/`6` files exist at a supplied artifact root and names the exact C# validator command to run after capture.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`
  - `FindGroupMutationPostTraceCaptureArtifactWriter.java`
  - `FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
  - `FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- Add a small non-live C# report that records:
  - artifact root,
  - expected action `2`/`6` paths,
  - deterministic timestamp property,
  - Java capture command,
  - C# validator command,
  - whether repository artifacts are present,
  - why runtime comparison remains blocked.
- Run focused C# tests for the new report plus the directory/validator report tests.
- Run Java/Maven only if Java fixture code changes.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a C# non-live report that records the deterministic timestamp property across all value-reader capture command providers.

## Parity Caution

Current status remains partial parity only. Verified parity is still blocked by missing live Java artifacts, missing C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
