# Phase 6 Session 2252 Completion - C# Live Boundary Row Intake Preflight

## Scope

Added a non-live intake preflight for accepted C# `CM_FIND_GROUP` action `2`/`6` live-boundary rows before explicit-root Java artifacts can feed runtime comparison.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedFixtureResultContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupDirectPacketMutationPostBoundaryTraceSchemaService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`

## Changes

Added `FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService`.

The preflight consumes `FindGroupMutationPostGuardedFixtureResultContractService` and records the exact accepted C# boundary-row gates required before Java artifacts can be paired:

- Accepted action `2` boundary row.
- Accepted action `6` boundary row.
- `boundaryAccepted=true`.
- `executorInvokedFromBoundary=true`.
- `registrySendsObservedInOrder=true`.
- Posted `SmSystemMessage` before refreshed `SmFindGroup`.
- `worldBroadcastCount=0`.
- `inviteDispatchCount=0`.
- Java artifact pairing identity by action and mutation kind.

Updated `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` and `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md` so the C# live boundary evidence chain names the new intake preflight.

`docs/PHASE-6-PROGRESS.md` was intentionally not touched. Current working context remains in latest completion/handoff docs.

## Validation Decision

Changed surface:

- Non-live C# service and unit tests.
- Adjacent runtime evidence checklist provider text.
- Design/session documentation.

Focused C# validation:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 28, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Focused Java/Maven validation: not run. No Java source or fixture changed in this UOW; Java action `2`/`6` source was reviewed and the change is a C# non-live intake contract over existing guarded boundary row metadata.

Broad-validation trigger: none. Full `.NET` build/suite was skipped because the filtered C# command built the affected project and dependencies and covered the new preflight plus directly adjacent guarded boundary/result/plan/checklist services.

## Parity Table Updates

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP.runImpl` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService` | Boundary Row Intake Metadata | Partial | Unit Tested | Partial Parity | Records the accepted C# boundary row gates for Java actions `2` and `6`. Does not execute `ProcessPacketAsync`, capture live rows, or compare Java/C# runtime output. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService` | Mutation Post Boundary Metadata | Partial | Unit Tested | Partial Parity | Encodes Java posted-message/refreshed-list ordering, zero broadcast/invite requirements, and action/mutation pairing identity for future C# row intake. Live boundary evidence and runtime comparison remain missing. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
| --- | --- | --- | --- | --- | --- |
| `Create_DefaultPreflightBlocksUntilAcceptedBoundaryRowsExist` | Unit | Java action `2`/`6` dispatch and C# guarded result contract | Default intake blocks every gate when no accepted live rows exist. | Non-live metadata. | Does not execute boundary. |
| `Create_DisabledShapeRowsDoNotSatisfyLiveBoundaryIntake` | Unit | C# disabled projection shape versus Java live boundary requirement | Disabled shape rows do not satisfy boundary, executor, or registry gates. | Guardrail metadata. | No live sends. |
| `Create_AcceptedActionTwoAndSixRowsSatisfyIntakeButNotParity` | Unit | Java posted/refreshed mapping and C# guarded accepted rows | Accepted action `2`/`6` rows satisfy intake and can feed pairing, but verified parity remains blocked. | Contract-level row evidence. | No value projection or runtime comparison. |
| `Create_SingleAcceptedActionStillBlocksJavaArtifactPairing` | Unit | Java requires both action `2` and `6` artifacts/rows | A single accepted row cannot feed Java artifact pairing. | Guardrail metadata. | Missing counterpart action. |
| `Create_RejectedPacketShapeCannotSatisfyPostedRefreshedOrdering` | Unit | Java message id/action mapping | Rejected packet shape cannot satisfy posted/refreshed ordering or action pairing. | C# contract rejection tied to Java mapping. | No live boundary execution. |

Adjacent updated test:

- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.Create_CSharpBoundaryAndRegistryRowsStayNonLive`

## Summary Metrics

- Java artifacts reviewed: 2
- C# non-live services added: 1
- Verified parity rows added: 0
- Partial parity rows added: 2
- Blocked by missing evidence: actual accepted live C# boundary rows, generated Java/C# row pairing result, value projection, materialization, emission, runtime/socket comparison, executable implementation, and live dispatch.
- Estimated Phase 6 completion: unchanged; this UOW clarifies C# live-boundary row intake but adds no runtime evidence.

## Next Recommended UOW

Add a non-live Java/C# mutation-post row pairing readiness report that consumes the explicit-root Java post-capture validator summary and the C# live-boundary row intake preflight, then reports whether action `2` and action `6` can be paired by action/mutation identity before value projection.

Safe candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.
