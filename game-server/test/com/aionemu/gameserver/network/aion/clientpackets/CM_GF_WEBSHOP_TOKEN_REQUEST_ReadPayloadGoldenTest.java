package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_GF_WEBSHOP_TOKEN_REQUEST_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsNoPayload() {
		CM_GF_WEBSHOP_TOKEN_REQUEST packet = new CM_GF_WEBSHOP_TOKEN_REQUEST(229, Set.of(State.IN_GAME));
		packet.setBuffer(ByteBuffer.allocate(0));

		packet.readImpl();

		assertEquals(0, packet.getRemainingBytes());
	}
}
