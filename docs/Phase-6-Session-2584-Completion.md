# Phase 6 Session 2584 Completion

## UOW

[Phase 6] UOW-2584: Wire quest-start item use

## Status

Completed and validated with focused live handler coverage. The C# runtime now loads Java `queststart` item-action
metadata, routes live `CM_USE_ITEM` for those items, persists the started quest through the existing
`player_quests` table shape, mutates the live player quest list, sends the Java item-use animation packet and
`SM_QUEST_ACTION(ActionType.ADD)`, and refreshes nearby quest markers when a runtime world-map context is available.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: Java quest-start item use now starts an eligible quest from a real item-use packet instead of silently falling through.
- Java source method or runtime path: CM_USE_ITEM.runImpl -> QuestStartAction.canAct/act -> QuestService.startQuest -> PlayerQuestListDAO.addQuests/updateQuests.
- C# runtime artifact wired or fixed: StaticData queststart action extraction, ItemTemplateSummary.QuestStartQuestId, GameServerConnection.HandleUseItemAsync, PlayerEnterWorldService.PersistQuestStartAsync, PlayerEnterWorldRepository.InsertPlayerQuestAsync.
- Client-visible/state/persistence effect changed: using an eligible quest-start item sends SM_ITEM_USAGE_ANIMATION and SM_QUEST_ACTION ADD, mutates player.Quests to START, and inserts or updates player_quests.
- Why this is not preview-only/test-only/documentation-only: it wires a real client packet path to live packet output, player quest state mutation, and database persistence.
```

## Java Source Reviewed

- `CM_USE_ITEM.runImpl`:
  - resolves the source inventory item;
  - checks `PlayerRestrictions.canUseItem`;
  - invokes quest item handlers;
  - executes item template actions when usable.
- `QuestStartAction.canAct`:
  - allows missing quest state or completed repeatable quest state;
  - rejects active non-complete quest states.
- `QuestStartAction.act`:
  - broadcasts `SM_ITEM_USAGE_ANIMATION(playerObjectId, itemObjectId, itemId)`;
  - calls the quest dialog/start path with `DialogAction.ASK_QUEST_ACCEPT`.
- `QuestService.startQuest`:
  - creates a new `QuestState(questId, START)` or updates an existing completed repeatable state to `START`;
  - sends `SM_QUEST_ACTION(ActionType.ADD, qs)`;
  - refreshes nearby quests.
- `PlayerQuestListDAO.addQuests/updateQuests`:
  - persists new and updated quest states to `player_quests`.

## C# Changes

- Added `ItemTemplateSummary.QuestStartQuestId`.
- Extended `StaticData` item XML extraction to read `<queststart questid="..."/>`.
- Added `IPlayerEnterWorldRepository.InsertPlayerQuestAsync` and MySQL insert implementation for `player_quests`.
- Added `PlayerEnterWorldService.PersistQuestStartAsync` to persist new starts as inserts and repeat starts as updates.
- Wired `GameServerConnection.HandleUseItemAsync` to handle quest-start items through live packet/state/persistence effects.
- Extended use-item tests with first-start and completed-repeatable-start cases that assert runtime quest mutation, repository calls, item animation bytes, and `SM_QUEST_ACTION` bytes.

## Known Gaps

- The broader Java `QuestEngine.onItemUseEvent` scripted-handler path remains unported for item use.
- Java dialog acceptance is represented by the immediate direct start path for this narrow action; full dialog-window negotiation is not implemented.
- Java system-message feedback for already-working or non-repeatable completed quests is not emitted yet.
- NPC-faction-specific `QuestService.startQuest` side effects are still partial for this item-start path.
- Real client and live MySQL integration were not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM dispatch, item static-data loading, quest state mutation, quest persistence, packet output.
- Specific behavior/contract: queststart item XML reaches live HandleUseItemAsync, persists a START quest row/update, mutates player.Quests, sends SM_ITEM_USAGE_ANIMATION, and sends SM_QUEST_ACTION ADD.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore -> 150/150 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet dispatch, quest state mutation, and persistence changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly exercised the modified live handler and repository/service seam.
- Why this scope is sufficient: the new tests drive the real HandleUseItemAsync path from loaded item XML through packet/state/persistence assertions while adjacent player-enter-world persistence tests cover interface compile compatibility.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_USE_ITEM.runImpl` quest-start action execution | `GameServerConnection.HandleUseItemAsync` quest-start branch | Handler | Partial | Unit Tested | Partial Parity | Quest-start item actions now reach live quest state/persistence/packet effects; generic quest item handlers and full action ordering across mixed actions remain partial. |
| `QuestStartAction.canAct/act` | `HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Missing-state and completed-repeatable quest starts are live; Java rejection system messages are not emitted yet. |
| `QuestService.startQuest` direct START path | `PlayerEnterWorldService.PersistQuestStartAsync` plus player quest mutation | Service/state | Partial | Unit Tested | Partial Parity | New starts insert, completed repeat starts update, `SM_QUEST_ACTION.ADD` is sent, and nearby refresh is requested. Dialog, quest callbacks, quest-count limits, and NPC-faction start side effects remain partial. |
| `PlayerQuestListDAO.addQuests` | `PlayerEnterWorldRepository.InsertPlayerQuestAsync` | Repository/persistence | Partial | Unit Tested | Partial Parity | New quest rows use the Java column shape; no live MySQL integration was run. |
| `QuestState.canRepeat` | `NearbyQuestStartConditionService.CheckNearbyStartConditions` | Condition gate | Partial | Unit Tested | Partial Parity | Repeat count and repeat timing gate completed repeat starts; broader start-condition warning semantics remain partial. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemPersistsQuestAndSendsQuestAdd` | Unit/live handler | `QuestStartAction.act`, `QuestService.startQuest`, `PlayerQuestListDAO.addQuests` | A loaded queststart item inserts a START quest, mutates player state, sends item animation, and sends quest ADD | Source-reviewed Java + live C# handler assertion | Repository is captured fake, not live MySQL. |
| `HandleUseItemAsync_QuestStartItemRestartsCompletedRepeatableQuest` | Unit/live handler | `QuestState.isStartable/canRepeat`, `QuestService.startQuest` | A completed repeatable quest is updated back to START and sends ADD with retained quest vars/flags | Source-reviewed Java + live C# handler assertion | Does not cover repeat cooldown failure packet messaging. |

## Summary Metrics

- Focused validation: 150 tests passed.
- Runtime progress: live `CM_USE_ITEM` quest-start item path now mutates and persists quest state and sends real packets.
- Total Java artifacts touched/discovered this UOW: 5.
- Total C# artifacts touched: 7.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 5.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, NPC-faction start side effects).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- Quest-start item action ordering is represented as a dedicated C# branch; Java can iterate multiple actions if present.
- The immediate start path is intentionally narrow and does not yet model every Java `QuestService.startQuest` warning or quest-list capacity branch.
- Live DB insert/update behavior was not integration-tested against MySQL.

## Next Runtime Candidate

UOW-2585 candidate: add Java-equivalent rejection feedback for live quest-start item use, only if the exact
`SM_SYSTEM_MESSAGE` ids/functions already exist or can be wired to real packet output in the same UOW. If the
system-message surface is missing and the work would become message scaffolding only, skip it and select another
live packet/state/persistence branch.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the quest is already working or completed non-repeatable instead of silently returning.
- Java source method or runtime path: QuestStartAction.canAct -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST / STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE.
- C# runtime artifact to wire or fix: SmSystemMessage helpers if present, GameServerConnection.HandleQuestStartUseItemAsync rejection branches.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends real system-message packets without mutating quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an already-wired item-use packet branch.
```

If that candidate fails the runtime gate, use the safe candidates in the handoff instead.
