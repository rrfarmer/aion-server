package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_OPEN_STATICDOOR_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsDoorId() throws Exception {
		CM_OPEN_STATICDOOR packet = new CM_OPEN_STATICDOOR(23, Set.of(State.IN_GAME));
		packet.setBuffer(payload(700123));

		packet.readImpl();

		assertEquals(700123, getField(packet, "doorId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int doorId) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(doorId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
