# Phase 6 Session 2587 Completion

## UOW

[Phase 6] UOW-2587: Reject quest-start items at normal quest cap

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now applies the
Java normal-quest list capacity guard before quest mutation/persistence. When a normal counted quest would exceed
`gameserver.basic.questsize.limit`, the handler sends Java `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_NORMAL`
and returns without mutating `player.Quests`, without inserting/updating `player_quests`, and without sending the
quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback when the player cannot accept more active normal quests.
- Java source method or runtime path: QuestService.startQuest -> checkQuestListSize -> QuestStateList.getNormalQuests -> QuestTemplate.isNoCount.
- C# runtime artifact wired or fixed: GameServerConnection.HandleQuestStartUseItemAsync, SmSystemMessage, GameServerOptions membership/custom settings, and NearbyQuestTemplateSummary runtime XML data.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE id 1300622 and preserve no-mutation/no-persistence behavior; Java membership and no-count bypasses still allow the live quest start.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output and live quest-start gating from an existing item-use runtime path.
```

## Java Source Reviewed

- `QuestService.startQuest` checks `!template.isNoCount() && !checkQuestListSize(qsl) && !player.hasPermission(MembershipConfig.QUEST_LIMIT_DISABLED)`.
- `QuestService.checkQuestListSize` allows the start only when `qsl.getNormalQuests().size() + 1 <= CustomConfig.BASIC_QUEST_SIZE_LIMIT`.
- `QuestStateList.getNormalQuests` counts quests with category `QUEST` and status neither `COMPLETE` nor `LOCKED`.
- `QuestTemplate.isNoCount` returns true for `QuestCategory.NON_COUNT` and `QuestCategory.EVENT`.
- `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_NORMAL()` uses message id `1300622`.
- Java config keys reviewed:
  - `gameserver.basic.questsize.limit`, default `40`;
  - `gameserver.quest.limit.disable`, default `10`.

## C# Changes

- Added `SmSystemMessage.QuestAcquireErrorMaxNormal()` for message id `1300622`.
- Loaded Java `QuestTemplate.isNoCount` semantics into `NearbyQuestTemplateSummary.IsNoCount` from quest XML category `NON_COUNT` or `EVENT`.
- Loaded `gameserver.quest.limit.disable` into `GameServerOptions.Membership.QuestLimitDisabled`.
- Updated `GameServerConnection.HandleQuestStartUseItemAsync` to enforce the Java normal quest cap after start conditions and before mutation/persistence.
- Added live use-item tests for:
  - over-limit normal quest rejection;
  - membership permission bypass;
  - no-count quest bypass through XML-loaded `EVENT` category.

## Known Gaps

- Full dialog acceptance remains unported; this remains the direct narrow quest-start item path.
- The normal quest count ignores C# quest rows whose templates are missing from the runtime quest table; Java expects `DataManager.QUEST_DATA` to resolve those templates.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output and quest-start gating, plus Java config/static quest-category data consumed by that live path.
- Specific behavior/contract: full normal quest lists send Java SM_SYSTEM_MESSAGE id 1300622 and do not mutate/persist; membership permission and no-count categories bypass the cap and still start the quest.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --no-restore -> 98/98 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output and live gating changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly exercised the modified live handler branches, XML-loaded no-count data, config options, packet serialization, and no-mutation/no-persistence assertions.
- Why this scope is sufficient: the new tests drive the real HandleUseItemAsync path from XML-loaded quest metadata through serialized SM_SYSTEM_MESSAGE and successful SM_QUEST_ACTION branches.
```

Additional hygiene:

```text
git diff --check -> passed; only CRLF conversion warnings were emitted.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.startQuest` normal quest cap branch | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Capacity rejection, membership bypass, and no-count bypass are live for quest-start items; broader dialog/QuestService parity remains partial. |
| `QuestService.checkQuestListSize` / `QuestStateList.getNormalQuests` | `GameServerConnection.CanStartNormalQuest` | Utility | Partial | Unit Tested | Partial Parity | Counts active normal `QUEST` rows and excludes `COMPLETE`/`LOCKED`; missing runtime templates are ignored instead of failing like Java's full data manager would. |
| `QuestTemplate.isNoCount` | `NearbyQuestTemplateSummary.IsNoCount` / `NearbyQuestTemplateXmlExtractor` | Runtime data | Partial | Unit Tested | Partial Parity | `NON_COUNT` and `EVENT` categories are loaded for live quest-start gating; broader quest-template category behavior remains partial. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_NORMAL` | `SmSystemMessage.QuestAcquireErrorMaxNormal` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300622` is serialized from a live handler test; broader generated-message catalog parity remains partial. |
| `CustomConfig.BASIC_QUEST_SIZE_LIMIT` | `GameServerCustomOptions.BasicQuestSizeLimit` | Config | Complete | Unit Tested | Partial Parity | Existing Java config key/default is consumed by the live quest-start guard. |
| `MembershipConfig.QUEST_LIMIT_DISABLED` | `GameServerMembershipOptions.QuestLimitDisabled` | Config | Complete | Unit Tested | Partial Parity | Java config key/default is loaded and consumed by the live permission bypass. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsMaxNormalMessageWhenQuestListIsFull` | Unit/live handler | `QuestService.startQuest`, `checkQuestListSize` | Full normal quest list sends `1300622` and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover missing quest-template behavior. |
| `HandleUseItemAsync_QuestStartItemAllowsMembershipQuestLimitBypass` | Unit/live handler | `Player.hasPermission(MembershipConfig.QUEST_LIMIT_DISABLED)` | Configured membership bypasses the cap and live quest start still persists/sends packets | Source-reviewed Java + live C# handler assertion | Does not cover all membership levels. |
| `HandleUseItemAsync_QuestStartItemAllowsNoCountQuestWhenQuestListIsFull` | Unit/live handler | `QuestTemplate.isNoCount` | XML `EVENT` category bypasses the cap and live quest start still persists/sends packets | Source-reviewed Java + live C# handler assertion | `NON_COUNT` is source-reviewed but not separately exercised in this live handler test. |

## Summary Metrics

- Focused validation: 98 tests passed.
- Runtime progress: over-cap live quest-start item uses now produce Java-equivalent client-visible feedback and preserve no-mutation/no-persistence behavior.
- Total Java artifacts touched/discovered this UOW: 6.
- Total C# artifacts touched: 6.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 6.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, missing-template parity for normal quest counting).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- Java's full `QuestStateList` uses a `TreeMap`; C# player quest row ordering is not changed by this UOW.
- Missing quest templates in C# are ignored for the normal-count calculation; Java static data should have all referenced quest templates.

## Next Runtime Candidate

UOW-2588 candidate: wire a narrow live inventory-item start-condition warning for quest-start item use if the Java
`QuestService.inventoryItemCheck` packet parameter can be sourced from already-loaded runtime item template data.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when a required inventory item is missing.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> inventoryItemCheck -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_INVENTORY_ITEM.
- C# runtime artifact to wire or fix: NearbyQuestTemplateSummary inventory-item data, item-template name/l10n lookup if available, SmSystemMessage helper, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

If the required item-name/l10n parameter is not available from live runtime data, skip this candidate and select another
packet/state/persistence branch with an existing runtime surface.
