package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_ABYSS_RANKING_PLAYERS_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsRaceId() throws Exception {
		CM_ABYSS_RANKING_PLAYERS packet = new CM_ABYSS_RANKING_PLAYERS(188, Set.of(State.IN_GAME));
		packet.setBuffer(payload((byte) 0));

		packet.readImpl();

		assertEquals((byte) 0, getField(packet, "raceId"));
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
