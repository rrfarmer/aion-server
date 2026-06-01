package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_MEGAPHONE_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsMessageAndItemObjectId() throws Exception {
		CM_MEGAPHONE packet = new CM_MEGAPHONE(237, Set.of(State.IN_GAME));
		packet.setBuffer(payload("Dredgion forming", 188910000));

		packet.readImpl();

		assertEquals("Dredgion forming", getField(packet, "message"));
		assertEquals(188910000, getField(packet, "itemObjId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(String message, int itemObjId) {
		ByteBuffer buffer = ByteBuffer.allocate((message.length() * 2) + 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < message.length(); i++)
			buffer.putChar(message.charAt(i));
		buffer.putChar('\0');
		buffer.putInt(itemObjId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
