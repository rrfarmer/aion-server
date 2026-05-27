# Phase 6AKG Completion - Revive Aggro Cleanup Audit

Date: 2026-05-27
Unit of Work: UOW-1457
Status: Complete after audit and documentation.

## Scope

Audit Java `PlayerReviveService.revive` aggro cleanup and determine whether the C# port has a safe equivalent surface to wire live during kisk revive.

## Completed Work

- Verified Java `PlayerReviveService.revive` calls `player.getAggroList().clear()` after resurrection skill reset.
- Verified the cleanup runs before `PlayerController.onBeforeSpawn`, group/alliance movement updates, and resurrect emotion fanout.
- Verified Java `PlayerAggroList` extends `AggroList` and is used by players and summons.
- Verified Java `AggroList.clear()` cancels `hateReductionTask` and clears the concurrent aggro map.
- Audited C# aggro surfaces and found only NPC-owned combat state in `WorldNpcCombatStateService`; no player-owned aggro list or hate-reduction task exists.
- Left production code unchanged because clearing NPC combat state during revive would not match Java player-owned aggro cleanup semantics.

## Validation

- Ran read-only searches over Java and C# aggro/revive surfaces.
- Confirmed the worktree was clean after UOW-1456 before starting this docs-only audit.

## Migration Parity Table - UOW-1457

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.player.PlayerReviveService` | `GameServerConnection.HandleReviveAsync` | Service / Connection Workflow | Partial | Regression Tested in prior unit | Partial Parity | Java revive clears `player.getAggroList()` before `onBeforeSpawn`, team movement updates, and resurrect emotion. C# revive still has no live player-owned aggro cleanup because the equivalent player aggro model is missing. |
| `com.aionemu.gameserver.controllers.attack.PlayerAggroList` | No C# equivalent discovered | Class / Combat State | Not Started | No Tests | Needs Verification | Java player/summon aggro list extends `AggroList` and gates awareness through known-list membership. C# has no player-owned hate map or known-list-aware player aggro surface. |
| `com.aionemu.gameserver.controllers.attack.AggroList` | `WorldNpcCombatStateService` is related but not equivalent | Class / Combat State | Partial | No Tests in this unit | Needs Verification | Java `AggroList.clear()` cancels `hateReductionTask` and clears a concurrent hate map. C# `WorldNpcCombatStateService.Clear(npcObjectId)` removes NPC combat state by NPC id; using it for revive would clear the wrong owner and lose NPC combat semantics. |
| `com.aionemu.gameserver.model.stats.container.PlayerLifeStats` | `WorldNpcResourceStatsService.IncreasePlayerHpAsync` result metadata | Stats / Resource Service | Partial | Existing Unit Coverage | Partial Parity | Java `PlayerLifeStats.onHpChanged` also clears owner aggro when HP is fully restored. C# resource result exposes `ClearAggroOnFullHp`, but no player aggro executor exists. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Audit Only | `PlayerReviveService.revive`, `PlayerAggroList`, `AggroList.clear` | No code path was changed because the C# mutation target is absent. | Source audit only. | Requires future player-owned aggro model before testable live parity. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Player-owned aggro state, known-list-aware awareness checks, master damage transfer, hate reduction scheduling, and `clear()` task cancellation are not ported.
- C# NPC combat aggro state must not be reused for revive player aggro cleanup without a deliberate model bridge.
- Threading difference is material: Java `AggroList.clear()` synchronizes task cancellation and clears a concurrent map; no C# player-side task exists.
- Serialization and date/time behavior were not changed in this unit.

## Summary Metrics

- Total Java artifacts discovered: 4 grouped artifact rows in this unit
- Total artifacts ported: 0 production artifacts in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4 grouped rows
- Total blocked artifacts: player-owned aggro model, hate-reduction task, Java runtime artifact generation
- Estimated overall migration completion: Phase 6 remains about 72% complete

# Next Unit Of Work

## Recommended Sequential Task

- Task: switch to toy-pet lifetime schedule observability design or start a dedicated player-owned aggro model design UOW.
- Do not wire revive aggro cleanup until a C# `PlayerAggroList` equivalent and hate-reduction/task-cancellation semantics exist.

## Suggested Acceptance Criteria

- If choosing toy-pet scheduling, document the Java source and current C# scheduling hooks before adding production code.
- If choosing player aggro design, identify owner semantics first: player/summon vs NPC, known-list awareness, master transfer, and hate-reduction task lifecycle.
- Keep revive handler changes paused until the model prerequisite exists.
- Update the Migration Parity Table for every Java artifact touched.
- Do not claim Java runtime parity without generated Java artifacts.

## Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Toy-pet lifetime schedule observability design | docs or fixture design | Medium | Good next safe unit after the aggro blocker. |
| B | Player aggro model design | Java/C# model docs, future service skeleton | High | Do not combine with live revive wiring. |
| C | Kisk revive socket-order hardening | kisk workflow tests | Medium | Sequential only if touching shared fixture again. |

## Do Not Parallelize

- Shared revive handler edits.
- Any future player aggro model and revive live wiring in the same unit.
- Shared progress/handoff docs.
- Git staging and commit.

## Context For Next Session

- Current unit should be committed with message `[Phase 6][UOW-1457] Audit revive aggro cleanup blocker`.
- Files changed in UOW-1457:
  - `docs/PHASE-6-PROGRESS.md`
  - `docs/Phase-6AKG-Completion.md`
