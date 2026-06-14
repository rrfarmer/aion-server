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

		List<Case> smQuitResponse = new ArrayList<>();
		smQuitResponse.add(new Case("normal",
			"{\"editMode\":false}",
			capture(new SM_QUIT_RESPONSE(false))));
		smQuitResponse.add(new Case("editMode",
			"{\"editMode\":true}",
			capture(new SM_QUIT_RESPONSE(true))));
		writeFixture(outDir.resolve("SM_QUIT_RESPONSE.json"), "SM_QUIT_RESPONSE", null, smQuitResponse);

		List<Case> smDeleteItem = new ArrayList<>();
		smDeleteItem.add(new Case("default",
			"{\"itemObjectId\":123456,\"deleteType\":0}",
			capture(new SM_DELETE_ITEM(123456, com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType.DEFAULT))));
		smDeleteItem.add(new Case("discard",
			"{\"itemObjectId\":999,\"deleteType\":21}",
			capture(new SM_DELETE_ITEM(999, com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType.DISCARD))));
		writeFixture(outDir.resolve("SM_DELETE_ITEM.json"), "SM_DELETE_ITEM", null, smDeleteItem);

		List<Case> smDeleteWarehouseItem = new ArrayList<>();
		smDeleteWarehouseItem.add(new Case("move",
			"{\"warehouseType\":1,\"itemObjectId\":777,\"deleteType\":20}",
			capture(new SM_DELETE_WAREHOUSE_ITEM(1, 777, com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType.MOVE))));
		writeFixture(outDir.resolve("SM_DELETE_WAREHOUSE_ITEM.json"), "SM_DELETE_WAREHOUSE_ITEM", null, smDeleteWarehouseItem);

		List<Case> smDeleteHouseObject = new ArrayList<>();
		smDeleteHouseObject.add(new Case("objectId",
			"{\"itemObjectId\":424242}",
			capture(new SM_DELETE_HOUSE_OBJECT(424242))));
		writeFixture(outDir.resolve("SM_DELETE_HOUSE_OBJECT.json"), "SM_DELETE_HOUSE_OBJECT", null, smDeleteHouseObject);

		List<Case> smDeleteHouse = new ArrayList<>();
		smDeleteHouse.add(new Case("address",
			"{\"addressId\":31001}",
			capture(new SM_DELETE_HOUSE(31001))));
		writeFixture(outDir.resolve("SM_DELETE_HOUSE.json"), "SM_DELETE_HOUSE", null, smDeleteHouse);

		List<Case> smRecipeDelete = new ArrayList<>();
		smRecipeDelete.add(new Case("recipeId",
			"{\"recipeId\":15001}",
			capture(new SM_RECIPE_DELETE(15001))));
		writeFixture(outDir.resolve("SM_RECIPE_DELETE.json"), "SM_RECIPE_DELETE", null, smRecipeDelete);

		List<Case> smCraftAnimation = new ArrayList<>();
		smCraftAnimation.add(new Case("animation",
			"{\"playerObjectId\":1001,\"targetObjectId\":2002,\"skillId\":40009,\"action\":3}",
			capture(new SM_CRAFT_ANIMATION(1001, 2002, 40009, 3))));
		writeFixture(outDir.resolve("SM_CRAFT_ANIMATION.json"), "SM_CRAFT_ANIMATION", null, smCraftAnimation);

		List<Case> smBlockResponse = new ArrayList<>();
		smBlockResponse.add(new Case("blockSuccessful",
			"{\"code\":0,\"playerName\":\"Nezekan\"}",
			capture(new SM_BLOCK_RESPONSE(0, "Nezekan"))));
		smBlockResponse.add(new Case("listFull",
			"{\"code\":3,\"playerName\":\"Siel\"}",
			capture(new SM_BLOCK_RESPONSE(3, "Siel"))));
		writeFixture(outDir.resolve("SM_BLOCK_RESPONSE.json"), "SM_BLOCK_RESPONSE", null, smBlockResponse);

		List<Case> smFriendResponse = new ArrayList<>();
		smFriendResponse.add(new Case("targetAdded",
			"{\"code\":0,\"playerName\":\"Israphel\"}",
			capture(new SM_FRIEND_RESPONSE("Israphel", 0x0))));
		smFriendResponse.add(new Case("targetOffline",
			"{\"code\":1,\"playerName\":\"\"}",
			capture(new SM_FRIEND_RESPONSE(0x1))));
		writeFixture(outDir.resolve("SM_FRIEND_RESPONSE.json"), "SM_FRIEND_RESPONSE", null, smFriendResponse);

		List<Case> smCloseQuestionWindow = new ArrayList<>();
		smCloseQuestionWindow.add(new Case("close",
			"{\"messageId\":0,\"params\":[]}",
			capture(SM_CLOSE_QUESTION_WINDOW.CLOSE_QUESTION_WINDOW())));
		smCloseQuestionWindow.add(new Case("duelWithdraw",
			"{\"messageId\":1300134,\"params\":[\"Vaizel\"]}",
			capture(SM_CLOSE_QUESTION_WINDOW.STR_DUEL_REQUESTER_WITHDRAW_REQUEST("Vaizel"))));
		writeFixture(outDir.resolve("SM_CLOSE_QUESTION_WINDOW.json"), "SM_CLOSE_QUESTION_WINDOW", null, smCloseQuestionWindow);
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
