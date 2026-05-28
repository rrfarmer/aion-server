# Phase 6AOY Completion - AP Extraction WING Target Parity

Date: 2026-05-27
Unit of Work: UOW-1579
Status: Complete after validation.

## Scope

Continue scheduled item-use/AP extraction parity by matching Java's rejection of `WING` item-group targets in `ApExtractAction.canAct`.

## Completed Work

- Performed Parallel Work Discovery across AP extraction, extraction partial-failure behavior, and independent protection readiness work.
- Spawned one read-only Explorer sub-agent to verify Java AP extraction `WING` behavior.
- Closed the Explorer after completion; it made no file changes.
- Verified Java source behavior:
  - `ApExtractAction.canAct` switches on `targetItem.getItemTemplate().getItemGroup()`;
  - Java has no `case WING`;
  - `ItemGroup.WING` is a distinct enum constant;
  - real wing items therefore fall to `default` and return `false`.
- Updated `ApExtractService.GetTargetType` so C# rejects `WING` targets like Java.
- Added a regression assertion for a `WING` item that otherwise has matching AP extraction metadata.

## Validation

- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "ApExtract"`.
- Result: passed 5 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter GameServerConnectionInventoryExpansionUseItemTests`.
- Result: passed 78 tests.
- Ran `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj`.
- Result: passed 3350 tests.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|---|
| A | AP extraction WING target classification | `ApExtractAction`, `ItemGroup` | `ApExtractService.cs`, `ApExtractServiceTests.cs` | Integration Fix / Regression Test | Yes with read-only Java check | Low-Medium | Java rejects `WING`; C# accepted it via `IsWing`. Tight isolated service/test change. |
| B | Extraction target-before-tool partial failure | `ExtractAction`, `EnchantService` | `EnchantService.cs`, `GameServerConnection.cs`, inventory tests | Integration Fix | No with A | Medium-High | Requires modeling live storage mutation failure and likely broader runtime/persistence design. |
| C | AP extraction target-before-tool/AP partial failure | `ApExtractAction`, `AbyssPointsService` | `ApExtractService.cs`, connection/tests | Integration Fix | No with A | Medium | Same service file as A and wider AP side effects. |
| D | Hook detail readiness surfacing | protection readiness/export artifacts | protection files | Integration/Test | Yes separately | Medium | Independent from item-use files, but less immediate than the AP extraction mismatch. |

Selected batch:

| Agent | Assigned Task | Task Type | Allowed Files | Forbidden Files | Dependencies | Expected Result |
|---|---|---|---|---|---|---|
| Explorer | Verify Java AP extraction WING target behavior | Java Analysis | read-only Java source under `game-server/src` | all writes, docs, C# edits | none | Confirm whether Java accepts or rejects `UseTarget.WING` for `ItemGroup.WING`. |
| Orchestrator | Implement AP extraction WING parity fix and docs | Integration/Test/Documentation | `ApExtractService.cs`, `ApExtractServiceTests.cs`, progress/handoff docs | Java source writes, unrelated item-use paths | Explorer/local Java review | Focused AP extraction regression and affected tests pass. |

Parallelism was used for read-only Java analysis only. No sub-agent wrote files.

## Migration Parity Table - UOW-1579

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.item.actions.ApExtractAction` | `Aion.GameServer.Services.ApExtractService` | Item Action / Service | Partial | Unit Tested / Regression Tested | Partial Parity | Java `canAct` rejects `ItemGroup.WING` because the switch has no `case WING`; C# now returns `CannotAct` instead of accepting `IsWing`. No Java runtime packet/artifact comparison was executed. |
| `com.aionemu.gameserver.model.templates.item.actions.UseTarget` | `Aion.GameServer.Dataholders.ItemApExtractActionInfo.Target` | Enum-like Action Metadata | Partial | Unit Tested | Needs Verification | The `WING` action target may still be parsed as metadata, but behavior now matches Java's unreachable WING branch for real wing item groups. Other `UseTarget` values remain covered by existing service tests but not runtime-compared. |
| `com.aionemu.gameserver.model.templates.item.enums.ItemGroup` | `Aion.GameServer.Dataholders.ItemTemplateSummary.ItemGroup`; `ItemTemplateSummary.IsWing` | Enum / Static Data Metadata | Partial | Unit Tested | Partial Parity | `ItemGroup.WING` is distinct from `NONE` in Java; C# no longer treats `IsWing` as an AP-extract target type. Broader item group enum coverage remains outside this unit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| Updated `CreateMutationPlan_RequiresLevelQualityMaskAndTargetType` | Unit / Regression | Java source review and Explorer confirmation of `ApExtractAction.canAct` plus `ItemGroup.WING` enum behavior | A target item with `ItemGroup` `WING`, matching quality/level, `CanApExtract` mask, and AP acquisition data returns `ApExtractFailure.CannotAct`. | Source-review backed C# regression; AP extraction focused tests and full game-server suite pass. | No Java runtime comparison; AP target-before-tool mutation failure remains future work. |

## Remaining Risks

- No Java runtime packet capture or golden artifact was generated for AP extraction WING rejection.
- The Java `case NONE` WING check appears unreachable; this is preserved for parity rather than corrected.
- AP extraction target-before-tool partial mutation behavior remains unmodeled in C# planner form.
- Other `UseTarget` values such as `OTHER` and `ALL` remain TODO/unknown as in Java comments.
- Java 25 JDK/Maven blocker still prevents new generated Java runtime artifacts in this environment.

## Summary Metrics

- Total Java artifacts discovered: 3 grouped rows in this unit
- Total artifacts ported: 1 production service branch adjusted plus 1 regression assertion
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 3 grouped rows
- Total blocked artifacts: Java runtime packet capture/golden generation for AP extraction WING rejection, Java 25 JDK, Java compiler, Maven, Maven wrapper, and AP extraction target-before-tool runtime comparison
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: continue scheduled item-use ordering with a branch that does not share the just-touched AP target classification path.
- Preferred target: extraction target-before-tool behavior through `ExtractAction` and `EnchantService.breakItem`, or a narrow AP extraction target-before-tool planner note/test only if live storage mutation failure can be modeled without overfitting.
- Why: Java source already shows more non-atomic item-use branches, but the extraction/AP target-before-tool cases need careful modeling because C# planners operate from snapshots.
- Scope:
  - choose exactly one branch;
  - source-review Java mutation and packet order;
  - avoid broad runtime refactors;
  - add a focused regression or document why a runtime fix needs a deeper storage mutation boundary;
  - keep persistence-boundary differences explicit.

## Suggested Acceptance Criteria

- One isolated branch is source-reviewed and either covered or explicitly documented as needing a new mutation boundary.
- Focused and affected tests pass.
- Progress and handoff docs include a conservative parity table.
- Completed unit is committed before starting another.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Extraction target-before-tool branch | extraction service/connection/tests | Medium-High | Avoid overfitting; may need a design note if live mutation failure cannot be represented cleanly. |
| B | AP extraction target-before-tool/AP branch | AP extraction service/connection/tests | Medium | Same caution as extraction; no-delay path. |
| C | Decompose success-message-before-reward audit | decompose tests/services | Medium | Existing tests may already cover much of this. |
| D | Hook detail readiness surfacing | protection readiness/export files | Medium | Independent from item-use files. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Explorer | Optional read-only deep dive for selected extraction/AP partial branch only | read-only Java source inspection | all writes, shared docs, C# edits |
| Orchestrator | Implement or document one focused parity branch | selected service/test files plus progress/handoff docs | Java source writes, unrelated item-use paths |

## Do Not Parallelize

- Multiple writers in `GameServerConnectionInventoryExpansionUseItemTests.cs`.
- Multiple writers in `ApExtractService.cs` if AP extraction is selected.
- Broad item-use runtime refactors.
- Java generator implementation without Java 25 JDK and Maven.
- Shared progress/handoff docs.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1579] Reject AP extraction wing targets`.
- Files changed in UOW-1579:
  - `dotnetConversion/src/Aion.GameServer/Services/ApExtractService.cs`
  - `dotnetConversion/tests/Aion.GameServer.Tests/ApExtractServiceTests.cs`
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AOY-Completion.md`
- Latest prior commits:
  - `2158444f7 [Phase 6][UOW-1578] Preserve assembly partial consumes`
  - `91c11d286 [Phase 6][UOW-1577] Validate full dotnet solution`
  - `10afdc8f6 [Phase 6][UOW-1576] Triage inventory cleanup-seal regressions`
- Required startup remains:
  - read `docs/csharp-port.md`;
  - read `docs/PHASE-6-PROGRESS.md`;
  - read latest `docs/Phase-6*-Completion.md`;
  - read `docs/orchestration-rules.md`;
  - read `docs/parallelization-strategy.md`;
  - read `docs/parity-verification.md`;
  - note `docs/commit-conventions.md` is still missing.
