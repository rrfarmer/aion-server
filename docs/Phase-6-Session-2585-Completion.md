# Phase 6 Session 2585 Completion

## UOW

[Phase 6] UOW-2585: Send quest-start item rejection messages

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now sends the
Java system-message packets for two `QuestStartAction.canAct` rejection cases: already-working quest state and
completed non-repeatable quest state. Rejected uses do not mutate `player.Quests`, do not persist quest rows, and do
not send the quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback when the quest is already active or cannot be repeated.
- Java source method or runtime path: QuestStartAction.canAct -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST / STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE.
- C# runtime artifact wired or fixed: SmSystemMessage quest-acquire helpers, NearbyQuestTemplateSummary.Name runtime loading, GameServerConnection.HandleQuestStartUseItemAsync rejection branches.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE packets and still avoid quest mutation/persistence.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output from an already-wired item-use packet path.
```

## Java Source Reviewed

- `QuestStartAction.canAct`:
  - sends `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST()` when the quest state is not complete;
  - sends `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE(DataManager.QUEST_DATA.getQuestById(questid).getName())` when a completed quest cannot repeat.
- `SM_SYSTEM_MESSAGE`:
  - `STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST()` uses message id `1300597`;
  - `STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE(String)` uses message id `1300599`.
- `quest_data.xsd`:
  - `quest` has a `name` attribute, which backs Java `QuestTemplate.getName()`.

## C# Changes

- Added `SmSystemMessage.QuestAcquireErrorWorkingQuest()` and `QuestAcquireErrorNoneRepeatable(string)`.
- Added `NearbyQuestTemplateSummary.Name` and loaded it from quest XML.
- Updated `GameServerConnection.HandleQuestStartUseItemAsync` to send:
  - working-quest message for existing non-complete quest states;
  - none-repeatable message for completed repeat-count/repeat-timing failures.
- Added live use-item tests for both rejection cases, including packet ids, quest name parameter, and no persistence calls.

## Known Gaps

- Other Java `QuestService.startQuest` warning messages for race, level, class, gender, rank, inventory, XML conditions, quest-list capacity, and NPC faction conditions remain partial or silent in this immediate-start C# path.
- Java `QuestStartAction.canAct` sends the same none-repeatable message for repeat count and repeat timing; this UOW follows that behavior for the currently modeled repeat failures.
- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output plus quest template XML runtime loading.
- Specific behavior/contract: rejected queststart item uses send Java SM_SYSTEM_MESSAGE ids 1300597/1300599 and do not mutate or persist quest state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore -> 102/102 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly exercised the modified live handler packet branches plus adjacent repeat-condition classification.
- Why this scope is sufficient: the new tests drive the real HandleUseItemAsync path from XML-loaded quest metadata through serialized SM_SYSTEM_MESSAGE assertions and verify no repository mutation calls occur.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestStartAction.canAct` rejection messages | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Working-quest and none-repeatable rejection packets are live; other start-condition warnings remain partial because full dialog/QuestService warning flow is not ported. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST` | `SmSystemMessage.QuestAcquireErrorWorkingQuest` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300597` is serialized from a live handler test; broader generated-message catalog parity remains partial. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE` | `SmSystemMessage.QuestAcquireErrorNoneRepeatable` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300599` and quest-name parameter are serialized from a live handler test. |
| `QuestTemplate.getName` | `NearbyQuestTemplateSummary.Name` | Runtime data | Partial | Unit Tested | Partial Parity | Quest XML `name` is now loaded for live item-start messaging; broader quest template data remains partial. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsWorkingQuestMessageForActiveState` | Unit/live handler | `QuestStartAction.canAct` | Active quest state sends `1300597` and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover every non-complete status value. |
| `HandleUseItemAsync_QuestStartItemSendsNoneRepeatableMessageForCompletedNonRepeatableState` | Unit/live handler | `QuestStartAction.canAct`, `QuestTemplate.getName` | Completed non-repeatable state sends `1300599` with XML quest name and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover repeat-timing failure separately. |

## Summary Metrics

- Focused validation: 102 tests passed.
- Runtime progress: rejected live quest-start item uses now produce Java-equivalent client-visible feedback.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 5.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: 2 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Quest-start item start-condition failures beyond active/non-repeatable remain silent or partial.
- The full Java dialog path may reorder or supplement messages once ported.
- Quest XML `name` is now loaded for this packet parameter, but no full quest-template parity audit was performed.

## Next Runtime Candidate

UOW-2586 candidate: wire a narrow subset of Java `QuestService.startQuest` warning messages for quest-start item
start-condition failures, starting with race/min-level/class/gender only if the message helpers and condition
failures can be mapped directly to real `SmSystemMessage` output from `HandleQuestStartUseItemAsync`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback for failed non-repeat start conditions instead of silently returning.
- Java source method or runtime path: QuestStartAction.act -> QuestEngine.onDialog(ASK_QUEST_ACCEPT) -> QuestService.startQuest/checkStartConditions warn=true.
- C# runtime artifact to wire or fix: SmSystemMessage helpers if needed, NearbyQuestStartConditionFailure mapping, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends real system-message packets without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

If the message mapping cannot be done directly from current condition results, skip it and choose another live packet/state/persistence branch.
