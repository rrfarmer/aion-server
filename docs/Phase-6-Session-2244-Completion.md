# Phase 6 Session 2244 Completion - Value Reader Executor Live-Capture Preflight Runbook

Date: 2026-06-02
Unit of Work: UOW-2244
Status: Completed

## Scope

This unit added a non-live live-capture preflight/runbook for the future `CM_FIND_GROUP` action `2` and action `6` value-reader executor evidence collection.

Java source reviewed:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`

C# source reviewed:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaArtifactCaptureRunbookService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`

This UOW does not run capture with the repository artifact-root property, does not implement executable value readers, read Java JSON values, read C# trace-export values, compare rows, materialize output rows, emit results, wire live C# `CmFindGroup` dispatch, or claim verified parity.

## Changes

- Added `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService`.
- The runbook maps runtime comparison handoff requirements to:
  - the exact focused Java capture command with `-Daion.findGroupMutationPost.capture=true`,
  - the guarded Java artifact-root property `-Daion.findGroupMutationPost.artifactRoot=parity-artifacts/find-group/mutation-post/java`,
  - Java artifact validation gates,
  - guarded C# boundary fixture gates,
  - boundary executor observation gates,
  - registry send observation gates,
  - row identity and value projection gates,
  - materialization gates,
  - result emission gates,
  - runtime comparison gates,
  - executable implementation gates.
- All capture execution, command acceptance, runtime comparison, executable implementation, and verified parity flags remain false.
- Runtime evidence checklist provider metadata now names the live-capture preflight runbook as existing non-live result-emission metadata.
- Updated focused tests and live-dispatch design notes.
- Left `docs/PHASE-6-PROGRESS.md` untouched.

## Validation

Validation decision:

- Changed surface: non-live C# service/test metadata plus design/session documentation; targeted Java fixture command shape reviewed and validated.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostJavaArtifactCaptureRunbookServiceTests|FullyQualifiedName~FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

- Focused Java/Maven command:

```powershell
mvn -pl game-server -am test "-Dtest=FindGroupMutationPostTraceCaptureTest" "-Daion.findGroupMutationPost.capture=true" "-Dmaven.test.skip=false" "-Dsurefire.failIfNoSpecifiedTests=false"
```

- Broad-validation trigger: none. This unit does not modify packet primitives, live dispatch, shared runtime state, persistence, scheduling, connection side effects, Java source, Java fixtures, or broad model state.
- Broad .NET decision: skipped intentionally. The filtered C# test command built the affected project and dependencies, and no broad trigger applied.
- Why this scope is sufficient: the edited service is a non-live capture-preflight runbook; the focused C# filter covers the new runbook plus adjacent runtime handoff, Java capture runbook, guarded boundary skeleton, and runtime-evidence provider mapping. The targeted Maven command validates the existing Java capture fixture scaffold without supplying the artifact-root property, so it does not write repository artifacts.

Result:

- Focused C# command: passed 26, failed 0, skipped 0. Existing nullable/analyzer warnings were emitted from unrelated C# files.
- Focused Java/Maven command: build success; `FindGroupMutationPostTraceCaptureTest` ran 30 tests, failed 0, errors 0, skipped 1. The skipped case is the artifact-root-property write case because the property was intentionally not supplied.
- `git diff --check`: passed with usual Windows line-ending warnings.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP` action `2`/`6` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Client Packet Boundary / Live-Capture Preflight Metadata | Partial | Unit Tested | Partial Parity | Runbook names capture commands, artifact roots, guarded C# boundary gates, value projection, materialization, result emission, runtime comparison, and executable implementation blockers, but it does not execute live boundary dispatch, compare rows, or emit result rows. |
| `com.aionemu.gameserver.services.findgroup.FindGroupService.addRecruitment/addApplication` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Service Mutation / Capture Acceptance Metadata | Partial | Unit Tested | Partial Parity | Runbook maps Java mutation-post capture and validation gates, but generated Java rows, live C# rows, registry observation, and runtime comparison remain missing. |
| `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureTest` | `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` | Java Fixture / Capture Command Metadata | Partial | Regression Tested | Partial Parity | Targeted Maven fixture command passed without repository artifact-root output; this validates fixture scaffolding only, not runtime parity. |

## Test Documentation

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.Create_DefaultRunbookBlocksUntilRuntimeComparisonHandoffIsReady` | C# unit | Java action `2`/`6` source context | Default runbook is non-live and blocks capture until runtime comparison handoff metadata is ready. | Focused C# test. | No runtime rows. |
| `Create_DefaultRunbookListsEveryPreflightStepAsBlocked` | C# unit | Runtime handoff metadata | Runbook lists every capture/preflight step and keeps commands non-runnable. | Focused C# test. | Metadata only. |
| `Create_RuntimeMissingRunbookNamesConcreteJavaCaptureCommandAndArtifactRoot` | C# unit | Java fixture command/property source review | Runbook names the focused Maven command and guarded artifact-root property. | Focused C# test plus targeted Maven fixture run. | Did not run artifact-root write command. |
| `Create_RuntimeMissingRunbookNamesCSharpBoundaryExecutorAndRegistryGates` | C# unit | Java mutation-post source context | Runbook names guarded C# boundary, executor observation, and registry send gates. | Focused C# test. | Live C# rows missing. |
| `Create_ReadyShapedHandoffStillBlocksComparisonAndExecutableImplementation` | C# unit | Executor implementation intentionally deferred | Ready-shaped handoff still cannot run comparison or executable implementation. | Focused C# test. | No comparison execution. |
| `FindGroupMutationPostTraceCaptureTest` | Java regression | Java fixture scaffold | Existing Java capture scaffold remains runnable with the capture flag and no artifact-root property. | Targeted Maven test. | One artifact-root write case skipped intentionally; no runtime parity comparison. |

## Summary Metrics

- Total Java artifacts discovered in this UOW: 4 Java artifacts reviewed
- Total artifacts ported in this UOW: 1 C# non-live value-reader executor live-capture preflight runbook service
- Total artifacts with verified parity in this UOW: 0
- Total artifacts needing verification or partial parity: 3 Java/C# trace-preparation artifacts
- Total blocked artifacts: live Java runtime trace artifacts, live C# boundary rows, boundary executor invocation, registry send observation, row identity matching, value projection, output materialization, result emission, runtime/socket comparison, executor implementation, and live `CM_FIND_GROUP` dispatch
- Estimated overall migration completion: unchanged; Phase 6 remains in progress.

## Remaining Risks

- The live-capture preflight runbook is metadata only and cannot prove parity.
- The targeted Maven command did not supply `aion.findGroupMutationPost.artifactRoot`, so it did not write repository artifacts.
- No Java JSON values or C# trace-export values are read.
- Future implementation must generate capture-enabled Java runtime rows, capture accepted live C# boundary rows, prove registry send observations, pair row identities, project values, materialize results, emit result rows, and run deterministic Java/C# comparison before claiming parity.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor capture acceptance matrix that records each live-capture preflight step's pass/fail evidence fields and the exact blockers preventing runtime comparison execution.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2244-Completion.md`
- `docs/Phase-6-Session-2244-Handoff.md`
