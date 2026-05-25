# ItemPurification AP And Quest Readiness Audit

Date: May 25, 2026
Unit of Work: UOW-966

## Purpose

This audit records the remaining Java side effects that block automatic `CM_ITEM_PURIFICATION` dispatch even after the C# plan, live mutation, concrete packets, and repository persistence seams exist.

Java remains the source of truth. This document does not enable production dispatch and does not claim verified runtime parity.

## Java Source Breadcrumbs

- `game-server/src/com/aionemu/gameserver/network/aion/clientpackets/CM_ITEM_PURIFICATION.java`
- `game-server/src/com/aionemu/gameserver/services/item/ItemPurificationService.java`
- `game-server/src/com/aionemu/gameserver/services/abyss/AbyssPointsService.java`
- `game-server/src/com/aionemu/gameserver/model/gameobjects/player/AbyssRank.java`
- `game-server/src/com/aionemu/gameserver/model/items/storage/Storage.java`
- `game-server/src/com/aionemu/gameserver/questEngine/QuestEngine.java`

## C# Source Breadcrumbs

- `dotnetConversion/src/Aion.GameServer/Services/AbyssPointsService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveExecutionService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationLiveMutationService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationApplicationPlanService.cs`
- `dotnetConversion/src/Aion.GameServer/Services/ItemPurificationPersistentLiveExecutionService.cs`
- `dotnetConversion/src/Aion.GameServer/Network/Aion/GameServerConnection.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/AbyssPointsServiceTests.cs`
- `dotnetConversion/tests/Aion.GameServer.Tests/ItemPurificationLiveExecutionServiceTests.cs`

## Java Runtime Sequence

For the normal successful purification path, Java performs:

1. `CM_ITEM_PURIFICATION.runImpl` resolves the player item by object id.
2. `ItemPurificationService.isPurificationAllowed` validates the template, result item, identified state, enchantment level, AP, Kinah, and required materials.
3. `isPurificationAllowed` sends the upgrade-success system message before mutation.
4. `decreaseMaterials` consumes required materials in template order.
5. If AP is required, `AbyssPointsService.addAp(player, -necessaryAbyssPoints)` runs.
6. If Kinah is required, Java calls `player.getInventory().decreaseKinah(-necessaryKinah)`. Current Java `Storage.decreaseKinah` only mutates when `amount > 0`, so the purification Kinah call is a source-level no-op.
7. The base item is deleted or updated according to the service path.
8. `upgradeItem` creates and adds the target item with inherited item state.

## AP Side Effects

Java `AbyssPointsService.addAp(Player, int)` does the following for purification AP spend:

- Captures old AP and rank.
- Mutates `AbyssRank.addAp(amount)`, including AP clamping and rank recompute rules from `AbyssRank`.
- Sends `SM_SYSTEM_MESSAGE.STR_MSG_USE_ABYSSPOINT(-added)` for spend.
- Sends `SM_ABYSS_RANK` when AP or rank changed.
- If rank changed, broadcasts `SM_ABYSS_RANK_UPDATE(0, player)`.
- If rank changed, calls `player.getEquipment().checkRankLimitItems()`.
- If rank changed, calls `AbyssSkillService.updateSkills(player)`.
- Adds Legion contribution only when `added > 0`; purification spend is negative, so no Legion contribution is expected.
- Runs Siege callback only through `addAp(Player, VisibleObject, int)`; purification calls the plain `addAp(Player, int)`, so no Siege callback is expected.

Current C# status:

- `Aion.GameServer.Services.AbyssPointsService.AddAp` mutates `Player.AbyssRank` and returns an `AbyssPointsAddPlan`.
- The plan includes spend/gain system-message packets, `SmAbyssRank`, `SmAbyssRankUpdate`, rank-limit and abyss-skill flags, Legion contribution intent for positive AP, and Siege callback intent only through `AddApFromObject`.
- `ItemPurificationLiveMutationService.Apply` calls `AbyssPointsService.AddAp(player, -AbyssPointsToSpend, ...)`.
- `ItemPurificationLiveExecutionService.ExecuteAsync` sends the success message first, applies live mutation, then sends the concrete mutation packet plan.
- UOW-968 adds live-execution regression coverage proving a purification AP spend that drops rank still produces `AbyssPointsAddPlan` rank-change metadata (`SmSystemMessage`, `SmAbyssRank`, `SmAbyssRankUpdate`, rank-limit flag, abyss-skill flag).
- UOW-969 emits `AbyssPointsAddPlan.PlayerPackets` from the explicit live-execution helper at the existing `AbyssPointsUpdate` packet-plan slot.
- UOW-970 broadcasts `AbyssPointsAddPlan.RankUpdatePacket` to visible players from the explicit live-execution helper after the AP player packets and before later mutation packets.
- UOW-971 runs `EquipmentService.CheckRankLimitItems` from the explicit live-execution helper at the AP rank-change point, mutates the player inventory when rank-limited equipment is unequipped, and exposes the `EquipmentChangeResult`. It still does not send the unequip packet fanout, persist the unequipped rows, or refresh abyss skills.
- UOW-972 sends explicit live-execution rank-limit unequip fanout after the rank-limit state mutation: owner `SmInventoryUpdateItem` equip/unequip packets, `SmSystemMessage.UnequipRankItem` messages, and visible-player `SmUpdatePlayerAppearance` broadcasts when `EquipmentChangeResult.BroadcastAppearance` is true. It still does not persist the unequipped rows, refresh stats, or execute abyss skills in this path.
- UOW-973 runs `AbyssSkillService.UpdateSkills` from explicit live execution after the equipment rank-limit pass when AP spend changes rank, mutates `player.Skills`, and sends `SmSkillRemove` / `SmSkillList` packets for the modeled skill deltas. It still does not execute broader SkillEngine effect fanout, persist skill rows, or compare against Java runtime artifacts.
- UOW-975 plumbs the configured `GameServerOptions.Custom.TopRankingXformMinRank` from `GameServerConnection` into explicit ItemPurification live and persistent live execution. The static helper still defaults to `AbyssSkillService.DefaultTransformMinRank` for isolated tests and direct callers.
- `ItemPurificationPersistentLiveExecutionService.ExecuteAsync` persists inventory mutation and updated abyss rank together, but does not execute rank-change side-effect services.

AP parity gaps:

- AP spend player packets are emitted by explicit ItemPurification live execution, but no Java runtime packet-byte/order artifact exists yet.
- Rank update broadcast is emitted from explicit ItemPurification live execution, but no Java runtime packet-byte/order artifact exists yet.
- Equipment rank-limit unequip is invoked from explicit ItemPurification live execution, mutates in-memory equipment state, and emits the current C# equip/unequip packet fanout. Persistence and Java runtime ordering/byte comparison are not wired in this path.
- Abyss skill refresh is invoked from explicit ItemPurification live execution and emits modeled skill add/remove packets. The connection-level explicit helpers now pass the configured transform-rank minimum, but broader SkillEngine effect fanout, skill persistence, and Java runtime comparison remain missing.
- Legion contribution is correctly absent for spend.
- Siege callback is correctly absent for purification's plain AP spend path.
- Ranking cache behavior and AP/login rank-limited equipment passes remain outside this dispatch scope.

Safe AP next tests:

- Add an explicit side-effect executor only behind opt-in live execution, then cover equipment rank-limit and abyss-skill invocation without enabling production dispatch.
- Add unequip packet fanout and persistence coverage for rank-limited equipment changes once the explicit live-execution persistence boundary is selected.
- Add abyss skill refresh execution once skill/effect packet fanout boundaries are ready.

## Quest Callback Side Effects

Java storage callback behavior relevant to purification:

- `Storage.decreaseItemCount` sends an update packet and marks storage dirty when the item remains after the count decrement.
- `Storage.decreaseItemCount` calls `delete(item, deleteType, actor)` only when the item count reaches `<= 0` and the item is not Kinah.
- `Storage.delete` removes the item from storage, marks it `DELETED`, enqueues it in `deletedItems`, marks storage `UPDATE_REQUIRED`, sends the delete packet, then calls `QuestEngine.onItemRemoved(actor, itemId)`.
- `Storage.add` inserts the item, assigns storage location, marks storage `UPDATE_REQUIRED`, sends the storage update packet, then calls `QuestEngine.onItemGet(actor, itemId)` only when `actor != null` and the storage type is `CUBE`.
- `QuestEngine.onItemGet` invokes registered get-item quest handlers, then calls `player.getController().updateNearbyQuests()` when the item id is in `questUpdateItems`.
- `QuestEngine.onItemRemoved` only calls `updateNearbyQuests()` when the item id is in `questUpdateItems`; there is no symmetric remove-handler map.

Current C# status:

- `ItemPurificationApplicationPlanService` records ordered operations and already flags quest-notification intent on exhausted material/base deletes and target adds.
- UOW-967 adds `ItemPurificationApplicationPlanService.ProjectQuestNotifications`, a pure projection that maps `DeleteMaterialItem` and `DeleteBaseItem` operations to `ItemRemoved` candidates and `AddTargetItem` operations to `ItemGet` candidates.
- UOW-974 adds `IItemPurificationQuestMutationNotifier` plus `NoOpItemPurificationQuestMutationNotifier` as an explicit opt-in live-execution seam. When a notifier is supplied, live execution projects the Java-ordered candidates after a successful mutation send and returns the dispatch result. The default path still does not invoke quest handlers or nearby-quest refresh.
- UOW-976 adds `docs/ItemPurification-QuestUpdateItems-Audit.md`, documenting that Java `QuestEngine.init` builds `questUpdateItems` from quest XML `<inventory_items><inventory_item item_id=...>` and that C# does not yet expose that membership set.
- UOW-977 adds `StaticData.QuestUpdateItems` backed by `QuestUpdateItemTable`, collecting distinct quest inventory `item_id` values in Java first-seen order and ignoring optional `count`. This is static-data membership only; no real nearby-quest refresh or quest handler dispatch is wired.
- UOW-978 adds `PlanningItemPurificationQuestMutationNotifier` and `ItemPurificationNearbyQuestRefreshPlan`, an opt-in no-op planning seam that filters projected get/remove notifications through `StaticData.QuestUpdateItems` and reports which candidates would request nearby refresh. It does not invoke `updateNearbyQuests`, quest handlers, or automatic production dispatch.
- UOW-979 adds `IItemPurificationNearbyQuestRefreshDispatcher` and `NoOpItemPurificationNearbyQuestRefreshDispatcher`, an explicit no-op dispatch seam for planned nearby-refresh candidates. It still does not invoke the player controller, quest handlers, or automatic production dispatch.
- UOW-980 adds `docs/ItemPurification-NearbyQuestRefresh-Audit.md`, documenting that Java `PlayerController.updateNearbyQuests()` depends on world-instance quest ids, `QuestService.checkStartConditions(... allowedDiffToMinLevel = 2 ...)`, `QuestService.getLevelRequirementDiff`, and `SM_NEARBY_QUESTS`. C# lacks those lower-level surfaces, so the dispatcher must remain no-op.
- UOW-981 adds `SmNearbyQuests` packet serialization and tests for Java's `SM_NEARBY_QUESTS` byte layout. C# still lacks candidate calculation, world-instance quest ids, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-982 adds minimal world-map instance quest-id registry storage and duplicate-collapsing registration tests. C# still lacks dynamic `QuestNpc.onQuestStart` population, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-983 adds staged `QuestNpcStartTable` storage plus source metadata for future Java handler/XML quest-start extraction. C# still lacks extractors/loaders, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-984 adds a pure XML quest-script `start_npc_ids` extractor that emits staged `QuestNpcStartRegistrationSource` rows and mirrors Java `ReportToMany.register` by skipping NPC start registration when `start_item_id` is nonzero. C# still lacks Java handler source extraction, loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-985 adds a conservative Java handler source extractor for direct `registerQuestNpc(...).addOnQuestStart(...)` calls, resolving literals, simple `int` assignments, `int[]` indexes, and inherited `questId` via `super(...)`, while reporting unsupported expressions as unresolved. C# still lacks loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-986 adds a staged offline source loader that composes XML quest-script and Java handler extractor outputs from source directories and preserves unresolved handler rows. C# still lacks production loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-987 adds a focused real-data audit test for the staged source loader. Current repository data yields 5184 resolved staged start sources, split into 4400 XML and 784 Java handler sources, with 6 unresolved Java handler registrations using `butlerId`. C# still lacks production loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-988 resolves the six `butlerId` Java handler registrations by supporting deterministic static integer-set iteration. Current repository data now yields 5214 resolved staged start sources, split into 4400 XML and 814 Java handler sources, with zero unresolved Java handler registrations. C# still lacks production loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-989 feeds the audited loader output into `QuestNpcStartTable` in a focused offline regression test, yielding 1668 registered NPC ids and 5214 registered NPC/quest start pairs. C# still lacks production loader integration, NPC-spawn population, candidate filtering, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-990 adds a staged nearby-quest candidate projection helper that reads `QuestNpcStartTable` and contributes matching NPC start quest ids to `WorldMapInstanceRuntimeState`. The real-data audit projects 1668 NPC ids into 4503 distinct world-instance quest ids. C# still lacks production loader integration, real NPC-spawn invocation, start-condition evaluation, and a send path, so the dispatcher must remain no-op.
- UOW-991 adds a source audit for Java nearby quest start-condition filtering and level-diff marker calculation. C# still lacks the quest-template data, repeatability, XML start-condition predicate, inventory precondition, combine-skill, NPC faction, and level-diff surfaces needed before any nearby-refresh send path can be wired.
- UOW-992 adds a staged nearby quest-template boundary and partial nearby start-condition service for early Java gates plus `getLevelRequirementDiff`. Unsupported XML start conditions, inventory preconditions, combine skill, NPC faction, and time-based repeat cooldowns remain explicit failures, and production loading/sending remains disabled.
- UOW-993 adds a staged XML extractor for the nearby quest-template summary fields used by the partial predicate. It remains offline and is not wired into production `StaticData`, `DataManager`, player-controller sends, or ItemPurification dispatch.
- UOW-994 adds a real-data audit for that staged extractor, pinning current repository `quest_data.xml` counts for 8043 quest summaries and unsupported dependency flags. It remains offline and does not invoke the predicate over live players.
- UOW-995 adds a staged marker projection bridge that filters world quest ids through the partial nearby predicate and returns marker DTOs plus rejection reasons. It still does not send packets or invoke production player-controller refresh.
- UOW-996 adds `docs/NearbyQuestRefresh-SendBoundary-Audit.md`, confirming the Java nearby send triggers that still block production dispatch: `CM_LEVEL_READY` calls `PlayerController.updateNearbyQuests()` immediately, and `WorldMapInstance.addObject(Npc)` schedules a debounced 1500 ms instance-wide refresh when new NPC quest-start ids are registered. C# has send primitives and staged marker projection, but no nearby-refresh send caller, no delayed NPC-spawn refresh scheduler, and no production ItemPurification refresh dispatcher.
- UOW-997 adds a real-data staged marker projection audit for templates with no currently unsupported nearby dependencies: 2072 supported projected quest ids, 920 staged markers, and 1152 supported early-gate rejections for a level-65 Elyos male Gladiator, with zero unsupported dependency failures. It still does not send packets or invoke production refresh.
- UOW-998 adds `NearbyQuestRefreshPlanService`, a non-sending refresh-plan boundary that reports whether a staged nearby marker packet would be ready, why it is not ready, and which quest ids were rejected. It remains disconnected from ItemPurification and all packet-send paths.
- UOW-999 adds staged nearby XML start-condition support for `finished`, `unfinished`, `noacquired`, `acquired`, Java nearby `equipped` no-op behavior, and `required_title`. Inventory/combine/NPC-faction/time-based behavior, production reward-group hydration, and live sends remain disabled.
- UOW-1000 hydrates Java-schema `player_quests.reward` into `PlayerQuestState.RewardGroup`, removing the production-load gap for XML `finished reward` checks while leaving repeat timing and live sends disabled.
- UOW-1001 adds staged nearby inventory item precondition support, matching Java `inventoryItemCheck` by checking item-id presence only. Optional XML counts are parsed but not enforced in this gate. A read-only repeat-timing analysis records the next date/time slice.
- UOW-1002 adds the narrow nearby repeat-timing slice: `PlayerQuestState.NextRepeatTime`/`CompleteTime`, repository hydration for `next_repeat_time`/`complete_time`, repeat-cycle token preservation, and deterministic Java `QuestState.canRepeat()` edge-case tests.
- UOW-1003 adds staged nearby combine-skill checks, matching Java `QuestService.checkCombineSkill` for explicit skills, the `-1` any-skill sentinel, NPC faction 12/13 tapping exclusion, and `TASK` work-order upper-bound behavior.
- UOW-1004 adds staged nearby NPC faction checks, matching Java active exact faction rows, mentor/non-mentor slot cooldown behavior, time-based cooldown skip, and `mentor_type` slot selection. Repository/static-data hydration and live sends remain disabled.
- UOW-1005 adds configurable master-crafting XML required-count parity for nearby checks. Production config plumbing and live sends remain disabled.
- Partial material count updates do not need item-remove callbacks under Java behavior.
- Live mutation replaces the player inventory snapshot and applies AP, but intentionally leaves persistence, sends, quest callbacks, and rollback outside its boundary.
- Production `HandleInfrastructurePacketAsync` still routes `CmItemPurification` to the plan-only `HandleItemPurificationAsync` path.
- No C# quest item get/remove dispatcher is wired into ItemPurification execution; the UOW-967 projection is metadata only.

Quest parity gaps:

- C# has metadata, a pure ordered projection, and an opt-in no-op notifier seam for quest notification intent, but it does not invoke real `onItemGet` or `onItemRemoved` equivalents by default.
- C# does not yet model the Java distinction between get-item handler dispatch and nearby-quest refresh.
- C# now has the `questUpdateItems` static-data membership projection, a no-op nearby-refresh planner, a no-op dispatcher seam, `SmNearbyQuests` packet serialization, minimal world-instance quest-id storage, staged `QuestNpc.onQuestStart` table storage, a pure XML quest-start extractor, a conservative Java handler quest-start extractor, an offline source loader, pinned real-data source/table audits with zero unresolved handler rows, staged world-instance quest-id projection, a source audit for nearby start-condition filtering, a staged partial predicate with XML and inventory subset support, reward-group hydration, a staged quest-template XML extractor, a real-data quest-template extractor audit, a staged marker projection bridge, a send-boundary audit, a supported-template real-data projection audit, and a non-sending refresh-plan boundary, but it still lacks `questItems` get-handler registration, production extractor loader integration, full executable start-condition filtering, level-ready nearby marker sends, delayed NPC-spawn refresh fanout, and any dispatcher that invokes real nearby-quest refresh.
- Target add callback must remain CUBE/actor-backed and must occur after storage update packet semantics are preserved.
- Remove callback must fire only for material/base deletes, not partial count updates.

Safe quest next tests:

- Add a disabled or no-op `IQuestItemMutationNotifier` seam behind explicit opt-in live execution only, preserving automatic dispatch disabled.
- Continue toward NPC-faction predicate support or broader refresh-plan audits before any player-controller refresh adapter; keep it disconnected from production ItemPurification dispatch before invoking any live quest handlers.

## Readiness Impact

Automatic `CM_ITEM_PURIFICATION` production dispatch remains blocked.

The next implementation units should prefer narrow opt-in tests and pure projection seams before any live production wiring:

1. AP packet/side-effect projection tests.
2. Quest notification projection tests.
3. Optional opt-in no-op dispatcher interfaces with tests.
4. Java observer artifact generation when Java 25/Maven tooling is available.

Do not mark any AP or quest side effect as verified parity until Java runtime artifacts, deterministic Java comparison, or equivalent objective validation exists.
