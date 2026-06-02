# Phase 6 Session 2260 Completion - Projected Value Row Contract

## Scope

Added a non-live projected-value row contract for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderFunctionExecutionPreflightService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`

`docs/PHASE-6-PROGRESS.md` was intentionally not read or updated. Current working context remains in latest completion/handoff documents.

## Changes

Added:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostValueReaderProjectedValueRowContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests.cs`

Updated:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`

The new contract consumes:

- `FindGroupMutationPostValueReaderFunctionExecutionPreflight`
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContract`
- `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContract`

It defines 42 projected-value row shapes:

- 38 required equality field rows with Java/C# values set to `<not-read>`.
- 4 ignored runtime-context rows with Java/C# values set to `<ignored-runtime-context>`.

It records action, mutation kind, field name, reader kind, read mode, value type, planned Java/C# reader function names, read status, row requirements, ordering requirements, blockers, source breadcrumb, and evidence strings.

It remains non-live and never invokes reader functions, reads Java JSON values, reads C# trace-export values, compares rows, attaches context, materializes output, emits results, runs runtime comparison, enables live dispatch, or proves verified parity.

## Validation Decision

Changed surface:

- One C# non-live service/report plus unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostValueReaderProjectedValueRowContractServiceTests|FullyQualifiedName~FindGroupMutationPostValueReaderFunctionExecutionPreflightServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorImplementationPlanContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderResultSchemaContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorResultEmissionGateContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 37, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused hygiene validation:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing files, but no whitespace errors.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed directly as source-of-truth evidence for the projected row context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution build were skipped because the filtered C# command built the affected project and dependencies and covered the new projected-value row contract plus directly adjacent function execution preflight, executor implementation plan, result schema, materialization preflight, result-emission gate, and runtime evidence checklist services.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostValueReaderProjectedValueRowContractService` | Projected Value Row Metadata | Partial | Unit Tested | Partial Parity | Defines per-field projected-value row shapes for action `2` and action `6`; does not invoke readers, read values, compare rows, emit results, or prove runtime parity. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostValueReaderProjectedValueRowContractService` | Mutation Post Projected Value Metadata | Partial | Unit Tested | Partial Parity | Preserves Java-derived recruitment/application action mapping, mutation kind, system-message/refreshed-list context, and ordered-list field shape. Runtime-backed Java artifacts, accepted C# rows, actual value reads, comparison, output materialization, and verified parity remain blocked. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultContractBlocksBeforeFunctionExecutionPreflight` | Unit | Java action `2`/`6` mutation-post mapping | Default projected-value row contract blocks before function execution preflight and exposes 42 row shapes. | Non-live contract metadata. | No runtime values. |
| `Create_ReadyFunctionPreflightStillBlocksWhenExecutorPlanNotReady` | Unit | Java/C# reader invocation and executor plan metadata | Function preflight readiness alone cannot project values without executor implementation plan readiness. | Contract-level blocker evidence. | No executor implementation. |
| `Create_ReadyInputsDefineProjectedValueRowsWithoutReading` | Unit | Java `addRecruitment`/`addApplication` row identity and typed-reader field mapping | Ready inputs define unread Java/C# projected-value rows for action `2` and action `6` fields. | Non-live projected row metadata. | No Java JSON or C# trace values are read. |
| `Create_OrderedListProjectedRowPreservesCollectionOrder` | Unit | Java refreshed-list materialized packet ordering | Ordered list projected row requires preservation of `visibleEntryObjectIdsAfterMutation` order. | Conservative ordering metadata. | No list values are read or compared. |
| `Create_IgnoredRuntimeContextRowsStayOutOfEquality` | Unit | Runtime context rules from value-reader preflight | `traceSource` and related runtime context rows remain ignored for equality and cannot enable `Matched`. | Conservative context metadata. | No context attachment occurs. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_CSharpBoundaryAndRegistryRowsStayNonLive`

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services added: 1
- C# test classes added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: runtime-backed Java artifacts, accepted live C# boundary rows from production `ProcessPacketAsync`, runtime Java/C# row values, concrete reader invocation, value projection, comparison, materialization, result emission, runtime/socket comparison, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW clarifies projected-value row shape but adds no runtime evidence.

## Next Recommended UOW

Add a non-live projected-value materialization blocker report that consumes `FindGroupMutationPostValueReaderProjectedValueRowContractService` and `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorMaterializationPreflightContractService`, then records why each output kind still cannot materialize from the unread projected-value rows.

Safe candidates:

- Add a focused runtime evidence checklist update for projected-value row contract placement in value projection and result emission readiness.
- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
