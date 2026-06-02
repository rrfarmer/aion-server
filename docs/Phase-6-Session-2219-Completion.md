# Phase 6 Session 2219 Completion - FindGroup Mutation Runtime Evidence Checklist

Date: 2026-06-02
Unit of Work: UOW-2219
Status: Completed

## Scope

This unit added a non-live projected-row comparison runtime evidence checklist for `CM_FIND_GROUP` action `2` and action `6` mutation-post comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureHooks.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonReadinessSummaryService.cs`
- existing mutation-post Java artifact, C# boundary-row, registry observation, value projection, comparison preflight, and result contract services discovered under `dotnetConversion/src/Aion.GameServer/Services`

This UOW does not wire live C# `CmFindGroup` dispatch, does not execute registry sends, does not read Java/C# row values, does not compare rows, and does not emit real result rows.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`.
- Maps every live-input handoff requirement to an existing non-live provider or future evidence producer.
- Distinguishes:
  - existing non-live metadata,
  - existing non-live scaffolds,
  - live dispatch still disabled,
  - runtime comparison not executed.
- Records the required next runtime evidence for Java trace artifacts, C# live boundary rows, boundary executor invocation, registry send observation, value projection, row identity matching, result emission, dispatch enablement, and runtime/socket comparison.
- Keeps `HasAnyRuntimeEvidence=false`, `CanStartProjectedComparison=false`, `CanClaimVerifiedParity=false`, and `IsLive=false`.
- Added focused tests for default blocked state, complete requirement-to-provider mapping, Java capture/C# reader mapping, C# boundary/registry mapping, and dispatch/runtime-comparison hard blockers.
- Updated live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: C# service/test-only non-live runtime evidence checklist plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only maps reviewed Java/C# scaffolds to future runtime evidence requirements.
- Broad-validation trigger: none. This unit does not modify live dispatch, packet primitives, shared runtime state, persistence, scheduling, connection side effects, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live evidence checklist; the focused filter covers the new checklist, the live-input handoff it consumes, and the adjacent readiness, blocked-result, value-contract, and executor-skeleton services.

Result:

- Focused C# command: passed 29, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Client Packet Boundary / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Checklist maps live-input requirements to existing non-live providers and future runtime evidence. No live boundary dispatch, registry observation, value projection, comparison, result emission, or socket comparison exists. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `2` provider mapping names Java capture hooks/artifact reader and future C# live boundary evidence, but runtime mutation/post/send observations are still missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Service Mutation / Runtime Evidence Checklist | Partial | Unit Tested | Partial Parity | Action `6` provider mapping names Java capture hooks/artifact reader and future C# live boundary evidence, but runtime mutation/post/send observations are still missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` | Java Trace Hook / Evidence Mapping | Partial | Unit Tested | Partial Parity | Existing Java hooks and fixture writer are mapped as non-live scaffolds. They are not runtime parity evidence until capture-enabled artifacts are generated and compared with live C# rows. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_DefaultChecklistBlocksBeforeRuntimeEvidenceReadiness` | C# unit | Java action `2`/`6` runtime evidence remains future work | Default checklist remains non-live, blocks projected comparison and verified parity, and has no runtime evidence. | Focused C# test. | No runtime artifacts. |
| `Create_MapsEveryLiveInputRequirementToAProvider` | C# unit | Existing handoff contract requirements | Every live-input requirement has a provider and required next evidence text. | Focused C# test. | Provider mapping only. |
| `Create_JavaRuntimeArtifactRowNamesJavaCaptureAndCSharpReader` | C# unit | Java trace hook/test scaffold source review | Java runtime artifact requirement maps to Java capture fixture/hooks and C# artifact reader while remaining scaffold-only. | Focused C# test. | No capture-enabled runtime artifact generated. |
| `Create_CSharpBoundaryAndRegistryRowsStayNonLive` | C# unit | Java action `2`/`6` direct-packet order reviewed from source | C# boundary and registry requirements map to existing non-live scaffolds and required live evidence. | Focused C# test. | No live C# boundary row or registry send observation. |
| `Create_DispatchAndRuntimeComparisonRowsRemainHardBlocked` | C# unit | Production `CmFindGroup` dispatch remains disabled | Live dispatch guard and runtime/socket comparison stay blocked and cannot claim verified parity. | Focused C# test. | No live dispatch or runtime/socket comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4 Java source/test artifacts
- Total artifacts ported in this UOW: 1 C# non-live evidence checklist service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 4 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, value projection, row identity matching, result emission, runtime/socket comparison, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- The runtime evidence checklist is metadata only and cannot prove Java/C# runtime parity.
- Existing Java hooks, checked-in Java artifacts, disabled C# projections, provider mappings, and synthetic summary rows prove checklist shape only, not runtime behavior.
- Live C# trace-row emitter, registry send observation, real projected-row comparison, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison remain missing.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison execution-readiness gate that combines the live-input handoff and runtime evidence checklist into one final go/no-go report before any comparator implementation.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2219-Completion.md`
- `docs/Phase-6-Session-2219-Handoff.md`
