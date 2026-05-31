# Phase 6 Condition Preview Java Golden Harness Design

Status: Source-only design. No Java runtime output has been captured.

## Purpose

Capture Java source-of-truth outputs for the isolated stat-condition preview fixtures defined by `SkillStatConditionPreviewGoldenFixturePlanService` and mirrored in `docs/phase6-condition-preview-golden-fixture-contract.json`.

The harness must execute Java `Conditions.validate(Stat2, IStatFunction)` for each fixture and write deterministic JSON output back into the contract artifact only after it runs under a JDK/Maven toolchain compatible with the repository.

## Current Blocker

- Local `java -version` reports Java `1.8.0_491`.
- Root `pom.xml` sets `maven.compiler.release` to `25`.
- Local `mvn` is unavailable on PATH.

Until those are fixed, this document is a design artifact, not runtime evidence.

## Proposed Location

Add a JUnit test or opt-in capture utility under:

`game-server/test/com/aionemu/gameserver/skillengine/condition/ConditionPreviewGoldenCaptureTest.java`

Keep it opt-in if it writes files. A read-only assertion test may run normally, but artifact writing should require a system property such as:

`-Daion.conditionPreview.capture=true`

## Java Artifacts To Exercise

- `com.aionemu.gameserver.skillengine.condition.Conditions`
- `com.aionemu.gameserver.skillengine.condition.Condition`
- `com.aionemu.gameserver.skillengine.condition.WeaponCondition`
- `com.aionemu.gameserver.skillengine.condition.FrontCondition`
- `com.aionemu.gameserver.skillengine.condition.ItemChargeCondition`
- `com.aionemu.gameserver.skillengine.condition.OnFlyCondition`
- `com.aionemu.gameserver.model.stats.calc.Stat2`
- `com.aionemu.gameserver.model.stats.calc.functions.IStatFunction`
- `com.aionemu.gameserver.model.gameobjects.Item`

## Harness Shape

1. Read `docs/phase6-condition-preview-golden-fixture-contract.json`.
2. For each fixture, construct a Java `Conditions` instance and append child conditions in the fixture's listed order using `getConditions().add(...)`.
3. Set condition XML-equivalent fields on Java condition instances:
   - `WeaponCondition.itemGroups` is private, so set it via reflection or JAXB unmarshalling.
   - `ChargeCondition.value` is protected and the test package can access it if the harness is in `com.aionemu.gameserver.skillengine.condition`.
4. Construct a minimal `Stat2` using `AdditionStat` with a controlled owner.
5. Construct a minimal `IStatFunction` whose `getOwner()` returns either an `Item` or a non-Item `StatOwner`, depending on the fixture.
6. Call `conditions.validate(stat, statFunction)`.
7. Record the overall boolean result and per-condition statuses, including `NotEvaluated` for children skipped by Java's first-failure short-circuit.
8. Write captured outputs into a new `capturedResults` section, preserving the source-derived expected values for comparison.

## Fixture Inputs

| Fixture | Java Setup |
|---|---|
| `weapon-player-mainhand-match` | Real or minimal `Player`; main-hand `ItemTemplate.itemGroup = ORB`; `WeaponCondition.itemGroups = [ORB, SPELLBOOK]`; expect satisfied. |
| `weapon-player-mainhand-mismatch` | Same player setup, but main-hand `ItemTemplate.itemGroup = DAGGER`; expect not satisfied. |
| `weapon-non-player-pass-through` | Non-`Player` `Creature` owner; `WeaponCondition.itemGroups = [ORB]`; expect satisfied because Java skips NPC weapon validation. |
| `front-stat-pass-through` | `FrontCondition` child only; any valid `Stat2` owner; expect satisfied from base `Condition.validate(Stat2, IStatFunction)`. |
| `charge-item-owner-level-satisfies` | `IStatFunction.getOwner()` returns an `Item` whose `getChargeLevel()` returns `2`; `value = 1`; expect satisfied. |
| `charge-item-owner-level-too-low` | `IStatFunction.getOwner()` returns an `Item` whose `getChargeLevel()` returns `1`; `value = 2`; expect not satisfied. |
| `charge-non-item-owner-false` | `IStatFunction.getOwner()` returns a non-`Item` `StatOwner`; `value = 1`; expect not satisfied. |
| `onfly-owner-flying` | `Stat2.getOwner().isFlying()` returns true; expect satisfied. |
| `onfly-owner-not-flying` | `Stat2.getOwner().isFlying()` returns false; expect not satisfied. |
| `mixed-short-circuit-weapon-before-charge` | `WeaponCondition` first returns false, `ItemChargeCondition` second would require an Item owner if reached; expect `weapon:NotSatisfied`, `charge:NotEvaluated`, and overall false. |

## Player/Item Construction Notes

The player weapon fixtures should use the real Java branch guarded by `creature instanceof Player`; do not replace this with a non-player fake.

Possible setup:

1. Create `PlayerCommonData`, set race, gender, player class, name, and level without relying on database state.
2. Create `PlayerAccountData` with a `PlayerAppearance`.
3. Create `Account` and `Player`.
4. Create an `ItemTemplate` and set its private `itemGroup` field via reflection.
5. Create an `Item`, mark it equipped in main hand, set its equipment slot, and call `player.getEquipment().onLoadHandler(item)`.
6. Confirm `player.getEquipment().getMainHandWeaponType()` returns the intended `ItemGroup` before running `WeaponCondition.validate`.

If the real `Player` constructor requires static data that is not initialized in a plain unit test, document that failure and either:

- initialize the same Java static data used by existing game-server tests, or
- move the capture to an opt-in runtime utility that runs after game-server static-data load.

## Charge Construction Notes

Prefer real `Item.getChargeLevel()` behavior instead of overriding the method. Use an `ItemTemplate` with improvement data if feasible, then construct `Item` with charge points:

- charge level 2 requires charge points above `ChargeInfo.LEVEL1`.
- charge level 1 requires positive charge points at or below `ChargeInfo.LEVEL1`.

If improvement/template setup is too heavy for a plain test, document the exact blocker before using a subclass or reflection-based shortcut.

## Required Output Schema Additions

When Java capture runs successfully, append this shape to each fixture:

```json
{
  "capturedJava": {
    "overallResult": true,
    "conditionStatuses": [ "weapon:Satisfied" ],
    "javaClassPath": "game-server/test/com/aionemu/gameserver/skillengine/condition/ConditionPreviewGoldenCaptureTest.java"
  }
}
```

Then update:

- `evidenceLevel` from `contract-only` to `java-runtime-captured`
- `javaRuntimeEvidenceCaptured` from `false` to `true`
- `javaCaptureStatus` from `blocked-local-toolchain` to `captured`
- `javaCaptureBlockers` to an empty array

Only after that should C# tests compare captured Java outputs against the isolated preview evaluator.

## Validation Command

Once JDK 25 and Maven are available:

```powershell
mvn -pl game-server -DskipTests=false -Dtest=ConditionPreviewGoldenCaptureTest test
```

If the harness is opt-in for writing artifacts:

```powershell
mvn -pl game-server -DskipTests=false -Dtest=ConditionPreviewGoldenCaptureTest -Daion.conditionPreview.capture=true test
```
