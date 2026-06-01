package com.aionemu.gameserver.network.aion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

public class AionClientPacketFactoryRegistrationGoldenTest {

	@Test
	public void opcode91GodstoneSocketRemainsUnregistered() throws Exception {
		Object[] packets = getPacketInfoTable();

		assertNotNull(packets[74], "CM_MANASTONE stays registered for modern godstone socketing.");
		assertNull(packets[91], "CM_GODSTONE_SOCKET is intentionally not registered in Java.");
	}

	private static Object[] getPacketInfoTable() throws Exception {
		Field field = AionClientPacketFactory.class.getDeclaredField("packets");
		field.setAccessible(true);
		return (Object[]) field.get(null);
	}
}
