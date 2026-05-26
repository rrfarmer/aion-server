# Phase 6XC Completion - UOW-1115 Quest Finish Reward Projection Lookup Contract

Date: May 26, 2026

## Unit Of Work

UOW-1115: `[Phase 6][UOW-1115] Add quest finish reward projection lookup contract`

## Summary

UOW-1115 adds a non-live lookup contract for future quest-finish reward projection sourcing.

The new `QuestFinishRewardProjectionLookupPlanService` accepts packet/quest context plus an already-built lookup table, then returns a prepared `QuestFinishRewardTemplateProjection` with dialog action id, extended reward index, player class, target NPC context, and reward-repeat metadata attached. It also reports missing quest templates, missing reward-group projections, missing player class for class-selectable rewards, and missing target NPC template for XP reward l10n context.

This remains deliberately non-live: it does not parse XML inside the socket path, does not update `StaticData`, does not wire `GameServerConnection`, and does not mutate rewards.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardProjectionLookupPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardProjectionLookupPlanServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XC-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardProjectionLookupPlanServiceTests\|QuestFinishRewardTemplateXmlProjectionExtractorTests\|QuestFinishRewardPlanServiceTests" --nologo` | Passed: 29 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,202 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1115

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Services.QuestFinishRewardProjectionLookupTable` | Static Data Repository / Lookup Contract | Partial | Unit Tested | Needs Verification | C# now has a non-live table shape keyed by quest id, mirroring Java's `getQuestById` lookup boundary. It is not populated by production `StaticData` and is not runtime-compared against JAXB. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardProjectionLookupEntry`; `QuestFinishRewardTemplateProjection` | Static Template DTO | Partial | Unit Tested | Needs Verification | Lookup entries bind a summary plus reward-group projections. C# still splits Java's full template into selected records and omits unsupported template fields. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishRewardProjectionLookupPlanService`; `QuestFinishOperationPlanService` | Finish Service / Projection Source Planner | Partial | Unit Tested | Needs Verification | Lookup can prepare a projection with packet/player context before operation planning. It does not call finish planning, mutate quest state, send packets, persist, or run callbacks. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardProjectionLookupInput`; `QuestFinishRewardPlanService` | Reward Service / Projection Input Contract | Partial | Unit Tested | Needs Verification | Input carries dialog action id, extended reward index, corrected reward group, complete count, player class, and target NPC context. Java reward materialization, selected item creation, and live bonus handler mutation remain disabled. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `CmDialogSelect`; future caller of `QuestFinishRewardProjectionLookupPlanService` | Packet / Production Caller | Partial | No Tests | Needs Verification | Packet fields now have a pure lookup input contract, but production `GameServerConnection.HandleDialogSelectAsync` remains unwired by design. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; reward-group projection dictionary | Static Reward DTO | Partial | Unit Tested | Needs Verification | Tests cover selecting a corrected reward group projection. Runtime corrected-reward-group ordering and multi-group static data materialization remain production gaps. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItem`; `QuestFinishRewardItemProjectionDescriptor` | Reward Item DTO | Partial | Existing Unit Coverage | Needs Verification | Lookup preserves the chosen group projection for downstream item projection tests. No live `ItemService.addItem` or Java runtime comparison. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection`; lookup-prepared `QuestFinishRewardTemplateProjection` | Bonus DTO / Reward Dependency | Partial | Existing Unit Coverage | Needs Verification | Bonus metadata can move through the prepared projection if present. Dynamic handler dispatch, Java RNG/Chance selection, and selected bonus mutation remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardProjectionLookupPlanServiceTests.CreatePlan_PreparesCorrectedRewardGroupProjectionWithPacketContext` | Lookup selects the corrected reward group and attaches dialog/player/target context to the projection. | Source-reviewed Java `QuestsData.getQuestById`, `QuestService.validateAndFixRewardGroup`, and `QuestService.getRewardItems`; no Java runtime comparison. |
| `QuestFinishRewardProjectionLookupPlanServiceTests.CreatePlan_ReturnsMissingQuestTemplateWhenJavaQuestLookupWouldFail` | Missing quest id returns a non-live missing-template status. | Mirrors Java's `DataManager.QUEST_DATA.getQuestById` dependency; no production socket comparison. |
| `QuestFinishRewardProjectionLookupPlanServiceTests.CreatePlan_ReturnsMissingRewardGroupProjectionForUnmaterializedRewardGroup` | Missing materialized reward group projection is reported instead of assuming parity. | Source-reviewed Java reward-group indexing; no runtime comparison. |
| `QuestFinishRewardProjectionLookupPlanServiceTests.CreatePlan_ReportsMissingPlayerClassAndTargetNpcTemplateDiagnostics` | Lookup emits diagnostics for class-selectable rewards without player class and XP rewards without target NPC template context. | Source-reviewed Java player-class and target-NPC dependencies; no live reward execution. |

## Remaining Risks

- Production `StaticData` still does not populate `QuestFinishRewardProjectionLookupTable`.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not route quest auto-reward actions into lookup or finish planning.
- Lookup depends on prebuilt reward-group projections; a real static-data materializer still needs to build all relevant groups from `quest_data.xml`.
- Java JAXB defaults and unsupported `QuestTemplate` fields are not runtime-compared.
- Live item/non-item reward mutation, dynamic bonus handler dispatch, Java RNG/Chance selection, packet ordering, persistence, rollback, threading/player-ordering, serialization, and date/time completion behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live reward projection lookup contract
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production static-data population, socket quest-finish routing, all reward-group materialization, Java JAXB/runtime comparison, live reward mutation, dynamic bonus handler execution, persistence/rollback, and packet-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a non-live static-data materialization helper/test that builds `QuestFinishRewardProjectionLookupTable` from real `quest_data.xml`, including every regular reward group for each quest, and validates aggregate counts without exposing it through production `StaticData` yet.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Real-data lookup table materialization helper/tests | New helper/tests around projection extractor and lookup table | Medium | Recommended next unit; keep production `StaticData` unwired. |
| B | Disabled selected-bonus envelope audit | Existing bonus selection services/tests or separate audit doc | Medium | Keep Java RNG/live selection disabled. |
| C | Dynamic handler registry source audit | Read-only Java/C# audit doc | Medium | Needed before live handler execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Real-data lookup materialization | New helper/tests plus progress/handoff docs | `GameServerConnection.cs` and production `StaticData` unless explicitly selected |
| Explorer A | Selected-bonus envelope audit | Separate audit doc/read-only notes | Lookup materialization files and progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.cs`: production socket path remains disabled.
- `StaticData.cs`: do not expose the lookup table until materialization has focused tests.
- `QuestFinishRewardProjectionLookupPlanService.cs`: just changed.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1115 adds a pure lookup planner/table, not production static-data loading.
- Focused tests passed 29 reward/projection tests; the full solution passed 2,202 tests.
- Next safest unit is a real-data materialization helper/test for every quest/reward group, still not wired into production `StaticData` or sockets.
