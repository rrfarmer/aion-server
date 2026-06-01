package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_CHARACTER_PASSKEY_ReadSignedTypeGoldenTest {

	@Test
	public void readImpl_highBitTypeIsSignedShortAndDoesNotReadNewPasskey() throws Exception {
		CM_CHARACTER_PASSKEY packet = new CM_CHARACTER_PASSKEY(210, Set.of(State.AUTHED));
		packet.setBuffer(payload(0xFFFF, "old-pass", "new-pass"));

		packet.readImpl();

		assertEquals((short) -1, getField(packet, "type"));
		assertNull(getField(packet, "newPasskey"));
	}

	@Test
	public void readImpl_updateTypeReadsNewPasskey() throws Exception {
		CM_CHARACTER_PASSKEY packet = new CM_CHARACTER_PASSKEY(210, Set.of(State.AUTHED));
		packet.setBuffer(payload(2, "old-pass", "new-pass"));

		packet.readImpl();

		assertEquals((short) 2, getField(packet, "type"));
		assertEquals(fixedUtf16String("new-pass"), getField(packet, "newPasskey"));
	}

	private static ByteBuffer payload(int type, String passkey, String newPasskey) {
		ByteBuffer buffer = ByteBuffer.allocate(2 + 48 + 48).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) type);
		buffer.put(fixedUtf16Bytes(passkey));
		buffer.put(fixedUtf16Bytes(newPasskey));
		buffer.flip();
		return buffer;
	}

	private static byte[] fixedUtf16Bytes(String value) {
		byte[] bytes = new byte[48];
		byte[] encoded = value.getBytes(StandardCharsets.UTF_16LE);
		System.arraycopy(encoded, 0, bytes, 0, Math.min(encoded.length, bytes.length));
		return bytes;
	}

	private static String fixedUtf16String(String value) {
		return new String(fixedUtf16Bytes(value), StandardCharsets.UTF_16LE);
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
