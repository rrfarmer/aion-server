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
- Partial material count updates do not need item-remove callbacks under Java behavior.
- Live mutation replaces the player inventory snapshot and applies AP, but intentionally leaves persistence, sends, quest callbacks, and rollback outside its boundary.
- Production `HandleInfrastructurePacketAsync` still routes `CmItemPurification` to the plan-only `HandleItemPurificationAsync` path.
- No C# quest item get/remove dispatcher is wired into ItemPurification execution; the UOW-967 projection is metadata only.

Quest parity gaps:

- C# has metadata, a pure ordered projection, and an opt-in no-op notifier seam for quest notification intent, but it does not invoke real `onItemGet` or `onItemRemoved` equivalents by default.
- C# does not yet model the Java distinction between get-item handler dispatch and nearby-quest refresh.
- C# now has the `questUpdateItems` static-data membership projection and a no-op nearby-refresh planner, but it still lacks `questItems` get-handler registration and any dispatcher that invokes nearby-quest refresh.
- Target add callback must remain CUBE/actor-backed and must occur after storage update packet semantics are preserved.
- Remove callback must fire only for material/base deletes, not partial count updates.

Safe quest next tests:

- Add a disabled or no-op `IQuestItemMutationNotifier` seam behind explicit opt-in live execution only, preserving automatic dispatch disabled.
- Add an opt-in nearby-refresh dispatcher interface that can consume `ItemPurificationNearbyQuestRefreshPlan`, keeping the default implementation no-op before invoking any live quest handlers.

## Readiness Impact

Automatic `CM_ITEM_PURIFICATION` production dispatch remains blocked.

The next implementation units should prefer narrow opt-in tests and pure projection seams before any live production wiring:

1. AP packet/side-effect projection tests.
2. Quest notification projection tests.
3. Optional opt-in no-op dispatcher interfaces with tests.
4. Java observer artifact generation when Java 25/Maven tooling is available.

Do not mark any AP or quest side effect as verified parity until Java runtime artifacts, deterministic Java comparison, or equivalent objective validation exists.
