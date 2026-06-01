package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_VIEW_PLAYER_DETAILS_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsTargetObjectId() throws Exception {
		CM_VIEW_PLAYER_DETAILS packet = new CM_VIEW_PLAYER_DETAILS(100, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0x01020304));

		packet.readImpl();

		assertEquals(0x01020304, getField(packet, "targetObjectId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int targetObjectId) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(targetObjectId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
