# Phase 6 Session 2245 Completion - Value Reader Executor Capture Acceptance Matrix

## Scope

Added a non-live capture acceptance matrix for future `CM_FIND_GROUP` action `2`/`6` value-reader executor evidence. The matrix turns the live-capture preflight runbook into explicit pass/fail evidence fields and keeps verified parity blocked until objective runtime evidence exists.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

## Changes

Added `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService`.

The matrix maps live-capture preflight steps to evidence fields:

- Java artifact rows
- Java artifact shape validation
- C# boundary rows
- Boundary executor observation
- Registry send observation
- Row identity matching
- Value projection
- Result materialization
- Result emission
- Runtime comparison execution
- Executable implementation gate

Every evidence field currently fails or blocks because no live runtime evidence has been captured for the value-reader executor path. The runtime evidence checklist now names the acceptance matrix provider and includes capture acceptance matrix text in the required next evidence and notes. The live dispatch design document now records the acceptance matrix as the current non-live gate before any executable implementation.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched. Phase 6 startup should continue to use the latest session completion and handoff documents rather than the historical progress archive.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests
- Adjacent runtime evidence checklist provider mapping
- Design/session documentation

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 19, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed, and this unit only adds C# non-live acceptance metadata derived from reviewed Java action `2`/`6` sources.

Broad validation trigger: none. A full `.NET` build or full test suite was intentionally skipped because this UOW touched a narrow non-live service/test surface and focused validation covered the new matrix plus adjacent preflight, handoff, and provider mapping.

`git diff --check`: passed with the repository's usual CRLF warnings.

## Parity Table Updates

| Java Source | C# Port | Type | Port Status | Test Status | Parity | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService` | Client Packet Boundary / Capture Acceptance Metadata | Partial | Unit Tested | Partial Parity | Names pass/fail evidence fields and blockers, but no live dispatch/value comparison/result emission evidence exists yet. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorCaptureAcceptanceMatrixContractService` | Service Mutation / Runtime Comparison Blocker Metadata | Partial | Unit Tested | Partial Parity | Records blockers for artifact rows, boundary rows, registry observation, value projection, materialization, emission, runtime comparison, executor implementation, and live dispatch. |

## Test Documentation

New tests:

- `Create_DefaultMatrixBlocksUntilLiveCapturePreflightIsReady`
- `Create_DefaultMatrixListsEveryAcceptanceFieldAsBlocked`
- `Create_RuntimeMissingMatrixNamesCaptureEvidenceFieldsAndBlockers`
- `Create_RuntimeMissingMatrixSeparatesProjectionMaterializationEmissionAndComparison`
- `Create_ReadyShapedRunbookStillBlocksRuntimeComparisonAndExecutableImplementation`

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_ResultEmissionChecklistRequiresRuntimeProviderAndEvidence`

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: live Java trace artifacts from supplied artifact-root capture command, live C# boundary rows, executor observation, registry observation, row identity, value projection, materialization, emission, runtime/socket comparison, executor implementation, and live dispatch.

## Next Recommended UOW

Add a non-live value-reader executor capture execution blocker summary that rolls the acceptance matrix into a single go/no-go report for runtime comparison execution and names the smallest next evidence-producing command.

Safe candidates:

- Add a deterministic Java artifact timestamp override for `serverEpochSeconds`.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
