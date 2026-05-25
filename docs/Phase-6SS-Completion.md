# Phase 6SS Completion - UOW-1001 Nearby Inventory Preconditions

Date: May 25, 2026

## Scope

Continued Phase 6 nearby quest refresh parity by staging Java inventory item start-condition support for the pure nearby predicate surface.

This unit intentionally did not enable live `SM_NEARBY_QUESTS` sends, `CM_LEVEL_READY` dispatch, delayed NPC-spawn refresh, production `StaticData` loading, or production `CM_ITEM_PURIFICATION` quest refresh dispatch.

## Parallel Work Discovery

| Candidate | Workstream | Java Artifacts | C# Target Files | Task Type | Can Parallelize? | Risk | Reason |
|---|---|---|---|---|---|---|
| A | Inventory preconditions | `QuestService.inventoryItemCheck`, `InventoryItems`, `InventoryItem` | `NearbyQuestTemplateTable.cs`, `NearbyQuestTemplateXmlExtractor.cs`, `NearbyQuestStartConditionService.cs`, nearby tests | Implementation | No with other predicate edits | Medium | Central predicate and extractor files need one writer. |
| B | Repeat timing audit | `QuestState.canRepeat`, repeat-cycle/template time fields, DAO timestamps | Read-only Java/docs report | Java Analysis | Yes | Low | Pure analysis can run beside implementation. |
| C | Broader archetype audit | `PlayerController.updateNearbyQuests`, real `quest_data.xml` | Existing real-data audit tests/docs | Test Creation | Not with A in this unit | Medium | Predicate changes affect baselines. |
| D | NPC faction predicate audit | `NpcFaction`, quest faction services | Read-only Java/docs report | Java Analysis | Yes | Low | Independent analysis. |

Selected work:
- Orchestrator implemented staged inventory item preconditions.
- Read-only sub-agent analyzed Java repeat timing and made no edits; the sub-agent was closed.

## Java Source Breadcrumbs

- `com.aionemu.gameserver.services.QuestService.inventoryItemCheck(Player, QuestTemplate, boolean)` checks every configured `InventoryItem.itemId` with `player.getInventory().getFirstItemByItemId(...)`.
- The Java inventory start gate does not enforce `InventoryItem.count`; count semantics belong to other collection/consume paths.
- Nearby checks call start conditions with `warn = false`, so Java warning packets are not part of this staged nearby path.
- Repeat timing analysis found `QuestState.canRepeat()` requires `completeCount < maxRepeatCount` except `255` is unlimited, and time-based repeat quests require `nextRepeatTime == null || now >= nextRepeatTime`.

## Implementation

- Added `Aion.GameServer.Dataholders.NearbyQuestInventoryItem`.
- Extended `NearbyQuestTemplateSummary` with staged inventory item rows.
- Extended `NearbyQuestTemplateXmlExtractor` to parse `<inventory_items><inventory_item item_id="..." count="..." /></inventory_items>`.
- Extended `NearbyQuestStartConditionService` with the Java item-id presence gate.
- Preserved XML `count` for traceability but intentionally ignored it in the nearby start predicate.
- Kept manually constructed templates with `HasInventoryItems = true` and no parsed rows fail-closed as `UnsupportedInventoryItems`.

## Files Changed

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateXmlExtractorTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestStartConditionServiceTests.cs`
- `docs/PHASE-6-PROGRESS.md`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `docs/ItemPurification-NearbyQuestRefresh-Audit.md`
- `docs/NearbyQuestRefresh-SendBoundary-Audit.md`
- `docs/ItemPurification-AP-Quest-Readiness-Audit.md`
- `docs/ItemPurification-Automatic-Dispatch-Readiness.md`

## Validation

| Command | Result |
|---|---|
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~NearbyQuestTemplateXmlExtractorTests|FullyQualifiedName~NearbyQuestRefreshPlanServiceTests|FullyQualifiedName~NearbyQuestMarkerProjectionServiceTests"` | Passed, 20 tests |
| `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj` | Passed, 1708 tests |

## Migration Parity Table - UOW-1001

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.inventoryItemCheck` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | Implements the nearby `warn = false` inventory precondition branch by checking item-id presence only. Java warning packet text is intentionally not modeled for this nearby path; count-based collection/consumption behavior remains outside this gate. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItems` | `Aion.GameServer.Dataholders.NearbyQuestInventoryItem`; `NearbyQuestTemplateXmlExtractor` | Dataholder / XML Predicate Dependency | Partial | Unit Tested | Partial Parity | Parses `inventory_items` containers into staged item rows. Empty containers are not represented as blocking conditions. Production JAXB/static-data integration remains unwired. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItem` | `Aion.GameServer.Dataholders.NearbyQuestInventoryItem` | DTO / XML Predicate Dependency | Partial | Unit Tested | Partial Parity | Parses required `item_id` and optional `count`. Count is kept for traceability but intentionally ignored by the nearby predicate, matching Java `inventoryItemCheck`. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `NearbyQuestTemplateXmlExtractor`; `NearbyQuestTemplateTable` | Dataholder / XML Extractor | Partial | Unit Tested; Regression Tested | Needs Verification | Template summary now carries inventory precondition rows. Production `StaticData`, JAXB equivalence, collect-item semantics, combine-skill metadata, NPC faction state, and repeat-cycle timing remain unverified. |
| `com.aionemu.gameserver.questEngine.model.QuestState`; `com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle`; `PlayerQuestListDAO` | Future C# repeat timing fields/service | Quest State / Date-Time Dependency | Not Started | Manual Only | Needs Verification | Read-only sub-agent analysis documents Java repeat timing. C# still lacks `NextRepeatTime`, `CompleteTime`, preserved repeat-cycle values, and server-timezone-aware comparisons. |

## Tests Added Or Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate`, `InventoryItems`, and `InventoryItem` JAXB fields | Validates staged parsing of multiple inventory item rows, including optional count. | Deterministic C# test from source-reviewed Java model fields. | No Java JAXB runtime comparison. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Java `QuestTemplate` defaults | Validates missing inventory rows produce empty staged inventory preconditions. | Deterministic C# test from source-reviewed defaults. | Empty `inventory_items` container behavior is not separately runtime-compared. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaInventoryItemPresenceGate` | Unit | Java `QuestService.inventoryItemCheck` | Validates all listed item ids must exist and optional XML count is not enforced for nearby checks. | Deterministic C# test from source-reviewed Java predicate. | Warning packet text is outside nearby `warn = false`; count-based collect-item checks remain separate. |
| Read-only repeat-timing sub-agent analysis | Manual | Java `QuestState.canRepeat`, `QuestTemplate.repeat_cycle`, `QuestRepeatCycle`, `PlayerQuestListDAO`, and nearby callers | Documents repeat reset behavior, timezone concerns, edge cases, and recommended C# slice. | Source-reviewed report; sub-agent made no edits and was closed. | No C# repeat-timing implementation yet. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Inventory preconditions are staged and unit-tested, but production static-data integration is still absent.
- Java warning packet behavior is not modeled because nearby checks call with `warn = false`; non-nearby quest acquisition remains outside this staged service.
- XML inventory `count` is intentionally ignored for this gate, but count-based collect item behavior remains a separate unported area.
- Repeat timing remains unsupported; C# still lacks next-repeat/complete-time fields, preserved repeat-cycle values, deterministic clock injection, and timezone-normalized comparisons.
- Combine-skill checks and NPC faction checks remain unsupported.
- Packet sends, `CM_LEVEL_READY`, NPC-spawn delayed refresh, production player-controller refresh, and ItemPurification dispatch remain disabled.
- Java `HashMap`/set ordering is not claimed.
- Reflection/dynamic handler execution and production startup integration remain unported.

## Summary Metrics

- Total Java artifacts discovered: 5 in this unit
- Total artifacts ported: 1 staged inventory precondition DTO/predicate slice in this unit
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 5
- Total blocked artifacts: 5 blocked/not-started categories: repeat timing, combine-skill predicates, NPC faction predicates, production static-data integration, and live nearby send triggers
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit reduces one nearby predicate blocker without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Implement repeat timing for nearby start checks in a narrow staged slice:

- Add `NextRepeatTime` and likely `CompleteTime` to `PlayerQuestState`.
- Load `next_repeat_time` and `complete_time` from `player_quests`.
- Preserve repeat-cycle values instead of only `IsTimeBased`.
- Add a deterministic clock and server-timezone-aware comparisons.
- Replace `UnsupportedRepeatTiming` only for covered Java `QuestState.canRepeat()` cases.

Keep packet sends, production integration, and production ItemPurification dispatch disabled until repeat timing has tests.

Alternative parallel-friendly next work:

- Read-only NPC faction predicate audit.
- Read-only combine-skill/master-crafting predicate audit.
- Broader representative-player archetype audit of real nearby refresh plans.
