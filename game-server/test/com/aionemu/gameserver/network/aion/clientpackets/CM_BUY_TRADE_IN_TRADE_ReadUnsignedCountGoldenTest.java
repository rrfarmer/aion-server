package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_BUY_TRADE_IN_TRADE_ReadUnsignedCountGoldenTest {

	@Test
	public void readImpl_readsUnsignedTradeInListCountAndObjectIdsInOrder() throws Exception {
		CM_BUY_TRADE_IN_TRADE packet = new CM_BUY_TRADE_IN_TRADE(88, Set.of(State.IN_GAME));
		packet.setBuffer(payload(7001, 0x80, 1001, 2, 2001, 2002));

		packet.readImpl();

		assertEquals(7001, getField(packet, "sellerObjId"));
		assertEquals((byte) 0x80, getField(packet, "mask"));
		assertEquals(1001, getField(packet, "itemId"));
		assertEquals(2, getField(packet, "count"));
		assertEquals(2, getField(packet, "tradeInListCount"));
		assertEquals(List.of(2001, 2002), getField(packet, "tradeInItemObjIds"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int sellerObjId, int mask, int itemId, int count, int... tradeInItemObjIds) {
		ByteBuffer buffer = ByteBuffer.allocate(15 + tradeInItemObjIds.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sellerObjId);
		buffer.put((byte) mask);
		buffer.putInt(itemId);
		buffer.putInt(count);
		buffer.putShort((short) tradeInItemObjIds.length);
		for (int tradeInItemObjId : tradeInItemObjIds)
			buffer.putInt(tradeInItemObjId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
