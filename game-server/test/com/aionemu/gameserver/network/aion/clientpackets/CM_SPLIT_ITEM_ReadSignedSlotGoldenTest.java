package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_SPLIT_ITEM_ReadSignedSlotGoldenTest {

	@Test
	public void readImpl_highBitSlotNumberReadsAsSignedShort() throws Exception {
		CM_SPLIT_ITEM packet = new CM_SPLIT_ITEM(157, Set.of(State.IN_GAME));
		packet.setBuffer(payload(7001, 123, 0, 8002, 3, 0xFFFF));

		packet.readImpl();

		assertEquals(7001, getField(packet, "sourceItemObjId"));
		assertEquals(123L, getField(packet, "itemAmount"));
		assertEquals((byte) 0, getField(packet, "sourceStorageType"));
		assertEquals(8002, getField(packet, "destinationItemObjId"));
		assertEquals((byte) 3, getField(packet, "destinationStorageType"));
		assertEquals((short) -1, getField(packet, "slotNum"));
	}

	private static ByteBuffer payload(int sourceItemObjId, long itemAmount, int sourceStorageType, int destinationItemObjId,
		int destinationStorageType, int slotNum) {
		ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(sourceItemObjId);
		buffer.putLong(itemAmount);
		buffer.put((byte) sourceStorageType);
		buffer.putInt(destinationItemObjId);
		buffer.put((byte) destinationStorageType);
		buffer.putShort((short) slotNum);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
