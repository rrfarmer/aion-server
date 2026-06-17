package com.aionemu.gameserver.network.aion.serverpackets;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.animations.TeleportAnimation;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * INTEGRATION golden harness for the World-reading SM_* family — increment 1 of the deferred integration-harness
 * sub-project (docs/next-slop-targets.md). This is the FIRST golden seam that drives a packet through the
 * DataManager.WORLD_MAPS_DATA world-map holder rather than only scalar/ctor state. It serializes
 * SM_TELEPORT_LOC.writeImpl byte-for-byte; the C# asserter (GoldenWorldPacketFixtureTests) rebuilds the
 * structurally identical WorldMapsData holder + packet and asserts identical bytes. Java is the single source of truth.
 *
 * <p>Seam: DataManager.WORLD_MAPS_DATA is reflectively populated with exactly the two map templates SM_TELEPORT_LOC
 * reads (one non-instance, one instance). SM_TELEPORT_LOC.writeImpl is pure scalar; its ONLY non-ctor read is the
 * ctor's DataManager.WORLD_MAPS_DATA.getTemplate(mapId).isInstance() branch, which selects between writing the
 * instanceId (instance map) or the mapId (regular map) for the channel field. No live World/WorldMapInstance/
 * ZoneService/Player is needed — this is the bounded WORLD_MAPS_DATA holder seam, deliberately NOT the live
 * World.getInstance() graph (that requires the heavier integration harness; see the report for the blocker).</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenWorldPacketFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenWorldPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	// The two map ids the SM_TELEPORT_LOC fixture exercises (identical on both sides). One regular (non-instance)
	// world and one instance world, so the ctor's WORLD_MAPS_DATA.getTemplate(mapId).isInstance() branch is covered
	// both ways. Morheim (220020000) is a regular field map; Draupnir Cave (320080000) is an instance.
	private static final int REGULAR_MAP_ID = 220020000;
	private static final int INSTANCE_MAP_ID = 320080000;

	@Test
	public void generateGoldenWorldPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installWorldMapsSeam();

		List<Case> cases = new ArrayList<>();
		// Regular (non-instance) map -> writeImpl writes mapId for the channel field.
		cases.add(teleportCase("teleportRegularMap", REGULAR_MAP_ID, 5, 1500.5f, 2500.25f, 412.125f, (byte) 30,
			TeleportAnimation.FADE_OUT));
		// Instance map -> writeImpl writes instanceId for the channel field.
		cases.add(teleportCase("teleportInstanceMap", INSTANCE_MAP_ID, 7, 100.5f, 200.25f, 300.75f, (byte) 90,
			TeleportAnimation.JUMP_IN_GATE));
		// Regular map with the NONE animation + instanceId 0 (the common bind/obelisk teleport shape).
		cases.add(teleportCase("teleportRegularNoneAnim", REGULAR_MAP_ID, 0, 0.0f, 0.0f, 0.0f, (byte) 0,
			TeleportAnimation.NONE));

		writeFixture(outDir.resolve("SM_TELEPORT_LOC.json"), "SM_TELEPORT_LOC", cases);
	}

	private static Case teleportCase(String name, int mapId, int instanceId, float x, float y, float z, byte heading,
			TeleportAnimation portAnimation) {
		String inputs = "{\"mapId\":" + mapId + ",\"instanceId\":" + instanceId + ",\"x\":" + x + ",\"y\":" + y
			+ ",\"z\":" + z + ",\"heading\":" + heading + ",\"portAnimation\":" + portAnimation.getId() + "}";
		return new Case(name, inputs,
			capture(new SM_TELEPORT_LOC(mapId, instanceId, x, y, z, heading, portAnimation), null));
	}

	// ---- World/instance DataManager seam ----

	/**
	 * Populate DataManager.WORLD_MAPS_DATA with exactly the two map templates SM_TELEPORT_LOC reads (one non-instance,
	 * one instance). Built WITHOUT JAXB by reflectively setting the mapsById index, so no XML/file/ZoneService is
	 * touched — the bounded holder seam.
	 */
	private void installWorldMapsSeam() {
		try {
			WorldMapsData data = new WorldMapsData();
			@SuppressWarnings("unchecked")
			Map<Integer, WorldMapTemplate> mapsById = (Map<Integer, WorldMapTemplate>) getField(data, "mapsById");
			mapsById.clear();
			mapsById.put(REGULAR_MAP_ID, worldMapTemplate(REGULAR_MAP_ID, "MORHEIM", false));
			mapsById.put(INSTANCE_MAP_ID, worldMapTemplate(INSTANCE_MAP_ID, "DRAUPNIR_CAVE", true));
			DataManager.WORLD_MAPS_DATA = data;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install WORLD_MAPS_DATA seam", e);
		}
	}

	private static WorldMapTemplate worldMapTemplate(int mapId, String name, boolean instance)
			throws ReflectiveOperationException {
		Constructor<WorldMapTemplate> ctor = WorldMapTemplate.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		WorldMapTemplate t = ctor.newInstance();
		setField(t, "mapId", mapId);
		setField(t, "name", name);
		setField(t, "instance", instance);
		return t;
	}

	private static Object getField(Object target, String name) throws ReflectiveOperationException {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	/** Capture the payload bytes a packet's writeImpl produces (no opcode, no crypt) for the given connection. */
	private static String capture(AionServerPacket packet, AionConnection con) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);
			Method writeImpl = AionServerPacket.class.getDeclaredMethod("writeImpl", AionConnection.class);
			writeImpl.setAccessible(true);
			writeImpl.invoke(packet, con);
			byte[] payload = new byte[buffer.position()];
			buffer.flip();
			buffer.get(payload);
			return toHex(payload);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to capture " + packet.getClass().getSimpleName(), e);
		}
	}

	// ---- fixture writing ----

	private static void writeFixture(Path file, String packet, List<Case> cases) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"schemaVersion\": 1,\n");
		sb.append("  \"packet\": \"").append(packet).append("\",\n");
		sb.append("  \"opcode\": null,\n");
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
