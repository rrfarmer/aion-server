package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_DIALOG_SELECT_ReadUnsignedFieldsGoldenTest {

	@Test
	public void readImpl_readsUnsignedDialogFieldsAndQuestIdInOrder() throws Exception {
		CM_DIALOG_SELECT packet = new CM_DIALOG_SELECT(54, Set.of(State.IN_GAME));
		packet.setBuffer(payload(7001, 0x8002, 0xFFFF, 0x8004, 11056, 0x8009));

		packet.readImpl();

		assertEquals(7001, getField(packet, "targetObjectId"));
		assertEquals(0x8002, getField(packet, "dialogActionId"));
		assertEquals(0xFFFF, getField(packet, "extendedRewardIndex"));
		assertEquals(0x8004, getField(packet, "lastPage"));
		assertEquals(11056, getField(packet, "questId"));
		assertEquals(0x8009, getField(packet, "unk"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int targetObjectId, int dialogActionId, int extendedRewardIndex, int lastPage, int questId, int unk) {
		ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(targetObjectId);
		buffer.putShort((short) dialogActionId);
		buffer.putShort((short) extendedRewardIndex);
		buffer.putShort((short) lastPage);
		buffer.putInt(questId);
		buffer.putShort((short) unk);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
