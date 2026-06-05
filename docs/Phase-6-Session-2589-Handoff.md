# Phase 6 Session 2589 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2589: Send quest-start combine-skill warning. See
[Phase-6-Session-2589-Completion.md](Phase-6-Session-2589-Completion.md).

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
- Current commit - `[Phase 6][UOW-2589] Send quest-start combine-skill warning`

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

## Files Changed In UOW-2589

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2589-Completion.md`
- `docs/Phase-6-Session-2589-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#checkCombineSkill`
- `com.aionemu.gameserver.model.templates.QuestTemplate#getCombineSkill`
- `com.aionemu.gameserver.model.templates.QuestTemplate#getCombineSkillPoint`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestStartUseItemAsync`
- `Aion.GameServer.Network.Aion.GameServerConnection.CreateQuestStartConditionFailureMessage`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Services.NearbyQuestStartConditionService`
- `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary.CombineSkill`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~HandleUseItemAsync_QuestStartItemSendsCombineSkillConditionFailureMessage|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
```

Result: passed, 13/13.

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestStartItem|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
```

Result: passed, 26/26.

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
```

Result: failed, 111/112. The failing test was unrelated to this UOW:
`ProcessPacketAsync_CompositeStonesMergesRewardWithoutCubeUpdate` expected cleanup seal flag `0` but saw `3` on a
composition-stone reward merge.

```text
git diff --check
```

Result: passed; only CRLF conversion warnings were emitted.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: not expanded after the unrelated broad-class failure. The passing quest-start filters built
`Aion.GameServer` and directly covered the modified live handler packet branch, XML-loaded combine-skill metadata,
existing Java-equivalent combine-skill condition rules, packet serialization, and no-mutation/no-persistence behavior.

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

## Known Gaps

- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- Rank, XML-condition, and NPC-faction warning messages remain partial or silent in this immediate-start C# branch.
- Full generated `SM_SYSTEM_MESSAGE` catalog parity remains partial.
- Real client validation was not run.

## Remaining Risks

- The full Java dialog path may add or reorder side effects once ported.
- The unrelated composition-stone cleanup-flag test failure remains outside this UOW.
- Broader combine-skill behavior depends on the existing condition-service implementation and loaded player skill state.

## Next Recommended Runtime UOW

**UOW-2590 candidate: verify and wire the next Java quest-start condition warning packet for live quest-start item use.**

Start with rank only if it passes the runtime gate:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback for abyss/rank start-condition failure.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true -> rank condition branch -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_MIN_RANK.
- C# runtime artifact to wire or fix: NearbyQuestStartConditionFailure rank mapping, SmSystemMessage helper, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only if feasible: it changes live client-visible packet output for an existing item-use runtime path.
```

Before editing, verify that C# already has live player abyss-rank data and a Java-equivalent parameter source for the
rank message. If the rank message cannot be mapped without scaffolding, skip it.

## Safe Runtime Candidates

- Rank quest-start warning packet, only after abyss-rank localization/parameter mapping can be sourced from live runtime data.
- XML start-condition warning packets only for already-supported XML condition elements whose packet parameters can be sourced from live runtime data.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_USE_ITEM` quest-start items now start missing/completed-repeatable quests and send Java rejection packets for active/non-repeatable quest states.
- Race, minimum-level, maximum-level, class, gender, full-normal-quest-list, missing-inventory-item, and combine-skill start-condition failures now send Java-equivalent `SM_SYSTEM_MESSAGE` packets.
- The normal quest cap uses Java config `gameserver.basic.questsize.limit`, counts active unlocked `QUEST` rows, and is bypassed for membership `gameserver.quest.limit.disable` and no-count categories.
- New quest starts insert rows; completed repeat starts update rows.
- Rejected starts do not call repository persistence and do not mutate quest state.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
