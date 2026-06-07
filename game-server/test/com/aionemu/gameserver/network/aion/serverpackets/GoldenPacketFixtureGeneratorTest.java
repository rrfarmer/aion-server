package com.aionemu.gameserver.network.aion.serverpackets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Phase A2 of the Port Fidelity & Remediation Plan: the Java golden-capture harness.
 *
 * Runs real Java server packets through {@code writeImpl} and writes the resulting
 * payload bytes to shared fixtures under {@code parity-artifacts/golden/packets/}.
 * The C# side (GoldenPacketFixtureTests) reads the SAME fixtures and asserts its
 * writers produce identical bytes. The Java-emitted bytes are the single source of
 * truth; this is "Java as the oracle", mechanically, without a live client.
 *
 * Regenerate fixtures with:
 *   mvn -q -pl game-server -am test -Dtest=GoldenPacketFixtureGenerator
 *
 * Only deterministic, constructor-driven packets belong here. Packets whose
 * writeImpl reads singletons/time (e.g. SM_VERSION_CHECK compatible path) need a
 * deterministic config harness before they can be captured; that is a later task.
 */
public class GoldenPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	@Test
	public void generateGoldenPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		List<Case> smGroupDataExchange = new ArrayList<>();
		smGroupDataExchange.add(new Case("nearbyBroadcast",
			"{\"ctor\":\"byteData\",\"byteData\":[1,2,255]}",
			capture(new SM_GROUP_DATA_EXCHANGE(new byte[] { 1, 2, (byte) 255 }))));
		smGroupDataExchange.add(new Case("groupBroadcast",
			"{\"ctor\":\"byteData_action_unk2\",\"byteData\":[10,11,12,13],\"action\":2,\"unk2\":7}",
			capture(new SM_GROUP_DATA_EXCHANGE(new byte[] { 10, 11, 12, 13 }, 2, 7))));
		writeFixture(outDir.resolve("SM_GROUP_DATA_EXCHANGE.json"), "SM_GROUP_DATA_EXCHANGE", 178, smGroupDataExchange);

		List<Case> smGfWebshop = new ArrayList<>();
		smGfWebshop.add(new Case("token",
			"{\"ctor\":\"token\",\"token\":\"ABC123\"}",
			capture(new SM_GF_WEBSHOP_TOKEN_RESPONSE("ABC123"))));
		writeFixture(outDir.resolve("SM_GF_WEBSHOP_TOKEN_RESPONSE.json"), "SM_GF_WEBSHOP_TOKEN_RESPONSE", null, smGfWebshop);
	}

	/** Capture the payload bytes a packet's writeImpl produces (no opcode, no crypt). */
	private static String capture(AionServerPacket packet) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);
			// writeImpl is protected on the base type; invoke reflectively so this generic
			// harness can serialize any packet without subclassing each one.
			Method writeImpl = AionServerPacket.class.getDeclaredMethod("writeImpl", AionConnection.class);
			writeImpl.setAccessible(true);
			writeImpl.invoke(packet, (AionConnection) null);
			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);
			return toHex(payload);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to capture " + packet.getClass().getSimpleName(), e);
		}
	}

	private static void writeFixture(Path file, String packet, Integer opcode, List<Case> cases) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"schemaVersion\": 1,\n");
		sb.append("  \"packet\": \"").append(packet).append("\",\n");
		sb.append("  \"opcode\": ").append(opcode == null ? "null" : opcode).append(",\n");
		sb.append("  \"source\": \"Java\",\n");
		sb.append("  \"cases\": [\n");
		for (int i = 0; i < cases.size(); i++) {
			Case c = cases.get(i);
			sb.append("    {\n");
			sb.append("      \"name\": \"").append(c.name).append("\",\n");
			sb.append("      \"inputs\": ").append(c.inputsJson).append(",\n");
			sb.append("      \"payloadHex\": \"").append(c.payloadHex).append("\"\n");
			sb.append("    }").append(i + 1 < cases.size() ? "," : "").append("\n");
		}
		sb.append("  ]\n");
		sb.append("}\n");
		Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static String toHex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			out[i * 2] = HEX[(bytes[i] >> 4) & 0xF];
			out[i * 2 + 1] = HEX[bytes[i] & 0xF];
		}
		return new String(out);
	}

	/** Walk up from the working dir to the repo root (the dir containing parity-artifacts/). */
	private static Path repoRoot() {
		Path dir = Paths.get("").toAbsolutePath();
		while (dir != null && !Files.isDirectory(dir.resolve("parity-artifacts"))) {
			dir = dir.getParent();
		}
		return dir != null ? dir : Paths.get("").toAbsolutePath();
	}

	private static final class Case {
		final String name;
		final String inputsJson;
		final String payloadHex;

		Case(String name, String inputsJson, String payloadHex) {
			this.name = name;
			this.inputsJson = inputsJson;
			this.payloadHex = payloadHex;
		}
	}
}
