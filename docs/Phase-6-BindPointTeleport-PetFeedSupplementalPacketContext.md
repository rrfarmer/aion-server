# Phase 6 Pet Feed Supplemental Packet Context

Date: May 27, 2026
Unit of Work: UOW-1327
Status: Supplied-context pet feed emotion and system-message metadata added; live dispatch remains disabled.

## Scope

This unit extends the non-sending feed packet metadata bridge so Java packet gaps that only needed supplied context can now be represented.

It covers:

- optional supplied player object id and creature-state context for `SM_EMOTION(END_FEEDING)`
- optional supplied pet name and localized item name for `STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR`
- preserving blocked metadata when the supplied context is absent
- keeping item unlock metadata blocked after read-only audit confirmed Java expands it through storage update packets
- recording read-only refeed-delay audit risk for `SM_PET` FOOD subtype `7`

It does not send packets, hydrate live players, localize item names, construct item unlock storage packets, mutate inventory, schedule tasks, create reward items, write DAO state, or compare Java runtime packet bytes.

## Migration Parity Table - UOW-1327

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` end-feeding branch | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge` + `SmEmotion` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending `SmEmotion` `EndFeeding` metadata when supplied a player object id. Live player hydration, visible-player broadcast/send semantics, and Java runtime packet comparison remain unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR` | `PetFeedPacketMetadataBridge` + `SmSystemMessage(1400618, petName, itemName)` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending rejected-food system-message metadata from supplied pet name and localized item name. Live `pet.getName()`, `item.getItemTemplate().getL10n()`, localization correctness, and Java runtime packet comparison remain unverified. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected/reward packet order | `PetFeedPacketMetadataBridge.Construct` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Preserves operation order while replacing context-only blocked entries with packet metadata when supplied. Unlock packet remains blocked and non-packet operations remain skipped. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedPacketMetadataResultStatus.BlockedItemUnlockPacket` | Packet Gap Marker | Not Started | Manual Only | Needs Verification | Read-only audit confirmed Java expands unlock to storage update packets: cube uses `SM_INVENTORY_ADD_ITEM` with `ItemAddType.ALL_SLOT` then `SM_CUBE_UPDATE`; non-cube uses warehouse/legion variants. C# still marks this as blocked rather than guessing. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` `ItemAddType.ALL_SLOT` | future C# unlock metadata boundary | Packet Dependency | Not Started | Manual Only | Needs Verification | Newly discovered dependency for normal cube-item unlock. C# has partial `SmInventoryAddItem`, but no `AllSlot = 0x13` helper was added in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | none identified | Packet Dependency | Not Started | Manual Only | Unknown | Newly discovered dependency for non-cube unlock paths. No C# equivalent was found during read-only audit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `7` mutable queue behavior | `PetFeedPacketMetadataBridge` supplied `RefeedDelaySeconds` | Packet Timing Boundary | Partial | Manual Only | Needs Verification | Read-only audit found Java queues packet objects before `setRefeedTime` and `progress.reset`, but serializes later from mutable `commonData`. Exact subtype `7` delay/progress bytes need runtime vector evidence or an explicit C# snapshot policy. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithSuppliedContextBuildsEmotionAndSystemMessageMetadata` | Unit | `PetService.checkFeeding`, `SM_EMOTION`, `SM_SYSTEM_MESSAGE` | Rejected-food metadata keeps unlock blocked but constructs `SmPet`, `SmEmotion`, and `SmSystemMessage(1400618)` when supplied context exists. | Source-derived deterministic assertion. | Does not serialize/compare Java runtime bytes. |
| `Construct_RewardedFeedWithSuppliedPlayerContextBuildsEndFeedingEmotionMetadata` | Unit | `PetService.checkFeeding` rewarded branch | Rewarded-feed packet metadata constructs end-feeding `SmEmotion` between `SmPet` progress/refeed packet metadata when player context exists. | Source-derived deterministic assertion. | No live player hydration or socket dispatch. |

## Remaining Risks

- The bridge constructs packet objects but never sends them.
- Item unlock remains blocked; Java storage update behavior is broader than `SmPet`.
- Pet name and item name are supplied and not hydrated from live `Pet` / item template data.
- End-feeding `SmEmotion` uses supplied player object id/state; live player context and broadcast/send target semantics are not wired.
- `SmSystemMessage` id/parameter shape is source-derived, but runtime localized text and Java packet bytes are not compared.
- Refeed-delay seconds remain supplied; live `PetCommonData.getRefeedDelay()` is not wired.
- Java mutable queued packet behavior for subtype `7` can observe state after enqueue; C# currently freezes supplied snapshot values.
- Java runtime packet byte comparison remains unavailable.

## Summary Metrics

- Total Java artifacts discovered: 7 grouped artifact rows in this unit
- Total artifacts ported: supplied-context metadata for 2 packet gaps and 2 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 7 grouped rows
- Total blocked artifacts: item unlock storage packet construction, live player/pet/item context hydration, live send dispatch, refeed-delay hydration, Java runtime packet comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Implement a non-live rejected-food unlock packet metadata boundary for normal cube inventory items: add/consume `SM_INVENTORY_ADD_ITEM` `ALL_SLOT = 0x13` metadata plus `SM_CUBE_UPDATE` metadata ahead of `SmPet` subtype `5`, while keeping warehouse unlocks, live inventory mutation, live sends, scheduler, DAO, and reward creation disabled. Keep subtype `7` runtime-vector design on deck before making stronger refeed-delay parity claims.
