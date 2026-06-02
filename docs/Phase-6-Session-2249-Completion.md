# Phase 6 Session 2249 Completion - Capture Command Consistency Report

## Scope

Added a non-live consistency report for the focused Java capture commands used by the `CM_FIND_GROUP` action `2`/`6` value-reader runtime-comparison runbook.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureScenarioBuilder.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactRootValidationCommandReportService.cs`

## Changes

Added `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService`.

The report verifies that these non-live command providers agree on the capture flag and deterministic timestamp fragment:

- `FindGroupMutationPostJavaArtifactCaptureRunbookService`
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService`
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService`
- `FindGroupMutationPostJavaArtifactRootValidationCommandReportService`

The expected deterministic timestamp fragment is:

```text
-Daion.findGroupMutationPost.serverEpochSeconds=1700000000
```

Root-aware command providers must also carry the selected artifact root through:

```text
-Daion.findGroupMutationPost.artifactRoot=<artifactRoot>
```

Updated `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the Java runtime trace artifact evidence chain names the new consistency report.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched. Current working context now belongs in the latest completion/handoff docs.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 26, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; the new report audits C# command metadata derived from already-reviewed Java capture scaffolds.

Broad-validation trigger: none. Full `.NET` build/suite was skipped because the filtered `dotnet test` command built the affected project and dependencies and covered the new report plus directly adjacent command providers/checklist.

## Focused Testing Note

Future Phase 6 sessions should keep validation narrow by default:

- Run the edited test class plus directly adjacent service/packet/parser classes with `--filter`.
- Treat a passing filtered `dotnet test` as the compile signal for the affected project and dependencies.
- Run Java/Maven only when Java source/fixtures change or a narrow Java source-of-truth command exists.
- Run full `.NET` project tests, solution tests, or solution builds only after documenting a broad-validation trigger from `docs/orchestration-rules.md`.
- For documentation-only units, run `git diff --check`; runtime tests are not applicable unless docs changed generated artifacts, scripts, test inputs, or run commands.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService` | Java Capture Command Metadata | Partial | Unit Tested | Partial Parity | Verifies non-live C# command providers carry the same deterministic timestamp capture fragment. Does not run Maven, generate artifacts, execute C# boundary capture, or compare runtime rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService` | Service Mutation Capture Metadata | Partial | Unit Tested | Partial Parity | Keeps action `2`/`6` capture command metadata aligned with the Java-derived deterministic timestamp. Live Java/C# runtime rows, registry observation, value projection, materialization, emission, and comparison remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportVerifiesEveryCaptureCommandProvider` | Unit | Java capture fixture/runbook command metadata | All four command providers carry the capture flag and deterministic timestamp fragment; runtime comparison remains blocked. | Non-live command metadata only. | Does not execute Java capture or compare rows. |
| `Create_InconsistentRunbookCommandBlocksBeforeCapture` | Unit | Java capture fixture/runbook command metadata | Missing timestamp command fragments mark the report inconsistent before capture can be attempted. | Guardrail metadata only. | Does not prove Java/C# behavior. |
| `Create_CustomArtifactRootMustAppearInRootAwareProviders` | Unit | Java explicit artifact-root capture path | Root-aware providers carry the selected artifact root while the base runbook command remains root-agnostic. | Command consistency only. | Does not create artifacts. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader`

## Summary Metrics

- Java artifacts reviewed: 4
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: generated runtime-backed Java artifacts, live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves capture-command safety and session validation discipline but adds no runtime parity evidence.

## Next Recommended UOW

Add a focused explicit-root Java capture dry-run command example/report that records the exact temporary-root command and acceptance gates for intentionally generating action `2`/`6` artifacts without touching repository artifact output.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.
