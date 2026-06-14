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
import java.util.Map;
import java.util.TreeMap;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.stats.container.CreatureGameStats;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.stats.StatsTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * Extends the golden packet pipeline (GoldenPacketFixtureGeneratorTest) to LIVE-OBJECT server
 * packets: those whose {@code writeImpl} reads a {@link Creature}/Player. We supply a deterministic
 * {@link HarnessCreature} (fixed objectId / level / state / visual+see state / fixed game-stats &
 * life-stats) so the bytes are bilaterally reproducible without a live World/Knownlist/spawn.
 *
 * The C# side (GoldenPacketFixtureTests.FaithfulCsharpPayloadMatchesJavaGoldenFixture) reads the SAME
 * fixtures and asserts its writers emit identical bytes. Java is the oracle.
 *
 * Only packets whose writeImpl is deterministic given the harness state belong here. Packets that
 * read live position/heading (SM_LOOKATOBJECT), Knownlist, Inventory, Legion, or wall-clock time
 * need more harness than exists and are intentionally excluded.
 */
public class GoldenLivePacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	@Test
	public void generateGoldenLivePacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		// ---- SM_PLAYER_STATE(creature): objectId + visualState + seeState + BLINKING conditional byte ----
		List<Case> smPlayerState = new ArrayList<>();
		{
			HarnessCreature c = creature(700001, (byte) 50, new TreeMap<>());
			smPlayerState.add(new Case("visible",
				"{\"objectId\":700001,\"visualState\":0,\"seeState\":0}",
				capture(new SM_PLAYER_STATE(c))));
		}
		{
			HarnessCreature c = creature(700002, (byte) 50, new TreeMap<>());
			c.setVisualState(CreatureVisualState.BLINKING); // 0 | 64 = 64
			smPlayerState.add(new Case("blinking",
				"{\"objectId\":700002,\"visualState\":64,\"seeState\":0}",
				capture(new SM_PLAYER_STATE(c))));
		}
		{
			HarnessCreature c = creature(700003, (byte) 50, new TreeMap<>());
			c.setVisualState(CreatureVisualState.HIDE2);   // 0 | 2 = 2
			c.setSeeState(CreatureSeeState.SEARCH1);        // 0 | 1 = 1
			smPlayerState.add(new Case("hidden",
				"{\"objectId\":700003,\"visualState\":2,\"seeState\":1}",
				capture(new SM_PLAYER_STATE(c))));
		}
		writeFixture(outDir.resolve("SM_PLAYER_STATE.json"), "SM_PLAYER_STATE", null, smPlayerState);

		// ---- SM_TARGET_SELECTED(creature) non-null: objectId, level, maxHp, currentHp, maxMp, currentMp ----
		List<Case> smTargetSelected = new ArrayList<>();
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 12345);
			stats.put(StatEnum.MAXMP, 6789);
			HarnessCreature c = creature(800010, (byte) 55, stats);
			c.setLifeStats(new HarnessLifeStats(c, 9000, 4000));
			smTargetSelected.add(new Case("creature",
				"{\"objectId\":800010,\"level\":55,\"maxHp\":12345,\"currentHp\":9000,\"maxMp\":6789,\"currentMp\":4000}",
				capture(new SM_TARGET_SELECTED(c))));
		}
		writeFixture(outDir.resolve("SM_TARGET_SELECTED_LIVE.json"), "SM_TARGET_SELECTED_LIVE", null, smTargetSelected);

		// ---- SM_EMOTION(creature, type): objectId, typeId, state(H), speed(F=6.0), then per-type branch ----
		// HarnessStats fixes ATTACK_SPEED base/current=1000 and movement speed current=6000 (-> 6.0f).
		List<Case> smEmotion = new ArrayList<>();
		{
			HarnessCreature c = creature(900020, (byte) 50, new TreeMap<>()); // state = ACTIVE (1)
			smEmotion.add(new Case("fly",
				"{\"objectId\":900020,\"type\":\"FLY\",\"state\":1,\"speed\":6.0}",
				capture(new SM_EMOTION(c, EmotionType.FLY)))); // no extra payload branch
		}
		{
			HarnessCreature c = creature(900021, (byte) 50, new TreeMap<>());
			smEmotion.add(new Case("resurrect",
				"{\"objectId\":900021,\"type\":\"RESURRECT\",\"state\":1,\"speed\":6.0}",
				capture(new SM_EMOTION(c, EmotionType.RESURRECT)))); // writeD(0)
		}
		{
			HarnessCreature c = creature(900022, (byte) 50, new TreeMap<>());
			smEmotion.add(new Case("changeSpeed",
				"{\"objectId\":900022,\"type\":\"CHANGE_SPEED\",\"state\":1,\"speed\":6.0,\"baseAtkSpeed\":1000,\"currentAtkSpeed\":1000}",
				capture(new SM_EMOTION(c, EmotionType.CHANGE_SPEED)))); // writeH(base) writeH(current) writeC(0)
		}
		{
			HarnessCreature c = creature(900023, (byte) 50, new TreeMap<>());
			smEmotion.add(new Case("emoteWithTarget",
				"{\"objectId\":900023,\"type\":\"EMOTE\",\"state\":1,\"speed\":6.0,\"emotion\":42,\"targetObjectId\":555}",
				capture(new SM_EMOTION(c, EmotionType.EMOTE, 42, 555)))); // writeD(target) writeH(emotion) writeC(1)
		}
		writeFixture(outDir.resolve("SM_EMOTION.json"), "SM_EMOTION", null, smEmotion);
	}

	private static HarnessCreature creature(int objectId, byte level, TreeMap<StatEnum, Integer> statMap) {
		return new HarnessCreature(objectId, level, statMap);
	}

	/** Capture the payload bytes a packet's writeImpl produces (no opcode, no crypt). */
	private static String capture(AionServerPacket packet) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
			packet.setBuf(buffer);
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

	/**
	 * Minimal deterministic Creature: fixed objectId/level, harness game-stats + life-stats. No spawn/world.
	 * Mirrors the formula-test HarnessCreature but adds a settable objectId (the only extra packet writeImpls need).
	 */
	static final class HarnessCreature extends Creature {
		private final byte level;
		private final CreatureGameStats<?> gs;

		HarnessCreature(int objectId, byte level, TreeMap<StatEnum, Integer> statMap) {
			super(objectId, null, null, new NpcTemplate(), null, false);
			this.level = level;
			this.gs = new HarnessStats(this, statMap);
		}

		@Override
		public byte getLevel() { return level; }

		@Override
		public Race getRace() { return Race.NPC; }

		@Override
		public CreatureGameStats<? extends Creature> getGameStats() { return gs; }

		@Override
		public com.aionemu.gameserver.model.gameobjects.player.Player getActingCreature() { return null; }
	}

	// Same seam as the formula HarnessStats: resolve a fixed value per StatEnum (else use the passed base).
	static final class HarnessStats extends CreatureGameStats<Creature> {
		private final Map<StatEnum, Integer> statMap;

		HarnessStats(Creature owner, Map<StatEnum, Integer> statMap) {
			super(owner);
			this.statMap = statMap;
		}

		@Override
		public Stat2 getStat(StatEnum statEnum, float base, com.aionemu.gameserver.utils.stats.CalculationType... calculationTypes) {
			float resolved = statMap.containsKey(statEnum) ? statMap.get(statEnum) : base;
			return new AdditionStat(statEnum, resolved, owner);
		}

		@Override public StatsTemplate getStatsTemplate() { return new StatsTemplate(); }
		@Override public Stat2 getAttackSpeed() { return new AdditionStat(StatEnum.ATTACK_SPEED, 1000, owner); }
		@Override public Stat2 getMovementSpeed() { return new AdditionStat(StatEnum.SPEED, 6000, owner); }
		@Override public Stat2 getAttackRange() { return new AdditionStat(StatEnum.ATTACK_RANGE, 1500, owner); }
		@Override public Stat2 getHpRegenRate() { return new AdditionStat(StatEnum.REGEN_HP, 1, owner); }
		@Override public Stat2 getMpRegenRate() { return new AdditionStat(StatEnum.REGEN_MP, 1, owner); }
	}

	// Fixed currentHp/currentMp; maxHp/maxMp resolve through the harness game-stats (StatEnum.MAXHP/MAXMP).
	static final class HarnessLifeStats extends CreatureLifeStats<Creature> {
		HarnessLifeStats(Creature owner, int currentHp, int currentMp) {
			super(owner, currentHp, currentMp);
		}

		@Override
		public void triggerRestoreTask() {
			// no-op: no world/scheduler in the harness
		}
	}
}
