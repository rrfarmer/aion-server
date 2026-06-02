# Phase 6 Session 2226 Completion - FindGroup Mutation Value Reader Preflight Contract

Date: 2026-06-02
Unit of Work: UOW-2226
Status: Completed

## Scope

This unit added a non-live projected-row comparison value-reader implementation preflight contract for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService`.
- Enumerates concrete schema-v1 reader kinds for the future value reader:
  - `Int32Scalar`
  - `StringScalar`
  - `BooleanScalar`
  - `EnumStringScalar`
  - `OrderedInt32List`
  - `IgnoredRuntimeContext`
- Maps planned Java JSON token shape, expected C# CLR type, C# value shape, and preconditions for every current value-reader design field.
- Explicitly preserves collection ordering for `visibleEntryObjectIdsAfterMutation`.
- Keeps all Java reads, C# reads, value comparison, and live dispatch disabled.
- Added focused tests for default blocked state, reader kind enumeration, scalar readers, string/enum/list readers, ignored runtime context, and runtime-evidence-ready-but-reader-deferred behavior.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live value-reader preflight contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryServiceTests|FullyQualifiedName~FindGroupDirectPacketMutationPostBoundaryTraceSchemaServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the C# changes only enumerate future typed-reader requirements from reviewed Java schema/source metadata.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live value-reader preflight contract; the focused filter covers the new preflight, value-reader design/readiness, and the mutation-post boundary trace schema that supplies schema-v1 field shapes.

Result:

- First focused C# run: failed because one test asserted `postedSystemMessageId`, which is not in the current projected value-reader field set for that assertion. The test was corrected to use an actual projected schema/equality field.
- Second focused C# run: failed because one test asserted `boundaryAccepted`, which is not in the current projected value-reader field set for that assertion. The test was corrected to use `stateMutationRecordedBeforeDirectPackets`.
- Final focused C# command: passed 20, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` | Client Packet Boundary / Value Reader Preflight | Partial | Unit Tested | Partial Parity | Preflight enumerates future typed readers from schema-v1 metadata. No live boundary dispatch, runtime row value reads, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` | Service Mutation / Value Reader Preflight | Partial | Unit Tested | Partial Parity | Action `2` typed-reader rows are metadata only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` | Service Mutation / Value Reader Preflight | Partial | Unit Tested | Partial Parity | Action `6` typed-reader rows are metadata only; no Java/C# values are read or compared. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService` | Java Trace Serializer / Value Reader Preflight | Partial | Unit Tested | Partial Parity | Java serializer overloads define schema-v1 token shapes; preflight maps them without parsing runtime artifact values. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests.Create_DefaultPreflightBlocksBeforeDesignReadinessAndDoesNotRead` | C# unit | Java schema-v1 value reading remains future work | Default preflight blocks before design readiness and performs no Java/C# reads. | Focused C# test. | No runtime rows or values. |
| `Create_EnumeratesSchemaV1ReaderKinds` | C# unit | Java serializer field overloads and C# trace-export schema | Preflight exposes all planned reader kinds for scalar, enum, list, and ignored context fields. | Focused C# test. | No typed reader implementation. |
| `Create_MapsScalarReadersToJavaJsonAndCSharpShapes` | C# unit | Java serializer integer/boolean fields | Scalar fields map to expected JSON token and C# CLR shapes. | Focused C# test. | No JSON parsing. |
| `Create_MapsStringEnumAndOrderedListReaders` | C# unit | Java serializer string fields, mutation kind string, and integer array field | String, enum-string, and ordered integer-list fields map to planned readers while preserving list order. | Focused C# test. | No collection value comparison. |
| `Create_KeepsRuntimeContextIgnoredAndNonReading` | C# unit | Existing projection metadata treats runtime context as ignored for equality | Runtime context rows remain ignored and non-reading. | Focused C# test. | No mismatch context emission. |
| `Create_RuntimeEvidenceReadyDesignStillDefersTypedReaders` | C# unit | Reader implementation remains intentionally deferred | Runtime-evidence-ready design still blocks typed readers as unimplemented. | Focused C# test. | No reader implementation. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader preflight contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Value-reader preflight rows are planning metadata only and are not Java/C# runtime comparison evidence.
- No Java JSON values or C# trace-export values are read or compared.
- Future implementation must validate missing fields, field types, collection ordering, ignored runtime context, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add the value-reader preflight contract to the value-reader readiness summary and runtime evidence checklist as existing non-live metadata, still without enabling Java/C# value reads.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2226-Completion.md`
- `docs/Phase-6-Session-2226-Handoff.md`
