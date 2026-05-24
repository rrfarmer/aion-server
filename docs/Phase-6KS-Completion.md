# Phase 6KS Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KR and covers Session 793.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 60 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1384 tests.

## Recent Work Completed

### Session 793 - Represented SpawnObject Dispatch Preview

- Re-inspected Java `SpawnEngine.spawnObject`, `SpawnEngine.getSpawnedObject`, and `VisibleObjectSpawner` dispatch methods.
- Added `PlayerSummonKnownObjectNpcSkillSpawnTemplateKind`, `PlayerSummonKnownObjectNpcSkillSpawnObjectDispatchPreview`, dispatch status enum, and branch enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillSpawnObjectDispatch`.
- Modeled:
  - missing spawn-template input;
  - non-ready spawn-template previews;
  - Java gatherable NPC id branch `npcId > 400000 && npcId < 499999`;
  - Java branch precedence where gatherable id range wins before rift/siege/vortex template subtype checks;
  - represented rift, siege, vortex, and ordinary NPC dispatch branches;
  - future `VisibleObjectSpawner` call requirement;
  - future NPC template lookup/config gate/null-return risks for NPC-like branches;
  - future temporary-spawn registration and instance handler `onSpawn` checks after a non-null spawned object.
- Kept live `VisibleObjectSpawner`, `DataManager.NPC_DATA`, `CustomConfig.RIFT_ENABLED`, `SiegeConfig.SIEGE_ENABLED`, `CustomConfig.VORTEX_ENABLED`, `RiftService`, `WalkerFormator`, `TemporarySpawnEngine`, `World`, instance handler callbacks, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.spawnengine.SpawnEngine.spawnObject` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillSpawnObjectDispatch` | Spawn Dispatch Projection | Partial | Regression Tested | Needs Verification | Represents missing/not-ready/dispatch outcomes and records follow-up temporary-spawn and instance-handler checks. It does not call `getSpawnedObject`, register temporary spawns, invoke `onSpawn`, or return a live object. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.getSpawnedObject` | `PlayerSummonKnownObjectNpcSkillSpawnObjectDispatchPreview` | Dispatch DTO | Partial | Regression Tested | Needs Verification | Mirrors branch selection for gatherable ids, rift, siege, vortex, and ordinary NPC templates. It does not use Java `instanceof`; template kind is represented metadata. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnGatherable` | `RequiresGatherableSpawner` / `Gatherable` dispatch branch | Spawn Dependency | Not Started | Regression Tested as branch metadata only | Needs Verification | Gatherable id range dispatch is represented and takes precedence over subtype metadata. Live `Gatherable`, controller, world insertion, packets, and persistence remain missing. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnRiftNpc` | `RiftNpc` dispatch branch / `RequiresRiftEnabledCheck` | Spawn Dependency | Not Started | Regression Tested as branch metadata only | Needs Verification | Records rift config-gate/template-lookup/null-return risk but does not check `CustomConfig.RIFT_ENABLED`, `RiftService`, or live rift location state. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnSiegeNpc` | `SiegeNpc` dispatch branch / `RequiresSiegeEnabledCheck` | Spawn Dependency | Not Started | Regression Tested as branch metadata only | Needs Verification | Records siege config-gate/template-lookup/null-return risk but does not check `SiegeConfig.SIEGE_ENABLED` or create `SiegeNpc`. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnInvasionNpc` | `InvasionNpc` dispatch branch / `RequiresVortexEnabledCheck` | Spawn Dependency | Not Started | Regression Tested as branch metadata only | Needs Verification | Records vortex config-gate/template-lookup/null-return risk but does not check `CustomConfig.VORTEX_ENABLED` or create invasion NPCs. |
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnNpc` | `Npc` dispatch branch / `RequiresNpcTemplateLookup` | Spawn Dependency | Not Started | Regression Tested as branch metadata only | Needs Verification | Records ordinary NPC dispatch and template lookup risk. Live `Npc`, controller, known-list selection, effect controller, `WalkerFormator`, world insertion, packets, and persistence remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillSpawnObjectDispatch_ProjectsJavaSpawnObjectBranches`
  - Validates missing-template and not-ready outcomes.
  - Validates gatherable id branch and precedence over represented rift subtype.
  - Validates rift, siege, invasion/vortex, and ordinary NPC branches.
  - Validates instance id and NPC id propagation.
  - Validates future `VisibleObjectSpawner` call requirements.
  - Validates NPC template lookup risk, config-gate flags, temporary-spawn registration check, and instance `onSpawn` check metadata.
- These tests are source-derived from Java. They do not compare against Java runtime execution, Java `instanceof` behavior, live `DataManager.NPC_DATA` lookup, config gates, rift service behavior, walker formator behavior, temporary-spawn registration, world insertion, instance handler callbacks, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented NPC skill spawn-object dispatch preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `VisibleObjectSpawner`, Java `instanceof` subtype behavior, `DataManager.NPC_DATA`, rift/siege/vortex config gates, `RiftService`, `WalkerFormator`, controllers, known lists, effect controllers, `TemporarySpawnEngine`, world insertion, instance handler `onSpawn`, packets, persistence, Java runtime comparison, XML/static loading, threading/serialization, reflection behavior, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Spawn-object dispatch is represented metadata only; no live object is created.
- Java `instanceof` checks are represented by explicit C# template-kind metadata, so live subtype/reflection behavior remains unverified.
- `VisibleObjectSpawner`, `DataManager.NPC_DATA`, config gates, `RiftService`, `WalkerFormator`, controllers, known lists, effect controllers, `TemporarySpawnEngine`, world insertion, instance handler `onSpawn`, packets, persistence, threading, serialization, XML/static-data loading, Java runtime behavior, scheduler callback execution, geometry, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling the ordinary `VisibleObjectSpawner.spawnNpc` creation preview for `DataManager.NPC_DATA`, creator id, known-list kind, effect-controller, `WalkerFormator`, and `bringIntoWorld` gaps. Keep live XML loading, Java RNG runtime comparison, Java geometry, live spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KR-Completion.md`
   - this handoff
3. Inspect Java `VisibleObjectSpawner.spawnNpc`, `SpawnEngine.bringIntoWorld`, `WalkerFormator.processClusteredNpc`, `Npc`, `NpcTemplate`, known-list classes, and `EffectController`.
4. Inspect C# post-spawn preview, location preview, spawn-template preview, dispatch preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or ordinary NPC spawn creation preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
