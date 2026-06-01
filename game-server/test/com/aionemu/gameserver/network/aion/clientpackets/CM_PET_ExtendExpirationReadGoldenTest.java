package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.PetAction;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_PET_ExtendExpirationReadGoldenTest {

	@Test
	public void readImpl_extendExpirationReadsItemAndPetObjectIds() throws Exception {
		CM_PET packet = new CM_PET(22, Set.of(State.IN_GAME));
		packet.setBuffer(payload(7001, 8801));

		packet.readImpl();

		assertEquals(PetAction.EXTEND_EXPIRATION, get(packet, "action"));
		assertEquals(7001, getInt(packet, "eggObjId"));
		assertEquals(8801, getInt(packet, "objectId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int itemObjectId, int petObjectId) {
		ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) PetAction.EXTEND_EXPIRATION.getActionId());
		buffer.putInt(itemObjectId);
		buffer.putInt(petObjectId);
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
