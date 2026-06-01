package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_UI_SETTINGS_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftDeclaredSizeOrData() throws Exception {
		CM_UI_SETTINGS packet = new CM_UI_SETTINGS(10, Set.of(State.IN_GAME));
		packet.setBuffer(payload(2, 0xFFFF, 0x8000, new byte[] { 0x10, 0x20, 0x30 }));

		packet.readImpl();

		assertEquals((byte) 2, getField(packet, "settingsType"));
		assertEquals(0x8000, getField(packet, "size"));
		assertArrayEquals(new byte[] { 0x10, 0x20, 0x30 }, (byte[]) getField(packet, "data"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int settingsType, int ignoredShort, int declaredSize, byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(5 + data.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) settingsType);
		buffer.putShort((short) ignoredShort);
		buffer.putShort((short) declaredSize);
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
