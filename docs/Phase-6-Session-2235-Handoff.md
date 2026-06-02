# Phase 6 Session 2235 Handoff - Value Reader Executor Implementation Plan

Date: 2026-06-02
Unit of Work: UOW-2235
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
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorReadinessGateService` combines comparator preflight, runtime evidence checklist, and live-input handoff metadata into a blocked go/no-go report before value-reader executor implementation.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService` now enumerates concrete future executor tasks for row pairing, Java/C# typed reads, equality comparison, result selection, mismatch-context attachment, and result emission.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the value-reader executor implementation plan as existing non-live result-emission metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2235 Summary

This UOW added a value-reader executor implementation plan contract.

Key behavior:

- row identity pairing must precede all value reads,
- Java and C# typed reads are separate implementation tasks,
- equality comparison must happen before result selection,
- runtime context attachment is diagnostic only and must follow a missing-row or mismatch result,
- result emission remains blocked until runtime-backed Java and C# rows can be compared.

Important notes:

- This is metadata only.
- It does not parse Java JSON values or access C# export values.
- It does not implement readers, compare fields, emit results, attach context, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorReadinessGateService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2235-Completion.md`
- `docs/Phase-6-Session-2235-Handoff.md`

## Validation In UOW-2235

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderComparatorPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderImplementationRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only adds C# non-live executor implementation-plan metadata derived from reviewed Java action/schema sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live implementation-plan metadata surface; the focused filter covers the new plan plus directly adjacent executor readiness gate, comparator preflight, result schema, implementation runbook, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 31, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService` | Client Packet Boundary / Value Reader Executor Implementation Plan | Partial | Unit Tested | Partial Parity | Plan enumerates future executor tasks only. No live boundary dispatch, runtime value reads, comparison, result emission, socket comparison, or verified parity evidence exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService` | Service Mutation / Executor Plan Metadata | Partial | Unit Tested | Partial Parity | Action `2` plan references Java recruitment mutation and refreshed-list behavior, but no Java/C# runtime row values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` checklist now names the value-reader executor implementation plan as existing non-live metadata, but runtime evidence and comparison remain missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader executor implementation plan rows, executor readiness gate rows, comparator preflight rows, result schema rows, implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The value-reader executor implementation plan is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate runtime row pairing, typed reader behavior, field types, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor blocked-output preview contract that consumes the implementation plan and result schema to show which `Matched`, `MissingJavaRow`, `MissingCSharpRow`, `FieldMismatch`, and ignored-context outputs remain unavailable before runtime rows exist.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2235] Add find group value reader executor implementation plan
```
