# Phase 6 Bind-Point Teleport - Pet Feed Unusual Storage STAT_BONUSES Mapping Audit

Date: 2026-05-27
Unit of Work: UOW-1387
Status: Read-only mapping audit complete. No `STAT_BONUSES` serialization implemented yet.

## Java Source Rule

Java `ItemInfoBlob.getFullBlob` appends one `STAT_BONUSES` blob entry for each item-template modifier when all of these are true:

- `itemTemplate.getModifiers()` is not null;
- `modifier.isBonus()` is true;
- `!modifier.hasConditions()` is true.

`BonusInfoBlobEntry.writeThisBlob` then writes:

- `H`: `modifier.getName().getItemStoneMask()`;
- `D`: `modifier.getValue() * modifier.getName().getSign()`;
- `C`: `1` when `modifier instanceof StatRateFunction`, otherwise `0`.

Important parity point: Java does not serialize the add/sub/set/abs operation into this blob. For non-rate modifiers, the final byte is `0`; the `D` value uses the raw modifier value multiplied only by `StatEnum.getSign()`.

## C# Inputs Available

C# `ItemTemplateSummary.StatModifiers` already carries:

- `Operation`: `add`, `sub`, `rate`, `set`, or `abs`;
- `Name`: Java stat enum text from XML;
- `Value`: raw XML value;
- `Bonus`: XML `bonus`;
- `ChargeCondition`: nonzero when a parsed `<conditions><charge .../>` exists.

This is enough for a first deterministic implementation for condition-free template modifiers, provided the Java stat-mask/sign mapping is added in code.

## Proposed C# Mapping Rule

For future implementation, serialize a `STAT_BONUSES` entry only when:

- `modifier.Bonus` is true;
- `modifier.ChargeCondition == 0`;
- `modifier.Name` maps to a Java `StatEnum` with nonzero `itemStoneMask`.

Payload:

- `WriteH(itemStoneMask)`;
- `WriteD(modifier.Value * sign)`;
- `WriteC(modifier.Operation == "rate" ? 1 : 0)`.

Do not negate `sub` values in this blob unless a Java runtime artifact proves otherwise. Java `BonusInfoBlobEntry` does not inspect `StatSubFunction`.

## Java Mask/Sign Values Needed First

The first implementation can use a private static dictionary for stat names with nonzero item-stone masks observed in Java `StatEnum`.

Special sign:

| Stat | Mask | Sign | Notes |
|---|---:|---:|---|
| `ATTACK_SPEED` | 29 | -1 | Java writes `value * -1`; rate flag still depends on `StatRateFunction`. |

Common nonzero masks with default sign `1` include:

| Stat | Mask |
|---|---:|
| `ABNORMAL_RESISTANCE_ALL` | 1 |
| `ALLRESIST` | 2 |
| `STRVIT` | 3 |
| `KNOWIL` | 4 |
| `AGIDEX` | 5 |
| `POWER` | 6 |
| `HEALTH` | 7 |
| `ACCURACY` | 8 |
| `AGILITY` | 9 |
| `KNOWLEDGE` | 10 |
| `WILL` | 11 |
| `WATER_RESISTANCE` | 12 |
| `WIND_RESISTANCE` | 13 |
| `EARTH_RESISTANCE` | 14 |
| `FIRE_RESISTANCE` | 15 |
| `LIGHT_RESISTANCE` | 16 |
| `DARK_RESISTANCE` | 17 |
| `MAXHP` | 18 |
| `REGEN_HP` | 19 |
| `MAXMP` | 20 |
| `REGEN_MP` | 21 |
| `MAXDP` | 22 |
| `FLY_TIME` | 23 |
| `REGEN_FP` | 24 |
| `PHYSICAL_ATTACK` | 25 |
| `PHYSICAL_DEFENSE` | 26 |
| `MAGICAL_ATTACK` | 27 |
| `MAGICAL_RESIST` | 28 |
| `PHYSICAL_ACCURACY` | 30 |
| `EVASION` | 31 |
| `PARRY` | 32 |
| `BLOCK` | 33 |
| `PHYSICAL_CRITICAL` | 34 |
| `HIT_COUNT` | 35 |
| `SPEED` | 36 |
| `FLY_SPEED` | 37 |
| `ATTACK_RANGE` | 38 |
| `WEIGHT` | 39 |
| `MAGICAL_CRITICAL` | 40 |
| `CONCENTRATION` | 41 |
| `POISON_RESISTANCE` | 43 |
| `BLEED_RESISTANCE` | 44 |
| `PARALYZE_RESISTANCE` | 45 |
| `SLEEP_RESISTANCE` | 46 |
| `ROOT_RESISTANCE` | 47 |
| `BLIND_RESISTANCE` | 48 |
| `CHARM_RESISTANCE` | 49 |
| `DISEASE_RESISTANCE` | 50 |
| `SILENCE_RESISTANCE` | 51 |
| `FEAR_RESISTANCE` | 52 |
| `CURSE_RESISTANCE` | 53 |
| `CONFUSE_RESISTANCE` | 54 |
| `STUN_RESISTANCE` | 55 |
| `PERIFICATION_RESISTANCE` | 56 |
| `STUMBLE_RESISTANCE` | 57 |
| `STAGGER_RESISTANCE` | 58 |
| `OPENAERIAL_RESISTANCE` | 59 |
| `SNARE_RESISTANCE` | 60 |
| `SLOW_RESISTANCE` | 61 |
| `SPIN_RESISTANCE` | 62 |
| `BIND_RESISTANCE` | 63 |
| `DEFORM_RESISTANCE` | 64 |
| `PULLED_RESISTANCE` | 65 |
| `NOFLY_RESISTANCE` | 66 |
| resistance penetration stats | 69-92 |
| `BOOST_MAGICAL_SKILL` | 104 |
| `MAGICAL_ACCURACY` | 105 |
| `PVP_ATTACK_RATIO` | 106 |
| `PVP_DEFEND_RATIO` | 107 |
| `BOOST_CASTING_TIME` | 108 |
| `BOOST_HATE` | 109 |
| `HEAL_BOOST` | 110 |
| PvP attack/defense stats | 111-114 |
| critical resist/fortitude stats | 115-118 |
| `MAGICAL_DEFEND` | 125 |
| `MAGIC_SKILL_BOOST_RESIST` | 126 |

Stats with Java mask `0` should be skipped for `STAT_BONUSES` until runtime evidence proves the client expects a zero-mask entry.

## Test Recommendation

Add a focused packet/blob test before implementation:

- template has equipment slots and two condition-free bonus modifiers:
  - `add MAXHP value=100 bonus=true` -> entry `0x0A`, mask `18`, value `100`, rate flag `0`;
  - `rate ATTACK_SPEED value=5 bonus=true` -> entry `0x0A`, mask `29`, value `-5`, rate flag `1`;
- include a conditioned bonus modifier and a non-bonus modifier, and assert neither is serialized;
- assert `STAT_BONUSES` appears after `PREMIUM_OPTION` and before `GENERAL_INFO`, matching Java `ItemInfoBlob.getFullBlob`.

## Remaining Risks

- This audit is source-derived only; no generated Java artifact verifies C# byte parity.
- Some XML modifier names may be Java enum aliases that C# stat calculation handles elsewhere but should not be serialized without a known mask.
- Java condition handling is broader than C# `ChargeCondition`; future parser work may need to preserve other conditions before this serializer can skip them safely.
