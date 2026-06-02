# Phase 6 Session 2293 Completion - Dry-Run Consistency Evidence

## Scope

Surfaced capture command consistency evidence inside the explicit-root Java capture dry-run command report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`

The explicit-root Java capture dry-run report now includes `CommandConsistencyEvidence`. The evidence preserves the upstream command consistency report status, provider consistency rows, selected command-decision kind and evidence field, and command-decision row evidence that already carries capture execution blocker summary rows, capture acceptance matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff metadata where available.

The report remains non-live: it names an intentional temporary-root Java capture command only, does not execute Maven, does not run C# guarded boundary capture, does not run runtime comparison, does not enable executable implementation, and does not claim verified parity.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, capture execution, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live explicit-root Java capture dry-run command report metadata plus unit tests.

Specific behavior/contract:

- The explicit-root Java capture dry-run command report preserves the command consistency evidence chain without enabling Java capture, C# live capture, runtime comparison, executable implementation, or verified parity.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureCommandConsistencyReportServiceTests" --no-restore
```

Result: passed 6, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live dry-run metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed dry-run command report plus the directly adjacent capture command consistency report.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService` | Explicit-Root Java Capture Dry-Run Metadata | Partial | Unit Tested | Partial Parity | Dry-run report now preserves command consistency evidence, including provider rows and command-decision evidence that carries nested blocker summary, acceptance matrix, live-capture preflight, runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata where present. Metadata only; no capture or comparison command was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService` | Explicit-Root Java Capture Dry-Run Metadata | Partial | Unit Tested | Partial Parity | Java action `2`/`6` mutation-post behavior remains represented as dry-run command and artifact-target metadata only. Verified parity remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_TemporaryRootNamesFocusedJavaCommandAndAcceptanceGates` | Unit | Java action `2`/`6` mutation-post mapping and existing command consistency metadata | Temporary-root dry-run report names the guarded Java capture command, remains runtime-comparison blocked, and preserves command consistency evidence through command-decision rows, blocker summary rows, matrix rows, live-capture preflight rows, runtime-comparison handoff rows, executor consistency audit evidence, executor bridge evidence, and result-emission blocker evidence. | Non-live dry-run metadata plus source review. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves explicit-root dry-run evidence traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2293] Surface consistency evidence in dry-run report
```

## Next Recommended UOW

Surface explicit-root Java capture dry-run evidence inside the explicit-root Java post-capture validator summary. Keep the unit metadata-only: the post-capture validator summary should continue consuming `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReport`, but preserve dry-run command consistency evidence so shape-valid Java artifact summaries still carry the command gates and blocker chain that led to the capture command.

Safe candidates:

- Inspect `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService` and its tests.
- Add a summary evidence field sourced from `dryRunReport.CommandConsistencyEvidence`.
- Keep validation focused on post-capture validator summary tests plus dry-run command report tests.
