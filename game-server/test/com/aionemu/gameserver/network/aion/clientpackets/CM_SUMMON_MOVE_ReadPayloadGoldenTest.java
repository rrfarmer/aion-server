package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.movement.MovementMask;
import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_SUMMON_MOVE_ReadPayloadGoldenTest {

	@Test
	public void readImpl_absoluteGlideVehiclePayloadConsumesJavaFieldOrder() throws Exception {
		CM_SUMMON_MOVE packet = new CM_SUMMON_MOVE(201, Set.of(State.IN_GAME));
		byte movementType = (byte) (MovementMask.POSITION | MovementMask.MANUAL | MovementMask.ABSOLUTE | MovementMask.GLIDE | MovementMask.VEHICLE);
		packet.setBuffer(absoluteGlideVehiclePayload(movementType));

		packet.readImpl();

		assertEquals(8101, getInt(packet, "objectId"));
		assertEquals(10.5f, getFloat(packet, "x"), 0.0001f);
		assertEquals(11.5f, getFloat(packet, "y"), 0.0001f);
		assertEquals(12.5f, getFloat(packet, "z"), 0.0001f);
		assertEquals((byte) 91, getByte(packet, "heading"));
		assertEquals(movementType, getByte(packet, "type"));
		assertEquals(20.5f, getFloat(packet, "x2"), 0.0001f);
		assertEquals(21.5f, getFloat(packet, "y2"), 0.0001f);
		assertEquals(22.5f, getFloat(packet, "z2"), 0.0001f);
		assertEquals((byte) 2, getByte(packet, "glideFlag"));
		assertEquals(301, getInt(packet, "unk1"));
		assertEquals(302, getInt(packet, "unk2"));
		assertEquals(30.5f, getFloat(packet, "vehicleX"), 0.0001f);
		assertEquals(31.5f, getFloat(packet, "vehicleY"), 0.0001f);
		assertEquals(32.5f, getFloat(packet, "vehicleZ"), 0.0001f);
		assertEquals(0, packet.getRemainingBytes());
	}

	@Test
	public void readImpl_manualPositionWithoutAbsoluteDoesNotReadVectorCoordinates() throws Exception {
		CM_SUMMON_MOVE packet = new CM_SUMMON_MOVE(201, Set.of(State.IN_GAME));
		byte movementType = (byte) (MovementMask.POSITION | MovementMask.MANUAL);
		packet.setBuffer(manualPositionPayload(movementType));

		packet.readImpl();

		assertEquals(8102, getInt(packet, "objectId"));
		assertEquals(40.5f, getFloat(packet, "x"), 0.0001f);
		assertEquals(41.5f, getFloat(packet, "y"), 0.0001f);
		assertEquals(42.5f, getFloat(packet, "z"), 0.0001f);
		assertEquals((byte) 13, getByte(packet, "heading"));
		assertEquals(movementType, getByte(packet, "type"));
		assertEquals(0f, getFloat(packet, "x2"), 0.0001f);
		assertEquals(0f, getFloat(packet, "y2"), 0.0001f);
		assertEquals(0f, getFloat(packet, "z2"), 0.0001f);
		assertEquals(0, packet.getRemainingBytes());
	}

	private static ByteBuffer absoluteGlideVehiclePayload(byte movementType) {
		ByteBuffer buffer = ByteBuffer.allocate(51).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(8101);
		buffer.putFloat(10.5f);
		buffer.putFloat(11.5f);
		buffer.putFloat(12.5f);
		buffer.put((byte) 91);
		buffer.put(movementType);
		buffer.putFloat(20.5f);
		buffer.putFloat(21.5f);
		buffer.putFloat(22.5f);
		buffer.put((byte) 2);
		buffer.putInt(301);
		buffer.putInt(302);
		buffer.putFloat(30.5f);
		buffer.putFloat(31.5f);
		buffer.putFloat(32.5f);
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer manualPositionPayload(byte movementType) {
		ByteBuffer buffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(8102);
		buffer.putFloat(40.5f);
		buffer.putFloat(41.5f);
		buffer.putFloat(42.5f);
		buffer.put((byte) 13);
		buffer.put(movementType);
		buffer.flip();
		return buffer;
	}

	private static Object get(CM_SUMMON_MOVE packet, String fieldName) throws Exception {
		Field field = CM_SUMMON_MOVE.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(packet);
	}

	private static int getInt(CM_SUMMON_MOVE packet, String fieldName) throws Exception {
		return (int) get(packet, fieldName);
	}

	private static float getFloat(CM_SUMMON_MOVE packet, String fieldName) throws Exception {
		return (float) get(packet, fieldName);
	}

	private static byte getByte(CM_SUMMON_MOVE packet, String fieldName) throws Exception {
		return (byte) get(packet, fieldName);
	}
}
