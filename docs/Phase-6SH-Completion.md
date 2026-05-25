# Phase 6SH Completion - Staged Nearby Quest Candidate Projection

Date: May 25, 2026
Unit of Work: UOW-990

## Session Summary

This unit continued Phase 6 nearby-quest prerequisites by adding a staged projection from populated `QuestNpcStartTable` rows into `WorldMapInstanceRuntimeState` quest ids.

Java remains the source of truth. This unit does not enable production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, delayed refresh scheduling, `QuestService.checkStartConditions`, player-controller sends, or `CM_ITEM_PURIFICATION` dispatch wiring.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestNpc.java`
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestCandidateProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/WorldMapRuntimeStateTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`

## Completed Work

- Added `NearbyQuestCandidateProjectionService.ProjectNpcStartQuestIds`.
- The helper consumes NPC template ids, reads staged `QuestNpcStartTable` registrations, and contributes matching `OnQuestStart` quest ids to `WorldMapInstanceRuntimeState`.
- Added `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance`.
- Extended the real-data audit with `RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring`.
- Pinned the staged real-data projection baseline:
  - inspected NPC ids: 1668
  - NPC ids with staged quest starts: 1668
  - projected distinct quest ids: 4503
  - newly registered world-instance quest ids: 4503
  - final world-instance quest ids: 4503
- Updated nearby-refresh, AP/quest readiness, automatic-dispatch readiness, real-data audit, and Phase 6 progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~WorldMapRuntimeStateTests` passed with 17 tests.
- An initial parallel `QuestNpcStartRegistrationSourceRealDataAuditTests` run failed with `CS2012` because a concurrent `dotnet test` process held `Aion.GameServer.dll`; this was a build-file lock, not a code failure.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests` passed with 3 tests when rerun by itself.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1686 tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | `Aion.GameServer.Services.NearbyQuestCandidateProjectionService`; `Aion.GameServer.World.WorldMapInstanceRuntimeState` | World / Quest Registry | Partial | Regression Tested | Partial Parity | C# now has a staged projection from NPC template ids through `QuestNpcStartTable` into the world quest-id set. Production NPC object add, delayed 1500 ms refresh scheduling, player iteration, threading, and runtime handler execution remain unported/unverified. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.getOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartRegistration.OnQuestStart`; `NearbyQuestCandidateProjectionService` | Quest NPC Registration / DTO | Partial | Regression Tested | Partial Parity | Projection reads staged `OnQuestStart` sets and duplicate-collapses quest ids into the world instance. Java `HashSet` iteration order is not claimed. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service; `NearbyQuestCandidateProjectionService` pre-filter source | Controller / Quest UI | Partial | Regression Tested | Needs Verification | This unit stages only the source world quest-id set consumed before Java's start-condition filtering. `QuestService.checkStartConditions`, level-diff calculation, `SmNearbyQuests` send, and packet order remain missing. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Still required with `allowedDiffToMinLevel = 2`, `warn = false`, and all skip flags false. Predicate parity is the next blocker before real nearby-refresh candidates can be sent. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance` | Unit | Java `WorldMapInstance.addObject(Npc)` and `QuestNpc.getOnQuestStart` | Validates staged NPC ids project quest-start ids into the world-instance quest-id set, duplicate quest ids collapse, missing NPC ids do not contribute, and newly added quest ids are reported. | Deterministic C# test from source-reviewed Java set contribution behavior. | Does not execute NPC spawn, delayed refresh scheduling, start-condition filtering, or packet sends. |
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring` | Regression | Real repository Java/XML quest-start source data and Java `WorldMapInstance.addObject(Npc)` set contribution behavior | Pins staged projection counts from current repository data: 1668 inspected/matched NPC ids and 4503 projected/new/world quest ids. | Deterministic C# audit over current repository source files, staged table behavior, and world quest-id storage. | Does not run Java/C# runtime NPC spawn, Java handlers, delayed refresh scheduling, start-condition filtering, or packet sends. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The staged source loader/table/projection path is not integrated into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, world instance population, or production dispatch.
- Java `HashSet`/concurrent set iteration order is not claimed for table or world quest-id ordering.
- XML extraction remains partial and does not model `aggro_start_npc_ids`, talk/kill/end/distance/zone registrations, template-specific dialogs, quest item registration, or JAXB schema validation.
- No C# nearby-UI `QuestService.checkStartConditions` equivalent exists.
- `SmNearbyQuests` remains a packet prerequisite only; no production code sends it.
- The current ItemPurification dispatcher seam must remain no-op until production source loading, start-condition evaluation, and a controlled send boundary exist.
- ItemPurification persistent execution still does not persist secondary rank-limit equipment unequips or abyss skill deletion intents from AP-rank side effects.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 4 in this unit
- Total artifacts ported: 1 staged projection helper in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 4
- Total blocked artifacts: 3 blocked/not-started categories, including production NPC-spawn integration, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds an offline world quest-id projection prerequisite without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Audit or stage the Java nearby-UI start-condition predicate path: `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)` and `QuestService.getLevelRequirementDiff(questId, playerLevel)`. Keep packet sends and production ItemPurification dispatch disabled.

Suggested narrow shape:

1. Read the Java `QuestService.checkStartConditions` branches and supporting dataholders used by the nearby-UI overload.
2. Produce a source audit or a test-only predicate skeleton that documents required inputs and known C# gaps.
3. Include `QuestService.getLevelRequirementDiff` because `SmNearbyQuests` uses the positive-diff marker bit.
4. Do not wire player-controller sends or production ItemPurification dispatch.
