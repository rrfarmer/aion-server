package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_AUTO_GROUP_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsInstanceMaskWindowAndEntryRequest() throws Exception {
		CM_AUTO_GROUP packet = new CM_AUTO_GROUP(200, Set.of(State.IN_GAME));
		packet.setBuffer(payload(300260000, (byte) 100, (byte) 1));

		packet.readImpl();

		assertEquals(300260000, getField(packet, "instanceMaskId"));
		assertEquals((byte) 100, getField(packet, "windowId"));
		assertEquals((byte) 1, getField(packet, "entryRequestId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int instanceMaskId, byte windowId, byte entryRequestId) {
		ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(instanceMaskId);
		buffer.put(windowId);
		buffer.put(entryRequestId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
