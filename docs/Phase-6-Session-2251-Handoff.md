# Phase 6 Session 2251 Handoff - Explicit Root Post Capture Validator Summary

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2251-Completion.md`
- `docs/Phase-6-Session-2251-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2251 added a non-live post-capture validator summary for explicit-root Java action `2`/`6` mutation-post artifacts.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2251-Completion.md`

The summary consumes a supplied explicit artifact root through existing C# directory/validator services and reports missing directory, missing files, invalid artifacts, or shape-valid action `2`/`6` files. Shape-valid generated Java files still do not allow runtime comparison or verified parity without accepted live C# boundary rows.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactDirectoryReportServiceTests|FullyQualifiedName~FindGroupMutationPostJavaTraceArtifactValidatorServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactRootValidationCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaCaptureDryRunCommandReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 30, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2251. UOW-2250 already executed the targeted explicit-root Java fixture command, and this UOW only summarizes C# reader/validator output over supplied files.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered test command built the affected project and dependencies.

## Focused Testing Guidance

Prefer focused validation over full builds/tests unless a documented broad trigger exists.

For this artifact family:

- C# artifact summary/report changes: run the edited test class plus directly adjacent directory/validator/command-report/checklist tests with a filtered `dotnet test`.
- Java explicit-root capture command changes: run only `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` with a temporary artifact root.
- C# live-boundary row intake changes: run the edited intake/preflight test class plus directly adjacent guarded boundary fixture/result contract tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a non-live C# live-boundary row intake preflight that states exactly what accepted C# action `2`/`6` boundary rows must contain before the explicit-root Java artifacts can feed runtime comparison.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryService.cs`
  - `FindGroupMutationPostGuardedFixtureResultContractService.cs`
  - `FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
  - `FindGroupMutationPostCSharpLiveTraceRowFixturePlanService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live preflight that lists required accepted C# boundary row fields for action `2`/`6`:
  - `boundaryAccepted=true`
  - `executorInvokedFromBoundary=true`
  - `registrySendsObservedInOrder=true`
  - posted `SmSystemMessage` before refreshed `SmFindGroup`
  - zero `worldBroadcastCount`
  - zero `inviteDispatchCount`
  - matching action/mutation identity for Java artifact pairing
- Run filtered C# tests for the new preflight plus directly adjacent guarded boundary/result contract tests.
- Skip Java/Maven unless Java source or fixture changes; document the Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. Explicit-root Java artifacts can be generated and C# can summarize their shape, but verified parity is still blocked by missing accepted C# boundary/runtime evidence, missing value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
