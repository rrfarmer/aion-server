# Phase 6 Session 2221 Completion - FindGroup Mutation Value Reader Design Contract

Date: 2026-06-02
Unit of Work: UOW-2221
Status: Completed

## Scope

This unit added a non-live projected-row comparison value-reader design contract for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not read Java/C# row values, does not compare rows, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService`.
- Adds one reader-design row per existing value-contract field.
- Names Java JSON paths in the shape `$.traces[*].fieldName`.
- Names C# accessors in the shape `FindGroupDirectPacketMutationPostBoundaryTraceExport.PropertyName`.
- Keeps runtime-only fields such as `traceSource` and `serverEpochSeconds` ignored for equality.
- Keeps `CanReadJavaValues=false`, `CanReadCSharpValues=false`, `CanCompareValues=false`, and `IsLive=false`.
- Added focused tests for default blocked state, Java/C# field mapping, runtime-only ignored fields, runtime-evidence-ready-but-reader-unimplemented behavior, and distinct path/accessor lists.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live value-reader design contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only maps reviewed Java serializer/C# trace-export field names to future reader paths.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
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

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests.Create_DefaultContractBlocksBeforeExecutionGateReadiness` | C# unit | Java action `2`/`6` value comparison remains future work | Default value-reader design is non-live and blocks all Java/C# reads and comparison. | Focused C# test. | No runtime values. |
| `Create_MapsRequiredFieldsToJavaJsonPathAndCSharpAccessor` | C# unit | Java serializer schema and C# trace export source review | Required fields map to Java JSON paths and C# trace-export property accessors. | Focused C# test. | No actual reads. |
| `Create_KeepsRuntimeOnlyFieldsIgnoredForEquality` | C# unit | Existing key projection metadata | Runtime-only fields stay ignored for equality. | Focused C# test. | No mismatch context emission. |
| `Create_RuntimeEvidenceReadyGateStillBlocksReaderImplementation` | C# unit | Execution-readiness gate remains non-live | Even synthetic runtime-evidence-ready gate keeps reads disabled because the reader is unimplemented. | Focused C# test with synthetic gate. | No reader implementation. |
| `Create_ListsDistinctJavaPathsAndCSharpAccessors` | C# unit | Java/C# schema field mapping | Contract exposes distinct Java paths and C# accessors for future reader implementation. | Focused C# test. | No runtime value projection. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader design contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The value-reader design contract is metadata only and cannot prove Java/C# runtime parity.
- JSON paths and C# accessors are not value reads, and future implementation must still parse/validate types and collection ordering.
- Live C# trace-row emitter, registry send observation, real projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison value-reader implementation skeleton that consumes accepted Java/C# row references and returns blocked read attempts for each field without reading values yet.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2221-Completion.md`
- `docs/Phase-6-Session-2221-Handoff.md`
