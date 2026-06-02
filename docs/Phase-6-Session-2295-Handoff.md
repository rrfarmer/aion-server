# Phase 6 Session 2295 Handoff - Row-Pairing Post-Capture Evidence

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2295-Completion.md`
- `docs/Phase-6-Session-2295-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# evidence.

Use focused validation by default. Before running validation, name the specific behavior, packet shape, metadata contract, or documentation invariant being checked. A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies. Do not run full `.NET` project tests, solution tests, or solution builds unless the active notes already name a broad-validation trigger, focused evidence of wider risk, an explicit user request, or a release/readiness checkpoint.

If a focused command is expected to take several minutes, narrow it first to the edited test class and nearest adjacent contract class. Full `.NET` validation is not a substitute for choosing the specific command that proves the scoped Java parity question.

## Current State

UOW-2295 surfaced explicit-root Java post-capture dry-run command consistency evidence inside the Java/C# row-pairing readiness report.

Updated artifacts:

- `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostValueProjectionHandoffGateServiceTests.cs`

The row-pairing readiness report now includes `JavaPostCaptureDryRunCommandConsistencyEvidence`, sourced from `FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummary.DryRunCommandConsistencyEvidence`. It preserves the post-capture dry-run evidence chain when Java artifacts are missing and when Java/C# action pairs can feed value projection but runtime comparison remains blocked.

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

- The Java/C# row-pairing readiness report preserves post-capture dry-run command consistency evidence while continuing to block runtime comparison and verified parity until accepted live C# boundary rows and runtime row values exist.

Focused C# validation, first attempt:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests" --no-restore
```

Result: failed during test project compilation because `FindGroupMutationPostValueProjectionHandoffGateServiceTests.ReadyPairingReport()` constructed `FindGroupMutationPostJavaCSharpRowPairingReadinessReport` directly and needed the new metadata argument. The fixture was updated.

Focused C# validation, final:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests|FullyQualifiedName~FindGroupMutationPostExplicitRootJavaPostCaptureValidatorSummaryServiceTests|FullyQualifiedName~FindGroupMutationPostValueProjectionHandoffGateServiceTests" --no-restore
```

Result: passed 15, failed 0, skipped 0. Existing nullable/analyzer warnings remain from unrelated files.

Repository hygiene:

```powershell
git diff --check
```

Result: passed. Git reported line-ending normalization warnings only for edited files.

Focused Java/Maven validation: not run. No Java source or fixture changed in UOW-2295; Java source was reviewed directly for the non-live metadata context.

Broad-validation trigger: none. Full `.NET` project tests, solution tests, and full solution builds were skipped because the filtered C# command built the affected project and dependencies and covered the changed row-pairing readiness report, the directly adjacent post-capture validator summary, and the adjacent value-projection handoff test fixture touched for constructor compatibility.

Commit made:

```text
[Phase 6][UOW-2295] Surface post-capture evidence in row pairing
```

## Focused Testing Guidance

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

For this artifact family:

- Java/C# row-pairing readiness changes: run Java/C# row-pairing readiness tests plus directly adjacent post-capture validator summary tests; include value-projection handoff tests if a direct row-pairing report fixture is touched.
- Runtime evidence checklist changes: run runtime evidence checklist tests plus directly adjacent row-pairing readiness tests.
- Value projection handoff changes: run value projection handoff tests plus directly adjacent row-pairing readiness and runtime-row-value intake tests.
- Full `.NET` project tests, solution tests, or solution builds require a documented broad-validation trigger from `docs/orchestration-rules.md`.

Treat a passing filtered `dotnet test` as the compile/build signal for the affected project and dependencies. Do not follow it with a full solution build or full test suite just to get a second compile signal.

## Next Recommended UOW

Surface row-pairing readiness evidence inside the runtime evidence checklist. Keep the unit metadata-only: the runtime evidence checklist should continue listing existing providers, but its Java artifact row can preserve row-pairing readiness evidence including post-capture dry-run command consistency evidence.

Suggested scope:

- Inspect:
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistService.cs`
  - `dotnetConversion/src/Aion.GameServer/Services/FindGroupMutationPostJavaCSharpRowPairingReadinessReportService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests.cs`
  - `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_FIND_GROUP.java`
  - `game-server/src/com/aionemu/gameserver/services/findgroup/FindGroupService.java`
- Keep the unit non-live. It should only update runtime evidence checklist metadata and tests.
- Update completion/handoff docs and commit.

Specific behavior/contract for the next UOW:

- The runtime evidence checklist preserves row-pairing readiness evidence, including post-capture dry-run command consistency evidence, while continuing to block runtime comparison and verified parity until accepted live C# boundary rows, runtime row values, and comparison evidence exist.

Exact focused validation recipe for the next UOW:

```powershell
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests|FullyQualifiedName~FindGroupMutationPostJavaCSharpRowPairingReadinessReportServiceTests" --no-restore
```

If that command is slow, first narrow to only `FindGroupMutationPostProjectedRowComparisonRuntimeEvidenceChecklistServiceTests`.

Focused Java/Maven command for the next UOW: not expected unless Java source or fixtures change. Java source review of `CM_FIND_GROUP.java` and `FindGroupService.java` is expected.

Broad-validation trigger for the next UOW: none, unless the implementation crosses live dispatch, persistence, scheduler, crypto, packet primitives, shared infrastructure, or common runtime state.

Broad `.NET` decision for the next UOW: skip full project tests, solution tests, and full solution builds unless a broad trigger becomes true and is documented before the command runs.

Safe alternate candidates:

- Surface runtime evidence checklist row evidence into execution readiness gates once checklist evidence carries row-pairing readiness.
- Capture live boundary/runtime trace evidence only after metadata gates remain visible and a handoff names the exact focused command.
- Continue surfacing existing evidence into downstream runtime-comparison planning reports, provided each unit remains metadata-only and has a focused test recipe.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation, C# accepted-row intake gates with required field metadata, accepted-boundary-row handoff metadata visible in checklist inventory, Java/C# row-pairing readiness that preserves post-capture dry-run command consistency evidence, runtime checklist row identity metadata that names that consumer relationship, value-projection handoff metadata that preserves row-pairing handoff evidence, runtime-row-value intake metadata that preserves value-projection handoff row evidence, typed-reader implementation readiness metadata that preserves runtime-row-value intake row evidence, value-reader invocation preflight metadata that preserves typed-reader gate row evidence, projected-value row metadata that preserves function preflight row evidence, materialization blocker metadata that preserves projected-value row evidence, result-emission blocker metadata that preserves materialization blocker evidence, executor evidence bridge metadata that preserves result-emission blocker row evidence, projected-value executor consistency audit metadata that preserves executor evidence bridge row evidence, runtime-comparison handoff metadata that preserves executor consistency audit row evidence, live-capture preflight runbook metadata that preserves runtime-comparison handoff row evidence, capture acceptance matrix metadata that preserves live-capture preflight row evidence, capture execution blocker summary metadata that preserves capture acceptance matrix row evidence, capture command decision metadata that preserves capture execution blocker summary row evidence, capture command consistency metadata that preserves capture command decision row evidence, explicit-root Java capture dry-run metadata that preserves command consistency evidence, and explicit-root Java post-capture summary metadata that preserves dry-run command consistency evidence, but verified parity is still blocked by missing runtime-backed Java artifacts, missing accepted live C# boundary rows from production dispatch, missing runtime row values, concrete reader invocation, row identity decisions, value comparison, result materialization/emission evidence, runtime comparison execution, and live dispatch.
