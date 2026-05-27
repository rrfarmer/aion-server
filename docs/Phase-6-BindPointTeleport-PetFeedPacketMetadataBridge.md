# Phase 6 Pet Feed Packet Metadata Bridge

Date: May 27, 2026
Unit of Work: UOW-1326
Status: Non-sending packet metadata bridge added for pet feed operation plans; live dispatch remains disabled.

## Scope

This unit translates `PetFeedServiceOperationPlan` packet-facing operations into concrete non-sending `SmPet.Food(...)` packet instances where the C# packet surface already exists.

It covers:

- `SM_PET` food subtype `2` progress/success metadata
- `SM_PET` food subtype `5` end/clean-feed-task metadata
- `SM_PET` food subtype `6` reward-item metadata
- `SM_PET` food subtype `7` present/refeed notification metadata
- explicit blocked metadata for item unlock, end-feeding emotion, and rejected-food system message gaps
- skipped metadata for non-packet operation intents such as inventory, scheduler, DAO, reward item, and reset operations

It does not send packets, construct item unlock packets, construct `SM_EMOTION` without a player context, construct localized system messages, execute inventory mutations, schedule tasks, create reward items, write DAO state, or validate Java runtime packet bytes.

## Migration Parity Table - UOW-1326

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `2` | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge` + `SmPet.Food` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending `SmPet` metadata for feed-progress/eating-success operation intents. Java runtime serialization comparison is not run in this unit. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `5` | `PetFeedPacketMetadataBridge` + `SmPet.Food` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending clean-feed/end packet metadata. Refeed-delay source is supplied; live `PetCommonData.getRefeedDelay()` is not wired. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `6` | `PetFeedPacketMetadataBridge` + `SmPet.Food` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending reward-item packet metadata from operation item id. Live reward item creation remains separate. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_PET` FOOD subtype `7` | `PetFeedPacketMetadataBridge` + `SmPet.Food` | Packet Metadata Bridge | Partial | Unit Tested | Partial Parity | Constructs non-sending present/refeed packet metadata with supplied delay seconds. Java send timing relative to `scheduleRefeed` remains unverified. |
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `PetFeedPacketMetadataResultStatus.BlockedItemUnlockPacket` | Packet Gap Marker | Not Started | Unit Tested | Needs Verification | Explicitly records that rejected-food unlock packets are outside `SmPet` construction. Concrete unlock packet/service behavior remains unported. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION` end-feeding branch | `PetFeedPacketMetadataResultStatus.BlockedEmotionContext` | Packet Gap Marker | Partial | Unit Tested | Needs Verification | Existing `SmEmotion` supports end feeding when a player context exists, but this bridge does not hydrate that context. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_MSG_TOYPET_FEED_FOOD_NOT_LOVEFLAVOR` | `PetFeedPacketMetadataResultStatus.BlockedSystemMessageContext` | Packet Gap Marker | Partial | Unit Tested | Needs Verification | Records missing pet name and localized item name context for rejected-food message construction. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` packet send order | `PetFeedPacketMetadataBridge.Construct` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Preserves operation-plan order while constructing packets, blocking packet gaps, and skipping non-packet operations. Does not execute live sends. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_NoOperationsReportsNoOperationsAndNeverLive` | Unit | Cancelled feed operation plan | Empty plans return no packet metadata and stay non-live. | Source-derived deterministic assertion. | No live cancellation runtime. |
| `Construct_RejectedFoodBuildsSmPetEndAndReportsPacketGaps` | Unit | Java rejected-food packet order | Rejected food constructs `SM_PET(5)` metadata and marks unlock/emotion/system-message gaps. | Source-derived deterministic assertion. | Concrete unlock/emotion/system message packets not built. |
| `Construct_NotFullContinueBuildsProgressPacketAndSkipsNonPacketOperations` | Unit | Java accepted not-full branch | Progress packet metadata is built and inventory/scheduler operations are skipped as non-packet boundaries. | Source-derived deterministic assertion. | Packet bytes not compared in this test. |
| `Construct_RewardedFeedBuildsAllSmPetFeedPacketsAndMarksOtherBoundaries` | Unit | Java rewarded feed branch | Rewarded path constructs `SM_PET` subtype `2`, `6`, `5`, and `7` metadata in operation order while marking emotion/non-packet boundaries. | Source-derived deterministic assertion. | No item service, scheduler, DAO, reset execution, or runtime packet capture. |

## Remaining Risks

- The bridge constructs packet objects but never sends them.
- `ItemPacketService.sendItemUnlockPacket` remains a blocked packet/service gap.
- End-feeding `SmEmotion` needs supplied/live player context before bridge construction.
- Rejected-food system messages need pet name and localized item name context.
- Refeed-delay seconds are supplied; live `PetCommonData.getRefeedDelay()` is not wired.
- Java runtime packet byte comparison remains unavailable.

## Summary Metrics

- Total Java artifacts discovered: 8 grouped artifact rows in this unit
- Total artifacts ported: 1 packet metadata bridge, 2 metadata status enums, 3 metadata request/result DTOs, and 4 focused tests
- Total artifacts with verified parity: 0 in this unit
- Total artifacts needing verification: 8 grouped rows
- Total blocked artifacts: item unlock packet/service, emotion context hydration, localized system-message construction, live send dispatch, refeed-delay hydration, Java runtime packet comparison
- Estimated overall migration completion: Phase 6 remains about 71% complete

## Next Recommended Unit of Work

Add supplied-context packet metadata for the remaining feed packet gaps: end-feeding `SmEmotion` construction and rejected-food system-message descriptor construction. Keep unlock packets, inventory mutation, scheduler execution, DAO writes, reward item creation, and socket dispatch disabled.
