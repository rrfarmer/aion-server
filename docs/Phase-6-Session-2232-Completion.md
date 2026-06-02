# Phase 6 Session 2232 Completion - Value Reader Result Schema

Date: 2026-06-02
Unit of Work: UOW-2232
Status: Completed

## Scope

This unit added a non-live value-reader result schema contract for future `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison output rows.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonResultSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonBlockedResultReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not implement readers, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, does not attach context, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService`.
- The schema defines future row shapes for:
  - `Matched`,
  - `MissingJavaRow`,
  - `MissingCSharpRow`,
  - `FieldMismatch`,
  - ignored runtime context.
- It records 38 required equality fields and 4 runtime-context fields from the existing result contract.
- It keeps value projection, missing-row decisions, context attachment, and result emission disabled.
- Runtime evidence checklist provider metadata now names the value-reader result schema as part of result-emission readiness metadata.
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live result-schema metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live metadata/result-schema surface; the focused filter covers the new result schema plus adjacent implementation runbook, result skeleton, blocked result report, execution result contract, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 33, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService` | Client Packet Boundary / Value Reader Result Schema | Partial | Unit Tested | Partial Parity | Result schema defines future output row shapes only. No live boundary dispatch, runtime row value reads, context attachment, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService` | Service Mutation / Value Reader Result Schema | Partial | Unit Tested | Partial Parity | Action `2` result output shape is metadata only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names value-reader result schema metadata, but runtime evidence and comparison remain missing. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService` | Java Trace Serializer / Value Reader Metadata | Partial | Unit Tested | Partial Parity | Java serializer schema and existing C# result metadata inform output-row fields. No JSON values are parsed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests.Create_DefaultSchemaBlocksBeforeRunbookReadiness` | C# unit | Reviewed Java action `2`/`6` trace schema | Default schema is non-live, reports 38 equality fields and 4 context fields, and keeps all output flags disabled. | Focused C# test. | No runtime values or live rows. |
| `Create_DefinesRowsForEveryFutureOutputKindWithoutEmitting` | C# unit | Existing result contract metadata | Schema rows exist for matched, missing Java, missing C#, field mismatch, and ignored runtime context. | Focused C# test. | No result emission. |
| `Create_MatchedAndFieldMismatchRowsRequireProjectedValues` | C# unit | Java/C# result contract source review | Matched and FieldMismatch shapes require projected values; only FieldMismatch may carry context. | Focused C# test. | No values are projected. |
| `Create_MissingRowsRequireRowDecisionAndAllowContext` | C# unit | Java action row identity metadata | Missing-row outputs require a row-identity decision and may carry context only after that decision. | Focused C# test. | No row matcher exists. |
| `Create_IgnoredRuntimeContextIsNotStandaloneResult` | C# unit | Java serializer runtime context fields | `traceSource` and `serverEpochSeconds` are context only and cannot emit standalone results. | Focused C# test. | No context attachment. |
| `Create_ReadyRunbookStillDefersResultEmission` | C# unit | Reader/result implementation remains intentionally deferred | Ready runbook metadata changes row statuses but still blocks value projection and result emission. | Focused C# test. | No comparison execution. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_CSharpBoundaryAndRegistryRowsStayNonLive` | C# unit | Java schema source review | Runtime evidence checklist maps result emission to result skeleton, blocked report, and value-reader result schema providers. | Focused C# test. | Runtime result evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader result schema service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, context attachment, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The value-reader result schema is planning metadata only and cannot prove parity.
- No Java JSON values or C# trace-export values are read.
- Future implementation must validate actual typed-reader behavior, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader comparator preflight contract that maps the runbook and result schema into future executor stages while keeping comparison execution disabled.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2232-Completion.md`
- `docs/Phase-6-Session-2232-Handoff.md`
