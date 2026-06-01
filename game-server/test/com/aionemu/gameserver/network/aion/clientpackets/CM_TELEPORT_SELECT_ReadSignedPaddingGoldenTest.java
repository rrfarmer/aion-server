package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_TELEPORT_SELECT_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftTeleportFields() throws Exception {
		CM_TELEPORT_SELECT packet = new CM_TELEPORT_SELECT(148, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0x01020304, 0x05060708, 0xFFFF));

		packet.readImpl();

		assertEquals(0x01020304, getField(packet, "targetObjId"));
		assertEquals(0x05060708, getField(packet, "locId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int targetObjId, int locId, int ignoredShort) {
		ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(targetObjId);
		buffer.putInt(locId);
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
