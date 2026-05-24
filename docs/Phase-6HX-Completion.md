# Phase 6HX Completion Handoff

Created: May 24, 2026

Status: Phase 6 remains in progress. This handoff follows Phase 6HW and covers Session 720.

## Ground Rules

- Java remains the source of truth.
- Keep C# code documented with Java breadcrumbs.
- Do not mark parity as verified without byte/runtime/client evidence.
- Continue doing one focused unit, validating it, updating `docs/PHASE-6-PROGRESS.md`, committing it, and repeating.
- Keep the Migration Parity Table, remaining risks, summary metrics, and next recommended work current after every completed unit.

## Validation Baseline

- Latest focused validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj --filter "IdianPolishServiceTests|IdianPolishBurnApplicationServiceTests|PlayerEnterWorldServiceTests"`
  - Result: Passed, 34 tests.
- Latest full GameServer validation:
  - `dotnet test dotnetConversion\tests\Aion.GameServer.Tests\Aion.GameServer.Tests.csproj`
  - Result: Passed, 1294 tests.

## Recent Work Completed

### Session 720 - Idian Main-Hand Observer Burn Bridge

- Added `IdianPolishObserverEvent`.
- Added `IdianPolishService.BurnEquippedWeaponPolishChargeForObserverEvent`.
- The observer bridge burns only equipped main-hand weapon idians, matching Java `IdianStone.onEquip` observer registration.
- It preserves Java's outgoing attack `skillId == 0` guard.
- It burns defend charge for attacked and dot-attacked observer events.
- It keeps the existing `PolishChargeCondition` skill-value helper separate, since that Java path has a broader non-offhand equipment scan.
- Added `IdianPolishServiceTests.BurnEquippedWeaponPolishChargeForObserverEvent_BurnsOnlyMainHandObserverIdian`.
- Added `IdianPolishServiceTests.BurnEquippedWeaponPolishChargeForObserverEvent_SkipsSkillAttackButAllowsDotAttacked`.

## Migration Parity Snapshot

| Java Artifact | C# Artifact | Type | Port Status | Test Status | Parity Status | Notes |
|---|---|---|---|---|---|---|
| `com.aionemu.gameserver.model.items.IdianStone.onEquip` | `Aion.GameServer.Services.IdianPolishService.BurnEquippedWeaponPolishChargeForObserverEvent` | Observer Bridge / Model Helper | Partial | Regression Tested | Needs Verification | C# now models the idian observer burn selection for equipped main-hand weapon idians. Actual observer registration with `ObserveController`, lifecycle removal, `RandomBonusEffect.applyEffect`, stat recalculation, packet fanout, and production combat/effect caller invocation are still not wired. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.attack` in `IdianStone.onEquip` | `IdianPolishObserverEvent.Attack` consumed by `BurnEquippedWeaponPolishChargeForObserverEvent` | Observer Callback | Partial | Unit Tested | Needs Verification | C# preserves Java's outgoing attack `skillId == 0` guard and uses attack burn data. Real skill/attack observer dispatch and socket ordering remain unverified. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.attacked` in `IdianStone.onEquip` | `IdianPolishObserverEvent.Attacked` consumed by `BurnEquippedWeaponPolishChargeForObserverEvent` | Observer Callback | Partial | Unit Tested | Needs Verification | C# uses defend burn data for incoming attacked events and does not apply the outgoing attack skill-id guard. Real attacked observer dispatch, threading, and packet/persistence ordering remain unverified. |
| `com.aionemu.gameserver.controllers.observer.ActionObserver.dotattacked` in `IdianStone.onEquip` | `IdianPolishObserverEvent.DotAttacked` consumed by `BurnEquippedWeaponPolishChargeForObserverEvent` | Observer Callback | Partial | Unit Tested | Needs Verification | C# uses defend burn data for dot-attacked events. Dot effect lifecycle, observer ordering, and live effect dispatch remain unverified. |
| `com.aionemu.gameserver.model.items.ItemSlot.MAIN_HAND` | `IdianPolishService` main-hand mask guard | Equipment Slot Dependency | Partial | Unit Tested | Needs Verification | C# main-hand observer bridge uses the Java `MAIN_HAND` slot mask and deliberately differs from the separate `PolishChargeCondition` skill-value helper, which can burn broader non-offhand weapon slots. Broader equipment-slot modeling and dual-weapon/offhand runtime behavior need live validation. |
| `com.aionemu.gameserver.skillengine.condition.PolishChargeCondition` | `IdianPolishService.BurnEquippedWeaponPolishCharge` | Skill Condition Helper | Partial | Regression Tested | Needs Verification | Existing C# skill-condition burn helper remains separate and continues to skip main/sub offhand weapons while honoring explicit skill-value burns. This session did not wire the Java `SkillEngine` caller or compare live skill runtime behavior. |
| `com.aionemu.gameserver.model.items.IdianStone.decreasePolishCharge` | `IdianPolishService.DecreasePolishCharge` / observer burn bridge | Model Helper | Partial | Regression Tested | Needs Verification | Observer bridge reuses the source-derived clamp, low-charge, and exhaustion update kinds. Java synchronization, `PersistentState`, `ItemStoneListDAO`, exact exhausted-item serialization ordering, and live-client behavior remain unresolved. |

## Tests Added Or Updated

- `IdianPolishServiceTests.BurnEquippedWeaponPolishChargeForObserverEvent_BurnsOnlyMainHandObserverIdian`
  - Validates the observer bridge burns only equipped main-hand weapon idians, skips sub-hand/offhand/non-weapon/unequipped items, and uses attack versus defend burn amounts.
- `IdianPolishServiceTests.BurnEquippedWeaponPolishChargeForObserverEvent_SkipsSkillAttackButAllowsDotAttacked`
  - Validates Java's outgoing attack `skillId == 0` guard and dot-attacked defend burn behavior.
- Existing `IdianPolishServiceTests`, `IdianPolishBurnApplicationServiceTests`, and `PlayerEnterWorldServiceTests` matched by the focused filter were rerun.

These tests are source-derived from Java. They do not compare against Java runtime execution, live observer dispatch, Java-generated golden packets, packet ordering, threading behavior, serialization, DAO behavior, or live client behavior.

## Summary Metrics

- Total Java artifacts discovered in this handoff window: 7
- Total artifacts ported or partially modeled in this handoff window: 1 idian main-hand observer burn bridge slice.
- Total artifacts with verified parity: 0
- Total artifacts needing verification: 7
- Total blocked/not-started artifacts: production combat/effect caller invocation, real ObserveController lifecycle, RandomBonusEffect/stat fanout, exhausted-idian packet/DAO ordering comparison, synchronized/threading comparison, and live-client validation.
- Estimated overall migration completion: 65%

## Remaining Risks

- The observer bridge is still not invoked from a production combat/effect observer caller.
- Actual Java `ObserveController` registration/removal, `RandomBonusEffect` stat application/removal, and `IdianStone.onUnEquip` side effects remain partial.
- Java synchronization, exhausted-idian packet-before-clear ordering, DAO delete timing, and persistence ordering remain unverified.
- Main-hand/offhand behavior is source-derived but not live validated against dual-wield runtime equipment states.
- No Java runtime, live database, or live-client comparison was run.

## Next Recommended Unit of Work

Combine the represented charge and idian observer burn paths behind a first production-style combat/effect caller seam if one is stable enough: plan burns, apply packets/in-memory updates, persist charge updates and exhausted idian deletes, and document packet ordering. If the caller seam remains premature, continue the idian lifecycle by modeling the `IdianStone.onUnEquip` stat/effect removal boundary and remaining `RandomBonusEffect` fanout assumptions.

## Resume Checklist

1. Confirm `git status --short --branch` is clean on branch `4.8`.
2. Read:
   - `docs/csharp-port.md`
   - `docs/PHASE-6-PROGRESS.md`
   - `docs/Phase-6HW-Completion.md`
   - this handoff
3. Inspect selected Java source and nearest C# tests before touching code.
4. Implement one narrow unit with Java breadcrumbs.
5. Add focused tests that state what is source-derived and what remains unverified.
6. Run focused tests, then full GameServer tests.
7. Update `docs/PHASE-6-PROGRESS.md` with the required Migration Parity Table and metrics.
8. Create the next handoff document and commit the unit.
