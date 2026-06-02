# Phase 6 Session 2271 Completion - Observation Blocker Wording

## Scope

Tightened non-live command-decision wording for `CM_FIND_GROUP` mutation-post action `2` and action `6` so executor observation and registry send observation remain distinct evidence gates.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests.cs`

The command-decision report now records separate notes for:

- `BoundaryExecutorObservation`: proves the guarded `CM_FIND_GROUP` boundary invoked the side-effect executor after packet acceptance; does not prove posted/refreshed registry send ordering.
- `RegistrySendObservation`: proves posted system message and refreshed `SM_FIND_GROUP` list were observed through direct registry sends in Java order; does not prove the boundary invoked the executor.

No live dispatch, packet sends, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live command-decision wording plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests" --no-restore
```

Result: passed 8, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java source was reviewed directly as source-of-truth context for the non-live wording.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution build were skipped because the filtered C# command built the affected project and dependencies and covered the changed command-decision report plus the directly adjacent blocker summary.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService` | Command Decision Metadata | Partial | Unit Tested | Partial Parity | Command-decision rows now distinguish guarded-boundary executor invocation from registry send ordering. Metadata only; no live dispatch or registry observation was executed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportSelectsConsistencyAuditBeforeJavaCapture` | Unit | Java `addRecruitment/addApplication` mutation and send ordering | Command-decision rows separately describe boundary executor observation and registry send observation. | Non-live command metadata. | No live boundary dispatch or registry send observation. |
| `Create_RuntimeMissingSummaryUsesRuntimeBlockersFromAcceptanceMatrix` | Unit | Java `addRecruitment/addApplication` mutation and send ordering | Blocker summary still maps boundary executor and registry send blockers to separate reasons and evidence. | Non-live blocker metadata. | No runtime comparison. |

## Summary Metrics

- Java artifacts reviewed: 1
- C# non-live services updated: 1
- C# test classes updated: 2
- Verified parity rows added: 0
- Partial parity rows updated: 1
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves blocker wording but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2271] Clarify observation blocker wording
```

## Next Recommended UOW

Begin the next safe evidence step by adding a non-live handoff/checklist row that defines the exact accepted C# boundary row fields required before any guarded live boundary capture can be consumed by runtime comparison. Keep the unit metadata-only unless a current handoff explicitly authorizes capture execution.

Safe candidates:

- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
- Review checklist strings for excessive length only if a future focused test or handoff becomes hard to read.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
