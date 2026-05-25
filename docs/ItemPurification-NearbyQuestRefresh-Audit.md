# ItemPurification Nearby Quest Refresh Audit

Date: May 25, 2026
Unit of Work: UOW-980, updated by UOW-981 through UOW-983

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
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcQuestDropService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`

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
- UOW-981 adds `SmNearbyQuests` and `NearbyQuestMarker`, with packet tests for empty, available, and not-yet-available marker payloads.
- UOW-982 adds a minimal `WorldMapInstanceRuntimeState` quest-id registry and `RegisterQuestStartIds` method, matching Java's duplicate-collapsing `questIds.add(id)` behavior. It is not yet wired to NPC spawn or dynamic quest handlers.
- UOW-983 adds a staged `QuestNpcStartTable`, `QuestNpcStartRegistration`, and `QuestNpcStartRegistrationSource` boundary for Java handler/XML quest-start registrations. It is not yet populated by a Java handler or XML extractor.
- No C# `QuestService.checkStartConditions` equivalent was found for this nearby-quest UI path.
- No C# player-controller method currently invokes real nearby quest refresh.

## Implementation Implications

A real ItemPurification nearby-refresh dispatcher cannot be a direct call from the current no-op seam yet. It still needs lower-level surfaces first:

1. A way to populate `QuestNpcStartTable` from dynamic Java handlers and XML quest scripts, then feed the player's current world-map instance quest ids during NPC spawn.
2. A Java-equivalent or deliberately staged `QuestService.checkStartConditions` surface for the nearby UI path, including `allowedDiffToMinLevel = 2`.
3. A level-requirement-difference calculator that matches Java's grey-marker bit behavior.
4. A player/connection send boundary that can emit the packet without enabling production `CM_ITEM_PURIFICATION`.

Completed prerequisite:

- UOW-981 implements the `SmNearbyQuests` packet byte layout with C# unit tests. This does not compute marker lists or send the packet from a player-controller refresh path.
- UOW-982 implements the minimal world-map instance quest-id registry with unit tests. This does not dynamically populate it from quest handlers or schedule Java's delayed 1500 ms refresh.
- UOW-983 implements the in-memory `QuestNpc` start-registration table and a neutral source-record boundary for future Java handler and XML extractors. This does not parse Java/XML sources or execute dynamic quest handlers.

## Parity Gaps

- C# cannot yet compute nearby quest marker lists from a player's map-region/world-instance state because registration source extraction, world-instance population, and start-condition filtering remain missing.
- C# has staged `QuestNpc.onQuestStart` storage, but lacks extractors/loaders for Java handler registrations and XML quest script registrations.
- C# lacks Java-equivalent quest start-condition checks for this UI path.
- Java's `HashMap` iteration order is not stable; C# must avoid claiming packet order parity without runtime or deterministic Java artifact evidence.
- Java's delayed 1500 ms world-instance refresh for newly spawned quest NPCs is outside the ItemPurification path but is part of the same broader nearby-refresh system.
- Java handler source parsing alone is incomplete; XML-driven `start_npc_ids` registrations must also be modeled, and `start_item_id` suppresses NPC start registration for some template paths.

## Recommended Next Implementation Slice

Add only the next candidate-population prerequisite:

1. Implement a conservative Java handler and/or XML quest-script extractor that emits `QuestNpcStartRegistrationSource` rows, starting with a limited subset and explicit unresolved cases.
2. Keep `QuestService.checkStartConditions`, packet sending, dynamic quest handlers, and real ItemPurification dispatch disabled until registration extraction is complete enough to drive candidate source tests.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Java source reviewed. C# has only no-op ItemPurification planning/dispatch metadata; no real player-controller refresh, quest candidate calculation, or packet send exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `Aion.GameServer.Network.Aion.ServerPackets.NearbyQuestMarker` | Server Packet / DTO | Complete | Unit Tested | Verified Parity | UOW-981 verifies deterministic payload layout from Java source: `C(0)`, negative count as unsigned `H`, and `1 << 17` marker bit for positive level diff. Packet order parity is only for caller-provided marker order; Java `HashMap` iteration order is not claimed. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests.PacketOpCode` | Opcode Mapping | Complete | Unit Tested | Verified Parity | Java registers `SM_NEARBY_QUESTS` opcode `127` (`S_UPDATE_ZONE_QUEST`); C# packet uses opcode `127`. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | Not started for nearby quest UI | Service / Quest Predicate | Not Started | No Tests | Unknown | Needed with `allowedDiffToMinLevel = 2`, `warn = false`, and no skip flags. Full Java predicate has quest state, repeat-count, race, precondition, and level gates; C# equivalent is missing. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState` | World / Quest Registry | Partial | Unit Tested | Partial Parity | UOW-982 adds duplicate-collapsing quest-id registry storage and registration result semantics. It does not wire Java `addObject(Npc)`, delayed 1500 ms refresh scheduling, map-region lookup, or player sends. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartRegistration`; `Aion.GameServer.Dataholders.QuestNpcStartTable`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest Handler Registration / DTO | Partial | Unit Tested | Partial Parity | UOW-983 mirrors `registerQuestNpc`, missing-`getQuestNpc`, default range, and duplicate-collapsing `addOnQuestStart` storage. It does not model talk/kill/attack events, Java reflection/dynamic handler execution, handler unload/reload, XML registration extraction, or Java `HashSet` iteration order. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` empty case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates empty payload writes `C(0)` and zero negative count. | Deterministic byte assertion from source-reviewed Java packet layout. | No Java runtime capture. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates one marker writes negative count and unflagged quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Does not calculate candidates. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` not-yet-available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates positive level diff sets bit `1 << 17` before writing the quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Packet order follows provided marker order; Java `HashMap` runtime order is not claimed. |
| `WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_TracksQuestStartIdsLikeJavaWorldMapInstance` | Unit | Java `WorldMapInstance.addObject(Npc)` and `QuestNpc.getOnQuestStart` | Validates quest ids are stored once, repeated ids do not report a new addition, and newly registered ids are visible via snapshot. | Deterministic test from Java source-reviewed set semantics. | Does not invoke NPC spawn, `QuestEngine.getQuestNpc`, delayed refresh scheduling, or packet send. |
| `QuestNpcStartTableTests.RegisterQuestNpc_ReusesRegistrationAndTracksStartQuestIdsLikeJava` | Unit | Java `QuestEngine.registerQuestNpc` and `QuestNpc.addOnQuestStart` | Validates registration reuse, default quest range, and duplicate-collapsing start quest ids. | Deterministic C# test from source-reviewed Java map/set behavior. | Does not execute Java handlers or parse source/XML. |
| `QuestNpcStartTableTests.GetQuestNpc_ReturnsUnregisteredEmptyRegistrationLikeJava` | Unit | Java `QuestEngine.getQuestNpc` | Validates missing lookup returns an empty non-stored registration. | Deterministic C# test from reviewed Java method. | Does not model other `QuestNpc` event lists. |
| `QuestNpcStartTableTests.RegisterOnQuestStart_RecordsSourceBoundaryForFutureHandlerAndXmlExtractors` | Unit | Java `QuestEngine.init`, script handler loading, and XML quest registration review | Validates Java-handler/XML/manual source metadata can feed the same start table. | C# boundary test informed by Java load paths and explorer analysis. | Does not parse Java/XML sources yet. |
| `QuestNpcStartTableTests.RegisterQuestNpc_PreservesFirstRegisteredRangeLikeJava` | Unit | Java `QuestEngine.registerQuestNpc(int, int)` | Validates first registered quest range is preserved on reuse. | Deterministic C# test from reviewed Java `containsKey` branch. | Does not verify range consumers. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- No C# quest start-condition evaluator exists for nearby quest UI.
- C# dynamic quest-start registration storage exists, but no extractor/loader populates it from Java handlers or XML quest scripts.
- C# world-instance quest id registry storage exists, but it is not populated from NPC spawn or dynamic quest handlers.
- The current ItemPurification dispatcher seam must remain no-op until these lower-level surfaces exist.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 6
- Total artifacts ported: 4 packet/opcode/registry/start-registration artifacts for the nearby-quest prerequisites
- Total artifacts with verified parity: 2
- Total artifacts needing verification: 4
- Total blocked artifacts: 3 blocked/not-started categories, including Java/XML registration extraction, quest start-condition evaluation, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete
