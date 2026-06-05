# Phase 6 Session 2580 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2580: Re-send assigned NPC-faction daily quest after abandon. See
[Phase-6-Session-2580-Completion.md](Phase-6-Session-2580-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- Current commit - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.
- UOW-2578 persisted the live quest abandon mutation to `player_quests` delete/update rows.
- UOW-2579 persisted the live NPC-faction abort mutation to `player_npc_factions`.
- UOW-2580 sends Java `SM_QUEST_ACTION(int questId)` for the reusable assigned NPC-faction daily quest branch after abort.

## Files Changed In UOW-2580

- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerNpcFactionState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestAction.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/CompleteAscensionQuestPlanServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerNpcFactionsSnapshotTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `docs/Phase-6-Session-2580-Completion.md`
- `docs/Phase-6-Session-2580-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.services.QuestService#abandonQuest`
- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#abortQuest`
- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#sendDailyQuest`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION#SM_QUEST_ACTION(int)`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION.ActionType#UNK`

## C# Artifacts Touched

- `PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds`
- `QuestAbandonResult.NpcFactionDailyQuestPackets`
- `QuestAbandonService.Abandon`
- `GameServerConnection.HandleDeleteQuestAsync`
- `SmQuestAction.Unknown`
- `QuestAbandonServiceTests`
- `PlayerNpcFactionsSnapshotTests`
- `CompleteAscensionQuestPlanServiceTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerNpcFactionsSnapshotTests|FullyQualifiedName~CompleteAscensionQuestPlanServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 97/97. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet send and packet serialization changed,
but the focused filter built Aion.GameServer and directly covered the edited packet, NPC-faction state query, abandon
service result, and persistence-adjacent consumer.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit/Manual | Partial Parity | Live dispatch wired; timer-clear, reusable NPC-faction daily packet, work-item delete, NPC-faction abort, work-order recipe delete, quest/faction persistence, and abandon packet ordering implemented; task-map cancellation and random daily replacement still missing. |
| `NpcFactions.sendDailyQuest` reusable branch | `PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds` / `QuestAbandonService.Abandon` | State/packet selection | Partial | Unit Tested | Partial Parity | Reuses future assigned quest ids for active NOTING factions; random replacement selection remains missing. |
| `SM_QUEST_ACTION(int questId)` | `SmQuestAction.Unknown` | Packet | Partial | Unit Tested | Partial Parity | Action id 6 payload covered by byte assertion; extra-category suppression still uses the existing C# flag rather than runtime template lookup. |

## Known Gaps

- Random NPC-faction daily replacement selection remains missing for `questId == 0`.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java persists quest/faction dirty state during player save/logout; C# currently persists immediately from live abandon as an incremental partial-parity step.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- The current UOW does not add a full `CM_DELETE_QUEST` socket capture; packet ordering is wired in the live handler and packet bytes are covered at service/packet level.
- Random daily selection must respect Java `QuestEngine.isHaveHandler` and `QuestService.checkStartConditions`; do not replace it with a simple first-by-faction lookup.

## Next Recommended Runtime UOW

**UOW-2581: Implement random NPC-faction daily quest replacement after abort.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: NpcFactions.sendDailyQuest questId == 0 branch -> QuestsData.getQuestsByNpcFaction -> Rnd.get -> faction.setQuestId/time/state -> SM_QUEST_ACTION(questId).
- Java source method or runtime path: NpcFactions.sendDailyQuest, QuestsData.afterUnmarshal, QuestsData.getQuestsByNpcFaction, QuestEngine.isHaveHandler, QuestService.checkStartConditions.
- C# runtime artifact to wire or fix: NearbyQuestTemplateTable NPC-faction index, handler-availability predicate if present, random candidate selection seam, PlayerNpcFactionsSnapshot assignment mutation, QuestAbandonService/GameServerConnection packet output and persistence.
- Client-visible/state/persistence effect expected: when no reusable assigned quest exists, abandoning an NPC-faction quest can assign a new eligible daily quest, persist that assignment, and send action id 6.
- Why this is not preview-only/test-only/documentation-only: it mutates live NPC-faction assignment state, persists it, and sends a real server packet from live abandon code.
```

Java artifacts to inspect:

- `NpcFactions.sendDailyQuest`
- `QuestsData.afterUnmarshal`
- `QuestsData.getQuestsByNpcFaction`
- `QuestEngine.isHaveHandler`
- `QuestService.checkStartConditions`
- `Rnd.get`

C# artifacts likely involved:

- `NearbyQuestTemplateTable`
- `NearbyQuestStartConditionService`
- dynamic quest handler registry/availability surface if present
- `PlayerNpcFactionsSnapshot`
- `NpcFactionDailyResetService`
- `QuestAbandonService`
- `PlayerEnterWorldService.PersistQuestAbandonAsync`
- `SmQuestAction.Unknown`

Focused validation recipe:

- Behavior/contract: expired/zero assigned NPC-faction abort selects an eligible non-time-based faction quest, updates the player faction assignment/time/state, persists it, and emits `SmQuestAction.Unknown`.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerNpcFactionsSnapshotTests|FullyQualifiedName~NearbyQuestTemplate|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore`; narrow further to edited tests if slow.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of the methods listed above is expected.
- Broad-validation trigger: live packet send, state mutation, persistence, and static-data indexing likely change; start focused and document whether broader .NET validation is skipped after focused evidence.

## Safe Runtime Candidates

- Implement the random NPC-faction daily quest replacement branch as above, if handler availability can be scoped safely.
- Timer task cancellation can become a runtime UOW only after discovery identifies or implements a real quest timer scheduler/task-owner path. Current C# code has scheduler utilities and a bind-point `SKILL_USE` task owner, but no live `QUEST_TIMER` slot has been found.
- Persist quest work-item deletion through the existing inventory item delete persistence path if not already covered by `TrackDeletedItem` plus logout persistence.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, timer-clear packet, reusable NPC-faction daily packet, NPC-faction abort, work-item inventory cleanup, work-order recipe delete, quest/faction persistence, and final abandon packet.
- `SmQuestAction.Unknown` is Java `SM_QUEST_ACTION(int questId)`, not the normal quest-state ADD packet.
- `PlayerNpcFactionsSnapshot.GetReusableDailyQuestIds` intentionally covers only the future-assigned `NOTING` branch; random replacement remains pending.
- Avoid preview/metadata/evidence-only work; the next UOW should mutate or send the next real abandon-path effect.
