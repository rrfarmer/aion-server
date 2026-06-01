package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_STOP_TRAINING_ReadPayloadGoldenTest {

	@Test
	public void readImpl_consumesNoPayloadBytes() {
		CM_STOP_TRAINING packet = new CM_STOP_TRAINING(84, Set.of(State.IN_GAME));
		packet.setBuffer(ByteBuffer.wrap(new byte[0]));

		packet.readImpl();

		assertEquals(0, packet.getRemainingBytes());
	}
}
