package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_TOGGLE_SKILL_DEACTIVATE_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftUnsignedSkillId() throws Exception {
		CM_TOGGLE_SKILL_DEACTIVATE packet = new CM_TOGGLE_SKILL_DEACTIVATE(34, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0xABCD, 0xFFFF, 0x8000));

		packet.readImpl();

		assertEquals(0xABCD, getField(packet, "skillId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int skillId, int ignoredShort1, int ignoredShort2) {
		ByteBuffer buffer = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) skillId);
		buffer.putShort((short) ignoredShort1);
		buffer.putShort((short) ignoredShort2);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
