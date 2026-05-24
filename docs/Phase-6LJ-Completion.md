# Phase 6LJ Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6LI and covers Session 810.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonSkillExecutionServiceTests|StaticDataNpcSkillTests"`
  - Result: Passed, 49 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1399 tests.

## Recent Work Completed

### Session 810 - Represented NPC Skill Action Workflow

- Re-inspected Java `SkillAttackManager.chooseNextSkill`, `getNpcSkillEntryIfNotTooFarAway`, `targetTooFar`, and `skillAction`.
- Added `PreviewMercenaryNpcSkillActionWorkflow`.
- Added represented `PlayerSummonKnownObjectNpcSkillActionWorkflowPreview` and `PlayerSummonKnownObjectNpcSkillActionWorkflowPreviewStatus`.
- Composed existing represented slices into one workflow result:
  - selection preview candidate;
  - represented current-target range invalidation;
  - Java 5000 ms next-skill-delay storage;
  - selected skill readiness;
  - action target selection;
  - action preview;
  - action-result / AI side-effect intent.
- Kept live `NpcAI`, `NpcGameStats`, `DataManager.SKILL_DATA`, live skill `Properties`, target lookup, can-see/range math, known-list/aggro target resolution, controller execution, target mutation, packets, threading, serialization, date/time scheduling, precision/rounding, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.chooseNextSkill` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillActionWorkflow` via represented selection preview | Service | Partial | Regression Tested | Needs Verification | C# composes an already represented selected candidate into a workflow. Live AI, queue mutation, Java random ordering, and runtime Java behavior remain unverified. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.getNpcSkillEntryIfNotTooFarAway` | `PreviewMercenaryNpcSkillActionWorkflow` plus `ApplyMercenaryTargetRangeDelay` | Service | Partial | Regression Tested | Needs Verification | C# preserves represented target-range veto and 5000 ms delay storage before action preview. Live `NpcGameStats` mutation and scheduler/date-time parity remain missing. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.targetTooFar` | `EvaluateMercenaryTargetRange(...)` inside workflow | Service | Partial | Regression Tested | Needs Verification | C# evaluates represented target death/visibility/can-see/range facts and AREA bypass. Live skill `Properties`, `owner.canSee`, `PositionUtil`, precision/rounding, collision, and geo behavior remain unverified. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.isReady` | `EvaluateMercenarySkillReadiness` inside workflow | Service | Partial | Regression Tested | Needs Verification | C# threads selected candidate timing/condition readiness into abnormal/transform skill gates. Live `DataManager.SKILL_DATA`, full effect-controller state, reflection, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.ai.manager.SkillAttackManager.skillAction` | `PreviewMercenaryNpcSkillActionWorkflow`, `PreviewMercenaryNpcSkillAction`, `ProjectMercenaryNpcSkillActionResult` | Service | Partial | Regression Tested | Needs Verification | C# exposes one represented workflow result for selected candidate, readiness, target selection, action preview, and AI side-effect intent. Live AI/controller execution, target mutation, effect application, and packets remain missing. |
| `com.aionemu.gameserver.controllers.CreatureController` | workflow action-result intent flags | Controller Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# records abort/use-skill intent only. Live `abortCast`, `useSkill`, effects, cooldowns, packets, threading, and serialization remain missing. |
| `com.aionemu.gameserver.world.knownlist.KnownList` / `AggroList` target selection | `SelectMercenaryNpcSkillActionTarget` inside workflow | Known-list / Aggro Dependency | Not Started | Regression Tested as metadata only | Needs Verification | C# consumes represented target-availability booleans. Live known-list traversal, aggro ordering, Java map/concurrent mutation behavior, target identity, and packet behavior remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillActionWorkflow_ComposesJavaSelectionRangeAndActionSlices`
  - Validates ready selected-candidate workflow projection.
  - Validates represented target-range readiness and no-delay ready path.
  - Validates 5000 ms target-range delay storage when represented first-target range blocks the candidate.
  - Validates selected skill readiness re-evaluation and target-selection metadata.
  - Validates action preview/result projection and blocked after-use behavior.
  - Validates missing selection guard.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live AI state, live target lookup, live visibility/range math, known-list or aggro target selection, controller abort/use behavior, reflection behavior, threading behavior, serialization behavior, date/time behavior, precision/rounding behavior, packets, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill-action workflow composition slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `SkillAttackManager`, live `NpcAI`, live `NpcGameStats`, live `DataManager.SKILL_DATA`, live skill `Properties`, live `owner.getTarget`, live `owner.canSee`, live `PositionUtil.isInRange`, live known-list/aggro target selection, Java random/ordering behavior, target mutation, `CreatureController.abortCast`, `CreatureController.useSkill`, effect application, packets, threading/serialization, precision/rounding, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The workflow remains represented metadata. It does not schedule or execute live AI, mutate live `NpcGameStats`, resolve live skill properties, perform live target/range/visibility lookups, call controller methods, set targets, apply effects, or send packets.
- Live target identity, Java random/ordering behavior, known-list and aggro-list mutation under concurrency, date/time scheduling, reflection behavior, threading, serialization, precision/rounding, persistence, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue NPC skill action parity by adding a capture boundary for the represented workflow result so `CaptureMercenaryNpcSkillPreview` can retain selection, action preview, post-spawn preview, and action workflow metadata together for later live AI/controller wiring. Keep live `NpcAI`, controller execution, target mutation, packets, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6LI-Completion.md`
   - this handoff
3. Inspect `CaptureMercenaryNpcSkillPreview`, `PlayerSummonKnownObjectNpcSkillPreviewCaptureResult`, represented preview storage on `Player`, and the new `PlayerSummonKnownObjectNpcSkillActionWorkflowPreview`.
4. Add a narrow capture/storage boundary for workflow metadata without invoking live AI/controller behavior.
5. Keep unsupported live AI/controller, target mutation, packets, threading, serialization, and live-client behavior explicit.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
