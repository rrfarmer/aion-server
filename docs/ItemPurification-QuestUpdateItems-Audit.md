# ItemPurification Quest Update Items Audit

Date: May 25, 2026
Unit of Work: UOW-976

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

- `StaticData.cs` currently parses quest drop and collect-item metadata for the world-NPC quest-drop path.
- The current C# static data scan has a Java breadcrumb for `QuestEngine.init` quest-drop transfer, but it does not expose a `questUpdateItems` equivalent.
- `ItemPurificationApplicationPlanService.ProjectQuestNotifications` projects Java-ordered item get/remove intent for material/base deletes and target add.
- `IItemPurificationQuestMutationNotifier` / `NoOpItemPurificationQuestMutationNotifier` can receive projected ItemPurification candidates when explicitly supplied, but they do not know whether an item id is in `questUpdateItems`.

## Parity Gaps

- No C# data holder currently exposes the distinct quest update item ids sourced from quest XML `<inventory_items>`.
- No C# path invokes `player.getController().updateNearbyQuests()` or an equivalent nearby-quest refresh.
- No C# get-item quest handler map equivalent is wired for ItemPurification item gets.
- No C# runtime distinction exists yet between get-item handler dispatch and nearby-quest refresh.
- Java JAXB null/list behavior is source-reviewed but not covered by C# XML tests for quest inventory items.

## Recommended Next Implementation Slice

Add a narrow static-data projection:

1. Extend the quest XML scan to collect distinct `inventory_item/@item_id` values into a dedicated table or summary.
2. Preserve Java first-seen ordering or document if C# intentionally stores a set for membership only.
3. Add tests with a small synthetic quest XML containing duplicate and absent `inventory_items` nodes.
4. Do not invoke real quest callbacks yet.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.StaticData` quest XML scan | Quest Engine / Static Data | Partial | Manual Only | Needs Verification | Java builds `questUpdateItems` from quest template inventory items. C# parses quest drops/collect items but does not expose quest update item ids. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItems` | Not started for quest update items | XML DTO | Not Started | No Tests | Unknown | Java returns an empty list when absent. C# needs equivalent absent-list behavior in the static-data projection. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItem` | Not started for quest update items | XML DTO | Not Started | No Tests | Unknown | Java uses `item_id` for update membership and ignores optional `count` for this specific list. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` | `IItemPurificationQuestMutationNotifier` future implementation | Quest Callback | Partial | Regression Tested as No-Op Intent | Needs Verification | C# can project/receive get-item intent, but real get-item handler dispatch and nearby refresh are not wired. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemRemoved` | `IItemPurificationQuestMutationNotifier` future implementation | Quest Callback | Partial | Regression Tested as No-Op Intent | Needs Verification | C# can project/receive remove intent, but Java remove behavior only refreshes nearby quests for `questUpdateItems`; that membership set is missing. |

## Remaining Risks

- Java runtime artifact generation remains blocked locally by Java 8 and missing Maven.
- Quest XML shape may include edge cases not covered by the current C# static-data loader.
- C# must avoid treating every ItemPurification get/remove candidate as a nearby-quest refresh; Java gates refresh through `questUpdateItems`.
- Real dynamic quest handler dispatch remains Phase 7-scale work unless a narrow bridge is added earlier.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 in this docs-only audit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including quest update item static-data projection, nearby-quest refresh, real get-item handler dispatch, and Java runtime comparison
- Estimated overall migration completion: Phase 6 remains about 70% complete
