# Phase 6 Session 2214 Handoff - FindGroup Mutation Projected Row Value Contract

Date: 2026-06-02
Unit of Work: UOW-2214
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Focused validation is the default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build unless a documented broad-validation trigger applies. Filtered `dotnet test` commands already build the affected project and dependencies.

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
- Broad .NET validation is not routine. Use focused test selection from `docs/orchestration-rules.md`.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService` consumes paired-readiness rows and emits blocked planned rows for missing Java input, missing C# input, or deferred value comparison.
- `FindGroupMutationPostProjectedRowComparisonValueContractService` names required Java/C# value sources for every required equality field and keeps runtime-only fields as ignored context.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`/`FieldMismatch` results.

## UOW-2214 Summary

This UOW added a non-live projected-row comparison value contract.

Key behavior:

- creates one value field per Java-derived result-contract field,
- requires Java and C# value sources for required equality fields,
- keeps `traceSource` and `serverEpochSeconds` as ignored runtime context,
- reports missing value sources when paired inputs are incomplete,
- reports future value projection deferred when paired inputs exist,
- keeps `CanProjectValues=false`, `CanEmitMatched=false`, and `CanEmitFieldMismatch=false`,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The value contract is a value-source report only.
- It does not read Java/C# values, compare fields, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.
- Shape-valid Java rows and synthetic C# rows in tests are not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostComparisonExecutionResultContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2214-Completion.md`
- `docs/Phase-6-Session-2214-Handoff.md`

## Validation In UOW-2214

Validation decision:

- Changed surface: C# service/test-only non-live value-source contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and this service only consumes existing Java-derived field/action metadata without executing Java logic.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live value-source contract; the focused filter covers the new value contract, its executor skeleton input, dry-run/readiness dependencies, result skeleton, and comparison result contract adjacency.

Result:

- Focused C# command: passed 29, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Client Packet Boundary / Value Contract | Partial | Unit Tested | Partial Parity | Value contract names required Java/C# value sources for action `2`/`6` equality fields, but does not read values, compare rows, or emit results. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Service Mutation / Value Contract | Partial | Unit Tested | Partial Parity | Action `2` field value sources are named from Java-derived trace fields such as posted system message and refreshed recruitment list, but runtime values remain unprojected. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Service Mutation / Value Contract | Partial | Unit Tested | Partial Parity | Action `6` field value sources are named from Java-derived trace fields such as posted system message and refreshed application list, but runtime values remain unprojected. |

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison blocked-result report that combines the executor skeleton and value contract into one final pre-execution report, explicitly listing why `Matched` and `FieldMismatch` outputs remain unavailable.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2214] Add find group mutation value contract
```
