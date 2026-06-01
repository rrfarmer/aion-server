package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_VERSION_CHECK_ReadPayloadGoldenTest {

	@Test
	public void readImpl_readsUnsignedVersionsAndLiteInfo() throws Exception {
		CM_VERSION_CHECK packet = new CM_VERSION_CHECK(0, Set.of(State.CONNECTED));
		packet.setBuffer(payload());

		packet.readImpl();

		assertEquals(0xFFFF, getInt(packet, "aionClientVersion"));
		assertEquals(0x8001, getInt(packet, "npcScriptInterfaceVersion"));
		assertEquals(65001, getInt(packet, "windowsEncoding"));
		assertEquals(10, getInt(packet, "windowsVersion"));
		assertEquals(19045, getInt(packet, "windowsSubVersion"));
		assertEquals(2, getInt(packet, "liteInfo"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload() {
		ByteBuffer buffer = ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) 0xFFFF);
		buffer.putShort((short) 0x8001);
		buffer.putInt(65001);
		buffer.putInt(10);
		buffer.putInt(19045);
		buffer.put((byte) 2);
		buffer.flip();
		return buffer;
	}

	private static int getInt(CM_VERSION_CHECK packet, String fieldName) throws Exception {
		Field field = CM_VERSION_CHECK.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getInt(packet);
	}
}
