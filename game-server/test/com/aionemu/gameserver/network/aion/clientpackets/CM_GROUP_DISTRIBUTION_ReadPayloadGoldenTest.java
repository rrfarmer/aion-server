package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_GROUP_DISTRIBUTION_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsAmountAndPartyType() throws Exception {
		CM_GROUP_DISTRIBUTION packet = new CM_GROUP_DISTRIBUTION(108, Set.of(State.IN_GAME));
		packet.setBuffer(payload(9876543210L, (byte) 3));

		packet.readImpl();

		assertEquals(9876543210L, getField(packet, "amount"));
		assertEquals((byte) 3, getField(packet, "partyType"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(long amount, byte partyType) {
		ByteBuffer buffer = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(amount);
		buffer.put(partyType);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
