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
- `ItemPurificationLiveExecutionService.ExecuteAsync` sends the success message first, applies live mutation, then sends the concrete inventory/cube packet plan. It does not currently send `AbyssPointsAddPlan.PlayerPackets` at the AP operation point, broadcast rank update packets, execute rank-limited unequip, or refresh abyss skills.
- UOW-968 adds live-execution regression coverage proving a purification AP spend that drops rank still produces `AbyssPointsAddPlan` rank-change metadata (`SmSystemMessage`, `SmAbyssRank`, `SmAbyssRankUpdate`, rank-limit flag, abyss-skill flag), while the live execution packet sender still skips the AP metadata operation.
- `ItemPurificationPersistentLiveExecutionService.ExecuteAsync` persists inventory mutation and updated abyss rank together, but does not execute rank-change side-effect services.

AP parity gaps:

- AP spend packets are modeled but not emitted by ItemPurification live execution.
- Rank update broadcast is modeled but not emitted from ItemPurification live execution.
- Equipment rank-limit unequip has a C# service home but is not invoked from ItemPurification AP spend.
- Abyss skill refresh has a C# service home but is not invoked from ItemPurification AP spend.
- Legion contribution is correctly absent for spend.
- Siege callback is correctly absent for purification's plain AP spend path.
- Ranking cache behavior and AP/login rank-limited equipment passes remain outside this dispatch scope.

Safe AP next tests:

- Add live-execution packet ordering coverage before wiring sends: success system message, material mutations, AP spend system message/rank packet, base delete, target add, with Java artifacts still required before verified parity.
- Add an explicit side-effect executor only behind opt-in live execution, then cover equipment rank-limit and abyss-skill invocation without enabling production dispatch.

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
- Partial material count updates do not need item-remove callbacks under Java behavior.
- Live mutation replaces the player inventory snapshot and applies AP, but intentionally leaves persistence, sends, quest callbacks, and rollback outside its boundary.
- Production `HandleInfrastructurePacketAsync` still routes `CmItemPurification` to the plan-only `HandleItemPurificationAsync` path.
- No C# quest item get/remove dispatcher is wired into ItemPurification execution; the UOW-967 projection is metadata only.

Quest parity gaps:

- C# has metadata and a pure ordered projection for quest notification intent but does not invoke `onItemGet` or `onItemRemoved` equivalents.
- C# does not yet model the Java distinction between get-item handler dispatch and nearby-quest refresh.
- C# does not yet have a complete `questItems`/`questUpdateItems` projection from Java quest registration data for this path.
- Target add callback must remain CUBE/actor-backed and must occur after storage update packet semantics are preserved.
- Remove callback must fire only for material/base deletes, not partial count updates.

Safe quest next tests:

- Add a disabled or no-op `IQuestItemMutationNotifier` seam behind explicit opt-in live execution only, preserving automatic dispatch disabled.
- Add static-data tests for `questUpdateItems` projection before invoking any live quest handlers.

## Readiness Impact

Automatic `CM_ITEM_PURIFICATION` production dispatch remains blocked.

The next implementation units should prefer narrow opt-in tests and pure projection seams before any live production wiring:

1. AP packet/side-effect projection tests.
2. Quest notification projection tests.
3. Optional opt-in no-op dispatcher interfaces with tests.
4. Java observer artifact generation when Java 25/Maven tooling is available.

Do not mark any AP or quest side effect as verified parity until Java runtime artifacts, deterministic Java comparison, or equivalent objective validation exists.
