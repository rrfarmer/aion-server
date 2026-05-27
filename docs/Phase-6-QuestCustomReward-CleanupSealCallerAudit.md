# Phase 6 Quest/Custom Reward Cleanup-Seal Caller Audit

Date: 2026-05-27
Unit of Work: UOW-1410
Status: read-only audit complete; no runtime packet behavior changed. UOW-1411 implemented the pet-feed normal-cube unlock metadata follow-up identified by this audit.

## Scope

This audit checks whether quest completion item rewards or custom level rewards are the next safe cleanup/seal flag implementation target after world-loot item collection.

Java remains the source of truth. This unit does not assume parity and does not mark any quest/custom reward item path as verified.

## Java Source Of Truth

- `com.aionemu.gameserver.services.QuestService.finishQuest` builds reward item lists from fixed, selectable, extended, class-selectable, and bonus reward sources, then calls `ItemService.addItem(player, qi.getItemId(), qi.getCount(), true)` for each `QuestItems` row before non-item reward side effects.
- `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.giveQuestItem` calls `ItemService.addItem` with `ItemAddType.QUEST_WORK_ITEM` and `ItemUpdateType.INC_ITEM_COLLECT` for work-item grants.
- `com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.GiveItemOperation` calls `ItemService.addItem(env.getPlayer(), itemId, count, true)`.
- `com.aionemu.gameserver.services.BonusPackService.addPlayerCustomReward` and `FactionPackService.addPlayerCustomReward` send reward items through `SystemMailService.sendMail(...)`, so their item blobs are mail-attachment data rather than immediate inventory add/update packets.

## Current C# State

- `QuestFinishRewardTemplateXmlProjectionExtractor` projects quest reward item metadata from XML into fixed, selectable, extended, class-selectable, and bonus descriptors, but it does not mutate inventory or emit `SmInventoryAddItem` / `SmInventoryUpdateItem`.
- `QuestRewardService` currently implements non-item reward planning/execution surfaces such as XP, AP, GP, DP, kinah, title/cube/warehouse side effects. Its kinah packets use `SmInventoryUpdateItem.IncreaseKinahQuest`; there is no ordinary quest item-add/update packet sender in this service yet.
- Custom level rewards and starter-kit rewards route through `SystemMailRewardPlanService` / persistence operations. These create `PlayerMail` and attached `InventoryItem` payloads for storage/mail delivery, not immediate inventory add/update packets in the audited service layer.
- The live read-mail path was already wired in UOW-1399 to compute cleanup/seal flags when attached mail items are read and serialized.
- The remaining concrete default-zero inventory-add caller found during this audit is `PetFeedPacketMetadataBridge.ConstructFoodItemUnlock` for normal-cube unlock metadata: `PetFeedUnlockPacketContext` already carries `GeneralInfoWarehouseRestrictionFlag`, warehouse/unusual/legion branches use it, but the cube branch still calls `SmInventoryAddItem.CreateAllSlot(context.Item, context.Template)` without the flag.

## Audit Result

Quest/custom reward item paths should not be the next implementation slice until a live quest item reward packet sender exists or a dedicated quest item mutation planner is selected. The safer next concrete unit is the pet-feed normal-cube unlock metadata gap, because it is narrow, non-sending, already has a precomputed cleanup/seal flag in context, and shares the same Java source breadcrumb as the warehouse unlock branches.

## Migration Parity Table - UOW-1410

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.QuestService.finishQuest` item reward loop | `Aion.GameServer.Services.QuestFinishRewardTemplateXmlProjectionExtractor` and future quest item reward executor | Service / Reward Orchestration | Partial | Manual Only | Needs Verification | Java immediately calls `ItemService.addItem` for fixed/selectable/extended/class/bonus quest reward items. C# currently projects metadata and non-item reward side effects, but no audited live item add/update packet sender exists. Missing methods include concrete inventory mutation, persistence, packet fanout, selectable reward packet assertions, and cleanup/seal flag sourcing. |
| `com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler.giveQuestItem` | future quest work-item grant executor | Service / Quest Work Item | Not Started | No Tests | Needs Verification | Java work-item grants use `ItemAddType.QUEST_WORK_ITEM` with `ItemUpdateType.INC_ITEM_COLLECT`. C# has quest-drop/work-item planning surfaces, but no direct packet-equivalent grant path was changed or verified in this unit. |
| `com.aionemu.gameserver.questEngine.handlers.models.xmlQuest.operations.GiveItemOperation` | future XML quest give-item operation executor | Service / XML Quest Operation | Not Started | No Tests | Needs Verification | Java XML quest operation delegates to `ItemService.addItem`; C# operation execution remains gated/non-live in the audited docs and services. |
| `com.aionemu.gameserver.services.BonusPackService.addPlayerCustomReward` | `Aion.GameServer.Services.CustomLevelRewardPlanService`, `SystemMailRewardPlanService`, and persistence services | Service / Custom Reward Mail | Partial | Unit Tested | Partial Parity | C# custom rewards plan system mail and attached items; UOW-1399 already covered live read-mail cleanup/seal serialization. Sending/persistence fanout remains opt-in/gated and not Java runtime verified. |
| `com.aionemu.gameserver.services.FactionPackService.addPlayerCustomReward` | `Aion.GameServer.Services.CustomLevelRewardPlanService`, `SystemMailRewardPlanService`, and persistence services | Service / Custom Reward Mail | Partial | Unit Tested | Partial Parity | C# has faction reward planning/mail surfaces, but live execution remains gated. Race filtering and mail persistence have source-derived tests, not Java runtime packet comparison. |
| `com.aionemu.gameserver.services.toypet.PetService` rejected-food unlock packet path / `ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge.ConstructFoodItemUnlock` | Service / Packet Metadata | Partial | No Tests For Cube Cleanup Flag | Needs Verification | Newly discovered implementation candidate: `PetFeedUnlockPacketContext.GeneralInfoWarehouseRestrictionFlag` is passed to warehouse/unusual/legion unlock packet branches, but normal-cube `SmInventoryAddItem.CreateAllSlot` still defaults to cleanup/seal flag `0`. |

## Tests Added Or Updated

No tests were added in this read-only audit unit.

Existing relevant coverage:

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `QuestFinishRewardTemplateXmlProjectionExtractorTests` | Unit / projection | `QuestService.getRewardItems` | Existing tests cover XML reward item projection metadata. | Source-derived C# metadata assertions. | Does not mutate inventory or serialize packets. |
| `CustomLevelRewardExecutionServiceTests` and `SystemMailReward*Tests` | Unit / persistence planning | `BonusPackService`, `FactionPackService`, `SystemMailService.sendMail` | Existing tests cover custom reward mail planning and persistence operation ordering. | Source-derived C# assertions. | Does not compare Java runtime mail packets; live persistence remains opt-in/gated. |
| `GamePacketTests.SmInventoryAddItem_WritesCleanupSealFlagInItemBlobLikeJava` | Regression / packet byte layout | `SM_INVENTORY_ADD_ITEM.writeItemInfo`, `GeneralInfoBlobEntry` | Existing packet guard confirms supplied cleanup/seal flag `3` reaches the full item blob. | C# packet-byte assertion against deterministic Java source order/field value. | Does not prove any quest/custom caller supplies the flag. |

## Remaining Risks

- Quest reward item execution is broad: fixed/selectable/extended/class/bonus rewards, inventory capacity, repeat behavior, quest-state ordering, persistence, and nearby quest callback ordering all need explicit C# homes before live packet wiring.
- Custom reward mail paths are not immediate inventory add/update packets; cleanup/seal must be validated at mail read/send serialization boundaries, not by assuming `ItemService.addItem` behavior.
- The pet-feed normal-cube unlock gap is non-sending metadata today. It is a good narrow cleanup/seal unit but still needs focused packet metadata tests.
- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- Temporary-exchange remaining seconds, runtime conditioning presence, and time-normalized expiration/dye comparison remain unresolved item-blob risks.

## Next Recommended Unit Of Work

UOW-1411 completed the immediate pet-feed normal-cube unlock metadata follow-up. The next cleanup/seal unit should search remaining `SmInventoryAddItem.CreateItemCollect` / `CreateAllSlot` and `SmInventoryUpdateItem` full-blob callers for default flag paths, then wire the smallest remaining caller with deterministic static-data or precomputed flag access.
