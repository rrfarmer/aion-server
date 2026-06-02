# Phase 6 Session 2270 Completion - Command Decision Checklist Inventory

## Scope

Added the command-decision report to the non-live runtime evidence checklist/readiness inventory for `CM_FIND_GROUP` mutation-post action `2` and action `6`. This keeps future Work Discovery from jumping from Java artifact command consistency directly to Java/Maven capture while the executor consistency acceptance gate remains visible.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`

The Java runtime artifact checklist row now names:

- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService`
- The requirement that Java capture should run only after the command-decision report selects Java capture after `executorConsistencyAuditAccepted`.

The result-emission checklist row now includes the command-decision report alongside the live-capture preflight runbook, capture acceptance matrix, and capture execution blocker summary.

No live dispatch, packet sends, value reads, comparison execution, materialization, result emission, Java capture, C# capture, or verified parity claim was added.

## Validation Decision

Changed surface:

- C# non-live runtime evidence checklist metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests" --no-restore
```

Result: passed 12, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live checklist metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution build were skipped because the filtered C# command built the affected project and dependencies and covered the changed checklist plus directly adjacent command-decision and command-consistency reports.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Runtime Evidence Checklist Metadata | Partial | Unit Tested | Partial Parity | Checklist now surfaces the command-decision report before Java capture evidence, preserving the `executorConsistencyAuditAccepted` blocker in Work Discovery. Metadata only; no Java/C# runtime rows are captured or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Runtime Evidence Checklist Metadata | Partial | Unit Tested | Partial Parity | Result-emission inventory now includes command-decision metadata beside the blocker summary/runbook chain. Still blocked by missing runtime-backed Java artifacts, accepted C# rows, value reads, materialization, emission, runtime comparison, and live dispatch. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader` | Unit | Java action `2`/`6` mutation-post mapping | Java runtime artifact row names command-decision report and requires Java capture selection after `executorConsistencyAuditAccepted`. | Non-live checklist metadata. | No Maven execution or Java artifact generation. |
| `Create_CSharpBoundaryAndRegistryRowsStayNonLive` | Unit | Java action `2`/`6` side-effect ordering | Result-emission row names command-decision report beside the blocker summary/runbook chain while staying non-live. | Non-live checklist metadata. | No C# live boundary dispatch or result emission. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves checklist visibility but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2270] Surface command decision in checklist
```

## Next Recommended UOW

Tighten precise blocker wording for executor observations versus registry observations in the command-decision/blocker metadata so future sessions can distinguish "executor invoked from guarded boundary" from "posted/refreshed packets observed through registry in Java order." Keep the unit non-live and metadata-only.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings after the metadata gates remain visible.
- Add another narrow command-decision consumer only if a current handoff or checklist still points directly to Java capture before `executorConsistencyAuditAccepted`.
- Review checklist strings for excessive length only if a future focused test or handoff becomes hard to read.
