package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_UNWRAP_ITEM_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsObjectId() throws Exception {
		CM_UNWRAP_ITEM packet = new CM_UNWRAP_ITEM(240, Set.of(State.IN_GAME));
		packet.setBuffer(payload(188920001));

		packet.readImpl();

		assertEquals(188920001, getField(packet, "objectId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int objectId) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(objectId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
