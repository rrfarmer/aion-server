package com.aionemu.gameserver.services.toypet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * Disabled-by-default construction context seam for pet-feed unusual-storage parity artifacts.
 */
public final class PetFeedUnusualStorageArtifactCapture {

	private static final int MAX_PENDING_CONTEXTS_PER_PLAYER = 4;
	private static final ConcurrentHashMap<Integer, Deque<CaptureContext>> pendingContexts = new ConcurrentHashMap<>();
	private static final ServerPacketCaptureObserver observer = new ServerPacketCaptureObserver() {

		@Override
		public boolean isEnabled() {
			return PetFeedUnusualStorageArtifactCapture.isEnabled();
		}

		@Override
		public void onPacketSerialized(AionConnection con, AionServerPacket packet, ByteBuffer clearFrame) {
			try {
				observePacketSerialized(con, packet, clearFrame);
			} catch (RuntimeException e) {
				// Capture must never affect packet dispatch.
			}
		}
	};

	private static volatile boolean enabled;

	private PetFeedUnusualStorageArtifactCapture() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static ServerPacketCaptureObserver observer() {
		return observer;
	}

	public static void registerStorageUpdate(Player player, StorageType storageType, Item item, ItemAddType addType) {
		if (!enabled || player == null || item == null || addType != ItemAddType.ALL_SLOT || !isUnusualStorage(storageType))
			return;

		// Java parity: ItemPacketService.sendStorageUpdatePacket chooses SM_WAREHOUSE_ADD_ITEM
		// followed by SM_CUBE_UPDATE.cubeSize(storageType, player). Future artifact capture will
		// record the construction context here and pair it with AionServerPacket serialization bytes.
		Deque<CaptureContext> contexts = getPendingContexts(player.getObjectId());
		synchronized (contexts) {
			while (contexts.size() >= MAX_PENDING_CONTEXTS_PER_PLAYER)
				contexts.removeFirst();
			contexts.addLast(new CaptureContext(storageType.getId(), storageType.ordinal(), item.getObjectId()));
		}
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

	private static Deque<CaptureContext> getPendingContexts(int playerObjectId) {
		Deque<CaptureContext> newContexts = new ArrayDeque<>();
		Deque<CaptureContext> existingContexts = pendingContexts.putIfAbsent(playerObjectId, newContexts);
		return existingContexts == null ? newContexts : existingContexts;
	}

	private static void observePacketSerialized(AionConnection con, AionServerPacket packet, ByteBuffer clearFrame) {
		if (!enabled || con == null || packet == null || clearFrame == null)
			return;
		Player player = con.getActivePlayer();
		if (player == null)
			return;
		Deque<CaptureContext> contexts = pendingContexts.get(player.getObjectId());
		if (contexts == null)
			return;
		synchronized (contexts) {
			CaptureContext context = contexts.peekFirst();
			if (context == null)
				return;
			if (context.advance(packet) && context.isComplete())
				contexts.removeFirst();
			if (contexts.isEmpty())
				pendingContexts.remove(player.getObjectId(), contexts);
		}
	}

	private static final class CaptureContext {

		private final int storageTypeId;
		private final int storageTypeOrdinal;
		private final int itemObjectId;
		private int nextPacketIndex;

		private CaptureContext(int storageTypeId, int storageTypeOrdinal, int itemObjectId) {
			this.storageTypeId = storageTypeId;
			this.storageTypeOrdinal = storageTypeOrdinal;
			this.itemObjectId = itemObjectId;
		}

		private boolean advance(AionServerPacket packet) {
			if (nextPacketIndex == 0 && packet instanceof SM_WAREHOUSE_ADD_ITEM) {
				nextPacketIndex++;
				return true;
			}
			if (nextPacketIndex == 1 && packet instanceof SM_CUBE_UPDATE) {
				nextPacketIndex++;
				return true;
			}
			return false;
		}

		private boolean isComplete() {
			return nextPacketIndex >= 2;
		}
	}
}
