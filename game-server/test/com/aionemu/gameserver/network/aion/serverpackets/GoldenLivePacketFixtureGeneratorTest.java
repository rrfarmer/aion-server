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
import com.aionemu.gameserver.model.animations.ObjectDeleteAnimation;
import com.aionemu.gameserver.model.gameobjects.PetSpecialFunction;
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

		// ---- SM_PET: the fully-deterministic scalar branches (no live Pet/PetCommonData/World needed). ----
		// RENAME / DISMISS / SPECIAL_FUNCTION read only ctor scalars + enum ids; no DataManager, no Pet object.
		// (LOAD_PETS/ADOPT/SURRENDER need a PetCommonData+PetTemplate from DataManager.PET_DATA, SPAWN needs a live
		//  Pet with position/moveController/master, MOOD/FOOD need a PetCommonData feed/mood model — excluded here.)
		List<Case> smPet = new ArrayList<>();
		// RENAME: writeH(action=10) writeD(petObjectId) writeS(petName)
		smPet.add(new Case("rename",
			"{\"action\":\"RENAME\",\"petObjectId\":424242,\"petName\":\"Fluffy\"}",
			capture(new SM_PET(424242, "Fluffy"))));
		// DISMISS: writeH(action=4) writeD(petObjectId) writeC(animationId)
		smPet.add(new Case("dismiss",
			"{\"action\":\"DISMISS\",\"petObjectId\":555111,\"animation\":\"FADE_OUT\"}",
			capture(new SM_PET(555111, ObjectDeleteAnimation.FADE_OUT))));
		// SPECIAL_FUNCTION / DOPING (subType=2): writeH(action=13) writeC(2) writeC(dopeAction) + per-dopeAction payload
		smPet.add(new Case("dopingAdd",
			"{\"action\":\"SPECIAL_FUNCTION\",\"subType\":2,\"dopeAction\":0,\"itemId\":700,\"slot\":3}",
			capture(new SM_PET(0, 700, 3))));
		smPet.add(new Case("dopingRemove",
			"{\"action\":\"SPECIAL_FUNCTION\",\"subType\":2,\"dopeAction\":1,\"itemId\":700,\"slot\":3}",
			capture(new SM_PET(1, 700, 3))));
		smPet.add(new Case("dopingMove",
			"{\"action\":\"SPECIAL_FUNCTION\",\"subType\":2,\"dopeAction\":2,\"itemId\":700,\"slot\":3}",
			capture(new SM_PET(2, 700, 3))));
		smPet.add(new Case("dopingUse",
			"{\"action\":\"SPECIAL_FUNCTION\",\"subType\":2,\"dopeAction\":3,\"itemId\":700,\"slot\":3}",
			capture(new SM_PET(3, 700, 3))));
		// SPECIAL_FUNCTION / AUTOLOOT (subType=3): with npc objId and without
		smPet.add(new Case("autolootNpc",
			"{\"action\":\"SPECIAL_FUNCTION\",\"specialFunction\":\"AUTOLOOT\",\"active\":true,\"npcObjId\":987654}",
			capture(new SM_PET(PetSpecialFunction.AUTOLOOT, true, 987654))));
		smPet.add(new Case("autolootActivate",
			"{\"action\":\"SPECIAL_FUNCTION\",\"specialFunction\":\"AUTOLOOT\",\"active\":true,\"npcObjId\":0}",
			capture(new SM_PET(PetSpecialFunction.AUTOLOOT, true, 0))));
		smPet.add(new Case("autolootDeactivate",
			"{\"action\":\"SPECIAL_FUNCTION\",\"specialFunction\":\"AUTOLOOT\",\"active\":false,\"npcObjId\":0}",
			capture(new SM_PET(PetSpecialFunction.AUTOLOOT, false, 0))));
		// SPECIAL_FUNCTION / AUTOSELL (subType=4)
		smPet.add(new Case("autosellActive",
			"{\"action\":\"SPECIAL_FUNCTION\",\"specialFunction\":\"AUTOSELL\",\"active\":true,\"npcObjId\":0}",
			capture(new SM_PET(PetSpecialFunction.AUTOSELL, true))));
		smPet.add(new Case("autosellInactive",
			"{\"action\":\"SPECIAL_FUNCTION\",\"specialFunction\":\"AUTOSELL\",\"active\":false,\"npcObjId\":0}",
			capture(new SM_PET(PetSpecialFunction.AUTOSELL, false))));
		writeFixture(outDir.resolve("SM_PET.json"), "SM_PET", null, smPet);

		// ---- SM_MANTRA_EFFECT(creature, subEffectId): writeD(0) writeD(effector.objId) writeH(subEffectId). ----
		// writeImpl reads ONLY effector.getObjectId() (ctor-stored) + the ctor subEffectId. Fully deterministic.
		List<Case> smMantraEffect = new ArrayList<>();
		{
			HarnessCreature c = creature(910001, (byte) 50, new TreeMap<>());
			smMantraEffect.add(new Case("basic",
				"{\"objectId\":910001,\"subEffectId\":1234}",
				capture(new SM_MANTRA_EFFECT(c, 1234))));
		}
		{
			HarnessCreature c = creature(910002, (byte) 50, new TreeMap<>());
			smMantraEffect.add(new Case("zeroSub",
				"{\"objectId\":910002,\"subEffectId\":0}",
				capture(new SM_MANTRA_EFFECT(c, 0))));
		}
		{
			HarnessCreature c = creature(910003, (byte) 50, new TreeMap<>());
			smMantraEffect.add(new Case("maxShort",
				"{\"objectId\":910003,\"subEffectId\":65535}",
				capture(new SM_MANTRA_EFFECT(c, 65535))));
		}
		writeFixture(outDir.resolve("SM_MANTRA_EFFECT.json"), "SM_MANTRA_EFFECT", null, smMantraEffect);

		// ---- SM_DELETE(visibleObject[, animation|inRange]): writeD(objId) writeC(animationId). ----
		// writeImpl reads ONLY the ctor-stored objectId + the ctor-resolved animationId. The animationId is
		// inRange ? animation.getId() : NONE.getId(). Covers each public ctor + the inRange=false NONE branch.
		List<Case> smDelete = new ArrayList<>();
		{
			HarnessCreature c = creature(920001, (byte) 50, new TreeMap<>());
			smDelete.add(new Case("defaultFadeOut", // SM_DELETE(obj) -> FADE_OUT, inRange true -> id 1
				"{\"objectId\":920001,\"animationId\":1}",
				capture(new SM_DELETE(c))));
		}
		{
			HarnessCreature c = creature(920002, (byte) 50, new TreeMap<>());
			smDelete.add(new Case("outOfRangeNone", // SM_DELETE(obj, false) -> inRange false -> NONE id 0
				"{\"objectId\":920002,\"animationId\":0}",
				capture(new SM_DELETE(c, false))));
		}
		{
			HarnessCreature c = creature(920003, (byte) 50, new TreeMap<>());
			smDelete.add(new Case("jumpIn", // SM_DELETE(obj, JUMP_IN) -> id 11
				"{\"objectId\":920003,\"animationId\":11}",
				capture(new SM_DELETE(c, ObjectDeleteAnimation.JUMP_IN))));
		}
		{
			HarnessCreature c = creature(920004, (byte) 50, new TreeMap<>());
			smDelete.add(new Case("delayed", // SM_DELETE(obj, DELAYED) -> id 19
				"{\"objectId\":920004,\"animationId\":19}",
				capture(new SM_DELETE(c, ObjectDeleteAnimation.DELAYED))));
		}
		writeFixture(outDir.resolve("SM_DELETE.json"), "SM_DELETE", null, smDelete);
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
