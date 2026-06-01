package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PING_ReadSignedUnknownGoldenTest {

	@Test
	public void readImpl_highBitUnknownIsConsumedLikeSignedReadH() {
		CM_PING packet = new CM_PING(44, Set.of(State.AUTHED, State.IN_GAME));
		packet.setBuffer(payload(0xFFFF));

		packet.readImpl();

		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int unknown) {
		ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) unknown);
		buffer.flip();
		return buffer;
	}
}
