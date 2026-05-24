# Phase 6KT Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6KS and covers Session 794.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "PlayerSummonCastSpellServiceTests|GameServerConnectionCastSpellTests|PlayerSummonSkillExecutionServiceTests"`
  - Result: Passed, 61 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1385 tests.

## Recent Work Completed

### Session 794 - Represented Ordinary NPC Spawn Creation Preview

- Re-inspected Java `VisibleObjectSpawner.spawnNpc`, `Npc`, `WalkerFormator.processClusteredNpc`, `FlagKnownList`, `NpcKnownList`, and `EffectController` usage.
- Added `PlayerSummonKnownObjectNpcSkillNpcCreationPreview`, creation status enum, and known-list-kind enum.
- Added `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillOrdinaryNpcCreation`.
- Modeled:
  - missing spawn dispatch input;
  - non-ordinary-NPC dispatch branches;
  - `DataManager.NPC_DATA.getNpcTemplate(npcId)` missing-template null return;
  - represented `NpcController` and `Npc` construction requirement;
  - Java creator-id copy from `spawn.getCreatorId()`;
  - Java `npc.isFlag() ? FlagKnownList : NpcKnownList` selection;
  - represented `EffectController` setup;
  - represented `WalkerFormator.processClusteredNpc` gate;
  - represented `SpawnEngine.bringIntoWorld` requirement only when walker formator did not already bring the NPC into the world;
  - represented controller delete requirement on `bringIntoWorld` failure.
- Kept live `Npc`, `NpcController`, `NpcSkillList`, `NpcGameStats`, `NpcLifeStats`, `NpcMoveController`, `DataManager.NPC_DATA`, `IDFactory`, live known lists, live `EffectController`, `WalkerFormator`, `SpawnEngine.bringIntoWorld`, world insertion, packets, persistence, threading, serialization, and live-client validation unwired.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.spawnengine.VisibleObjectSpawner.spawnNpc` | `PlayerSummonSkillExecutionService.PreviewMercenaryNpcSkillOrdinaryNpcCreation` | Ordinary NPC Spawn Projection | Partial | Regression Tested | Needs Verification | Represents template lookup, NPC creation prerequisites, creator copy, known-list selection, effect-controller setup, walker gate, bring-into-world requirement, and delete-on-failure metadata. It does not create live NPCs or insert them into world. |
| `com.aionemu.gameserver.dataholders.DataManager.NPC_DATA` | `npcTemplateExists` input and `MissingNpcTemplate` status | Static Data Dependency | Not Started | Regression Tested as explicit branch input | Needs Verification | Models the missing-template null-return branch but does not load or query live NPC templates, compare XML/static-data behavior, or validate template identity. |
| `com.aionemu.gameserver.model.gameobjects.Npc` | `PlayerSummonKnownObjectNpcSkillNpcCreationPreview` | NPC Object Dependency | Partial | Regression Tested as creation metadata only | Needs Verification | Records that Java would construct `Npc(new NpcController(), spawn, npcTemplate)` and copy creator id. Live `Npc`, ID allocation, move controller, skill list, stat containers, AI, threading, and serialization remain missing. |
| `com.aionemu.gameserver.world.knownlist.FlagKnownList` / `NpcKnownList` | `PlayerSummonKnownObjectNpcSkillNpcKnownListKind` | Known List Dependency | Partial | Regression Tested | Needs Verification | Models flag-vs-ordinary known-list selection from represented template type. Live known-list objects, visibility, synchronization, packet fanout, and serialization remain missing. |
| `com.aionemu.gameserver.controllers.effect.EffectController` | `RequiresEffectController` metadata | Effect Controller Dependency | Not Started | Regression Tested as explicit gap | Needs Verification | Records effect-controller setup but does not instantiate live controller, abnormal/effect state, packet updates, threading, or serialization. |
| `com.aionemu.gameserver.spawnengine.WalkerFormator.processClusteredNpc` | `WalkerFormatorBroughtIntoWorld` input and related preview flags | Walker Spawn Dependency | Partial | Regression Tested as explicit branch input | Needs Verification | Models the branch where walker processing avoids direct `bringIntoWorld`, but does not inspect walker ids, walker templates, clustered groups, cache behavior, synchronization, Java RNG, or live movement state. |
| `com.aionemu.gameserver.spawnengine.SpawnEngine.bringIntoWorld` | `RequiresBringIntoWorld` / `RequiresControllerDeleteOnBringIntoWorldFailure` metadata | World Insertion Dependency | Not Started | Regression Tested as explicit gap | Needs Verification | Records direct world insertion and failure cleanup requirements. Live `World.storeObject`, `World.setPosition`, `World.spawn`, region/zone updates, packets, persistence, and client visibility remain missing. |

## Tests Added Or Updated

- `PlayerSummonSkillExecutionServiceTests.PreviewMercenaryNpcSkillOrdinaryNpcCreation_ProjectsJavaVisibleObjectSpawnerSpawnNpc`
  - Validates missing dispatch and non-ordinary dispatch.
  - Validates missing NPC template null-return metadata.
  - Validates ordinary NPC creation metadata.
  - Validates `NpcController`, NPC, creator-copy, known-list, effect-controller, and walker requirements.
  - Validates bring-into-world and delete-on-failure requirements.
  - Validates creator id, NPC id, and instance id propagation.
  - Validates flag known-list selection.
  - Validates walker-formator already-brought-into-world behavior.
- These tests are source-derived from Java. They do not compare against Java runtime execution, live NPC template lookup, ID allocation, controller behavior, skill-list/stat initialization, known-list behavior, effect-controller behavior, walker-formator behavior, world insertion, packets, persistence, XML/static-data loading, reflection behavior, threading behavior, serialization behavior, date/time runtime behavior, precision/rounding behavior, or live-client validation.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 represented ordinary NPC spawn creation preview slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: live `DataManager.NPC_DATA`, XML/static loading, `IDFactory`, live `Npc`, `NpcController`, `NpcSkillList`, stat containers, move controller, AI, live known lists, effect controller, `WalkerFormator`, world insertion, region/zone updates, packets, persistence, Java runtime comparison, threading/serialization, reflection behavior, scheduler callback execution, geometry/precision, and live-client validation.
- Estimated overall migration completion: 66%

## Remaining Risks

- Ordinary NPC creation is represented metadata only; no live `Npc`, controller, known list, effect controller, walker state, or world object is created.
- Live `DataManager.NPC_DATA`, XML/static-data loading, `IDFactory`, `NpcSkillList`, `NpcGameStats`, `NpcLifeStats`, `NpcMoveController`, AI, known-list visibility, effect controller state, `WalkerFormator`, `World.storeObject`, `World.setPosition`, `World.spawn`, region/zone updates, packets, persistence, threading, serialization, Java runtime behavior, scheduler callback execution, geometry, precision/rounding, and live-client behavior remain missing or unverified.

## Next Recommended Unit of Work

Continue by starting a static-data loader adapter for represented `NpcSkillSpawn` XML fields or by modeling `SpawnEngine.bringIntoWorld` world-insertion preview for `World.storeObject`, `World.setPosition`, `World.spawn`, region/zone update, temporary-spawn, and instance handler gaps. Keep live XML loading, Java RNG runtime comparison, Java geometry, live spawn-engine execution, live AI mutation, controller execution, effects, packets, scheduler/date-time behavior, threading, serialization, and live-client validation explicit until supported.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6KS-Completion.md`
   - this handoff
3. Inspect Java `SpawnEngine.bringIntoWorld`, `World.storeObject`, `World.setPosition`, `World.spawn`, instance handler `onSpawn`, temporary-spawn handling, and `VisibleObjectSpawner.spawnNpc`.
4. Inspect C# post-spawn preview, dispatch preview, ordinary NPC creation preview records/methods, and tests.
5. Implement one narrow represented static-data loader adapter or world-insertion preview slice with Java breadcrumbs.
6. Add focused tests that state what is source-derived and what remains unverified.
7. Run focused tests, then full GameServer tests.
8. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
9. Create the next handoff document and commit the unit.
