# Phase 6 Session 2294 Completion - Post-Capture Dry-Run Evidence

## Scope

Surfaced explicit-root Java capture dry-run consistency evidence inside the explicit-root Java post-capture validator summary for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`

The explicit-root Java post-capture validator summary now includes `DryRunCommandConsistencyEvidence`, sourced from `FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReport.CommandConsistencyEvidence`. The summary preserves the dry-run command consistency chain even when post-capture validation is blocked by a missing artifact directory or remains blocked after shape-valid Java artifacts.

The summary remains non-live: it validates generated Java artifact shape only, does not execute Maven, does not run C# guarded boundary capture, does not run runtime comparison, does not enable executable implementation, and does not claim verified parity.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, capture execution, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live explicit-root Java post-capture validator summary metadata plus unit tests.

Specific behavior/contract:

- The explicit-root Java post-capture validator summary preserves dry-run command consistency evidence while continuing to block runtime comparison and verified parity until accepted live C# boundary rows and runtime comparison evidence exist.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests" --no-restore
```

Result: passed 9, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live post-capture metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed post-capture validator summary plus the directly adjacent explicit-root Java capture dry-run command report.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService` | Explicit-Root Java Post-Capture Summary Metadata | Partial | Unit Tested | Partial Parity | Post-capture summary now preserves dry-run command consistency evidence, including command-provider consistency rows and command-decision evidence that carries nested blocker summary, acceptance matrix, live-capture preflight, runtime-comparison handoff, executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata where present. Metadata only; no capture or comparison command was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService` | Explicit-Root Java Post-Capture Summary Metadata | Partial | Unit Tested | Partial Parity | Java action `2`/`6` mutation-post behavior remains represented as shape-valid artifact and blocker metadata only. Verified parity remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_MissingDirectoryNamesExpectedRowsAndCaptureCommand` | Unit | Java action `2`/`6` mutation-post mapping and dry-run command evidence | Missing-directory post-capture summary keeps the guarded Java capture command, expected action rows, runtime-comparison blockers, and dry-run command consistency evidence. | Non-live post-capture metadata plus source review. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |
| `Create_ShapeValidArtifactsRemainRuntimeComparisonBlocked` | Unit | Java action `2`/`6` mutation-post artifact shape fixtures | Shape-valid Java artifacts remain blocked from runtime comparison and still preserve dry-run command consistency evidence. | Non-live artifact-shape metadata plus source review. | Shape-valid Java artifacts are not accepted live C# boundary rows or runtime comparison evidence. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves post-capture evidence traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2294] Surface dry-run evidence in post-capture summary
```

## Next Recommended UOW

Surface explicit-root Java post-capture validator summary evidence inside the Java/C# row-pairing readiness report. Keep the unit metadata-only: the row-pairing readiness report should continue consuming `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummary`, but preserve `DryRunCommandConsistencyEvidence` so row-pairing readiness carries the Java capture command gate and post-capture blocker chain.

Safe candidates:

- Inspect `FindGroupMutationPostJavaCSharpRowPairingReadinessReportService` and its tests.
- Add row-pairing evidence sourced from `javaSummary.DryRunCommandConsistencyEvidence`.
- Keep validation focused on row-pairing readiness tests plus post-capture validator summary tests.
