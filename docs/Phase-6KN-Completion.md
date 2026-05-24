# Phase 6KN Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KM and covers Session 788.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 56 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1380 tests.

## Recent Work Completed

### Session 788 - Represented NPC Skill Post-Spawn Preview Capture

- Re-inspected the represented known-object preview capture boundary and the Java post-cast spawn path from `NpcSkillTemplateEntry.fireOnEndCastEvents`.
- Added `LastNpcSkillPostSpawnPreview` to `PlayerSummonKnownObject`.
- Extended `Player.TryStoreSummonKnownObjectNpcSkillPreview` to persist the represented post-spawn preview beside list, selection, and action previews.
- Extended `PlayerSummonSkillExecutionService.CaptureMercenaryNpcSkillPreview` with an optional post-spawn preview parameter.
- Kept live `ThreadPoolManager.schedule`, delayed owner-alive recheck execution, `SpawnEngine`, Java RNG, Java geometry/heading math, live AI mutation, controller execution, effects, packets, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.skill.NpcSkillTemplateEntry.fireOnEndCastEvents` represented output | `PlayerSummonKnownObject.LastNpcSkillPostSpawnPreview` / `PlayerSummonSkillExecutionService.CaptureMercenaryNpcSkillPreview` | Post-Cast Spawn Consumer Boundary | Partial | Regression Tested | Needs Verification | Retains represented post-spawn preview metadata on the known object. It does not schedule delayed work, recheck owner state at execution time, or call spawn engine. |
| `com.aionemu.gameserver.model.gameobjects.Npc` post-cast skill state owner | `PlayerSummonKnownObject` preview fields | Known Object State Projection | Partial | Regression Tested | Needs Verification | Stores list, selection, action, and post-spawn previews together. Live `Npc`, `NpcSkillList`, queued/last-skill state, spawn state, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.model.gameobjects.player.Player` known-list access to owner-created mercenary NPCs | `Player.TryStoreSummonKnownObjectNpcSkillPreview` | Player Known-Object Repository Boundary | Partial | Regression Tested | Needs Verification | Updates represented known-object preview state by object id and returns explicit missing-object status. Live known-list visibility, synchronization, threading, and serialization remain unverified. |
| `com.aionemu.gameserver.utils.ThreadPoolManager` delayed spawn scheduling | stored `PlayerSummonKnownObjectNpcSkillPostSpawnPreview.ShouldScheduleSpawn` metadata | Scheduler Dependency | Not Started | Regression Tested as stored metadata only | Needs Verification | Stored preview can identify delayed spawn intent and owner recheck requirement, but no task scheduling, cancellation, date/time runtime, or threading parity exists. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine` | stored `PlayerSummonKnownObjectNpcSkillPostSpawnPreview` metadata only | Spawn Engine Dependency | Not Started | Manual Only as explicit gap | Needs Verification | Live `newSingleTimeSpawn`, instance id handling, owner reference, spawn execution, packets, persistence, and client visibility remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.CaptureMercenaryNpcSkillPreview_StoresRepresentedSelectionAndActionState`
  - Updated to store and verify a delayed post-spawn preview alongside list, selection, and action previews.
  - Preserves explicit missing-known-object behavior.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live scheduler execution, spawn-engine behavior, delayed owner recheck, random number output, geometry/heading math, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, packet behavior, persistence behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 5
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill post-spawn preview capture slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked/not-started artifacts: live `ThreadPoolManager` scheduling, delayed owner recheck execution, Java `Rnd.get`, heading conversion, distance offset math, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, instance id propagation, owner references, XML/static loading, Java geometry, packets, persistence, Java runtime comparison, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- The post-spawn preview is stored as represented metadata only and is not consumed by a scheduler or spawn engine.
- Java delayed owner-alive recheck, `ThreadPoolManager.schedule`, `Rnd.get`, heading conversion, distance offsets, `SpawnEngine.newSingleTimeSpawn`, `SpawnEngine.spawnObject`, packets, persistence, and visibility remain missing.
- Live `Npc`, `NpcSkillList`, queued/last-skill ownership, AI event flow, controller execution, effects, and target mutation remain represented/unwired.
- XML/JAXB loading, reflection behavior, serialization behavior, threading, date/time runtime behavior, precision/rounding, Java runtime, scheduler, geometry, and live-client behavior remain unverified.

## Next Recommended Unit of Work

Continue by modeling the delayed spawn scheduler boundary as represented metadata/results without executing `SpawnEngine`, or start a static-data loader adapter for represented `NpcSkillSpawn` XML fields. Keep XML loading, `DataManager.SKILL_DATA` pruning, Java `Rnd.get`, Java geometry, live AI mutation, controller execution, effects, packets, spawn-engine execution, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KM-Completion.md`
   - this handoff
3. Inspect Java `NpcSkillTemplateEntry.fireOnEndCastEvents`, `ThreadPoolManager`, `SpawnEngine`, `Rnd`, and `NpcSkillSpawn`.
4. Inspect C# `LastNpcSkillPostSpawnPreview`, `CaptureMercenaryNpcSkillPreview`, `PreviewMercenaryNpcSkillPostSpawn`, and tests.
5. Implement one narrow represented delayed-scheduler boundary or static-data loader adapter slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
