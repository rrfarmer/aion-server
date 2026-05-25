# ItemPurification Nearby Quest Refresh Audit

Date: May 25, 2026
Unit of Work: UOW-980

## Purpose

This audit records the Java `PlayerController.updateNearbyQuests()` behavior that must exist before ItemPurification quest notifications can execute real nearby-quest refresh.

The Java project remains the source of truth. This document does not enable production `CM_ITEM_PURIFICATION` dispatch, does not invoke real quest handlers, and does not claim runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_NEARBY_QUESTS.java`
- `game-server/src/com/aionemu/gameserver/services/QuestService.java`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java`
- `game-server/src/com/aionemu/gameserver/world/MapRegion.java`
- `game-server/src/com/aionemu/gameserver/model/templates/quest/QuestNpc.java`
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationQuestMutationNotifier.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestUpdateItemTable.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestCompletedList.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcQuestDropService.cs`

## Java Behavior

`PlayerController.updateNearbyQuests()`:

1. Creates a `HashMap<Integer, Integer>` named `nearbyQuestList`.
2. Iterates `getOwner().getPosition().getMapRegion().getParent().getQuestIds()`.
3. For each quest id, calls `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)`.
4. When start conditions pass, stores `questId -> QuestService.getLevelRequirementDiff(questId, player level)`.
5. Sends `SM_NEARBY_QUESTS(nearbyQuestList)` to the owner.

`SM_NEARBY_QUESTS` wire behavior:

- Writes `C(0)`.
- Writes `H(-nearbyQuestList.size() & 0xFFFF)`.
- Iterates the map entries.
- If the stored level-difference value is greater than zero, sets bit `1 << 17` on the quest id before writing it.
- Writes each resulting quest id as `D`.

Quest id source:

- `WorldMapInstance` owns a concurrent `questIds` set.
- When an `Npc` is added to a world-map instance, Java asks `QuestEngine.getQuestNpc(npc template id)`.
- For each `QuestNpc.getOnQuestStart()` id, Java adds the quest id to the world instance set.
- If new ids were added, Java schedules a delayed 1500 ms task, guarded by a single pending-task field, that calls `player.getController().updateNearbyQuests()` for every player in the instance.
- `MapRegion.getParent()` returns the `WorldMapInstance` that owns the region and quest id set.

Other Java call sites also use `updateNearbyQuests`, including item get/remove via `QuestEngine`, quest abandon/start/complete flows, level changes, daily/weekly reset messages, title list changes, and skill-learning changes.

## C# Current Status

- UOW-977 exposes static `questUpdateItems` membership through `StaticData.QuestUpdateItems`.
- UOW-978 adds `ItemPurificationNearbyQuestRefreshPlan`, a no-op planner that filters projected ItemPurification item get/remove candidates through that membership.
- UOW-979 adds `IItemPurificationNearbyQuestRefreshDispatcher` and `NoOpItemPurificationNearbyQuestRefreshDispatcher`, an explicit no-op dispatch seam.
- C# currently has quest state packets such as `SmQuestList` and `SmQuestCompletedList`, and quest-drop static-data/runtime services for NPC drops.
- No C# `SmNearbyQuests` packet equivalent was found.
- No C# world-map-instance quest id registry equivalent was found.
- No C# `QuestNpc` registration table equivalent for dynamic quest handler `addOnQuestStart` was found.
- No C# `QuestService.checkStartConditions` equivalent was found for this nearby-quest UI path.
- No C# player-controller method currently invokes real nearby quest refresh.

## Implementation Implications

A real ItemPurification nearby-refresh dispatcher cannot be a direct call from the current no-op seam yet. It needs lower-level surfaces first:

1. A `SmNearbyQuests` packet with Java byte layout and tests.
2. A way to represent the candidate quest ids for the player's current world-map instance.
3. A Java-equivalent or deliberately staged `QuestService.checkStartConditions` surface for the nearby UI path, including `allowedDiffToMinLevel = 2`.
4. A level-requirement-difference calculator that matches Java's grey-marker bit behavior.
5. A player/connection send boundary that can emit the packet without enabling production `CM_ITEM_PURIFICATION`.

## Parity Gaps

- C# cannot yet compute nearby quest marker lists from a player's map-region/world-instance state.
- C# cannot yet serialize `SM_NEARBY_QUESTS`.
- C# lacks dynamic quest handler registration data that populates Java `QuestNpc.onQuestStart`.
- C# lacks Java-equivalent quest start-condition checks for this UI path.
- Java's `HashMap` iteration order is not stable; C# must avoid claiming packet order parity without runtime or deterministic Java artifact evidence.
- Java's delayed 1500 ms world-instance refresh for newly spawned quest NPCs is outside the ItemPurification path but is part of the same broader nearby-refresh system.

## Recommended Next Implementation Slice

Add only the packet/model prerequisite:

1. Implement `SmNearbyQuests` with Java packet layout.
2. Add packet tests for empty, available, and not-yet-available quest ids.
3. Keep quest candidate calculation, `QuestService.checkStartConditions`, and real ItemPurification dispatch disabled.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Java source reviewed. C# has only no-op ItemPurification planning/dispatch metadata; no real player-controller refresh, quest candidate calculation, or packet send exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | Not started | Server Packet | Not Started | No Tests | Unknown | Java packet writes `C(0)`, negative list size as unsigned `H`, and each quest id with bit `1 << 17` set when level diff is positive. No C# packet exists yet. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Needed with `allowedDiffToMinLevel = 2`, `warn = false`, and no skip flags. Full Java predicate has quest state, repeat-count, race, precondition, and level gates; C# equivalent is missing. |
| `com.aionemu.gameserver.world.WorldMapInstance` | Future C# world-map quest registry | World / Quest Registry | Partial | Manual Only | Needs Verification | C# has world/NPC spawn services, but no discovered equivalent of Java instance-level `questIds` populated from `QuestNpc.onQuestStart`. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc` | Not started for dynamic quest start registration | Quest Handler Registration | Not Started | No Tests | Unknown | Java dynamic handlers call `registerQuestNpc(...).addOnQuestStart`; C# dynamic quest handler registration is not ported for this path. Reflection/dynamic loading differences are significant. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| None | Manual | Java `PlayerController.updateNearbyQuests`, `SM_NEARBY_QUESTS`, `QuestService.checkStartConditions`, `WorldMapInstance.addObject`, and `QuestNpc` source review | Documents the dependency chain required before real nearby-refresh dispatch. | Static source audit only. | No C# packet, no service implementation, no Java runtime comparison. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No `SmNearbyQuests` packet exists in C# yet.
- No C# quest start-condition evaluator exists for nearby quest UI.
- No C# dynamic quest handler registration table exists for `QuestNpc.onQuestStart`.
- The current ItemPurification dispatcher seam must remain no-op until these lower-level surfaces exist.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 5
- Total artifacts ported: 0 in this docs-only audit
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 5
- Total blocked artifacts: 4 blocked/not-started categories, including `SM_NEARBY_QUESTS`, nearby quest candidate calculation, quest start-condition evaluation, and dynamic quest handler registration
- Estimated overall migration completion: Phase 6 remains about 70% complete
