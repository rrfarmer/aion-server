# Phase 6 Session 2231 Completion - Value Reader Implementation Runbook

Date: 2026-06-02
Unit of Work: UOW-2231
Status: Completed

## Scope

This unit added a non-live value-reader implementation runbook contract for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not implement readers, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, does not attach context, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService`.
- The runbook orders future implementation phases:
  - typed scalar equality readers,
  - ordered integer-list equality readers,
  - enum/string equality readers,
  - mismatch-context attachment.
- It computes schema-v1 counts from existing preflight metadata: 38 equality fields and 4 runtime-context fields.
- It keeps implementation, Java/C# value reads, comparison, context attachment, and result emission disabled.
- Runtime evidence checklist provider metadata now names the implementation runbook alongside the value-reader summary, preflight, mismatch-context preflight, and implementation-readiness checklist.
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live runbook metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live metadata/runbook surface; the focused filter covers the new runbook plus adjacent implementation checklist, typed-reader preflight, mismatch-context preflight, readiness summary, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 28, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` | Client Packet Boundary / Value Reader Implementation Runbook | Partial | Unit Tested | Partial Parity | Runbook orders future typed scalar, ordered-list, enum/string, and mismatch-context implementation work. No live boundary dispatch, runtime row value reads, context attachment, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` | Service Mutation / Value Reader Implementation Runbook | Partial | Unit Tested | Partial Parity | Action `2` implementation sequencing is metadata only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names implementation runbook metadata, but runtime evidence and comparison remain missing. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` | Java Trace Serializer / Value Reader Metadata | Partial | Unit Tested | Partial Parity | Java serializer schema and existing C# preflight metadata inform runbook field counts. No JSON values are parsed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests.Create_DefaultRunbookBlocksBeforeReadinessSummary` | C# unit | Reviewed Java action `2`/`6` trace schema | Default runbook is non-live, reports 38 equality fields and 4 context fields, and keeps all execution flags disabled. | Focused C# test. | No runtime values or live rows. |
| `Create_OrdersReaderImplementationWithoutEnablingReads` | C# unit | Java serializer schema plus existing preflight metadata | Implementation order is typed scalar readers, ordered-list readers, enum/string readers, then mismatch-context attachment. | Focused C# test. | No reader implementation. |
| `Create_GroupsSchemaV1ReaderKindsByImplementationPhase` | C# unit | Existing preflight metadata from Java schema-v1 fields | Reader-kind groups have expected counts and preserve ordered-list handling. | Focused C# test. | No Java JSON or C# trace values read. |
| `Create_AttachesMismatchContextLastOnlyAfterRealResults` | C# unit | Java runtime context fields from serializer schema | Runtime context attachment is last and requires real missing-row or field-mismatch results. | Focused C# test. | No context values attached. |
| `Create_ReadyMetadataStillDefersRunbookExecution` | C# unit | Reader and context implementation remain intentionally deferred | Even runtime-evidence-ready metadata leaves runbook execution blocked. | Focused C# test. | No value comparison or result emission. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader` | C# unit | Java schema source review | Checklist maps value-reader readiness to summary, preflight, mismatch-context, implementation checklist, and implementation runbook providers. | Focused C# test. | Runtime value-reader evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader implementation runbook service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, context attachment, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The implementation runbook is planning metadata only and cannot prove parity.
- No Java JSON values or C# trace-export values are read.
- Future implementation must validate actual typed-reader behavior, field types, missing fields, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader result schema contract that defines the future projected-value output rows for `Matched`, `FieldMismatch`, `MissingJavaRow`, `MissingCSharpRow`, and ignored runtime context without executing comparison.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2231-Completion.md`
- `docs/Phase-6-Session-2231-Handoff.md`
