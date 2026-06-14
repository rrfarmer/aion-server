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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PassportsList;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Golden harness for the SOCIAL / GROUP / LEGION / TRADE / HOUSE packet domain — packets whose {@code writeImpl}
 * bytes derive entirely from constructor scalars/strings, an empty player-owned collection, or the active
 * player's deterministic state. The C# asserter (GoldenSocialPacketFixtureTests) rebuilds the structurally
 * identical inputs and asserts byte-for-byte identical payloads. Java is the single source of truth.
 *
 * <p>Packets that read live World/Knownlist/AbyssRankingCache/wall-clock/ItemInfoBlob+Item are intentionally
 * excluded (SM_LEGION_INFO needs AbyssRankingCache, SM_TRADELIST needs GOODSLIST_DATA+LimitedItemTradeService,
 * SM_EXCHANGE_ADD_ITEM/SM_PRIVATE_STORE need ItemInfoBlob+live Item, SM_MAIL_SERVICE needs a live Mailbox).</p>
 *
 * Regenerate with:
 *   mvn -pl game-server -am test -Dtest=GoldenSocialPacketFixtureGeneratorTest -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false
 */
public class GoldenSocialPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	@Test
	public void generateGoldenSocialPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		installSeam();

		// ---- SM_EXCHANGE_ADD_KINAH: writeC(action) writeQ(kinahCount) ----
		List<Case> exchangeAddKinah = new ArrayList<>();
		exchangeAddKinah.add(new Case("self",
			"{\"action\":0,\"kinahCount\":123456789}",
			capture(new SM_EXCHANGE_ADD_KINAH(123456789L, 0), null)));
		exchangeAddKinah.add(new Case("other",
			"{\"action\":1,\"kinahCount\":9999999999}",
			capture(new SM_EXCHANGE_ADD_KINAH(9999999999L, 1), null)));
		writeFixture(outDir.resolve("SM_EXCHANGE_ADD_KINAH.json"), "SM_EXCHANGE_ADD_KINAH", exchangeAddKinah);

		// ---- SM_EXCHANGE_CONFIRMATION: writeC(action) ----
		List<Case> exchangeConfirmation = new ArrayList<>();
		exchangeConfirmation.add(new Case("confirm",
			"{\"action\":0}", capture(new SM_EXCHANGE_CONFIRMATION(0), null)));
		exchangeConfirmation.add(new Case("cancel",
			"{\"action\":1}", capture(new SM_EXCHANGE_CONFIRMATION(1), null)));
		writeFixture(outDir.resolve("SM_EXCHANGE_CONFIRMATION.json"), "SM_EXCHANGE_CONFIRMATION", exchangeConfirmation);

		// ---- SM_LEGION_LEAVE_MEMBER: writeD(objId) writeC(0) writeD(0) writeD(msgId) writeS(name) writeS(name1) ----
		List<Case> legionLeave = new ArrayList<>();
		legionLeave.add(new Case("leftSingleName",
			"{\"msgId\":1300253,\"playerObjId\":770100,\"name\":\"Nezekan\",\"name1\":null}",
			capture(new SM_LEGION_LEAVE_MEMBER(1300253, 770100, "Nezekan"), null)));
		legionLeave.add(new Case("kickedTwoNames",
			"{\"msgId\":1300254,\"playerObjId\":770101,\"name\":\"Siel\",\"name1\":\"Israphel\"}",
			capture(new SM_LEGION_LEAVE_MEMBER(1300254, 770101, "Siel", "Israphel"), null)));
		writeFixture(outDir.resolve("SM_LEGION_LEAVE_MEMBER.json"), "SM_LEGION_LEAVE_MEMBER", legionLeave);

		// ---- SM_ATREIAN_PASSPORT: header writeH(year/month/day) writeH(size) + per-passport rows (empty list here) ----
		List<Case> atreianPassport = new ArrayList<>();
		atreianPassport.add(new Case("emptyPassports",
			"{\"year\":2024,\"month\":3,\"day\":15,\"stamps\":7,\"passports\":[]}",
			capture(new SM_ATREIAN_PASSPORT(new PassportsList(), 7, LocalDate.of(2024, 3, 15)), null)));
		writeFixture(outDir.resolve("SM_ATREIAN_PASSPORT.json"), "SM_ATREIAN_PASSPORT", atreianPassport);

		// ---- SM_MESSAGE: reads con.getActivePlayer().isStaff(); manual-ctor (sender==null) so senderRace=0, no coords ----
		// activePlayer is a non-staff (accessLevel 0) harness Player, so writeC(activePlayer.isStaff()?0:senderRace)==0.
		List<Case> message = new ArrayList<>();
		{
			Player active = newPlayer(790001, Race.ELYOS);
			AionConnection con = newConnectionWithActivePlayer(active);
			message.add(new Case("normal",
				"{\"chatType\":\"NORMAL\",\"senderObjectId\":790050,\"senderName\":\"Sender\",\"message\":\"Hello Atreia\"}",
				capture(new SM_MESSAGE(790050, "Sender", "Hello Atreia", ChatType.NORMAL), con)));
		}
		{
			Player active = newPlayer(790002, Race.ELYOS);
			AionConnection con = newConnectionWithActivePlayer(active);
			message.add(new Case("systemAnnounce",
				"{\"chatType\":\"GROUP\",\"senderObjectId\":0,\"senderName\":\"\",\"message\":\"Server restart in 5 minutes\"}",
				capture(new SM_MESSAGE(0, "", "Server restart in 5 minutes", ChatType.GROUP), con)));
		}
		{
			// SHOUT branch: sender==null -> x/y/z all 0.0f; still exercises the writeF(x/y/z) tail deterministically.
			Player active = newPlayer(790003, Race.ELYOS);
			AionConnection con = newConnectionWithActivePlayer(active);
			message.add(new Case("shoutNullCoords",
				"{\"chatType\":\"SHOUT\",\"senderObjectId\":790060,\"senderName\":\"Loud\",\"message\":\"Hey!\"}",
				capture(new SM_MESSAGE(790060, "Loud", "Hey!", ChatType.SHOUT), con)));
		}
		writeFixture(outDir.resolve("SM_MESSAGE.json"), "SM_MESSAGE", message);
	}

	// ---- minimal player (only objectId/race vary; faithful base ctor builds the rest; accessLevel 0 => non-staff) ----

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

	private void installSeam() {
		com.aionemu.gameserver.configs.main.CustomConfig.BASE_FLYTIME = 60;
		if (com.aionemu.gameserver.dataholders.DataManager.ABSOLUTE_STATS_DATA == null)
			com.aionemu.gameserver.dataholders.DataManager.ABSOLUTE_STATS_DATA = new com.aionemu.gameserver.dataholders.AbsoluteStatsData();
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
			com.aionemu.gameserver.dataholders.DataManager.PLAYER_EXPERIENCE_TABLE = pxt;
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
