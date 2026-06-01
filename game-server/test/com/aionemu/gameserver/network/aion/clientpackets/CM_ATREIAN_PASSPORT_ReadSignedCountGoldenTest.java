package com.aionemu.gameserver.network.aion.clientpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection.State;

public class CM_ATREIAN_PASSPORT_ReadSignedCountGoldenTest {

	@Test
	public void readImpl_sentinelCountConsumesCompletePassportPairsUntilTrailingBytes() throws Exception {
		CM_ATREIAN_PASSPORT packet = new CM_ATREIAN_PASSPORT(248, Set.of(State.IN_GAME));
		packet.setBuffer(payload(0xFFFF, new int[] { 1001, 1001 }, new int[] { 1717200000, 1717286400 }, 9999));

		packet.readImpl();

		Map<Integer, Set<Integer>> passports = getPassports(packet);
		assertEquals(1, passports.size());
		assertEquals(Set.of(1717200000, 1717286400), passports.get(1001));
	}

	@Test
	public void readImpl_positiveCountConsumesOnlyDeclaredPassportPairs() throws Exception {
		CM_ATREIAN_PASSPORT packet = new CM_ATREIAN_PASSPORT(248, Set.of(State.IN_GAME));
		packet.setBuffer(payload(1, new int[] { 1001, 2002 }, new int[] { 1717200000, 1717286400 }));

		packet.readImpl();

		Map<Integer, Set<Integer>> passports = getPassports(packet);
		assertEquals(1, passports.size());
		assertEquals(Set.of(1717200000), passports.get(1001));
	}

	private static ByteBuffer payload(int count, int[] passportIds, int[] timestamps, int... trailingDwords) {
		ByteBuffer buffer = ByteBuffer.allocate(2 + passportIds.length * 8 + trailingDwords.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short) count);
		for (int i = 0; i < passportIds.length; i++) {
			buffer.putInt(passportIds[i]);
			buffer.putInt(timestamps[i]);
		}
		for (int trailingDword : trailingDwords)
			buffer.putInt(trailingDword);
		buffer.flip();
		return buffer;
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Set<Integer>> getPassports(CM_ATREIAN_PASSPORT packet) throws Exception {
		Field field = packet.getClass().getDeclaredField("passports");
		field.setAccessible(true);
		return (Map<Integer, Set<Integer>>) field.get(packet);
	}
}
