# Phase 6 Session 2588 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2588: Send quest-start inventory-item warning. See
[Phase-6-Session-2588-Completion.md](Phase-6-Session-2588-Completion.md).

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
- Current commit - `[Phase 6][UOW-2588] Send quest-start inventory-item warning`

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

## Files Changed In UOW-2588

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2588-Completion.md`
- `docs/Phase-6-Session-2588-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#inventoryItemCheck`
- `com.aionemu.gameserver.model.templates.QuestTemplate#getInventoryItems`
- `com.aionemu.gameserver.model.templates.item.ItemTemplate#getL10n`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestStartUseItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateInventoryItemStartConditionFailureMessage`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary.InventoryItems`
- `Aion.GameServer.Dataholders.ItemTemplateSummary.GetClientName`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests" --no-restore
```

Result: passed, 99/99. Existing nullable/analyzer warnings were emitted; no new failing tests.

```text
git diff --check
```

Result: passed; only CRLF conversion warnings were emitted.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet output changed, but the focused
filter built Aion.GameServer and directly covered the modified live handler packet branch plus XML-loaded inventory
requirements and item-template client-name lookup.

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

## Known Gaps

- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- XML condition, combine-skill, rank, and NPC-faction warning messages remain partial or silent in this immediate-start C# branch.
- Missing required item-template behavior is softer in C# than Java's assumed data-manager lookup.
- Real client validation was not run.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- Collect-item count/removal checks remain separate from this start-condition warning path.
- Broader item-template L10n parity was not audited.

## Next Recommended Runtime UOW

**UOW-2589 candidate: wire the Java combine-skill quest-start warning packet for live quest-start item use.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the player lacks required combine/crafting skill rank.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> checkCombineSkill -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_TS_RANK.
- C# runtime artifact to wire or fix: NearbyQuestStartConditionFailure.CombineSkill mapping, SmSystemMessage helper, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

Focused validation recipe:

```text
- Behavior/contract: combine-skill start-condition failure serializes Java system-message output and preserves no-mutation/no-persistence behavior.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
- Java/Maven: not expected unless a narrow Java fixture is introduced; source review of QuestService.checkCombineSkill and SM_SYSTEM_MESSAGE is expected.
- Broad-validation trigger: live packet output changes; start focused and document whether broader .NET validation is skipped after focused evidence.
```

## Safe Runtime Candidates

- Combine-skill quest-start warning packet, if the Java `STR_QUEST_ACQUIRE_ERROR_TS_RANK` parameter maps directly from loaded quest XML.
- Rank quest-start warning packet, only after abyss-rank localization can be mapped without scaffolding.
- XML start-condition warning packets only for already-supported XML condition elements whose packet parameters can be sourced from live runtime data.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_USE_ITEM` quest-start items now start missing/completed-repeatable quests and send Java rejection packets for active/non-repeatable quest states.
- Race, minimum-level, maximum-level, class, gender, full-normal-quest-list, and missing-inventory-item start-condition failures now send Java-equivalent `SM_SYSTEM_MESSAGE` packets.
- The normal quest cap uses Java config `gameserver.basic.questsize.limit`, counts active unlocked `QUEST` rows, and is bypassed for membership `gameserver.quest.limit.disable` and no-count categories.
- New quest starts insert rows; completed repeat starts update rows.
- Rejected starts do not call repository persistence and do not mutate quest state.
- Remaining start-condition failures need either direct live runtime mapping or should be skipped if they would become scaffolding-only.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
