package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_MOVE_ITEM_ReadSignedSlotGoldenTest {

	@Test
	public void readImpl_highBitSlotReadsAsSignedShort() throws Exception {
		CM_MOVE_ITEM packet = new CM_MOVE_ITEM(156, Set.of(State.IN_GAME));
		packet.setBuffer(payload(7001, 0, 3, 0xFFFF));

		packet.readImpl();

		assertEquals(7001, getField(packet, "itemObjId"));
		assertEquals((byte) 0, getField(packet, "source"));
		assertEquals((byte) 3, getField(packet, "destination"));
		assertEquals((short) -1, getField(packet, "slot"));
	}

	private static ByteBuffer payload(int itemObjId, int source, int destination, int slot) {
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(itemObjId);
		buffer.put((byte) source);
		buffer.put((byte) destination);
		buffer.putShort((short) slot);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
