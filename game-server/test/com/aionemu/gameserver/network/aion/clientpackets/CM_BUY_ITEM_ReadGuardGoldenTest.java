package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.PunishmentConfig;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.trade.RepurchaseList;
import com.aionemu.gameserver.model.trade.TradeItem;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.RepurchaseService;

import sun.misc.Unsafe;

public class CM_BUY_ITEM_ReadGuardGoldenTest {

	@Test
	public void readImpl_privateStoreActionAllowsZeroIndexAndMaxCount() throws Exception {
		CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
		packet.setConnection(allocateConnection());
		packet.setBuffer(payload(7001, 0, 1, 0, 20000));

		packet.readImpl();

		assertEquals(7001, getField(packet, "sellerObjId"));
		assertEquals((short) 0, getField(packet, "tradeActionId"));
		assertEquals(1, getField(packet, "amount"));
		assertFalse((boolean) getField(packet, "isAudit"));
		assertEquals(0, getField(packet, "itemId"));
		assertEquals(20000L, getField(packet, "count"));

		TradeList tradeList = (TradeList) getField(packet, "tradeList");
		assertEquals(7001, tradeList.getSellerObjId());
		assertEquals(1, tradeList.size());
		TradeItem item = tradeList.getTradeItems().get(0);
		assertEquals(0, item.getItemId());
		assertEquals(20000L, item.getCount());
	}

	@Test
	public void readImpl_tradeListActionsStoreItemsInReadOrder() throws Exception {
		int[] tradeListActions = { 1, 13, 14, 15, 16, 17 };
		for (int tradeActionId : tradeListActions) {
			assertTradeListActionStoresItemsInReadOrder(tradeActionId);
		}
	}

	@Test
	public void readImpl_repurchaseActionFiltersItemsThroughRepurchaseServiceInFirstSeenOrder() throws Exception {
		RepurchaseService repurchaseService = RepurchaseService.getInstance();
		Object originalRepurchaseItems = getField(repurchaseService, "repurchaseItems");
		try {
			Map<Integer, Set<Item>> repurchaseItems = new ConcurrentHashMap<>();
			repurchaseItems.put(5001, Set.of(item(101), item(102)));
			setField(repurchaseService, "repurchaseItems", repurchaseItems);

			CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
			packet.setConnection(allocateConnection(player(5001)));
			packet.setBuffer(payload(7001, 2, new int[] { 101, 999, 102, 101 }, new long[] { 1, 1, 5, 7 }));

			packet.readImpl();

			assertEquals(7001, getField(packet, "sellerObjId"));
			assertEquals((short) 2, getField(packet, "tradeActionId"));
			assertEquals(4, getField(packet, "amount"));
			assertFalse((boolean) getField(packet, "isAudit"));
			assertEquals(101, getField(packet, "itemId"));
			assertEquals(7L, getField(packet, "count"));

			RepurchaseList repurchaseList = (RepurchaseList) getField(packet, "repurchaseList");
			assertEquals(7001, repurchaseList.getSellerObjId());
			assertEquals(2, repurchaseList.size());
			LinkedHashSet<Integer> expectedItems = new LinkedHashSet<>();
			expectedItems.add(101);
			expectedItems.add(102);
			assertEquals(expectedItems, repurchaseList.getRepurchaseItems());
		} finally {
			setField(repurchaseService, "repurchaseItems", originalRepurchaseItems);
		}
	}

	@Test
	public void readImpl_amountAboveMaximumSetsAuditBeforeCreatingLists() throws Exception {
		boolean originalPunishmentEnable = PunishmentConfig.PUNISHMENT_ENABLE;
		boolean originalLogAudit = LoggingConfig.LOG_AUDIT;
		SkillData originalSkillData = DataManager.SKILL_DATA;
		try {
			PunishmentConfig.PUNISHMENT_ENABLE = false;
			LoggingConfig.LOG_AUDIT = false;
			DataManager.SKILL_DATA = new SkillData();

			CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
			packet.setConnection(allocateConnection());
			packet.setBuffer(header(7001, 13, 37));

			packet.readImpl();

			assertEquals(7001, getField(packet, "sellerObjId"));
			assertEquals((short) 13, getField(packet, "tradeActionId"));
			assertEquals(37, getField(packet, "amount"));
			assertEquals(true, getField(packet, "isAudit"));
			assertEquals(null, getField(packet, "tradeList"));
			assertEquals(null, getField(packet, "repurchaseList"));
		} finally {
			PunishmentConfig.PUNISHMENT_ENABLE = originalPunishmentEnable;
			LoggingConfig.LOG_AUDIT = originalLogAudit;
			DataManager.SKILL_DATA = originalSkillData;
		}
	}

	@Test
	public void readImpl_negativeCountSetsAuditAndLeavesPriorTradeListItems() throws Exception {
		boolean originalPunishmentEnable = PunishmentConfig.PUNISHMENT_ENABLE;
		boolean originalLogAudit = LoggingConfig.LOG_AUDIT;
		SkillData originalSkillData = DataManager.SKILL_DATA;
		try {
			PunishmentConfig.PUNISHMENT_ENABLE = false;
			LoggingConfig.LOG_AUDIT = false;
			DataManager.SKILL_DATA = new SkillData();

			CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
			packet.setConnection(allocateConnection());
			packet.setBuffer(payload(7001, 13, new int[] { 101, 102 }, new long[] { 1, -1 }));

			packet.readImpl();

			assertEquals(7001, getField(packet, "sellerObjId"));
			assertEquals((short) 13, getField(packet, "tradeActionId"));
			assertEquals(2, getField(packet, "amount"));
			assertEquals(true, getField(packet, "isAudit"));
			assertEquals(102, getField(packet, "itemId"));
			assertEquals(-1L, getField(packet, "count"));

			TradeList tradeList = (TradeList) getField(packet, "tradeList");
			assertEquals(7001, tradeList.getSellerObjId());
			assertEquals(1, tradeList.size());
			assertEquals(101, tradeList.getTradeItems().get(0).getItemId());
			assertEquals(1L, tradeList.getTradeItems().get(0).getCount());
		} finally {
			PunishmentConfig.PUNISHMENT_ENABLE = originalPunishmentEnable;
			LoggingConfig.LOG_AUDIT = originalLogAudit;
			DataManager.SKILL_DATA = originalSkillData;
		}
	}

	@Test
	public void readImpl_nonPositiveItemIdSetsAuditForNonPrivateStoreAction() throws Exception {
		boolean originalPunishmentEnable = PunishmentConfig.PUNISHMENT_ENABLE;
		boolean originalLogAudit = LoggingConfig.LOG_AUDIT;
		SkillData originalSkillData = DataManager.SKILL_DATA;
		try {
			PunishmentConfig.PUNISHMENT_ENABLE = false;
			LoggingConfig.LOG_AUDIT = false;
			DataManager.SKILL_DATA = new SkillData();

			CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
			packet.setConnection(allocateConnection());
			packet.setBuffer(payload(7001, 13, 1, 0, 1));

			packet.readImpl();

			assertEquals(7001, getField(packet, "sellerObjId"));
			assertEquals((short) 13, getField(packet, "tradeActionId"));
			assertEquals(1, getField(packet, "amount"));
			assertEquals(true, getField(packet, "isAudit"));
			assertEquals(0, getField(packet, "itemId"));
			assertEquals(1L, getField(packet, "count"));

			TradeList tradeList = (TradeList) getField(packet, "tradeList");
			assertEquals(7001, tradeList.getSellerObjId());
			assertEquals(0, tradeList.size());
		} finally {
			PunishmentConfig.PUNISHMENT_ENABLE = originalPunishmentEnable;
			LoggingConfig.LOG_AUDIT = originalLogAudit;
			DataManager.SKILL_DATA = originalSkillData;
		}
	}

	private static void assertTradeListActionStoresItemsInReadOrder(int tradeActionId) throws Exception {
		CM_BUY_ITEM packet = new CM_BUY_ITEM(51, Set.of(State.IN_GAME));
		packet.setConnection(allocateConnection());
		packet.setBuffer(payload(7001, tradeActionId, new int[] { 100000001, 100000002 }, new long[] { 1, 5 }));

		packet.readImpl();

		assertEquals(7001, getField(packet, "sellerObjId"));
		assertEquals((short) tradeActionId, getField(packet, "tradeActionId"));
		assertEquals(2, getField(packet, "amount"));
		assertFalse((boolean) getField(packet, "isAudit"));
		assertEquals(100000002, getField(packet, "itemId"));
		assertEquals(5L, getField(packet, "count"));

		TradeList tradeList = (TradeList) getField(packet, "tradeList");
		assertEquals(7001, tradeList.getSellerObjId());
		assertEquals(2, tradeList.size());
		assertEquals(100000001, tradeList.getTradeItems().get(0).getItemId());
		assertEquals(1L, tradeList.getTradeItems().get(0).getCount());
		assertEquals(100000002, tradeList.getTradeItems().get(1).getItemId());
		assertEquals(5L, tradeList.getTradeItems().get(1).getCount());
	}

	private static ByteBuffer payload(int sellerObjectId, int tradeActionId, int amount, int itemId, long count) {
		ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sellerObjectId);
		buffer.putShort((short) tradeActionId);
		buffer.putShort((short) amount);
		buffer.putInt(itemId);
		buffer.putLong(count);
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer header(int sellerObjectId, int tradeActionId, int amount) {
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sellerObjectId);
		buffer.putShort((short) tradeActionId);
		buffer.putShort((short) amount);
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer payload(int sellerObjectId, int tradeActionId, int[] itemIds, long[] counts) {
		ByteBuffer buffer = ByteBuffer.allocate(8 + itemIds.length * 12).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sellerObjectId);
		buffer.putShort((short) tradeActionId);
		buffer.putShort((short) itemIds.length);
		for (int i = 0; i < itemIds.length; i++) {
			buffer.putInt(itemIds[i]);
			buffer.putLong(counts[i]);
		}
		buffer.flip();
		return buffer;
	}

	private static AionConnection allocateConnection() throws Exception {
		return allocateConnection(null);
	}

	private static AionConnection allocateConnection(Player player) throws Exception {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = 5000;
		Unsafe unsafe = unsafe();
		AionConnection connection = (AionConnection) unsafe.allocateInstance(AionConnection.class);
		setField(connection, "activePlayer", new AtomicReference<>(player));
		return connection;
	}

	private static Player player(int objectId) throws Exception {
		Player player = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(player, objectId);
		return player;
	}

	private static Item item(int objectId) throws Exception {
		Item item = (Item) unsafe().allocateInstance(Item.class);
		setAionObjectId(item, objectId);
		return item;
	}

	private static void setAionObjectId(AionObject object, int objectId) throws Exception {
		Field field = AionObject.class.getDeclaredField("objectId");
		long offset = unsafe().objectFieldOffset(field);
		unsafe().putInt(object, offset, objectId);
	}

	private static Unsafe unsafe() throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		return (Unsafe) unsafeField.get(null);
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
