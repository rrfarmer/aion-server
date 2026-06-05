# Phase 6 Session 2585 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2585: Send quest-start item rejection messages. See
[Phase-6-Session-2585-Completion.md](Phase-6-Session-2585-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- `d5a7adf` - `[Phase 6][UOW-2581] Assign NPC faction daily quest`
- `d84e7b53b` - `[Phase 6][UOW-2582] Filter NPC faction daily handlers`
- `ed8eabf35` - `[Phase 6][UOW-2583] Persist quest work item deletes`
- `a767ee5cb` - `[Phase 6][UOW-2584] Wire quest-start item use`
- Current commit - `[Phase 6][UOW-2585] Send quest-start rejection messages`

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

## Files Changed In UOW-2585

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmSystemMessage.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/GameServerConnectionInventoryExpansionUseItemTests.cs`
- `docs/Phase-6-Session-2585-Completion.md`
- `docs/Phase-6-Session-2585-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.templates.item.actions.QuestStartAction#canAct`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE`
- `com.aionemu.gameserver.model.templates.QuestTemplate#getName`

## C# Artifacts Touched

- `Aion.GameServer.Network.Aion.GameServerConnection.HandleQuestStartUseItemAsync`
- `Aion.GameServer.Network.Aion.ServerPackets.SmSystemMessage`
- `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`
- `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor`
- `Aion.GameServer.Tests.GameServerConnectionInventoryExpansionUseItemTests`

## Validation

```text
dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
```

Result: passed, 102/102. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet output changed, but the focused
filter built Aion.GameServer and directly covered the modified live handler packet branches plus adjacent
repeat-condition classification.

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

## Known Gaps

- Other Java `QuestService.startQuest` warning messages for race, level, class, gender, rank, inventory, XML conditions, quest-list capacity, and NPC faction conditions remain partial or silent in this immediate-start C# path.
- Java `QuestStartAction.canAct` sends the same none-repeatable message for repeat count and repeat timing; C# follows that behavior for the currently modeled repeat failures.
- Full dialog acceptance remains unported; this is still a direct narrow item-start path.
- Real client validation was not run.

## Remaining Risks

- Quest-start item start-condition failures beyond active/non-repeatable remain silent or partial.
- The full Java dialog path may reorder or supplement messages once ported.
- Quest XML `name` is now loaded for this packet parameter, but no full quest-template parity audit was performed.

## Next Recommended Runtime UOW

**UOW-2586 candidate: wire a narrow subset of Java `QuestService.startQuest` warning messages for quest-start item start-condition failures.**

Start with race/min-level/class/gender only if the message helpers and `NearbyQuestStartConditionFailure` mapping can
be wired directly to real `SmSystemMessage` output from `HandleQuestStartUseItemAsync`.

Runtime progress gate to verify before editing:

```text
- Deferred/live behavior being advanced: quest-start item use should send Java feedback for failed non-repeat start conditions instead of silently returning.
- Java source method or runtime path: QuestStartAction.act -> QuestEngine.onDialog(ASK_QUEST_ACCEPT) -> QuestService.startQuest/checkStartConditions warn=true.
- C# runtime artifact to wire or fix: SmSystemMessage helpers if needed, NearbyQuestStartConditionFailure mapping, GameServerConnection.HandleQuestStartUseItemAsync failure branch.
- Client-visible/state/persistence effect expected: rejected quest-start item use sends real system-message packets without mutating or persisting quest state.
- Why this is not preview-only/test-only/documentation-only: it changes live client-visible packet output for an existing item-use runtime path.
```

Focused validation recipe:

```text
- Behavior/contract: quest-start item start-condition failures serialize the Java system-message ids and preserve no-mutation/no-persistence behavior.
- Focused C# command: dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~GameServerConnectionInventoryExpansionUseItemTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests" --no-restore
- Java/Maven: not expected unless a narrow Java fixture is introduced; source review of QuestService.checkStartConditions is expected.
- Broad-validation trigger: live packet output changes; start focused and document whether broader .NET validation is skipped after focused evidence.
```

If that mapping cannot be done directly from current condition results, skip it and choose another live packet/state/persistence branch.

## Safe Runtime Candidates

- Quest-start item start-condition system-message packets for condition failures with direct C# condition mappings.
- A narrow `QuestEngine.onItemUseEvent` scripted-handler path only after a concrete C# quest-handler execution surface can be used live.
- Quest timer cancellation only after a real C# quest timer task owner exists.
- `QuestEngine.onItemRemoved` callback only after a concrete C# quest-handler execution surface exists.
- Another live packet path with existing state/repository packet surfaces, selected from current `GameServerConnection` deferred comments.

## Context Needed By Next Session

- `CM_USE_ITEM` quest-start items now start missing/completed-repeatable quests and send Java rejection packets for active/non-repeatable quest states.
- New quest starts insert rows; completed repeat starts update rows.
- Rejected active/non-repeatable starts do not call repository persistence and do not mutate quest state.
- `NearbyQuestTemplateSummary.Name` is loaded from quest XML solely for live quest-start messaging so far.
- Remaining start-condition failures still need Java warning packet mapping or should be skipped if the mapping would become scaffolding-only.
- Avoid preview/metadata/evidence-only work. If the next candidate cannot wire live behavior, re-plan from Java source and current live runtime gaps.
