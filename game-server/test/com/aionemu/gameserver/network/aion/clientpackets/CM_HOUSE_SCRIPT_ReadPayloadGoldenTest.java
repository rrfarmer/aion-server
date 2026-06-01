package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_SCRIPTS;

public class CM_HOUSE_SCRIPT_ReadPayloadGoldenTest {

	@Test
	public void readImpl_validCompressedScriptReadsContent() throws Exception {
		CM_HOUSE_SCRIPT packet = new CM_HOUSE_SCRIPT(30, Set.of(State.IN_GAME));
		packet.setBuffer(payload(12345, 255, 11, 3, 9, new byte[] { 1, 2, 3 }));

		packet.readImpl();

		assertEquals(12345, getField(packet, "address"));
		assertEquals(255, getField(packet, "scriptId"));
		assertEquals(11, getField(packet, "totalSize"));
		assertEquals(3, getField(packet, "compressedSize"));
		assertEquals(9, getField(packet, "uncompressedSize"));
		assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) getField(packet, "scriptContent"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_oversizedCompressedScriptStopsBeforeUncompressedSize() throws Exception {
		CM_HOUSE_SCRIPT packet = new CM_HOUSE_SCRIPT(30, Set.of(State.IN_GAME));
		packet.setBuffer(payload(12345, 7, 11, SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE + 1, 9, new byte[] { 1, 2, 3 }));

		packet.readImpl();

		assertEquals(SM_HOUSE_SCRIPTS.MAX_COMPRESSED_SCRIPT_SIZE + 1, getField(packet, "compressedSize"));
		assertEquals(0, getField(packet, "uncompressedSize"));
		assertEquals(null, getField(packet, "scriptContent"));
		assertEquals(7, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(int address, int scriptId, int totalSize, int compressedSize, int uncompressedSize, byte[] scriptContent) {
		ByteBuffer buffer = ByteBuffer.allocate(15 + scriptContent.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(address);
		buffer.put((byte) scriptId);
		buffer.putShort((short) totalSize);
		buffer.putInt(compressedSize);
		buffer.putInt(uncompressedSize);
		buffer.put(scriptContent);
		buffer.flip();
		return buffer;
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
