# Phase 6 Session 2219 Handoff - FindGroup Mutation Runtime Evidence Checklist

Date: 2026-06-02
Unit of Work: UOW-2219
Status: Completed

## Startup Instructions

Future Phase 6 sessions should read:

1. `docs/csharp-port.md`
2. `docs/orchestration-rules.md`
3. `docs/parity-verification.md`
4. latest `docs/Phase-6-Session-*-Completion.md`
5. latest `docs/Phase-6-Session-*-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is historical archive material only.

Session startup is not a validation trigger. Do not run the broad .NET suite, an unfiltered project-wide test run, or a full solution build during startup, ordinary handoff review, or as an end-of-unit habit.

Use focused validation by default:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single service/planner/schema/test change: run filtered `dotnet test` for that test class plus directly adjacent classes only.
- Packet/parser or boundary change: run only the immediately related packet/parser/boundary tests.
- Java parity check: run a targeted Maven test only when a narrow Java fixture or source-of-truth command exists.
- Full project test, solution test, or solution build: run only after documenting a broad-validation trigger from `docs/orchestration-rules.md`.

Filtered `dotnet test` commands already build the affected project and dependencies. Treat a passing filtered test command as the compile signal unless a named broad-validation trigger requires wider validation.

Use this validation decision template in future completion/handoff docs:

```text
Validation decision:
- Changed surface:
- Focused C# command:
- Focused Java/Maven command:
- Broad-validation trigger:
- Broad .NET decision:
- Why this scope is sufficient:
```

## Current State

- Phase 6 remains in progress.
- Java remains the source of truth.
- `PHASE-6-PROGRESS.md` remained untouched and should not be reopened for normal startup.
- Completion/handoff docs are the active progress/parity record.
- Live `CM_FIND_GROUP` dispatch remains deferred in `GameServerConnection.ProcessPacketAsync`.
- Checked-in Java action `2`/`6` mutation-post artifacts exist under `parity-artifacts/find-group/mutation-post/java` and are shape-valid to the C# artifact reader.
- Java action `2`/`6` capture hooks and fixture-side artifact writer/validator scaffolds exist, but they remain non-live until capture-enabled runtime artifacts are generated and compared.
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService` consumes paired-readiness rows and emits blocked planned rows for missing Java input, missing C# input, or deferred value comparison.
- `FindGroupMutationPostProjectedRowComparisonValueContractService` names required Java/C# value sources for every required equality field and keeps runtime-only fields as ignored context.
- `FindGroupMutationPostProjectedRowComparisonBlockedResultReportService` combines the executor skeleton and value contract into a final non-live pre-execution report with all planned output rows marked unavailable.
- `FindGroupMutationPostProjectedRowComparisonReadinessSummaryService` links the dry-run, executor skeleton, value contract, and blocked-result report into one top-level readiness summary.
- `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` enumerates the exact runtime artifacts required before summary metadata can become real comparison execution.
- `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService` now maps each live-input requirement to existing non-live providers or future runtime evidence producers.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2219 Summary

This UOW added a non-live runtime evidence checklist after the live-input handoff contract.

Key behavior:

- maps every live-input handoff requirement to an existing provider or future evidence producer,
- records required next evidence for Java runtime artifacts, C# live boundary rows, boundary executor invocation, registry send observation, value projection, row identity matching, result emission, live dispatch guard, and runtime/socket comparison,
- distinguishes existing non-live metadata, existing non-live scaffold, live dispatch disabled, and comparison not executed states,
- keeps `HasAnyRuntimeEvidence=false`, `CanStartProjectedComparison=false`, `CanClaimVerifiedParity=false`, and `IsLive=false`,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The checklist is a provider map only.
- It does not read Java/C# values, compare fields, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.
- Existing provider names are scaffolds, not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`
- `com.aionemu.gameserver.services.findgroup.FindGroupMutationPostTraceCaptureHooks`
- `game-server/test/com/aionemu/gameserver/services/findgroup/FindGroupMutationPostTraceCaptureTest`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService`
- Existing mutation-post Java artifact, C# boundary-row, registry observation, value projection, comparison preflight, and result contract services under `Aion.GameServer.Services`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2219-Completion.md`
- `docs/Phase-6-Session-2219-Handoff.md`

## Validation In UOW-2219

Validation decision:

- Changed surface: C# service/test-only non-live runtime evidence checklist plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only maps reviewed Java/C# scaffolds to future runtime evidence requirements.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Runtime evidence checklist rows, live-input handoff rows, readiness summary rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The checklist makes evidence ownership visible, but it cannot prove parity.
- Future implementation must avoid treating provider presence as evidence completion.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison execution-readiness gate that combines the live-input handoff and runtime evidence checklist into one final go/no-go report before any comparator implementation.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2219] Add find group mutation runtime evidence checklist
```
