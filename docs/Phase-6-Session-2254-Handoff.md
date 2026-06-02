# Phase 6 Session 2254 Handoff - Java/C# Row Pairing Readiness Report

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2254-Completion.md`
- `docs/Phase-6-Session-2254-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2254 added a non-live Java/C# row-pairing readiness report for `CM_FIND_GROUP` mutation-post action `2` and action `6`.

New C# artifact:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`

New test artifact:

- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`

Updated adjacent artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2254-Completion.md`

The new report consumes the explicit-root Java post-capture validator summary and the C# live-boundary row intake preflight, then reports whether action `2`/`Recruitment` and action `6`/`Application` can pair by action/mutation identity before value projection.

It does not execute `ProcessPacketAsync`, send packets, read projected values, materialize comparison results, execute runtime comparison, or prove verified parity.

## Validation From Last Session

Focused C# validation passed:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostCSharpLiveBoundaryRowIntakePreflightServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

Result: passed 21, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Java/Maven was not run because no Java source or fixture changed in UOW-2254. Java action `2`/`6` source was reviewed directly.

Broad `.NET` validation was skipped because there was no broad-validation trigger. The filtered C# test command built the affected project and dependencies.

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Java/C# row-pairing readiness changes: run the edited report test class plus explicit-root post-capture summary, C# live-boundary row intake preflight, and runtime evidence checklist tests.
- C# row-intake/report changes: run the edited test class plus directly adjacent guarded boundary/result/plan/checklist tests with a filtered `dotnet test`.
- Java explicit-root capture command changes: run only `FindGroupMutationPostTraceCaptureTest#commandSuppliedArtifactRootPropertyWritesGuardedArtifacts` with a temporary artifact root.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build just to get a second compile signal.

## Next Recommended UOW

Add a non-live value-projection handoff gate that consumes `FindGroupMutationPostJavaCSharpRowPairingReadinessReportService` and the existing value contract/readiness surfaces, then records that value projection may only proceed after all action/mutation row pairs are ready and runtime Java/C# row values exist.

Suggested scope:

- Read the startup docs listed above.
- Inspect:
  - `FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderPreflightContractService.cs`
  - `FindGroupMutationPostProjectedRowComparisonValueReaderReadinessSummaryService.cs`
  - `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Add a small non-live gate/report that links row pairing readiness to the existing value projection blockers.
- Run filtered C# tests for the new gate plus row-pairing readiness and closest value-reader/value-contract tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates, and a non-live Java/C# action-mutation row-pairing readiness report, but verified parity is still blocked by missing runtime-backed Java artifacts, missing actual accepted live C# boundary rows from production dispatch, value projection/materialization/emission evidence, runtime comparison execution, and executable value-reader implementation.
