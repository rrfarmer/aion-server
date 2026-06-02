# Phase 6 Session 2229 Completion - Value Reader Mismatch Context Preflight

Date: 2026-06-02
Unit of Work: UOW-2229
Status: Completed

## Scope

This unit added a non-live value-reader mismatch-context preflight contract for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonKeyProjectionMetadataService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonBlockedResultReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, does not attach context, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService`.
- The new contract names runtime-only context fields:
  - `traceSource`
  - `serverEpochSeconds`
- It permits context attachment only after a real future result exists:
  - `MissingJavaRow`
  - `MissingCSharpRow`
  - `FieldMismatch`
- It explicitly keeps context fields out of equality, reads no Java/C# values, attaches no context, and emits no comparison result.
- Added `MismatchContextPreflightContract` as the third value-reader readiness stage.
- Updated live-input handoff and runtime-evidence checklist provider metadata to include mismatch-context preflight.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires existing C# non-live metadata based on reviewed Java action/schema sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited services are non-live metadata/readiness/reporting surfaces; the focused filter covers the new mismatch-context contract, adjacent typed-reader preflight/readiness, the handoff row that consumes the summary, and the checklist mapping that names providers.

Result:

- Focused C# command: passed 25, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService` | Client Packet Boundary / Value Reader Context Preflight | Partial | Unit Tested | Partial Parity | Mismatch-context preflight names runtime-only fields for future diagnostic attachment. No live boundary dispatch, runtime row value reads, context attachment, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService` | Service Mutation / Value Reader Readiness | Partial | Unit Tested | Partial Parity | Action `2` readiness now includes mismatch-context metadata as non-live planning only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names the mismatch-context preflight provider, but runtime evidence and comparison remain missing. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService` | Java Trace Serializer / Runtime Context Metadata | Partial | Unit Tested | Partial Parity | Java serializer schema includes `traceSource` and `serverEpochSeconds`; C# records them as runtime-only context, not equality values. No JSON values are parsed. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests.Create_DefaultContextPreflightBlocksBeforeTypedReaderPreflightReadiness` | C# unit | Java action `2`/`6` schema source review | Default contract is non-live, names `traceSource`/`serverEpochSeconds`, and keeps reads/context/result emission disabled. | Focused C# test. | No runtime values or live rows. |
| `Create_AllowsOnlyMissingRowsAndFieldMismatchAsContextTriggers` | C# unit | Existing result contract and blocked-result report shape | Context attachment is limited to MissingJavaRow, MissingCSharpRow, and FieldMismatch; not Matched. | Focused C# test. | No real comparison results. |
| `Create_MapsTraceSourceAndServerEpochSecondsAsRuntimeOnlyContext` | C# unit | Java serializer schema field review | Runtime-only fields keep Java JSON paths, C# accessors, and non-equality rules. | Focused C# test. | No JSON parsing. |
| `Create_RuntimeEvidenceReadyPreflightStillDefersContextAttachment` | C# unit | Reader implementation remains intentionally deferred | Even runtime-evidence-ready metadata cannot attach context before a real result exists. | Focused C# test. | No context attachment implementation. |
| `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests.Create_DefaultSummaryListsEachValueReaderStage` | C# unit | Java serializer schema and result context rules | Readiness stages now include mismatch-context preflight with disabled read/attach flags. | Focused C# test. | Non-live metadata only. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader` | C# unit | Java schema source review | Checklist maps value-reader readiness to readiness, typed-reader preflight, and mismatch-context preflight providers. | Focused C# test. | Runtime value-reader evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live mismatch-context preflight contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, context attachment, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The mismatch-context preflight is planning metadata only and cannot prove parity.
- No Java JSON values or C# trace-export values are read or attached.
- Future implementation must validate actual result selection, field types, missing fields, collection ordering, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader implementation readiness checklist that separates typed-reader implementation blockers from mismatch-context attachment blockers before any reader code is written.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2229-Completion.md`
- `docs/Phase-6-Session-2229-Handoff.md`
