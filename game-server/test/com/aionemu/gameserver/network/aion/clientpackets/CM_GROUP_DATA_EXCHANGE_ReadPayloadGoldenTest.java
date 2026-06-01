package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_GROUP_DATA_EXCHANGE_ReadPayloadGoldenTest {

	@Test
	public void readImpl_actionOneReadsOnlyActionSizeAndData() throws Exception {
		CM_GROUP_DATA_EXCHANGE packet = new CM_GROUP_DATA_EXCHANGE(79, Set.of(State.IN_GAME));
		packet.setBuffer(payload((byte) 1, null, null, new byte[] { 1, 2, (byte) 255 }));

		packet.readImpl();

		assertEquals(1, getField(packet, "action"));
		assertEquals(0, getField(packet, "groupType"));
		assertEquals(0, getField(packet, "unk2"));
		assertArrayEquals(new byte[] { 1, 2, (byte) 255 }, (byte[]) getField(packet, "data"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_nonActionOneReadsGroupTypeUnknownSizeAndData() throws Exception {
		CM_GROUP_DATA_EXCHANGE packet = new CM_GROUP_DATA_EXCHANGE(79, Set.of(State.IN_GAME));
		packet.setBuffer(payload((byte) 2, (byte) 1, (byte) 7, new byte[] { 10, 11, 12, 13 }));

		packet.readImpl();

		assertEquals(2, getField(packet, "action"));
		assertEquals(1, getField(packet, "groupType"));
		assertEquals(7, getField(packet, "unk2"));
		assertArrayEquals(new byte[] { 10, 11, 12, 13 }, (byte[]) getField(packet, "data"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(byte action, Byte groupType, Byte unk2, byte[] data) {
		int headerSize = action == 1 ? 5 : 7;
		ByteBuffer buffer = ByteBuffer.allocate(headerSize + data.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(action);
		if (action != 1) {
			buffer.put(groupType);
			buffer.put(unk2);
		}
		buffer.putInt(data.length);
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
