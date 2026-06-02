# Phase 6 Session 2214 Completion - FindGroup Mutation Projected Row Value Contract

Date: 2026-06-02
Unit of Work: UOW-2214
Status: Completed

## Scope

This unit added a non-live projected-row comparison value contract for action `2`/`6` mutation-post traces.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not read Java/C# row values, and does not emit real `Matched` or `FieldMismatch` results.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueContractService`.
- Added value contract records and statuses for:
  - executor skeleton not ready,
  - missing value sources,
  - future value projection deferred.
- Names required Java and C# value sources for every required equality field in the action `2`/`6` result contract.
- Keeps runtime-only fields such as `traceSource` and `serverEpochSeconds` as ignored context.
- Keeps `CanProjectValues=false`, `CanEmitMatched=false`, and `CanEmitFieldMismatch=false`.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live value-source contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and this service only consumes existing Java-derived field/action metadata without executing Java logic.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live value-source contract; the focused filter covers the new value contract, its executor skeleton input, dry-run/readiness dependencies, result skeleton, and comparison result contract adjacency.

Result:

- Focused C# command: passed 29, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Client Packet Boundary / Value Contract | Partial | Unit Tested | Partial Parity | Value contract names required Java/C# value sources for action `2`/`6` equality fields, but does not read values, compare rows, or emit results. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Service Mutation / Value Contract | Partial | Unit Tested | Partial Parity | Action `2` field value sources are named from Java-derived trace fields such as posted system message and refreshed recruitment list, but runtime values remain unprojected. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService` | Service Mutation / Value Contract | Partial | Unit Tested | Partial Parity | Action `6` field value sources are named from Java-derived trace fields such as posted system message and refreshed application list, but runtime values remain unprojected. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueContractServiceTests.Create_DefaultContractBlocksBeforeValueProjection` | C# unit | Java action `2`/`6` comparison remains future work | Default value contract blocks before value projection and cannot emit match/mismatch rows. | Focused C# test. | No live boundary execution. |
| `Create_NamesRequiredEqualityValueSourcesWithoutReadingValues` | C# unit | Java-derived result field contract | Required equality fields require Java and C# values but keep emission disabled. | Focused C# test. | No values are read. |
| `Create_KeepsRuntimeOnlyFieldsAsIgnoredContext` | C# unit | Java/C# trace schema runtime fields | Runtime-only fields are not equality inputs. | Focused C# test. | No mismatch context materialized. |
| `Create_PairedInputsAreValueProjectionDeferredNotCompared` | C# unit | Java action `2`/`6` mutation identity reviewed from source | Paired inputs can reach future-projection-deferred status but still cannot emit results. | Focused C# test with synthetic executor rows. | No real Java/C# row comparison. |
| `Create_ReadyExecutorMissingOnePairBlocksValueSources` | C# unit | Future comparison needs both Java and C# row values | Missing action pair blocks value sources before comparison. | Focused C# test. | No runtime/socket evidence. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# value contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, real projected-row value projection/comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The value contract is metadata only and cannot prove Java/C# runtime parity.
- Shape-valid Java rows and synthetic C# rows in tests prove value-source naming shape, not runtime behavior.
- Live C# trace-row emitter, registry send observation, real projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison blocked-result report that combines the executor skeleton and value contract into one final pre-execution report, explicitly listing why `Matched` and `FieldMismatch` outputs remain unavailable.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2214-Completion.md`
