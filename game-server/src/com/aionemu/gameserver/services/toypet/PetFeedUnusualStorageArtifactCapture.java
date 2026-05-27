package com.aionemu.gameserver.services.toypet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
		// Future JSON/file writer boundary. Intentionally no-op.
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
		private final ItemBlobPayloadSnapshot payload;

		private ItemBlobSnapshot(int totalPayloadSize, List<ItemBlobEntrySnapshot> entries, ItemBlobPayloadSnapshot payload) {
			this.totalPayloadSize = totalPayloadSize;
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
			return new ItemBlobSnapshot(blob.size(), entries, item == null ? null : ItemBlobPayloadSnapshot.from(item));
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
			if (packet instanceof SM_WAREHOUSE_ADD_ITEM) {
				SM_WAREHOUSE_ADD_ITEM warehouseAddItem = (SM_WAREHOUSE_ADD_ITEM) packet;
				observedItemBlob = ItemBlobSnapshot.from(warehouseAddItem.getFirstItemInfoBlob(), warehouseAddItem.getFirstItem());
			}
			return new PacketSnapshot(packetIndex, packet.getClass().getName(), clearFrameLength, encodedOpcode, clearFrame.remaining(),
				observedItemBlob);
		}
	}
}
