# Phase 6 Session 2269 Completion - Capture Command Decision Report

## Scope

Added a non-live command-decision report for `CM_FIND_GROUP` mutation-post action `2` and action `6` value-reader capture evidence. The report consumes the capture execution blocker summary and selects the next focused evidence command without executing Java capture, C# capture, runtime comparison, or executable implementation.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Java Source Findings

`CM_FIND_GROUP.readImpl` still maps:

- Action `2`: reads `playerOrTeamId`, `message`, and `groupType`.
- Action `6`: reads `playerOrTeamId`, `message`, `groupType`, `classId`, and `level`.

`CM_FIND_GROUP.runImpl` still maps:

- Action `2` to `FindGroupService.addRecruitment(player, message, groupType)`.
- Action `6` to `FindGroupService.addApplication(player, message, groupType, classId, level)`.

`FindGroupService.addRecruitment` and `addApplication` still mutate the recruitment/application maps, record mutation and posted-message trace hooks, send the posted system message, then refresh the related list with `SM_FIND_GROUP`.

## Changes

Added:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`

The command-decision report now records:

- Default selected evidence field: `executorConsistencyAuditAccepted`.
- Default selected command kind: `ExecutorConsistencyAudit`.
- Java capture becomes selectable only when the primary blocker moves to `JavaArtifactRows` or `JavaArtifactShapeValidation`.
- C# guarded boundary capture becomes selectable only when the primary blocker moves to C# boundary/observation fields.
- Every selected command is metadata-only: `ShouldRunSelectedCommand=false`, `CanRunJavaCapture=false`, `CanRunCSharpCapture=false`, `CanRunRuntimeComparison=false`, `CanStartExecutableImplementation=false`, and `CanClaimVerifiedParity=false`.

The Java capture command consistency report now audits only actual Java capture command providers:

- Java artifact capture runbook.
- Live-capture preflight runbook Java capture command.
- Java artifact root validation command report.

It no longer treats the blocker summary's smallest next command as a Java capture provider, because that command is now intentionally the executor consistency audit while `executorConsistencyAuditAccepted` is missing. It records the command-decision report's selected evidence field and confirms Java capture is deferred behind the consistency gate.

No live dispatch, packet sends, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live command-decision report service and unit tests.
- Adjacent Java-capture command consistency report and unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: passed 8, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

The adjacent command consistency test was then run because work discovery showed it still assumed the blocker summary's smallest next command was a Java capture command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests" --no-restore
```

Initial result: failed 2, passed 1. The failures were stale expectations after `executorConsistencyAuditAccepted` became the first visible command decision blocker.

Fix applied: the consistency report now audits Java capture providers directly and records that the command-decision report defers Java capture until the executor consistency gate is accepted.

Re-run result: passed 3, failed 0, skipped 0.

Full focused C# validation for this UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests" --no-restore
```

Result: passed 21, failed 0, skipped 0.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live command metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution build were skipped because the filtered C# commands built the affected project and dependencies and covered the changed report plus directly adjacent blocker, matrix, runbook, and command consistency contracts.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService` | Command Decision Metadata | Partial | Unit Tested | Partial Parity | Non-live report selects the next focused evidence command for action `2`/`6` mutation-post comparison blockers. It selects executor consistency before Java capture and never executes capture, comparison, dispatch, or verified parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService` | Java Capture Command Metadata | Partial | Unit Tested | Partial Parity | Command consistency now audits actual Java capture providers while preserving the decision gate that defers Java capture behind `executorConsistencyAuditAccepted`. No runtime-backed Java/C# row comparison exists yet. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportSelectsConsistencyAuditBeforeJavaCapture` | Unit | Java action `2`/`6` mutation-post mapping and current blocker chain | Default command decision chooses `executorConsistencyAuditAccepted` before Java capture and does not authorize execution. | Non-live command metadata. | No Java capture execution. |
| `Create_JavaArtifactRowsPrimarySelectsJavaCaptureButStillDoesNotRunIt` | Unit | Capture blocker requirements after consistency acceptance | Java capture can become the selected command when Java artifact rows are the primary blocker, but remains metadata-only. | Synthetic blocker summary. | Does not prove capture output. |
| `Create_CSharpBoundaryRowsPrimarySelectsGuardedBoundaryCaptureButStillDoesNotRunIt` | Unit | C# boundary evidence requirements | C# guarded boundary capture can become selected only for boundary blockers and remains metadata-only. | Synthetic blocker summary. | No live boundary dispatch. |
| `Create_ReadyRuntimeComparisonSummaryStillDoesNotExecuteComparison` | Unit | Runtime comparison gate requirements | Even a ready-shaped source summary does not let this report execute comparison or claim parity. | Synthetic blocker summary. | No runtime comparison. |
| `Create_DefaultReportVerifiesEveryCaptureCommandProvider` | Unit | Java capture runbook command contract | Java capture consistency audits direct Java capture providers and records the separate command decision deferral. | Non-live command metadata. | No Maven execution. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services added: 1
- C# non-live services updated: 1
- C# test classes added: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves command decision visibility but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2269] Add capture command decision report
```

## Next Recommended UOW

Add the command-decision report to the runtime evidence checklist/readiness inventory so future Work Discovery sees it alongside the existing blocker summary, runbook, and command consistency report. Keep the unit non-live and metadata-only.

Safe candidates:

- Tighten precise blocker wording for executor observations versus registry observations.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings after the metadata gates remain visible.
- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
