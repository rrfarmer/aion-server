# Phase 6SP Completion - Non-Sending Nearby Refresh Plan Composer

Date: May 25, 2026
Unit of Work: UOW-998
Commit message: `[Phase 6][UOW-998] Stage nearby refresh plan composer`

## Session Summary

This unit added a non-sending nearby quest refresh plan boundary for the staged nearby-marker work. Java remains the source of truth. The new C# service composes staged world quest ids, staged quest templates, and staged marker projection into an explicit plan with readiness/failure status and rejection counts, but it does not send `SM_NEARBY_QUESTS`.

Production packet sends, `CM_LEVEL_READY` nearby integration, NPC-spawn delayed refresh, production `StaticData`/`DataManager` wiring, and production ItemPurification dispatch remain disabled.

## Completed Work

- Added `NearbyQuestRefreshPlanService`.
- Added `NearbyQuestRefreshPlan`, `NearbyQuestRefreshPlanStatus`, and non-sending readiness helpers.
- Added fail-closed statuses for:
  - missing world instance
  - missing nearby quest template table
  - empty world quest-id set
  - no projected markers
- Added rejection-count and unsupported-dependency reporting around the existing staged nearby predicate.
- Added `NearbyQuestRefreshPlanServiceTests` with four focused unit tests.
- Used a read-only sub-agent to inspect Java `XMLStartCondition` dependencies for the next predicate slice, then closed the sub-agent.
- Updated nearby-refresh, start-condition, AP/quest readiness, automatic-dispatch readiness, send-boundary, and Phase 6 progress docs.

## Validation

- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter FullyQualifiedName~NearbyQuestRefreshPlanServiceTests` passed with 4 tests.
- `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` passed with 1701 tests.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestRefreshPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestRefreshPlanServiceTests.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6SP-Completion.md`

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshPlanService` | Controller / Quest UI Refresh Plan | Partial | Unit Tested | Partial Parity | Adds a non-sending plan boundary that composes staged marker projection and rejection counts. It fails closed for missing instance/template data. It does not resolve live map regions, send `SM_NEARBY_QUESTS`, schedule delayed refresh, or claim Java `HashMap` order parity. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService`; `NearbyQuestRefreshPlanService` | Service / Quest Predicate Dependency | Partial | Unit Tested | Partial Parity | The plan reports rejection counts from the staged predicate. Full XML start conditions, inventory checks, combine-skill, NPC faction, exception/log behavior, and time-based repeat timing remain unsupported. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState`; `NearbyQuestRefreshPlanService` | World / Quest Registry Dependency | Partial | Unit Tested | Partial Parity | Plan consumes an already staged world-instance quest-id set. Production NPC spawn, map-region parent lookup, 1500 ms delayed refresh scheduling, and threading remain unported. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `NearbyQuestRefreshPlanService` | Dataholder / Quest Template Dependency | Partial | Unit Tested | Needs Verification | Plan fails closed when the staged table is missing and delegates per-quest lookup to the staged predicate. Production JAXB/`StaticData` loading and XML start-condition data remain unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | Manual Only | Needs Verification | Read-only analysis clarified optional `finished` rows, mandatory non-finished rows, reward matching, repeatable prerequisite complete count, `acquired` COMPLETE behavior, `required_title` enforcement, and `equipped` warn gating. No C# predicate code added yet. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `NearbyQuestRefreshPlan` | Server Packet / Plan Output Dependency | Complete packet; plan partial | Unit Tested | Partial Parity | Existing packet byte tests remain the serialization evidence. UOW-998 only prepares marker DTOs in a plan; it does not serialize or send the packet. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_FailsClosedWithoutWorldInstanceOrQuestTemplates` | Unit | Java `PlayerController.updateNearbyQuests` prerequisites and C# staged safety gates | Validates missing instance/template table do not produce a send-ready plan. | Deterministic C# fail-closed test around Java-derived refresh prerequisites. | Java does not have this exact null-plan object; this is a staged safety boundary. |
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoWorldQuestIdsWithoutSending` | Unit | Java `WorldMapInstance.getQuestIds` candidate source | Validates empty world quest-id set returns no-send status. | Deterministic C# test over staged world-state boundary. | Does not invoke live map-region lookup. |
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_ComposesMarkersAndRejectionReasonsWithoutSending` | Unit | Java `PlayerController.updateNearbyQuests` filtering through `QuestService.checkStartConditions` | Validates marker projection, rejected quest ids, rejection counts, and unsupported-dependency reporting. | Deterministic C# test over Java-source-derived staged services. | Does not serialize/send `SM_NEARBY_QUESTS`. |
| `NearbyQuestRefreshPlanServiceTests.CreatePlan_ReturnsNoMarkersWhenAllQuestIdsAreRejected` | Unit | Java nearby predicate filtering before packet send | Validates all-rejected plans are not send-ready and report supported early-gate rejection counts. | Deterministic C# test over staged predicate. | No Java runtime comparison. |
| Read-only sub-agent XMLStartCondition analysis | Manual | Java `XMLStartCondition`, `FinishedQuestCond`, `QuestTemplate.getRequiredConditionCount`, `QuestState`, `QuestStatus`, and `QuestStateList` | Documents the next XML predicate slice and edge cases. | Source-reviewed report; sub-agent made no edits and was closed. | No C# implementation yet. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- The refresh plan service is non-sending and has no production caller.
- `CM_LEVEL_READY` nearby marker send is absent in C#.
- NPC-spawn delayed refresh fanout and Java 1500 ms debounce are absent in C#.
- Production `StaticData`/`DataManager`, player-controller refresh, and ItemPurification dispatch remain disabled.
- XML start-condition, inventory item, combine-skill, NPC faction, and time-based repeat semantics are still unsupported.
- Java `HashMap`/set ordering is not claimed.
- Reflection/dynamic handler execution and production startup integration remain unported.
- Serialization was not changed in this unit.
- Required `docs/commit-conventions.md` is still missing; commit format continues to follow `docs/orchestration-rules.md`.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 1 non-sending refresh-plan boundary in this unit
- Total artifacts with verified parity: 1 existing packet artifact referenced by this unit
- Total artifacts needing verification: 6
- Total blocked artifacts: 5 blocked/not-started categories: production send method, level-ready send trigger, delayed NPC-spawn refresh scheduler, production static-data integration, and unsupported predicate dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit adds a non-sending plan boundary without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add a narrow XML start-condition staged data model and predicate slice for `finished`, `unfinished`, `noacquired`, `acquired`, and `required_title`, preserving Java nearby behavior that `equipped` passes when `warn = false`.

Keep inventory items, combine skill, time-based repeat cooldowns, NPC faction, packet sends, production integration, and production ItemPurification dispatch disabled until each dependency has tests.

## Next Work Options

### Recommended Sequential Task

- Task: Add staged XML start-condition data and predicate support for the source-audited nearby subset.
- Why: The refresh plan now reports unsupported XML dependencies, and Java source review identified a narrow subset that can be implemented safely before sends.
- Files: `NearbyQuestTemplateSummary`, `NearbyQuestTemplateXmlExtractor`, `NearbyQuestStartConditionService`, focused tests, and docs.
- Guardrail: Do not treat XML templates as fully supported until `finished`, `unfinished`, `noacquired`, `acquired`, `required_title`, optional-block counting, reward matching, and repeatable prerequisite behavior have tests.

### Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | XML start-condition staged DTO/extractor design notes | docs/read-only Java + proposed C# shape | Low | Good sub-agent read-only task before code. |
| B | Broader refresh-plan archetype audit | new or existing audit tests/docs | Medium | Avoid changing central predicate in parallel with XML implementation. |
| C | ItemPurification side-effect persistence follow-up | sidecar audit/docs or isolated persistence tests | Medium | Keep separate from nearby quest files. |

### Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Implement selected XML predicate slice and update shared docs | selected service/dataholder/test files, Phase 6 docs | Production send paths unless this becomes the sole owner |
| Agent A | Read-only XML start-condition DTO/extractor checklist | Java source and docs only | All writes |

If no sub-agent tool is available, do the recommended task sequentially.

### Do Not Parallelize

- `NearbyQuestStartConditionService.cs`: central staged predicate; one owner at a time.
- `NearbyQuestTemplateXmlExtractor.cs`: central staged extractor; one owner at a time.
- `NearbyQuestTemplateSummary.cs`: central staged template shape; one owner at a time.
- `NearbyQuestRefreshPlanService.cs`: one owner at a time if plan behavior changes.
- `QuestNpcStartRegistrationSourceRealDataAuditTests.cs`: one owner at a time for real-data baselines.
- `GameServerConnection.cs`: production send path; one owner only.
- `GameClientSocketServer.cs`: connection registry/send infrastructure; one owner only.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By Next Session

- Read `docs/csharp-port.md`, `docs/PHASE-6-PROGRESS.md`, `docs/Phase-6SP-Completion.md`, `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, `docs/QuestStartConditions-Nearby-Audit.md`, and `docs/NearbyQuestRefresh-SendBoundary-Audit.md`.
- `docs/commit-conventions.md` is still missing; use the commit format in `docs/orchestration-rules.md`.
- Production `CM_ITEM_PURIFICATION` dispatch and real nearby-refresh packet sends must remain disabled.
- The next strongest nearby quest move is staged XML start-condition support, not live sends.
