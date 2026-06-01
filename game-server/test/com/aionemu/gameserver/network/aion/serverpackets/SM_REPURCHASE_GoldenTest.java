package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import sun.misc.Unsafe;

public class SM_REPURCHASE_GoldenTest {

	@Test
	public void writeImpl_writesEmptyRepurchaseListPayload() throws Exception {
		SM_REPURCHASE packet = allocatePacket();
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);

		assertEquals("29230000010000000000", toHex(payload));
		assertArrayEquals(new byte[] { 0x29, 0x23, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }, payload);
	}

	private static SM_REPURCHASE allocatePacket() throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		SM_REPURCHASE packet = (SM_REPURCHASE) unsafe.allocateInstance(SM_REPURCHASE.class);
		setField(packet, "targetObjectId", 9001);
		setField(packet, "items", Collections.emptyList());
		setField(packet, "player", null);
		return packet;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
			sb.append(String.format("%02X", value));
		return sb.toString();
	}
}
