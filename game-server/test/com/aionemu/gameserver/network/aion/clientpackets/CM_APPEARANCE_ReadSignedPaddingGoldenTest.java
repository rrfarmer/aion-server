package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_APPEARANCE_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftRenameFields() throws Exception {
		CM_APPEARANCE packet = new CM_APPEARANCE(197, Set.of(State.IN_GAME));
		packet.setBuffer(renamePayload(0, 0x7F, 0xFFFF, 9001, "Newname"));

		packet.readImpl();

		assertEquals((byte) 0, getField(packet, "type"));
		assertEquals(9001, getField(packet, "itemObjId"));
		assertEquals("Newname", getField(packet, "newName"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer renamePayload(int type, int ignoredByte, int ignoredShort, int itemObjId, String newName) {
		byte[] nameBytes = newName.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
		ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 2 + 4 + nameBytes.length + 2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) type);
		buffer.put((byte) ignoredByte);
		buffer.putShort((short) ignoredShort);
		buffer.putInt(itemObjId);
		buffer.put(nameBytes);
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
