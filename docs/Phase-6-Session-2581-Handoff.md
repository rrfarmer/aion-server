# Phase 6 Session 2581 Handoff

## Current Phase

Phase 6: Port Game Core

## Last Completed UOW

[Phase 6] UOW-2581: Assign random NPC-faction daily quest after abandon. See
[Phase-6-Session-2581-Completion.md](Phase-6-Session-2581-Completion.md).

## Commits Made

- `ca69019` - `[Phase 6][UOW-2578] Persist abandoned quest state`
- `559431a` - `[Phase 6][UOW-2579] Persist NPC faction abort state`
- `5aa6cdf` - `[Phase 6][UOW-2580] Re-send assigned NPC faction daily quest`
- Current commit - `[Phase 6][UOW-2581] Assign NPC faction daily quest`

## Session Summary

- UOW-2574 wired `CM_DELETE_QUEST` live for core quest-state mutation and ABANDON/TIMER packets.
- UOW-2575 removed Java quest work-item stacks from live inventory and sent item-delete/cube-size packets.
- UOW-2576 reset active matching NPC-faction quest state to `NOTING` during live abandon.
- UOW-2577 loaded Java work-order `recipe_id` data and wired live recipe delete/send into `CM_DELETE_QUEST`.
- UOW-2578 persisted the live quest abandon mutation to `player_quests` delete/update rows.
- UOW-2579 persisted the live NPC-faction abort mutation to `player_npc_factions`.
- UOW-2580 sent Java `SM_QUEST_ACTION(int questId)` for the reusable assigned NPC-faction daily quest branch after abort.
- UOW-2581 now supports Java's random NPC-faction daily replacement branch after abort, including assignment state mutation, packet send, and persistence.

## Files Changed In UOW-2581

- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerNpcFactionState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/src/Aion.GameServer/Services/PlayerEnterWorldService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/QuestAbandonService.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/NearbyQuestTemplateTableTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/PlayerEnterWorldServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestAbandonServiceTests.cs`
- `docs/Phase-6-Session-2581-Completion.md`
- `docs/Phase-6-Session-2581-Handoff.md`

## Java Artifacts Touched

- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#sendDailyQuest`
- `com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions#getNextTime`
- `com.aionemu.gameserver.dataholders.QuestsData#afterUnmarshal`
- `com.aionemu.gameserver.dataholders.QuestsData#getQuestsByNpcFaction`
- `com.aionemu.gameserver.questEngine.QuestEngine#isHaveHandler`
- `com.aionemu.gameserver.services.QuestService#checkStartConditions`
- `com.aionemu.commons.utils.Rnd#get`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION#SM_QUEST_ACTION(int)`

## C# Artifacts Touched

- `NearbyQuestTemplateTable.GetQuestsByNpcFaction`
- `PlayerNpcFactionsSnapshot.AssignDailyQuest`
- `QuestAbandonResult.NpcFactionPersistenceUpdates`
- `QuestAbandonService.Abandon`
- `GameServerConnection.HandleDeleteQuestAsync`
- `PlayerEnterWorldService.PersistQuestAbandonAsync`
- `NearbyQuestTemplateTableTests`
- `QuestAbandonServiceTests`
- `PlayerEnterWorldServiceTests`

## Validation

```text
dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~PlayerNpcFactionsSnapshotTests|FullyQualifiedName~NearbyQuestTemplateTableTests|FullyQualifiedName~NearbyQuestStartConditionServiceTests|FullyQualifiedName~PlayerEnterWorldServiceTests" --no-restore
```

Result: passed, 106/106. Existing nullable/analyzer warnings were emitted; no new failing tests.

Java/Maven: not run. No narrow Java fixture exists; behavior was verified by source review against Java methods listed
above.

Broad .NET: skipped after focused pass. Broad trigger exists because live packet send, state mutation, persistence, and
static-data indexing changed, but the focused filter built Aion.GameServer and directly covered the edited table,
state, packet, start-condition, abandon, and persistence contracts.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `CM_DELETE_QUEST.runImpl` | `GameServerConnection.HandleDeleteQuestAsync` | Handler | Partial | Unit/Manual | Partial Parity | Live dispatch wired; timer-clear, reusable/random NPC-faction daily packet, work-item delete, NPC-faction abort, work-order recipe delete, quest/faction persistence, and abandon packet ordering implemented; task-map cancellation still missing. |
| `NpcFactions.sendDailyQuest` random branch | `QuestAbandonService.Abandon` / `PlayerNpcFactionsSnapshot.AssignDailyQuest` | State/packet selection | Partial | Unit Tested | Partial Parity | Random assignment, packet send, and persistence are live; full parity still needs live `QuestEngine.isHaveHandler` wiring. |
| `QuestsData.afterUnmarshal` NPC-faction index | `NearbyQuestTemplateTable.GetQuestsByNpcFaction` | Runtime static-data index | Partial | Unit Tested | Partial Parity | Non-time-based faction indexing and load order are covered; handler/start-condition filtering remains outside the index like Java. |
| `QuestsData.getQuestsByNpcFaction` | `QuestAbandonService` candidate filter | Service | Partial | Unit Tested | Partial Parity | Applies start conditions and supports handler predicate; live handler availability currently defaults to available until a registry is wired. |
| `NpcFactions.getNextTime` | `NpcFactionDailyResetService.GetNextResetEpochSeconds` | Utility | Partial | Unit Tested | Partial Parity | Existing 09:00 boundary service is now wired into live abandon random assignment. |
| `PlayerNpcFactionsDAO.updateNpcFaction` after random assignment | `PlayerEnterWorldService.PersistQuestAbandonAsync` | Persistence | Partial | Unit Tested | Partial Parity | Persists final assigned faction row instead of only the intermediate abort row. |
| `SM_QUEST_ACTION(int questId)` | `SmQuestAction.Unknown` | Packet | Partial | Unit Tested | Partial Parity | Action id 6 payload covered by byte assertions for reusable and random daily branches. |

## Known Gaps

- Live `QuestEngine.isHaveHandler` equivalent is not wired; random daily selection currently treats static candidates as handler-available unless a predicate is passed in tests.
- Quest timer task-map cancellation remains missing; timer-clear packet output is live.
- Java persists quest/faction dirty state during player save/logout; C# currently persists immediately from live abandon as an incremental partial-parity step.
- Java `QuestEngine.onItemRemoved` callback from storage deletion remains missing.

## Remaining Risks

- Full real-client abandon flow has not been run.
- Random selection uses .NET `Random.Shared.Next`; no Java RNG sequence parity is claimed.
- The handler-availability gap can cause C# to assign a faction daily quest that Java would filter out if the handler is absent.

## Next Recommended Runtime UOW

**UOW-2582: Wire live quest-handler availability into NPC-faction random daily selection.**

Runtime progress gate:

```text
- Deferred/live behavior being advanced: QuestsData.getQuestsByNpcFaction filters random daily candidates through QuestEngine.isHaveHandler before assignment.
- Java source method or runtime path: QuestEngine.addQuestHandler, QuestEngine.isHaveHandler, QuestsData.getQuestsByNpcFaction, data/handlers/quest AbstractQuestHandler constructors/register calls.
- C# runtime artifact to wire or fix: runtime quest-handler availability table/source, GameServerRuntimeContext or StaticData exposure, GameServerConnection.HandleDeleteQuestAsync handler predicate passed into QuestAbandonService.
- Client-visible/state/persistence effect expected: NPC-faction random daily abandon selection will stop assigning/sending/persisting quests that have no loaded handler equivalent.
- Why this is not preview-only/test-only/documentation-only: the handler availability data will be consumed by the live abandon selector and will directly change whether packets/state/persistence occur.
```

Java artifacts to inspect:

- `QuestEngine.addQuestHandler`
- `QuestEngine.isHaveHandler`
- representative `data/handlers/quest/**/_*.java` constructors and `register()` methods
- existing C# quest handler extractor/source-loader artifacts

C# artifacts likely involved:

- `QuestNpcStartJavaHandlerExtractor` or a new narrowly scoped handler-id source
- `QuestNpcStartRegistrationSourceLoader` if it can safely provide handler ids
- `StaticData` or `GameServerRuntimeContext`
- `GameServerConnection.HandleDeleteQuestAsync`
- `QuestAbandonServiceTests`

Focused validation recipe:

- Behavior/contract: a runtime handler-availability table is loaded or exposed, and live abandon random daily selection refuses candidates whose quest id is not handler-available.
- Focused C# command: start with `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "FullyQualifiedName~QuestAbandonServiceTests|FullyQualifiedName~QuestNpcStartJavaHandlerExtractorTests|FullyQualifiedName~QuestNpcStartRegistrationSourceLoaderTests|FullyQualifiedName~NearbyQuestTemplateTableTests" --no-restore`; narrow further to edited tests if slow.
- Java/Maven: not expected unless a narrow Java fixture is added; source review of `QuestEngine` and representative handlers is expected.
- Broad-validation trigger: live packet/state/persistence selection behavior changes; start focused and document whether broader .NET validation is skipped after focused evidence.

## Safe Runtime Candidates

- Wire handler availability into random NPC-faction daily selection as above.
- Timer task cancellation can become a runtime UOW only after discovery identifies or implements a real quest timer scheduler/task-owner path.
- Persist quest work-item deletion through the existing inventory item delete persistence path if not already covered by `TrackDeletedItem` plus logout persistence.

## Context Needed By Next Session

- `CM_DELETE_QUEST` is live for core quest mutation, timer-clear packet, reusable/random NPC-faction daily packet, NPC-faction abort, work-item inventory cleanup, work-order recipe delete, quest/faction persistence, and final abandon packet.
- `SmQuestAction.Unknown` is Java `SM_QUEST_ACTION(int questId)`, not the normal quest-state ADD packet.
- Random daily replacement now mutates `Player.NpcFactions`, sends action id 6, and persists final faction state.
- The next runtime gap in this branch is the Java `QuestEngine.isHaveHandler` filter.
- Avoid preview/metadata/evidence-only work; the next UOW must wire handler availability into a live runtime selector or choose a different live runtime branch.
