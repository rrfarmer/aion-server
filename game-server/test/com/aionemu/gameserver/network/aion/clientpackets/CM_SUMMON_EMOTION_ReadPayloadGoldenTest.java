package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_SUMMON_EMOTION_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsObjectIdAndUnsignedEmotionTypeId() throws Exception {
		CM_SUMMON_EMOTION packet = new CM_SUMMON_EMOTION(202, Set.of(State.IN_GAME));
		packet.setBuffer(payload(8103, 0xFF));

		packet.readImpl();

		assertEquals(8103, getInt(packet, "objId"));
		assertEquals(255, getInt(packet, "emotionTypeId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int objectId, int emotionTypeId) {
		ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(objectId);
		buffer.put((byte) emotionTypeId);
		buffer.flip();
		return buffer;
	}

	private static int getInt(CM_SUMMON_EMOTION packet, String fieldName) throws Exception {
		Field field = CM_SUMMON_EMOTION.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (int) field.get(packet);
	}
}
