# Phase 6 Session 2210 Completion - FindGroup Mutation Dry-Run Accepted Row Projection

Date: 2026-06-02
Unit of Work: UOW-2210
Status: Completed

## Scope

This unit added non-live accepted C# row references from the guarded fixture result contract into the action `2`/`6` projected-row comparison dry-run contract.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedFixtureResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostComparisonExecutionResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonResultSkeletonService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not add live runtime rows, and does not compare Java/C# rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonDryRunAcceptedCSharpRowReference`.
- Added `AcceptedCSharpRows` to `FindGroupMutationPostProjectedRowComparisonDryRunContract`.
- Added `HasGuardedFixtureResultContract` to the dry-run contract.
- Projected accepted guarded C# rows into the dry-run executor input shape with:
  - action,
  - mutation kind,
  - guarded candidate status,
  - row identity fields,
  - accepted-live-boundary flag,
  - evidence string,
  - planned input source.
- Updated downstream result-skeleton test construction for the new dry-run shape.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only dry-run comparison input-shape wiring plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonInputEnvelopeServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionBlockerReportServiceTests|FullyQualifiedName~FindGroupMutationPostComparisonExecutionResultContractServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW.
- Broad-validation trigger: none. This unit does not modify `GameServerConnection.ProcessPacketAsync`, live dispatch, shared runtime primitives, packet primitives, persistence, common world state, or live side effects.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live dry-run contract and the focused filter covers its guarded-row source, comparison envelope/blocker surfaces, result contract, and downstream result skeleton.

Result:

- Focused C# command: passed 35, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed. Git emitted line-ending normalization warnings for touched documentation and C# files on Windows.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`; `Aion.GameServer.Services.FindGroupMutationPostGuardedFixtureResultContractService` | Client Packet Boundary / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Dry-run contract now names accepted guarded C# row references as future executor inputs. No live boundary rows, registry observation, or comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `2` accepted-row references carry Java-derived row identity and packet expectations, but evidence is synthetic/non-live only. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService` | Service Mutation / Dry-Run Comparison Contract | Partial | Unit Tested | Partial Parity | Action `6` accepted-row references carry Java-derived row identity and packet expectations, but evidence is synthetic/non-live only. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.Create_DefaultDryRunBlocksAndDoesNotCompareRows` | C# unit | Java action `2`/`6` comparison remains future work | Default dry-run remains non-live and has no accepted C# rows. | Focused C# test. | No live boundary execution. |
| `Create_ReadyBlockerReportAllowsFutureExecutorButStillDryRunOnly` | C# unit | Future comparison may run only after envelope gates are ready | Ready blocker report plus accepted guarded rows make the dry-run ready but still non-live. | Focused C# test with synthetic rows. | No comparison execution. |
| `Create_ProjectsAcceptedGuardedCSharpRowsAsFutureExecutorInputs` | C# unit | Java row identity uses action, mutation kind, active player, and mutated entry | Accepted guarded C# row references carry identity, status, evidence, and planned input source. | Focused C# test with synthetic accepted row. | Synthetic rows are not runtime evidence. |
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
- Synthetic accepted rows in tests prove projection shape, not runtime behavior.
- Live C# trace-row emitter, registry send observation, projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live Java accepted-row reference projection alongside the accepted C# row references so the dry-run contract can name both future executor inputs without executing comparison.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonDryRunContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonResultSkeletonServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2210-Completion.md`
- `docs/Phase-6-Session-2210-Handoff.md`
