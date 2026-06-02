# Phase 6 Session 2212 Completion - FindGroup Mutation Dry-Run Paired Row Readiness

Date: 2026-06-02
Unit of Work: UOW-2212
Status: Completed

## Scope

This unit added a non-live paired-row readiness summary to the action `2`/`6` projected-row comparison dry-run contract.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonResultSkeletonService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not compare Java/C# row values, and does not mark runtime parity verified.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonDryRunPairedRowReadiness`.
- Added `PairedRowReadiness` to `FindGroupMutationPostProjectedRowComparisonDryRunContract`.
- Reports one paired-readiness row per Java action contract for action `2` recruitment and action `6` application.
- Marks a row identity as future-input ready only when both a shape-valid Java artifact row and an accepted live C# boundary row exist for the same action, mutation kind, and required identity string.
- Keeps `ShouldCompareRows` governed by the existing execution blocker report.
- Keeps readiness rows explicitly non-live and value-comparison-free.
- Updated downstream result-skeleton test construction for the expanded dry-run shape.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only dry-run comparison input-shape aggregation plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the Java source review was sufficient for the non-live action/mutation identity mapping.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live dry-run contract; the focused filter covers the changed dry-run contract, downstream result skeleton construction, Java artifact row source, guarded C# row source, and comparison result contract adjacency.

Result:

- Focused C# command: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Client Packet Boundary / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Dry-run contract now reports whether Java/C# row references are paired by action, mutation kind, and required identity. No live C# boundary dispatch or row comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Readiness Metadata | Partial | Unit Tested | Partial Parity | Action `2` readiness requires Java `Recruitment` artifact shape plus accepted C# live-boundary evidence before a future executor input is considered paired. Values are not compared. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Readiness Metadata | Partial | Unit Tested | Partial Parity | Action `6` readiness requires Java `Application` artifact shape plus accepted C# live-boundary evidence before a future executor input is considered paired. Values are not compared. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_DefaultDryRunBlocksAndDoesNotCompareRows` | C# unit | Java action `2`/`6` comparison remains future work | Default dry-run exposes action `2`/`6` readiness rows but no Java/C# accepted rows and no value comparison. | Focused C# test. | No live boundary execution. |
| `Create_ReadyBlockerReportAllowsFutureExecutorButStillDryRunOnly` | C# unit | Java action `2`/`6` mutation identity reviewed from `CM_FIND_GROUP.runImpl` and `FindGroupService` | Paired Java and accepted C# references are marked future-input ready while still refusing value comparison. | Focused C# test with synthetic rows. | Synthetic rows are not runtime comparison evidence. |
| `Create_ProjectsShapeValidJavaRowsAsFutureExecutorInputs` | C# unit | Java artifact rows must preserve action mapping | Java-only rows set `HasAcceptedJavaRow` but remain unpaired without C# accepted rows. | Focused C# test with synthetic artifact rows. | No live C# rows. |
| `Create_ProjectsAcceptedGuardedCSharpRowsAsFutureExecutorInputs` | C# unit | Future comparison needs both Java and C# references | C#-only rows set `HasAcceptedCSharpRow` only for accepted live-boundary candidate rows and remain unpaired without Java rows. | Focused C# test with synthetic rows. | No value comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# dry-run comparison contract update
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Paired readiness is metadata only and cannot prove Java/C# runtime parity.
- Shape-valid Java rows and synthetic accepted C# rows in tests prove aggregation shape, not runtime behavior.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison executor skeleton that consumes paired-readiness rows and emits only blocked planned result rows when values cannot be compared.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2212-Completion.md`
