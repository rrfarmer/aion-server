# Phase 6 Session 2590 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2590: Send quest-start rank warning. See
[Phase-6-Session-2590-Completion.md](Phase-6-Session-2590-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- `5f98483` - `[Phase 6][UOW-2585] Send quest-start rejection messages`
- `84c0f0b` - `[Phase 6][UOW-2586] Send quest-start condition messages`
- `d92efac` - `[Phase 6][UOW-2587] Reject quest-start items at normal quest cap`
- `7c7c3e3` - `[Phase 6][UOW-2588] Send quest-start inventory-item warning`
- `cd9ecd7` - `[Phase 6][UOW-2589] Send quest-start combine-skill warning`
- Current commit - `[Phase 6][UOW-2590] Send quest-start rank warning`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.
- UOW-2578 persisted the live quest abandon mutation to `player_quests` delete/update rows.
- UOW-2579 persisted the live NPC-faction abort mutation to `player_npc_factions`.
- UOW-2580 sent Java `SM_QUEST_ACTION(int questId)` for the reusable assigned NPC-faction daily quest branch after abort.
- UOW-2581 supported Java's random NPC-faction daily replacement branch after abort, including assignment state mutation, packet send, and persistence.
- UOW-2582 loaded quest-handler availability into runtime static data and wired it into the live random NPC-faction daily selector.
- UOW-2583 persisted live quest work-item inventory deletions through the existing inventory repository delete path.
- UOW-2584 loaded Java `queststart` item actions and wired live `CM_USE_ITEM` quest-start state/persistence/packet effects.
- UOW-2585 added Java-equivalent client feedback for active and non-repeatable quest-start item rejections.
- UOW-2586 added Java-equivalent client feedback for race, minimum-level, maximum-level, class, and gender quest-start condition failures.
- UOW-2587 added Java-equivalent client feedback for full normal quest lists, including membership and no-count bypass behavior.
- UOW-2588 added Java-equivalent client feedback for missing required inventory items.
- UOW-2589 added Java-equivalent client feedback for missing combine/crafting skill rank.
- UOW-2590 added Java-equivalent client feedback for missing abyss rank.

## Files Changed In UOW-2590

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2590-Completion.md`
- `docs/Phase-6-Session-2590-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#checkStartConditions`
- `com.aionemu.gameserver.model.templates.QuestTemplate#getRequiredRank`
- `com.aionemu.gameserver.utils.stats.AbyssRankEnum#getRankL10n`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestStartUseItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateQuestStartConditionFailureMessage`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Model.GameObjects.PlayerAbyssRank.GetRankL10n`
- `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary.RequiredRank`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_QuestStartItemSendsRankStartConditionFailureMessage|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~AbyssRankDataServiceTests" --no-restore
```

Result: passed, 22/22.

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestStartItem|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~AbyssRankDataServiceTests" --no-restore
```

Result: passed, 36/36.

```text
git diff --check
```

Result: passed; only CRLF conversion warnings were emitted.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused and wider quest-start passes. The filters built `Aion.GameServer` and directly
covered the modified live handler packet branch, XML-loaded rank metadata, existing rank condition rules,
abyss-rank l10n helpers, packet serialization, and no-mutation/no-persistence behavior.

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

## Known Gaps

- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- XML-condition and NPC-faction warning messages remain partial or silent in this immediate-start C# branch.
- Full generated `SM_SYSTEM_MESSAGE` catalog parity remains partial.
- Real client validation was not run.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- The unrelated composition-stone cleanup-flag test failure observed in UOW-2589's broad class filter remains outside this UOW.
- Broader generated-message and client-localization parity remains partial.

## Blocked Candidate Checked After UOW-2590

XML start-condition warning packets do not currently pass the Runtime Progress Gate.

- Java emits warning packets inside concrete `XMLStartCondition.check(player, warn)` implementations.
- C# currently collapses evaluated XML failures into `NearbyQuestStartConditionFailure.XmlStartConditions`.
- The collapsed result does not preserve which concrete XML child failed or the Java packet parameter source.
- Wiring a packet from the generic failure would be guesswork, and adding a detail-only adapter first would be scaffolding-only.

NPC-faction quest-start failure also does not look like a packet candidate from `QuestService`: the Java path returns
`false` silently for the daily-limit/faction-active gates after condition checks.

## Next Recommended Runtime UOW

**UOW-2591 candidate: leave the quest-start warning packet series and select another deferred live packet/state path.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: a currently deferred client packet path should either send a real server packet or mutate/persist live runtime state.
- Java source method or runtime path: start from a concrete `CM_*` Java client packet and its service method, not from docs or preview data.
- C# runtime artifact to wire or fix: the matching `GameServerConnection` deferred case plus the existing service/repository/world artifact needed for live behavior.
- Client-visible/state/persistence effect expected: packet output, player/world/group/inventory/quest state mutation, or persistence must change.
- Why this is not preview-only/test-only/documentation-only if feasible: the selected branch must execute from live packet handling.
```

First discovery target: a narrow deferred `GameServerConnection` packet path with existing C# state surfaces, such as
`CM_GROUP_LOOT` only if drop distribution state and packet fanout already exist enough to wire one Java-equivalent
roll/bid branch. If that does not pass the gate, continue scanning deferred packet paths for the smallest live
state/packet/persistence UOW.

## Safe Runtime Candidates

- `CM_GROUP_LOOT` roll/bid branch, only if current C# drop distribution state can be mutated and packetized from live code.
- Another deferred `GameServerConnection` packet path with an existing service/repository/world surface that can be wired without adapters.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- XML start-condition warning packets only after the live condition result carries concrete Java-equivalent failure detail as part of a runtime UOW.

## Context Needed By Next Session

- `CM_USE_ITEM` quest-start items now start missing/completed-repeatable quests and send Java rejection packets for active/non-repeatable quest states.
- Race, minimum-level, maximum-level, class, gender, full-normal-quest-list, missing-inventory-item, combine-skill, and rank start-condition failures now send Java-equivalent `SM_SYSTEM_MESSAGE` packets.
- The normal quest cap uses Java config `gameserver.basic.questsize.limit`, counts active unlocked `QUEST` rows, and is bypassed for membership `gameserver.quest.limit.disable` and no-count categories.
- New quest starts insert rows; completed repeat starts update rows.
- Rejected starts do not call repository persistence and do not mutate quest state.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
