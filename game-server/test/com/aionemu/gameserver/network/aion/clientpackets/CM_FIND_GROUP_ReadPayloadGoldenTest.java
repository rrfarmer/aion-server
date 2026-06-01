package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_FIND_GROUP_ReadPayloadGoldenTest {

	@Test
	public void readImpl_actionZeroReadsOnlyAction() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> buffer.put((byte) 0)));

		packet.readImpl();

		assertEquals(0, getField(packet, "action"));
		assertEquals(0, getField(packet, "playerOrTeamId"));
		assertNull(getField(packet, "message"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionTwoReadsRecruitmentOffer() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 2);
			buffer.putInt(700100);
			putS(buffer, "Need cleric");
			buffer.put((byte) 3);
		}));

		packet.readImpl();

		assertEquals(2, getField(packet, "action"));
		assertEquals(700100, getField(packet, "playerOrTeamId"));
		assertEquals("Need cleric", getField(packet, "message"));
		assertEquals(3, getField(packet, "groupType"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionEightReadsInstanceGroupRegistration() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 8);
			buffer.putInt(300320000);
			buffer.put((byte) 0);
			putS(buffer, "Dredgion now");
			buffer.put((byte) 6);
		}));

		packet.readImpl();

		assertEquals(8, getField(packet, "action"));
		assertEquals(300320000, getField(packet, "instanceMaskId"));
		assertEquals("Dredgion now", getField(packet, "message"));
		assertEquals(6, getField(packet, "minMembers"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionOneReadsRecruitmentDelete() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 1);
			buffer.putInt(700101);
			buffer.put((byte) 5);
			buffer.put((byte) 6);
			buffer.put((byte) 7);
			buffer.put((byte) 8);
		}));

		packet.readImpl();

		assertEquals(1, getField(packet, "action"));
		assertEquals(700101, getField(packet, "playerOrTeamId"));
		assertEquals((byte) 5, getField(packet, "serverId"));
		assertEquals((byte) 6, getField(packet, "unk1"));
		assertEquals((byte) 7, getField(packet, "unk2"));
		assertEquals((byte) 8, getField(packet, "unk3"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionThreeReadsRecruitmentUpdate() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 3);
			buffer.putInt(700103);
			buffer.put((byte) 1);
			buffer.put((byte) 2);
			buffer.put((byte) 3);
			buffer.put((byte) 4);
			putS(buffer, "Update group");
			buffer.put((byte) 2);
		}));

		packet.readImpl();

		assertEquals(3, getField(packet, "action"));
		assertEquals(700103, getField(packet, "playerOrTeamId"));
		assertEquals((byte) 1, getField(packet, "serverId"));
		assertEquals((byte) 2, getField(packet, "unk1"));
		assertEquals((byte) 3, getField(packet, "unk2"));
		assertEquals((byte) 4, getField(packet, "unk3"));
		assertEquals("Update group", getField(packet, "message"));
		assertEquals(2, getField(packet, "groupType"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionFiveReadsPostDelete() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 5);
			buffer.putInt(700105);
		}));

		packet.readImpl();

		assertEquals(5, getField(packet, "action"));
		assertEquals(700105, getField(packet, "playerOrTeamId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionSixReadsApplicationCreate() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 6);
			buffer.putInt(700106);
			putS(buffer, "Apply");
			buffer.put((byte) 1);
			buffer.put((byte) 4);
			buffer.put((byte) 65);
		}));

		packet.readImpl();

		assertEquals(6, getField(packet, "action"));
		assertEquals(700106, getField(packet, "playerOrTeamId"));
		assertEquals("Apply", getField(packet, "message"));
		assertEquals(1, getField(packet, "groupType"));
		assertEquals(4, getField(packet, "classId"));
		assertEquals(65, getField(packet, "level"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionNineReadsInstanceGroupDelete() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 9);
			buffer.putInt(700109);
			buffer.putInt(300110000);
		}));

		packet.readImpl();

		assertEquals(9, getField(packet, "action"));
		assertEquals(700109, getField(packet, "playerOrTeamId"));
		assertEquals(300110000, getField(packet, "instanceMaskId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionTwelveReadsInstanceApplicationReply() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 12);
			buffer.putInt(700112);
			buffer.put((byte) 1);
		}));

		packet.readImpl();

		assertEquals(12, getField(packet, "action"));
		assertEquals(700112, getField(packet, "playerOrTeamId"));
		assertEquals((byte) 1, getField(packet, "instanceApplicationReply"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionSeventeenReadsInstanceGroupUpdate() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 17);
			buffer.putInt(700117);
			buffer.putInt(300140000);
			putS(buffer, "Updated instance");
		}));

		packet.readImpl();

		assertEquals(17, getField(packet, "action"));
		assertEquals(700117, getField(packet, "playerOrTeamId"));
		assertEquals(300140000, getField(packet, "instanceMaskId"));
		assertEquals("Updated instance", getField(packet, "message"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionTwentyReadsOnlyAction() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> buffer.put((byte) 20)));

		packet.readImpl();

		assertEquals(20, getField(packet, "action"));
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_actionTwentyFiveReadsBanFromInstanceGroup() throws Exception {
		CM_FIND_GROUP packet = new CM_FIND_GROUP(77, Set.of(State.IN_GAME));
		packet.setBuffer(payload(buffer -> {
			buffer.put((byte) 25);
			buffer.putInt(700125);
			buffer.putInt(300150000);
			buffer.putInt(800125);
		}));

		packet.readImpl();

		assertEquals(25, getField(packet, "action"));
		assertEquals(700125, getField(packet, "playerOrTeamId"));
		assertEquals(300150000, getField(packet, "instanceMaskId"));
		assertEquals(800125, getField(packet, "bannedPlayerId"));
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer payload(BufferWriter writer) {
		ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
		writer.write(buffer);
		buffer.flip();
		return buffer;
	}

	private static void putS(ByteBuffer buffer, String value) {
		buffer.put(value.getBytes(StandardCharsets.UTF_16LE));
		buffer.putShort((short) 0);
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	@FunctionalInterface
	private interface BufferWriter {
		void write(ByteBuffer buffer);
	}
}
