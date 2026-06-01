package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import sun.misc.Unsafe;

public class SM_FIND_GROUP_GoldenTest {

	@Test
	public void writeImpl_removeRecruitmentWritesActionAndIds() {
		SM_FIND_GROUP packet = new SM_FIND_GROUP(700101, (byte) 5, (byte) 6, (byte) 7, (byte) 8);
		byte[] payload = write(packet);

		assertEquals("01C5AE0A0005060708", toHex(payload));
	}

	@Test
	public void writeImpl_removeApplicationWritesActionAndPlayerId() {
		SM_FIND_GROUP packet = new SM_FIND_GROUP(700105);
		byte[] payload = write(packet);

		assertEquals("05C9AE0A00", toHex(payload));
	}

	@Test
	public void writeImpl_enableRegisterForInstancesWritesActionCountAndMasks() {
		SM_FIND_GROUP packet = new SM_FIND_GROUP(List.of(300110000, 300150000));
		byte[] payload = write(packet);

		assertEquals("1A0200B050E311F0ECE311", toHex(payload));
	}

	@Test
	public void writeImpl_showEnterButtonWritesActionGroupIdAndInstanceMask() throws Exception {
		ServerWideGroup group = simpleInstanceGroup();
		SM_FIND_GROUP packet = new SM_FIND_GROUP(18, List.of(group));
		byte[] payload = write(packet);

		assertEquals("120403020144332211", toHex(payload));
	}

	@Test
	public void writeImpl_showPrepareWindowWritesActionGroupIdAndInstanceMask() throws Exception {
		ServerWideGroup group = simpleInstanceGroup();
		SM_FIND_GROUP packet = new SM_FIND_GROUP(22, List.of(group));
		byte[] payload = write(packet);

		assertEquals("160403020144332211", toHex(payload));
	}

	private static byte[] write(SM_FIND_GROUP packet) {
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);
		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
	}

	private static String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder(bytes.length * 2);
		for (byte value : bytes)
			hex.append(String.format("%02X", value));
		return hex.toString();
	}

	private static ServerWideGroup simpleInstanceGroup() throws Exception {
		Player recruiter = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(recruiter, 0x01020304);
		return new ServerWideGroup(recruiter, 0x11223344, 3, "Entry");
	}

	private static void setAionObjectId(AionObject object, int objectId) throws Exception {
		Field field = AionObject.class.getDeclaredField("objectId");
		unsafe().putInt(object, unsafe().objectFieldOffset(field), objectId);
	}

	private static Unsafe unsafe() throws Exception {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		return (Unsafe) unsafeField.get(null);
	}
}
