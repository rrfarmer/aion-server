# Phase 6 Session 2233 Handoff - Value Reader Comparator Preflight

Date: 2026-06-02
Unit of Work: UOW-2233
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Use focused validation by default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit. A passing filtered `dotnet test` command is the compile signal for its affected project and dependencies unless a documented broad-validation trigger applies.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` enumerates concrete schema-v1 typed readers for future Java JSON and C# trace-export value projection without reading values.
- `FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService` names `traceSource` and `serverEpochSeconds` as runtime-only diagnostic context that can attach only after a real missing-row or field-mismatch result exists.
- `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` orders future typed scalar, ordered-list, enum/string, and mismatch-context implementation phases while keeping all reads and outputs disabled.
- `FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService` defines future output row shapes while keeping value projection, context attachment, comparison, and result emission disabled.
- `FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService` now maps runbook and result-schema metadata into future executor stages while keeping row pairing, reader execution, value comparison, result selection, context attachment, and result emission disabled.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the comparator preflight as existing non-live result-emission metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2233 Summary

This UOW added a value-reader comparator preflight contract.

Key behavior:

- row identity pairing must precede reader execution,
- typed reader execution must precede equality comparison,
- equality comparison can feed only `Matched` or `FieldMismatch`,
- result selection remains separate from context attachment,
- ignored runtime context cannot affect equality or stand alone as a result,
- Java/C# value reading, comparison, context attachment, result emission, and live dispatch remain disabled.

Important notes:

- This is metadata only.
- It does not parse Java JSON values or access C# export values.
- It does not implement readers, compare fields, emit results, attach context, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2233-Completion.md`
- `docs/Phase-6-Session-2233-Handoff.md`

## Validation In UOW-2233

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live comparator-preflight metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader comparator preflight rows, result schema rows, implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The value-reader comparator preflight is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate runtime row pairing, typed reader behavior, field types, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor readiness gate that combines the comparator preflight, runtime evidence checklist, and live-input handoff into a go/no-go report before any value-reader executor implementation.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2233] Add find group value reader comparator preflight
```
