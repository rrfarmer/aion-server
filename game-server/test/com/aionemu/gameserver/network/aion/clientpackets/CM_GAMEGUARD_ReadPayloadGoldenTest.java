package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_GAMEGUARD_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsSizeAndConsumesPayload() throws Exception {
		CM_GAMEGUARD packet = new CM_GAMEGUARD(104, Set.of(State.IN_GAME, State.AUTHED));
		packet.setBuffer(payload(3, new byte[] { (byte) 0xA5, 0x5A, (byte) 0xFF }));

		packet.readImpl();

		assertEquals(3, getField(packet, "size"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int size, byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(4 + data.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(size);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
