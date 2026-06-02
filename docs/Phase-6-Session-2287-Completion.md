# Phase 6 Session 2287 Completion - Live Capture Runbook Handoff Evidence

## Scope

Surfaced runtime-comparison handoff row evidence inside the value-reader executor live-capture preflight runbook for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in the latest completion/handoff documents.

## Changes

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`

The live-capture preflight runbook rows now include `runtimeComparisonHandoffRows`, preserving runtime-comparison handoff row evidence that includes executor consistency audit rows, executor evidence bridge rows, result-emission blocker evidence, materialization blocker evidence, projected-value row evidence, and accepted-boundary-row handoff status.

The runbook remains non-live: Java artifact capture, C# guarded boundary capture, value reads, row identity matching, materialization, result emission, runtime comparison, executable implementation, live dispatch, and verified parity are still blocked.

No Java source, fixture, packet send, live dispatch, reader invocation, value read, materialization, result emission, runtime comparison, or verified parity claim changed.

## Validation Decision

Changed surface:

- C# non-live live-capture preflight runbook metadata plus unit tests.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth context for the non-live runbook metadata.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed live-capture preflight runbook plus the directly adjacent runtime-comparison handoff.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Live Capture Preflight Runbook Metadata | Partial | Unit Tested | Partial Parity | Runbook now preserves runtime-comparison handoff row evidence, including executor consistency audit, executor bridge, result-emission blocker, materialization, projected-value row, and accepted-boundary-row handoff metadata. Metadata only; no capture command was run or enabled. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Live Capture Preflight Runbook Metadata | Partial | Unit Tested | Partial Parity | Runbook remains blocked until runtime-backed Java artifacts, accepted C# boundary rows, runtime row values, concrete reader invocation, row identity decisions, comparison, materialization, result emission, runtime comparison, and live dispatch evidence exist. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_RuntimeMissingRunbookNamesConcreteJavaCaptureCommandAndArtifactRoot` | Unit | Java action `2`/`6` mutation-post mapping | Runtime-missing runbook exposes runtime-comparison handoff rows that include consistency audit, executor bridge, result-emission blocker, and accepted-boundary-row handoff evidence while Java capture remains blocked. | Non-live runbook metadata. | No Java capture, accepted live C# rows, executor execution, comparison, or emitted results. |
| `Create_ReadyShapedHandoffStillBlocksComparisonAndExecutableImplementation` | Unit | `addRecruitment` and `addApplication` direct-send mutation-post actions | Ready-shaped handoff evidence remains visible in runtime-comparison execution rows while runtime comparison and executable implementation stay blocked. | Non-live runbook metadata. | No executable implementation, runtime comparison, or live dispatch. |

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services updated: 1
- C# test classes updated: 1
- Verified parity rows added: 0
- Partial parity rows updated: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW improves live-capture preflight traceability but adds no runtime evidence.

## Commit

Commit message:

```text
[Phase 6][UOW-2287] Surface handoff evidence in capture runbook
```

## Next Recommended UOW

Surface live-capture preflight runbook row evidence inside the value-reader executor capture acceptance matrix. Keep the unit metadata-only: the matrix should continue consuming `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContract`, but its rows can preserve runbook evidence that now includes runtime-comparison handoff rows, executor consistency audit rows, executor bridge rows, result-emission blocker, materialization blocker, projected-value row, and accepted-boundary-row handoff data.

Safe candidates:

- Inspect `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService` and its tests.
- Update the matrix to include live-capture preflight row evidence without running Java capture, C# live capture, runtime comparison, or executable implementation.
- Keep validation focused on capture acceptance matrix tests plus live-capture preflight runbook tests.
