package com.aionemu.gameserver.services.toypet;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.configs.main.PetFeedUnusualStorageArtifactCaptureConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemStone;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.stats.container.PlumStatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
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
	private static final Object artifactQueueLock = new Object();
	private static final Deque<ArtifactSnapshot> queuedArtifacts = new ArrayDeque<>();
	private static final Object writerWorkerLock = new Object();
	private static volatile boolean writerWorkerRunning;
	private static Thread writerWorker;
	private static long droppedArtifactCount;
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

	public static void installIfEnabled() {
		if (!isEnabled())
			return;
		// Java parity artifact seam: future GameServer startup may install this after Config.load()
		// and before NIO startup. This method is intentionally not called by production startup yet.
		AionServerPacket.setCaptureObserver(observer());
		startWriterWorker();
	}

	public static void shutdown() {
		// Java parity artifact seam: future ShutdownHook wiring should reset the global packet observer
		// before NIO shutdown can emit disconnect/save packet fanout.
		AionServerPacket.setCaptureObserver(null);
		stopWriterWorker();
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
			contexts.addLast(new CaptureContext(player.getObjectId(), storageType.getId(), storageType.ordinal(), item.getObjectId(), now,
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

	private static int getMaxQueuedArtifacts() {
		return Math.max(1, PetFeedUnusualStorageArtifactCaptureConfig.MAX_QUEUED_ARTIFACTS);
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
		if (snapshot == null)
			return;
		synchronized (artifactQueueLock) {
			if (queuedArtifacts.size() >= getMaxQueuedArtifacts()) {
				droppedArtifactCount++;
				return;
			}
			queuedArtifacts.addLast(snapshot);
		}
		// Future artifact writer boundary. Intentionally no-op while capture remains disabled.
	}

	private static boolean drainQueuedArtifact() {
		ArtifactSnapshot snapshot;
		synchronized (artifactQueueLock) {
			snapshot = queuedArtifacts.pollFirst();
		}
		if (snapshot == null)
			return false;
		writeArtifact(snapshot);
		return true;
	}

	private static void writeArtifact(ArtifactSnapshot snapshot) {
		try {
			buildArtifactPath(snapshot);
			buildSchemaV1Artifact(snapshot);
		} catch (RuntimeException e) {
			// Capture artifact validation must never affect packet dispatch or writer lifecycle.
		}
		// Future JSON/file writer boundary. Intentionally no-op.
	}

	private static Map<String, Object> buildSchemaV1Artifact(ArtifactSnapshot snapshot) {
		Map<String, Object> artifact = orderedMap();
		artifact.put("schemaVersion", 1);
		artifact.put("scenario", snapshot.scenarioName);
		artifact.put("javaSources", List.of("com.aionemu.gameserver.services.toypet.PetService.checkFeeding",
			"com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket",
			"com.aionemu.gameserver.network.aion.serverpackets.SM_WAREHOUSE_ADD_ITEM",
			"com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE"));
		artifact.put("storage", buildStorageDto(snapshot));
		artifact.put("timing", buildTimingDto());
		artifact.put("constructionSnapshot", buildConstructionSnapshotDto(snapshot));
		artifact.put("encodeSnapshot", buildEncodeSnapshotDto(snapshot));
		artifact.put("packets", List.of(buildPacketDto(snapshot.warehouseAddPacket, snapshot), buildPacketDto(snapshot.cubeUpdatePacket, snapshot)));
		artifact.put("notes", List.of("itemBlob.hex is observer-time reserialization until a future packet-body slice verifier is added.",
			"JSON serialization and file output are intentionally disabled."));
		return artifact;
	}

	private static Map<String, Object> buildStorageDto(ArtifactSnapshot snapshot) {
		Map<String, Object> storage = orderedMap();
		storage.put("storageId", snapshot.storageTypeId);
		storage.put("storageTypeName", null);
		storage.put("storageTypeOrdinal", snapshot.storageTypeOrdinal);
		storage.put("expectedReachability", "delayed-mutable-item-reference");
		storage.put("normalUiFlow", false);
		return storage;
	}

	private static Map<String, Object> buildTimingDto() {
		Map<String, Object> timing = orderedMap();
		timing.put("feedItemLookupPhase", "pre-delay-cube-inventory");
		timing.put("unlockDecisionPhase", "post-delay-rejected-food");
		timing.put("packetConstructionPhase", "sendItemUnlockPacket/sendStorageUpdatePacket");
		timing.put("packetSerializationPhase", "AionConnection.writeData/AionServerPacket.write");
		timing.put("itemReferenceIsMutable", true);
		return timing;
	}

	private static Map<String, Object> buildConstructionSnapshotDto(ArtifactSnapshot snapshot) {
		Map<String, Object> construction = orderedMap();
		construction.put("warehouseType", snapshot.storageTypeId);
		construction.put("addType", ItemAddType.ALL_SLOT.name());
		construction.put("addTypeMask", ItemAddType.ALL_SLOT.getMask());
		construction.put("packetOrder", List.of("SM_WAREHOUSE_ADD_ITEM", "SM_CUBE_UPDATE"));
		return construction;
	}

	private static Map<String, Object> buildEncodeSnapshotDto(ArtifactSnapshot snapshot) {
		Map<String, Object> encode = orderedMap();
		encode.put("item", buildItemDto(snapshot));
		encode.put("itemBlob", buildItemBlobDto(getEncodeTimeItemBlob(snapshot)));
		return encode;
	}

	private static Map<String, Object> buildItemDto(ArtifactSnapshot snapshot) {
		EncodeTimeItemSnapshot itemSnapshot = getEncodeTimeItemSnapshot(snapshot);
		Map<String, Object> item = orderedMap();
		item.put("objectId", snapshot.itemObjectId);
		item.put("itemId", itemSnapshot == null ? null : itemSnapshot.itemId);
		item.put("count", itemSnapshot == null ? null : itemSnapshot.itemCount);
		item.put("itemLocation", itemSnapshot == null ? snapshot.storageTypeId : itemSnapshot.itemLocation);
		item.put("equipmentSlot", itemSnapshot == null ? null : itemSnapshot.equipmentSlot);
		item.put("itemTemplateId", itemSnapshot == null ? null : itemSnapshot.itemTemplateId);
		item.put("localizedName", itemSnapshot == null ? null : itemSnapshot.localizedName);
		item.put("packCount", itemSnapshot == null ? getPackCount(getEncodeTimeItemBlob(snapshot)) : itemSnapshot.packCount);
		item.put("expireTime", itemSnapshot == null ? null : itemSnapshot.expireTime);
		item.put("temporaryExchangeTime", itemSnapshot == null ? getTemporaryExchangeTime(getEncodeTimeItemBlob(snapshot))
			: itemSnapshot.temporaryExchangeTimeRemaining);
		item.put("charge", itemSnapshot == null ? getChargePoints(getEncodeTimeItemBlob(snapshot)) : itemSnapshot.chargePoints);
		item.put("enchantLevel", itemSnapshot == null ? getEnchantLevel(getEncodeTimeItemBlob(snapshot)) : itemSnapshot.enchantLevel);
		item.put("itemMask", itemSnapshot == null ? getItemMask(getEncodeTimeItemBlob(snapshot)) : itemSnapshot.itemMask);
		item.put("color", itemSnapshot == null ? getDyeColor(getEncodeTimeItemBlob(snapshot)) : itemSnapshot.color);
		return item;
	}

	private static Map<String, Object> buildItemBlobDto(ItemBlobSnapshot itemBlob) {
		Map<String, Object> blob = orderedMap();
		blob.put("hex", itemBlob == null ? "" : itemBlob.hex);
		blob.put("size", itemBlob == null ? 0 : itemBlob.totalPayloadSize);
		blob.put("entryIds", buildEntryIds(itemBlob));
		blob.put("decodedEntries", buildDecodedEntries(itemBlob));
		blob.put("templateDerivedInputs", buildTemplateDerivedInputs(itemBlob));
		blob.put("dynamicInputs", buildDynamicInputs(itemBlob));
		blob.put("timeNormalization", buildTimeNormalization(itemBlob));
		return blob;
	}

	private static List<Integer> buildEntryIds(ItemBlobSnapshot itemBlob) {
		if (itemBlob == null)
			return List.of();
		List<Integer> entryIds = new ArrayList<>();
		for (ItemBlobEntrySnapshot entry : itemBlob.entries)
			entryIds.add(entry.entryId);
		return entryIds;
	}

	private static List<Map<String, Object>> buildDecodedEntries(ItemBlobSnapshot itemBlob) {
		if (itemBlob == null)
			return List.of();
		List<Map<String, Object>> decodedEntries = new ArrayList<>();
		for (ItemBlobEntrySnapshot entry : itemBlob.entries) {
			Map<String, Object> decodedEntry = orderedMap();
			decodedEntry.put("entryName", entry.entryName);
			decodedEntry.put("entryId", entry.entryId);
			decodedEntry.put("payloadSize", entry.payloadSize);
			decodedEntries.add(decodedEntry);
		}
		return decodedEntries;
	}

	private static Map<String, Object> buildTemplateDerivedInputs(ItemBlobSnapshot itemBlob) {
		Map<String, Object> templateInputs = orderedMap();
		templateInputs.put("itemMask", getItemMask(itemBlob));
		templateInputs.put("slotGroup", null);
		templateInputs.put("polishEligible", getPolishCharge(itemBlob) > 0);
		templateInputs.put("conditionable", getConditioningInfoPresent(itemBlob));
		templateInputs.put("bonusStatModifiers", List.of());
		return templateInputs;
	}

	private static Map<String, Object> buildDynamicInputs(ItemBlobSnapshot itemBlob) {
		Map<String, Object> dynamicInputs = orderedMap();
		dynamicInputs.put("fusionRandomBonusStatsId", getFusionedItemBonusStatsId(itemBlob));
		dynamicInputs.put("temporaryExchangeTime", getTemporaryExchangeTime(itemBlob));
		dynamicInputs.put("cleanupSealFlag", getWarehouseRestrictionFlag(itemBlob));
		dynamicInputs.put("accountLegionWarehouseRestrictionFlag", getWarehouseRestrictionFlag(itemBlob));
		dynamicInputs.put("unsealTime", 0);
		dynamicInputs.put("conditioningInfoPresent", getConditioningInfoPresent(itemBlob));
		dynamicInputs.put("plumeTemperingStats", getPlumeTemperingStats(itemBlob));
		return dynamicInputs;
	}

	private static Map<String, Object> buildTimeNormalization(ItemBlobSnapshot itemBlob) {
		Map<String, Object> timeNormalization = orderedMap();
		timeNormalization.put("capturedAtEpochSeconds", 0);
		timeNormalization.put("expirationRemainingSeconds", getSecondsUntilExpiration(itemBlob));
		timeNormalization.put("dyeRemainingSeconds", getDyeTimeLeft(itemBlob));
		return timeNormalization;
	}

	private static Map<String, Object> buildPacketDto(PacketSnapshot packet, ArtifactSnapshot snapshot) {
		Map<String, Object> packetDto = orderedMap();
		packetDto.put("javaClass", packet == null ? null : packet.packetClassName);
		packetDto.put("opcode", packet == null ? 0 : packet.encodedOpcode);
		packetDto.put("bodyHex", packet == null ? "" : packet.bodyHex);
		packetDto.put("canonicalPayloadHex", packet == null ? "" : packet.canonicalPayloadHex);
		packetDto.put("decoded", buildPacketDecodedDto(packet, snapshot));
		return packetDto;
	}

	private static Map<String, Object> buildPacketDecodedDto(PacketSnapshot packet, ArtifactSnapshot snapshot) {
		Map<String, Object> decoded = orderedMap();
		if (packet == null)
			return decoded;
		if (packet.packetIndex == 0) {
			decoded.put("warehouseType", snapshot.storageTypeId);
			decoded.put("addTypeMask", ItemAddType.ALL_SLOT.getMask());
			decoded.put("itemCount", 1);
		} else {
			decoded.put("action", 0);
			decoded.put("actionValue", snapshot.storageTypeOrdinal);
			decoded.put("itemsCount", 0);
			decoded.put("npcExpands", 0);
			decoded.put("questExpands", 0);
			decoded.put("itemExpands", 0);
		}
		return decoded;
	}

	private static ItemBlobSnapshot getEncodeTimeItemBlob(ArtifactSnapshot snapshot) {
		return snapshot.warehouseAddPacket != null && snapshot.warehouseAddPacket.observedItemBlob != null ? snapshot.warehouseAddPacket.observedItemBlob
			: snapshot.constructionTimeItemBlob;
	}

	private static EncodeTimeItemSnapshot getEncodeTimeItemSnapshot(ArtifactSnapshot snapshot) {
		return snapshot.warehouseAddPacket == null ? null : snapshot.warehouseAddPacket.observedItem;
	}

	private static int getItemMask(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.general != null ? itemBlob.payload.general.itemMask : 0;
	}

	private static int getSecondsUntilExpiration(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.general != null ? itemBlob.payload.general.secondsUntilExpiration : 0;
	}

	private static int getTemporaryExchangeTime(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.general != null
			? itemBlob.payload.general.temporaryExchangeTimeRemaining : 0;
	}

	private static int getWarehouseRestrictionFlag(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.general != null ? itemBlob.payload.general.warehouseRestrictionFlag : 0;
	}

	private static int getFusionedItemBonusStatsId(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.composite != null ? itemBlob.payload.composite.fusionedItemBonusStatsId : 0;
	}

	private static int getEnchantLevel(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.enchant != null ? itemBlob.payload.enchant.enchantLevel : 0;
	}

	private static Integer getDyeColor(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.enchant != null ? itemBlob.payload.enchant.dyeColor : null;
	}

	private static int getDyeTimeLeft(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.enchant != null ? itemBlob.payload.enchant.dyeTimeLeft : 0;
	}

	private static List<Integer> getPlumeTemperingStats(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.enchant != null ? itemBlob.payload.enchant.plumeTemperingStats : List.of();
	}

	private static boolean getConditioningInfoPresent(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.conditioning != null
			&& itemBlob.payload.conditioning.conditioningInfoPresent;
	}

	private static int getChargePoints(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.conditioning != null ? itemBlob.payload.conditioning.chargePoints : 0;
	}

	private static int getPolishCharge(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.polish != null ? itemBlob.payload.polish.polishCharge : 0;
	}

	private static int getPackCount(ItemBlobSnapshot itemBlob) {
		return itemBlob != null && itemBlob.payload != null && itemBlob.payload.wrap != null ? itemBlob.payload.wrap.packCount : 0;
	}

	private static Map<String, Object> orderedMap() {
		return new LinkedHashMap<>();
	}

	private static Path buildArtifactPath(ArtifactSnapshot snapshot) {
		Path outputDirectory = resolveOutputDirectory(snapshot.outputDirectory);
		String fileName = sanitizeFileNameFragment(snapshot.scenarioName) + "-player-" + snapshot.playerObjectId + "-storage-"
			+ snapshot.storageTypeId + "-" + snapshot.storageTypeOrdinal + "-item-" + snapshot.itemObjectId + "-"
			+ snapshot.registeredAtMillis + "-" + snapshot.completedAtMillis + ".json";
		Path target = outputDirectory.resolve(fileName).normalize();
		if (!target.startsWith(outputDirectory) || !target.getFileName().toString().endsWith(".json"))
			throw new IllegalStateException("Invalid unusual-storage artifact path: " + target);
		return target;
	}

	private static Path resolveOutputDirectory(String outputDirectory) {
		if (outputDirectory == null || outputDirectory.trim().isEmpty())
			throw new IllegalStateException("Unusual-storage artifact output directory is blank");
		return Path.of(outputDirectory).toAbsolutePath().normalize();
	}

	private static String sanitizeFileNameFragment(String value) {
		if (value == null || value.isBlank())
			return "unknown";
		StringBuilder sanitized = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			sanitized.append(isSafeFileNameChar(c) ? c : '_');
		}
		return sanitized.toString();
	}

	private static boolean isSafeFileNameChar(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '.' || c == '_' || c == '-';
	}

	private static String compactHex(ByteBuffer buffer, int start, int end) {
		if (buffer == null || start >= end)
			return "";
		int safeStart = Math.max(0, start);
		int safeEnd = Math.min(end, buffer.limit());
		if (safeStart >= safeEnd)
			return "";
		StringBuilder hex = new StringBuilder((safeEnd - safeStart) * 2);
		for (int i = safeStart; i < safeEnd; i++) {
			int b = buffer.get(i) & 0xFF;
			appendHexNibble(hex, b >>> 4);
			appendHexNibble(hex, b & 0x0F);
		}
		return hex.toString();
	}

	private static void appendHexNibble(StringBuilder hex, int nibble) {
		hex.append((char) (nibble < 10 ? '0' + nibble : 'A' + nibble - 10));
	}

	private static void startWriterWorker() {
		synchronized (writerWorkerLock) {
			if (writerWorker != null)
				return;
			writerWorkerRunning = true;
			writerWorker = new Thread(PetFeedUnusualStorageArtifactCapture::runWriterWorker, "PetFeedUnusualStorageArtifactWriter");
			writerWorker.setDaemon(true);
			writerWorker.start();
		}
	}

	private static void stopWriterWorker() {
		Thread worker;
		synchronized (writerWorkerLock) {
			writerWorkerRunning = false;
			worker = writerWorker;
		}
		if (worker != null)
			worker.interrupt();
	}

	private static void runWriterWorker() {
		try {
			while (writerWorkerRunning && isEnabled()) {
				if (!drainQueuedArtifact()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
		} finally {
			synchronized (writerWorkerLock) {
				if (Thread.currentThread() == writerWorker) {
					writerWorker = null;
					writerWorkerRunning = false;
				}
			}
		}
	}

	private static final class CaptureContext {

		private final int playerObjectId;
		private final int storageTypeId;
		private final int storageTypeOrdinal;
		private final int itemObjectId;
		private final long registeredAtMillis;
		private final String scenarioName;
		private final String outputDirectory;
		private final ItemBlobSnapshot constructionTimeItemBlob;
		private final PacketSnapshot[] packets = new PacketSnapshot[2];
		private int nextPacketIndex;

		private CaptureContext(int playerObjectId, int storageTypeId, int storageTypeOrdinal, int itemObjectId, long registeredAtMillis,
			String scenarioName, String outputDirectory, ItemBlobSnapshot constructionTimeItemBlob) {
			this.playerObjectId = playerObjectId;
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
			return new ArtifactSnapshot(scenarioName, outputDirectory, playerObjectId, storageTypeId, storageTypeOrdinal, itemObjectId,
				registeredAtMillis, completedAtMillis, constructionTimeItemBlob, packets[0], packets[1]);
		}
	}

	private static final class ArtifactSnapshot {

		private final String scenarioName;
		private final String outputDirectory;
		private final int playerObjectId;
		private final int storageTypeId;
		private final int storageTypeOrdinal;
		private final int itemObjectId;
		private final long registeredAtMillis;
		private final long completedAtMillis;
		private final ItemBlobSnapshot constructionTimeItemBlob;
		private final PacketSnapshot warehouseAddPacket;
		private final PacketSnapshot cubeUpdatePacket;

		private ArtifactSnapshot(String scenarioName, String outputDirectory, int playerObjectId, int storageTypeId, int storageTypeOrdinal,
			int itemObjectId, long registeredAtMillis, long completedAtMillis, ItemBlobSnapshot constructionTimeItemBlob,
			PacketSnapshot warehouseAddPacket, PacketSnapshot cubeUpdatePacket) {
			this.scenarioName = scenarioName;
			this.outputDirectory = outputDirectory;
			this.playerObjectId = playerObjectId;
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
		private final String hex;
		private final List<ItemBlobEntrySnapshot> entries;
		private final ItemBlobPayloadSnapshot payload;

		private ItemBlobSnapshot(int totalPayloadSize, String hex, List<ItemBlobEntrySnapshot> entries, ItemBlobPayloadSnapshot payload) {
			this.totalPayloadSize = totalPayloadSize;
			this.hex = hex;
			this.entries = Collections.unmodifiableList(entries);
			this.payload = payload;
		}

		private static ItemBlobSnapshot from(Player player, Item item) {
			return from(ItemInfoBlob.getFullBlob(player, item), item);
		}

		private static ItemBlobSnapshot from(ItemInfoBlob blob) {
			return from(blob, null);
		}

		private static ItemBlobSnapshot from(ItemInfoBlob blob, Item item) {
			if (blob == null)
				return null;
			List<ItemBlobEntrySnapshot> entries = new ArrayList<>();
			for (ItemBlobEntryMetadata metadata : blob.getBlobEntryMetadata())
				entries.add(new ItemBlobEntrySnapshot(metadata.getEntryName(), metadata.getEntryId(), metadata.getPayloadSize()));
			return new ItemBlobSnapshot(blob.size(), serializeBlobHex(blob), entries, item == null ? null : ItemBlobPayloadSnapshot.from(item));
		}

		private static String serializeBlobHex(ItemInfoBlob blob) {
			ByteBuffer serializedBlob = ByteBuffer.allocate(2 + blob.size());
			blob.writeMe(serializedBlob);
			serializedBlob.flip();
			return compactHex(serializedBlob, 0, serializedBlob.limit());
		}
	}

	private static final class ItemBlobPayloadSnapshot {

		private final GeneralPayloadSnapshot general;
		private final CompositePayloadSnapshot composite;
		private final EnchantPayloadSnapshot enchant;
		private final ConditioningPayloadSnapshot conditioning;
		private final PremiumOptionPayloadSnapshot premiumOption;
		private final PolishPayloadSnapshot polish;
		private final WrapPayloadSnapshot wrap;

		private ItemBlobPayloadSnapshot(GeneralPayloadSnapshot general, CompositePayloadSnapshot composite, EnchantPayloadSnapshot enchant,
			ConditioningPayloadSnapshot conditioning, PremiumOptionPayloadSnapshot premiumOption, PolishPayloadSnapshot polish,
			WrapPayloadSnapshot wrap) {
			this.general = general;
			this.composite = composite;
			this.enchant = enchant;
			this.conditioning = conditioning;
			this.premiumOption = premiumOption;
			this.polish = polish;
			this.wrap = wrap;
		}

		private static ItemBlobPayloadSnapshot from(Item item) {
			return new ItemBlobPayloadSnapshot(GeneralPayloadSnapshot.from(item), CompositePayloadSnapshot.from(item),
				EnchantPayloadSnapshot.from(item), ConditioningPayloadSnapshot.from(item), PremiumOptionPayloadSnapshot.from(item),
				PolishPayloadSnapshot.from(item), WrapPayloadSnapshot.from(item));
		}
	}

	private static final class GeneralPayloadSnapshot {

		private final int itemMask;
		private final long itemCount;
		private final String itemCreator;
		private final int secondsUntilExpiration;
		private final int temporaryExchangeTimeRemaining;
		private final int warehouseRestrictionFlag;

		private GeneralPayloadSnapshot(int itemMask, long itemCount, String itemCreator, int secondsUntilExpiration,
			int temporaryExchangeTimeRemaining, int warehouseRestrictionFlag) {
			this.itemMask = itemMask;
			this.itemCount = itemCount;
			this.itemCreator = itemCreator;
			this.secondsUntilExpiration = secondsUntilExpiration;
			this.temporaryExchangeTimeRemaining = temporaryExchangeTimeRemaining;
			this.warehouseRestrictionFlag = warehouseRestrictionFlag;
		}

		private static GeneralPayloadSnapshot from(Item item) {
			int warehouseRestrictionFlag = DataManager.ITEM_CLEAN_UP.hasAccountOrLegionWhStorabilityDisabled(item.getItemId()) ? 3 : 0;
			return new GeneralPayloadSnapshot(item.getItemMask(), item.getItemCount(), item.getItemCreator(), item.secondsUntilExpiration(),
				item.getTemporaryExchangeTimeRemaining(), warehouseRestrictionFlag);
		}
	}

	private static final class CompositePayloadSnapshot {

		private final int fusionedItemId;
		private final List<Integer> fusionStoneItemIdsBySlot;
		private final int fusionedItemOptionalSockets;
		private final int fusionedItemBonusStatsId;

		private CompositePayloadSnapshot(int fusionedItemId, List<Integer> fusionStoneItemIdsBySlot, int fusionedItemOptionalSockets,
			int fusionedItemBonusStatsId) {
			this.fusionedItemId = fusionedItemId;
			this.fusionStoneItemIdsBySlot = Collections.unmodifiableList(fusionStoneItemIdsBySlot);
			this.fusionedItemOptionalSockets = fusionedItemOptionalSockets;
			this.fusionedItemBonusStatsId = fusionedItemBonusStatsId;
		}

		private static CompositePayloadSnapshot from(Item item) {
			return new CompositePayloadSnapshot(item.getFusionedItemId(), itemStoneIdsBySlot(item.hasFusionStones() ? item.getFusionStones() : null),
				item.getFusionedItemOptionalSockets(), item.getFusionedItemBonusStatsId());
		}
	}

	private static final class EnchantPayloadSnapshot {

		private final boolean soulBound;
		private final int enchantLevel;
		private final int itemSkinTemplateId;
		private final int optionalManastoneSockets;
		private final int enchantBonus;
		private final List<Integer> manaStoneItemIdsBySlot;
		private final int godStoneId;
		private final Integer dyeColor;
		private final int dyeTimeLeft;
		private final int idianStoneItemId;
		private final int idianPolishNumber;
		private final int tempering;
		private final List<Integer> plumeTemperingStats;
		private final boolean amplified;
		private final int buffSkill;

		private EnchantPayloadSnapshot(boolean soulBound, int enchantLevel, int itemSkinTemplateId, int optionalManastoneSockets,
			int enchantBonus, List<Integer> manaStoneItemIdsBySlot, int godStoneId, Integer dyeColor, int dyeTimeLeft, int idianStoneItemId,
			int idianPolishNumber, int tempering, List<Integer> plumeTemperingStats, boolean amplified, int buffSkill) {
			this.soulBound = soulBound;
			this.enchantLevel = enchantLevel;
			this.itemSkinTemplateId = itemSkinTemplateId;
			this.optionalManastoneSockets = optionalManastoneSockets;
			this.enchantBonus = enchantBonus;
			this.manaStoneItemIdsBySlot = Collections.unmodifiableList(manaStoneItemIdsBySlot);
			this.godStoneId = godStoneId;
			this.dyeColor = dyeColor;
			this.dyeTimeLeft = dyeTimeLeft;
			this.idianStoneItemId = idianStoneItemId;
			this.idianPolishNumber = idianPolishNumber;
			this.tempering = tempering;
			this.plumeTemperingStats = Collections.unmodifiableList(plumeTemperingStats);
			this.amplified = amplified;
			this.buffSkill = buffSkill;
		}

		private static EnchantPayloadSnapshot from(Item item) {
			int optionalManastoneSockets = !item.isIdentified() ? -1 : item.getOptionalSockets();
			int enchantBonus = !item.isIdentified() ? -1 : item.getEnchantBonus();
			int dyeTimeLeft = item.getColorTimeLeft();
			IdianStone idianStone = item.getIdianStone();
			int idianStoneItemId = idianStone != null && idianStone.getPolishNumber() > 0 ? idianStone.getItemId() : 0;
			int idianPolishNumber = idianStone != null && idianStone.getPolishNumber() > 0 ? idianStone.getPolishNumber() : 0;
			return new EnchantPayloadSnapshot(item.isSoulBound(), item.getEnchantLevel(), item.getItemSkinTemplate().getTemplateId(),
				optionalManastoneSockets, enchantBonus, itemStoneIdsBySlot(item.hasManaStones() ? item.getItemStones() : null),
				item.getGodStoneId(), dyeTimeLeft < 0 ? null : item.getItemColor(), dyeTimeLeft, idianStoneItemId, idianPolishNumber,
				item.getTempering(), plumeTemperingStats(item), item.isAmplified(), item.getBuffSkill());
		}

		private static List<Integer> plumeTemperingStats(Item item) {
			List<Integer> stats = new ArrayList<>();
			if (item.getTempering() > 0 && item.getItemTemplate().getItemGroup() == ItemGroup.PLUME) {
				PlumStatEnum stat = item.getItemTemplate().getTemperingName().equals("TSHIRT_PHYSICAL") ? PlumStatEnum.PLUM_PHISICAL_ATTACK
					: PlumStatEnum.PLUM_BOOST_MAGICAL_SKILL;
				stats.add(PlumStatEnum.PLUM_HP.getId());
				stats.add(PlumStatEnum.PLUM_HP.getBoostValue() * item.getTempering());
				stats.add(stat.getId());
				stats.add(stat.getBoostValue() * item.getTempering() + item.getRndPlumeBonusValue());
			}
			return stats;
		}
	}

	private static final class ConditioningPayloadSnapshot {

		private final boolean conditioningInfoPresent;
		private final int chargePoints;

		private ConditioningPayloadSnapshot(boolean conditioningInfoPresent, int chargePoints) {
			this.conditioningInfoPresent = conditioningInfoPresent;
			this.chargePoints = chargePoints;
		}

		private static ConditioningPayloadSnapshot from(Item item) {
			return new ConditioningPayloadSnapshot(item.getConditioningInfo() != null, item.getChargePoints());
		}
	}

	private static final class PremiumOptionPayloadSnapshot {

		private final boolean identified;
		private final int bonusStatsId;
		private final int tuneCount;

		private PremiumOptionPayloadSnapshot(boolean identified, int bonusStatsId, int tuneCount) {
			this.identified = identified;
			this.bonusStatsId = bonusStatsId;
			this.tuneCount = tuneCount;
		}

		private static PremiumOptionPayloadSnapshot from(Item item) {
			return new PremiumOptionPayloadSnapshot(item.isIdentified(), !item.isIdentified() ? -1 : item.getBonusStatsId(),
				!item.isIdentified() ? 0 : item.getTuneCount());
		}
	}

	private static final class PolishPayloadSnapshot {

		private final int polishCharge;

		private PolishPayloadSnapshot(int polishCharge) {
			this.polishCharge = polishCharge;
		}

		private static PolishPayloadSnapshot from(Item item) {
			IdianStone stone = item.getIdianStone();
			return new PolishPayloadSnapshot(stone == null ? 0 : stone.getPolishCharge());
		}
	}

	private static final class WrapPayloadSnapshot {

		private final int packCount;

		private WrapPayloadSnapshot(int packCount) {
			this.packCount = packCount;
		}

		private static WrapPayloadSnapshot from(Item item) {
			return new WrapPayloadSnapshot(item.getPackCount());
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

	private static List<Integer> itemStoneIdsBySlot(Set<? extends ItemStone> stones) {
		List<Integer> itemIdsBySlot = new ArrayList<>();
		for (int i = 0; i < Item.MAX_BASIC_STONES; i++)
			itemIdsBySlot.add(0);
		if (stones == null)
			return itemIdsBySlot;
		for (ItemStone stone : stones) {
			int slot = stone.getSlot();
			if (slot >= 0 && slot < Item.MAX_BASIC_STONES)
				itemIdsBySlot.set(slot, stone.getItemId());
		}
		return itemIdsBySlot;
	}

	private static final class PacketSnapshot {

		private final int packetIndex;
		private final String packetClassName;
		private final int clearFrameLength;
		private final int encodedOpcode;
		private final int remainingBytesAtObserver;
		private final String clearFrameHex;
		private final String bodyHex;
		private final String canonicalPayloadHex;
		private final ItemBlobSnapshot observedItemBlob;
		private final EncodeTimeItemSnapshot observedItem;

		private PacketSnapshot(int packetIndex, String packetClassName, int clearFrameLength, int encodedOpcode,
			int remainingBytesAtObserver, String clearFrameHex, String bodyHex, String canonicalPayloadHex, ItemBlobSnapshot observedItemBlob,
			EncodeTimeItemSnapshot observedItem) {
			this.packetIndex = packetIndex;
			this.packetClassName = packetClassName;
			this.clearFrameLength = clearFrameLength;
			this.encodedOpcode = encodedOpcode;
			this.remainingBytesAtObserver = remainingBytesAtObserver;
			this.clearFrameHex = clearFrameHex;
			this.bodyHex = bodyHex;
			this.canonicalPayloadHex = canonicalPayloadHex;
			this.observedItemBlob = observedItemBlob;
			this.observedItem = observedItem;
		}

		private static PacketSnapshot from(int packetIndex, AionServerPacket packet, ByteBuffer clearFrame) {
			int clearFrameLength = clearFrame.limit() >= 2 ? clearFrame.getShort(0) & 0xFFFF : 0;
			int encodedOpcode = clearFrame.limit() >= 4 ? clearFrame.getShort(2) & 0xFFFF : 0;
			String clearFrameHex = compactHex(clearFrame, 0, clearFrame.limit());
			String bodyHex = compactHex(clearFrame, 7, clearFrame.limit());
			ItemBlobSnapshot observedItemBlob = null;
			EncodeTimeItemSnapshot observedItem = null;
			if (packet instanceof SM_WAREHOUSE_ADD_ITEM) {
				SM_WAREHOUSE_ADD_ITEM warehouseAddItem = (SM_WAREHOUSE_ADD_ITEM) packet;
				Item item = warehouseAddItem.getFirstItem();
				observedItemBlob = ItemBlobSnapshot.from(warehouseAddItem.getFirstItemInfoBlob(), item);
				observedItem = EncodeTimeItemSnapshot.from(item);
			}
			return new PacketSnapshot(packetIndex, packet.getClass().getName(), clearFrameLength, encodedOpcode, clearFrame.remaining(),
				clearFrameHex, bodyHex, bodyHex, observedItemBlob, observedItem);
		}
	}

	private static final class EncodeTimeItemSnapshot {

		private final int itemId;
		private final long itemCount;
		private final int itemLocation;
		private final long equipmentSlot;
		private final int itemTemplateId;
		private final String localizedName;
		private final int packCount;
		private final int expireTime;
		private final int temporaryExchangeTime;
		private final int temporaryExchangeTimeRemaining;
		private final int chargePoints;
		private final int enchantLevel;
		private final int itemMask;
		private final Integer color;

		private EncodeTimeItemSnapshot(int itemId, long itemCount, int itemLocation, long equipmentSlot, int itemTemplateId,
			String localizedName, int packCount, int expireTime, int temporaryExchangeTime, int temporaryExchangeTimeRemaining,
			int chargePoints, int enchantLevel, int itemMask, Integer color) {
			this.itemId = itemId;
			this.itemCount = itemCount;
			this.itemLocation = itemLocation;
			this.equipmentSlot = equipmentSlot;
			this.itemTemplateId = itemTemplateId;
			this.localizedName = localizedName;
			this.packCount = packCount;
			this.expireTime = expireTime;
			this.temporaryExchangeTime = temporaryExchangeTime;
			this.temporaryExchangeTimeRemaining = temporaryExchangeTimeRemaining;
			this.chargePoints = chargePoints;
			this.enchantLevel = enchantLevel;
			this.itemMask = itemMask;
			this.color = color;
		}

		private static EncodeTimeItemSnapshot from(Item item) {
			if (item == null)
				return null;
			ItemTemplate itemTemplate = item.getItemTemplate();
			return new EncodeTimeItemSnapshot(item.getItemId(), item.getItemCount(), item.getItemLocation(), item.getEquipmentSlot(),
				itemTemplate.getTemplateId(), itemTemplate.getL10n(), item.getPackCount(), item.getExpireTime(), item.getTemporaryExchangeTime(),
				item.getTemporaryExchangeTimeRemaining(), item.getChargePoints(), item.getEnchantLevel(), item.getItemMask(), item.getItemColor());
		}
	}
}
