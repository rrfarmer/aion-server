# Phase 6XB Completion - UOW-1114 Quest Finish Reward Projection Source Audit

Date: May 26, 2026

## Unit Of Work

UOW-1114: `[Phase 6][UOW-1114] Audit quest finish reward projection source`

## Summary

UOW-1114 adds a production reward-projection source audit for future quest-finish wiring.

The audit traces Java from `CM_DIALOG_SELECT.runImpl` through `DataManager.QUEST_DATA.getQuestById(...)`, `QuestService.finishQuest`, and `QuestService.getRewardItems`, then compares that to the current C# XML projection extractors, non-live operation planner, and missing runtime static-data lookup. No production socket path or live reward mutation was enabled.

## Files Changed

- `docs/QuestFinishRewardProjectionSource-Audit.md`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XB-Completion.md`

## Validation

| Command | Result |
|---|---|
| `git diff --check` | Passed. |

No .NET tests were rerun for this docs-only audit. UOW-1113 passed focused quest bonus tests and the full solution with 2,198 tests.

## Migration Parity Table - UOW-1114

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleDialogSelectAsync`; `Aion.GameServer.Network.Aion.ClientPackets.CmDialogSelect` | Packet / Production Caller | Partial | Manual Only | Needs Verification | Java auto-reward dialog actions call `QuestService.finishQuest` for reportable quests. C# production dialog select does not route quest finish actions, does not source quest templates, and does not pass reward projection. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `Aion.GameServer.Services.QuestFinishOperationPlanService` | Service / Finish Planner | Partial | Existing Unit Coverage | Needs Verification | C# planner consumes an optional projection and composes non-live descriptors. It does not lookup templates, mutate rewards, send packets, persist state, or run callbacks live. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `Aion.GameServer.Services.QuestFinishRewardPlanService`; `QuestFinishRewardTemplateProjection` | Reward Service / Projection Planner | Partial | Existing Unit Coverage | Needs Verification | Projection covers regular, extended, class-selectable, and bonus metadata in tests. Production input sourcing for reward group, extended index, player class, and live bonus handler mutation remains missing. |
| `com.aionemu.gameserver.dataholders.QuestsData` | `NearbyQuestTemplateTable`; future `QuestFinishRewardProjectionTable` or lookup service | Static Data Repository | Partial | Manual Only | Needs Verification | Java indexes full `QuestTemplate` by quest id. C# has separate summary/projection extractors but no runtime static-data table exposing full finish reward projections. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardTemplateProjection` | Static Template DTO | Partial | Existing Unit Coverage | Needs Verification | C# splits Java template concerns across summary and reward projection records. XML extraction covers selected fields only; unsupported Java template fields and JAXB defaults remain risks. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardNonItemTemplateProjection` | Static Reward DTO | Partial | Existing Unit Coverage | Needs Verification | Regular/extended non-item and item metadata are projected in tests. Runtime lookup by corrected reward group is not implemented. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItem`; `QuestFinishRewardItemProjectionDescriptor` | Reward Item DTO | Partial | Existing Unit Coverage | Needs Verification | Item id/count metadata is projected without live `ItemService.addItem`; Java count defaults and selectable indexing are source-reviewed but not runtime-compared. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection`; `QuestBonusRewardPlanningReport` | Bonus DTO / Reward Dependency | Partial | Existing Unit Coverage | Needs Verification | Bonus metadata can be projected and carried, but production handler dispatch, Java RNG/Chance selection, and selected bonus item mutation remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| _None_ | Documentation-only audit of the production reward projection source contract. | Source-reviewed Java `CM_DIALOG_SELECT`, `QuestsData`, `QuestTemplate`, `QuestService.finishQuest`, and `QuestService.getRewardItems`; no Java runtime comparison. |

## Remaining Risks

- No production `questId -> QuestFinishRewardTemplateProjection` lookup exists.
- No production `questId -> NearbyQuestTemplateSummary` table is exposed through `StaticData`.
- Multi-reward-group projection requires an explicit reward-group-aware lookup; default group `0` extraction is not enough for Java parity.
- C# has not runtime-compared XML projection output against Java JAXB `QuestTemplate` objects.
- Production player-class normalization, target NPC l10n lookup, rate application, bonus handler side effects, live inventory mutation, packet ordering, persistence, rollback, threading, serialization, and date/time completion behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 0; 1 read-only production reward-projection source audit added
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production quest-template table, production reward-projection table, reward-group-aware lookup, socket quest-finish routing, player-class normalization, dynamic bonus handler execution, live reward mutation/persistence, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a non-live `QuestFinishRewardProjectionLookupPlanService` or equivalent lookup table contract that accepts quest id, corrected reward group, dialog action id, extended reward index, player class, complete count, and target NPC context, then returns a prepared `QuestFinishRewardTemplateProjection` plus diagnostics. Keep XML parsing and live socket wiring outside `GameServerConnection` until the lookup is covered with unit tests and a real-data smoke test.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Non-live reward projection lookup contract | New service/tests plus existing projection extractor tests if needed | Medium | Recommended next unit; do not wire `GameServerConnection`. |
| B | Disabled selected-bonus envelope audit | Existing bonus selection services/tests | Medium | Keep Java RNG/live selection disabled. |
| C | Dynamic handler registry source audit | Read-only Java/C# audit doc | Medium | Needed before live handler execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Add lookup contract or plan docs | New lookup service/tests, progress/handoff docs | `GameServerConnection.cs` unless explicitly selected |
| Explorer A | Audit selected-bonus envelope | Separate audit doc or read-only notes | Lookup service files and progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.cs`: production socket path remains disabled.
- `QuestFinishOperationPlanService.cs`: recently changed shared planner.
- `QuestFinishRewardPlanService.cs`: shared projection planner.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1114 is documentation only.
- The current C# projection extractor is test-covered but not exposed through production `StaticData`.
- Java uses `DataManager.QUEST_DATA.getQuestById(...)` at both packet and finish service boundaries.
- Next safest implementation unit is a non-live lookup contract/table with diagnostics, not socket wiring.
