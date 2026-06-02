# Phase 6 Session 2253 Handoff - Focused Validation Documentation Update

## Startup Instructions

For the next session, read these documents first:

- `docs/csharp-port.md`
- `docs/orchestration-rules.md`
- `docs/parity-verification.md`
- `docs/Phase-6-Session-2253-Completion.md`
- `docs/Phase-6-Session-2253-Handoff.md`

Do not read `docs/PHASE-6-PROGRESS.md` during normal startup. It is a historical archive; current working state lives in the latest completion and handoff documents.

Java remains the source of truth. Do not claim verified parity without objective Java/C# runtime evidence.

## Current State

UOW-2253 was documentation-only. It tightened Phase 6 validation policy so routine sessions avoid slow full `.NET` tests/builds unless a broad-validation trigger is documented first.

Updated artifacts:

- `docs/orchestration-rules.md`
- `docs/csharp-port.md`
- `docs/Phase-6-Session-2253-Completion.md`
- `docs/Phase-6-Session-2253-Handoff.md`

No Java source, C# source, test fixtures, generated artifacts, scripts, or run commands changed.

## Focused Testing Policy

Use the Phase 6 test targeting matrix in `docs/orchestration-rules.md` before running validation.

Default rules:

- Documentation-only change: run `git diff --check`; skip runtime tests.
- Single C# non-live service/report/planner change: run filtered `dotnet test` for the edited test class plus closest adjacent contract/checklist classes.
- Packet/parser/boundary shape change: run only the directly related packet/parser/golden/boundary tests.
- Java source or fixture change: run targeted Maven `-Dtest=SpecificJavaTest` where possible.
- Full `.NET` project tests, solution tests, or solution builds require a named broad-validation trigger in the active completion/handoff notes.
- A passing filtered `dotnet test` is the compile/build signal for the affected project and dependencies.
- If a focused command is still slow, narrow the filter first and document residual risk.

Do not run full `.NET` validation as a startup heartbeat, reassurance step, or ordinary end-of-unit habit.

## Validation From This Session

Focused hygiene validation:

```powershell
git diff --check
```

Result: passed. Git emitted line-ending normalization warnings for existing Markdown files, but no whitespace errors.

Java/Maven: not applicable; no Java source or fixture changed.

C# runtime tests: not applicable; no C# source, test code, generated artifact, script, or executable command changed.

Broad `.NET` validation: skipped because there was no broad-validation trigger and this was documentation-only.

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
- Run filtered C# tests for the new report plus explicit-root post-capture summary and C# live-boundary row intake preflight tests.
- Skip Java/Maven unless Java source or fixture changes; document Java source review.
- Skip full `.NET` validation unless a broad trigger is documented.
- Update completion/handoff docs and commit.

Safe alternate candidates:

- Capture live boundary/runtime trace evidence for shared singleton caller interleavings.
- Tighten precise blocker wording for executor observations versus registry observations.
- Add a narrow C# documentation-hygiene report that lists current focused validation commands by artifact type.

## Parity Caution

Current status remains partial parity only. The project has explicit-root Java artifact validation and C# accepted-row intake gates, but verified parity is still blocked by missing actual accepted live C# boundary rows, missing Java/C# row pairing result, value projection/materialization/emission evidence, no runtime comparison execution, and no executable value-reader implementation.
