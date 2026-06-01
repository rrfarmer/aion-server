package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_DISCONNECT_ReadPayloadGoldenTest {

	@Test
	public void readImpl_consumesSingleDisconnectFlagByte() {
		CM_DISCONNECT packet = new CM_DISCONNECT(2, Set.of(State.AUTHED, State.IN_GAME));
		packet.setBuffer(ByteBuffer.wrap(new byte[] { (byte) 0xFF }));

		packet.readImpl();

		assertEquals(0, packet.getRemainingBytes());
	}
}
