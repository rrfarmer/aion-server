# Phase 6 Session 2218 Handoff - FindGroup Mutation Live Input Handoff Contract

Date: 2026-06-02
Unit of Work: UOW-2218
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
- `FindGroupMutationPostProjectedRowComparisonDryRunContractService` carries shape-valid Java artifact row references, accepted guarded C# row references, and paired readiness rows for action `2`/`6` future executor inputs.
- `FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService` consumes paired-readiness rows and emits blocked planned rows for missing Java input, missing C# input, or deferred value comparison.
- `FindGroupMutationPostProjectedRowComparisonValueContractService` names required Java/C# value sources for every required equality field and keeps runtime-only fields as ignored context.
- `FindGroupMutationPostProjectedRowComparisonBlockedResultReportService` combines the executor skeleton and value contract into a final non-live pre-execution report with all planned output rows marked unavailable.
- `FindGroupMutationPostProjectedRowComparisonReadinessSummaryService` links the dry-run, executor skeleton, value contract, and blocked-result report into one top-level readiness summary.
- `FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService` now enumerates the exact runtime artifacts required before summary metadata can become real comparison execution.
- No service currently reads Java/C# mutation-post row values, compares them, or emits real `Matched`, missing-row, `FieldMismatch`, or ignored-context results.

## UOW-2218 Summary

This UOW added a non-live runtime input handoff contract after the projected-row readiness summary.

Key behavior:

- emits requirement rows for projected-row readiness metadata, Java runtime trace artifacts, C# live boundary rows, boundary executor invocation, registry send observation, value projection, row identity matching, result emission, live dispatch guard status, and runtime/socket comparison,
- treats summary-ready metadata as non-live only,
- keeps every runtime requirement blocked,
- keeps `CanStartLiveComparison=false`, `CanEnableLiveDispatch=false`, and `IsLive=false`,
- keeps production `CmFindGroup` dispatch disabled.

Important notes:

- The handoff contract is a runtime artifact checklist only.
- It does not read Java/C# values, compare fields, execute side effects, or registry sends.
- It does not wire production `CmFindGroup` dispatch.
- Shape-valid Java rows, disabled C# projections, and synthetic summary rows in tests are not runtime evidence.

## Java Artifacts Reviewed

- `com.aionemu.gameserver.network.aion.clientpackets.CM_FIND_GROUP`
- `com.aionemu.gameserver.services.findgroup.FindGroupService`

## C# Artifacts Reviewed

- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonDryRunContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonExecutorSkeletonService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonValueContractService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonBlockedResultReportService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonReadinessSummaryService`
- `Aion.GameServer.Services.FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService`

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests.cs`
- `docs/Phase-6-CmFindGroup-Live-Dispatch-Design.md`
- `docs/Phase-6-Session-2218-Completion.md`
- `docs/Phase-6-Session-2218-Handoff.md`

## Validation In UOW-2218

Validation decision:

- Changed surface: C# service/test-only non-live live-input handoff contract plus non-live design/session documentation.
- Focused C# command:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonLiveInputHandoffContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonReadinessSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonBlockedResultReportServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonValueContractServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutorSkeletonServiceTests" --no-restore
```

- Focused Java/Maven command: not run; no Java source or Java fixture file changed in this UOW, and the new C# service only enumerates runtime artifacts still required after reviewing Java source.
- Broad-validation trigger: none.
- Broad .NET decision: skipped intentionally because filtered tests built the affected C# project/dependencies and no broad trigger applied.
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

## Known Gaps

- Live `CM_FIND_GROUP` dispatch remains disabled.
- Readiness summary rows, live-input handoff rows, blocked-result report rows, value contract rows, executor skeleton rows, paired readiness, shape-valid Java fixture artifacts, disabled C# projection rows, guarded fixture skeleton metadata, readiness aggregate metadata, guarded fixture result contract metadata, envelope handoff metadata, and dry-run row references are not Java/C# runtime comparison evidence.
- Live C# mutation-post rows, Java runtime trace artifacts, executor observation from the guarded boundary, registry observation, real projected-row value projection/comparison, result emission, world-broadcast fanout, action `12` invite dispatch, and runtime/socket comparison are still missing.

## Remaining Risks

- The handoff contract can make missing runtime evidence visible, but it cannot prove parity.
- Future implementation must avoid treating shape-valid Java artifacts or disabled C# fixture rows as live evidence.
- Live dispatch remains a broad-validation trigger if it is enabled later.

## Next Recommended Unit of Work

Next sequential task:

- Add a non-live projected-row comparison runtime evidence checklist that maps each live-input handoff requirement to the specific existing or future fixture/service expected to satisfy it.

Safe candidates:

- Add a deterministic Java artifact timestamp override if future fixture churn around `serverEpochSeconds` becomes noisy.
- Add live boundary or runtime trace evidence for shared singleton caller interleavings before enabling any live `CM_FIND_GROUP` direct-packet dispatch.
- Add more precise readiness blocker wording for executor versus registry observations if the result contract needs separate status rows.

## Commit

Commit message:

```text
[Phase 6][UOW-2218] Add find group mutation live input handoff contract
```
