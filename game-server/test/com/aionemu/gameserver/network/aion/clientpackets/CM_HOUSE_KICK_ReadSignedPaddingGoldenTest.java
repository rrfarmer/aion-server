package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_HOUSE_KICK_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftOption() throws Exception {
		CM_HOUSE_KICK packet = new CM_HOUSE_KICK(72, Set.of(State.IN_GAME));
		packet.setBuffer(payload(2, 0xFFFF));

		packet.readImpl();

		assertEquals((byte) 2, getField(packet, "option"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int option, int ignoredShort) {
		ByteBuffer buffer = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) option);
		buffer.putShort((short) ignoredShort);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
