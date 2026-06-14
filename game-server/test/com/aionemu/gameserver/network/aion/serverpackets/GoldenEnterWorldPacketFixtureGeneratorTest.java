package com.aionemu.gameserver.network.aion.serverpackets;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dataholders.AbsoluteStatsData;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * INTEGRATION golden harness for the deterministic EARLY ENTER-WORLD SM_* packets — the login send sequence in
 * {@link com.aionemu.gameserver.services.player.PlayerEnterWorldService}. These packets' writeImpls read only fixed
 * scalars, an empty player-owned collection (titles/friends/blocks/motions/emotions/quests), the active player's
 * objectId, or a constant; nothing live (World/Knownlist/Legion/wall-clock). The C# asserter
 * (GoldenEnterWorldPacketFixtureTests) rebuilds the structurally identical inputs and asserts identical bytes.
 * Java is the single source of truth.
 *
 * <p>Packets that read live World/Knownlist/Legion/Influence/time-beyond-stub are intentionally excluded
 * (SM_PRICES needs the Influence singleton; SM_ABYSS_RANK needs AbyssRankingCache; SM_GAME_TIME needs wall clock;
 * SM_INSTANCE_INFO needs INSTANCE_COOLTIME_DATA; SM_PLAYER_SPAWN/SM_STATS_INFO are covered elsewhere).</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenEnterWorldPacketFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenEnterWorldPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	@Test
	public void generateGoldenEnterWorldPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installSeam();

		// ---- SM_AFTER_TIME_CHECK_4_7_5: constant writeH(1) writeD(0) ----
		List<Case> afterTimeCheck = new ArrayList<>();
		afterTimeCheck.add(new Case("constant", "{}", capture(new SM_AFTER_TIME_CHECK_4_7_5(), null)));
		writeFixture(outDir.resolve("SM_AFTER_TIME_CHECK_4_7_5.json"), "SM_AFTER_TIME_CHECK_4_7_5", afterTimeCheck);

		// ---- SM_ENTER_WORLD_CHECK: writeC(msg) writeC(0) writeC(0) ----
		List<Case> enterWorldCheck = new ArrayList<>();
		enterWorldCheck.add(new Case("ok", "{\"msg\":\"OK\"}", capture(new SM_ENTER_WORLD_CHECK(), null)));
		enterWorldCheck.add(new Case("reentryTime", "{\"msg\":\"REENTRY_TIME\"}",
			capture(new SM_ENTER_WORLD_CHECK(SM_ENTER_WORLD_CHECK.Msg.REENTRY_TIME), null)));
		enterWorldCheck.add(new Case("connectionError", "{\"msg\":\"CONNECTION_ERROR\"}",
			capture(new SM_ENTER_WORLD_CHECK(SM_ENTER_WORLD_CHECK.Msg.CONNECTION_ERROR), null)));
		writeFixture(outDir.resolve("SM_ENTER_WORLD_CHECK.json"), "SM_ENTER_WORLD_CHECK", enterWorldCheck);

		// ---- SM_TITLE_INFO: action 0 (empty title list), 1 (self set), 3 (broadcast set), 6 (bonus title) ----
		List<Case> titleInfo = new ArrayList<>();
		Player titlePlayer = newPlayer(770001, Race.ELYOS);
		titleInfo.add(new Case("listEmpty", "{\"action\":0}", capture(new SM_TITLE_INFO(titlePlayer), null)));
		titleInfo.add(new Case("selfSet", "{\"action\":1,\"titleId\":42}", capture(new SM_TITLE_INFO(42), null)));
		titleInfo.add(new Case("broadcastSet", "{\"action\":3,\"playerObjId\":770001,\"titleId\":7}",
			capture(new SM_TITLE_INFO(titlePlayer, 7), null)));
		titleInfo.add(new Case("bonusTitle", "{\"action\":6,\"bonusTitleId\":13}",
			capture(new SM_TITLE_INFO(6, 13), null)));
		writeFixture(outDir.resolve("SM_TITLE_INFO.json"), "SM_TITLE_INFO", titleInfo);

		// ---- SM_EMOTION_LIST: writeC(action) writeH(size=0) ----
		List<Case> emotionList = new ArrayList<>();
		emotionList.add(new Case("emptyAction0", "{\"action\":0}",
			capture(new SM_EMOTION_LIST((byte) 0, Collections.emptyList()), null)));
		emotionList.add(new Case("emptyAction1", "{\"action\":1}",
			capture(new SM_EMOTION_LIST((byte) 1, Collections.emptyList()), null)));
		writeFixture(outDir.resolve("SM_EMOTION_LIST.json"), "SM_EMOTION_LIST", emotionList);

		// ---- SM_MOTION action 1 (list): writeC(1) writeH(size=0) ----
		List<Case> motion = new ArrayList<>();
		motion.add(new Case("emptyList", "{\"action\":1}",
			capture(new SM_MOTION(Collections.emptyList()), null)));
		writeFixture(outDir.resolve("SM_MOTION.json"), "SM_MOTION", motion);

		// ---- SM_QUEST_LIST: writeH(1) writeH(-size & 0xFFFF) ----
		List<Case> questList = new ArrayList<>();
		questList.add(new Case("empty", "{}", capture(new SM_QUEST_LIST(new ArrayList<>()), null)));
		writeFixture(outDir.resolve("SM_QUEST_LIST.json"), "SM_QUEST_LIST", questList);

		// ---- SM_CHANNEL_INFO(null position): unspawned -> currentChannel=1 instanceCount=1 ----
		List<Case> channelInfo = new ArrayList<>();
		channelInfo.add(new Case("nullPosition", "{\"position\":null}",
			capture(new SM_CHANNEL_INFO(null), null)));
		writeFixture(outDir.resolve("SM_CHANNEL_INFO.json"), "SM_CHANNEL_INFO", channelInfo);

		// ---- SM_UNK_3_5_1: reads con.getActivePlayer().getObjectId() + NetworkConfig.GAMESERVER_ID ----
		List<Case> unk351 = new ArrayList<>();
		{
			Player active = newPlayer(780002, Race.ELYOS);
			AionConnection con = newConnectionWithActivePlayer(active);
			unk351.add(new Case("default",
				"{\"objectId\":780002,\"gameserverId\":" + NetworkConfig.GAMESERVER_ID + "}",
				capture(new SM_UNK_3_5_1(), con)));
		}
		writeFixture(outDir.resolve("SM_UNK_3_5_1.json"), "SM_UNK_3_5_1", unk351);

		// ---- SM_FRIEND_LIST: empty friend list -> writeH(-0) writeC(0) ----
		List<Case> friendList = new ArrayList<>();
		{
			Player active = newPlayer(780003, Race.ELYOS);
			active.setFriendList(new FriendList(active, new ArrayList<>()));
			AionConnection con = newConnectionWithActivePlayer(active);
			friendList.add(new Case("empty", "{}", capture(new SM_FRIEND_LIST(), con)));
		}
		writeFixture(outDir.resolve("SM_FRIEND_LIST.json"), "SM_FRIEND_LIST", friendList);

		// ---- SM_BLOCK_LIST: empty block list -> writeH(-0) writeC(0) ----
		List<Case> blockList = new ArrayList<>();
		{
			Player active = newPlayer(780004, Race.ELYOS);
			active.setBlockList(new BlockList());
			AionConnection con = newConnectionWithActivePlayer(active);
			blockList.add(new Case("empty", "{}", capture(new SM_BLOCK_LIST(), con)));
		}
		writeFixture(outDir.resolve("SM_BLOCK_LIST.json"), "SM_BLOCK_LIST", blockList);
	}

	// ---- integration seam (same shape as the SM_PLAYER_INFO harness) ----

	private void installSeam() {
		com.aionemu.gameserver.configs.main.CustomConfig.BASE_FLYTIME = 60;
		if (DataManager.ABSOLUTE_STATS_DATA == null)
			DataManager.ABSOLUTE_STATS_DATA = new AbsoluteStatsData();
		setExperienceTable();
		com.aionemu.commons.configuration.ConfigurableProcessor.process(new java.util.Properties(),
			com.aionemu.gameserver.configs.network.NetworkConfig.class,
			com.aionemu.gameserver.configs.main.ThreadConfig.class,
			com.aionemu.gameserver.configs.network.PffConfig.class);
		installDbStub();
	}

	private static void setExperienceTable() {
		try {
			long[] table = new long[67];
			for (int i = 0; i < table.length; i++) {
				long n = i;
				table[i] = 100L * n * n * n + 1000L * n;
			}
			com.aionemu.gameserver.dataholders.PlayerExperienceTable pxt = new com.aionemu.gameserver.dataholders.PlayerExperienceTable();
			Field f = com.aionemu.gameserver.dataholders.PlayerExperienceTable.class.getDeclaredField("experience");
			f.setAccessible(true);
			f.set(pxt, table);
			DataManager.PLAYER_EXPERIENCE_TABLE = pxt;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install PLAYER_EXPERIENCE_TABLE fixture", e);
		}
	}

	private void installDbStub() {
		try {
			Class<?> dbFactory = Class.forName("com.aionemu.commons.database.DatabaseFactory");
			Field dataSourceField = dbFactory.getDeclaredField("dataSource");
			dataSourceField.setAccessible(true);
			if (dataSourceField.get(null) == null) {
				Object stub = java.lang.reflect.Proxy.newProxyInstance(
					getClass().getClassLoader(),
					new Class<?>[] { javax.sql.DataSource.class },
					(proxy, method, args) -> {
						if ("getConnection".equals(method.getName()))
							throw new java.sql.SQLException("harness stub: no database");
						if ("toString".equals(method.getName()))
							return "HarnessStubDataSource";
						if ("hashCode".equals(method.getName()))
							return System.identityHashCode(proxy);
						if ("equals".equals(method.getName()))
							return proxy == args[0];
						Class<?> rt = method.getReturnType();
						if (rt == boolean.class)
							return false;
						if (rt == int.class)
							return 0;
						return null;
					});
				dataSourceField.set(null, stub);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to install DB stub", e);
		}
	}

	// ---- minimal player (only objectId/race vary; faithful base ctor builds title/etc.) ----

	private static Player newPlayer(int objectId, Race race) {
		PlayerCommonData common = new PlayerCommonData(objectId);
		common.setPlayerClass(PlayerClass.WARRIOR);
		common.setRace(race);
		common.setGender(Gender.MALE);
		common.setName("Harness" + objectId);
		common.setNote("");
		PlayerAccountData accountData = new PlayerAccountData(common, new PlayerAppearance());
		return new Player(accountData, new Account(1));
	}

	/** Allocate an uninitialized AionConnection (no socket) and pin its activePlayer field to the given player. */
	private static AionConnection newConnectionWithActivePlayer(Player activePlayer) {
		try {
			Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
			AionConnection con = (AionConnection) allocate.invoke(unsafe, AionConnection.class);
			Field apField = AionConnection.class.getDeclaredField("activePlayer");
			apField.setAccessible(true);
			apField.set(con, new AtomicReference<>(activePlayer));
			return con;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to build harness AionConnection", e);
		}
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
