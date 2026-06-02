# Phase 6 Session 2238 Handoff - Value Reader Executor Materialization Preflight

Date: 2026-06-02
Unit of Work: UOW-2238
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

Run Java/Maven only when Java source or fixtures changed, or when a narrow Java source-of-truth command exists for the touched behavior. Record the skip reason when Java is not run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractService` lists the runtime evidence still required before output rows can materialize.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractService` previews blocked `Matched`, missing-row, `FieldMismatch`, and ignored-context outputs.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService` now joins intake and preview rows to document why no output can materialize before runtime prerequisites are satisfied.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the materialization preflight contract as existing non-live result-emission metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2238 Summary

This UOW added a value-reader executor materialization preflight contract.

Key behavior:

- default preflight blocks until runtime-evidence intake is ready,
- every blocked-output preview row remains non-materializable and non-emittable,
- `Matched` and `FieldMismatch` require value projection plus runtime comparison,
- missing-row outputs require row identity/missing-row decisions plus runtime comparison,
- ignored runtime context cannot materialize as a standalone output.

Important notes:

- This is metadata only.
- It does not parse Java JSON values or access C# export values.
- It does not implement readers, compare fields, emit results, attach context, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2238-Completion.md`
- `docs/Phase-6-Session-2238-Handoff.md`

## Validation In UOW-2238

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only adds C# non-live materialization preflight metadata derived from reviewed Java action `2`/`6` sources.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live materialization preflight metadata surface; the focused filter covers the new preflight plus directly adjacent runtime-evidence intake, blocked-output preview, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 20, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService` | Client Packet Boundary / Materialization Preflight Metadata | Partial | Unit Tested | Partial Parity | Preflight documents why output rows cannot materialize without Java/C# runtime evidence, but it does not execute live boundary dispatch or compare rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService` | Service Mutation / Output Materialization Prerequisites | Partial | Unit Tested | Partial Parity | Preflight records required row pairing, value projection, missing-row decisions, context attachment, and runtime comparison, but all runtime evidence remains missing. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Materialization preflight rows, runtime-evidence intake rows, blocked-output preview rows, implementation plan rows, executor readiness gate rows, comparator preflight rows, result schema rows, implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The materialization preflight contract is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate runtime row pairing, typed reader behavior, field types, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor result emission gate that consumes materialization preflight and states the exact result-emission conditions that must be true before any `Matched`, missing-row, `FieldMismatch`, or ignored-context row can be emitted.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2238] Add find group value reader materialization preflight
```
