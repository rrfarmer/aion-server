# Phase 6 Session 2222 Completion - FindGroup Mutation Value Reader Skeleton

Date: 2026-06-02
Unit of Work: UOW-2222
Status: Completed

## Scope

This unit added a non-live projected-row comparison value-reader implementation skeleton for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not parse Java trace JSON values, does not read C# trace-export values, does not compare rows, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService`.
- Consumes the value-reader design contract and dry-run accepted row references.
- Emits one blocked read attempt per designed field.
- Distinguishes missing Java rows, missing C# rows, ignored runtime context fields, and paired rows whose reader implementation is still deferred.
- Keeps every attempt non-live with `AttemptsJavaRead=false`, `AttemptsCSharpRead=false`, and `CanReadValue=false`.
- Keeps top-level `CanReadValues=false`, `CanCompareValues=false`, and `IsLive=false`.
- Added focused tests for default blocked design state, missing Java rows, ignored runtime context, Java-only rows, and fully paired rows that still defer reader implementation.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live value-reader skeleton plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderDesignContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only consumes existing reviewed Java/C# row-reference metadata while returning blocked read attempts without reading values.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live value-reader skeleton; the focused filter covers the new skeleton, the value-reader design contract, value contract, execution gate, dry-run contract, and adjacent executor skeleton.

Result:

- Focused C# command: passed 32, failed 0, skipped 0.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService` | Client Packet Boundary / Value Reader Skeleton | Partial | Unit Tested | Partial Parity | Skeleton plans blocked field read attempts from accepted row references. No live boundary dispatch, runtime row value reads, value comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService` | Service Mutation / Value Reader Skeleton | Partial | Unit Tested | Partial Parity | Action `2` field reads are represented as blocked attempts only; no Java/C# values are read or compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService` | Service Mutation / Value Reader Skeleton | Partial | Unit Tested | Partial Parity | Action `6` field reads are represented as blocked attempts only; no Java/C# values are read or compared. |
| `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureSerializer` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService` | Java Trace Serializer / Value Reader Skeleton | Partial | Unit Tested | Partial Parity | Serializer schema paths are carried into blocked read attempts, but no runtime artifact values are read. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonServiceTests.Create_DefaultSkeletonBlocksBeforeDesignReadinessAndDoesNotRead` | C# unit | Java action `2`/`6` value comparison remains future work | Default skeleton blocks before design readiness and performs no Java/C# reads. | Focused C# test. | No runtime rows or values. |
| `Create_DefaultSkeletonReportsMissingJavaRowsForRequiredFields` | C# unit | Java trace serializer fields remain required future input | Required equality fields report missing Java rows when default dry-run inputs lack Java rows. | Focused C# test. | No artifact JSON parsing. |
| `Create_IgnoresRuntimeContextAttemptsWithoutReadingValues` | C# unit | Existing key projection metadata treats runtime context as ignored for equality | Runtime-only context fields are ignored and still perform no value reads. | Focused C# test. | No mismatch context emission. |
| `Create_JavaOnlyRowsBlockMissingCSharpRows` | C# unit | Java action rows require corresponding C# projected rows for comparison | Java-only accepted rows block on missing C# rows. | Focused C# test with synthetic dry-run rows. | No live C# trace rows. |
| `Create_PairedRowsStillDeferReaderImplementation` | C# unit | Runtime row-value reading remains intentionally deferred | Fully paired synthetic rows still block with reader implementation deferred and no value reads. | Focused C# test with synthetic dry-run rows. | No reader implementation or comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 3 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live value-reader skeleton service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value reader implementation, value projection, row identity matching, result emission, runtime/socket comparison, comparator implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Skeleton read attempts are planning metadata only and are not Java/C# runtime comparison evidence.
- No Java JSON values or C# trace-export values are read or compared.
- Future implementation must validate missing fields, field types, collection ordering, ignored runtime context, live C# trace-row emission, registry observation, and real projected-row comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison value-reader blocked-result report that summarizes missing Java rows, missing C# rows, ignored runtime context, and deferred reader implementation counts without reading values.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2222-Completion.md`
- `docs/Phase-6-Session-2222-Handoff.md`
