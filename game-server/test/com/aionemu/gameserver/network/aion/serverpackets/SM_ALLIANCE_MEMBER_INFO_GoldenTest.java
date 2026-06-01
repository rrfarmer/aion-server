package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.state.FlyState;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.team.alliance.PlayerAllianceMember;
import com.aionemu.gameserver.model.team.common.legacy.PlayerAllianceEvent;
import com.aionemu.gameserver.world.WorldPosition;

import sun.misc.Unsafe;

public class SM_ALLIANCE_MEMBER_INFO_GoldenTest {

	@Test
	public void writeImpl_movementWritesFixedPrefixAndNoBranchPayload() throws Exception {
		Player player = player(2004, "AllyMover", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(player, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		player.setFlyState(FlyState.FLYING);
		setField(player.getLifeStats(), "currentHp", 777);
		setField(player.getLifeStats(), "currentMp", 333);
		setField(player.getLifeStats(), "currentFp", 60);
		PlayerAllianceMember member = new PlayerAllianceMember(player);
		member.setAllianceId(88001);

		byte[] payload = write(new SM_ALLIANCE_MEMBER_INFO(member, PlayerAllianceEvent.MOVEMENT));
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

		assertEquals(88001, buffer.getInt());
		assertEquals(2004, buffer.getInt());
		assertEquals(819, buffer.getInt());
		assertEquals(777, buffer.getInt());
		assertEquals(840, buffer.getInt());
		assertEquals(333, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(10.5f, buffer.getFloat());
		assertEquals(20.25f, buffer.getFloat());
		assertEquals(30.75f, buffer.getFloat());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(10, Byte.toUnsignedInt(buffer.get()));
		assertEquals(PlayerAllianceEvent.MOVEMENT.getId(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(FlyState.FLYING.getId(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.remaining());
	}

	@Test
	public void writeImpl_joinWritesOnlineNameZeroEffectPayload() throws Exception {
		Player player = player(2006, "AllianceJoin", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(player, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		PlayerAllianceMember member = member(player, 88001);

		byte[] payload = write(new SM_ALLIANCE_MEMBER_INFO(member, PlayerAllianceEvent.JOIN));
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

		assertOnlinePrefix(buffer, 88001, 2006, PlayerAllianceEvent.JOIN);
		assertEquals("AllianceJoin", readS(buffer));
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(127, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		for (int i = 0; i < 8; i++)
			assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	@Test
	public void writeImpl_enterOfflineWritesEffectiveEventAndNamePayload() throws Exception {
		Player player = player(2007, "AllianceOffline", PlayerClass.RIDER, Gender.MALE, 20, false);
		setField(player, "position", new WorldPosition(210010000, 1.25f, 2.5f, 3.75f, (byte) 0));
		PlayerAllianceMember member = member(player, 88002);

		byte[] payload = write(new SM_ALLIANCE_MEMBER_INFO(member, PlayerAllianceEvent.ENTER));
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

		assertEquals(88002, buffer.getInt());
		assertEquals(2007, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(210010000, buffer.getInt());
		assertEquals(210010000, buffer.getInt());
		assertEquals(1.25f, buffer.getFloat());
		assertEquals(2.5f, buffer.getFloat());
		assertEquals(3.75f, buffer.getFloat());
		assertEquals(13, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(20, Byte.toUnsignedInt(buffer.get()));
		assertEquals(PlayerAllianceEvent.ENTER_OFFLINE.getId(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals("AllianceOffline", readS(buffer));
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0, buffer.remaining());
	}

	@Test
	public void writeImpl_updateEffectsWritesTargetSlotZeroEffectPayload() throws Exception {
		Player player = player(2008, "AllianceEffects", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(player, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		PlayerAllianceMember member = member(player, 88003);

		byte[] payload = write(new SM_ALLIANCE_MEMBER_INFO(member, PlayerAllianceEvent.UPDATE_EFFECTS, 4));
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

		assertEquals(88003, buffer.getInt());
		assertEquals(2008, buffer.getInt());
		assertEquals(819, buffer.getInt());
		assertEquals(819, buffer.getInt());
		assertEquals(840, buffer.getInt());
		assertEquals(840, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(10.5f, buffer.getFloat());
		assertEquals(20.25f, buffer.getFloat());
		assertEquals(30.75f, buffer.getFloat());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(10, Byte.toUnsignedInt(buffer.get()));
		assertEquals(PlayerAllianceEvent.UPDATE_EFFECTS.getId(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(4, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		for (int i = 0; i < 8; i++)
			assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	@Test
	public void writeImpl_memberGroupChangeWritesNameOnlyDespiteJoinWireId() throws Exception {
		Player player = player(2009, "AllianceShift", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(player, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		PlayerAllianceMember member = member(player, 88004);

		byte[] payload = write(new SM_ALLIANCE_MEMBER_INFO(member, PlayerAllianceEvent.MEMBER_GROUP_CHANGE));
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

		assertOnlinePrefix(buffer, 88004, 2009, PlayerAllianceEvent.MEMBER_GROUP_CHANGE);
		assertEquals("AllianceShift", readS(buffer));
		assertEquals(0, buffer.remaining());
	}

	@Test
	public void writeImpl_enterAndUpdateWriteOnlineNameZeroEffectPayloads() throws Exception {
		Player enterPlayer = player(2010, "AllianceEnter", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(enterPlayer, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		Player updatePlayer = player(2011, "AllianceUpdate", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(updatePlayer, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));

		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(enterPlayer, 88005), PlayerAllianceEvent.ENTER)), 88005, 2010,
			"AllianceEnter", PlayerAllianceEvent.ENTER);
		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(updatePlayer, 88006), PlayerAllianceEvent.UPDATE)), 88006, 2011,
			"AllianceUpdate", PlayerAllianceEvent.UPDATE);
	}

	@Test
	public void writeImpl_captainRoleEventsWriteOnlineNameZeroEffectPayloads() throws Exception {
		Player appointVicePlayer = player(2012, "AllianceVice", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(appointVicePlayer, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		Player demoteVicePlayer = player(2013, "AllianceDemote", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(demoteVicePlayer, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));
		Player appointCaptainPlayer = player(2014, "AllianceCaptain", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(appointCaptainPlayer, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));

		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(appointVicePlayer, 88007), PlayerAllianceEvent.APPOINT_VICE_CAPTAIN)),
			88007, 2012, "AllianceVice", PlayerAllianceEvent.APPOINT_VICE_CAPTAIN);
		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(demoteVicePlayer, 88008), PlayerAllianceEvent.DEMOTE_VICE_CAPTAIN)),
			88008, 2013, "AllianceDemote", PlayerAllianceEvent.DEMOTE_VICE_CAPTAIN);
		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(appointCaptainPlayer, 88009), PlayerAllianceEvent.APPOINT_CAPTAIN)),
			88009, 2014, "AllianceCaptain", PlayerAllianceEvent.APPOINT_CAPTAIN);
	}

	@Test
	public void writeImpl_reconnectWritesOnlineNameZeroEffectPayload() throws Exception {
		Player player = player(2015, "AllianceReconnect", PlayerClass.GLADIATOR, Gender.FEMALE, 10, true);
		setField(player, "position", new WorldPosition(220010000, 10.5f, 20.25f, 30.75f, (byte) 64));

		assertOnlineNameZeroEffectPayload(write(new SM_ALLIANCE_MEMBER_INFO(member(player, 88010), PlayerAllianceEvent.RECONNECT)), 88010, 2015,
			"AllianceReconnect", PlayerAllianceEvent.RECONNECT);
	}

	private static Player player(int objectId, String name, PlayerClass playerClass, Gender gender, int level, boolean online) throws Exception {
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName(name);
		commonData.setPlayerClass(playerClass);
		commonData.setGender(gender);
		commonData.setOnline(online);
		setField(commonData, "level", level);
		PlayerAppearance appearance = new PlayerAppearance();
		appearance.setHeight(1);
		Player player = (Player) unsafe().allocateInstance(Player.class);
		setAionObjectId(player, objectId);
		setField(player, "playerAccountData", new PlayerAccountData(commonData, appearance));
		setField(player, "playerAccount", new Account(1));
		if (online)
			setUnsafeReference(player, "clientConnection", new Object());
		CustomConfig.BASE_FLYTIME = 60;
		player.setEffectController(new PlayerEffectController(player));
		player.setGameStats(new PlayerGameStats(player));
		player.setLifeStats(new PlayerLifeStats(player));
		return player;
	}

	private static byte[] write(SM_ALLIANCE_MEMBER_INFO packet) {
		ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
	}

	private static PlayerAllianceMember member(Player player, int allianceId) {
		PlayerAllianceMember member = new PlayerAllianceMember(player);
		member.setAllianceId(allianceId);
		return member;
	}

	private static void assertOnlinePrefix(ByteBuffer buffer, int allianceId, int objectId, PlayerAllianceEvent event) {
		assertEquals(allianceId, buffer.getInt());
		assertEquals(objectId, buffer.getInt());
		assertEquals(819, buffer.getInt());
		assertEquals(819, buffer.getInt());
		assertEquals(840, buffer.getInt());
		assertEquals(840, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(60, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(220010000, buffer.getInt());
		assertEquals(10.5f, buffer.getFloat());
		assertEquals(20.25f, buffer.getFloat());
		assertEquals(30.75f, buffer.getFloat());
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(10, Byte.toUnsignedInt(buffer.get()));
		assertEquals(event.getId(), Byte.toUnsignedInt(buffer.get()));
		assertEquals(1, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Byte.toUnsignedInt(buffer.get()));
	}

	private static void assertOnlineNameZeroEffectPayload(byte[] payload, int allianceId, int objectId, String name, PlayerAllianceEvent event) {
		ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
		assertOnlinePrefix(buffer, allianceId, objectId, event);
		assertEquals(name, readS(buffer));
		assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.getInt());
		assertEquals(127, Byte.toUnsignedInt(buffer.get()));
		assertEquals(0, Short.toUnsignedInt(buffer.getShort()));
		for (int i = 0; i < 8; i++)
			assertEquals(0, buffer.getInt());
		assertEquals(0, buffer.remaining());
	}

	private static String readS(ByteBuffer buffer) {
		StringBuilder value = new StringBuilder();
		while (buffer.remaining() >= Short.BYTES) {
			char c = (char) Short.toUnsignedInt(buffer.getShort());
			if (c == 0)
				return value.toString();
			value.append(c);
		}
		throw new IllegalStateException("Unterminated string in SM_ALLIANCE_MEMBER_INFO payload");
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = findField(target.getClass(), name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void setUnsafeReference(Object target, String name, Object value) throws Exception {
		Field field = findField(target.getClass(), name);
		unsafe().putObject(target, unsafe().objectFieldOffset(field), value);
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
