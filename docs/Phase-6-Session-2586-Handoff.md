# Phase 6 Session 2586 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2586: Send quest-start item condition messages. See
[Phase-6-Session-2586-Completion.md](Phase-6-Session-2586-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- `5f98483` - `[Phase 6][UOW-2585] Send quest-start rejection messages`
- Current commit - `[Phase 6][UOW-2586] Send quest-start condition messages`

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

## Files Changed In UOW-2586

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2586-Completion.md`
- `docs/Phase-6-Session-2586-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.questEngine.QuestEngine#onDialog`
- `com.aionemu.gameserver.questEngine.QuestService#startQuest`
- `com.aionemu.gameserver.questEngine.QuestService#checkStartConditions`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestStartUseItemAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
```

Result: passed, 107/107.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet output changed, but the focused
filter built Aion.GameServer and directly covered the modified live handler packet branches plus adjacent
condition classification.

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

## Known Gaps

- Rank failure remains unmapped because the Java packet parameter depends on abyss-rank localization data not present in this narrow runtime path.
- Inventory, XML condition, combine-skill, quest-list capacity, and NPC-faction start-condition messages remain partial or silent in this immediate-start C# branch.
- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- Real client validation was not run.

## Remaining Risks

- The full Java dialog path may reorder or supplement messages once ported.
- Conditions not represented by `NearbyQuestStartConditionFailure` remain silent in the immediate item-start branch.
- No full generated system-message catalog parity audit was performed.

## Next Recommended Runtime UOW

**UOW-2587 candidate: wire a narrow live quest-start rejection for quest-list capacity.**

Only take this if the C# player quest collection exposes the real capacity rule and Java's `STR_QUEST_LIST_FULL`
packet mapping can be applied from live `CM_USE_ITEM` without scaffolding.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the player cannot accept more active quests.
- Java source method or runtime path: QuestService.startQuest/checkStartConditions warn=true quest-list capacity branch.
- C# runtime artifact to wire or fix: live quest-start condition service or GameServerConnection.HandleQuestStartUseItemAsync plus SmSystemMessage helper if the existing player quest collection has a real capacity signal.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends a real system-message packet without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

If that does not pass the gate, skip it and select another live packet/state/persistence branch.

## Safe Runtime Candidates

- Quest-list capacity warning packet for live quest-start item use, only if the existing runtime quest collection has a real capacity signal.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- Quest timer cancellation only after a real C# quest timer task owner exists.
- `QuestEngine.onItemRemoved` callback only after a concrete C# quest-handler execution surface exists.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_USE_ITEM` quest-start items now start missing/completed-repeatable quests and send Java rejection packets for active/non-repeatable quest states.
- Race, minimum-level, maximum-level, class, and gender start-condition failures now send Java-equivalent `SM_SYSTEM_MESSAGE` packets.
- New quest starts insert rows; completed repeat starts update rows.
- Rejected starts do not call repository persistence and do not mutate quest state.
- Remaining start-condition failures need either direct live runtime mapping or should be skipped if they would become scaffolding-only.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
