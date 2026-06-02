# Phase 6 Session 2233 Completion - Value Reader Comparator Preflight

Date: 2026-06-02
Unit of Work: UOW-2233
Status: Completed

## Scope

This unit added a non-live value-reader comparator preflight contract for future `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison execution.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not implement readers, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, does not attach context, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService`.
- The preflight maps existing runbook and result-schema metadata into future executor stages:
  - row identity pairing,
  - typed reader execution,
  - equality value comparison,
  - result selection,
  - mismatch-context attachment.
- It records 38 required equality fields and 4 runtime-context fields from the result schema.
- It keeps row pairing, value projection, comparison, context attachment, and result emission disabled.
- Runtime evidence checklist provider metadata now names the comparator preflight as part of result-emission readiness metadata.
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live comparator-preflight metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live metadata/comparator-preflight surface; the focused filter covers the new comparator preflight plus adjacent runbook, result schema, result skeleton, blocked result report, execution result contract, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 38, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService` | Client Packet Boundary / Value Reader Comparator Preflight | Partial | Unit Tested | Partial Parity | Comparator preflight defines future executor stage order only. No live boundary dispatch, runtime row value reads, context attachment, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService` | Service Mutation / Value Reader Comparator Preflight | Partial | Unit Tested | Partial Parity | Action `2` comparator sequencing is metadata only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names comparator preflight metadata, but runtime evidence and comparison remain missing. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService` | Java Trace Serializer / Value Reader Metadata | Partial | Unit Tested | Partial Parity | Java serializer schema and existing C# metadata inform preflight field counts and stage blockers. No JSON values are parsed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests.Create_DefaultPreflightBlocksBeforeResultSchemaReadiness` | C# unit | Reviewed Java action `2`/`6` trace schema | Default comparator preflight is non-live, reports 38 equality fields and 4 context fields, and keeps all executor flags disabled. | Focused C# test. | No runtime values or live rows. |
| `Create_OrdersFutureComparatorStagesWithoutExecuting` | C# unit | Existing runbook/result-schema metadata | Future executor stages are row identity pairing, typed reader execution, equality comparison, result selection, and context attachment. | Focused C# test. | No comparator implementation. |
| `Create_ValueComparisonStageRequiresProjectedValuesAndMatchedOrMismatchOutput` | C# unit | Java/C# result contract source review | Equality comparison requires projected values and can only feed Matched or FieldMismatch output kinds. | Focused C# test. | No values are projected. |
| `Create_ResultSelectionAndContextAttachmentStaySeparated` | C# unit | Java serializer runtime context fields | Result selection and context attachment remain distinct; context cannot affect equality. | Focused C# test. | No context attachment. |
| `Create_ReadyResultSchemaStillDefersEveryComparatorStage` | C# unit | Comparator implementation remains intentionally deferred | Ready result-schema metadata changes stage statuses but still blocks runtime rows, reader execution, comparison, result selection, and context attachment. | Focused C# test. | No comparison execution. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_CSharpBoundaryAndRegistryRowsStayNonLive` | C# unit | Java schema source review | Runtime evidence checklist maps result emission to result skeleton, blocked report, value-reader result schema, and comparator preflight providers. | Focused C# test. | Runtime result evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader comparator preflight service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, row identity matching, value comparison, context attachment, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The value-reader comparator preflight is planning metadata only and cannot prove parity.
- No Java JSON values or C# trace-export values are read.
- Future implementation must validate actual runtime row pairing, typed-reader behavior, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor readiness gate that combines the comparator preflight, runtime evidence checklist, and live-input handoff into a go/no-go report before any value-reader executor implementation.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2233-Completion.md`
- `docs/Phase-6-Session-2233-Handoff.md`
