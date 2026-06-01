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
		ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
		packet.setBuf(buffer);

		packet.writeImpl(null);

		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
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
