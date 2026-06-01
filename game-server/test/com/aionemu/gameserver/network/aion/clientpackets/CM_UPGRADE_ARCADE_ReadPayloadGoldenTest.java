package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_UPGRADE_ARCADE_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsActionAndSessionId() throws Exception {
		CM_UPGRADE_ARCADE packet = new CM_UPGRADE_ARCADE(246, Set.of(State.IN_GAME));
		packet.setBuffer(payload((byte) 5, 20260601));

		packet.readImpl();

		assertEquals((byte) 5, getField(packet, "action"));
		assertEquals(20260601, getField(packet, "sessionId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(byte action, int sessionId) {
		ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(action);
		buffer.putInt(sessionId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
