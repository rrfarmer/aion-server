# Phase 6 - Pet Feed Rejected-Food Unlock Metadata

Date: May 27, 2026
Unit of Work: UOW-1328

## Scope

This unit adds the first non-live packet metadata boundary for Java `ItemPacketService.sendItemUnlockPacket` as used by `PetService.checkFeeding` rejected-food handling.

Java source of truth:

- `com.aionemu.gameserver.services.toypet.PetService.checkFeeding`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket`
- `com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM`
- `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE`

## Implemented

- Added `SmInventoryAddItem.AllSlot = 0x13`.
- Added `SmInventoryAddItem.CreateAllSlot(...)` for Java `ItemAddType.ALL_SLOT`.
- Extended `PetFeedSupplementalPacketContext` with optional unlock packet context.
- Added `PetFeedUnlockPacketStorageKind` and `PetFeedUnlockPacketContext`.
- Extended `PetFeedPacketMetadataResult` with a packet sequence so one Java operation can describe the normal-cube unlock pair.
- Constructed non-sending normal-cube unlock metadata as:
  1. `SmInventoryAddItem.CreateAllSlot(item, template)`
  2. `SmCubeUpdate.CubeSizeSnapshot(...)`
- Kept warehouse/account/legion warehouse unlock paths blocked until corresponding warehouse packet metadata exists.

## Not Implemented

- No live inventory mutation.
- No live packet sends.
- No live item/template/player hydration.
- No warehouse unlock packet metadata.
- No `SM_WAREHOUSE_ADD_ITEM` C# equivalent.
- No Java runtime packet byte comparison.
- No attempt to resolve item name localization beyond existing `ItemTemplateSummary.GetClientName()` behavior.

## Validation

- `dotnet test dotnetConversion/tests/Aion.GameServer.Tests/Aion.GameServer.Tests.csproj --filter "PetFeedPacketMetadataBridge|SmInventoryAddItem|SmCubeUpdate"` passed 8 tests.

## Migration Parity Table - UOW-1328

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.services.item.ItemPacketService.sendItemUnlockPacket` | `Aion.GameServer.Services.ToyPet.PetFeedPacketMetadataBridge.ConstructFoodItemUnlock` | Service / Packet Metadata Boundary | Partial | Unit Tested | Partial Parity | Normal cube storage can now produce the non-sending Java packet sequence. Null storage, warehouse paths, live storage lookup, and live sends remain unsupported. |
| `com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType.ALL_SLOT` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.AllSlot` | Packet Constant | Complete | Unit Tested | Partial Parity | Preserves Java mask `0x13`; test serializes the C# packet and asserts the mask. No Java runtime packet vector was compared. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_ADD_ITEM` | `Aion.GameServer.Network.Aion.ServerPackets.SmInventoryAddItem.CreateAllSlot` | Server Packet | Partial | Unit Tested | Partial Parity | Constructs the normal-cube unlock add packet using existing item/template snapshots. Java `ITEM_COLLECT` partial-slot remapping is unrelated and still not expanded here. Client-name serialization depends on existing `GetClientName()` l10n behavior. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE.cubeSize(StorageType.CUBE, Player)` | `Aion.GameServer.Network.Aion.ServerPackets.SmCubeUpdate.CubeSizeSnapshot` | Server Packet | Partial | Unit Tested | Partial Parity | Uses supplied post-mutation cube count/expand snapshots to preserve packet shape without live player/storage hydration. Snapshot accuracy remains caller-owned. |
| `com.aionemu.gameserver.services.toypet.PetService.checkFeeding` rejected-food packet order | `PetFeedPacketMetadataBridge` packet sequence for `UnlockFoodItem` before `SmPet` subtype `5` | Packet Metadata Composition | Partial | Unit Tested | Partial Parity | Unit test confirms unlock metadata contains inventory-add then cube-update and appears before `SmPet` end-feed metadata when context is supplied. No live dispatch or Java runtime order capture exists. |
| `com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM` | none identified | Packet Dependency | Not Started | Unit Tested as blocked | Needs Verification | Warehouse, account warehouse, and legion warehouse unlock paths remain explicitly blocked. C# has no equivalent packet in this unit. |

## Tests Added

| Test Name | Type | Java Behavior Source | What It Validates | Parity Evidence | Gaps |
|---|---|---|---|---|---|
| `Construct_RejectedFoodWithNormalCubeUnlockContextBuildsUnlockPacketsBeforeSmPetLikeJava` | Unit | `ItemPacketService.sendItemUnlockPacket`, `sendStorageUpdatePacket`, `SM_INVENTORY_ADD_ITEM`, `SM_CUBE_UPDATE` | Supplied normal-cube unlock context emits `SmInventoryAddItem` with mask `0x13`, then `SmCubeUpdate`, before `SmPet` rejected-food end packet. | Source-derived serialization assertions against C# packet payloads. | No Java runtime byte/order comparison; supplied snapshots may differ from live Java storage state. |
| `Construct_RejectedFoodUnlockKeepsWarehouseStorageBlockedUntilWarehousePacketExists` | Unit | `ItemPacketService.sendStorageUpdatePacket` non-cube branch | Warehouse unlock context remains blocked instead of guessing a missing packet. | Explicit blocked boundary assertion. | Does not implement `SM_WAREHOUSE_ADD_ITEM`. |

## Remaining Risks

- Java runtime packet bytes and queue ordering have not been captured.
- Live `StorageType.getStorageTypeById(item.getItemLocation())` is not wired.
- Supplied item/template/cube snapshots can be stale if a future live adapter captures them at the wrong point.
- Warehouse and legion warehouse unlocks remain blocked.
- C# packet item names continue to use existing `ItemTemplateSummary.GetClientName()` l10n behavior; this unit does not validate localized Java `ItemTemplate.getL10n()` output.
- No socket dispatch, inventory mutation, scheduler, reward creation, DAO writes, or feed task execution is enabled.

## Next Recommended Unit of Work

Audit and design the missing warehouse unlock packet boundary for rejected-food handling: Java uses `SM_WAREHOUSE_ADD_ITEM` plus `SM_CUBE_UPDATE` for non-cube storage, with a legion-warehouse kinah special case. Keep it read-only or non-live until the C# warehouse packet shape and storage snapshots are explicit.
