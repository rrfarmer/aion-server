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

		List<Case> smDeleteCharacter = new ArrayList<>();
		smDeleteCharacter.add(new Case("present",
			"{\"playerObjId\":654321,\"deletionTime\":86400}",
			capture(new SM_DELETE_CHARACTER(654321, 86400))));
		smDeleteCharacter.add(new Case("zero",
			"{\"playerObjId\":0,\"deletionTime\":0}",
			capture(new SM_DELETE_CHARACTER(0, 0))));
		writeFixture(outDir.resolve("SM_DELETE_CHARACTER.json"), "SM_DELETE_CHARACTER", null, smDeleteCharacter);

		List<Case> smRestoreCharacter = new ArrayList<>();
		smRestoreCharacter.add(new Case("success",
			"{\"chaOid\":11223,\"success\":true}",
			capture(new SM_RESTORE_CHARACTER(11223, true))));
		smRestoreCharacter.add(new Case("failure",
			"{\"chaOid\":44556,\"success\":false}",
			capture(new SM_RESTORE_CHARACTER(44556, false))));
		writeFixture(outDir.resolve("SM_RESTORE_CHARACTER.json"), "SM_RESTORE_CHARACTER", null, smRestoreCharacter);

		List<Case> smNicknameCheckResponse = new ArrayList<>();
		smNicknameCheckResponse.add(new Case("ok",
			"{\"value\":0}",
			capture(new SM_NICKNAME_CHECK_RESPONSE(0))));
		smNicknameCheckResponse.add(new Case("notOk",
			"{\"value\":10}",
			capture(new SM_NICKNAME_CHECK_RESPONSE(10))));
		writeFixture(outDir.resolve("SM_NICKNAME_CHECK_RESPONSE.json"), "SM_NICKNAME_CHECK_RESPONSE", null, smNicknameCheckResponse);

		List<Case> smLearnRecipe = new ArrayList<>();
		smLearnRecipe.add(new Case("recipe",
			"{\"recipeId\":15042}",
			capture(new SM_LEARN_RECIPE(15042))));
		writeFixture(outDir.resolve("SM_LEARN_RECIPE.json"), "SM_LEARN_RECIPE", null, smLearnRecipe);

		List<Case> smSummonOwnerRemove = new ArrayList<>();
		smSummonOwnerRemove.add(new Case("summon",
			"{\"summonObjId\":700001}",
			capture(new SM_SUMMON_OWNER_REMOVE(700001))));
		writeFixture(outDir.resolve("SM_SUMMON_OWNER_REMOVE.json"), "SM_SUMMON_OWNER_REMOVE", null, smSummonOwnerRemove);

		List<Case> smSummonPanelRemove = new ArrayList<>();
		smSummonPanelRemove.add(new Case("withSkill",
			"{\"skillId\":1601}",
			capture(new SM_SUMMON_PANEL_REMOVE(1601))));
		smSummonPanelRemove.add(new Case("zeroSkill",
			"{\"skillId\":0}",
			capture(new SM_SUMMON_PANEL_REMOVE(0))));
		writeFixture(outDir.resolve("SM_SUMMON_PANEL_REMOVE.json"), "SM_SUMMON_PANEL_REMOVE", null, smSummonPanelRemove);

		List<Case> smDpInfo = new ArrayList<>();
		smDpInfo.add(new Case("dp",
			"{\"playerObjectId\":800042,\"currentDp\":4000}",
			capture(new SM_DP_INFO(800042, 4000))));
		writeFixture(outDir.resolve("SM_DP_INFO.json"), "SM_DP_INFO", null, smDpInfo);

		List<Case> smFlyTime = new ArrayList<>();
		smFlyTime.add(new Case("flyTime",
			"{\"currentFp\":3500,\"maxFp\":7000}",
			capture(new SM_FLY_TIME(3500, 7000))));
		writeFixture(outDir.resolve("SM_FLY_TIME.json"), "SM_FLY_TIME", null, smFlyTime);

		List<Case> smStatUpdateDp = new ArrayList<>();
		smStatUpdateDp.add(new Case("dp",
			"{\"currentDp\":2500}",
			capture(new SM_STATUPDATE_DP(2500))));
		writeFixture(outDir.resolve("SM_STATUPDATE_DP.json"), "SM_STATUPDATE_DP", null, smStatUpdateDp);

		List<Case> smStatUpdateHp = new ArrayList<>();
		smStatUpdateHp.add(new Case("hp",
			"{\"currentHp\":1234,\"maxHp\":5678}",
			capture(new SM_STATUPDATE_HP(1234, 5678))));
		writeFixture(outDir.resolve("SM_STATUPDATE_HP.json"), "SM_STATUPDATE_HP", null, smStatUpdateHp);

		List<Case> smStatUpdateMp = new ArrayList<>();
		smStatUpdateMp.add(new Case("mp",
			"{\"currentMp\":345,\"maxMp\":900}",
			capture(new SM_STATUPDATE_MP(345, 900))));
		writeFixture(outDir.resolve("SM_STATUPDATE_MP.json"), "SM_STATUPDATE_MP", null, smStatUpdateMp);

		List<Case> smStatUpdateExp = new ArrayList<>();
		smStatUpdateExp.add(new Case("exp",
			"{\"currentExp\":123456789,\"recoverableExp\":1000,\"maxExp\":500000000,\"rep1\":250,\"rep2\":2000}",
			capture(new SM_STATUPDATE_EXP(123456789L, 1000L, 500000000L, 250L, 2000L))));
		writeFixture(outDir.resolve("SM_STATUPDATE_EXP.json"), "SM_STATUPDATE_EXP", null, smStatUpdateExp);

		List<Case> smUnwrapItem = new ArrayList<>();
		smUnwrapItem.add(new Case("unwrap",
			"{\"objectId\":900111,\"count\":5}",
			capture(new SM_UNWRAP_ITEM(900111, 5))));
		writeFixture(outDir.resolve("SM_UNWRAP_ITEM.json"), "SM_UNWRAP_ITEM", null, smUnwrapItem);

		List<Case> smWindstream = new ArrayList<>();
		smWindstream.add(new Case("windstream",
			"{\"unk1\":12,\"unk2\":3}",
			capture(new SM_WINDSTREAM(12, 3))));
		writeFixture(outDir.resolve("SM_WINDSTREAM.json"), "SM_WINDSTREAM", null, smWindstream);

		List<Case> smFriendNotify = new ArrayList<>();
		smFriendNotify.add(new Case("login",
			"{\"code\":0,\"name\":\"Nezekan\"}",
			capture(new SM_FRIEND_NOTIFY((byte) 0, "Nezekan"))));
		smFriendNotify.add(new Case("logout",
			"{\"code\":1,\"name\":\"Siel\"}",
			capture(new SM_FRIEND_NOTIFY((byte) 1, "Siel"))));
		writeFixture(outDir.resolve("SM_FRIEND_NOTIFY.json"), "SM_FRIEND_NOTIFY", null, smFriendNotify);

		List<Case> smBindPointTeleport = new ArrayList<>();
		smBindPointTeleport.add(new Case("action0",
			"{\"action\":0,\"playerId\":100,\"locId\":0,\"cooldown\":0}",
			capture(new SM_BIND_POINT_TELEPORT(0, 100, 0, 0))));
		smBindPointTeleport.add(new Case("action1",
			"{\"action\":1,\"playerId\":101,\"locId\":555,\"cooldown\":0}",
			capture(new SM_BIND_POINT_TELEPORT(1, 101, 555, 0))));
		smBindPointTeleport.add(new Case("action3",
			"{\"action\":3,\"playerId\":102,\"locId\":556,\"cooldown\":60}",
			capture(new SM_BIND_POINT_TELEPORT(3, 102, 556, 60))));
		writeFixture(outDir.resolve("SM_BIND_POINT_TELEPORT.json"), "SM_BIND_POINT_TELEPORT", null, smBindPointTeleport);

		// ----- New batch: faithful SM_* packets (AionServerPacket writeImpl reads only ctor args) -----

		List<Case> smReconnectKey = new ArrayList<>();
		smReconnectKey.add(new Case("key",
			"{\"key\":1234567}",
			capture(new SM_RECONNECT_KEY(1234567))));
		smReconnectKey.add(new Case("zero",
			"{\"key\":0}",
			capture(new SM_RECONNECT_KEY(0))));
		writeFixture(outDir.resolve("SM_RECONNECT_KEY.json"), "SM_RECONNECT_KEY", null, smReconnectKey);

		List<Case> smGatherAnimation = new ArrayList<>();
		smGatherAnimation.add(new Case("gather",
			"{\"playerObjId\":5001,\"gatherableObjId\":6002,\"skillId\":30001,\"action\":1}",
			capture(new SM_GATHER_ANIMATION(5001, 6002, 30001, 1))));
		writeFixture(outDir.resolve("SM_GATHER_ANIMATION.json"), "SM_GATHER_ANIMATION", null, smGatherAnimation);

		List<Case> smShowBrand = new ArrayList<>();
		smShowBrand.add(new Case("single",
			"{\"iconId\":3,\"targetObjectId\":700123}",
			capture(new SM_SHOW_BRAND(3, 700123))));
		smShowBrand.add(new Case("remove",
			"{\"iconId\":0,\"targetObjectId\":0}",
			capture(new SM_SHOW_BRAND(0, 0))));
		writeFixture(outDir.resolve("SM_SHOW_BRAND.json"), "SM_SHOW_BRAND", null, smShowBrand);

		List<Case> smCubeUpdate = new ArrayList<>();
		smCubeUpdate.add(new Case("stigmaSlots",
			"{\"action\":6,\"actionValue\":4}",
			capture(SM_CUBE_UPDATE.stigmaSlots(4))));
		smCubeUpdate.add(new Case("stigmaSlotsZero",
			"{\"action\":6,\"actionValue\":0}",
			capture(SM_CUBE_UPDATE.stigmaSlots(0))));
		writeFixture(outDir.resolve("SM_CUBE_UPDATE.json"), "SM_CUBE_UPDATE", null, smCubeUpdate);

		List<Case> smTeleportMap = new ArrayList<>();
		smTeleportMap.add(new Case("teleporter",
			"{\"targetObjId\":800200,\"teleportId\":4012}",
			capture(new SM_TELEPORT_MAP(800200, 4012))));
		writeFixture(outDir.resolve("SM_TELEPORT_MAP.json"), "SM_TELEPORT_MAP", null, smTeleportMap);

		List<Case> smLootStatus = new ArrayList<>();
		smLootStatus.add(new Case("disable",
			"{\"targetObjectId\":900300,\"status\":1}",
			capture(new SM_LOOT_STATUS(900300, SM_LOOT_STATUS.Status.LOOT_DISABLE))));
		smLootStatus.add(new Case("openDropList",
			"{\"targetObjectId\":900301,\"status\":2}",
			capture(new SM_LOOT_STATUS(900301, SM_LOOT_STATUS.Status.OPEN_DROP_LIST))));
		smLootStatus.add(new Case("closeDropList",
			"{\"targetObjectId\":900302,\"status\":3}",
			capture(new SM_LOOT_STATUS(900302, SM_LOOT_STATUS.Status.CLOSE_DROP_LIST))));
		writeFixture(outDir.resolve("SM_LOOT_STATUS.json"), "SM_LOOT_STATUS", null, smLootStatus);

		List<Case> smTargetSelected = new ArrayList<>();
		smTargetSelected.add(new Case("noTarget",
			"{\"target\":null}",
			capture(new SM_TARGET_SELECTED(null))));
		writeFixture(outDir.resolve("SM_TARGET_SELECTED.json"), "SM_TARGET_SELECTED", null, smTargetSelected);

		List<Case> smRiftAnnounce = new ArrayList<>();
		smRiftAnnounce.add(new Case("silentera",
			"{\"actionId\":1,\"gelkmaros\":true,\"inggison\":false}",
			capture(new SM_RIFT_ANNOUNCE(true, false))));
		smRiftAnnounce.add(new Case("despawn",
			"{\"actionId\":4,\"objectId\":750400}",
			capture(new SM_RIFT_ANNOUNCE(750400))));
		writeFixture(outDir.resolve("SM_RIFT_ANNOUNCE.json"), "SM_RIFT_ANNOUNCE", null, smRiftAnnounce);

		List<Case> smRecipeList = new ArrayList<>();
		smRecipeList.add(new Case("single",
			"{\"recipeIds\":[15001]}",
			capture(new SM_RECIPE_LIST(new java.util.LinkedHashSet<>(java.util.Arrays.asList(15001))))));
		smRecipeList.add(new Case("empty",
			"{\"recipeIds\":[]}",
			capture(new SM_RECIPE_LIST(new java.util.LinkedHashSet<>()))));
		writeFixture(outDir.resolve("SM_RECIPE_LIST.json"), "SM_RECIPE_LIST", null, smRecipeList);
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
