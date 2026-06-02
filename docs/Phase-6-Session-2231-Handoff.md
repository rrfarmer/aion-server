# Phase 6 Session 2231 Handoff - Value Reader Implementation Runbook

Date: 2026-06-02
Unit of Work: UOW-2231
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
- `FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService` now orders future typed scalar, ordered-list, enum/string, and mismatch-context implementation phases while keeping all reads and outputs disabled.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the implementation runbook as existing non-live metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2231 Summary

This UOW added a value-reader implementation runbook contract.

Key behavior:

- typed scalar equality readers are ordered before ordered-list readers,
- ordered-list readers are separated so `visibleEntryObjectIdsAfterMutation` ordering remains explicit,
- enum/string readers are grouped with exact Java JSON name/case preservation called out,
- mismatch-context attachment remains last and requires real missing-row or field-mismatch results,
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

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2231-Completion.md`
- `docs/Phase-6-Session-2231-Handoff.md`

## Validation In UOW-2231

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationReadinessChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires C# non-live runbook metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The implementation runbook is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate field types, missing fields, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, row identity, and real value projection before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader result schema contract that defines the future projected-value output rows for `Matched`, `FieldMismatch`, `MissingJavaRow`, `MissingCSharpRow`, and ignored runtime context without executing comparison.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2231] Add find group value reader implementation runbook
```
