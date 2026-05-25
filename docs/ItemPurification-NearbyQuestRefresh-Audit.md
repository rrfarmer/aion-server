# ItemPurification Nearby Quest Refresh Audit

Date: May 25, 2026
Unit of Work: UOW-980, updated by UOW-981 through UOW-994

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
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/models/XMLQuest.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/models/ReportToManyData.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/template/ReportTo.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/template/ReportToMany.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/template/WorkOrders.java`
- `game-server/src/com/aionemu/gameserver/questEngine/handlers/template/MonsterHunt.java`
- Representative Java handlers under `game-server/data/handlers/quest/**`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationQuestMutationNotifier.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestUpdateItemTable.cs`
- `dotnetConversion/src/Aion.GameServer/Model/GameObjects/PlayerQuestState.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmQuestCompletedList.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/ServerPackets/SmNearbyQuests.cs`
- `dotnetConversion/src/Aion.GameServer/Services/WorldNpcQuestDropService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestCandidateProjectionService.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/NearbyQuestTemplateXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Services/NearbyQuestStartConditionService.cs`
- `dotnetConversion/src/Aion.GameServer/World/WorldMapInstanceRuntimeState.cs`
- `docs/QuestStartConditions-Nearby-Audit.md`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartTable.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartXmlExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartJavaHandlerExtractor.cs`
- `dotnetConversion/src/Aion.GameServer/Dataholders/QuestNpcStartRegistrationSourceLoader.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/QuestNpcStartRegistrationSourceRealDataAuditTests.cs`

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
- UOW-984 adds `QuestNpcStartXmlExtractor`, a pure XML quest-script extractor that emits `QuestNpcStartRegistrationSource` rows from `start_npc_ids` attributes and skips `report_to_many` rows when `start_item_id` is nonzero, matching Java `ReportToMany.register` source behavior for that suppression path. It is not wired into `StaticData`, `DataManager`, NPC spawn, or runtime dispatch.
- UOW-985 adds `QuestNpcStartJavaHandlerExtractor`, a conservative Java source extractor that emits `QuestNpcStartRegistrationSource` rows for direct `registerQuestNpc(...).addOnQuestStart(...)` calls when the NPC id and quest id can be resolved from literals, simple `int` assignments, `int[]` indexes, or inherited `questId` via `super(...)`. Unsupported expressions are returned as unresolved rows instead of guessed. It is not wired into any loader or runtime dispatch.
- UOW-986 adds `QuestNpcStartRegistrationSourceLoader`, a staged offline file/directory loader that composes XML quest-script and Java handler extractor outputs in stable file order and preserves unresolved handler rows. It is not wired into production `StaticData`, `DataManager`, `QuestEngine`, NPC spawn, or runtime dispatch.
- UOW-987 adds a focused real-data audit test and `docs/QuestNpcStart-RealData-Audit.md`. The staged loader currently resolves 5184 start sources from repository data: 4400 XML sources and 784 Java handler sources, with 6 unresolved Java handler registrations, all using the unsupported `butlerId` expression.
- UOW-988 resolves the six `butlerId` registrations by supporting deterministic static integer-set iteration in Java handlers. The staged real-data audit now resolves 5214 start sources: 4400 XML sources and 814 Java handler sources, with zero unresolved Java handler registrations.
- UOW-989 adds a focused offline table-population audit that feeds those 5214 source rows into `QuestNpcStartTable`, producing 1668 registered NPC ids, 5214 registered NPC/quest start pairs, and a largest per-NPC quest-start set of 50. It still does not wire production startup or world-instance population.
- UOW-990 adds `NearbyQuestCandidateProjectionService`, a staged projection helper that consumes spawned NPC template ids, reads `QuestNpcStartTable`, and contributes matching `onQuestStart` quest ids to `WorldMapInstanceRuntimeState`. The real-data audit projects 1668 NPC ids into 4503 distinct world-instance quest ids. It still does not run start-condition filtering, delayed refresh scheduling, packet sends, or production startup.
- UOW-991 adds `docs/QuestStartConditions-Nearby-Audit.md`, a source audit for the Java nearby UI predicate `QuestService.checkStartConditions(player, questId, false, 2, false, false, false)` and `QuestService.getLevelRequirementDiff`. It identifies missing C# quest-template data, repeatability, XML start conditions, inventory preconditions, combine-skill checks, NPC faction checks, and level-diff projection before any real nearby-refresh send can be wired.
- UOW-992 adds `NearbyQuestTemplateTable` and `NearbyQuestStartConditionService`, a staged partial nearby predicate for early Java gates and `getLevelRequirementDiff`. It is not wired into production static data or packet sends, and unsupported dependencies are surfaced explicitly instead of assumed.
- UOW-993 adds `NearbyQuestTemplateXmlExtractor`, a staged XML extractor for the nearby predicate fields already represented by `NearbyQuestTemplateSummary`. It is not wired into production `StaticData`, `DataManager`, packet sends, or ItemPurification dispatch.
- UOW-994 adds a focused real-data audit for the staged nearby quest-template extractor. Current repository `quest_data.xml` yields 8043 summaries, including 2427 with XML start conditions, 363 with inventory preconditions, 628 with combine-skill requirements, 365 with NPC faction requirements, and 927 time-based repeat templates.
- No production C# `QuestService.checkStartConditions` equivalent is wired for this nearby-quest UI path.
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
- UOW-984 implements a pure XML `start_npc_ids` extractor with unit tests. This does not scan directories, invoke JAXB-equivalent model loading, parse Java handler sources, or populate runtime world instances.

## Parity Gaps

- C# cannot yet compute nearby quest marker lists from a player's map-region/world-instance state because the staged extractor loader is not production-wired, world-instance population is missing, and start-condition filtering remains missing.
- C# has staged `QuestNpc.onQuestStart` storage, a pure XML extractor, a conservative Java handler extractor, and an offline source loader, but still lacks production static-data integration, NPC-spawn population, and runtime refresh wiring.
- C# lacks Java-equivalent quest start-condition checks for this UI path.
- Java's `HashMap` iteration order is not stable; C# must avoid claiming packet order parity without runtime or deterministic Java artifact evidence.
- Java's delayed 1500 ms world-instance refresh for newly spawned quest NPCs is outside the ItemPurification path but is part of the same broader nearby-refresh system.
- XML extraction currently models only the `start_npc_ids` start-registration path and the `report_to_many start_item_id != 0` suppression rule. Other template-specific behavior, `aggro_start_npc_ids`, talk/kill/distance registrations, JAXB validation details, and runtime handler side effects remain outside this unit.

## Recommended Next Implementation Slice

Add only the next start-condition prerequisite:

1. Add a staged bridge that combines world-instance quest ids, `NearbyQuestTemplateTable`, and `NearbyQuestStartConditionService` into candidate marker DTOs without sending `SM_NEARBY_QUESTS`.
2. Keep XML start conditions, NPC faction, combine skill, packet sending, dynamic quest handlers, production `StaticData` integration, and real ItemPurification dispatch disabled until each dependency is modeled and tested.

## Migration Parity Table

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | Future C# nearby quest refresh service/adapter | Controller / Quest UI | Not Started | Manual Only | Needs Verification | Java source reviewed. C# has only no-op ItemPurification planning/dispatch metadata; no real player-controller refresh, quest candidate calculation, or packet send exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_NEARBY_QUESTS` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests`; `Aion.GameServer.Network.Aion.ServerPackets.NearbyQuestMarker` | Server Packet / DTO | Complete | Unit Tested | Verified Parity | UOW-981 verifies deterministic payload layout from Java source: `C(0)`, negative count as unsigned `H`, and `1 << 17` marker bit for positive level diff. Packet order parity is only for caller-provided marker order; Java `HashMap` iteration order is not claimed. |
| `com.aionemu.gameserver.network.aion.ServerPacketsOpcodes` | `Aion.GameServer.Network.Aion.ServerPackets.SmNearbyQuests.PacketOpCode` | Opcode Mapping | Complete | Unit Tested | Verified Parity | Java registers `SM_NEARBY_QUESTS` opcode `127` (`S_UPDATE_ZONE_QUEST`); C# packet uses opcode `127`. |
| `com.aionemu.gameserver.services.QuestService.checkStartConditions` | `Aion.GameServer.Services.NearbyQuestStartConditionService` | Service / Quest Predicate | Partial | Unit Tested | Partial Parity | UOW-992 stages early nearby gates only: missing template, started/reward state, conservative repeat-count checks, race, min/max level with allowed diff 2, class, gender, and abyss rank. XML start conditions, inventory item checks, combine skill, NPC faction, warning packets, exception/log behavior, and time-based repeat cooldowns remain unsupported. |
| `com.aionemu.gameserver.services.QuestService.getLevelRequirementDiff` | `Aion.GameServer.Services.NearbyQuestStartConditionService.GetLevelRequirementDiff` | Utility / Quest Predicate | Partial | Unit Tested | Partial Parity | UOW-992 tests Java's missing-template `99` and `minlevel_permitted - playerLevel` behavior. Production quest template loading and packet send integration remain unwired. |
| `com.aionemu.gameserver.model.templates.QuestTemplate`; `com.aionemu.gameserver.dataholders.QuestsData` | `Aion.GameServer.Dataholders.NearbyQuestTemplateSummary`; `Aion.GameServer.Dataholders.NearbyQuestTemplateTable`; `Aion.GameServer.Dataholders.NearbyQuestTemplateXmlExtractor` | Dataholder / DTO / XML Extractor | Partial | Unit Tested | Needs Verification | UOW-993 source-parses staged nearby predicate fields from XML. Production XML/JAXB loading, enum mapping from real static data, optional condition counting, category defaults, master-crafting adjustment, collect/inventory details, and repeat-cycle timing remain unported. |
| `com.aionemu.gameserver.model.templates.quest.XMLStartCondition` | Future C# XML start-condition predicate | Dataholder / Predicate | Not Started | Manual Only | Needs Verification | UOW-991 source-audits finished/unfinished/acquired/noacquired/equipped/title checks. Equipped-item checks do not block nearby UI because `warn = false`, but that intentional behavior still needs implementation tests. |
| `com.aionemu.gameserver.world.WorldMapInstance` | `Aion.GameServer.World.WorldMapInstanceRuntimeState`; `Aion.GameServer.Services.NearbyQuestCandidateProjectionService` | World / Quest Registry | Partial | Regression Tested | Partial Parity | UOW-982 adds duplicate-collapsing quest-id storage, and UOW-990 adds a staged NPC-template-id projection into that set. It does not wire production `addObject(Npc)`, delayed 1500 ms refresh scheduling, map-region lookup, or player sends. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc` | `Aion.GameServer.Dataholders.QuestNpcStartRegistration`; `Aion.GameServer.Dataholders.QuestNpcStartTable`; `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSource` | Quest Handler Registration / DTO | Partial | Unit Tested | Partial Parity | UOW-983 mirrors `registerQuestNpc`, missing-`getQuestNpc`, default range, and duplicate-collapsing `addOnQuestStart` storage. It does not model talk/kill/attack events, Java reflection/dynamic handler execution, handler unload/reload, XML registration extraction, or Java `HashSet` iteration order. |
| `com.aionemu.gameserver.questEngine.handlers.models.XMLQuest` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | XML Quest Loader Boundary | Partial | Unit Tested | Needs Verification | UOW-984 source-parses XML quest-script attributes into registration sources. It does not instantiate Java template handlers, run JAXB, process all XML model fields, or integrate with `QuestEngine.init`. |
| `com.aionemu.gameserver.questEngine.handlers.template.ReportToMany` | `Aion.GameServer.Dataholders.QuestNpcStartXmlExtractor` | Template / Quest Registration | Partial | Unit Tested | Partial Parity | UOW-984 mirrors the narrow `startItemId != 0` suppression of NPC start registration for `report_to_many`. Dialog behavior, item-use start, talk events, work items, reward state, and runtime handler execution are not ported here. |
| Representative `game-server/data/handlers/quest/**` classes extending `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler` | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor` | Java Handler Source Extractor | Partial | Unit Tested | Needs Verification | UOW-985 extracts direct `registerQuestNpc(...).addOnQuestStart(...)` calls only when expressions resolve conservatively. Dynamic expressions, loops, collection lookups, nonliteral assignments, reflection/loading behavior, and runtime handler execution remain unresolved. |
| `com.aionemu.gameserver.questEngine.QuestEngine.init` | `Aion.GameServer.Dataholders.QuestNpcStartRegistrationSourceLoader` | Offline Loader / Source Aggregator | Partial | Unit Tested | Needs Verification | UOW-986 composes XML and Java handler extractor outputs over files/directories with stable ordering and unresolved-row preservation. It does not run Java reflection/JAXB, register handlers into `QuestEngine`, or integrate with production `DataManager`. |
| `game-server/data/handlers/quest/oriel/*` and `game-server/data/handlers/quest/pernon/*` butler start handlers | `Aion.GameServer.Dataholders.QuestNpcStartJavaHandlerExtractor`; `QuestNpcStartRegistrationSourceRealDataAuditTests` | Java Handler Source Audit | Partial | Regression Tested | Partial Parity | UOW-988 resolves static integer-set iterator/enhanced-for registrations used by the six `butlerId` handlers. Java `HashSet` iteration order is not claimed, and runtime handler execution remains unported. |
| `com.aionemu.gameserver.model.templates.quest.QuestNpc.addOnQuestStart` | `Aion.GameServer.Dataholders.QuestNpcStartTable`; `QuestNpcStartRegistrationSourceRealDataAuditTests` | Quest NPC Registration Table | Partial | Regression Tested | Partial Parity | UOW-989 feeds all audited real-data source rows into the staged table, yielding 1668 NPC registrations and 5214 NPC/quest start pairs. Production `QuestEngine`, NPC spawn, world-instance population, threading, and Java `HashSet` iteration order remain unverified. |
| `com.aionemu.gameserver.controllers.PlayerController.updateNearbyQuests` | `Aion.GameServer.Services.NearbyQuestCandidateProjectionService`; future start-condition filter | Controller / Quest UI | Partial | Regression Tested | Needs Verification | UOW-990 stages the pre-filter world quest-id candidate source only. Java `QuestService.checkStartConditions`, level-diff calculation, `SM_NEARBY_QUESTS` send, and Java `HashMap` iteration order remain unimplemented/unverified. |

## Tests Added/Updated

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` empty case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates empty payload writes `C(0)` and zero negative count. | Deterministic byte assertion from source-reviewed Java packet layout. | No Java runtime capture. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates one marker writes negative count and unflagged quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Does not calculate candidates. |
| `GamePacketTests.ServerPacketPayloads_MatchJavaShapes` (`SmNearbyQuests` not-yet-available case) | Unit | Java `SM_NEARBY_QUESTS.writeImpl` | Validates positive level diff sets bit `1 << 17` before writing the quest id. | Deterministic byte assertion from source-reviewed Java packet layout. | Packet order follows provided marker order; Java `HashMap` runtime order is not claimed. |
| `WorldMapRuntimeStateTests.WorldMapInstanceRuntimeState_TracksQuestStartIdsLikeJavaWorldMapInstance` | Unit | Java `WorldMapInstance.addObject(Npc)` and `QuestNpc.getOnQuestStart` | Validates quest ids are stored once, repeated ids do not report a new addition, and newly registered ids are visible via snapshot. | Deterministic test from Java source-reviewed set semantics. | Does not invoke NPC spawn, `QuestEngine.getQuestNpc`, delayed refresh scheduling, or packet send. |
| `WorldMapRuntimeStateTests.NearbyQuestCandidateProjectionService_RegistersNpcStartQuestIdsLikeJavaWorldMapInstance` | Unit | Java `WorldMapInstance.addObject(Npc)` and `QuestNpc.getOnQuestStart` | Validates staged NPC ids project table `onQuestStart` ids into the world-instance quest-id set, preserving duplicate collapse and reporting newly added ids. | Deterministic C# test from source-reviewed Java set contribution behavior. | Does not evaluate start conditions, schedule refresh, or send packets. |
| `QuestNpcStartTableTests.RegisterQuestNpc_ReusesRegistrationAndTracksStartQuestIdsLikeJava` | Unit | Java `QuestEngine.registerQuestNpc` and `QuestNpc.addOnQuestStart` | Validates registration reuse, default quest range, and duplicate-collapsing start quest ids. | Deterministic C# test from source-reviewed Java map/set behavior. | Does not execute Java handlers or parse source/XML. |
| `QuestNpcStartTableTests.GetQuestNpc_ReturnsUnregisteredEmptyRegistrationLikeJava` | Unit | Java `QuestEngine.getQuestNpc` | Validates missing lookup returns an empty non-stored registration. | Deterministic C# test from reviewed Java method. | Does not model other `QuestNpc` event lists. |
| `QuestNpcStartTableTests.RegisterOnQuestStart_RecordsSourceBoundaryForFutureHandlerAndXmlExtractors` | Unit | Java `QuestEngine.init`, script handler loading, and XML quest registration review | Validates Java-handler/XML/manual source metadata can feed the same start table. | C# boundary test informed by Java load paths and explorer analysis. | Does not parse Java/XML sources yet. |
| `QuestNpcStartTableTests.RegisterQuestNpc_PreservesFirstRegisteredRangeLikeJava` | Unit | Java `QuestEngine.registerQuestNpc(int, int)` | Validates first registered quest range is preserved on reuse. | Deterministic C# test from reviewed Java `containsKey` branch. | Does not verify range consumers. |
| `QuestNpcStartXmlExtractorTests.ExtractsStartNpcIdsFromXmlQuestTemplates` | Unit | Java XML template `register` methods such as `ReportTo`, `WorkOrders`, and `MonsterHunt` | Validates XML `start_npc_ids` values produce one source per NPC id, including multiline whitespace-separated lists. | Deterministic C# test from source-reviewed Java template registration loops and JAXB list shape. | No Java runtime/JAXB comparison; not wired to loader. |
| `QuestNpcStartXmlExtractorTests.ReportToManyWithStartItemIdSkipsNpcStartRegistrationLikeJava` | Unit | Java `ReportToMany.register` | Validates nonzero `start_item_id` suppresses NPC start registration, while `0` or absent item id emits NPC starts. | Deterministic C# test from source-reviewed Java `if (startItemId != 0) registerQuestItem else startNpcIds` branch. | Does not model quest item registration. |
| `QuestNpcStartXmlExtractorTests.ExtractedSourcesCanPopulateQuestNpcStartTable` | Unit | Java XML start registrations feeding `QuestNpc.addOnQuestStart` | Validates extracted XML sources can populate the staged start table. | C# integration-style unit test for the staged extractor/table boundary. | No runtime world-instance population. |
| `QuestNpcStartXmlExtractorTests.ExtractFromStreamUsesSameXmlAttributeRules` | Unit | Java XML quest-script file loading | Validates stream input uses the same attribute parsing behavior. | Deterministic C# test for loader-friendly input. | No directory scan or `DataManager` integration. |
| `QuestNpcStartJavaHandlerExtractorTests.ExtractsLiteralNpcIdWithInheritedQuestId` | Unit | Representative Java handler `super(questId)` plus direct `registerQuestNpc(literal).addOnQuestStart(questId)` | Validates literal NPC ids and inherited `questId` extraction. | Deterministic C# test from source-reviewed Java handler shape. | No runtime Java handler loading. |
| `QuestNpcStartJavaHandlerExtractorTests.ExtractsScalarConstantsAndArrayIndexes` | Unit | Java handlers using `START_NPC_ID`, `questStartNpcId`, and `npcIds[0]` | Validates simple integer assignment and array-index resolution. | Deterministic C# test from source-reviewed handler patterns. | Does not cover loops or computed indexes. |
| `QuestNpcStartJavaHandlerExtractorTests.ReportsUnsupportedExpressionsInsteadOfGuessing` | Unit | Dynamic Java registration expressions | Validates unresolved rows are reported for unsupported NPC/quest expressions. | Conservative parser behavior test. | Does not enumerate every unsupported shape in the Java tree. |
| `QuestNpcStartJavaHandlerExtractorTests.ExtractedHandlerSourcesCanPopulateQuestNpcStartTable` | Unit | Java handler start registration feeding `QuestNpc.addOnQuestStart` | Validates extracted handler sources can populate the staged start table. | C# integration-style unit test for extractor/table boundary. | No runtime world-instance population. |
| `QuestNpcStartRegistrationSourceLoaderTests.Load_ComposesXmlAndJavaHandlerExtractorOutputsInStableFileOrder` | Unit | Java `QuestEngine.init` loads XML and handler registrations before runtime use | Validates staged loader composes XML and Java handler extractor outputs from directories in stable file order. | Deterministic C# test over temp source files. | Does not run Java classloading/JAXB or production `DataManager`. |
| `QuestNpcStartRegistrationSourceLoaderTests.Load_ReportsJavaHandlerUnresolvedRowsAlongsideResolvedSources` | Unit | Java handler source extraction limitations | Validates unresolved handler rows are preserved beside resolved registrations. | Conservative loader behavior test. | Does not triage real handler-tree unresolved counts. |
| `QuestNpcStartRegistrationSourceLoaderTests.Load_MissingDirectoriesReturnEmptyResult` | Unit | Staged offline loader safety | Validates missing optional source directories do not crash the staged loader. | Deterministic C# test. | Production missing-data policy is not selected. |
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_LoadsStagedQuestStartSourcesWithoutProductionWiring` | Regression | Real repository Java/XML quest-start source data | Pins current staged-loader counts: 5214 total sources, 4400 XML, 814 Java handler, zero unresolved handler registrations, 1668 distinct NPC ids, and 4503 distinct quest ids. | Deterministic C# audit over current repository source files. | Does not run Java reflection/JAXB, execute handlers, or prove runtime parity. |
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_PopulatesStagedQuestNpcStartTableWithoutProductionWiring` | Regression | Java `QuestNpc.addOnQuestStart` set semantics plus real repository source data | Pins current staged table-population counts: 5214 source rows, 1668 registered NPC ids, 5214 registered NPC/quest start pairs, and largest per-NPC quest count 50. | Deterministic C# audit over current repository source files and staged table behavior. | Does not populate Java/C# runtime world instances or run start-condition filtering. |
| `QuestNpcStartRegistrationSourceRealDataAuditTests.RealDataAudit_ProjectsStagedQuestIdsIntoWorldInstanceWithoutRefreshWiring` | Regression | Java `WorldMapInstance.addObject(Npc)` reading `QuestNpc.getOnQuestStart` | Pins staged world-instance projection counts from current real data: 1668 inspected/matched NPC ids and 4503 projected/new/world quest ids. | Deterministic C# audit over current repository source files, staged table behavior, and world quest-id storage. | Does not run Java/C# runtime NPC spawn, delayed refresh scheduling, start-condition filtering, or packet sends. |
| `docs/QuestStartConditions-Nearby-Audit.md` | Manual Source Audit | Java `QuestService.checkStartConditions`, `QuestService.getLevelRequirementDiff`, `QuestTemplate`, and `XMLStartCondition` | Documents gate order and missing C# dependencies for nearby UI filtering. | Source-reviewed Java audit with explicit C# gap list. | No executable C# predicate or Java runtime comparison yet. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaBasicQuestTemplateGates` | Unit | Java `QuestService.checkStartConditions` early template gates | Validates race, min/max level with nearby grace, class, gender, and abyss-rank gate behavior. | Deterministic C# test from source-reviewed Java predicate order. | Does not load real quest XML or evaluate XML start conditions. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_AppliesJavaQuestStateAndRepeatGatesConservatively` | Unit | Java `QuestState.canRepeat` and active-state checks | Validates START/REWARD blocks, nonrepeat complete blocks, repeat-count allowance, and time-based repeat cooldown is reported unsupported. | Deterministic C# test from source-reviewed Java state/repeat branches. | Does not model next-repeat timestamps. |
| `NearbyQuestStartConditionServiceTests.CheckNearbyStartConditions_ReportsUnsupportedJavaDependenciesInsteadOfAssumingParity` | Unit | Java XML/inventory/combine-skill/NPC-faction predicate dependencies | Validates staged predicate fails explicitly for dependencies not yet ported. | Conservative C# test preventing optimistic parity claims. | Does not implement those dependencies. |
| `NearbyQuestStartConditionServiceTests.GetLevelRequirementDiff_MatchesJavaMissingTemplateAndMinLevelBehavior` | Unit | Java `QuestService.getLevelRequirementDiff` | Validates missing-template `99`, positive min-level diff, and negative min-level diff behavior. | Deterministic C# test from source-reviewed Java utility. | Production quest-template loading remains unwired. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_ReadsNearbyPredicateQuestTemplateFieldsLikeJavaQuestTemplate` | Unit | Java `QuestTemplate` JAXB attributes/elements | Validates staged extraction of quest id, min/max level, race, class list, gender, rank, repeat count, repeat-cycle presence, XML condition presence, inventory item presence, combine skill, and NPC faction id. | Deterministic C# test from reviewed Java `QuestTemplate` annotations/getters. | Does not run JAXB or real-data audit. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_AppliesJavaQuestTemplateDefaultsForMissingOptionalFields` | Unit | Java `QuestTemplate` primitive/default field values | Validates missing optional fields map to staged defaults, including max repeat count `1`. | Deterministic C# test from source-reviewed Java field defaults. | Does not validate all `QuestTemplate` fields. |
| `NearbyQuestTemplateXmlExtractorTests.Extract_StreamInputFeedsNearbyQuestTemplateTableAndPredicate` | Unit | Java `QuestsData` indexing shape | Validates stream input can feed the staged table boundary. | C# staged boundary test informed by Java `QuestsData.afterUnmarshal`. | Not production `StaticData` integration. |
| `NearbyQuestTemplateXmlExtractorTests.RealDataAudit_LoadsNearbyQuestTemplateSummariesWithoutProductionWiring` | Regression | Real repository `quest_data.xml` and Java `QuestTemplate` fields | Pins staged extractor counts: 8043 summaries; 2427 XML-condition templates; 363 inventory templates; 628 combine-skill templates; 365 NPC-faction templates; 927 time-based templates; race/class/gender/rank/min/max-level counts. | Deterministic C# audit over current repository XML. | Does not run Java JAXB, production `StaticData`, XML condition predicates, or packet sends. |

## Remaining Risks

- Java runtime capture remains blocked locally by Java 8 and missing Maven.
- Only a staged partial C# quest start-condition evaluator, staged XML extractor, and real-data extractor audit exist for nearby quest UI; production wiring and unsupported dependencies remain absent.
- C# dynamic quest-start registration storage exists and XML/handler sources can be source-extracted, staged into `QuestNpcStartTable`, and projected into a staged world-instance quest-id set with zero unresolved real-data rows, but no production loader populates it from real data.
- C# world-instance quest id registry storage exists, but it is not populated from production NPC spawn or dynamic quest handlers.
- The current ItemPurification dispatcher seam must remain no-op until these lower-level surfaces exist.
- Automatic `CM_ITEM_PURIFICATION` dispatch remains plan-only and must stay disabled.

## Summary Metrics

- Total Java artifacts discovered: 14
- Total artifacts ported: 11 packet/opcode/registry/start-registration/XML-extractor/handler-extractor/source-loader/projection/template-boundary/partial-predicate/template-XML-extractor artifacts for the nearby-quest prerequisites
- Total artifacts with verified parity: 2
- Total artifacts needing verification: 13
- Total blocked artifacts: 4 blocked/not-started categories, including production NPC-spawn integration, production quest-template loading, XML condition data, and dynamic quest handler execution
- Estimated overall migration completion: Phase 6 remains about 70% complete
