# ItemPurification Quest Update Items Audit

Date: May 25, 2026
Unit of Work: UOW-976 audit, updated by UOW-977 implementation

## Purpose

This audit records the Java `QuestEngine.questUpdateItems` behavior that matters before ItemPurification can invoke real quest item callbacks.

The Java project remains the source of truth. This document does not enable production `CM_ITEM_PURIFICATION` dispatch and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/InventoryItems.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/InventoryItem.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestUpdateItemTable.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/StaticDataLoadingTests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationQuestMutationNotifier.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveExecutionService.cs`

## Java Behavior

During `QuestEngine.init`, Java iterates every `QuestTemplate` from `DataManager.QUEST_DATA.getQuestTemplates()`.

For each quest:

1. Java registers each `quest_drop` with `QuestService.addQuestDrop`.
2. If `data.getInventoryItems()` is not null, Java iterates `data.getInventoryItems().getInventoryItems()`.
3. For each `inventory_item`, Java adds `inventoryItem.getItemId()` to `questUpdateItems` only if it is not already present.

The JAXB model matters:

- `InventoryItems.getInventoryItems()` returns `Collections.emptyList()` when the XML list is absent.
- `InventoryItem.item_id` is an `Integer` XML attribute.
- `InventoryItem.count` is optional and is not used for `questUpdateItems`.

Runtime item callbacks then use the set differently:

- `QuestEngine.onItemGet(player, itemId)` invokes registered get-item quest handlers from `questItems`, then calls `player.getController().updateNearbyQuests()` when `questUpdateItems` contains the item id.
- `QuestEngine.onItemRemoved(player, itemId)` does not invoke a symmetric remove-handler map; it only calls `updateNearbyQuests()` when `questUpdateItems` contains the item id.

## C# Current Status

- UOW-977 adds `QuestUpdateItemTable` and exposes `StaticData.QuestUpdateItems`.
- `StaticData.cs` now parses quest drop and collect-item metadata for the world-NPC quest-drop path and also projects distinct `inventory_item/@item_id` values from quest XML into `QuestUpdateItems`.
- The projection preserves Java first-seen ordering for `questUpdateItems` membership and ignores optional `count`, matching the source-reviewed `QuestEngine.init` behavior for this narrow static-data slice.
- `StaticData_LoadsQuestUpdateItemIdsFromQuestInventoryItems` covers duplicate item ids, ignored `count`, absent `inventory_items`, and membership lookups.
- `ItemPurificationApplicationPlanService.ProjectQuestNotifications` projects Java-ordered item get/remove intent for material/base deletes and target add.
- `IItemPurificationQuestMutationNotifier` / `NoOpItemPurificationQuestMutationNotifier` can receive projected ItemPurification candidates when explicitly supplied, but no real nearby-quest refresh or get-item handler dispatch is wired yet.
- UOW-978 adds `PlanningItemPurificationQuestMutationNotifier` and `ItemPurificationNearbyQuestRefreshPlan`, which filter projected ItemPurification candidates through `QuestUpdateItems` and report which ones would request Java-style nearby refresh. This remains a no-op planning seam and does not invoke the player controller.

## Parity Gaps

- C# now exposes distinct quest update item ids sourced from quest XML `<inventory_items>`, but the projection has not been compared against Java runtime output or a Java-generated golden file.
- C# can now plan which ItemPurification candidates would request `player.getController().updateNearbyQuests()`, but no C# path invokes the player controller or an equivalent nearby-quest refresh.
- No C# get-item quest handler map equivalent is wired for ItemPurification item gets.
- No C# runtime distinction exists yet between get-item handler dispatch and nearby-quest refresh.
- Java JAXB null/list behavior is source-reviewed and covered for the absent-`inventory_items` static-data case, but broader quest XML handler behavior remains unported.

## Recommended Next Implementation Slice

Add a narrow quest refresh dispatch seam:

1. Add an opt-in dispatcher interface for nearby-quest refresh that can consume `ItemPurificationNearbyQuestRefreshPlan`.
2. Keep the default implementation no-op and keep automatic production dispatch disabled.
3. Do not invoke dynamic get-item quest handlers until handler registration has a C# home.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.StaticData` quest XML scan plus `QuestUpdateItemTable` | Quest Engine / Static Data | Partial | Unit Tested | Partial Parity | C# now projects distinct quest update item ids in first-seen order from quest template inventory items. Real quest registration, dynamic handler load, nearby refresh invocation, Java runtime comparison, threading behavior, and reflection/dynamic behavior remain missing. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItems` | `Aion.GameServer.Dataholders.StaticData` inventory item scan | XML DTO / Projection | Partial | Unit Tested | Partial Parity | C# covers absent `inventory_items` by producing no update ids, matching Java `Collections.emptyList()` effect for this projection. No dedicated JAXB-shaped DTO exists; broader XML serialization/reflection behavior is not ported. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItem` | `Aion.GameServer.Dataholders.StaticData` `inventory_item/@item_id` projection | XML DTO / Projection | Partial | Unit Tested | Partial Parity | C# reads `item_id` and ignores optional `count` for update membership. Missing/nullable `item_id` Java edge behavior is not covered; Java runtime/golden comparison is still absent. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` | `PlanningItemPurificationQuestMutationNotifier`; `ItemPurificationNearbyQuestRefreshPlan`; `QuestUpdateItemTable` | Quest Callback / Planning Service | Partial | Unit Tested | Partial Parity | C# can project get-item intent and plan nearby-refresh candidates through static update-item membership. Real get-item handler dispatch, player-controller refresh invocation, threading behavior, and Java runtime comparison remain missing. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemRemoved` | `PlanningItemPurificationQuestMutationNotifier`; `ItemPurificationNearbyQuestRefreshPlan`; `QuestUpdateItemTable` | Quest Callback / Planning Service | Partial | Unit Tested | Partial Parity | C# can project remove intent and plan nearby-refresh candidates through static update-item membership. Java remove behavior has no symmetric handler map, but the actual controller refresh invocation is still not wired. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `StaticData_LoadsQuestUpdateItemIdsFromQuestInventoryItems` | Unit | Java `QuestEngine.init`, `InventoryItems.getInventoryItems`, and `InventoryItem.getItemId/getCount` source review | Validates first-seen distinct item-id projection, ignored `count`, absent `inventory_items`, and membership lookup behavior. | Deterministic XML unit test from source-reviewed Java behavior. | Does not invoke Java runtime, compare against Java-generated output, or test dynamic quest handler/nearby-refresh behavior. |
| `PlanningNotifier_FiltersNearbyRefreshCandidatesThroughQuestUpdateItems` | Unit | Java `QuestEngine.onItemGet` and `onItemRemoved` source review | Validates that only projected get/remove candidates whose item id is in `questUpdateItems` become nearby-refresh candidates, preserving notification order. | Deterministic unit test from source-reviewed Java membership behavior. | Does not invoke real `updateNearbyQuests`, dynamic handlers, or Java runtime. |
| `PlanningNotifier_ReportsNoRefreshCandidatesWhenItemsAreNotQuestUpdateItems` | Unit | Java `questUpdateItems.contains(itemId)` source review | Validates non-member get/remove candidates do not request nearby refresh. | Deterministic unit test from source-reviewed Java behavior. | No live quest engine execution. |
| `PlanningNotifier_ReportsNoNotificationsWithoutRefreshing` | Unit | Java callback no-op effect when no storage notification occurs | Validates empty notification lists produce no refresh candidates. | Deterministic unit test over C# planner guard. | No Java runtime comparison. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by Java 8 and missing Maven.
- Quest XML shape may include edge cases not covered by the current synthetic C# static-data test, especially missing/nullable `item_id` handling.
- C# must avoid treating every ItemPurification get/remove candidate as a nearby-quest refresh; UOW-978 covers the planning filter, but no live dispatcher invokes refresh yet.
- Real dynamic quest handler dispatch remains Phase 7-scale work unless a narrow bridge is added earlier.
- No C# path invokes nearby-quest refresh yet; this table is static-data membership plus no-op planning only.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 2 narrow static-data projection/table and no-op nearby-refresh planner
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 3 blocked/not-started categories, including live nearby-quest refresh dispatch, real get-item handler dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 70% complete
