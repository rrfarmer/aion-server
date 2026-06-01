package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_ABYSS_RANKING_LEGIONS_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsRaceId() throws Exception {
		CM_ABYSS_RANKING_LEGIONS packet = new CM_ABYSS_RANKING_LEGIONS(118, Set.of(State.IN_GAME));
		packet.setBuffer(payload((byte) 1));

		packet.readImpl();

		assertEquals((byte) 1, getField(packet, "raceId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(byte raceId) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(raceId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
