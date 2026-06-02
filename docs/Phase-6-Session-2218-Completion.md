# Phase 6 Session 2218 Completion - FindGroup Mutation Live Input Handoff Contract

Date: 2026-06-02
Unit of Work: UOW-2218
Status: Completed

## Scope

This unit added a non-live projected-row comparison live-input handoff contract for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonBlockedResultReportService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonReadinessSummaryService.cs`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not read Java/C# row values, does not compare rows, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService`.
- Added live-input requirement rows for:
  - projected-row readiness summary,
  - Java runtime trace artifacts,
  - C# live boundary rows,
  - boundary executor invocation,
  - registry send observation,
  - value projection,
  - row identity matching,
  - result emission,
  - live dispatch guard status,
  - runtime/socket comparison.
- Keeps every runtime requirement non-live and blocking.
- Keeps `CanStartLiveComparison=false`, `CanEnableLiveDispatch=false`, and `IsLive=false`.
- Added focused tests for default blocked state, runtime requirement coverage, synthetic summary-ready metadata, live dispatch guard blocking, and row identity wording.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live live-input handoff contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only enumerates runtime artifacts still required after reviewing Java source.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live handoff contract; the focused filter covers the new contract and the adjacent readiness, blocked-result, value-contract, and executor-skeleton services it depends on.

Result:

- Focused C# command: passed 24, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` | Client Packet Boundary / Runtime Input Handoff | Partial | Unit Tested | Partial Parity | Contract names the live artifacts still required after non-live readiness metadata. No live boundary dispatch, registry observation, value projection, comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` | Service Mutation / Runtime Input Handoff | Partial | Unit Tested | Partial Parity | Action `2` runtime handoff remains blocked until Java runtime trace rows and C# live boundary rows prove mutation state, posted system message, refreshed list send, and zero broadcast/invite observations. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` | Service Mutation / Runtime Input Handoff | Partial | Unit Tested | Partial Parity | Action `6` runtime handoff remains blocked until Java runtime trace rows and C# live boundary rows prove mutation state, posted system message, refreshed list send, and zero broadcast/invite observations. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.Create_DefaultContractBlocksBeforeSummaryReadiness` | C# unit | Java source review plus existing readiness summary blockers | Default handoff remains non-live, blocks live comparison/dispatch, and marks every row as non-runtime evidence. | Focused C# test. | No runtime artifacts. |
| `Create_DefaultContractListsExpectedRuntimeRequirements` | C# unit | Java `CM_FIND_GROUP` and `FindGroupService` action `2`/`6` source review | Handoff enumerates Java runtime trace, C# live boundary, executor, registry, value projection, row identity, result emission, dispatch guard, and socket comparison requirements. | Focused C# test. | No live comparison. |
| `Create_SummaryReadyForRuntimeInputsStillBlocksMissingRuntimeArtifacts` | C# unit | Existing projected-row readiness summary chain | Synthetic summary-ready metadata still blocks because runtime artifacts are missing. | Focused C# test with synthetic readiness summary. | No live C# rows or Java runtime rows. |
| `Create_LiveDispatchGuardNeverEnablesDispatch` | C# unit | Production live `CmFindGroup` dispatch remains disabled | Handoff keeps `GameServerConnection.ProcessPacketAsync` dispatch guard blocked and `CanEnableLiveDispatch=false`. | Focused C# test. | No live dispatch. |
| `Create_RowIdentityRequirementNamesActionMutationAndObjectIds` | C# unit | Java action `2`/`6` mutation identity reviewed from source | Handoff requires action, mutation kind, active-player object id, and mutated-entry object id matching. | Focused C# test. | No value projection. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 2 Java source artifacts
- Total artifacts ported in this UOW: 1 C# non-live handoff contract service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value projection, row identity matching, result emission, runtime/socket comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The live-input handoff contract is metadata only and cannot prove Java/C# runtime parity.
- Shape-valid Java artifacts, disabled C# projections, and synthetic summary rows prove contract shape only, not runtime behavior.
- Live C# trace-row emitter, registry send observation, real projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison runtime evidence checklist that maps each live-input handoff requirement to the specific existing or future fixture/service expected to satisfy it.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2218-Completion.md`
- `docs/Phase-6-Session-2218-Handoff.md`
