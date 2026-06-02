# Phase 6 Session 2296 Handoff - Checklist Row-Pairing Evidence

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2296-Completion.md`
- `docs/Phase-6-Session-2296-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. Before running validation, name the specific behavior, packet shape, metadata contract, or documentation invariant being checked. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active notes already name a broad-validation trigger, focused evidence of wider risk, an explicit user request, or a release/readiness checkpoint.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2296 surfaced Java/C# row-pairing readiness evidence inside the runtime evidence checklist.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`

The runtime evidence checklist now accepts an optional `FindGroupMutationPostJavaCSharpRowPairingReadinessReport` and appends row-pairing status plus `JavaPostCaptureDryRunCommandConsistencyEvidence` to the `RowIdentityMatching` checklist row. This is intentionally opt-in to avoid recursively constructing the row-pairing/readiness/checklist evidence chain.

No Java source, fixtures, live dispatch, packet sends, reader invocation, value reads, runtime comparison, capture execution, executable implementation, materialization, result emission, or verified parity status changed.

## Java Source Context

Reviewed source-of-truth Java:

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
- `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`

Relevant Java facts:

- Action `2` reads `playerOrTeamId`, `message`, `groupType`, then calls `FindGroupService.addRecruitment(player, message, groupType)`.
- Action `6` reads `playerOrTeamId`, `message`, `groupType`, `classId`, `level`, then calls `FindGroupService.addApplication(player, message, groupType, classId, level)`.
- `addRecruitment` sends posted system message `1400392` before refreshed `SM_FIND_GROUP` action `0`.
- `addApplication` sends posted system message `1400393` before refreshed `SM_FIND_GROUP` action `4`.
- Both mutation-post actions use direct sends, with zero world broadcasts and zero invite dispatches expected.

## Validation From Last Session

Specific behavior/contract validated:

- The runtime evidence checklist preserves row-pairing readiness evidence on the row-identity requirement while continuing to block runtime comparison and verified parity until accepted live C# boundary rows, runtime row values, and comparison evidence exist.

Focused C# validation, first attempt:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests" --no-restore
```

Result: failed because default-creating row-pairing readiness from the checklist introduced a recursive non-live evidence-chain construction path and crashed the test host with a stack overflow. The implementation was corrected so row-pairing evidence is opt-in and no default row-pairing report is created by the checklist.

Focused C# validation, final:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests" --no-restore
```

Result: passed 10, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2296; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed runtime evidence checklist plus the directly adjacent row-pairing readiness report.

Commit made:

```text
[Phase 6][UOW-2296] Surface row-pairing evidence in checklist
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Runtime evidence checklist changes: run runtime evidence checklist tests plus directly adjacent row-pairing readiness tests.
- Execution readiness gate changes: run execution readiness gate tests plus directly adjacent runtime evidence checklist tests.
- Value projection handoff changes: run value projection handoff tests plus directly adjacent execution readiness gate and runtime-row-value intake tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Surface runtime evidence checklist row evidence inside the projected row comparison execution readiness gate. Keep the unit metadata-only: the execution readiness gate should continue consuming `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklist`, but preserve the row-identity checklist evidence that now carries row-pairing readiness and post-capture dry-run command consistency evidence.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update execution readiness gate metadata and tests.
- Update completion/handoff docs and commit.

Specific behavior/contract for the next UOW:

- The projected row comparison execution readiness gate preserves row-identity checklist evidence, including row-pairing readiness and post-capture dry-run command consistency evidence, while continuing to block runtime comparison and verified parity until runtime evidence and comparison output exist.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests|FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests" --no-restore
```

If that command is slow, first narrow to only `FindGroupMutationPostProjectedRowComparisonExecutionReadinessGateServiceTests`.

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Surface execution readiness gate evidence into value projection handoff after execution readiness carries checklist evidence.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Continue surfacing existing evidence into downstream runtime-comparison planning reports, provided each unit remains metadata-only and has a focused test recipe.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates with required field metadata, accepted-boundary-row handoff metadata visible in checklist inventory, Java/C# row-pairing readiness that preserves post-capture dry-run command consistency evidence, runtime checklist row identity metadata that preserves row-pairing readiness evidence, value-projection handoff metadata that preserves row-pairing handoff evidence, runtime-row-value intake metadata that preserves value-projection handoff row evidence, typed-reader implementation readiness metadata that preserves runtime-row-value intake row evidence, value-reader invocation preflight metadata that preserves typed-reader gate row evidence, projected-value row metadata that preserves function preflight row evidence, materialization blocker metadata that preserves projected-value row evidence, result-emission blocker metadata that preserves materialization blocker evidence, executor evidence bridge metadata that preserves result-emission blocker row evidence, projected-value executor consistency audit metadata that preserves executor evidence bridge row evidence, runtime-comparison handoff metadata that preserves executor consistency audit row evidence, live-capture preflight runbook metadata that preserves runtime-comparison handoff row evidence, capture acceptance matrix metadata that preserves live-capture preflight row evidence, capture execution blocker summary metadata that preserves capture acceptance matrix row evidence, capture command decision metadata that preserves capture execution blocker summary row evidence, capture command consistency metadata that preserves capture command decision row evidence, explicit-root Java capture dry-run metadata that preserves command consistency evidence, and explicit-root Java post-capture summary metadata that preserves dry-run command consistency evidence, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
