# Phase 6SO Completion - Supported Nearby Marker Real-Data Projection

Date: May 25, 2026
Unit of Work: UOW-997

## Session Summary

This unit added a staged real-data audit that projects nearby quest markers only for templates with no currently unsupported nearby dependencies.

Java remains the source of truth. This unit does not send `SM_NEARBY_QUESTS`, wire `CM_LEVEL_READY`, implement NPC-spawn delayed refresh, wire production `StaticData`, or enable production ItemPurification dispatch.

## Completed Work

- Extended `QuestNpcStartRegistrationSourceRealDataAuditTests` with `RealDataAudit_ProjectsSupportedNearbyMarkersWithoutProductionSendWiring`.
- Composed existing staged pieces over real repository data:
  - XML/Java handler quest-start source loader
  - `QuestNpcStartTable`
  - `NearbyQuestCandidateProjectionService`
  - `NearbyQuestTemplateXmlExtractor`
  - `NearbyQuestTemplateTable`
  - `NearbyQuestMarkerProjectionService`
- Filtered projected quest ids to templates without XML start conditions, inventory preconditions, combine-skill requirements, NPC faction requirements, or time-based repeat behavior.
- Pinned the current supported-template baseline:
  - 2072 supported projected quest ids
  - 920 staged markers for a level-65 Elyos male Gladiator
  - 1152 supported early-gate rejections
  - 0 unsupported dependency failures
- Updated nearby-refresh, nearby start-condition, AP/quest readiness, automatic-dispatch readiness, send-boundary, and progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsSupportedNearbyMarkersWithoutProductionSendWiring` passed with 1 test.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1697 tests.

## Files Changed

- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SO-Completion.md`

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestMarkerProjectionService`; `QuestNpcStartRegistrationSourceRealDataAuditTests` | Controller / Quest UI Projection Audit | Partial | Regression Tested | Partial Parity | Pins one supported-template real-data projection slice: 2072 supported projected quest ids, 920 staged markers, and 1152 supported early-gate rejections for a level-65 Elyos male Gladiator. It excludes unsupported XML/inventory/combine-skill/NPC-faction/time-based templates and does not send packets. |
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | `Aion.GameServer.World.WorldMapInstanceRuntimeState`; `Aion.GameServer.Services.NearbyQuestCandidateProjectionService` | World Instance / Quest Registry Dependency | Partial | Regression Tested | Partial Parity | Existing real-data source projection feeds the staged world quest-id set. Production NPC object add, delayed 1500 ms refresh scheduling, task reset, threading, and per-player fanout remain unported. |
| `com.aionemu.gameserver.questEngine.QuestEngine.init`; `com.aionemu.gameserver.model.templates.quest.QuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader`; `QuestNpcStartTable` | Quest Handler Registration Source | Partial | Regression Tested | Partial Parity | Existing real-data loader/table audit remains the source for projected quest ids. Java reflection/JAXB handler execution, handler lifecycle, and production startup integration remain unported. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor`; `NearbyQuestTemplateTable` | Dataholder / Real-Data Projection Input | Partial | Regression Tested | Needs Verification | UOW-997 consumes the staged real-data table to filter out unsupported dependency categories before marker projection. Production JAXB/`StaticData` loading, XML start-condition semantics, and enum mapping remain unverified. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `NearbyQuestMarkerProjectionService` | Service / Quest Predicate Dependency | Partial | Regression Tested | Partial Parity | Audit proves the filtered supported subset avoids unsupported dependency failures. Race, min/max level, class, and gender early-gate rejections are exercised over real data. Full Java predicate behavior is still incomplete. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `NearbyQuestMarker` | Server Packet / Marker DTO Dependency | Complete | Unit Tested; Regression Input Audited | Verified Parity | Existing packet byte tests remain the serialization evidence. UOW-997 produces marker DTO inputs only; it does not serialize or send them in the real-data audit. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsSupportedNearbyMarkersWithoutProductionSendWiring` | Regression | Java `QuestEngine.init`, `QuestNpc.addOnQuestStart`, `WorldMapInstance.addObject`, `QuestService.checkStartConditions`, and real repository quest XML | Pins supported-template staged projection counts: 2072 supported projected quest ids, 920 markers, 1152 supported early-gate rejections, and zero unsupported dependency failures. | Deterministic C# audit over current repository source/XML through staged Java-derived services. | Uses one synthetic player archetype; no Java runtime comparison, production `StaticData`, packet serialization, or packet send. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The supported-template projection audit uses one synthetic player archetype and excludes unsupported dependency categories.
- Production `StaticData`/`DataManager`, player-controller refresh, `CM_LEVEL_READY` send integration, NPC-spawn delayed refresh scheduling, and ItemPurification dispatch remain disabled.
- XML start-condition, inventory item, combine-skill, NPC faction, and time-based repeat semantics are still unsupported beyond filtering/rejection.
- Java `HashMap`/set ordering is not claimed.
- Reflection/dynamic handler execution and production startup integration remain unported.
- No date/time behavior was added; repeat-cycle date/time remains unsupported from prior units.
- Serialization was not changed in this unit.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 6 in this unit
- Total artifacts ported: 0 new runtime artifacts in this unit; 1 real-data regression audit added
- Total artifacts with verified parity: 1 existing packet artifact referenced by this audit
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/not-started categories: production send method, level-ready send trigger, delayed NPC-spawn refresh scheduler, production static-data integration, and unsupported predicate dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds supported-template real-data projection evidence without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Implement a non-sending `NearbyQuestRefreshPlanService` that composes current staged world quest ids, staged templates, and marker projection into a send-ready plan with explicit readiness/failure reasons, or broaden the supported-template projection audit across representative player archetypes.

Keep actual packet sends, `CM_LEVEL_READY` integration, NPC-spawn delayed refresh, production `StaticData` integration, and production ItemPurification dispatch disabled until follow-up tests cover each gate.

## Next Work Options

### Recommended Sequential Task

- Task: Add a non-sending nearby refresh plan composer.
- Why: The supported real-data projection exists; a plan object can make send-readiness and failure reasons explicit before any packet-send integration.
- Files: new service/test files plus docs. Avoid production send paths.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-sending refresh-plan service | new service/test files | Medium | Must not call `SendPacketAsync` or registry sends. |
| B | Broader archetype projection audit | existing real-data audit test/docs | Medium | Same test file as UOW-997; do not parallelize with A if both need docs. |
| C | XMLStartCondition dependency expansion | docs/read-only Java source | Low | Read-only dependency map; no production predicate changes. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement selected refresh-plan or archetype audit and update shared docs | selected service/test files, Phase 6 docs | Production send paths unless this becomes the sole owner |
| Agent A | Read-only XMLStartCondition dependency expansion | Read-only source inspection | All writes |

If no sub-agent tool is available, do the recommended task sequentially.

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestMarkerProjectionService.cs`: one owner at a time if projection behavior changes.
- `QuestNpcStartRegistrationSourceRealDataAuditTests.cs`: one owner at a time for real-data baselines.
- `GameServerConnection.cs`: production send path; one owner only.
- `GameClientSocketServer.cs`: connection registry/send infrastructure; one owner only.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6SO-Completion.md`, `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, `docs/QuestStartConditions-Nearby-Audit.md`, and `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- `docs/commit-conventions.md` is still missing; use the commit format in `docs/orchestration-rules.md`.
- Production `CM_ITEM_PURIFICATION` dispatch and real nearby-refresh packet sends must remain disabled.
