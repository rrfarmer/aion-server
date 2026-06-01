package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_SUMMON_COMMAND_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsUnsignedModeSkipsPaddingAndReadsTargetObjectId() throws Exception {
		CM_SUMMON_COMMAND packet = new CM_SUMMON_COMMAND(121, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0xFF, 111, 222, 9001));

		packet.readImpl();

		assertEquals(255, getInt(packet, "mode"));
		assertEquals(9001, getInt(packet, "targetObjId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int mode, int unknown1, int unknown2, int targetObjectId) {
		ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) mode);
		buffer.putInt(unknown1);
		buffer.putInt(unknown2);
		buffer.putInt(targetObjectId);
		buffer.flip();
		return buffer;
	}

	private static int getInt(CM_SUMMON_COMMAND packet, String fieldName) throws Exception {
		Field field = CM_SUMMON_COMMAND.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (int) field.get(packet);
	}
}
