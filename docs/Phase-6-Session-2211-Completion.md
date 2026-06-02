# Phase 6 Session 2211 Completion - FindGroup Mutation Dry-Run Java Row Projection

Date: 2026-06-02
Unit of Work: UOW-2211
Status: Completed

## Scope

This unit added non-live Java row references from shape-valid mutation-post artifact files into the action `2`/`6` projected-row comparison dry-run contract.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactDirectoryReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaTraceArtifactValidatorService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonResultSkeletonService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonDryRunAcceptedJavaRowReference`.
- Added `AcceptedJavaRows` to `FindGroupMutationPostProjectedRowComparisonDryRunContract`.
- Added `HasJavaArtifactDirectoryReport` to the dry-run contract.
- Projected shape-valid Java artifact rows into the dry-run executor input shape with:
  - action,
  - mutation kind,
  - row identity fields,
  - shape-valid Java artifact flag,
  - evidence string,
  - planned input source.
- Kept default dry-run blocked when no shape-valid Java rows are available.
- Updated downstream result-skeleton test construction for the new dry-run shape.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only dry-run comparison input-shape wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live dry-run contract and the focused filter covers its Java artifact source, guarded C# row source, result contract, and downstream result skeleton.

Result:

- Focused C# command: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`; `Aion.GameServer.Services.FindGroupMutationPostJavaTraceArtifactDirectoryReportService` | Client Packet Boundary / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Dry-run contract now names shape-valid Java artifact row references as future executor inputs. No live C# boundary rows, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `2` Java row references carry artifact path, mutation kind, posted system message `1400392`, refreshed action `0`, and row identity metadata. Evidence is artifact-shape only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `6` Java row references carry artifact path, mutation kind, posted system message `1400393`, refreshed action `4`, and row identity metadata. Evidence is artifact-shape only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_DefaultDryRunBlocksAndDoesNotCompareRows` | C# unit | Java action `2`/`6` comparison remains future work | Default dry-run remains non-live and has no Java or C# row references when no artifacts are supplied. | Focused C# test. | No live boundary execution. |
| `Create_ReadyBlockerReportAllowsFutureExecutorButStillDryRunOnly` | C# unit | Future comparison may run only after envelope gates are ready | Ready blocker report plus Java/C# accepted rows make the dry-run ready but still non-live. | Focused C# test with synthetic rows. | No comparison execution. |
| `Create_ProjectsShapeValidJavaRowsAsFutureExecutorInputs` | C# unit | Java artifact rows must preserve action mapping | Shape-valid Java rows are projected as future executor inputs with identity and packet mapping evidence. | Focused C# test with synthetic artifact rows. | Synthetic artifacts are not runtime evidence. |
| `FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.Create_ReadyDryRunAllowsFutureMaterializationButStillSkeletonOnly` | C# unit | Dry-run readiness does not equal materialized comparison result | Downstream result skeleton accepts the expanded dry-run shape without materializing real results. | Focused C# test. | No row comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# dry-run comparison contract update
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live C# mutation-post boundary rows, executor observation, registry observation, projected-row comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The dry-run contract is metadata/input-shape only and cannot prove Java/C# runtime parity.
- Shape-valid Java rows and synthetic accepted C# rows in tests prove projection shape, not runtime behavior.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live paired-row readiness summary in the dry-run contract that reports which action identities have both Java and C# accepted references, while still refusing to compare values.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2211-Completion.md`
- `docs/Phase-6-Session-2211-Handoff.md`
