# Phase 6 Condition Preview Java Golden Capture Draft

Status: Uncompiled source draft. No Java runtime output has been captured.

This draft follows `docs/Phase-6-ConditionPreviewJavaGoldenHarnessDesign.md` and is intended to become:

`game-server/test/com/aionemu/gameserver/skillengine/condition/ConditionPreviewGoldenCaptureTest.java`

after JDK 25 and Maven are available.

The draft deliberately keeps Java runtime evidence marked absent. It must not be copied into `game-server/test` or used to update the contract artifact until it compiles and executes against the Java source-of-truth classes.

```java
package com.aionemu.gameserver.skillengine.condition;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.CreatureTemplate;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.state.FlyState;
import com.aionemu.gameserver.model.items.ChargeInfo;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * Draft-only Java source-of-truth capture for Phase 6 condition preview parity.
 *
 * Java artifacts exercised:
 * - Conditions.validate(Stat2, IStatFunction)
 * - Condition.validate(Stat2, IStatFunction)
 * - WeaponCondition.validate(Stat2, IStatFunction) with WeaponCondition.itemGroups
 * - ItemChargeCondition.validate(Stat2, IStatFunction)
 * - OnFlyCondition.validate(Stat2, IStatFunction)
 * - Item.getChargeLevel()
 * - Player.getEquipment().getMainHandWeaponType()
 */
class ConditionPreviewGoldenCaptureTest {

	private static final Path CONTRACT_PATH = Path.of("docs", "phase6-condition-preview-golden-fixture-contract.json");

	@Test
	void capturesConditionPreviewFixtures() throws Exception {
		List<CapturedFixture> captured = List.of(
			weaponPlayerMainhandMatch(),
			weaponPlayerMainhandMismatch(),
			weaponNonPlayerPassThrough(),
			frontStatPassThrough(),
			chargeItemOwnerLevelSatisfies(),
			chargeItemOwnerLevelTooLow(),
			chargeNonItemOwnerFalse(),
			onflyOwnerFlying(),
			onflyOwnerNotFlying(),
			mixedShortCircuitWeaponBeforeCharge());

		assertEquals(10, captured.size());
		assertAll(captured.stream().map(fixture -> () -> assertNotNull(fixture.fixtureName())));

		if (Boolean.getBoolean("aion.conditionPreview.capture"))
			writeCapturedContract(captured);
	}

	private static CapturedFixture weaponPlayerMainhandMatch() throws Exception {
		WeaponCondition weapon = weaponCondition(ItemGroup.ORB, ItemGroup.SPELLBOOK);
		Player player = playerWithMainHand(ItemGroup.ORB);
		return capture("weapon-player-mainhand-match", conditions(weapon), stat(player), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture weaponPlayerMainhandMismatch() throws Exception {
		WeaponCondition weapon = weaponCondition(ItemGroup.ORB, ItemGroup.SPELLBOOK);
		Player player = playerWithMainHand(ItemGroup.DAGGER);
		return capture("weapon-player-mainhand-mismatch", conditions(weapon), stat(player), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture weaponNonPlayerPassThrough() throws Exception {
		WeaponCondition weapon = weaponCondition(ItemGroup.ORB);
		return capture("weapon-non-player-pass-through", conditions(weapon), stat(nonPlayerCreature(false)), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture frontStatPassThrough() {
		return capture("front-stat-pass-through", conditions(new FrontCondition()), stat(nonPlayerCreature(false)), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture chargeItemOwnerLevelSatisfies() throws Exception {
		Item item = chargedItem(ChargeInfo.LEVEL1 + 1);
		return capture("charge-item-owner-level-satisfies", conditions(chargeCondition(1)), stat(nonPlayerCreature(false)), statFunction(item));
	}

	private static CapturedFixture chargeItemOwnerLevelTooLow() throws Exception {
		Item item = chargedItem(1);
		return capture("charge-item-owner-level-too-low", conditions(chargeCondition(2)), stat(nonPlayerCreature(false)), statFunction(item));
	}

	private static CapturedFixture chargeNonItemOwnerFalse() {
		return capture("charge-non-item-owner-false", conditions(chargeCondition(1)), stat(nonPlayerCreature(false)), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture onflyOwnerFlying() {
		return capture("onfly-owner-flying", conditions(new OnFlyCondition()), stat(nonPlayerCreature(true)), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture onflyOwnerNotFlying() {
		return capture("onfly-owner-not-flying", conditions(new OnFlyCondition()), stat(nonPlayerCreature(false)), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture mixedShortCircuitWeaponBeforeCharge() throws Exception {
		WeaponCondition weapon = weaponCondition(ItemGroup.ORB);
		Player player = playerWithMainHand(ItemGroup.DAGGER);
		return capture("mixed-short-circuit-weapon-before-charge", conditions(weapon, chargeCondition(1)), stat(player), statFunction(new TestStatOwner()));
	}

	private static CapturedFixture capture(String fixtureName, Conditions conditions, Stat2 stat, IStatFunction statFunction) {
		List<String> conditionStatuses = new ArrayList<>();
		boolean overall = true;
		for (Condition condition : conditions.getConditions()) {
			if (!overall) {
				conditionStatuses.add(conditionName(condition) + ":NotEvaluated");
				continue;
			}

			boolean satisfied = condition.validate(stat, statFunction);
			conditionStatuses.add(conditionName(condition) + ":" + (satisfied ? "Satisfied" : "NotSatisfied"));
			if (!satisfied)
				overall = false;
		}

		assertEquals(overall, conditions.validate(stat, statFunction));
		return new CapturedFixture(fixtureName, overall, conditionStatuses);
	}

	private static Conditions conditions(Condition... conditionList) {
		Conditions conditions = new Conditions();
		for (Condition condition : conditionList)
			conditions.getConditions().add(condition);
		return conditions;
	}

	private static WeaponCondition weaponCondition(ItemGroup... itemGroups) throws Exception {
		WeaponCondition condition = new WeaponCondition();
		setPrivateField(WeaponCondition.class, condition, "itemGroups", List.of(itemGroups));
		return condition;
	}

	private static ItemChargeCondition chargeCondition(int value) {
		ItemChargeCondition condition = new ItemChargeCondition();
		condition.value = value;
		return condition;
	}

	private static Stat2 stat(Creature owner) {
		return new AdditionStat(StatEnum.POWER, 100, owner);
	}

	private static IStatFunction statFunction(StatOwner owner) {
		return new IStatFunction() {
			@Override
			public StatEnum getName() {
				return StatEnum.POWER;
			}

			@Override
			public boolean isBonus() {
				return true;
			}

			@Override
			public int getPriority() {
				return 0;
			}

			@Override
			public int getValue() {
				return 0;
			}

			@Override
			public boolean validate(Stat2 stat) {
				return true;
			}

			@Override
			public void apply(Stat2 stat, CalculationType... calculationTypes) {
			}

			@Override
			public StatOwner getOwner() {
				return owner;
			}

			@Override
			public boolean hasConditions() {
				return true;
			}
		};
	}

	private static Player playerWithMainHand(ItemGroup itemGroup) throws Exception {
		PlayerCommonData commonData = new PlayerCommonData(1);
		commonData.setName("condition-preview");
		// TODO: set race, gender, class, and level with real enum values after compiling this draft.
		PlayerAppearance appearance = new PlayerAppearance();
		Player player = new Player(new PlayerAccountData(commonData, appearance), new Account(1));
		Item item = itemWithGroup(itemGroup, 1001, true, ItemSlot.MAIN_HAND.getSlotIdMask(), 0);
		player.getEquipment().onLoadHandler(item);
		assertEquals(itemGroup, player.getEquipment().getMainHandWeaponType());
		return player;
	}

	private static Item chargedItem(int chargePoints) throws Exception {
		// TODO: prefer a real ItemTemplate with improvement data so Item.updateChargeInfo creates ChargeInfo naturally.
		// If that setup is too heavy, document the blocker before using reflection to set Item.conditioningInfo.
		return itemWithGroup(ItemGroup.ORB, 1002, false, 0, chargePoints);
	}

	private static Item itemWithGroup(ItemGroup itemGroup, int objectId, boolean equipped, long equipmentSlot, int chargePoints) throws Exception {
		ItemTemplate template = new ItemTemplate();
		setPrivateField(ItemTemplate.class, template, "itemId", objectId);
		setPrivateField(ItemTemplate.class, template, "itemGroup", itemGroup);
		Item item = new Item(objectId, template, 1, equipped, equipmentSlot);
		if (chargePoints > 0)
			setPrivateField(Item.class, item, "conditioningInfo", new ChargeInfo(chargePoints, item));
		return item;
	}

	private static Creature nonPlayerCreature(boolean flying) {
		return new Creature(2001, null, null, new TestCreatureTemplate(), null, false) {
			@Override
			public byte getLevel() {
				return 1;
			}

			@Override
			public boolean isFlying() {
				return flying;
			}
		};
	}

	private static String conditionName(Condition condition) {
		if (condition instanceof WeaponCondition)
			return "weapon";
		if (condition instanceof FrontCondition)
			return "front";
		if (condition instanceof ItemChargeCondition)
			return "charge";
		if (condition instanceof OnFlyCondition)
			return "onfly";
		return condition.getClass().getSimpleName();
	}

	private static void setPrivateField(Class<?> type, Object target, String name, Object value) throws Exception {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void writeCapturedContract(List<CapturedFixture> captured) throws Exception {
		Map<String, Object> artifact = new LinkedHashMap<>();
		artifact.put("schemaVersion", 1);
		artifact.put("artifactType", "condition-preview-golden-fixture-contract");
		artifact.put("evidenceLevel", "java-runtime-captured");
		artifact.put("javaRuntimeEvidenceCaptured", true);
		artifact.put("javaCaptureStatus", "captured");
		artifact.put("fixtures", captured);
		Files.writeString(CONTRACT_PATH, JSON.toJSONString(artifact, JSONWriter.Feature.PrettyFormat), StandardCharsets.UTF_8);
	}

	private record CapturedFixture(String fixtureName, boolean overallResult, List<String> conditionStatuses) {
	}

	private static final class TestStatOwner implements StatOwner {
	}

	private static final class TestCreatureTemplate extends CreatureTemplate {
		@Override
		public int getTemplateId() {
			return 0;
		}

		@Override
		public int getL10nId() {
			return 0;
		}

		@Override
		public String getName() {
			return "condition-preview-creature";
		}
	}
}
```

## Draft Risks To Resolve Before Copying

- `PlayerCommonData` setup in the draft still needs concrete race, gender, class, and level values verified against Java startup/static-data requirements.
- The `Creature` constructor may touch AI/static-data services through `Creature`/`VisibleObject`; if plain JUnit construction fails, move capture to an opt-in runtime utility after game-server data initialization.
- `Item.getChargeLevel()` should be captured through real `ChargeInfo` behavior. Reflection is shown only as a draft fallback and must be documented if retained.
- The writer currently sketches only a minimal captured contract shape. Before replacing the existing contract artifact, merge captured fields into the full schema rather than dropping source-derived expected inputs.
