# Phase 6 Session 2238 Completion - Value Reader Executor Materialization Preflight

Date: 2026-06-02
Unit of Work: UOW-2238
Status: Completed

## Scope

This unit added a non-live materialization preflight contract for the future `CM_FIND_GROUP` action `2` and action `6` value-reader executor.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not implement value readers, read Java JSON values, read C# trace-export values, compare rows, materialize output rows, emit results, or wire live C# `CmFindGroup` dispatch.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService`.
- The preflight joins runtime-evidence intake rows with blocked-output preview rows for:
  - `Matched`,
  - `MissingJavaRow`,
  - `MissingCSharpRow`,
  - `FieldMismatch`,
  - `IgnoredRuntimeContext`.
- It records output-specific prerequisites so future materialization cannot treat runtime-evidence metadata or preview rows as real results.
- It keeps output materialization, result emission, verified parity, and live dispatch disabled.
- Runtime evidence checklist provider metadata now names the materialization preflight contract as existing non-live result-emission metadata.
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeEvidenceIntakeContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorBlockedOutputPreviewContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture changed, and the unit only adds C# non-live materialization preflight metadata derived from reviewed Java action `2`/`6` sources.
- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live materialization preflight metadata surface; the focused filter covers the new preflight plus directly adjacent runtime-evidence intake, blocked-output preview, and runtime-evidence provider mapping.

Result:

- Focused C# command: passed 20, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService` | Client Packet Boundary / Materialization Preflight Metadata | Partial | Unit Tested | Partial Parity | Preflight documents why output rows cannot materialize without Java/C# runtime evidence, but it does not execute live boundary dispatch or compare rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService` | Service Mutation / Output Materialization Prerequisites | Partial | Unit Tested | Partial Parity | Preflight records required row pairing, value projection, missing-row decisions, context attachment, and runtime comparison, but all runtime evidence remains missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests.Create_DefaultPreflightBlocksUntilRuntimeEvidenceIntakeIsReady` | C# unit | Java action `2`/`6` source context | Default preflight is non-live, has intake/preview metadata, and cannot materialize or emit output. | Focused C# test. | No runtime rows. |
| `Create_DefaultPreflightListsEveryBlockedPreviewOutput` | C# unit | Existing blocked-output preview metadata | Preflight lists all five preview output kinds and keeps every row blocked. | Focused C# test. | Metadata only. |
| `Create_RuntimeMissingPreflightMapsOutputSpecificBlockers` | C# unit | Java mutation-post source context | Runtime-missing preflight maps matched/mismatch to value-projection blockers, missing rows to missing-row decisions, and ignored context to context attachment blockers. | Focused C# test. | Runtime evidence is absent. |
| `Create_FieldMismatchRequiresValueProjectionAndRuntimeComparison` | C# unit | Java action `2`/`6` equality comparison target | FieldMismatch requires projected Java/C# values, diagnostic context attachment, and runtime comparison before materialization. | Focused C# test. | No values are read. |
| `Create_OutputPreviewReadyStillBlocksRuntimeComparisonAndEmission` | C# unit | Executor output emission intentionally deferred | Even when output preview is ready-shaped, runtime comparison and emission remain blocked. | Focused C# test. | No comparison execution. |
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_CSharpBoundaryAndRegistryRowsStayNonLive` | C# unit | Java action `2`/`6` source context | Runtime evidence checklist maps result emission to the materialization preflight provider. | Focused C# test. | Runtime result evidence missing. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 source artifacts reviewed
- Total artifacts ported in this UOW: 1 C# non-live value-reader executor materialization preflight service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 2 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, row identity matching, value projection, output materialization, result emission, runtime/socket comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The materialization preflight contract is metadata only and cannot prove parity.
- No Java JSON values or C# trace-export values are read.
- Future implementation must validate runtime row pairing, typed-reader behavior, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor result emission gate that consumes materialization preflight and states the exact result-emission conditions that must be true before any `Matched`, missing-row, `FieldMismatch`, or ignored-context row can be emitted.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2238-Completion.md`
- `docs/Phase-6-Session-2238-Handoff.md`
