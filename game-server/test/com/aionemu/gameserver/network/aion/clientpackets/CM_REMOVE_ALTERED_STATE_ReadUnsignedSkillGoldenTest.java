package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_REMOVE_ALTERED_STATE_ReadUnsignedSkillGoldenTest {

	@Test
	public void readImpl_highBitSkillIdReadsAsUnsignedShortAndConsumesTrailingBytes() throws Exception {
		CM_REMOVE_ALTERED_STATE packet = new CM_REMOVE_ALTERED_STATE(35, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0xFFFF, 0x80, 0x01));

		packet.readImpl();

		assertEquals(65535, getField(packet, "skillId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int skillId, int unknown1, int unknown2) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) skillId);
		buffer.put((byte) unknown1);
		buffer.put((byte) unknown2);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
