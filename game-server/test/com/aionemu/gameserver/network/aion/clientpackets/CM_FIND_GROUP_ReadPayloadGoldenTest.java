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

public class CM_FIND_GROUP_ReadPayloadGoldenTest {

	@Test
	public void readImpl_actionZeroReadsOnlyAction() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> buffer.put((byte) 0)));

		packet.readImpl();

		assertEquals(0, getField(packet, "action"));
		assertEquals(0, getField(packet, "playerOrTeamId"));
		assertNull(getField(packet, "message"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionTwoReadsRecruitmentOffer() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 2);
			buffer.putInt(700100);
			putS(buffer, "Need cleric");
			buffer.put((byte) 3);
		}));

		packet.readImpl();

		assertEquals(2, getField(packet, "action"));
		assertEquals(700100, getField(packet, "playerOrTeamId"));
		assertEquals("Need cleric", getField(packet, "message"));
		assertEquals(3, getField(packet, "groupType"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionEightReadsInstanceGroupRegistration() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 8);
			buffer.putInt(300320000);
			buffer.put((byte) 0);
			putS(buffer, "Dredgion now");
			buffer.put((byte) 6);
		}));

		packet.readImpl();

		assertEquals(8, getField(packet, "action"));
		assertEquals(300320000, getField(packet, "instanceMaskId"));
		assertEquals("Dredgion now", getField(packet, "message"));
		assertEquals(6, getField(packet, "minMembers"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(BufferWriter writer) {
		ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
		writer.write(buffer);
		buffer.flip();
		return buffer;
	}

	private static void putS(ByteBuffer buffer, String value) {
		buffer.put(value.getBytes(StandardCharsets.UTF_16LE));
		buffer.putShort((short) 0);
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	@FunctionalInterface
	private interface BufferWriter {
		void write(ByteBuffer buffer);
	}
}
