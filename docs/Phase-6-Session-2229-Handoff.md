# Phase 6 Session 2229 Handoff - Value Reader Mismatch Context Preflight

Date: 2026-06-02
Unit of Work: UOW-2229
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Session startup is not a validation trigger. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit.

Use focused validation by default:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single service/planner/schema/test change: run filtered `dotnet test` for that test class plus directly adjacent classes only.
- Packet/parser or boundary change: run only the immediately related packet/parser/boundary tests.
- Java parity check: run a targeted Maven test only when a narrow Java fixture or source-of-truth command exists.
- Full project test, solution test, or solution build: run only after documenting a broad-validation trigger from `docs/orchestration-rules.md`.

Filtered `dotnet test` commands already build the affected project and dependencies. Treat a passing filtered test command as the compile signal unless a named broad-validation trigger requires wider validation.

Do not run `dotnet build dotnetConversion\AionServer.slnx` after a passing filtered test command merely to confirm compilation.

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` enumerates concrete schema-v1 typed readers for future Java JSON and C# trace-export value projection without reading values.
- `FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService` now names `traceSource` and `serverEpochSeconds` as runtime-only diagnostic context that can attach only after a real missing-row or field-mismatch result exists.
- `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService` now includes mismatch-context preflight as stage 3, between typed-reader preflight and skeleton metadata.
- `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` and `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now name mismatch-context preflight as existing non-live metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2229 Summary

This UOW added mismatch-context preflight metadata for future value-reader diagnostics.

Key behavior:

- `traceSource` and `serverEpochSeconds` are runtime-only context fields,
- they are not equality inputs,
- they may attach only after `MissingJavaRow`, `MissingCSharpRow`, or `FieldMismatch`,
- they do not attach to `Matched`,
- Java/C# value reading, comparison, context attachment, result emission, and live dispatch remain disabled.

Important notes:

- This is metadata only.
- It does not parse Java JSON values or access C# export values.
- It does not compare fields, emit results, attach context, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostComparisonKeyProjectionMetadataService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonBlockedResultReportService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService`

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

## Validation In UOW-2229

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderMismatchContextPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only wires existing C# non-live metadata based on reviewed Java action/schema sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader preflight rows, mismatch-context preflight rows, value-reader handoff/checklist rows, value-reader readiness summary rows, value-reader blocked report rows, value-reader skeleton attempts, value-reader design rows, execution-readiness gate rows, runtime evidence checklist rows, live-input handoff rows, readiness summary rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The mismatch-context preflight is now included in readiness/checklist metadata, but it cannot prove parity.
- Future implementation must validate field types, missing fields, collection ordering, ignored runtime context attachment, and row identity before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader implementation readiness checklist that separates typed-reader implementation blockers from mismatch-context attachment blockers before any reader code is written.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2229] Add find group value reader mismatch context preflight
```
