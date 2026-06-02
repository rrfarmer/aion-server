# Phase 6 Session 2232 Handoff - Value Reader Result Schema

Date: 2026-06-02
Unit of Work: UOW-2232
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
- `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistService` separates typed equality-reader implementation blockers from mismatch-context attachment blockers.
- `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` orders future typed scalar, ordered-list, enum/string, and mismatch-context implementation phases while keeping all reads and outputs disabled.
- `FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService` now defines future output row shapes while keeping value projection, context attachment, comparison, and result emission disabled.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the value-reader result schema as existing non-live result-emission metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2232 Summary

This UOW added a value-reader result schema contract.

Key behavior:

- `Matched` requires projected Java/C# equality values and does not allow runtime context,
- `FieldMismatch` requires projected Java/C# equality values and may attach runtime context after a mismatch is selected,
- `MissingJavaRow` and `MissingCSharpRow` require row-identity decisions and may attach runtime context after the missing-row result exists,
- ignored runtime context is not a standalone result and cannot enable `Matched`,
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

- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonResultSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonBlockedResultReportService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2232-Completion.md`
- `docs/Phase-6-Session-2232-Handoff.md`

## Validation In UOW-2232

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live result-schema metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader result schema rows, implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The value-reader result schema is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate field types, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader comparator preflight contract that maps the runbook and result schema into future executor stages while keeping comparison execution disabled.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2232] Add find group value reader result schema
```
