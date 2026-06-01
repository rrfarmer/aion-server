package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_LEGION_ReadSignedPermissionsGoldenTest {

	@Test
	public void readImpl_editPermissionsReadsSignedShorts() throws Exception {
		CM_LEGION packet = new CM_LEGION(45, Set.of(State.IN_GAME));
		packet.setBuffer(permissionsPayload(0xFFFF, 0x8000, 0x7FFF, 1));

		packet.readImpl();

		assertEquals(0x0D, getField(packet, "exOpcode"));
		assertEquals((short) -1, getField(packet, "deputyPermission"));
		assertEquals(Short.MIN_VALUE, getField(packet, "centurionPermission"));
		assertEquals(Short.MAX_VALUE, getField(packet, "legionarPermission"));
		assertEquals((short) 1, getField(packet, "volunteerPermission"));
	}

	private static ByteBuffer permissionsPayload(int deputy, int centurion, int legionar, int volunteer) {
		ByteBuffer buffer = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) 0x0D);
		buffer.putShort((short) deputy);
		buffer.putShort((short) centurion);
		buffer.putShort((short) legionar);
		buffer.putShort((short) volunteer);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
