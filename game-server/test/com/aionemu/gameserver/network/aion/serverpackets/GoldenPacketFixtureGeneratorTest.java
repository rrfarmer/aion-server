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

		// ----- Batch 3: faithful SM_* packets (AionServerPacket writeImpl reads only ctor args) -----

		List<Case> smUseObject = new ArrayList<>();
		smUseObject.add(new Case("use",
			"{\"playerObjId\":1001,\"targetObjId\":2002,\"time\":3000,\"actionType\":1}",
			capture(new SM_USE_OBJECT(1001, 2002, 3000, 1))));
		smUseObject.add(new Case("stop",
			"{\"playerObjId\":4004,\"targetObjId\":0,\"time\":0,\"actionType\":0}",
			capture(new SM_USE_OBJECT(4004, 0, 0, 0))));
		writeFixture(outDir.resolve("SM_USE_OBJECT.json"), "SM_USE_OBJECT", null, smUseObject);

		List<Case> smPong = new ArrayList<>();
		smPong.add(new Case("pong",
			"{}",
			capture(new SM_PONG())));
		writeFixture(outDir.resolve("SM_PONG.json"), "SM_PONG", null, smPong);

		List<Case> smPingResponse = new ArrayList<>();
		smPingResponse.add(new Case("ping",
			"{}",
			capture(new SM_PING_RESPONSE())));
		writeFixture(outDir.resolve("SM_PING_RESPONSE.json"), "SM_PING_RESPONSE", null, smPingResponse);

		List<Case> smPositionSelf = new ArrayList<>();
		smPositionSelf.add(new Case("position",
			"{\"x\":1234.5,\"y\":6789.0,\"z\":250.25,\"heading\":60}",
			capture(new SM_POSITION_SELF(1234.5f, 6789.0f, 250.25f, (byte) 60))));
		smPositionSelf.add(new Case("origin",
			"{\"x\":0.0,\"y\":0.0,\"z\":0.0,\"heading\":0}",
			capture(new SM_POSITION_SELF(0.0f, 0.0f, 0.0f, (byte) 0))));
		writeFixture(outDir.resolve("SM_POSITION_SELF.json"), "SM_POSITION_SELF", null, smPositionSelf);

		List<Case> smSummonUseSkill = new ArrayList<>();
		smSummonUseSkill.add(new Case("useSkill",
			"{\"summonId\":700001,\"skillId\":1601,\"skillLvl\":5,\"targetId\":800002}",
			capture(new SM_SUMMON_USESKILL(700001, 1601, 5, 800002))));
		writeFixture(outDir.resolve("SM_SUMMON_USESKILL.json"), "SM_SUMMON_USESKILL", null, smSummonUseSkill);

		List<Case> smIconInfo = new ArrayList<>();
		smIconInfo.add(new Case("display",
			"{\"buffId\":12345,\"display\":true}",
			capture(new SM_ICON_INFO(12345, true))));
		smIconInfo.add(new Case("hide",
			"{\"buffId\":0,\"display\":false}",
			capture(new SM_ICON_INFO(0, false))));
		writeFixture(outDir.resolve("SM_ICON_INFO.json"), "SM_ICON_INFO", null, smIconInfo);

		List<Case> smAscensionMorph = new ArrayList<>();
		smAscensionMorph.add(new Case("morph",
			"{\"inascension\":1}",
			capture(new SM_ASCENSION_MORPH(1))));
		smAscensionMorph.add(new Case("none",
			"{\"inascension\":0}",
			capture(new SM_ASCENSION_MORPH(0))));
		writeFixture(outDir.resolve("SM_ASCENSION_MORPH.json"), "SM_ASCENSION_MORPH", null, smAscensionMorph);

		List<Case> smQuestRepeat = new ArrayList<>();
		smQuestRepeat.add(new Case("multiple",
			"{\"repeatableQuests\":[15001,15002,15003]}",
			capture(new SM_QUEST_REPEAT(java.util.Arrays.asList(15001, 15002, 15003)))));
		smQuestRepeat.add(new Case("empty",
			"{\"repeatableQuests\":[]}",
			capture(new SM_QUEST_REPEAT(new java.util.ArrayList<>()))));
		writeFixture(outDir.resolve("SM_QUEST_REPEAT.json"), "SM_QUEST_REPEAT", null, smQuestRepeat);

		List<Case> smSecurityToken = new ArrayList<>();
		smSecurityToken.add(new Case("token",
			"{\"token\":[1,2,3,4,255]}",
			capture(new SM_SECURITY_TOKEN(new byte[] { 1, 2, 3, 4, (byte) 255 }))));
		writeFixture(outDir.resolve("SM_SECURITY_TOKEN.json"), "SM_SECURITY_TOKEN", null, smSecurityToken);

		List<Case> smMotion = new ArrayList<>();
		smMotion.add(new Case("add",
			"{\"ctor\":\"motionId_remainingTime\",\"motionId\":1001,\"remainingTime\":3600}",
			capture(new SM_MOTION((short) 1001, 3600))));
		smMotion.add(new Case("set",
			"{\"ctor\":\"motionId_type\",\"motionId\":1002,\"type\":3}",
			capture(new SM_MOTION((short) 1002, (byte) 3))));
		smMotion.add(new Case("remove",
			"{\"ctor\":\"motionId\",\"motionId\":1003}",
			capture(new SM_MOTION((short) 1003))));
		writeFixture(outDir.resolve("SM_MOTION_SCALAR.json"), "SM_MOTION", null, smMotion);

		List<Case> smMacroResult = new ArrayList<>();
		smMacroResult.add(new Case("created",
			"{\"code\":0}",
			capture(SM_MACRO_RESULT.SM_MACRO_CREATED)));
		smMacroResult.add(new Case("deleted",
			"{\"code\":1}",
			capture(SM_MACRO_RESULT.SM_MACRO_DELETED)));
		writeFixture(outDir.resolve("SM_MACRO_RESULT.json"), "SM_MACRO_RESULT", null, smMacroResult);

		// ----- Batch 4: faithful pure value-ctor SM_* packets (writeImpl reads only ctor-stored scalars/strings) -----

		List<Case> smDuel = new ArrayList<>();
		smDuel.add(new Case("started",
			"{\"type\":0,\"requesterObjId\":700123}",
			capture(SM_DUEL.SM_DUEL_STARTED(700123))));
		smDuel.add(new Case("resultWon",
			"{\"type\":1,\"result\":\"DUEL_WON\",\"playerName\":\"Vaizel\"}",
			capture(SM_DUEL.SM_DUEL_RESULT(com.aionemu.gameserver.model.DuelResult.DUEL_WON, "Vaizel"))));
		writeFixture(outDir.resolve("SM_DUEL.json"), "SM_DUEL", null, smDuel);

		List<Case> smSiegeLocationState = new ArrayList<>();
		smSiegeLocationState.add(new Case("vulnerable",
			"{\"locationId\":2011,\"state\":1}",
			capture(new SM_SIEGE_LOCATION_STATE(2011, 1))));
		smSiegeLocationState.add(new Case("invulnerable",
			"{\"locationId\":2021,\"state\":0}",
			capture(new SM_SIEGE_LOCATION_STATE(2021, 0))));
		writeFixture(outDir.resolve("SM_SIEGE_LOCATION_STATE.json"), "SM_SIEGE_LOCATION_STATE", null, smSiegeLocationState);

		List<Case> smMayLogin = new ArrayList<>();
		smMayLogin.add(new Case("ok",
			"{}",
			capture(new SM_MAY_LOGIN_INTO_GAME())));
		writeFixture(outDir.resolve("SM_MAY_LOGIN_INTO_GAME.json"), "SM_MAY_LOGIN_INTO_GAME", null, smMayLogin);

		List<Case> smQuestionWindow = new ArrayList<>();
		smQuestionWindow.add(new Case("withParam",
			"{\"code\":50028,\"senderId\":700500,\"rangeOrCooldownSeconds\":30,\"params\":[\"Vaizel\"]}",
			capture(new SM_QUESTION_WINDOW(50028, 700500, 30, "Vaizel"))));
		smQuestionWindow.add(new Case("noParam",
			"{\"code\":60000,\"senderId\":0,\"rangeOrCooldownSeconds\":0,\"params\":[]}",
			capture(new SM_QUESTION_WINDOW(60000, 0, 0))));
		writeFixture(outDir.resolve("SM_QUESTION_WINDOW.json"), "SM_QUESTION_WINDOW", null, smQuestionWindow);

		List<Case> smInstanceStageInfo = new ArrayList<>();
		smInstanceStageInfo.add(new Case("event",
			"{\"type\":2,\"event\":17,\"unk\":1}",
			capture(new SM_INSTANCE_STAGE_INFO(2, 17, 1))));
		smInstanceStageInfo.add(new Case("zero",
			"{\"type\":0,\"event\":0,\"unk\":0}",
			capture(new SM_INSTANCE_STAGE_INFO(0, 0, 0))));
		writeFixture(outDir.resolve("SM_INSTANCE_STAGE_INFO.json"), "SM_INSTANCE_STAGE_INFO", null, smInstanceStageInfo);

		List<Case> smFortressInfo = new ArrayList<>();
		smFortressInfo.add(new Case("teleportOn",
			"{\"locationId\":1011,\"teleportStatus\":true}",
			capture(new SM_FORTRESS_INFO(1011, true))));
		smFortressInfo.add(new Case("teleportOff",
			"{\"locationId\":1221,\"teleportStatus\":false}",
			capture(new SM_FORTRESS_INFO(1221, false))));
		writeFixture(outDir.resolve("SM_FORTRESS_INFO.json"), "SM_FORTRESS_INFO", null, smFortressInfo);

		List<Case> smLeaveGroupMember = new ArrayList<>();
		smLeaveGroupMember.add(new Case("leave",
			"{}",
			capture(new SM_LEAVE_GROUP_MEMBER())));
		writeFixture(outDir.resolve("SM_LEAVE_GROUP_MEMBER.json"), "SM_LEAVE_GROUP_MEMBER", null, smLeaveGroupMember);

		List<Case> smShieldEffect = new ArrayList<>();
		smShieldEffect.add(new Case("empty",
			"{\"locations\":[]}",
			capture(new SM_SHIELD_EFFECT(new java.util.ArrayList<com.aionemu.gameserver.model.siege.SiegeLocation>()))));
		writeFixture(outDir.resolve("SM_SHIELD_EFFECT.json"), "SM_SHIELD_EFFECT", null, smShieldEffect);

		// ---- SM_TOLL_INFO(long): writeQ(tollCount). Pure scalar. ----
		List<Case> smTollInfo = new ArrayList<>();
		smTollInfo.add(new Case("zero",
			"{\"tollCount\":0}",
			capture(new SM_TOLL_INFO(0L))));
		smTollInfo.add(new Case("typical",
			"{\"tollCount\":123456789}",
			capture(new SM_TOLL_INFO(123456789L))));
		smTollInfo.add(new Case("large",
			"{\"tollCount\":9223372036854775807}",
			capture(new SM_TOLL_INFO(9223372036854775807L))));
		writeFixture(outDir.resolve("SM_TOLL_INFO.json"), "SM_TOLL_INFO", null, smTollInfo);

		// ---- SM_INSTANCE_COUNT_INFO(int,int): writeD(mapId) writeD(instanceId) writeD(1). Pure scalar. ----
		List<Case> smInstanceCountInfo = new ArrayList<>();
		smInstanceCountInfo.add(new Case("default",
			"{\"mapId\":300100000,\"instanceId\":7}",
			capture(new SM_INSTANCE_COUNT_INFO(300100000, 7))));
		writeFixture(outDir.resolve("SM_INSTANCE_COUNT_INFO.json"), "SM_INSTANCE_COUNT_INFO", null, smInstanceCountInfo);

		// ---- SM_STATS_STATUS_UNK(int lvl,int points): lvl==50 conditional branch. Pure scalar. ----
		List<Case> smStatsStatusUnk = new ArrayList<>();
		smStatsStatusUnk.add(new Case("level50",
			"{\"lvl\":50,\"points\":12}",
			capture(new SM_STATS_STATUS_UNK(50, 12))));
		smStatsStatusUnk.add(new Case("other",
			"{\"lvl\":65,\"points\":3}",
			capture(new SM_STATS_STATUS_UNK(65, 3))));
		writeFixture(outDir.resolve("SM_STATS_STATUS_UNK.json"), "SM_STATS_STATUS_UNK", null, smStatsStatusUnk);

		// ---- SM_PACKAGE_INFO_NOTIFY(): constant payload. ----
		List<Case> smPackageInfoNotify = new ArrayList<>();
		smPackageInfoNotify.add(new Case("constant",
			"{}",
			capture(new SM_PACKAGE_INFO_NOTIFY())));
		writeFixture(outDir.resolve("SM_PACKAGE_INFO_NOTIFY.json"), "SM_PACKAGE_INFO_NOTIFY", null, smPackageInfoNotify);

		// ---- SM_ACTION_ANIMATION(int,ActionAnimation,int): writeD(target) writeH(animId) writeD(levelOrObjectId). ----
		List<Case> smActionAnimation = new ArrayList<>();
		smActionAnimation.add(new Case("levelUp",
			"{\"targetObjectId\":700001,\"actionAnimation\":\"LEVEL_UP\",\"levelOrObjectId\":50}",
			capture(new SM_ACTION_ANIMATION(700001,
				com.aionemu.gameserver.model.animations.ActionAnimation.LEVEL_UP, 50))));
		smActionAnimation.add(new Case("bindKisk",
			"{\"targetObjectId\":800002,\"actionAnimation\":\"BIND_KISK\",\"levelOrObjectId\":0}",
			capture(new SM_ACTION_ANIMATION(800002,
				com.aionemu.gameserver.model.animations.ActionAnimation.BIND_KISK))));
		smActionAnimation.add(new Case("craftLevelUp",
			"{\"targetObjectId\":900003,\"actionAnimation\":\"CRAFT_LEVEL_UP\",\"levelOrObjectId\":12345}",
			capture(new SM_ACTION_ANIMATION(900003,
				com.aionemu.gameserver.model.animations.ActionAnimation.CRAFT_LEVEL_UP, 12345))));
		writeFixture(outDir.resolve("SM_ACTION_ANIMATION.json"), "SM_ACTION_ANIMATION", null, smActionAnimation);

		// ----- Batch 6: faithful pure value-ctor SM_* packets (writeImpl reads only ctor-stored scalars/strings/enums) -----

		// SM_GAMEGUARD(int size): writeD(size) writeB(new byte[size]). Pure scalar.
		List<Case> smGameguard = new ArrayList<>();
		smGameguard.add(new Case("typical",
			"{\"size\":16}",
			capture(new SM_GAMEGUARD(16))));
		smGameguard.add(new Case("zero",
			"{\"size\":0}",
			capture(new SM_GAMEGUARD(0))));
		writeFixture(outDir.resolve("SM_GAMEGUARD.json"), "SM_GAMEGUARD", null, smGameguard);

		// SM_CAPTCHA: type 1 (count,data) and type 3 (isCorrect,banTime). Pure scalars/bytes.
		List<Case> smCaptcha = new ArrayList<>();
		smCaptcha.add(new Case("challenge",
			"{\"ctor\":\"count_data\",\"count\":2,\"data\":[10,20,255,0]}",
			capture(new SM_CAPTCHA(2, new byte[] { 10, 20, (byte) 255, 0 }))));
		smCaptcha.add(new Case("resultCorrect",
			"{\"ctor\":\"isCorrect_banTime\",\"isCorrect\":true,\"banTime\":3000}",
			capture(new SM_CAPTCHA(true, 3000))));
		smCaptcha.add(new Case("resultWrong",
			"{\"ctor\":\"isCorrect_banTime\",\"isCorrect\":false,\"banTime\":0}",
			capture(new SM_CAPTCHA(false, 0))));
		writeFixture(outDir.resolve("SM_CAPTCHA.json"), "SM_CAPTCHA", null, smCaptcha);

		// SM_MEGAPHONE(FactionLabel, senderName, message, itemId): writeS/writeS/writeD/writeC(faction.id).
		// faction.id is derived from Race.getRaceId() (immutable enum) on both sides.
		List<Case> smMegaphone = new ArrayList<>();
		smMegaphone.add(new Case("elyos",
			"{\"faction\":\"ELYOS\",\"senderName\":\"Nezekan\",\"message\":\"Hello Atreia\",\"itemId\":188052612}",
			capture(new SM_MEGAPHONE(SM_MEGAPHONE.FactionLabel.ELYOS, "Nezekan", "Hello Atreia", 188052612))));
		smMegaphone.add(new Case("asmodians",
			"{\"faction\":\"ASMODIANS\",\"senderName\":\"Siel\",\"message\":\"For Asmodae\",\"itemId\":0}",
			capture(new SM_MEGAPHONE(SM_MEGAPHONE.FactionLabel.ASMODIANS, "Siel", "For Asmodae", 0))));
		smMegaphone.add(new Case("none",
			"{\"faction\":\"NONE\",\"senderName\":\"System\",\"message\":\"Notice\",\"itemId\":1}",
			capture(new SM_MEGAPHONE(SM_MEGAPHONE.FactionLabel.NONE, "System", "Notice", 1))));
		writeFixture(outDir.resolve("SM_MEGAPHONE.json"), "SM_MEGAPHONE", null, smMegaphone);

		// SM_QUESTIONNAIRE(messageId, chunk, count, html): writeD/writeC/writeC/writeH(len*2)/writeS. Pure.
		List<Case> smQuestionnaire = new ArrayList<>();
		smQuestionnaire.add(new Case("survey",
			"{\"messageId\":1300000,\"chunk\":0,\"count\":1,\"html\":\"<html>Q</html>\"}",
			capture(new SM_QUESTIONNAIRE(1300000, (byte) 0, (byte) 1, "<html>Q</html>"))));
		smQuestionnaire.add(new Case("empty",
			"{\"messageId\":0,\"chunk\":0,\"count\":0,\"html\":\"\"}",
			capture(new SM_QUESTIONNAIRE(0, (byte) 0, (byte) 0, ""))));
		writeFixture(outDir.resolve("SM_QUESTIONNAIRE.json"), "SM_QUESTIONNAIRE", null, smQuestionnaire);

		// SM_FORCED_MOVE(creature, objectId, x, y, z): writeD(creature.getObjectId()) writeD(objectId) writeC(16) + xyz.
		// Only creature.getObjectId() is read from the harness creature; the rest are ctor scalars.
		List<Case> smForcedMove = new ArrayList<>();
		smForcedMove.add(new Case("moveBack",
			"{\"creatureObjectId\":700001,\"objectId\":800002,\"x\":1234.5,\"y\":6789.0,\"z\":250.25}",
			capture(new SM_FORCED_MOVE(harnessCreature(700001), 800002, 1234.5f, 6789.0f, 250.25f))));
		writeFixture(outDir.resolve("SM_FORCED_MOVE.json"), "SM_FORCED_MOVE", null, smForcedMove);

		// SM_WEATHER(WeatherEntry[]): writeC(0) writeC(len) + writeC(entry.getCode()) each. Pure (entry code only).
		List<Case> smWeather = new ArrayList<>();
		smWeather.add(new Case("multiple",
			"{\"codes\":[1,2,7]}",
			capture(new SM_WEATHER(new com.aionemu.gameserver.model.templates.world.WeatherEntry[] {
				new com.aionemu.gameserver.model.templates.world.WeatherEntry(0, 1),
				new com.aionemu.gameserver.model.templates.world.WeatherEntry(0, 2),
				new com.aionemu.gameserver.model.templates.world.WeatherEntry(0, 7) }))));
		smWeather.add(new Case("empty",
			"{\"codes\":[]}",
			capture(new SM_WEATHER(new com.aionemu.gameserver.model.templates.world.WeatherEntry[0]))));
		writeFixture(outDir.resolve("SM_WEATHER.json"), "SM_WEATHER", null, smWeather);

		// SM_GROUP_LOOT(groupId, playerId, itemId, itemCount, lootCorpseId, distributionId, luck, index): pure scalars.
		List<Case> smGroupLoot = new ArrayList<>();
		smGroupLoot.add(new Case("roll",
			"{\"groupId\":1001,\"playerId\":2002,\"itemId\":188000001,\"itemCount\":3,\"lootCorpseId\":3003,\"distributionId\":1,\"luck\":777,\"index\":5}",
			capture(new SM_GROUP_LOOT(1001, 2002, 188000001, 3, 3003, 1, 777L, 5))));
		writeFixture(outDir.resolve("SM_GROUP_LOOT.json"), "SM_GROUP_LOOT", null, smGroupLoot);

		// SM_WINDSTREAM_ANNOUNCE(bidirectional, mapId, streamId, state): writeD/writeD/writeD/writeC. Pure scalars.
		List<Case> smWindstreamAnnounce = new ArrayList<>();
		smWindstreamAnnounce.add(new Case("open",
			"{\"bidirectional\":1,\"mapId\":210050000,\"streamId\":3,\"state\":1}",
			capture(new SM_WINDSTREAM_ANNOUNCE(1, 210050000, 3, 1))));
		smWindstreamAnnounce.add(new Case("close",
			"{\"bidirectional\":0,\"mapId\":220070000,\"streamId\":0,\"state\":0}",
			capture(new SM_WINDSTREAM_ANNOUNCE(0, 220070000, 0, 0))));
		writeFixture(outDir.resolve("SM_WINDSTREAM_ANNOUNCE.json"), "SM_WINDSTREAM_ANNOUNCE", null, smWindstreamAnnounce);

		// SM_SKILL_REMOVE(PlayerSkillEntry): writeH(skillId) writeC(level/professionFlag) writeC(type).
		// The (skillId, skillLvl, skillType, persistentState) ctor stores everything directly — no DataManager.
		// writeImpl/getProfessionFlag read ONLY skillId/skillLevel/skillType (+ currentXp=0 default). Pure.
		// Covers: normal skill (level sent), normal stigma (type 1), linked stigma (type 3), tapping profession
		// (professionFlag=1), morph 40009 (professionFlag=1), crafting (professionFlag=currentXp default 0).
		List<Case> smSkillRemove = new ArrayList<>();
		java.util.function.BiFunction<int[], String, Case> mkSkillRemove = (in, name) -> new Case(name,
			"{\"skillId\":" + in[0] + ",\"skillLvl\":" + in[1] + ",\"skillType\":" + in[2] + "}",
			capture(new com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_REMOVE(
				new com.aionemu.gameserver.model.skill.PlayerSkillEntry(in[0], in[1], in[2],
					com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState.NOACTION))));
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 1001, 5, 0 }, "normalSkill"));          // <30000, type 0 -> level sent
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 31001, 12, 1 }, "normalStigma"));        // type 1
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 31002, 9, 3 }, "linkedStigma"));         // type 3
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 30001, 250, 0 }, "tappingProfession"));  // isTappingSkill -> flag 1
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 40009, 100, 0 }, "morphSkill"));         // isMorphSkill -> flag 1
		smSkillRemove.add(mkSkillRemove.apply(new int[] { 40001, 300, 0 }, "craftingSkill"));      // crafting -> flag currentXp=0
		writeFixture(outDir.resolve("SM_SKILL_REMOVE.json"), "SM_SKILL_REMOVE", null, smSkillRemove);
	}

	// Minimal deterministic Creature for packets that only read creature.getObjectId() in writeImpl.
	private static com.aionemu.gameserver.model.gameobjects.Creature harnessCreature(int objectId) {
		return new HarnessObjectIdCreature(objectId);
	}

	/** Minimal deterministic Creature: only objectId matters (set via the base ctor). */
	static final class HarnessObjectIdCreature extends com.aionemu.gameserver.model.gameobjects.Creature {
		HarnessObjectIdCreature(int objectId) {
			super(objectId, null, null, new com.aionemu.gameserver.model.templates.npc.NpcTemplate(), null, false);
		}

		@Override
		public byte getLevel() { return 1; }

		@Override
		public com.aionemu.gameserver.model.Race getRace() { return com.aionemu.gameserver.model.Race.NPC; }

		@Override
		public com.aionemu.gameserver.model.stats.container.CreatureGameStats<? extends com.aionemu.gameserver.model.gameobjects.Creature> getGameStats() { return null; }

		@Override
		public com.aionemu.gameserver.model.gameobjects.player.Player getActingCreature() { return null; }
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
