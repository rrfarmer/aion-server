package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_DELETE_ITEM_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsItemObjectId() {
		CM_DELETE_ITEM packet = new CM_DELETE_ITEM(116, Set.of(State.IN_GAME));
		packet.setBuffer(payload(9001));

		packet.readImpl();

		assertEquals(9001, packet.itemObjectId);
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int itemObjectId) {
		ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(itemObjectId);
		buffer.flip();
		return buffer;
	}
}
