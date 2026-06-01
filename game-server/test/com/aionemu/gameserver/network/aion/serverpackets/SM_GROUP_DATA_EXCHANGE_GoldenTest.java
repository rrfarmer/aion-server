package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

public class SM_GROUP_DATA_EXCHANGE_GoldenTest {

	@Test
	public void writeImpl_actionOneWritesActionSizeAndData() {
		byte[] payload = write(new SM_GROUP_DATA_EXCHANGE(new byte[] { 1, 2, (byte) 255 }));

		assertArrayEquals(new byte[] { 0x01, 0x03, 0x00, 0x00, 0x00, 0x01, 0x02, (byte) 0xFF }, payload);
	}

	@Test
	public void writeImpl_nonActionOneWritesActionUnknownSizeAndData() {
		byte[] payload = write(new SM_GROUP_DATA_EXCHANGE(new byte[] { 10, 11, 12, 13 }, 2, 7));

		assertArrayEquals(new byte[] { 0x02, 0x07, 0x04, 0x00, 0x00, 0x00, 0x0A, 0x0B, 0x0C, 0x0D }, payload);
	}

	private static byte[] write(SM_GROUP_DATA_EXCHANGE packet) {
		ByteBuffer buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
	}
}
