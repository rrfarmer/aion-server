package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupApplication;
import com.aionemu.gameserver.model.gameobjects.findGroup.GroupRecruitment;
import com.aionemu.gameserver.model.gameobjects.findGroup.ServerWideGroup;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.world.WorldPosition;

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
	public void writeImpl_showApplicationsWritesTimestampedApplicationSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			GroupApplication application = new GroupApplication(
				simplePlayer(0x01020304, "Applicant", PlayerClass.RANGER, 65), "Apply", 1, PlayerClass.RANGER.getClassId(), 65);
			setField(application, "lastUpdate", 0x01020305);
			SM_FIND_GROUP packet = new SM_FIND_GROUP(4, List.of(application));

			int before = (int) (System.currentTimeMillis() / 1000);
			byte[] payload = write(packet);
			int after = (int) (System.currentTimeMillis() / 1000);

			ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
			assertEquals(4, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			int headerLastUpdate = buffer.getInt();
			assertTrue(headerLastUpdate >= before && headerLastUpdate <= after);
			assertEquals(0x01020304, buffer.getInt());
			assertEquals(1, Byte.toUnsignedInt(buffer.get()));
			assertEquals("Apply", readS(buffer));
			assertEquals("Applicant", readS(buffer));
			assertEquals(PlayerClass.RANGER.getClassId(), Byte.toUnsignedInt(buffer.get()));
			assertEquals(65, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0x01020305, buffer.getInt());
			assertEquals(0, buffer.remaining());
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
	}

	@Test
	public void writeImpl_enableRegisterForInstancesWritesActionCountAndMasks() {
		SM_FIND_GROUP packet = new SM_FIND_GROUP(List.of(300110000, 300150000));
		byte[] payload = write(packet);

		assertEquals("1A0200B050E311F0ECE311", toHex(payload));
	}

	@Test
	public void writeImpl_showRecruitmentsWritesTimestampedSoloRecruitmentSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		int originalGameServerId = NetworkConfig.GAMESERVER_ID;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			NetworkConfig.GAMESERVER_ID = 1;
			GroupRecruitment recruitment = new GroupRecruitment(simplePlayer(0x01020304, "Recruiter", PlayerClass.GLADIATOR, 65), "LFG", 2);
			setField(recruitment, "lastUpdate", 0x01020305);
			SM_FIND_GROUP packet = new SM_FIND_GROUP(0, List.of(recruitment));

			int before = (int) (System.currentTimeMillis() / 1000);
			byte[] payload = write(packet);
			int after = (int) (System.currentTimeMillis() / 1000);

			ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			int headerLastUpdate = buffer.getInt();
			assertTrue(headerLastUpdate >= before && headerLastUpdate <= after);
			assertEquals(0x01020304, buffer.getInt());
			assertEquals(1, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(16, Byte.toUnsignedInt(buffer.get()));
			assertEquals(2, Byte.toUnsignedInt(buffer.get()));
			assertEquals("LFG", readS(buffer));
			assertEquals("Recruiter", readS(buffer));
			assertEquals(1, Byte.toUnsignedInt(buffer.get()));
			assertEquals(65, Byte.toUnsignedInt(buffer.get()));
			assertEquals(65, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0x01020305, buffer.getInt());
			assertEquals(0, buffer.remaining());
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
			NetworkConfig.GAMESERVER_ID = originalGameServerId;
		}
	}

	@Test
	public void writeImpl_showInstanceGroupsWritesTimestampedGroupSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			ServerWideGroup group = simpleInstanceGroup();
			setField(group, "lastUpdate", 0x01020305);
			SM_FIND_GROUP packet = new SM_FIND_GROUP(10, List.of(group));

			int before = (int) (System.currentTimeMillis() / 1000);
			byte[] payload = write(packet);
			int after = (int) (System.currentTimeMillis() / 1000);

			ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
			assertEquals(10, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			int headerLastUpdate = buffer.getInt();
			assertTrue(headerLastUpdate >= before && headerLastUpdate <= after);
			assertEquals(0x01020304, buffer.getInt());
			assertEquals(0x11223344, buffer.getInt());
			assertEquals(1, buffer.getInt());
			assertEquals(1, Byte.toUnsignedInt(buffer.get()));
			assertEquals(3, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(0x01020304, buffer.getInt());
			assertEquals(1, buffer.getInt());
			assertEquals(0, buffer.getInt());
			assertEquals(65, Byte.toUnsignedInt(buffer.get()));
			assertEquals(65, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(0x01020305, buffer.getInt());
			assertEquals(0, buffer.getInt());
			assertEquals("Recruiter", readS(buffer));
			assertEquals("Entry", readS(buffer));
			assertEquals(0, buffer.remaining());
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
	}

	@Test
	public void writeImpl_instanceApplicationWhisperWritesApplicantSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			SM_FIND_GROUP packet = new SM_FIND_GROUP(simplePlayer(0x01020304, "Applicant", PlayerClass.RANGER, 65));
			byte[] payload = write(packet);

			assertEquals("0B04030201000000000000000000000005410000004100700070006C006900630061006E0074000000", toHex(payload));
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
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

	@Test
	public void writeImpl_registerInstanceGroupWritesGroupSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			ServerWideGroup group = simpleInstanceGroup();
			setField(group, "lastUpdate", 0x01020305);
			SM_FIND_GROUP packet = new SM_FIND_GROUP(14, List.of(group));
			byte[] payload = write(packet);

			assertEquals(
				"0E0104030201443322110100000001030000040302010100010000000000414100000503020100000000520065006300720075006900740065007200000045006E007400720079000000",
				toHex(payload));
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
	}

	@Test
	public void writeImpl_destroyPrepareWindowWritesGroupAndEnterMessageFlag() throws Exception {
		ServerWideGroup group = simpleInstanceGroup();
		SM_FIND_GROUP packet = new SM_FIND_GROUP(23, List.of(group));
		byte[] payload = write(packet);

		assertEquals("17040302014433221100", toHex(payload));
	}

	@Test
	public void writeImpl_updatePrepareWindowWritesMemberSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			ServerWideGroup group = simpleInstanceGroup();
			SM_FIND_GROUP packet = new SM_FIND_GROUP(24, List.of(group));
			byte[] payload = write(packet);

			assertEquals(
				"180403020144332211010000000000000000040302014100000001000000000001005200650063007200750069007400650072000000",
				toHex(payload));
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
	}

	@Test
	public void writeImpl_showInstanceGroupMemberInfoWritesTimestampedMemberSnapshot() throws Exception {
		String[] originalNameTags = AdminConfig.NAME_TAGS;
		try {
			AdminConfig.NAME_TAGS = new String[0];
			ServerWideGroup group = simpleInstanceGroup();
			SM_FIND_GROUP packet = new SM_FIND_GROUP(16, List.of(group));

			int before = (int) (System.currentTimeMillis() / 1000);
			byte[] payload = write(packet);
			int after = (int) (System.currentTimeMillis() / 1000);

			ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
			assertEquals(16, Byte.toUnsignedInt(buffer.get()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			int lastUpdate = buffer.getInt();
			assertTrue(lastUpdate >= before && lastUpdate <= after);
			assertEquals(0, buffer.getInt());
			assertEquals(300110000, buffer.getInt());
			assertEquals(0x01020304, buffer.getInt());
			assertEquals(65, buffer.getInt());
			assertEquals(PlayerClass.GLADIATOR.getClassId(), buffer.getInt());
			assertEquals(1, Short.toUnsignedInt(buffer.getShort()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals(0, Byte.toUnsignedInt(buffer.get()));
			assertEquals("Recruiter", readS(buffer));
			assertEquals(0, buffer.remaining());
		} finally {
			AdminConfig.NAME_TAGS = originalNameTags;
		}
	}

	private static byte[] write(SM_FIND_GROUP packet) {
		ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
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

	private static String readS(ByteBuffer buffer) {
		StringBuilder value = new StringBuilder();
		while (buffer.remaining() >= 2) {
			char c = buffer.getChar();
			if (c == 0)
				return value.toString();
			value.append(c);
		}
		throw new IllegalStateException("String terminator not found");
	}

	private static ServerWideGroup simpleInstanceGroup() throws Exception {
		Player recruiter = simplePlayer(0x01020304, "Recruiter", PlayerClass.GLADIATOR, 65);
		return new ServerWideGroup(recruiter, 0x11223344, 3, "Entry");
	}

	private static Player simplePlayer(int objectId, String name, PlayerClass playerClass, int level) throws Exception {
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName(name);
		commonData.setPlayerClass(playerClass);
		setField(commonData, "level", level);
		PlayerAppearance appearance = new PlayerAppearance();
		appearance.setHeight(1);
		PlayerAccountData accountData = new PlayerAccountData(commonData, appearance);
		Account account = new Account(1);

		Player player = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(player, objectId);
		setField(player, "playerAccountData", accountData);
		setField(player, "playerAccount", account);
		setField(player, "position", new WorldPosition(300110000));
		return player;
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

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = findField(target.getClass(), name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
