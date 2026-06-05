# Phase 6 Session 2590 Completion

## UOW

[Phase 6] UOW-2590: Send quest-start rank warning

## Status

Completed and validated with focused live handler coverage. The C# `CM_USE_ITEM` quest-start branch now sends Java
`SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK` when a quest requires a higher abyss rank than the player has.
The rejected start still does not mutate `player.Quests`, does not persist quest rows, and does not send the
quest-start animation or `SM_QUEST_ACTION`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: quest-start item use now sends Java feedback when the player lacks the required abyss rank.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> required-rank branch -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK.
- C# runtime artifact wired or fixed: GameServerConnection.HandleQuestStartUseItemAsync condition-failure mapping and SmSystemMessage helper.
- Client-visible/state/persistence effect changed: rejected CM_USE_ITEM quest-start attempts now send real SM_SYSTEM_MESSAGE id 1300573 with the Java abyss-rank l10n parameter while preserving no-mutation/no-persistence behavior.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output from an already-wired item-use packet path.
```

## Java Source Reviewed

- `QuestService.checkStartConditions` checks `template.getRequiredRank() != 0`.
- Java compares `player.getAbyssRank().getRank().getId() < template.getRequiredRank()`.
- When warning is enabled, Java sends
  `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK(AbyssRankEnum.getRankL10n(player.getRace(), template.getRequiredRank()))`.
- `AbyssRankEnum.getRankL10n(Race, int)` resolves the required rank enum and encodes `ChatUtil.l10n(baseRank9Id + ordinal)`.
- `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK(String)` uses message id `1300573`.

## C# Changes

- Added `SmSystemMessage.QuestAcquireErrorMinRank(string)` for message id `1300573`.
- Updated `GameServerConnection.CreateQuestStartConditionFailureMessage` to receive the live `Player` so rank failures
  can use `PlayerAbyssRank.GetRankL10n(player.Race, template.RequiredRank)`.
- Added a live use-item test with XML-loaded `rank` metadata and serialized rank-l10n packet assertion.

## Known Gaps

- Full dialog acceptance remains unported; this is still the direct narrow quest-start item path.
- The existing C# condition detector covers the rank comparison, but broader QuestService parity remains partial.
- XML-condition and NPC-faction warning messages remain partial or silent in this immediate-start branch.
- Real client validation was not run.

## Validation Decision

```text
Validation decision:
- Changed surface: live CM_USE_ITEM packet output for quest-start abyss-rank condition failures.
- Specific behavior/contract: insufficient abyss rank sends Java SM_SYSTEM_MESSAGE id 1300573 with the required-rank l10n parameter and does not mutate or persist quest state.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_QuestStartItemSendsRankStartConditionFailureMessage|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~AbyssRankDataServiceTests" --no-restore -> 22/22 passed.
- Wider quest-start C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestStartItem|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~AbyssRankDataServiceTests" --no-restore -> 36/36 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live packet output changed.
- Broad .NET decision: skipped after focused and wider quest-start passes; the filters built Aion.GameServer and directly exercised the modified live handler branch, XML-loaded rank metadata, existing rank condition rules, abyss-rank l10n helpers, packet serialization, and no-mutation/no-persistence assertions.
- Why this scope is sufficient: the passing tests drive the real HandleUseItemAsync quest-start path and the Java-equivalent rank l10n helper used as the packet parameter.
```

Additional hygiene:

```text
git diff --check -> passed; only CRLF conversion warnings were emitted.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.checkStartConditions` required-rank warning branch | `GameServerConnection.HandleQuestStartUseItemAsync` | Item action | Partial | Unit Tested | Partial Parity | Live quest-start item use now sends a packet for rank failure; broader QuestService/dialog parity remains partial. |
| `QuestTemplate.getRequiredRank` | `NearbyQuestTemplateSummary.RequiredRank` | Runtime data | Partial | Unit Tested | Partial Parity | Already-loaded quest XML `rank` values are used by live condition and packet paths. |
| `AbyssRankEnum.getRankL10n(Race, int)` | `PlayerAbyssRank.GetRankL10n` | Runtime data | Partial | Unit Tested | Partial Parity | Existing C# rank l10n helper supplies the Java-style packet parameter. |
| `SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK` | `SmSystemMessage.QuestAcquireErrorMinRank` | Packet helper | Complete | Unit Tested | Partial Parity | Message id `1300573` and parameter are serialized from a live handler test; broader generated-message catalog parity remains partial. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `HandleUseItemAsync_QuestStartItemSendsRankStartConditionFailureMessage` | Unit/live handler | `QuestService.checkStartConditions`, `SM_SYSTEM_MESSAGE`, `AbyssRankEnum` | Insufficient abyss rank sends `1300573` with required-rank l10n and does not persist quest changes | Source-reviewed Java + live C# handler assertion | Does not cover all rank ids/races in the live handler; existing abyss-rank l10n tests cover helper behavior. |

## Summary Metrics

- Focused UOW validation: 22 tests passed.
- Wider quest-start validation: 36 tests passed.
- Runtime progress: rank live quest-start rejections now produce Java-equivalent client-visible feedback.
- Total Java artifacts touched/discovered this UOW: 4.
- Total C# artifacts touched: 4.
- Artifacts with verified parity: 0.
- Artifacts needing verification / partial parity: 4.
- Blocked artifacts: 3 (`QuestEngine.onItemUseEvent` handlers, full dialog accept flow, XML/NPC-faction warning packets).
- Estimated overall Phase 6 completion: incremental; still far from replacement readiness.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- The unrelated composition-stone cleanup-flag test failure observed in UOW-2589's broad class filter remains outside this UOW.
- Real client behavior was not validated against a running Aion client.

## Next Runtime Candidate

UOW-2591 candidate: verify whether any already-supported XML start-condition failures can send Java-equivalent live
warning packets from existing runtime data.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback for a concrete XML start-condition failure already evaluated by C#.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> XMLStartCondition.check(player, warn) -> concrete SM_SYSTEM_MESSAGE branch.
- C# runtime artifact to wire or fix: NearbyQuestStartConditionFailure.XmlStartConditions detail source or another existing live condition artifact, SmSystemMessage helper, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes live client-visible packet output for an existing item-use runtime path.
```

If XML failure detail is not available from existing live runtime data, skip XML warnings and select another live
packet/state/persistence/runtime-loading path rather than adding scaffolding.
