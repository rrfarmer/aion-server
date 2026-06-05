# Phase 6 Session 2579 Completion

## UOW

[Phase 6] UOW-2579: Persist NPC-faction abort state during quest abandon

## Status

Completed and validated with the focused quest-abandon/player-enter-world service filter. The live `CM_DELETE_QUEST`
path now persists the NPC-faction row changed by `QuestAbandonService.Abandon` when Java `NpcFactions.abortQuest`
resets an active faction quest to `NOTING`.

## Runtime Progress Gate

```text
Runtime progress gate:
- Deferred/live behavior being advanced: QuestService.abandonQuest -> player.getNpcFactions().abortQuest(template) state reset persisted through player_npc_factions.
- Java source method or runtime path: QuestService.abandonQuest, NpcFactions.abortQuest, NpcFaction.setState, PlayerService.storePlayer, PlayerNpcFactionsDAO.storeNpcFactions/updateNpcFaction.
- C# runtime artifact wired or fixed: PlayerEnterWorldRepository.UpdatePlayerNpcFactionAsync, PlayerEnterWorldService.PersistQuestAbandonAsync, live GameServerConnection.HandleDeleteQuestAsync persistence branch already invoking that service.
- Client-visible/state/persistence effect changed: abandoning an active NPC-faction quest now updates player_npc_factions to preserve the active faction row with state NOTING, time, quest id, and active flag across reloads.
- Why this is not preview-only/test-only/documentation-only: it writes live abandon state to the existing player_npc_factions database table from the live CM_DELETE_QUEST path.
```

## Java Source Reviewed

- `QuestService.abandonQuest(Player, int)`:
  - calls `player.getNpcFactions().abortQuest(template)` when the quest template has an NPC-faction id.
- `NpcFactions.abortQuest(QuestTemplate)`:
  - looks up the exact faction id;
  - returns without mutation if the row is missing or inactive;
  - sets the active faction state to `ENpcFactionQuestState.NOTING`;
  - calls `sendDailyQuest()`.
- `NpcFaction.setState(ENpcFactionQuestState)`:
  - marks existing loaded rows as `UPDATE_REQUIRED`.
- `PlayerNpcFactionsDAO.updateNpcFaction`:
  - writes `active`, `time`, `state`, and `quest_id` by `player_id` and `faction_id`.

## C# Changes

- Extended `IPlayerEnterWorldRepository` with `UpdatePlayerNpcFactionAsync`.
- Added MySQL `player_npc_factions` update SQL matching Java `PlayerNpcFactionsDAO.UPDATE_QUERY`.
- Added Java enum-name mapping for C# `PlayerNpcFactionQuestState` values (`NOTING`, `START`, `COMPLETE`).
- Extended `PlayerEnterWorldService.PersistQuestAbandonAsync` so it persists both quest-state and NPC-faction abort mutations from the same live `QuestAbandonResult`.
- Added a focused service test proving an active faction quest abandon persists the aborted faction row with `NOTING`.

## Known Gaps

- Java `NpcFactions.sendDailyQuest()` after abort remains missing; this UOW persists the abort state but does not select/send the replacement daily quest packet.
- Java persists faction dirty state during player save/logout; C# persists immediately from live abandon as an incremental partial-parity step.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Validation Decision

```text
Validation decision:
- Changed surface: production player_npc_factions repository write, PlayerEnterWorldService live abandon persistence, focused tests.
- Specific behavior/contract: active NPC-faction quest abandon persists the Java NOTING state while preserving active flag, mentor flag, time, and assigned quest id.
- Focused C# command: dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~PlayerEnterWorldServiceTests|FullyQualifiedName~QuestAbandonServiceTests" --no-restore -> 74/74 passed.
- Focused Java/Maven command: none; no narrow Java fixture exists, and behavior was taken from reviewed Java source.
- Broad-validation trigger: live persistence and CM_DELETE_QUEST handler-side effect path changed through an existing live service call.
- Broad .NET decision: skipped after focused pass; the filtered command built Aion.GameServer and directly covered the live abandon mutation plus persistence service contract.
- Why this scope is sufficient: tests run the real QuestAbandonService result used by the live handler and assert the repository-facing NPC-faction row update; SQL follows the reviewed Java DAO update shape.
```

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `QuestService.abandonQuest` NPC-faction branch | `QuestAbandonService.Abandon` / `PlayerEnterWorldService.PersistQuestAbandonAsync` | Service/state/persistence | Partial | Unit Tested | Partial Parity | Active faction state reset and persistence are live; `sendDailyQuest()` replacement packet remains missing. |
| `NpcFactions.abortQuest` | `PlayerNpcFactionsSnapshot.AbortQuest` | State | Partial | Unit Tested | Partial Parity | Exact active faction row resets to `Noting`; missing row and inactive row guards are covered in abandon tests. |
| `PlayerNpcFactionsDAO.updateNpcFaction` | `MySqlPlayerEnterWorldRepository.UpdatePlayerNpcFactionAsync` | Persistence | Partial | Unit Tested via service fake | Partial Parity | Uses existing table shape and Java state names; no live DB integration test was run. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `PersistQuestAbandon_UpdatesAbortedNpcFactionRow` | Unit | `NpcFactions.abortQuest` + `PlayerNpcFactionsDAO.updateNpcFaction` | Active faction quest abandon persists the aborted faction as active `NOTING` with original time/quest id | C# uses the real `QuestAbandonService.Abandon` result | No MySQL fixture; replacement daily quest packet not covered. |

## Summary Metrics

- Focused validation: 74 tests passed.
- Live abandon path now mutates and persists quest state and NPC-faction abort state, and mutates/sends work-item and work-order recipe side effects.
- Overall Phase 6 completion estimate: incremental.

## Remaining Risks

- Full real-client abandon flow has not been run.
- No database integration test was added for `player_npc_factions`; SQL was matched by source review against Java DAO shape.
- Immediate C# persistence is a practical partial-parity step until a Java-like dirty faction save pipeline exists.

## Next Runtime Candidate

UOW-2580: Send the replacement daily quest packet after NPC-faction abort.

Runtime progress gate:

```text
- Deferred/live behavior being advanced: NpcFactions.abortQuest -> sendDailyQuest -> PacketSendUtility.sendPacket(owner, new SM_QUEST_ACTION(questId)).
- Java source method or runtime path: NpcFactions.abortQuest, NpcFactions.sendDailyQuest, QuestsData.getQuestsByNpcFaction, QuestService.checkStartConditions.
- C# runtime artifact to wire or fix: runtime quest template lookup by NPC faction, Java-equivalent daily quest selection/update of PlayerNpcFactionsSnapshot, GameServerConnection.HandleDeleteQuestAsync packet send.
- Client-visible/state/persistence effect expected: abandoning an NPC-faction quest can assign or reuse a daily faction quest and send a real SmQuestAction ADD packet to the client.
- Why this is not preview-only/test-only/documentation-only: it mutates live NPC-faction quest assignment state and sends a real server packet from the live CM_DELETE_QUEST path.
```

Risks for UOW-2580:

- Java filters out time-based faction quests and requires `QuestEngine.isHaveHandler`.
- Java uses `QuestService.checkStartConditions(player, questId, false)` before selecting candidates.
- Java randomizes among eligible faction quests; C# needs a deterministic test seam without changing runtime behavior.
