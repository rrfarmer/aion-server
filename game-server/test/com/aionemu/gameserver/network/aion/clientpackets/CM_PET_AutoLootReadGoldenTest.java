package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PET_AutoLootReadGoldenTest {

	@Test
	public void readImpl_foodActionTypeThreeReadsActivationAndSkipsPadding() throws Exception {
		CM_PET packet = new CM_PET(22, Set.of(State.IN_GAME));
		packet.setBuffer(payload(1));

		packet.readImpl();

		assertEquals(PetAction.FOOD, get(packet, "action"));
		assertEquals(3, getInt(packet, "actionType"));
		assertEquals(1, getInt(packet, "activateSpecialFunction"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int activateSpecialFunction) {
		ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) PetAction.FOOD.getActionId());
		buffer.putInt(3);
		buffer.putInt(activateSpecialFunction);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.flip();
		return buffer;
	}

	private static Object get(CM_PET packet, String fieldName) throws Exception {
		Field field = CM_PET.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(packet);
	}

	private static int getInt(CM_PET packet, String fieldName) throws Exception {
		return (int) get(packet, fieldName);
	}
}
