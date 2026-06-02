# Phase 6 Session 2221 Handoff - FindGroup Mutation Value Reader Design Contract

Date: 2026-06-02
Unit of Work: UOW-2221
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
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService` consumes paired-readiness rows and emits blocked planned rows for missing Java input, missing C# input, or deferred value comparison.
- `FindGroupMutationPostProjectedRowComparisonValueContractService` names required Java/C# value sources for every required equality field and keeps runtime-only fields as ignored context.
- `FindGroupMutationPostProjectedRowComparisonBlockedResultReportService` combines the executor skeleton and value contract into a final non-live pre-execution report with all planned output rows marked unavailable.
- `FindGroupMutationPostProjectedRowComparisonReadinessSummaryService` links the dry-run, executor skeleton, value contract, and blocked-result report into one top-level readiness summary.
- `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` enumerates the exact runtime artifacts required before summary metadata can become real comparison execution.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` maps each live-input requirement to existing non-live providers or future runtime evidence producers.
- `FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateService` combines the handoff/checklist into a final non-live go/no-go report before comparator implementation.
- `FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService` now names Java JSON paths and C# trace-export accessors for each future value read.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2221 Summary

This UOW added a non-live value-reader design contract.

Key behavior:

- emits one design row per value-contract field,
- maps Java reads to `$.traces[*].fieldName`,
- maps C# reads to `FindGroupDirectPacketMutationPostBoundaryTraceExport.PropertyName`,
- keeps runtime-only context fields ignored for equality,
- keeps Java reads, C# reads, value comparison, and live dispatch disabled.

Important notes:

- The contract is a reader design only.
- It does not parse Java JSON values or access C# export values.
- It does not compare fields, emit results, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupDirectPacketMutationPostBoundaryTraceSchemaService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactValidatorService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2221-Completion.md`
- `docs/Phase-6-Session-2221-Handoff.md`

## Validation In UOW-2221

Validation decision:

- Changed surface: C# service/test-only non-live value-reader design contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only maps reviewed Java serializer/C# trace-export field names to future reader paths.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live value-reader design contract; the focused filter covers the new contract, the value contract, execution gate, runtime checklist, live-input handoff, and adjacent readiness/executor services.

Result:

- Focused C# command: passed 34, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService` | Client Packet Boundary / Value Reader Design | Partial | Unit Tested | Partial Parity | Contract names future Java JSON paths and C# trace-export property accessors. No live boundary dispatch, runtime row value reads, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService` | Service Mutation / Value Reader Design | Partial | Unit Tested | Partial Parity | Action `2` field reads are designed for future runtime rows only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService` | Service Mutation / Value Reader Design | Partial | Unit Tested | Partial Parity | Action `6` field reads are designed for future runtime rows only; no Java/C# values are read or compared. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService` | Java Trace Serializer / Value Reader Design | Partial | Unit Tested | Partial Parity | Serializer schema fields are mapped to Java JSON paths and C# trace-export accessors, but no runtime artifact values are read. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value-reader design rows, execution-readiness gate rows, runtime evidence checklist rows, live-input handoff rows, readiness summary rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The value-reader design makes field access targets visible, but it cannot prove parity.
- Future implementation must validate field types, missing fields, collection ordering, and ignored runtime context before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison value-reader implementation skeleton that consumes accepted Java/C# row references and returns blocked read attempts for each field without reading values yet.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2221] Add find group mutation value reader design contract
```
