package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_BREAK_WEAPONS_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsNpcAndWeaponObjectIds() throws Exception {
		CM_BREAK_WEAPONS packet = new CM_BREAK_WEAPONS(207, Set.of(State.IN_GAME));
		packet.setBuffer(payload(700001, 1001));

		packet.readImpl();

		assertEquals(700001, getField(packet, "npcObjId"));
		assertEquals(1001, getField(packet, "weaponObjId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int npcObjId, int weaponObjId) {
		ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(npcObjId);
		buffer.putInt(weaponObjId);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
