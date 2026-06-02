# Phase 6 Session 2288 Completion - Acceptance Matrix Runbook Evidence

## Scope

Surfaced live-capture preflight runbook row evidence inside the value-reader executor capture acceptance matrix for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests.cs`

The capture acceptance matrix rows now include `liveCapturePreflightRows`, preserving source runbook row evidence that already carries runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff status.

The matrix remains non-live: Java artifact capture, C# guarded boundary capture, value reads, row identity matching, materialization, result emission, runtime comparison, executable implementation, live dispatch, and verified parity are still blocked.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live capture acceptance matrix metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live matrix metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed capture acceptance matrix plus the directly adjacent live-capture preflight runbook.

## Focused Testing Note

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active completion/handoff notes name a broad-validation trigger first.

If a focused command is expected to take several minutes, narrow it to the edited test class and the closest adjacent contract class. Do not broaden to a full project or solution run just to avoid choosing a specific parity evidence command.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService` | Capture Acceptance Matrix Metadata | Partial | Unit Tested | Partial Parity | Matrix rows now preserve live-capture preflight row evidence, including nested runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata. Metadata only; no capture command was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService` | Capture Acceptance Matrix Metadata | Partial | Unit Tested | Partial Parity | Matrix remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_RuntimeMissingMatrixNamesCaptureEvidenceFieldsAndBlockers` | Unit | Java action `2`/`6` mutation-post mapping | Runtime-missing matrix exposes capture evidence fields and preserves runbook row evidence that includes runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, and accepted-boundary-row handoff evidence. | Non-live matrix metadata. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |
| `Create_ReadyShapedRunbookStillBlocksRuntimeComparisonAndExecutableImplementation` | Unit | `addRecruitment` and `addApplication` direct-send mutation-post actions | Ready-shaped runbook evidence remains visible in executable implementation gate rows while runtime comparison and executable implementation stay blocked. | Non-live matrix metadata. | No executable implementation, runtime comparison, or live dispatch. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves capture acceptance matrix traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2288] Surface runbook evidence in acceptance matrix
```

## Next Recommended UOW

Surface capture acceptance matrix row evidence inside the value-reader executor capture execution blocker summary. Keep the unit metadata-only: the blocker summary should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContract`, but its rows can preserve matrix evidence that now includes live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureExecutionBlockerSummaryService` and its tests.
- Update the blocker summary to include capture acceptance matrix row evidence without running Java capture, C# live capture, runtime comparison, or executable implementation.
- Keep validation focused on capture execution blocker summary tests plus capture acceptance matrix tests.
