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

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
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
 * Extends the golden packet pipeline to the COMBAT / SKILL / ITEM packet domain: server packets whose
 * {@code writeImpl} is deterministic given ctor scalars + a deterministic {@link HarnessCreature}
 * (fixed objectId / level / game-stats / life-stats). Java is the oracle; the C# side
 * (GoldenPacketFixtureTests) reads the SAME fixtures and asserts byte-for-byte identical payloads.
 *
 * Packets covered here:
 *   SM_SKILL_ACTIVATION    - pure scalars (skillId, isActive, unk via ctor selection)
 *   SM_SKILL_CANCEL        - creature objectId + skillId
 *   SM_ITEM_USAGE_ANIMATION- pure scalars (the time==0 path: no World/Inventory lookup)
 *   SM_CASTSPELL           - creature objectId + scalars + floats; all targetType branches (0/3/4, 1, 2)
 *   SM_ATTACK_STATUS       - creature objectId + getHp/MpPercentage(harness life-stats) + TYPE/LOG ids
 *
 * Excluded (need Rnd / live engine / DataManager / World): SM_ATTACK (AttackResult list + animations),
 * SM_SKILL_COOLDOWN (DataManager.SKILL_DATA duration + System.currentTimeMillis), SM_SKILL_REMOVE
 * (PlayerSkillEntry), SM_CASTSPELL_RESULT (live Effect/SkillEngine state), SM_ABNORMAL_STATE/EFFECT
 * (live effect controller maps).
 */
public class GoldenCombatPacketFixtureGeneratorTest {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	@Test
	public void generateGoldenCombatPacketFixtures() throws IOException {
		Path outDir = repoRoot().resolve("parity-artifacts/golden/packets");
		Files.createDirectories(outDir);

		// ---- SM_SKILL_ACTIVATION: writeH(skillId) writeD(unk) writeC(isActive?1:0) ----
		List<Case> smSkillActivation = new ArrayList<>();
		// toggle ctor (skillId, isActive): unk=0
		smSkillActivation.add(new Case("toggleOn",
			"{\"ctor\":\"toggle\",\"skillId\":1601,\"isActive\":true}",
			capture(new SM_SKILL_ACTIVATION(1601, true))));
		smSkillActivation.add(new Case("toggleOff",
			"{\"ctor\":\"toggle\",\"skillId\":1601,\"isActive\":false}",
			capture(new SM_SKILL_ACTIVATION(1601, false))));
		// stigma-remove ctor (skillId): unk=1, isActive=true
		smSkillActivation.add(new Case("stigmaRemove",
			"{\"ctor\":\"stigma\",\"skillId\":3232}",
			capture(new SM_SKILL_ACTIVATION(3232))));
		writeFixture(outDir.resolve("SM_SKILL_ACTIVATION.json"), "SM_SKILL_ACTIVATION", null, smSkillActivation);

		// ---- SM_SKILL_CANCEL(creature, skillId): writeD(objectId) writeH(skillId) ----
		List<Case> smSkillCancel = new ArrayList<>();
		{
			HarnessCreature c = creature(810001, (byte) 50, new TreeMap<>());
			smSkillCancel.add(new Case("cancel",
				"{\"objectId\":810001,\"skillId\":1346}",
				capture(new SM_SKILL_CANCEL(c, 1346))));
		}
		{
			HarnessCreature c = creature(810002, (byte) 50, new TreeMap<>());
			smSkillCancel.add(new Case("cancelZero",
				"{\"objectId\":810002,\"skillId\":0}",
				capture(new SM_SKILL_CANCEL(c, 0))));
		}
		writeFixture(outDir.resolve("SM_SKILL_CANCEL.json"), "SM_SKILL_CANCEL", null, smSkillCancel);

		// ---- SM_ITEM_USAGE_ANIMATION (time==0 paths only: no World/Inventory access) ----
		// writeD(player) writeD(target) writeD(itemObj) writeD(itemId) writeD(time) writeC(end)
		// writeC(unk) writeC(unk1) writeC(unk2) writeD(unk3)
		List<Case> smItemUsage = new ArrayList<>();
		// 3-arg ctor: target=self, time=0, end=1, unk2=1, unk3=1
		smItemUsage.add(new Case("simple",
			"{\"ctor\":\"player_itemObj_itemId\",\"playerObjId\":820001,\"itemObjId\":830001,\"itemId\":162000001}",
			capture(new SM_ITEM_USAGE_ANIMATION(820001, 830001, 162000001))));
		// 6-arg ctor (player, itemObj, itemId, time, end, unk): time=0, unk3=unk
		smItemUsage.add(new Case("withUnk3",
			"{\"ctor\":\"player_itemObj_itemId_time_end_unk\",\"playerObjId\":820002,\"itemObjId\":830002,\"itemId\":162000002,\"time\":0,\"end\":1,\"unk3\":7}",
			capture(new SM_ITEM_USAGE_ANIMATION(820002, 830002, 162000002, 0, 1, 7))));
		// 7-arg ctor (player, target, itemObj, itemId, time, end, unk): distinct target
		smItemUsage.add(new Case("withTarget",
			"{\"ctor\":\"player_target_itemObj_itemId_time_end_unk\",\"playerObjId\":820003,\"targetObjId\":840003,\"itemObjId\":830003,\"itemId\":162000003,\"time\":0,\"end\":2,\"unk3\":1}",
			capture(new SM_ITEM_USAGE_ANIMATION(820003, 840003, 830003, 162000003, 0, 2, 1))));
		// 10-arg ctor (full): all unks explicit, time=0
		smItemUsage.add(new Case("full",
			"{\"ctor\":\"full\",\"playerObjId\":820004,\"targetObjId\":840004,\"itemObjId\":830004,\"itemId\":162000004,\"time\":0,\"end\":1,\"unk\":2,\"unk1\":3,\"unk2\":4,\"unk3\":5}",
			capture(new SM_ITEM_USAGE_ANIMATION(820004, 840004, 830004, 162000004, 0, 1, 2, 3, 4, 5))));
		writeFixture(outDir.resolve("SM_ITEM_USAGE_ANIMATION.json"), "SM_ITEM_USAGE_ANIMATION", null, smItemUsage);

		// ---- SM_CASTSPELL: writeD(objId) writeH(spellId) writeC(level) writeC(targetType) [branch]
		//      writeH(castDuration) writeC(0) writeF(castSpeed) writeC(boost?1:0) ----
		List<Case> smCastSpell = new ArrayList<>();
		// targetType 0 (object id branch)
		{
			HarnessCreature c = creature(850001, (byte) 50, new TreeMap<>());
			smCastSpell.add(new Case("targetObject",
				"{\"objectId\":850001,\"spellId\":1346,\"level\":5,\"targetType\":0,\"targetObjectId\":860001,\"castDuration\":1500,\"castSpeed\":1.0,\"boost\":true}",
				capture(new SM_CASTSPELL(c, 1346, 5, 0, 860001, 1500, 1.0f, true))));
		}
		// targetType 3 (also object id branch)
		{
			HarnessCreature c = creature(850002, (byte) 50, new TreeMap<>());
			smCastSpell.add(new Case("targetType3",
				"{\"objectId\":850002,\"spellId\":1347,\"level\":1,\"targetType\":3,\"targetObjectId\":860002,\"castDuration\":0,\"castSpeed\":0.75,\"boost\":false}",
				capture(new SM_CASTSPELL(c, 1347, 1, 3, 860002, 0, 0.75f, false))));
		}
		// targetType 1 (ground point: x/y/z floats)
		{
			HarnessCreature c = creature(850003, (byte) 50, new TreeMap<>());
			smCastSpell.add(new Case("groundPoint",
				"{\"objectId\":850003,\"spellId\":1348,\"level\":10,\"targetType\":1,\"x\":1234.5,\"y\":678.25,\"z\":-12.0,\"castDuration\":2000,\"castSpeed\":1.0,\"boost\":true}",
				capture(new SM_CASTSPELL(c, 1348, 10, 1, 1234.5f, 678.25f, -12.0f, 2000, 1.0f, true))));
		}
		// targetType 2 (ground point + 8 unk dwords)
		{
			HarnessCreature c = creature(850004, (byte) 50, new TreeMap<>());
			smCastSpell.add(new Case("groundPointExtended",
				"{\"objectId\":850004,\"spellId\":1349,\"level\":3,\"targetType\":2,\"x\":10.0,\"y\":20.0,\"z\":30.0,\"castDuration\":500,\"castSpeed\":0.5,\"boost\":false}",
				capture(new SM_CASTSPELL(c, 1349, 3, 2, 10.0f, 20.0f, 30.0f, 500, 0.5f, false))));
		}
		writeFixture(outDir.resolve("SM_CASTSPELL.json"), "SM_CASTSPELL", null, smCastSpell);

		// ---- SM_ATTACK_STATUS: writeD(objId) writeD(+/-value) writeC(type) writeC(hpOrMp%) writeH(skill) writeH(log) ----
		// hpOrMp% comes from the harness life-stats (currentHp/maxHp or currentMp/maxMp). Cover the value-sign +
		// hp-vs-mp percentage branches plus the convenience ctors.
		List<Case> smAttackStatus = new ArrayList<>();
		// DAMAGE: writes -value, uses hp%. hp = max(1, 100*9000/12000) = 75.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 12000);
			stats.put(StatEnum.MAXMP, 6000);
			HarnessCreature c = creature(870001, (byte) 55, stats);
			c.setLifeStats(new HarnessLifeStats(c, 9000, 3000));
			smAttackStatus.add(new Case("damage",
				"{\"objectId\":870001,\"type\":\"DAMAGE\",\"typeId\":7,\"skillId\":1346,\"value\":1500,\"writtenValue\":-1500,\"maxHp\":12000,\"maxMp\":6000,\"currentHp\":9000,\"currentMp\":3000,\"hpOrMp\":75,\"log\":\"SPELLATK\",\"logId\":1}",
				capture(new SM_ATTACK_STATUS(c, SM_ATTACK_STATUS.TYPE.DAMAGE, 1346, 1500, SM_ATTACK_STATUS.LOG.SPELLATK))));
		}
		// MP heal: writes +value, uses mp%. mp = 100*3000/6000 = 50.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 12000);
			stats.put(StatEnum.MAXMP, 6000);
			HarnessCreature c = creature(870002, (byte) 55, stats);
			c.setLifeStats(new HarnessLifeStats(c, 9000, 3000));
			smAttackStatus.add(new Case("mpHeal",
				"{\"objectId\":870002,\"type\":\"MP\",\"typeId\":21,\"skillId\":2391,\"value\":500,\"writtenValue\":500,\"maxHp\":12000,\"maxMp\":6000,\"currentHp\":9000,\"currentMp\":3000,\"hpOrMp\":50,\"log\":\"MPHEAL\",\"logId\":4}",
				capture(new SM_ATTACK_STATUS(c, SM_ATTACK_STATUS.TYPE.MP, 2391, 500, SM_ATTACK_STATUS.LOG.MPHEAL))));
		}
		// USED_MP: writes -value, uses mp%.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 10000);
			stats.put(StatEnum.MAXMP, 8000);
			HarnessCreature c = creature(870003, (byte) 50, stats);
			c.setLifeStats(new HarnessLifeStats(c, 5000, 2000)); // mp% = 25
			smAttackStatus.add(new Case("usedMp",
				"{\"objectId\":870003,\"type\":\"USED_MP\",\"typeId\":23,\"skillId\":1234,\"value\":300,\"writtenValue\":-300,\"maxHp\":10000,\"maxMp\":8000,\"currentHp\":5000,\"currentMp\":2000,\"hpOrMp\":25,\"log\":\"REGULAR\",\"logId\":191}",
				capture(new SM_ATTACK_STATUS(c, SM_ATTACK_STATUS.TYPE.USED_MP, 1234, 300))));
		}
		// 2-arg convenience ctor (creature, value): TYPE.REGULAR (default branch -> +value, hp%), LOG.REGULAR.
		{
			TreeMap<StatEnum, Integer> stats = new TreeMap<>();
			stats.put(StatEnum.MAXHP, 4000);
			stats.put(StatEnum.MAXMP, 2000);
			HarnessCreature c = creature(870004, (byte) 50, stats);
			c.setLifeStats(new HarnessLifeStats(c, 4000, 2000)); // hp% = 100
			smAttackStatus.add(new Case("regular",
				"{\"objectId\":870004,\"type\":\"REGULAR\",\"typeId\":5,\"skillId\":0,\"value\":250,\"writtenValue\":250,\"maxHp\":4000,\"maxMp\":2000,\"currentHp\":4000,\"currentMp\":2000,\"hpOrMp\":100,\"log\":\"REGULAR\",\"logId\":191}",
				capture(new SM_ATTACK_STATUS(c, 250))));
		}
		writeFixture(outDir.resolve("SM_ATTACK_STATUS.json"), "SM_ATTACK_STATUS", null, smAttackStatus);

		// ---- SM_ATTACK_RESPONSE: writeC(message) writeC(attackCount); built via the named factories. ----
		List<Case> smAttackResponse = new ArrayList<>();
		smAttackResponse.add(new Case("differentArea",
			"{\"factory\":\"TARGET_IN_DIFFERENT_AREA\",\"message\":1,\"attackCount\":3}",
			capture(SM_ATTACK_RESPONSE.TARGET_IN_DIFFERENT_AREA(3))));
		smAttackResponse.add(new Case("invalidTarget",
			"{\"factory\":\"STOP_INVALID_TARGET\",\"message\":2,\"attackCount\":0}",
			capture(SM_ATTACK_RESPONSE.STOP_INVALID_TARGET(0))));
		smAttackResponse.add(new Case("tooFar",
			"{\"factory\":\"TARGET_TOO_FAR_AWAY\",\"message\":4,\"attackCount\":7}",
			capture(SM_ATTACK_RESPONSE.TARGET_TOO_FAR_AWAY(7))));
		smAttackResponse.add(new Case("obstacle",
			"{\"factory\":\"STOP_OBSTACLE_IN_THE_WAY\",\"message\":5,\"attackCount\":2}",
			capture(SM_ATTACK_RESPONSE.STOP_OBSTACLE_IN_THE_WAY(2))));
		smAttackResponse.add(new Case("tooClose",
			"{\"factory\":\"STOP_TOO_CLOSE_TO_ATTACK\",\"message\":6,\"attackCount\":1}",
			capture(SM_ATTACK_RESPONSE.STOP_TOO_CLOSE_TO_ATTACK(1))));
		smAttackResponse.add(new Case("noMessage",
			"{\"factory\":\"STOP_WITHOUT_MESSAGE\",\"message\":7,\"attackCount\":255}",
			capture(SM_ATTACK_RESPONSE.STOP_WITHOUT_MESSAGE(255))));
		writeFixture(outDir.resolve("SM_ATTACK_RESPONSE.json"), "SM_ATTACK_RESPONSE", null, smAttackResponse);

		// ---- SM_ABNORMAL_STATE: header path with an EMPTY effect collection (no live SkillEngine Effect
		//      needed). writeD(abnormals) writeD(0) writeD(0) writeC(slot) writeH(0). ----
		List<Case> smAbnormalState = new ArrayList<>();
		smAbnormalState.add(new Case("emptySlot0",
			"{\"abnormals\":0,\"slot\":0,\"effectCount\":0}",
			capture(new SM_ABNORMAL_STATE(new java.util.ArrayList<>(), 0, 0))));
		smAbnormalState.add(new Case("emptyWithBitmask",
			"{\"abnormals\":131073,\"slot\":2,\"effectCount\":0}",
			capture(new SM_ABNORMAL_STATE(new java.util.ArrayList<>(), 131073, 2))));
		writeFixture(outDir.resolve("SM_ABNORMAL_STATE.json"), "SM_ABNORMAL_STATE", null, smAbnormalState);
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
	 * Mirrors GoldenLivePacketFixtureGeneratorTest.HarnessCreature.
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
