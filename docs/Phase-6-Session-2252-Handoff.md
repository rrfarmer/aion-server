# Phase 6 Session 2252 Handoff - C# Live Boundary Row Intake Preflight

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2252-Completion.md`
- `docs/Phase-6-Session-2252-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2252 added a non-live C# live-boundary row intake preflight for `CM_FIND_GROUP` action `2`/`6` mutation-post comparison readiness.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2252-Completion.md`

The preflight consumes `FindGroupMutationPostGuardedFixtureResultContractService` and records the accepted C# boundary-row gates required before explicit-root Java artifacts can feed runtime comparison:

- accepted action `2` row,
- accepted action `6` row,
- boundary acceptance,
- executor invocation from boundary,
- registry send ordering,
- posted `SmSystemMessage` before refreshed `SmFindGroup`,
- zero world broadcasts,
- zero invite dispatches,
- action/mutation identity for Java artifact pairing.

It does not execute `ProcessPacketAsync`, send packets, project values, materialize results, or compare Java/C# runtime rows.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedFixtureResultContractServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveTraceRowFixturePlanServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 28, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2252. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

For this artifact family:

- C# row-intake/report changes: run the edited test class plus directly adjacent guarded boundary/result/plan/checklist tests with a filtered `dotnet test`.
- Java explicit-root capture command changes: run only `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` with a temporary artifact root.
- C# Java/C# pairing-readiness changes: run the edited test class plus explicit-root post-capture validator summary and C# live-boundary row intake preflight tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a non-live Java/C# mutation-post row pairing readiness report that consumes the explicit-root Java post-capture validator summary and the C# live-boundary row intake preflight, then reports whether action `2` and action `6` can be paired by action/mutation identity before value projection.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
  - `FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightService.cs`
  - `FindGroupMutationPostProjectedRowComparisonDryRunContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live readiness report with action `2` and action `6` pairing rows.
- Run filtered C# tests for the new report plus explicit-root post-capture summary and C# intake preflight tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project now has explicit-root Java artifact validation and C# accepted-row intake gates, but verified parity is still blocked by missing actual accepted live C# boundary rows, missing Java/C# row pairing result, value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
