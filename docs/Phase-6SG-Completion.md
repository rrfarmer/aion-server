# Phase 6SG Completion - Staged Quest Start Table Population

Date: May 25, 2026
Unit of Work: UOW-989

## Session Summary

This unit continued Phase 6 nearby-quest prerequisites by proving that the audited real-data quest-start source rows can populate the staged C# `QuestNpcStartTable`.

Java remains the source of truth. This unit does not enable production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world-instance population, `QuestService.checkStartConditions`, player-controller sends, or `CM_ITEM_PURIFICATION` dispatch wiring.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestNpc.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/data/static_data/quest_script_data/**/*.xml`
- `game-server/data/handlers/quest/**/*.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartRegistrationSourceLoader.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`

## Completed Work

- Added `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_PopulatesStagedQuestNpcStartTableWithoutProductionWiring`.
- Reused the audited real-data loader output from `QuestNpcStartRegistrationSourceLoader`.
- Fed all resolved source rows into `QuestNpcStartTable.RegisterOnQuestStart`.
- Pinned the staged table-population baseline:
  - source rows recorded by `QuestNpcStartTable.Sources`: 5214
  - registered NPC ids: 1668
  - registered NPC/quest start pairs: 5214
  - largest quest-start set on one NPC: 50
- Updated the nearby-refresh, AP/quest readiness, automatic-dispatch readiness, real-data audit, and Phase 6 progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests` passed with 2 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1684 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable.RegisterOnQuestStart`; `Aion.GameServer.Tests.QuestNpcStartRegistrationSourceRealDataAuditTests` | Quest NPC Registration Table | Partial | Regression Tested | Partial Parity | Real-data staged table population stores 5214 NPC/quest start pairs across 1668 NPC ids. Production `QuestEngine`, NPC spawn, world-instance population, threading, and Java `HashSet` iteration order remain unverified. |
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader`; `Aion.GameServer.Dataholders.QuestNpcStartTable` | Offline Loader / Source Aggregator | Partial | Regression Tested | Needs Verification | Loader output can populate the staged table with current real-data counts. This still does not run Java reflection/JAXB, instantiate handlers, register into runtime `QuestEngine`, model reload/unload behavior, or integrate with production `DataManager`. |
| `com.aionemu.gameserver.world.WorldMapInstance` | Future staged candidate-population adapter consuming `QuestNpcStartTable` | World / Quest Registry | Partial | Unit Tested | Needs Verification | World-instance quest-id storage exists from UOW-982, but this unit does not feed table rows into `WorldMapInstanceRuntimeState` or schedule Java's delayed nearby refresh. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future nearby-quest candidate projection | Controller / Quest UI | Not Started | No Tests | Unknown | Candidate filtering remains missing: no map-region lookup, `QuestService.checkStartConditions`, level-diff calculation, or `SmNearbyQuests` send path. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_PopulatesStagedQuestNpcStartTableWithoutProductionWiring` | Regression | Java `QuestNpc.addOnQuestStart` set semantics plus real repository source data | Pins staged table counts after feeding audited source rows: 5214 source rows, 1668 registered NPC ids, 5214 registered NPC/quest pairs, and largest per-NPC quest count 50. | Deterministic C# audit over current repository source files and staged table behavior. | Does not populate Java/C# runtime world instances, execute handlers, or run start-condition filtering. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged source loader/table audit is not integrated into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- Java `HashSet` iteration order is not claimed for expanded set values or table source ordering.
- XML extraction remains partial and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until extraction, candidate calculation, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 0 new production artifacts in this unit; 1 real-data table-population regression added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 3 blocked/not-started categories, including production loader/world-instance integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit strengthens the offline candidate-source baseline without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a staged nearby-quest candidate projection from a populated `QuestNpcStartTable` and a world-instance quest-id set, preserving Java's duplicate-collapsing set semantics, but keep `QuestService.checkStartConditions`, player-controller sends, and ItemPurification dispatch disabled.

Suggested narrow shape:

1. Read Java `WorldMapInstance.addObject(Npc)` and `PlayerController.updateNearbyQuests()` again before coding.
2. Add a pure staged projection helper or test fixture that copies `QuestNpcStartTable` quest ids into `WorldMapInstanceRuntimeState` for selected NPC ids.
3. Assert duplicate-collapsing world quest-id behavior against real-data table rows.
4. Do not evaluate start conditions, send `SmNearbyQuests`, run Java handlers, or wire production startup.
