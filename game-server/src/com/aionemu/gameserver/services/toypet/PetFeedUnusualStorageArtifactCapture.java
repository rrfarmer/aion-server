package com.aionemu.gameserver.services.toypet;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * Disabled-by-default construction context seam for pet-feed unusual-storage parity artifacts.
 */
public final class PetFeedUnusualStorageArtifactCapture {

	private static volatile boolean enabled;

	private PetFeedUnusualStorageArtifactCapture() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void registerStorageUpdate(Player player, StorageType storageType, Item item, ItemAddType addType) {
		if (!enabled || addType != ItemAddType.ALL_SLOT || !isUnusualStorage(storageType))
			return;

		// Java parity: ItemPacketService.sendStorageUpdatePacket chooses SM_WAREHOUSE_ADD_ITEM
		// followed by SM_CUBE_UPDATE.cubeSize(storageType, player). Future artifact capture will
		// record the construction context here and pair it with AionServerPacket serialization bytes.
	}

	public static boolean isUnusualStorage(StorageType storageType) {
		if (storageType == null)
			return false;
		int storageId = storageType.getId();
		return storageId >= StorageType.PET_BAG_MIN && storageId <= StorageType.PET_BAG_MAX
			|| storageId >= StorageType.HOUSE_WH_MIN && storageId <= StorageType.HOUSE_WH_MAX
			|| storageType == StorageType.BROKER
			|| storageType == StorageType.MAILBOX;
	}
}
