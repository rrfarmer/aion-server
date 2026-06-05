# Phase 6 Session 2584 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2584: Wire quest-start item use. See
[Phase-6-Session-2584-Completion.md](Phase-6-Session-2584-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- Current commit - `[Phase 6][UOW-2584] Wire quest-start item use`

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

## Files Changed In UOW-2584

- `dotnetConversion/src/Aion.GameServer/Data/PlayerEnterWorldRepository.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/ItemTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/StaticData.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `docs/Phase-6-Session-2584-Completion.md`
- `docs/Phase-6-Session-2584-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.network.aion.clientpackets.CM_USE_ITEM#runImpl`
- `com.aionemu.gameserver.model.templates.item.actions.QuestStartAction#canAct`
- `com.aionemu.gameserver.model.templates.item.actions.QuestStartAction#act`
- `com.aionemu.gameserver.services.QuestService#startQuest`
- `com.aionemu.gameserver.dao.PlayerQuestListDAO#addQuests`
- `com.aionemu.gameserver.dao.PlayerQuestListDAO#updateQuests`

## C# Artifacts Touched

- `Aion.GameServer.Dataholders.StaticData` item action extraction
- `Aion.GameServer.Dataholders.ItemTemplateSummary.QuestStartQuestId`
- `Aion.GameServer.Network.Aion.GameServerConnection.HandleUseItemAsync`
- `Aion.GameServer.Services.PlayerEnterWorldService.PersistQuestStartAsync`
- `Aion.GameServer.Data.IPlayerEnterWorldRepository.InsertPlayerQuestAsync`
- `Aion.GameServer.Data.PlayerEnterWorldRepository.InsertPlayerQuestAsync`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 150/150. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet dispatch, quest state mutation, and
persistence changed, but the focused filter built Aion.GameServer and directly covered the modified item-use runtime
path plus the player-enter-world persistence interface seam.

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

## Known Gaps

- The broader Java `QuestEngine.onItemUseEvent` scripted-handler path remains unported for item use.
- Java dialog acceptance is represented by the immediate direct start path for this narrow action; full dialog-window negotiation is not implemented.
- Java system-message feedback for already-working or non-repeatable completed quests is not emitted yet.
- NPC-faction-specific `QuestService.startQuest` side effects are still partial for this item-start path.
- Real client and live MySQL integration were not run.

## Remaining Risks

- Quest-start item action ordering is represented as a dedicated C# branch; Java can iterate multiple actions if present.
- The immediate start path does not yet model every Java `QuestService.startQuest` warning or quest-list capacity branch.
- Live DB insert/update behavior was not integration-tested against MySQL.

## Next Recommended Runtime UOW

**UOW-2585 candidate: add Java-equivalent rejection feedback for live quest-start item use, only if the exact system-message packet surface can be wired to real output.**

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback when the quest is already working or completed non-repeatable instead of silently returning.
- Java source method or runtime path: QuestStartAction.canAct -> SM_SYSTEM_MESSAGE.STR_QUEST_ACQUIRE_ERROR_WORKING_QUEST / STR_QUEST_ACQUIRE_ERROR_NONE_REPEATABLE.
- C# runtime artifact to wire or fix: SmSystemMessage helpers if present, GameServerConnection.HandleQuestStartUseItemAsync rejection branches.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends real system-message packets without mutating quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an already-wired item-use packet branch.
```

If that candidate fails because the system-message surface is missing and the work would be adapter-only or
metadata-only, skip it and re-plan from the safe runtime candidates below.

## Safe Runtime Candidates

- Quest-start item rejection system-message packets if existing message ids/helpers can be confirmed and wired directly.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- Quest timer cancellation only after a real C# quest timer task owner exists.
- `QuestEngine.onItemRemoved` callback only after a concrete C# quest-handler execution surface exists.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_USE_ITEM` now handles item template `<queststart questid="..."/>` for eligible missing/completed-repeatable quest states.
- New quest starts call `InsertPlayerQuestAsync`; completed repeat starts call `UpdatePlayerQuestAsync`.
- `SM_QUEST_ACTION.ADD` is used for both missing-state starts and completed-repeatable restarts, matching Java `QuestService.startQuest`.
- Repeat-start retains existing quest vars/flags/complete count/reward/complete time because Java `QuestState.setStatus(START)` does not reset them.
- No Java rejection system messages are emitted yet for already-working or non-repeatable quest states.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
