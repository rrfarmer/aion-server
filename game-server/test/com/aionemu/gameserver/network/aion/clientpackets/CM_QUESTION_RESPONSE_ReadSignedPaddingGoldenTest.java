package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_QUESTION_RESPONSE_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_highBitPaddingDoesNotShiftMeaningfulFields() throws Exception {
		CM_QUESTION_RESPONSE packet = new CM_QUESTION_RESPONSE(50, Set.of(State.IN_GAME));
		packet.setBuffer(payload(900001, 1, 0x7F, 0xFFFF, 7001, 8002, 0x8000));

		packet.readImpl();

		assertEquals(900001, getField(packet, "questionid"));
		assertEquals(1, getField(packet, "response"));
		assertEquals(7001, getField(packet, "senderid"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int questionId, int response, int ignoredByte, int firstIgnoredShort, int senderId,
		int ignoredDword, int secondIgnoredShort) {
		ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(questionId);
		buffer.put((byte) response);
		buffer.put((byte) ignoredByte);
		buffer.putShort((short) firstIgnoredShort);
		buffer.putInt(senderId);
		buffer.putInt(ignoredDword);
		buffer.putShort((short) secondIgnoredShort);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
