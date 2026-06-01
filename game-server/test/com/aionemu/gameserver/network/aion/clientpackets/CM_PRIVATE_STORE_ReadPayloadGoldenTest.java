package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.trade.TradePSItem;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PRIVATE_STORE_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsItemsInFieldOrderWithUnsignedCounts() throws Exception {
		CM_PRIVATE_STORE packet = new CM_PRIVATE_STORE(119, Set.of(State.IN_GAME));
		packet.setBuffer(itemsPayload());

		packet.readImpl();

		TradePSItem[] items = (TradePSItem[]) getField(packet, "tradePSItems");
		assertEquals(2, items.length);
		assertItem(items[0], 3001, 100000001, 1, 10000);
		assertItem(items[1], 3002, 182003001, 65535, 9999999999L);
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_zeroItemsCreatesEmptyArrayForCloseBranch() throws Exception {
		CM_PRIVATE_STORE packet = new CM_PRIVATE_STORE(119, Set.of(State.IN_GAME));
		ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) 0);
		buffer.flip();
		packet.setBuffer(buffer);

		packet.readImpl();

		TradePSItem[] items = (TradePSItem[]) getField(packet, "tradePSItems");
		assertEquals(0, items.length);
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer itemsPayload() {
		ByteBuffer buffer = ByteBuffer.allocate(2 + 2 * 18).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) 2);
		buffer.putInt(3001);
		buffer.putInt(100000001);
		buffer.putShort((short) 1);
		buffer.putLong(10000);
		buffer.putInt(3002);
		buffer.putInt(182003001);
		buffer.putShort((short) 0xFFFF);
		buffer.putLong(9999999999L);
		buffer.flip();
		return buffer;
	}

	private static void assertItem(TradePSItem item, int objectId, int itemId, long count, long price) {
		assertEquals(objectId, item.getItemObjId());
		assertEquals(itemId, item.getItemId());
		assertEquals(count, item.getCount());
		assertEquals(price, item.getPrice());
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
