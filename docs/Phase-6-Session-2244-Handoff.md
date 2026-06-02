# Phase 6 Session 2244 Handoff - Value Reader Executor Live-Capture Preflight Runbook

Date: 2026-06-02
Unit of Work: UOW-2244
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Use focused validation by default. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit. A passing filtered `dotnet test` command is the compile signal for its affected project and dependencies unless a documented broad-validation trigger applies.

If a focused command is still expected to take several minutes, narrow the filter to the edited test class and closest adjacent class first. Document residual risk instead of using full .NET validation as a reassurance step.

Run Java/Maven only when Java source or fixtures changed, or when a narrow Java source-of-truth command exists for the touched behavior. Record the skip reason when Java is not run.

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Full .NET test/build commands are opt-in by documented broad-validation trigger, not routine validation.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService` names the exact Java artifact rows, accepted C# boundary rows, value projection, materialization, result emission, runtime comparison, and executable implementation evidence required before executable value-reader work can start.
- `FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService` now maps those requirements to concrete Java capture commands, guarded artifact roots, guarded C# boundary fixture gates, value projection gates, materialization gates, emission gates, runtime comparison gates, and executable implementation gates.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now names the live-capture preflight runbook as existing non-live result-emission metadata.
- No service currently reads Java/C# mutation-post row values, compares them, attaches context, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2244 Summary

This UOW added a value-reader executor live-capture preflight/runbook contract.

Key behavior:

- default runbook blocks until runtime comparison handoff metadata is ready,
- runtime-missing runbook names the concrete Java capture command and artifact root,
- guarded C# boundary capture gates require boundary acceptance, executor observation, registry observation, and zero broadcast/invite counts,
- row identity, value projection, materialization, emission, runtime comparison, and executable implementation gates remain blocked,
- runtime evidence checklist result-emission provider metadata now includes the live-capture preflight runbook.

Important notes:

- This is metadata only.
- It does not run the artifact-root writing command.
- It does not parse Java JSON values or access C# export values.
- It does not implement readers, compare fields, materialize output rows, emit results, attach context, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.

## Java Artifacts Reviewed

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest.java`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureInMemoryArtifactBridge.java`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorRuntimeComparisonHandoffContractService`
- `Aion.GameServer.Services.FindGroupMutationPostJavaArtifactCaptureRunbookService`
- `Aion.GameServer.Services.FindGroupMutationPostGuardedLiveBoundaryFixtureSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonValueReaderExecutorLiveCapturePreflightRunbookContractServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2244-Completion.md`
- `docs/Phase-6-Session-2244-Handoff.md`

## Validation In UOW-2244

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

- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Live-capture preflight rows, runtime comparison handoff rows, implementation readiness audit rows, evidence summary rows, result-emission gate rows, materialization preflight rows, runtime-evidence intake rows, blocked-output preview rows, implementation plan rows, executor readiness gate rows, comparator preflight rows, result schema rows, implementation runbook rows, implementation checklist rows, value-reader preflight rows, mismatch-context preflight rows, readiness summary rows, blocked report rows, skeleton attempts, design rows, execution-readiness gate rows, runtime evidence checklist rows, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts generated from a supplied artifact-root capture command, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, context attachment, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The live-capture preflight runbook is included in runtime-evidence metadata, but it cannot prove parity.
- Future implementation must validate runtime row pairing, typed reader behavior, field types, row identity matching, missing-row selection, ordered-list equality, string/enum case-sensitivity, ignored runtime context attachment, live C# trace-row emission, registry observation, and real projected-row comparison before comparing values.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live value-reader executor capture acceptance matrix that records each live-capture preflight step's pass/fail evidence fields and the exact blockers preventing runtime comparison execution.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2244] Add find group value reader live capture preflight
```
