package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.trade.TradeItem;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionConnection.State;

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

	private static AionConnection allocateConnection() throws Exception {
		NetworkConfig.PACKET_PROCESSOR_MIN_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_MAX_THREADS = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_KILL_THRESHOLD = 1;
		NetworkConfig.PACKET_PROCESSOR_THREAD_SPAWN_THRESHOLD = 1;
		ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = 5000;
		Unsafe unsafe = unsafe();
		AionConnection connection = (AionConnection) unsafe.allocateInstance(AionConnection.class);
		setField(connection, "activePlayer", new AtomicReference<Player>());
		return connection;
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
