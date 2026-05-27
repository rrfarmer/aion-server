package com.aionemu.gameserver.services.toypet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.capture.ServerPacketCaptureObserver;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobEntryMetadata;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * Disabled-by-default construction context seam for pet-feed unusual-storage parity artifacts.
 */
public final class PetFeedUnusualStorageArtifactCapture {

	private static final long MAX_PENDING_CONTEXT_AGE_MILLIS = 30000;
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

	private PetFeedUnusualStorageArtifactCapture() {
	}

	public static boolean isEnabled() {
		return PetFeedUnusualStorageArtifactCaptureConfig.ENABLED;
	}

	public static ServerPacketCaptureObserver observer() {
		return observer;
	}

	public static void registerStorageUpdate(Player player, StorageType storageType, Item item, ItemAddType addType) {
		if (!isEnabled() || player == null || item == null || addType != ItemAddType.ALL_SLOT || !isUnusualStorage(storageType))
			return;

		// Java parity: ItemPacketService.sendStorageUpdatePacket chooses SM_WAREHOUSE_ADD_ITEM
		// followed by SM_CUBE_UPDATE.cubeSize(storageType, player). Future artifact capture will
		// record the construction context here and pair it with AionServerPacket serialization bytes.
		Deque<CaptureContext> contexts = getPendingContexts(player.getObjectId());
		long now = System.currentTimeMillis();
		synchronized (contexts) {
			removeExpiredContexts(contexts, now);
			int maxContexts = getMaxPendingContextsPerPlayer();
			while (contexts.size() >= maxContexts)
				contexts.removeFirst();
			contexts.addLast(new CaptureContext(storageType.getId(), storageType.ordinal(), item.getObjectId(), now,
				PetFeedUnusualStorageArtifactCaptureConfig.ALLOWED_SCENARIO, PetFeedUnusualStorageArtifactCaptureConfig.OUTPUT_DIR,
				ItemBlobSnapshot.from(player, item)));
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

	private static int getMaxPendingContextsPerPlayer() {
		return Math.max(1, PetFeedUnusualStorageArtifactCaptureConfig.MAX_PENDING_CONTEXTS_PER_PLAYER);
	}

	private static void observePacketSerialized(AionConnection con, AionServerPacket packet, ByteBuffer clearFrame) {
		if (!isEnabled() || con == null || packet == null || clearFrame == null)
			return;
		Player player = con.getActivePlayer();
		if (player == null)
			return;
		Deque<CaptureContext> contexts = pendingContexts.get(player.getObjectId());
		if (contexts == null)
			return;
		synchronized (contexts) {
			removeExpiredContexts(contexts, System.currentTimeMillis());
			CaptureContext context = contexts.peekFirst();
			if (context == null)
				return;
			if (context.advance(packet, clearFrame) && context.isComplete()) {
				contexts.removeFirst();
				onSnapshotReady(context.toSnapshot(System.currentTimeMillis()));
			}
			if (contexts.isEmpty())
				pendingContexts.remove(player.getObjectId(), contexts);
		}
	}

	private static void removeExpiredContexts(Deque<CaptureContext> contexts, long now) {
		while (!contexts.isEmpty() && contexts.peekFirst().isExpired(now))
			contexts.removeFirst();
	}

	private static void onSnapshotReady(ArtifactSnapshot snapshot) {
		// Future artifact writer boundary. Intentionally no-op while capture remains disabled.
	}

	private static final class CaptureContext {

		private final int storageTypeId;
		private final int storageTypeOrdinal;
		private final int itemObjectId;
		private final long registeredAtMillis;
		private final String scenarioName;
		private final String outputDirectory;
		private final ItemBlobSnapshot constructionTimeItemBlob;
		private final PacketSnapshot[] packets = new PacketSnapshot[2];
		private int nextPacketIndex;

		private CaptureContext(int storageTypeId, int storageTypeOrdinal, int itemObjectId, long registeredAtMillis, String scenarioName,
			String outputDirectory, ItemBlobSnapshot constructionTimeItemBlob) {
			this.storageTypeId = storageTypeId;
			this.storageTypeOrdinal = storageTypeOrdinal;
			this.itemObjectId = itemObjectId;
			this.registeredAtMillis = registeredAtMillis;
			this.scenarioName = scenarioName;
			this.outputDirectory = outputDirectory;
			this.constructionTimeItemBlob = constructionTimeItemBlob;
		}

		private boolean advance(AionServerPacket packet, ByteBuffer clearFrame) {
			if (nextPacketIndex == 0 && packet instanceof SM_WAREHOUSE_ADD_ITEM) {
				packets[nextPacketIndex] = PacketSnapshot.from(nextPacketIndex, packet, clearFrame);
				nextPacketIndex++;
				return true;
			}
			if (nextPacketIndex == 1 && packet instanceof SM_CUBE_UPDATE) {
				packets[nextPacketIndex] = PacketSnapshot.from(nextPacketIndex, packet, clearFrame);
				nextPacketIndex++;
				return true;
			}
			return false;
		}

		private boolean isComplete() {
			return nextPacketIndex >= 2;
		}

		private boolean isExpired(long now) {
			return now >= registeredAtMillis && now - registeredAtMillis > MAX_PENDING_CONTEXT_AGE_MILLIS;
		}

		private ArtifactSnapshot toSnapshot(long completedAtMillis) {
			return new ArtifactSnapshot(scenarioName, outputDirectory, storageTypeId, storageTypeOrdinal, itemObjectId, registeredAtMillis,
				completedAtMillis, constructionTimeItemBlob, packets[0], packets[1]);
		}
	}

	private static final class ArtifactSnapshot {

		private final String scenarioName;
		private final String outputDirectory;
		private final int storageTypeId;
		private final int storageTypeOrdinal;
		private final int itemObjectId;
		private final long registeredAtMillis;
		private final long completedAtMillis;
		private final ItemBlobSnapshot constructionTimeItemBlob;
		private final PacketSnapshot warehouseAddPacket;
		private final PacketSnapshot cubeUpdatePacket;

		private ArtifactSnapshot(String scenarioName, String outputDirectory, int storageTypeId, int storageTypeOrdinal, int itemObjectId,
			long registeredAtMillis, long completedAtMillis, ItemBlobSnapshot constructionTimeItemBlob, PacketSnapshot warehouseAddPacket,
			PacketSnapshot cubeUpdatePacket) {
			this.scenarioName = scenarioName;
			this.outputDirectory = outputDirectory;
			this.storageTypeId = storageTypeId;
			this.storageTypeOrdinal = storageTypeOrdinal;
			this.itemObjectId = itemObjectId;
			this.registeredAtMillis = registeredAtMillis;
			this.completedAtMillis = completedAtMillis;
			this.constructionTimeItemBlob = constructionTimeItemBlob;
			this.warehouseAddPacket = warehouseAddPacket;
			this.cubeUpdatePacket = cubeUpdatePacket;
		}
	}

	private static final class ItemBlobSnapshot {

		private final int totalPayloadSize;
		private final List<ItemBlobEntrySnapshot> entries;

		private ItemBlobSnapshot(int totalPayloadSize, List<ItemBlobEntrySnapshot> entries) {
			this.totalPayloadSize = totalPayloadSize;
			this.entries = Collections.unmodifiableList(entries);
		}

		private static ItemBlobSnapshot from(Player player, Item item) {
			return from(ItemInfoBlob.getFullBlob(player, item));
		}

		private static ItemBlobSnapshot from(ItemInfoBlob blob) {
			if (blob == null)
				return null;
			List<ItemBlobEntrySnapshot> entries = new ArrayList<>();
			for (ItemBlobEntryMetadata metadata : blob.getBlobEntryMetadata())
				entries.add(new ItemBlobEntrySnapshot(metadata.getEntryName(), metadata.getEntryId(), metadata.getPayloadSize()));
			return new ItemBlobSnapshot(blob.size(), entries);
		}
	}

	private static final class ItemBlobEntrySnapshot {

		private final String entryName;
		private final int entryId;
		private final int payloadSize;

		private ItemBlobEntrySnapshot(String entryName, int entryId, int payloadSize) {
			this.entryName = entryName;
			this.entryId = entryId;
			this.payloadSize = payloadSize;
		}
	}

	private static final class PacketSnapshot {

		private final int packetIndex;
		private final String packetClassName;
		private final int clearFrameLength;
		private final int encodedOpcode;
		private final int remainingBytesAtObserver;
		private final ItemBlobSnapshot observedItemBlob;

		private PacketSnapshot(int packetIndex, String packetClassName, int clearFrameLength, int encodedOpcode,
			int remainingBytesAtObserver, ItemBlobSnapshot observedItemBlob) {
			this.packetIndex = packetIndex;
			this.packetClassName = packetClassName;
			this.clearFrameLength = clearFrameLength;
			this.encodedOpcode = encodedOpcode;
			this.remainingBytesAtObserver = remainingBytesAtObserver;
			this.observedItemBlob = observedItemBlob;
		}

		private static PacketSnapshot from(int packetIndex, AionServerPacket packet, ByteBuffer clearFrame) {
			int clearFrameLength = clearFrame.limit() >= 2 ? clearFrame.getShort(0) & 0xFFFF : 0;
			int encodedOpcode = clearFrame.limit() >= 4 ? clearFrame.getShort(2) & 0xFFFF : 0;
			ItemBlobSnapshot observedItemBlob = null;
			if (packet instanceof SM_WAREHOUSE_ADD_ITEM)
				observedItemBlob = ItemBlobSnapshot.from(((SM_WAREHOUSE_ADD_ITEM) packet).getFirstItemInfoBlob());
			return new PacketSnapshot(packetIndex, packet.getClass().getName(), clearFrameLength, encodedOpcode, clearFrame.remaining(),
				observedItemBlob);
		}
	}
}
