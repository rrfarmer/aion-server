package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PRIVATE_STORE_NAME_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsStoreNameAsJavaString() throws Exception {
		CM_PRIVATE_STORE_NAME packet = new CM_PRIVATE_STORE_NAME(120, Set.of(State.IN_GAME));
		packet.setBuffer(namePayload("For Atreia"));

		packet.readImpl();

		assertEquals("For Atreia", getField(packet, "name"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer namePayload(String name) {
		byte[] encoded = name.getBytes(StandardCharsets.UTF_16LE);
		ByteBuffer buffer = ByteBuffer.allocate(encoded.length + 2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(encoded);
		buffer.putChar('\0');
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
