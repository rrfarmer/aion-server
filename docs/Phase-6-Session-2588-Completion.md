# Phase 6 Session 2588 Completion

## UOW

[Phase 6] UOW-2588: Send quest-start inventory-item warning

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now sends Java
`SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM` when a quest requires an inventory item that the player
does not have. The rejected start still does not mutate `player.Quests`, does not persist quest rows, and does not
send the quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback when a required inventory item is missing.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> inventoryItemCheck -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM.
- C# runtime artifact wired or fixed: GameServerConnection.HandleQuestStartUseItemAsync inventory failure branch, SmSystemMessage helper, and runtime item-template client-name lookup.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE id 1300594 with the required item client-name parameter while preserving no-mutation/no-persistence behavior.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output from an already-wired item-use packet path.
```

## Java Source Reviewed

- `QuestService.checkStartConditions` calls `inventoryItemCheck(env, warn)`.
- `QuestService.inventoryItemCheck` iterates `QuestTemplate.getInventoryItems().getInventoryItems()`.
- Java checks `player.getInventory().getFirstItemByItemId(inventoryItem.getItemId()) == null`; it does not enforce the XML count in this start-condition gate.
- When warning is enabled, Java sends `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM(requiredItemL10n)` for the first missing item.
- `requiredItemL10n` is `DataManager.ITEM_DATA.getItemTemplate(itemId).getL10n()`.
- `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM(String)` uses message id `1300594`.

## C# Changes

- Added `SmSystemMessage.QuestAcquireErrorInventoryItem(string)` for message id `1300594`.
- Updated `GameServerConnection.HandleQuestStartUseItemAsync` to send the inventory-item warning when
  `NearbyQuestStartConditionService` reports `InventoryItems`.
- Added `CreateInventoryItemStartConditionFailureMessage`, which matches Java's first-missing-item loop and uses
  `ItemTemplateSummary.GetClientName()` for the client-name parameter.
- Added a live use-item test with XML-loaded quest inventory requirements and item-template client-name assertion.

## Known Gaps

- If the required item template is missing from C# runtime static data, C# returns silently for this message branch; Java assumes `DataManager.ITEM_DATA` resolves the template.
- Inventory-item counts are not checked, matching the Java start-condition gate; separate collect-item checks remain outside this UOW.
- Full dialog acceptance remains unported; this is still the direct narrow quest-start item path.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output for quest-start inventory-item condition failures.
- Specific behavior/contract: missing required inventory item sends Java SM_SYSTEM_MESSAGE id 1300594 with ItemTemplate.getL10n-compatible client-name parameter and does not mutate or persist quest state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --no-restore -> 99/99 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly exercised the modified live handler branch, XML-loaded inventory requirement, item-template client-name lookup, packet serialization, and no-mutation/no-persistence assertions.
- Why this scope is sufficient: the new test drives the real HandleUseItemAsync path from XML-loaded quest/item metadata through serialized SM_SYSTEM_MESSAGE assertions.
```

Additional hygiene:

```text
git diff --check -> passed; only CRLF conversion warnings were emitted.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.inventoryItemCheck` warning branch | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | First missing required item now sends a live packet; broader QuestService/dialog parity remains partial. |
| `QuestTemplate.getInventoryItems` | `NearbyQuestTemplateSummary.InventoryItems` | Runtime data | Partial | Unit Tested | Partial Parity | Already-loaded inventory requirements are now consumed by a live warning path; broader inventory/collect-item behavior remains partial. |
| `ItemTemplate.getL10n` | `ItemTemplateSummary.GetClientName` | Runtime data | Partial | Unit Tested | Partial Parity | Existing client-name encoding is used as the packet parameter; missing item-template behavior differs by returning silently. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM` | `SmSystemMessage.QuestAcquireErrorInventoryItem` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300594` and parameter are serialized from a live handler test; broader generated-message catalog parity remains partial. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsInventoryItemConditionFailureMessage` | Unit/live handler | `QuestService.inventoryItemCheck`, `SM_SYSTEM_MESSAGE` | Missing required item sends `1300594` with item client-name parameter and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover multiple missing items beyond first-missing-loop behavior. |

## Summary Metrics

- Focused validation: 99 tests passed.
- Runtime progress: missing required item live quest-start uses now produce Java-equivalent client-visible feedback.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, missing-item-template exception parity).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- Missing required item-template behavior is softer in C# than Java's assumed data-manager lookup.
- Collect-item removal/count checks remain separate from this start-condition warning path.

## Next Runtime Candidate

UOW-2589 candidate: wire the Java combine-skill quest-start warning packet for live quest-start item use.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the player lacks required combine/crafting skill rank.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> checkCombineSkill -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK.
- C# runtime artifact to wire or fix: NearbyQuestStartConditionFailure.CombineSkill mapping, SmSystemMessage helper, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

If the combine-skill failure cannot be mapped directly from existing quest XML and player skill state, skip it and
select another live packet/state/persistence branch.
