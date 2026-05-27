# Phase 6 Bind-Point Teleport - Plume Tempering Stat Payload Audit

Date: 2026-05-27
Unit of Work: UOW-1390
Status: Read-only audit complete. No serializer code changed.

## Scope

This unit audits Java plume tempering fields in `EnchantInfoBlobEntry`, another remaining item-blob gap that blocks unusual-storage `SM_WAREHOUSE_ADD_ITEM` byte comparison.

## Java Behavior

Java `EnchantInfoBlobEntry.writeInfo` writes the tempering level, then several unknown zero fields, then plume stat pairs:

- if `item.getTempering() > 0` and item group is `PLUME`:
  - first stat id: `PlumStatEnum.PLUM_HP.getId()` = `42`;
  - first value: `150 * item.getTempering()`;
  - second stat id:
    - `30` for `TSHIRT_PHYSICAL`;
    - `35` otherwise;
  - second value:
    - `4 * item.getTempering() + item.getRndPlumeBonusValue()` for physical;
    - `20 * item.getTempering() + item.getRndPlumeBonusValue()` otherwise.
- otherwise, Java writes zero id/value pairs for the first two stat slots.

After those first two stat pairs, Java still writes zeroes for stat slots three through six and the 4.7.5 unknown field.

Java `TemperingEffect.addPlumeStatFunctions` uses the same values for runtime stats:

- `PHYSICAL_ATTACK` or `BOOST_MAGICAL_SKILL`;
- `MAXHP`;
- `item.getRndPlumeBonusValue()`.

Java `TamperingAction.setTemperingLevel` mutates `rndPlumeBonusValue` only for plume tempering above level 4 and resets it to zero when tempering drops to 4 or below.

## Current C# Inputs

C# already has the inputs needed for the packet payload:

- `ItemTemplateSummary.IsPlume`;
- `ItemTemplateSummary.TemperingName`;
- `InventoryItem.Tempering`;
- `InventoryItem.RandomPlumeBonus`.

C# `TemperingTable.GetPlumeModifiers` already mirrors Java runtime stat values:

- HP boost: 150 per tempering level;
- magical boost: 20 per tempering level plus random plume bonus;
- physical attack: 4 per tempering level plus random plume bonus;
- physical branch selected by `TemperingName == "TSHIRT_PHYSICAL"`.

## Future Implementation Recommendation

This gap can be implemented directly inside `SmInventoryInfo.WriteEnchantInfo` without adding a new static-data dependency:

1. Replace the four zero `WriteD` calls for the first two plume stat pairs with a helper.
2. Helper rule:
   - if `item.Tempering > 0 && template.IsPlume`, write `(42, 150 * tempering)` and either `(30, 4 * tempering + randomPlumeBonus)` or `(35, 20 * tempering + randomPlumeBonus)`;
   - otherwise write the existing four zero `D` values.
3. Pass `ItemTemplateSummary template` into `WriteEnchantInfo` or add a new private helper that receives both item and template.
4. Keep the remaining third-to-sixth stat slots and unknown field zero as Java does.

## Test Recommendation

Add two focused blob tests:

- physical plume:
  - `ItemGroup = "PLUME"`;
  - `TemperingName = "TSHIRT_PHYSICAL"`;
  - `Tempering = 5`;
  - `RandomPlumeBonus = 3`;
  - assert first pair `(42, 750)` and second pair `(30, 23)`.
- magical plume:
  - `ItemGroup = "PLUME"`;
  - `TemperingName` not equal to `TSHIRT_PHYSICAL`;
  - `Tempering = 5`;
  - `RandomPlumeBonus = 8`;
  - assert first pair `(42, 750)` and second pair `(35, 108)`.

Keep generated Java runtime artifact comparison guarded until actual Java artifacts exist.

## Remaining Risks

- Java runtime artifact generation remains blocked locally by missing Maven/Java 25 tooling.
- `ItemTemplateSummary.IsPlume` depends on item group string parity with Java `ItemGroup.PLUME`.
- Runtime mutation of `RandomPlumeBonus` is handled elsewhere; this audit only covers packet serialization inputs.
- The exact byte offset inside the 138-byte enchant blob should be covered by focused tests before implementation is committed.
