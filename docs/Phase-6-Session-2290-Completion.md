# Phase 6 Session 2290 Completion - Command Decision Summary Evidence

## Scope

Surfaced capture execution blocker summary row evidence inside the value-reader executor capture command decision report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests.cs`

The capture command decision report rows now include `captureExecutionBlockerSummaryRows`, preserving blocker summary row evidence that already carries capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff metadata where available.

The command decision report remains non-live: selected commands still have `ShouldRunCommand=false`, Java capture remains disabled, C# guarded boundary capture remains disabled, runtime comparison remains disabled, executable implementation remains blocked, and verified parity is not claimed.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, capture execution, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live capture command decision report metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryServiceTests" --no-restore
```

Result: final run passed 8, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Note: the first focused run failed because one assertion expected fixture-only accepted-boundary handoff wording in the default generated contract. The assertion was corrected to check the real default evidence chain: `captureExecutionBlockerSummaryRows`, `captureAcceptanceMatrixRows`, `liveCapturePreflightRows`, `runtimeComparisonHandoffRows`, `consistencyAuditRowEvidence`, `ExecutorEvidenceBridge`, and `ResultEmissionBlocker`.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live command decision metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed capture command decision report plus the directly adjacent capture execution blocker summary.

## Focused Testing Note

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active completion/handoff notes name a broad-validation trigger first.

If a focused command is expected to take several minutes, narrow it to the edited test class and the closest adjacent contract class. Do not broaden to a full project or solution run just to avoid choosing a specific parity evidence command.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService` | Capture Command Decision Metadata | Partial | Unit Tested | Partial Parity | Command decision rows now preserve capture execution blocker summary row evidence, including nested acceptance matrix, live-capture preflight, runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata where present. Metadata only; no capture or comparison command was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReportService` | Capture Command Decision Metadata | Partial | Unit Tested | Partial Parity | Report remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultReportSelectsConsistencyAuditBeforeJavaCapture` | Unit | Java action `2`/`6` mutation-post mapping | Default report selects executor consistency audit before Java capture and preserves blocker-summary, matrix, live-capture preflight, runtime-comparison handoff, consistency audit, executor bridge, and result-emission blocker evidence in the primary decision row. | Non-live command decision metadata. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves capture command decision traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2290] Surface summary evidence in command decision
```

## Next Recommended UOW

Surface capture command decision row evidence inside the value-reader executor capture command consistency report. Keep the unit metadata-only: the consistency report should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandDecisionReport`, but it can preserve command-decision rows that now include capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService` and its tests.
- Update the command consistency report to include command-decision row evidence without running Java capture, C# live capture, runtime comparison, or executable implementation.
- Keep validation focused on capture command consistency report tests plus capture command decision report tests.
