package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.EventTheme;

public class SM_VERSION_CHECK_GoldenTest {

	@Test
	public void writeImpl_incompatibleClientVersionWritesAnswerIdOnly() {
		SM_VERSION_CHECK packet = new SM_VERSION_CHECK(SM_VERSION_CHECK.INTERNAL_VERSION - 1, EventTheme.NONE);
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);

		assertEquals(207, SM_VERSION_CHECK.INTERNAL_VERSION);
		assertArrayEquals(new byte[] { 0x01 }, payload);
	}
}
