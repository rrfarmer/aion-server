package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_CHALLENGE_LIST_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsUnsignedActionsAndTaskFields() throws Exception {
		CM_CHALLENGE_LIST packet = new CM_CHALLENGE_LIST(232, Set.of(State.IN_GAME));
		packet.setBuffer(payload(255, 7001, 1, 1001, 20240601));

		packet.readImpl();

		assertEquals(255, getField(packet, "action"));
		assertEquals(7001, getField(packet, "taskOwner"));
		assertEquals(1, getField(packet, "ownerType"));
		assertEquals(1001, getField(packet, "playerId"));
		assertEquals(20240601, getField(packet, "dateSince"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int action, int taskOwner, int ownerType, int playerId, int dateSince) {
		ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) action);
		buffer.putInt(taskOwner);
		buffer.put((byte) ownerType);
		buffer.putInt(playerId);
		buffer.putInt(dateSince);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
