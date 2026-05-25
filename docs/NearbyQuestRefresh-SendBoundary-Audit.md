# Nearby Quest Refresh Send Boundary Audit

Date: May 25, 2026
Unit of Work: UOW-996

## Purpose

This audit records the Java send triggers and C# production safety gates for future `SM_NEARBY_QUESTS` wiring.

Java remains the source of truth. This document does not enable packet sends, production player-controller refresh, production `StaticData` integration, or ItemPurification dispatch.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/controllers/PlayerController.java::updateNearbyQuests`
- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_LEVEL_READY.java::runImpl`
- `game-server/src/com/aionemu/gameserver/world/WorldMapInstance.java::addObject`
- `game-server/src/com/aionemu/gameserver/utils/PacketSendUtility.java::sendPacket`
- `game-server/src/com/aionemu/gameserver/network/aion/serverpackets/SM_NEARBY_QUESTS.java::writeImpl`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs::HandleLevelReadyAsync`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs::SendPacketAsync`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameClientSocketServer.cs::SendPacketToPlayerAsync`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/IGameClientConnectionRegistry.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestMarkerProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationQuestMutationNotifier.cs`

## Java Send Triggers

Java sends nearby quest markers from two reviewed paths:

1. `CM_LEVEL_READY.runImpl`
   - After the player reaches the level-ready/map-ready point, Java calls `activePlayer.getController().updateNearbyQuests()`.
   - This is an owner-only send through `PacketSendUtility.sendPacket`.

2. `WorldMapInstance.addObject(Npc)`
   - When an NPC is added, Java asks `QuestEngine.getQuestNpc(object.getObjectTemplate().getTemplateId())`.
   - New `QuestNpc.getOnQuestStart()` ids are added to the world-instance `questIds` set.
   - If any ids are new and no refresh task is already pending, Java schedules one delayed task for 1500 ms.
   - The delayed task clears the pending-task field and calls `player.getController().updateNearbyQuests()` for every player in the instance.
   - The null-check plus delay is a packet-spam guard for multi-spawn batches.

`PacketSendUtility.sendPacket(player, packet)` only sends when `player.isOnline()` is true, then calls `player.getClientConnection().sendPacket(packet)`.

## C# Current Boundary

C# already has these staged pieces:

- `SmNearbyQuests` serializes the reviewed Java payload shape.
- `WorldMapInstanceRuntimeState` stores staged world-instance quest ids.
- `NearbyQuestCandidateProjectionService` can contribute quest ids from staged NPC template ids.
- `NearbyQuestStartConditionService` handles only the early nearby predicate gates and rejects unsupported dependencies explicitly.
- `NearbyQuestMarkerProjectionService` projects passing staged quest ids to `NearbyQuestMarker` DTOs without sending.
- `GameServerConnection.HandleLevelReadyAsync` sends the current map-ready baseline packets but does not call a nearby refresh method.
- `IGameClientConnectionRegistry.SendPacketToPlayerAsync` and `GameServerConnection.SendPacketAsync` are available send boundaries, but no nearby-refresh caller uses them.
- `NoOpItemPurificationNearbyQuestRefreshDispatcher` remains intentionally no-op even when a refresh plan says nearby quests should refresh.

## Production Safety Gates

Do not wire a real C# nearby-refresh send until all selected gates for the target scope are satisfied:

1. Production quest-id source
   - `QuestNpcStartTable` must be populated from production startup data, or the send path must be explicitly limited to a staged/offline table.
   - NPC spawn or world-instance initialization must populate the active instance quest-id set.

2. Predicate coverage
   - The target send path must either implement full Java nearby `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)` behavior or filter out unsupported templates before sending.
- Current unsupported or incomplete categories include production player-controller refresh wiring, NPC faction daily assignment/mutation and quest-start wiring, and quest-finish repeat-date calculation. Staged static-data loading, configurable master-crafting adjustment, enter-world NPC faction hydration, the future assigned-faction quest-start helper, and a read-only repeat-date audit now exist, but they have not been Java-runtime verified or wired to live sends.

3. Player/world lookup
   - The send method must resolve the player's current map-region parent/world-instance equivalent.
   - Missing instance data must fail closed rather than sending an empty list that could hide real markers.

4. Send trigger selection
   - `CM_LEVEL_READY` immediate owner send and `WorldMapInstance.addObject(Npc)` delayed instance fanout are separate Java triggers.
   - Implement and test them independently.

5. Debounce semantics
   - The NPC-spawn refresh path needs a 1500 ms one-pending-task guard before runtime parity can be claimed.
   - C# async scheduling/threading will differ from Java `ThreadPoolManager`; that difference needs tests around duplicate suppression and task reset.

6. Ordering and collection semantics
   - Java builds a `HashMap<Integer, Integer>` and iterates `entrySet()`.
   - C# must not claim packet order parity unless a deterministic runtime artifact or approved ordering decision exists.

7. Socket/send behavior
   - Java checks `player.isOnline()` before calling the connection send.
   - C# registry sends fail when no active connection/player exists; the final player-controller boundary needs equivalent online/connection gating.

8. ItemPurification isolation
   - ItemPurification may continue to plan nearby-refresh candidates, but production `CM_ITEM_PURIFICATION` dispatch must not invoke real nearby-refresh until the above gates are satisfied.

## Tests Added Or Updated

No tests were added in UOW-996. Verification for this unit is manual source review of the Java/C# send boundary and documentation updates only.

UOW-997 follow-up:

- `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsSupportedNearbyMarkersWithoutProductionSendWiring` now pins a supported-template staged marker projection over current repository data.
- The test remains offline and does not send `SM_NEARBY_QUESTS`.

UOW-998 follow-up:

- `NearbyQuestRefreshPlanServiceTests` covers a non-sending refresh-plan boundary that composes staged marker projection into explicit readiness/failure states.
- The plan remains offline and does not call connection or registry send APIs.

UOW-999 follow-up:

- `NearbyQuestStartConditionServiceTests` now cover a staged XML start-condition subset for nearby checks.
- This reduces one predicate blocker but still does not enable production nearby sends.

UOW-1000 follow-up:

- `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAgainstJavaSchema_WhenEnabled` covers Java-schema reward-group hydration for repository-loaded quest states.
- This supports XML `finished reward` checks but still does not enable production nearby sends.

UOW-1001 follow-up:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaInventoryItemPresenceGate` covers staged inventory item-id presence checks.
- A read-only sub-agent mapped Java repeat timing for the next slice and was closed.

UOW-1002 follow-up:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively` now covers Java `QuestState.canRepeat()` next-repeat timing edge cases for nearby checks.
- `PlayerEnterWorldRepositoryDatabaseIntegrationTests.LoadPlayerQuests_HydratesRewardGroupAndRepeatTimesAgainstJavaSchema_WhenEnabled` covers gated Java-schema hydration of `reward`, `next_repeat_time`, and `complete_time`.

UOW-1003 follow-up:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaCombineSkillGate` covers staged Java `QuestService.checkCombineSkill` behavior.
- A read-only sub-agent mapped NPC faction behavior for the next slice and was closed.

UOW-1004 follow-up:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaNpcFactionGate` covers staged Java NPC faction predicate behavior.
- A read-only sub-agent mapped configurable master-crafting XML required-count behavior for the next slice and was closed.

UOW-1005 follow-up:

- `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaMasterCraftingRequiredConditionAdjustment` covers Java's configurable master-crafting XML required-count adjustment.

UOW-1006 follow-up:

- `StaticDataLoadingTests.StaticData_LoadsNpcFactionTemplatesLikeJavaDataholder` covers Java NPC faction static-data indexing and mentor-category behavior.

Existing relevant tests remain:

- `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` for `SmNearbyQuests` payload serialization.
- `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance`.
- `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring`.
- `NearbyQuestStartConditionServiceTests.*`.
- `NearbyQuestTemplateXmlExtractorTests.RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring`.
- `NearbyQuestMarkerProjectionServiceTests.*`.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby refresh send service using `NearbyQuestMarkerProjectionService` and `SmNearbyQuests` | Controller / Quest UI Send Boundary | Not Started | Manual Only | Needs Verification | Java source reviewed for marker calculation and owner packet send. C# has staged marker projection only; no player-controller method, map-region lookup, production quest-template loading, packet send, or Java `HashMap` ordering parity. |
| `com.aionemu.gameserver.network.aion.clientpackets.CM_LEVEL_READY` | `Aion.GameServer.Network.Aion.GameServerConnection.HandleLevelReadyAsync` | Client Packet Handler / Enter-Map Trigger | Partial | Manual Only | Needs Verification | Java calls `activePlayer.getController().updateNearbyQuests()` from level-ready. C# level-ready sends baseline map-ready packets but intentionally does not send nearby quest markers yet. |
| `com.aionemu.gameserver.world.WorldMapInstance.addObject` | `Aion.GameServer.World.WorldMapInstanceRuntimeState`; future NPC-spawn refresh scheduler | World Instance / Delayed Refresh Trigger | Partial | Regression Tested plus Manual Audit | Partial Parity | Staged quest-id storage/projection has tests, but production `addObject(Npc)`, `QuestEngine.getQuestNpc`, 1500 ms debounce scheduling, task reset, and per-player instance fanout are not wired. C# async/threading parity is unknown. |
| `com.aionemu.gameserver.utils.PacketSendUtility.sendPacket` | `Aion.GameServer.Network.Aion.IGameClientConnectionRegistry.SendPacketToPlayerAsync`; `GameServerConnection.SendPacketAsync` | Packet Send Utility Boundary | Partial | Existing Regression Coverage Elsewhere plus Manual Audit | Needs Verification | C# has owner-send primitives used by other systems. A nearby-refresh caller has not been implemented. Java `player.isOnline()` gating must be matched by active connection/player checks. Socket send failure behavior remains unverified for this path. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests` | Server Packet / Serialization | Complete | Unit Tested | Verified Parity | Existing tests cover source-reviewed byte layout: `C(0)`, negative count, and `1 << 17` marker flag for positive level diff. Caller ordering remains not claimed because Java `HashMap` iteration order is not deterministic. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions`; `QuestService.inventoryItemCheck`; `QuestState.canRepeat`; `QuestService.checkCombineSkill`; `NpcFactions.canStartQuest` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate Dependency | Partial | Unit Tested | Partial Parity | Current staged predicate covers early gates, XML subset, reward-group hydration, inventory item-id presence checks, completed-quest repeat timing, combine-skill checks, and UOW-1004 NPC faction checks. Exception/log behavior, production static-data loading, and live send triggers remain unsupported. |
| `com.aionemu.gameserver.model.templates.quest.InventoryItems`; `com.aionemu.gameserver.model.templates.quest.InventoryItem` | `Aion.GameServer.Dataholders.NearbyQuestInventoryItem`; `NearbyQuestTemplateXmlExtractor` | Dataholder / Predicate Dependency | Partial | Unit Tested | Partial Parity | UOW-1001 parses `inventory_item.item_id` and optional `count`; nearby start filtering uses item-id presence only. Count-based collection/consumption behavior remains outside this send-boundary unit. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition`; `QuestTemplate.getRequiredConditionCount`; `CraftConfig.MAX_MASTER_CRAFTING_SKILLS` | `Aion.GameServer.Dataholders.NearbyQuestXmlStartCondition`; `NearbyQuestStartConditionService` | Dataholder / Predicate Dependency | Partial | Unit Tested | Partial Parity | UOW-999 adds staged support for the source-audited nearby XML subset while keeping unknown XML children fail-closed. UOW-1005 adds configurable master-crafting required-count adjustment. Java runtime comparison and production config plumbing remain missing. |
| `com.aionemu.gameserver.dao.PlayerQuestListDAO.load` | `Aion.GameServer.Data.MySqlPlayerEnterWorldRepository.LoadPlayerQuestsAsync` | Repository / Quest State Hydration | Partial | Integration Tested when DB flag enabled | Partial Parity | UOW-1002 reads nullable `player_quests.reward`, `next_repeat_time`, and `complete_time` into `PlayerQuestState`, matching Java's load shape. Timezone interpretation of unspecified MySQL timestamps needs live DB verification. |
| `com.aionemu.gameserver.dataholders.NpcFactionsData`; `NpcFactionTemplate` | `Aion.GameServer.Dataholders.NpcFactionTable`; `StaticData.NpcFactions` | Static Data / NPC Faction Dependency | Partial | Unit Tested; Regression Tested | Partial Parity | UOW-1006 loads faction templates by id and registrar NPC id, including mentor-category behavior needed by future `player_npc_factions` hydration. Java JAXB runtime comparison remains missing. |
| `com.aionemu.gameserver.questEngine.QuestEngine.onItemGet` / `onItemRemoved` nearby refresh calls | `Aion.GameServer.Services.NoOpItemPurificationNearbyQuestRefreshDispatcher` | Quest Callback / ItemPurification Refresh Dependency | Partial | Unit Tested for Planning; Manual Audit for Send Boundary | Needs Verification | ItemPurification can plan refresh candidates through `questUpdateItems`, but dispatcher stays no-op. Real quest handlers and nearby marker sends remain disabled by design. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestRefreshPlanService` | Controller / Quest UI Refresh Plan | Partial | Unit Tested | Partial Parity | Non-sending plan composes staged marker projection and rejection counts with fail-closed missing-data statuses. It does not send `SM_NEARBY_QUESTS`, resolve live map regions, schedule delayed refresh, or invoke ItemPurification dispatch. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- C# has no production nearby-refresh send method.
- `CM_LEVEL_READY` nearby marker send is absent in C#.
- NPC-spawn delayed refresh fanout is absent in C#.
- Production quest-template loading and production quest-start source loading remain unwired.
- Unsupported nearby predicate dependencies remain broad and must fail closed; XML, inventory, repeat timing, combine-skill, NPC faction, master required-count, and NPC faction static-data conditions are now partial with reward/time hydration, while NPC faction repository hydration remains unsupported.
- Java `HashMap`/set ordering is not deterministic; packet marker order parity is not claimed.
- C# async scheduling for a future 1500 ms debounce will need concurrency tests.
- `NearbyQuestRefreshPlanService` has no production caller and is intentionally non-sending.
- Reflection/dynamic Java quest-handler execution remains unported.
- Repeat timing is staged for start checks only; quest-finish reset-date calculation and DB timezone verification remain incomplete.
- No serialization code changed in this unit; existing `SmNearbyQuests` tests remain the serialization evidence.

## Summary Metrics

- Total Java artifacts discovered: 7 in this unit
- Total artifacts ported: 9 staged boundaries after this audit's original unit: non-sending refresh plan, XML start-condition subset, reward/repeat hydration, inventory preconditions, repeat timing predicate support, combine-skill predicate support, NPC faction predicate support, master required-count support, and NPC faction static-data support
- Total artifacts with verified parity: 1 existing packet artifact referenced by this audit
- Total artifacts needing verification: 11
- Total blocked artifacts: 4 blocked/not-started categories: production send method, level-ready send trigger, delayed NPC-spawn refresh scheduler, and unsupported predicate dependencies
- Estimated overall migration completion: Phase 6 remains about 70% complete; this unit clarifies send-boundary blockers without enabling live nearby quest refresh.

## Next Recommended Unit Of Work

Add NPC faction repository hydration or broaden refresh-plan audits across representative player archetypes. Keep actual packet sends, `CM_LEVEL_READY` integration, NPC-spawn delayed refresh, production `StaticData` integration, and production ItemPurification dispatch disabled until follow-up tests cover each gate.
