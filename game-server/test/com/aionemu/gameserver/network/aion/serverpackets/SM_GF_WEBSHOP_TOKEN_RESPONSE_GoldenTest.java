package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

public class SM_GF_WEBSHOP_TOKEN_RESPONSE_GoldenTest {

	@Test
	public void writeImpl_writesEmptyFixedLengthToken() {
		SM_GF_WEBSHOP_TOKEN_RESPONSE packet = new SM_GF_WEBSHOP_TOKEN_RESPONSE("");
		ByteBuffer buffer = ByteBuffer.allocate(66).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);

		assertArrayEquals(new byte[66], payload);
	}
}
