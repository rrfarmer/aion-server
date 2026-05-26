# Phase 6XD Completion - UOW-1116 Quest Finish Reward Projection Lookup Materialization

Date: May 26, 2026

## Unit Of Work

UOW-1116: `[Phase 6][UOW-1116] Materialize quest finish reward projection lookup`

## Summary

UOW-1116 adds a non-live XML materialization helper for the quest-finish reward projection lookup table.

`QuestFinishRewardProjectionLookupTableXmlFactory` builds a `QuestFinishRewardProjectionLookupTable` from quest XML by pairing `NearbyQuestTemplateSummary` records with all regular reward-group projections for each quest. Quests without regular `<rewards>` still receive a group `0` projection so extended-reward and bonus-only metadata remains addressable.

This still does not expose the table through production `StaticData`, does not parse XML in `GameServerConnection`, and does not enable live quest-finish rewards.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Services/QuestFinishRewardProjectionLookupPlanService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestFinishRewardProjectionLookupTableXmlFactoryTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/Phase-6XD-Completion.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "QuestFinishRewardProjectionLookupTableXmlFactoryTests\|QuestFinishRewardProjectionLookupPlanServiceTests\|QuestFinishRewardTemplateXmlProjectionExtractorTests" --nologo` | Passed: 14 tests. |
| `dotnet test dotnetConversion/AionServer.slnx --nologo` | Passed: 2,204 tests. |
| `git diff --check` | Passed. |

## Migration Parity Table - UOW-1116

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.dataholders.QuestsData` | `QuestFinishRewardProjectionLookupTableXmlFactory`; `QuestFinishRewardProjectionLookupTable` | Static Data Repository / Materializer | Partial | Unit Tested | Needs Verification | C# can now materialize a non-live lookup table from XML and index entries by quest id. It is not wired into production `StaticData` and is not runtime-compared with Java JAXB objects. |
| `com.aionemu.gameserver.model.templates.QuestTemplate` | `NearbyQuestTemplateSummary`; `QuestFinishRewardProjectionLookupEntry` | Static Template DTO | Partial | Unit Tested | Needs Verification | Materializer pairs the summary and reward projections per quest. C# still splits Java's full template and omits unsupported fields. |
| `com.aionemu.gameserver.model.templates.quest.Rewards` | `QuestFinishRewardGroupProjection`; `QuestFinishRewardTemplateProjection` | Static Reward DTO | Partial | Unit Tested | Needs Verification | Materializer builds every regular reward group projection per quest and keeps group `0` for no-regular-reward quests. Java reward selection remains source-reviewed, not runtime-compared. |
| `com.aionemu.gameserver.services.QuestService.finishQuest` | `QuestFinishRewardProjectionLookupPlanService`; future operation planner caller | Finish Service / Projection Source | Partial | Existing Unit Coverage | Needs Verification | Projection lookup can now be populated from XML for tests. Finish planning still is not production-wired and does not mutate state or rewards. |
| `com.aionemu.gameserver.services.QuestService.getRewardItems` | `QuestFinishRewardTemplateXmlProjectionExtractor`; lookup materialized reward groups | Reward Service / Projection Source | Partial | Unit Tested | Needs Verification | Real-data smoke test validates aggregate projection counts, including multi-group quest `1007`. It does not validate selected reward behavior through Java runtime. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_DIALOG_SELECT` | future caller of `QuestFinishRewardProjectionLookupPlanService` | Packet / Production Caller | Partial | No Tests | Needs Verification | Lookup table can be built in tests, but production dialog select remains unwired by design. |
| `com.aionemu.gameserver.model.templates.quest.QuestItems` | `QuestFinishRewardItem`; `QuestFinishRewardItemProjectionDescriptor` | Reward Item DTO | Partial | Existing Unit Coverage | Needs Verification | Materialized group projections preserve fixed/selectable item metadata for downstream planner tests. Live `ItemService.addItem` remains disabled. |
| `com.aionemu.gameserver.model.templates.quest.QuestBonuses` | `QuestFinishRewardBonusTemplateProjection` | Bonus DTO / Reward Dependency | Partial | Existing Unit Coverage | Needs Verification | Bonus-only quests remain addressable through default group `0` projections. Dynamic handler dispatch, Java RNG/Chance selection, and live selected bonus mutation remain disabled. |

## Tests Added Or Updated

| Test Name | What It Validates | Java Comparison |
|---|---|---|
| `QuestFinishRewardProjectionLookupTableXmlFactoryTests.Create_MaterializesEveryRegularRewardGroupForLookup` | Synthetic XML materializes every regular reward group and still creates a group `0` projection for extended-only quests. | Source-reviewed Java `QuestTemplate#getRewards` and `QuestService.finishQuest`; no Java runtime comparison. |
| `QuestFinishRewardProjectionLookupTableXmlFactoryTests.RealDataAudit_MaterializesLookupTableWithoutProductionStaticDataExposure` | Real `quest_data.xml` materializes 8,043 quest entries and 8,464 reward-group projections; quest `1007` has six reward groups. | Deterministic XML aggregate audit; not compared against Java JAXB runtime. |

## Remaining Risks

- Production `StaticData` still does not expose `QuestFinishRewardProjectionLookupTable`.
- Production `GameServerConnection.HandleDialogSelectAsync` still does not call lookup or finish planning.
- The factory reparses XML through summary/projection extractors; production loading shape and memory ownership are undecided.
- Java JAXB defaults, enum conversions, unsupported `QuestTemplate` fields, and script handler side effects are not runtime-compared.
- Live item/non-item reward mutation, dynamic bonus handler dispatch, Java RNG/Chance selection, packet ordering, persistence, rollback, threading/player-ordering, serialization, and date/time completion behavior remain unverified.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 partial non-live XML lookup-table materializer
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: 8 blocked/partial categories: production static-data exposure, socket quest-finish routing, Java JAXB/runtime comparison, unsupported template fields, live reward mutation, dynamic bonus handler execution, persistence/rollback, and packet-order validation
- Estimated overall migration completion: Phase 6 remains about 71% complete.

## Next Recommended Unit Of Work

Add a disabled `StaticData` exposure plan for `QuestFinishRewardProjectionLookupTable`: document or implement the narrow constructor/property plumbing behind tests without using it in `GameServerConnection`, and keep socket quest-finish routing disabled until lookup ownership, memory impact, and real-data validation are settled.

## Suggested Safe Parallel Candidates

| Candidate | Task | Files | Risk | Notes |
|---|---|---|---|---|
| A | Disabled `StaticData` lookup exposure plan/plumbing | `StaticData.cs` plus tests, or audit doc first | Medium/High | Recommended only if kept unused by sockets. |
| B | Disabled selected-bonus envelope audit | Existing bonus selection services/tests or separate audit doc | Medium | Keep Java RNG/live selection disabled. |
| C | Dynamic handler registry source audit | Read-only Java/C# audit doc | Medium | Needed before live handler execution. |

## Suggested Parallel Batch

| Agent | Task | Allowed Files | Forbidden Files |
|---|---|---|---|
| Orchestrator | Plan or stage disabled static-data exposure | `StaticData.cs`, focused static-data tests, progress/handoff docs | `GameServerConnection.cs` |
| Explorer A | Selected-bonus envelope audit | Separate audit doc/read-only notes | Static-data plumbing files and progress/handoff docs |

## Do Not Parallelize

- `GameServerConnection.cs`: production socket path remains disabled.
- `QuestFinishRewardProjectionLookupPlanService.cs`: just changed.
- `StaticData.cs`: use exclusive ownership if selected.
- Phase 6 progress/handoff docs: orchestrator-owned.

## Context Needed By The Next Session

- UOW-1116 adds test-only XML materialization for the lookup table.
- Real-data audit expects 8,043 quest entries and 8,464 reward-group projections.
- Quest `1007` is the current real-data multi-group sentinel with six reward groups.
- The full solution passed 2,204 tests.
- Next safest unit is disabled static-data exposure planning/plumbing, still not socket wiring.
