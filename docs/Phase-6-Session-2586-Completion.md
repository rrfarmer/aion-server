# Phase 6 Session 2586 Completion

## UOW

[Phase 6] UOW-2586: Send quest-start item condition messages

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now sends the
Java system-message packets for directly mapped `QuestService.checkStartConditions(..., warn=true)` failures:
race, minimum level, maximum level, class, and gender. Rejected uses still do not mutate `player.Quests`, do not
persist quest rows, and do not send the quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback for failed non-repeat start conditions instead of silently returning.
- Java source method or runtime path: QuestStartAction.act -> QuestEngine.onDialog(ASK_QUEST_ACCEPT) -> QuestService.startQuest/checkStartConditions warn=true.
- C# runtime artifact wired or fixed: SmSystemMessage quest-acquire helpers and GameServerConnection.HandleQuestStartUseItemAsync failure-message mapping from NearbyQuestStartConditionFailure.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE packets for race, level, class, and gender condition failures while preserving no-mutation/no-persistence behavior.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output from an already-wired item-use packet path.
```

## Java Source Reviewed

- `QuestStartAction.act` delegates accepted starts through the quest engine dialog path.
- `QuestService.startQuest` calls `checkStartConditions(env, true)` before mutating quest state.
- `QuestService.checkStartConditions` sends warning messages before returning false for these mapped failures:
  - race: `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_RACE()`;
  - minimum level: `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL(template.getMinlevelPermitted())`;
  - maximum level: `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL(template.getMaxlevelPermitted())`;
  - class: `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_CLASS()`;
  - gender: `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_GENDER()`.
- `SM_SYSTEM_MESSAGE` ids used by the Java helpers are `1300575`, `1300571`, `1300572`, `1300580`, and `1300579`.

## C# Changes

- Added `SmSystemMessage` helpers for race, minimum-level, maximum-level, class, and gender quest-acquire failures.
- Updated `GameServerConnection.HandleQuestStartUseItemAsync` to map directly modeled
  `NearbyQuestStartConditionFailure` values to those Java-equivalent system messages.
- Preserved existing active-quest and none-repeatable rejection behavior from UOW-2585.
- Added live use-item tests for fixed-message condition failures and level-parameter condition failures, including
  packet ids, level parameters, and no persistence calls.

## Known Gaps

- Rank failure remains unmapped because the Java packet parameter depends on abyss-rank localization data not present in this narrow runtime path.
- Inventory, XML condition, combine-skill, quest-list capacity, and NPC-faction start-condition messages remain partial or silent in this immediate-start C# branch.
- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output for quest-start condition failures.
- Specific behavior/contract: rejected queststart item uses send Java SM_SYSTEM_MESSAGE ids 1300575/1300571/1300572/1300580/1300579 and do not mutate or persist quest state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore -> 107/107 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly exercised the modified live handler packet branches plus adjacent condition classification.
- Why this scope is sufficient: the new tests drive the real HandleUseItemAsync path from XML-loaded quest metadata through serialized SM_SYSTEM_MESSAGE assertions and verify no repository mutation calls occur.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.checkStartConditions` race/level/class/gender warnings | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Directly modeled start-condition failures now send live packets; broader startQuest/dialog parity remains partial. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_RACE` | `SmSystemMessage.QuestAcquireErrorRace` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300575` is serialized from a live handler test. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_LEVEL` | `SmSystemMessage.QuestAcquireErrorMinLevel` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300571` and level parameter are serialized from a live handler test. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MAX_LEVEL` | `SmSystemMessage.QuestAcquireErrorMaxLevel` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300572` and level parameter are serialized from a live handler test. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_CLASS` | `SmSystemMessage.QuestAcquireErrorClass` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300580` is serialized from a live handler test. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_GENDER` | `SmSystemMessage.QuestAcquireErrorGender` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300579` is serialized from a live handler test. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsFixedStartConditionFailureMessages` | Unit/live handler | `QuestService.checkStartConditions` | Race, class, and gender failures send Java message ids and do not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover rank or XML condition messages. |
| `HandleUseItemAsync_QuestStartItemSendsLevelStartConditionFailureMessages` | Unit/live handler | `QuestService.checkStartConditions` | Minimum and maximum level failures send Java message ids with XML-configured level parameters and do not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover quest-list capacity or inventory checks. |

## Summary Metrics

- Focused validation: 107 tests passed.
- Runtime progress: rejected live quest-start item uses now produce Java-equivalent feedback for five additional start-condition failures.
- Total Java artifacts touched/discovered this UOW: 2.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 6.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, abyss-rank localized warning parameter).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The full Java dialog path may reorder or supplement messages once ported.
- Conditions not represented by `NearbyQuestStartConditionFailure` remain silent in the immediate item-start branch.
- No full generated system-message catalog parity audit was performed.

## Next Runtime Candidate

UOW-2587 candidate: wire a narrow live `QuestService.startQuest` rejection for quest-list capacity if the C# player
quest collection exposes the real capacity rule and Java's `STR_QUEST_LIST_FULL` packet mapping can be applied from
live `CM_USE_ITEM` without scaffolding.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the player cannot accept more active quests.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true quest-list capacity branch.
- C# runtime artifact to wire or fix: live quest-start condition service or GameServerConnection.HandleQuestStartUseItemAsync plus SmSystemMessage helper if the existing player quest collection has a real capacity signal.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

If the capacity rule is not present in live C# state, skip it and select a packet/state/persistence branch with an
existing runtime surface.
