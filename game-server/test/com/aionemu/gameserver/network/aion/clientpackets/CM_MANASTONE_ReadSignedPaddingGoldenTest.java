package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_MANASTONE_ReadSignedPaddingGoldenTest {

	@Test
	public void readImpl_removeManastoneHighBitPaddingDoesNotShiftNpcObjectId() throws Exception {
		CM_MANASTONE packet = new CM_MANASTONE(74, Set.of(State.IN_GAME));
		packet.setBuffer(removePayload(3, 1, 7001, 4, 0x7F, 0xFFFF, 9001));

		packet.readImpl();

		assertEquals(3, getField(packet, "actionType"));
		assertEquals(1, getField(packet, "targetFusedSlot"));
		assertEquals(7001, getField(packet, "targetItemUniqueId"));
		assertEquals(4, getField(packet, "slotNum"));
		assertEquals(9001, getField(packet, "npcObjId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer removePayload(int actionType, int targetFusedSlot, int targetItemUniqueId, int slotNum,
		int ignoredByte, int ignoredShort, int npcObjId) {
		ByteBuffer buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put((byte) actionType);
		buffer.put((byte) targetFusedSlot);
		buffer.putInt(targetItemUniqueId);
		buffer.put((byte) slotNum);
		buffer.put((byte) ignoredByte);
		buffer.putShort((short) ignoredShort);
		buffer.putInt(npcObjId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
