package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PET_AutoSellReadGoldenTest {

	@Test
	public void readImpl_foodActionTypeFourReadsActivationAndSkipsPadding() throws Exception {
		CM_PET packet = new CM_PET(22, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0x80000000, 0x7fffffff, 0xffffffff));

		packet.readImpl();

		assertEquals(PetAction.FOOD, get(packet, "action"));
		assertEquals(4, getInt(packet, "actionType"));
		assertEquals(0x80000000, getInt(packet, "activateSpecialFunction"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int activateSpecialFunction, int ignored1, int ignored2) {
		ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) PetAction.FOOD.getActionId());
		buffer.putInt(4);
		buffer.putInt(activateSpecialFunction);
		buffer.putInt(ignored1);
		buffer.putInt(ignored2);
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
