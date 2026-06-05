# Phase 6 Session 2589 Completion

## UOW

[Phase 6] UOW-2589: Send quest-start combine-skill warning

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now sends Java
`SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK` when a quest requires a crafting/tapping skill rank that the
player does not meet. The rejected start still does not mutate `player.Quests`, does not persist quest rows, and does
not send the quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback when the player lacks the required combine/crafting skill rank.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> checkCombineSkill -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK.
- C# runtime artifact wired or fixed: GameServerConnection.HandleQuestStartUseItemAsync condition-failure mapping and SmSystemMessage helper.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE id 1300574 with the loaded combine_skillpoint parameter while preserving no-mutation/no-persistence behavior.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output from an already-wired item-use packet path.
```

## Java Source Reviewed

- `QuestService.checkStartConditions` calls `checkCombineSkill(env, warn)`.
- `QuestService.checkCombineSkill` checks `QuestTemplate.getCombineSkill()` and `getCombineSkillPoint()` against the player's `PlayerSkillEntry` list.
- Java expands `combineskill="-1"` into crafting skill ids, excluding essence/aether tapping for NPC faction ids 12 and 13.
- Java preserves the TASK-special case where a skill more than 40 points above the required point does not satisfy that TASK requirement.
- When warning is enabled and no candidate skill passes, Java sends `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK(Integer.toString(template.getCombineSkillPoint()))`.
- `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK(String)` uses message id `1300574`.

## C# Changes

- Added `SmSystemMessage.QuestAcquireErrorTsRank(string)` for message id `1300574`.
- Updated `GameServerConnection.CreateQuestStartConditionFailureMessage` to map
  `NearbyQuestStartConditionFailure.CombineSkill` to `QuestAcquireErrorTsRank(template.CombineSkillPoint.ToString())`.
- Added a live use-item test with XML-loaded `combineskill` and `combine_skillpoint` metadata.

## Known Gaps

- Full dialog acceptance remains unported; this is still the direct narrow quest-start item path.
- The existing C# condition detector covers Java combine-skill comparison rules, but broader QuestService parity remains partial.
- Rank, XML-condition, and NPC-faction warning messages remain partial or silent in this immediate-start branch.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output for quest-start combine-skill condition failures.
- Specific behavior/contract: missing required combine-skill rank sends Java SM_SYSTEM_MESSAGE id 1300574 with the combine_skillpoint parameter and does not mutate or persist quest state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_QuestStartItemSendsCombineSkillConditionFailureMessage|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore -> 13/13 passed.
- Wider quest-start C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestStartItem|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore -> 26/26 passed.
- Initial broad class command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore -> failed 1 unrelated composition-stone cleanup-flag assertion, passed 111/112.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: not expanded after the unrelated broad-class failure; the wider quest-start filter built Aion.GameServer and directly exercised the modified live handler branch, XML-loaded combine-skill metadata, packet serialization, and no-mutation/no-persistence assertions.
- Why this scope is sufficient: the passing tests drive the real HandleUseItemAsync quest-start path and the existing Java-equivalent condition-service combine-skill rules without pulling unrelated composition-item nondeterminism into this runtime UOW.
```

Additional hygiene:

```text
git diff --check -> passed; only CRLF conversion warnings were emitted.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.checkCombineSkill` warning branch | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Live quest-start item use now sends a packet for combine-skill failure; broader QuestService/dialog parity remains partial. |
| `QuestTemplate.getCombineSkill/getCombineSkillPoint` | `NearbyQuestTemplateSummary.CombineSkill/CombineSkillPoint` | Runtime data | Partial | Unit Tested | Partial Parity | Already-loaded quest XML values are used by live condition and packet paths. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK` | `SmSystemMessage.QuestAcquireErrorTsRank` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300574` and parameter are serialized from a live handler test; broader generated-message catalog parity remains partial. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsCombineSkillConditionFailureMessage` | Unit/live handler | `QuestService.checkCombineSkill`, `SM_SYSTEM_MESSAGE` | Missing combine-skill rank sends `1300574` with `combine_skillpoint` and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not independently re-test every combine-skill candidate id; existing service tests cover those rules. |

## Summary Metrics

- Focused UOW validation: 13 tests passed.
- Wider quest-start validation: 26 tests passed.
- Runtime progress: combine-skill live quest-start rejections now produce Java-equivalent client-visible feedback.
- Total Java artifacts touched/discovered this UOW: 3.
- Total C# artifacts touched: 3.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 3.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, rank/XML condition warning packets).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- The unrelated composition-stone cleanup-flag test failure seen in the broad class filter remains outside this UOW.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2590 candidate: verify and wire the next Java quest-start condition warning packet only if it can be driven from
already-loaded runtime data and the live `CM_USE_ITEM` quest-start path.

Likely candidate to verify first:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback for abyss/rank start-condition failure.
- Java source method or runtime path to inspect: QuestService.startQuest/checkStartConditions warn=true -> rank-condition branch -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK.
- C# runtime artifact to verify before editing: NearbyQuestStartConditionFailure rank mapping, player abyss-rank runtime data, and packet parameter source.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes live client-visible packet output for an existing item-use runtime path.
```

If rank cannot be mapped from live runtime data without scaffolding, skip it and select another live packet, state,
persistence, runtime-loading, or handler path.
